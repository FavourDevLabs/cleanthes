package dev.favourdevlabs.cleanthes.ui.auth

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dev.favourdevlabs.cleanthes.security.KeyDerivation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Package-level — shared with LoginActivity (same package, no qualification needed)
internal const val PREFS_NAME            = "vault_secure_prefs"
internal const val KEY_VAULT_EXISTS      = "vault_exists"
internal const val KEY_AUTH_SALT         = "auth_salt"
internal const val KEY_ENC_SALT          = "enc_salt"
internal const val KEY_MASTER_HASH       = "master_hash"
internal const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
internal const val KEY_BIOMETRIC_SECRET  = "biometric_secret"
internal const val MIN_PASSWORD_LENGTH   = 8

sealed interface SetupNavEvent {
    data object NavigateToHome : SetupNavEvent
}

data class SetupUiState(
    val password: String = "",
    val confirm: String = "",
    val passwordVisible: Boolean = false,
    val confirmVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val acknowledged: Boolean = false,
) {
    val strengthScore: Int get() = computeStrengthScore(password)

    val matchState: MatchState get() = when {
        confirm.isEmpty()      -> MatchState.EMPTY
        password == confirm    -> MatchState.MATCH
        else                   -> MatchState.MISMATCH
    }

    val canCreate: Boolean get() = acknowledged && !isLoading

    enum class MatchState { EMPTY, MATCH, MISMATCH }
}

private fun computeStrengthScore(password: String): Int {
    if (password.isEmpty()) return 0
    var score = 0
    if (password.length >= MIN_PASSWORD_LENGTH)                               score++
    if (password.any { it.isUpperCase() })                                    score++
    if (password.any { it.isDigit() })                                        score++
    if (password.any { it in "!@#\$%^&*()_+-=[]{}|;':\",./<>?" })           score++
    if (password.length >= 16)                                                 score++
    return score
}

class SetupViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private val _navEvents = Channel<SetupNavEvent>(Channel.BUFFERED)
    val navEvents = _navEvents.receiveAsFlow()

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

        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch { performSetup(password) }
    }

    private suspend fun performSetup(masterPassword: String) {
        try {
            withContext(Dispatchers.IO) {
                val storedHash   = KeyDerivation.hashPassword(masterPassword.toCharArray())
                val encSaltBytes = KeyDerivation.generateSalt()
                val encSalt      = Base64.encodeToString(encSaltBytes, Base64.NO_WRAP)

                val masterKey = MasterKey.Builder(getApplication<Application>())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    getApplication(),
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                ).edit()
                    .putBoolean(KEY_VAULT_EXISTS,      true)
                    .putString(KEY_AUTH_SALT,          storedHash.saltBase64)
                    .putString(KEY_ENC_SALT,           encSalt)
                    .putString(KEY_MASTER_HASH,        storedHash.hashBase64)
                    .putBoolean(KEY_BIOMETRIC_ENABLED, true)
                    .putString(KEY_BIOMETRIC_SECRET,   masterPassword)
                    .apply()
            }
            _navEvents.send(SetupNavEvent.NavigateToHome)
        } catch (_: Exception) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "Setup failed. Please try again.")
            }
        }
    }
}
