package dev.favourdevlabs.cleanthes.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.favourdevlabs.cleanthes.data.api.usecase.EnrolBiometric
import dev.favourdevlabs.cleanthes.domain.usecase.RecordAuditEvent
import dev.favourdevlabs.cleanthes.domain.usecase.RotateVaultKey
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

sealed interface RotateKeyEvent {
    data class TriggerBiometricEnrollment(val cipher: Cipher) : RotateKeyEvent
}

data class RotateKeyUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val completed: Boolean = false,
    val biometricReenrollFailed: Boolean = false,
)

@HiltViewModel
class RotateKeyViewModel @Inject constructor(
    private val rotateVaultKey: RotateVaultKey,
    private val enrolBiometric: EnrolBiometric,
    private val recordAuditEvent: RecordAuditEvent,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RotateKeyUiState())
    val uiState: StateFlow<RotateKeyUiState> = _uiState.asStateFlow()

    private val _events = Channel<RotateKeyEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var pendingNewVaultKey: SecretKey? = null

    fun onRotationConfirmed(masterPassword: String) {
        val currentKey = sessionManager.getSessionKey() ?: run {
            _uiState.update { it.copy(errorMessage = "Session expired. Please unlock again.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val result = rotateVaultKey(masterPassword, currentKey)
                sessionManager.setSessionKey(result.newVaultKey)
                recordAuditEvent(RecordAuditEvent.EventType.KEY_ROTATED)

                if (result.biometricWasEnabled) {
                    pendingNewVaultKey = result.newVaultKey
                    val cipher = withContext(Dispatchers.IO) {
                        KeystoreManager.generateBiometricKey()
                        KeystoreManager.getEncryptCipher()
                    }
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(RotateKeyEvent.TriggerBiometricEnrollment(cipher))
                } else {
                    _uiState.update { it.copy(isLoading = false, completed = true) }
                }
            } catch (e: SecurityException) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Incorrect master password.") }
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Rotation failed. Please try again.") }
            }
        }
    }

    fun onBiometricEnrollmentSuccess(cipher: Cipher) {
        val newKey = pendingNewVaultKey ?: return
        viewModelScope.launch {
            try {
                enrolBiometric(newKey, cipher)
                _uiState.update { it.copy(completed = true) }
            } catch (_: Exception) {
                _uiState.update { it.copy(completed = true, biometricReenrollFailed = true) }
            }
        }
    }

    fun onBiometricEnrollmentFailure() {
        _uiState.update { it.copy(completed = true, biometricReenrollFailed = true) }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}
