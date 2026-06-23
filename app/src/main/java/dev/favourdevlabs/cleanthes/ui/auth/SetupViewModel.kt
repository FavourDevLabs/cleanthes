package dev.favourdevlabs.cleanthes.ui.auth

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.favourdevlabs.cleanthes.security.KeyDerivation
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

// Package-level — shared with LoginActivity (same package, no qualification needed)
internal const val PREFS_NAME = "vault_secure_prefs"
internal const val KEY_VAULT_EXISTS = "vault_exists"
internal const val KEY_AUTH_SALT = "auth_salt"
internal const val KEY_ENC_SALT = "enc_salt"
internal const val KEY_MASTER_HASH = "master_hash"
internal const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
internal const val MIN_PASSWORD_LENGTH = 8
internal const val KEY_WRAPPED_VAULT_KEY_PASSWORD = "wrapped_vault_key_password"
internal const val KEY_WRAPPED_VAULT_KEY_BIOMETRIC = "wrapped_vault_key_biometric"
internal const val KEY_BIOMETRIC_IV = "biometric_iv"

sealed interface SetupNavEvent {
    data object NavigateToHome : SetupNavEvent

    data class TriggerBiometricEnrollment(
        val cipher: Cipher,
    ) : SetupNavEvent
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

    val matchState: MatchState get() =
        when {
            confirm.isEmpty() -> MatchState.EMPTY
            password == confirm -> MatchState.MATCH
            else -> MatchState.MISMATCH
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
class SetupViewModel
    @Inject
    constructor(
        app: Application,
        private val sessionManager: SessionManager,
    ) : AndroidViewModel(app) {
        private val _uiState = MutableStateFlow(SetupUiState())
        val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

        private val _navEvents = Channel<SetupNavEvent>(Channel.BUFFERED)
        val navEvents = _navEvents.receiveAsFlow()

        // Held in memory only between the password-step and the optional biometric
        // enrollment step — never persisted unwrapped.
        private var pendingVaultKey: SecretKey? = null

        fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }

        fun onConfirmChange(value: String) = _uiState.update { it.copy(confirm = value, errorMessage = null) }

        fun onPasswordVisibilityToggle() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

        fun onConfirmVisibilityToggle() = _uiState.update { it.copy(confirmVisible = !it.confirmVisible) }

        fun onAcknowledgeToggle(checked: Boolean) = _uiState.update { it.copy(acknowledged = checked) }

        fun attemptSetup() {
            val state = _uiState.value
            val password = state.password
            val confirm = state.confirm

            val error =
                when {
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

            if (error != null) {
                _uiState.update { it.copy(errorMessage = error) }
                return
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            viewModelScope.launch { performSetup(password) }
        }

        private suspend fun performSetup(masterPassword: String) {
            try {
                val (encSalt, vaultKey, wrappedVaultKeyPassword, storedHash) =
                    withContext(Dispatchers.IO) {
                        val storedHash = KeyDerivation.hashPassword(masterPassword.toCharArray())
                        val encSaltBytes = KeyDerivation.generateSalt()
                        val encSalt = Base64.encodeToString(encSaltBytes, Base64.NO_WRAP)

                        // Single random vault key — the only key that ever encrypts vault data.
                        // Both unlock paths independently wrap this same key.
                        val vaultKey = KeyDerivation.generateVaultKey()

                        // Password path: derive a key from the password, wrap vaultKey under it.
                        val pwdDerivedKey = KeyDerivation.deriveKey(masterPassword.toCharArray(), encSaltBytes)
                        val wrappedVaultKeyPassword = KeyDerivation.wrapKey(vaultKey, pwdDerivedKey)

                        Quadruple(encSalt, vaultKey, wrappedVaultKeyPassword, storedHash)
                    }

                val masterKey =
                    MasterKey
                        .Builder(getApplication<Application>())
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()

                EncryptedSharedPreferences
                    .create(
                        getApplication(),
                        PREFS_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                    ).edit()
                    .putBoolean(KEY_VAULT_EXISTS, true)
                    .putString(KEY_AUTH_SALT, storedHash.saltBase64)
                    .putString(KEY_ENC_SALT, encSalt)
                    .putString(KEY_MASTER_HASH, storedHash.hashBase64)
                    .putBoolean(KEY_BIOMETRIC_ENABLED, false)
                    .putString(KEY_WRAPPED_VAULT_KEY_PASSWORD, wrappedVaultKeyPassword)
                    .apply()

                pendingVaultKey = vaultKey
                _uiState.update { it.copy(isLoading = false, showSecondGate = true) }
            } catch (e: Exception) {
                android.util.Log.e("CLEANTHES_SETUP", "Setup failed", e)
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Setup failed. Please try again.")
                }
            }
        }

        fun enableBiometricEnrollment() {
            val vaultKey = pendingVaultKey ?: return
            _uiState.update { it.copy(isEnrollingBiometric = true) }
            viewModelScope.launch {
                try {
                    val cipher =
                        withContext(Dispatchers.IO) {
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
            val vaultKey = pendingVaultKey
            if (vaultKey == null) {
                _uiState.update { it.copy(isEnrollingBiometric = false) }
                return
            }
            viewModelScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val wrappedBytes = unlockedCipher.doFinal(vaultKey.encoded)
                        val wrappedVaultKeyBiometric = Base64.encodeToString(wrappedBytes, Base64.NO_WRAP)
                        val biometricIv = Base64.encodeToString(unlockedCipher.iv, Base64.NO_WRAP)

                        val masterKey =
                            MasterKey
                                .Builder(getApplication<Application>())
                                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                                .build()

                        EncryptedSharedPreferences
                            .create(
                                getApplication(),
                                PREFS_NAME,
                                masterKey,
                                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                            ).edit()
                            .putBoolean(KEY_BIOMETRIC_ENABLED, true)
                            .putString(KEY_WRAPPED_VAULT_KEY_BIOMETRIC, wrappedVaultKeyBiometric)
                            .putString(KEY_BIOMETRIC_IV, biometricIv)
                            .apply()
                    }
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

        fun onBiometricEnrollmentFailure() {
            _uiState.update { it.copy(isEnrollingBiometric = false) }
        }

        fun onBiometricEnrollmentError(message: String) {
            _uiState.update { it.copy(isEnrollingBiometric = false, errorMessage = message) }
        }

        fun skipBiometricEnrollment() {
            val vaultKey = pendingVaultKey
            pendingVaultKey = null
            if (vaultKey != null) {
                sessionManager.setSessionKey(vaultKey)
            }
            viewModelScope.launch { _navEvents.send(SetupNavEvent.NavigateToHome) }
        }
    }

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)
