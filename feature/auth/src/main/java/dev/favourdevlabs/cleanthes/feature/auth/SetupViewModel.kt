package dev.favourdevlabs.cleanthes.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.favourdevlabs.cleanthes.data.api.usecase.EnrolBiometric
import dev.favourdevlabs.cleanthes.data.api.usecase.InitialiseCitadel
import dev.favourdevlabs.cleanthes.data.api.usecase.LoadCitadelCredentials
import dev.favourdevlabs.cleanthes.domain.model.CitadelProfile
import dev.favourdevlabs.cleanthes.security.KeystoreManager
import dev.favourdevlabs.cleanthes.security.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.inject.Inject

internal const val MIN_PASSWORD_LENGTH = 8

sealed interface SetupNavEvent {
    data object NavigateToHome : SetupNavEvent
    data object NavigateToLogin : SetupNavEvent
    data class TriggerBiometricEnrollment(val cipher: Cipher) : SetupNavEvent
}

data class SetupUiState(
    val password: String = "",
    val confirm: String = "",
    val passwordVisible: Boolean = false,
    val confirmVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val acknowledged: Boolean = false,
    val showSecondGate: Boolean = false,
    val isEnrollingBiometric: Boolean = false,
    val showThirdGate: Boolean = false,
    val decoyPassword: String = "",
    val decoyConfirm: String = "",
    val decoyPasswordVisible: Boolean = false,
    val decoyConfirmVisible: Boolean = false,
    val isCreatingDecoy: Boolean = false,
    val decoyErrorMessage: String? = null,
) {
    val strengthScore: Int get() = computeStrengthScore(password)

    val matchState: MatchState get() = when {
        confirm.isEmpty()   -> MatchState.EMPTY
        password == confirm -> MatchState.MATCH
        else                -> MatchState.MISMATCH
    }

    val canCreate: Boolean get() = acknowledged && !isLoading

    enum class MatchState { EMPTY, MATCH, MISMATCH }

    val decoyStrengthScore: Int get() = computeStrengthScore(decoyPassword)

    val decoyMatchState: MatchState get() = when {
        decoyConfirm.isEmpty()        -> MatchState.EMPTY
        decoyPassword == decoyConfirm -> MatchState.MATCH
        else                          -> MatchState.MISMATCH
    }
}

private fun computeStrengthScore(password: String): Int {
    if (password.isEmpty()) return 0
    var score = 0
    if (password.length >= MIN_PASSWORD_LENGTH) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { it in "!@#\$%^&*()_+-=[]{}|;':\",./<>?" }) score++
    if (password.length >= 16) score++
    return score
}

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val initialiseCitadel: InitialiseCitadel,
    private val enrolBiometric: EnrolBiometric,
    private val loadCitadelCredentials: LoadCitadelCredentials,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private val _navEvents = Channel<SetupNavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    private var pendingCitadelKey: SecretKey? = null

    // Held only for the duration of setup, to validate the decoy password
    // differs from the real one at the third gate. Cleared the moment the
    // flow finishes (decoy created or skipped) — never persisted.
    private var pendingMasterPassword: String? = null

    fun checkCitadelExists() {
        viewModelScope.launch {
            try {
                val result = loadCitadelCredentials(CitadelProfile.REAL)
                if (result.citadelExists) {
                    _navEvents.send(SetupNavEvent.NavigateToLogin)
                }
            } catch (_: Exception) {}
        }
    }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, errorMessage = null) }

    fun onConfirmChange(value: String) =
        _uiState.update { it.copy(confirm = value, errorMessage = null) }

    fun onPasswordVisibilityToggle() =
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun onConfirmVisibilityToggle() =
        _uiState.update { it.copy(confirmVisible = !it.confirmVisible) }

    fun onAcknowledgeToggle(checked: Boolean) =
        _uiState.update { it.copy(acknowledged = checked) }

    fun attemptSetup() {
        val state    = _uiState.value
        val password = state.password
        val confirm  = state.confirm

        val error = when {
            password.length < MIN_PASSWORD_LENGTH ->
                "Password must be at least $MIN_PASSWORD_LENGTH characters"
            !password.any { it.isDigit() } ->
                "Password must contain at least one number"
            !password.any { it in "!@#\$%^&*()_+-=[]{}|;':\",./<>?" } ->
                "Password must contain a special character"
            password != confirm ->
                "Passwords do not match"
            else -> null
        }

        if (error != null) { _uiState.update { it.copy(errorMessage = error) }; return }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch { performSetup(password) }
    }

    private suspend fun performSetup(masterPassword: String) {
        try {
            val result = initialiseCitadel(masterPassword, CitadelProfile.REAL)
            pendingCitadelKey = result.citadelKey
            pendingMasterPassword = masterPassword
            _uiState.update { it.copy(isLoading = false, showSecondGate = true) }
        } catch (e: Exception) {
            android.util.Log.e("CLEANTHES_SETUP", "Setup failed", e)
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "Setup failed. Please try again.")
            }
        }
    }

    fun enableBiometricEnrollment() {
        pendingCitadelKey ?: return
        _uiState.update { it.copy(isEnrollingBiometric = true) }
        viewModelScope.launch {
            try {
                val cipher = withContext(Dispatchers.IO) {
                    KeystoreManager.generateBiometricKey()
                    KeystoreManager.getEncryptCipher()
                }
                _navEvents.send(SetupNavEvent.TriggerBiometricEnrollment(cipher))
            } catch (e: Exception) {
                android.util.Log.e("CLEANTHES_SETUP", "Biometric enrollment failed", e)
                _uiState.update {
                    it.copy(isEnrollingBiometric = false, errorMessage = "Biometric setup unavailable.")
                }
            }
        }
    }

    fun onBiometricEnrollmentSuccess(unlockedCipher: Cipher) {
        val citadelKey = pendingCitadelKey ?: run {
            _uiState.update { it.copy(isEnrollingBiometric = false) }
            return
        }
        viewModelScope.launch {
            try {
                enrolBiometric(citadelKey, unlockedCipher)
                _uiState.update {
                    it.copy(isEnrollingBiometric = false, showSecondGate = false, showThirdGate = true)
                }
            } catch (e: Exception) {
                android.util.Log.e("CLEANTHES_SETUP", "Biometric wrap failed", e)
                _uiState.update {
                    it.copy(isEnrollingBiometric = false, errorMessage = "Biometric setup failed.")
                }
            }
        }
    }

    fun onBiometricEnrollmentFailure() =
        _uiState.update { it.copy(isEnrollingBiometric = false) }

    fun onBiometricEnrollmentError(message: String) =
        _uiState.update { it.copy(isEnrollingBiometric = false, errorMessage = message) }

    fun skipBiometricEnrollment() {
        _uiState.update { it.copy(showSecondGate = false, showThirdGate = true) }
    }

    // ── Third gate: decoy citadel ────────────────────────────────────────────

    fun onDecoyPasswordChange(value: String) =
        _uiState.update { it.copy(decoyPassword = value, decoyErrorMessage = null) }

    fun onDecoyConfirmChange(value: String) =
        _uiState.update { it.copy(decoyConfirm = value, decoyErrorMessage = null) }

    fun onDecoyPasswordVisibilityToggle() =
        _uiState.update { it.copy(decoyPasswordVisible = !it.decoyPasswordVisible) }

    fun onDecoyConfirmVisibilityToggle() =
        _uiState.update { it.copy(decoyConfirmVisible = !it.decoyConfirmVisible) }

    fun attemptCreateDecoy() {
        val state = _uiState.value
        val decoyPassword = state.decoyPassword
        val decoyConfirm = state.decoyConfirm
        val realPassword = pendingMasterPassword

        val error = when {
            decoyPassword.length < MIN_PASSWORD_LENGTH ->
                "Decoy password must be at least $MIN_PASSWORD_LENGTH characters"
            !decoyPassword.any { it.isDigit() } ->
                "Decoy password must contain at least one number"
            !decoyPassword.any { it in "!@#\$%^&*()_+-=[]{}|;':\",./<>?" } ->
                "Decoy password must contain a special character"
            decoyPassword != decoyConfirm ->
                "Decoy passwords do not match"
            realPassword != null && decoyPassword == realPassword ->
                "The decoy must be a stranger to your true gate — choose a different password"
            else -> null
        }

        if (error != null) { _uiState.update { it.copy(decoyErrorMessage = error) }; return }

        _uiState.update { it.copy(isCreatingDecoy = true, decoyErrorMessage = null) }
        viewModelScope.launch { performDecoyCreation(decoyPassword) }
    }

    private suspend fun performDecoyCreation(decoyPassword: String) {
        try {
            initialiseCitadel(decoyPassword, CitadelProfile.DECOY)
            finishSetup()
        } catch (e: Exception) {
            android.util.Log.e("CLEANTHES_SETUP", "Decoy creation failed", e)
            _uiState.update {
                it.copy(isCreatingDecoy = false, decoyErrorMessage = "The second citadel could not be sealed. Try again.")
            }
        }
    }

    fun skipDecoyCreation() {
        viewModelScope.launch { finishSetup() }
    }

    private suspend fun finishSetup() {
        val citadelKey = pendingCitadelKey
        pendingCitadelKey = null
        pendingMasterPassword = null
        if (citadelKey != null) sessionManager.setSessionKey(citadelKey)
        _navEvents.send(SetupNavEvent.NavigateToHome)
    }
}
