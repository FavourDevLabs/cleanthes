package dev.favourdevlabs.cleanthes.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.favourdevlabs.cleanthes.data.api.usecase.EnrolBiometric
import dev.favourdevlabs.cleanthes.data.api.usecase.InitialiseVault
import dev.favourdevlabs.cleanthes.data.api.usecase.LoadVaultCredentials
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
) {
    val strengthScore: Int get() = computeStrengthScore(password)

    val matchState: MatchState get() = when {
        confirm.isEmpty()   -> MatchState.EMPTY
        password == confirm -> MatchState.MATCH
        else                -> MatchState.MISMATCH
    }

    val canCreate: Boolean get() = acknowledged && !isLoading

    enum class MatchState { EMPTY, MATCH, MISMATCH }
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
    private val initialiseVault: InitialiseVault,
    private val enrolBiometric: EnrolBiometric,
    private val loadVaultCredentials: LoadVaultCredentials,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private val _navEvents = Channel<SetupNavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

    private var pendingVaultKey: SecretKey? = null

    fun checkVaultExists() {
        viewModelScope.launch {
            try {
                val result = loadVaultCredentials()
                if (result.vaultExists) {
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
            val result = initialiseVault(masterPassword)
            pendingVaultKey = result.vaultKey
            _uiState.update { it.copy(isLoading = false, showSecondGate = true) }
        } catch (e: Exception) {
            android.util.Log.e("CLEANTHES_SETUP", "Setup failed", e)
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "Setup failed. Please try again.")
            }
        }
    }

    fun enableBiometricEnrollment() {
        pendingVaultKey ?: return
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
        val vaultKey = pendingVaultKey ?: run {
            _uiState.update { it.copy(isEnrollingBiometric = false) }
            return
        }
        viewModelScope.launch {
            try {
                enrolBiometric(vaultKey, unlockedCipher)
                sessionManager.setSessionKey(vaultKey)
                pendingVaultKey = null
                _navEvents.send(SetupNavEvent.NavigateToHome)
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
        val vaultKey = pendingVaultKey
        pendingVaultKey = null
        if (vaultKey != null) sessionManager.setSessionKey(vaultKey)
        viewModelScope.launch { _navEvents.send(SetupNavEvent.NavigateToHome) }
    }
}

