package dev.favourdevlabs.cleanthes.feature.auth

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.favourdevlabs.cleanthes.data.api.usecase.LoadVaultCredentials
import dev.favourdevlabs.cleanthes.domain.usecase.ActivateVaultProfile
import dev.favourdevlabs.cleanthes.domain.model.VaultProfile
import dev.favourdevlabs.cleanthes.domain.usecase.RecordAuditEvent
import dev.favourdevlabs.cleanthes.domain.usecase.UnlockVault
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

    data class TriggerBiometric(
        val cipher: Cipher,
    ) : LoginEvent
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
class LoginViewModel
    @Inject
    constructor(
        private val sessionManager: SessionManager,
        private val unlockVault: UnlockVault,
        private val loadVaultCredentials: LoadVaultCredentials,
        private val recordAuditEvent: RecordAuditEvent,
        private val activateVaultProfile: ActivateVaultProfile,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LoginUiState())
        val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

        private val _events = Channel<LoginEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        // Biometric unlock is scoped to the REAL profile only — a duress
        // scenario is precisely when someone can force a fingerprint/face
        // unlock, so the decoy must never be biometric-reachable.
        private var realCredentials: LoadVaultCredentials.Result? = null
        private var decoyCredentials: LoadVaultCredentials.Result? = null
        private var failedAttempts = 0

        init {
            viewModelScope.launch { loadCredentials() }
        }

        private suspend fun loadCredentials() {
            try {
                val real = loadVaultCredentials(VaultProfile.REAL)
                val decoy = loadVaultCredentials(VaultProfile.DECOY)
                realCredentials = real
                decoyCredentials = decoy

                val biometricAvailable =
                    real.biometricEnabled &&
                        real.biometricIv != null &&
                        real.wrappedVaultKeyBiometric != null

                _uiState.update { it.copy(showBiometricSection = biometricAvailable) }
                if (biometricAvailable) requestBiometricAuth()
            } catch (_: Exception) {
            }
        }

        fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }

        fun onPasswordVisibilityToggle() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

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

        /**
         * Checks the attempt against BOTH profiles unconditionally, every
         * time — never short-circuits on the first match. If the decoy
         * profile doesn't exist, a dummy derivation of identical cost runs
         * in its place so that timing cannot reveal whether a decoy is
         * configured. Whoever is holding this app under duress must not be
         * able to learn anything from response latency.
         */
        private suspend fun verifyPassword(attempt: String) {
            val real = realCredentials
            val realSalt = real?.authSalt ?: return resetLoading("Vault data missing")
            val realHash = real.masterHash ?: return resetLoading("Vault data missing")

            val decoy = decoyCredentials
            val decoySalt = decoy?.authSalt
            val decoyHash = decoy?.masterHash

            try {
                val (realMatch, decoyMatch) =
                    withContext(Dispatchers.IO) {
                        val r = KeyDerivation.verifyMasterPassword(attempt.toCharArray(), realSalt, realHash)
                        val d =
                            if (decoySalt != null && decoyHash != null) {
                                KeyDerivation.verifyMasterPassword(attempt.toCharArray(), decoySalt, decoyHash)
                            } else {
                                // No decoy configured — burn identical PBKDF2 cost
                                // against the real salt so response time is
                                // indistinguishable from the decoy-exists case.
                                KeyDerivation.verifyMasterPassword(attempt.toCharArray(), realSalt, realHash)
                                false
                            }
                        r to d
                    }

                val matchedProfile =
                    when {
                        realMatch -> VaultProfile.REAL
                        decoyMatch -> VaultProfile.DECOY
                        else -> null
                    }

                if (matchedProfile != null) {
                    failedAttempts = 0
                    unlockWithPassword(attempt, matchedProfile)
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                    handleFailedAttempt()
                }
            } catch (_: Exception) {
                resetLoading("An error occurred. Please try again.")
            }
        }

        private suspend fun unlockWithPassword(masterPassword: String, profile: VaultProfile) {
            try {
                val creds = if (profile == VaultProfile.REAL) realCredentials else decoyCredentials
                val encSalt = creds?.encSalt ?: throw IllegalStateException("Salt missing")
                val wrappedVaultKey = creds.wrappedVaultKeyPassword ?: throw IllegalStateException("Vault key missing")

                activateVaultProfile(profile)
                unlockVault(UnlockVault.Params.Password(masterPassword, encSalt, wrappedVaultKey))
                recordAuditEvent(RecordAuditEvent.EventType.UNLOCK_SUCCESS)
                _events.send(LoginEvent.NavigateToHome)
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticating = false,
                        errorMessage = "An error occurred. Please try again.",
                    )
                }
            }
        }

        fun requestBiometricAuth() {
            val state = _uiState.value
            if (state.isAuthenticating || state.isLoading || state.isLockedOut) return

            val ivB64 =
                realCredentials?.biometricIv ?: run {
                    _uiState.update { it.copy(errorMessage = "Biometric data missing") }
                    return
                }

            try {
                val iv = Base64.decode(ivB64, Base64.NO_WRAP)
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
                val wrappedVaultKeyB64 =
                    realCredentials?.wrappedVaultKeyBiometric
                        ?: throw IllegalStateException("Vault key missing")
                val vaultKey =
                    withContext(Dispatchers.IO) {
                        val wrappedBytes = Base64.decode(wrappedVaultKeyB64, Base64.NO_WRAP)
                        val rawKeyBytes = unlockedCipher.doFinal(wrappedBytes)
                        SecretKeySpec(rawKeyBytes, "AES")
                    }
                activateVaultProfile(VaultProfile.REAL)
                unlockVault(UnlockVault.Params.Biometric(vaultKey))
                recordAuditEvent(RecordAuditEvent.EventType.UNLOCK_SUCCESS)
                _events.send(LoginEvent.NavigateToHome)
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticating = false,
                        errorMessage = "An error occurred. Please try again.",
                    )
                }
            }
        }

        fun onBiometricFailure() = _uiState.update { it.copy(isAuthenticating = false) }

        fun onBiometricError(message: String) = _uiState.update { it.copy(isAuthenticating = false, errorMessage = message) }

        private fun handleFailedAttempt() {
            failedAttempts++
            _uiState.update {
                it.copy(
                    errorMessage = "Wrong password",
                    password = "",
                    failedAttempts = failedAttempts,
                    shakeCounter = it.shakeCounter + 1,
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
                    it.copy(
                        isLockedOut = false,
                        lockoutSecondsRemaining = 0,
                        failedAttempts = 0,
                        errorMessage = null,
                    )
                }
            }
        }

        private fun resetLoading(error: String) = _uiState.update { it.copy(isLoading = false, errorMessage = error) }
    }
