package dev.favourdevlabs.cleanthes.feature.auth

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.favourdevlabs.cleanthes.data.api.usecase.LoadVaultCredentials
import dev.favourdevlabs.cleanthes.domain.usecase.UnlockVault
import dev.favourdevlabs.cleanthes.security.BiometricHelper
import dev.favourdevlabs.cleanthes.security.KeyDerivation
import dev.favourdevlabs.cleanthes.security.KeystoreManager
import dev.favourdevlabs.cleanthes.security.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

internal const val MAX_ATTEMPTS = 5
internal const val LOCKOUT_DURATION_SECONDS = 30

sealed interface LoginEvent {
    data object NavigateToHome : LoginEvent
    data class TriggerBiometric(val cipher: Cipher) : LoginEvent
}

data class LoginUiState(
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val failedAttempts: Int = 0,
    val isLockedOut: Boolean = false,
    val lockoutSecondsRemaining: Int = 0,
    val showBiometricSection: Boolean = false,
    val isAuthenticating: Boolean = false,
    val shakeCounter: Int = 0,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val unlockVault: UnlockVault,
    private val loadVaultCredentials: LoadVaultCredentials,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var credentials: LoadVaultCredentials.Result? = null
    private var failedAttempts = 0

    init {
        viewModelScope.launch { loadCredentials() }
    }

    private suspend fun loadCredentials() {
        try {
            val result = loadVaultCredentials()
            credentials = result

            val biometricAvailable =
                result.biometricEnabled &&
                result.biometricIv != null &&
                result.wrappedVaultKeyBiometric != null

            _uiState.update { it.copy(showBiometricSection = biometricAvailable) }
            if (biometricAvailable) requestBiometricAuth()
        } catch (_: Exception) {}
    }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, errorMessage = null) }

    fun onPasswordVisibilityToggle() =
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun attemptPasswordUnlock() {
        val state = _uiState.value
        if (state.isLockedOut || state.isLoading) return
        if (state.password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Enter your master password") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch { verifyPassword(state.password) }
    }

    private suspend fun verifyPassword(attempt: String) {
        val creds = credentials
        val authSalt   = creds?.authSalt   ?: return resetLoading("Vault data missing")
        val masterHash = creds.masterHash  ?: return resetLoading("Vault data missing")
        try {
            val correct = withContext(Dispatchers.IO) {
                KeyDerivation.verifyMasterPassword(attempt.toCharArray(), authSalt, masterHash)
            }
            if (correct) {
                failedAttempts = 0
                unlockWithPassword(attempt)
            } else {
                _uiState.update { it.copy(isLoading = false) }
                handleFailedAttempt()
            }
        } catch (_: Exception) {
            resetLoading("An error occurred. Please try again.")
        }
    }

    private suspend fun unlockWithPassword(masterPassword: String) {
        try {
            val encSalt       = credentials?.encSalt               ?: throw IllegalStateException("Salt missing")
            val wrappedVaultKey = credentials?.wrappedVaultKeyPassword ?: throw IllegalStateException("Vault key missing")
            unlockVault(UnlockVault.Params.Password(masterPassword, encSalt, wrappedVaultKey))
            _events.send(LoginEvent.NavigateToHome)
        } catch (_: Exception) {
            _uiState.update {
                it.copy(isLoading = false, isAuthenticating = false,
                    errorMessage = "An error occurred. Please try again.")
            }
        }
    }

    fun requestBiometricAuth() {
        val state = _uiState.value
        if (state.isAuthenticating || state.isLoading || state.isLockedOut) return

        val ivB64 = credentials?.biometricIv ?: run {
            _uiState.update { it.copy(errorMessage = "Biometric data missing") }
            return
        }

        try {
            val iv     = Base64.decode(ivB64, Base64.NO_WRAP)
            val cipher = KeystoreManager.getDecryptCipher(iv)
            _uiState.update { it.copy(isAuthenticating = true) }
            viewModelScope.launch { _events.send(LoginEvent.TriggerBiometric(cipher)) }
        } catch (_: Exception) {
            _uiState.update { it.copy(errorMessage = "Biometric unlock unavailable. Use your password.") }
        }
    }

    fun onBiometricSuccess(unlockedCipher: Cipher) {
        _uiState.update { it.copy(isAuthenticating = false, isLoading = true) }
        viewModelScope.launch { unlockWithBiometric(unlockedCipher) }
    }

    private suspend fun unlockWithBiometric(unlockedCipher: Cipher) {
        try {
            val wrappedVaultKeyB64 = credentials?.wrappedVaultKeyBiometric
                ?: throw IllegalStateException("Vault key missing")
            val vaultKey = withContext(Dispatchers.IO) {
                val wrappedBytes = Base64.decode(wrappedVaultKeyB64, Base64.NO_WRAP)
                val rawKeyBytes  = unlockedCipher.doFinal(wrappedBytes)
                SecretKeySpec(rawKeyBytes, "AES")
            }
            unlockVault(UnlockVault.Params.Biometric(vaultKey))
            _events.send(LoginEvent.NavigateToHome)
        } catch (_: Exception) {
            _uiState.update {
                it.copy(isLoading = false, isAuthenticating = false,
                    errorMessage = "An error occurred. Please try again.")
            }
        }
    }

    fun onBiometricFailure() = _uiState.update { it.copy(isAuthenticating = false) }

    fun onBiometricError(message: String) =
        _uiState.update { it.copy(isAuthenticating = false, errorMessage = message) }

    private fun handleFailedAttempt() {
        failedAttempts++
        _uiState.update {
            it.copy(
                errorMessage  = "Wrong password",
                password      = "",
                failedAttempts = failedAttempts,
                shakeCounter  = it.shakeCounter + 1,
            )
        }
        if (failedAttempts >= MAX_ATTEMPTS) startLockout()
    }

    private fun startLockout() {
        _uiState.update { it.copy(isLockedOut = true) }
        viewModelScope.launch {
            var remaining = LOCKOUT_DURATION_SECONDS
            while (remaining > 0) {
                _uiState.update { it.copy(lockoutSecondsRemaining = remaining) }
                delay(1000)
                remaining--
            }
            failedAttempts = 0
            _uiState.update {
                it.copy(isLockedOut = false, lockoutSecondsRemaining = 0,
                    failedAttempts = 0, errorMessage = null)
            }
        }
    }

    private fun resetLoading(error: String) =
        _uiState.update { it.copy(isLoading = false, errorMessage = error) }
}

