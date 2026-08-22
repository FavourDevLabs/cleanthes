package dev.favourdevlabs.cleanthes.feature.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.favourdevlabs.cleanthes.domain.usecase.ExportCitadel
import dev.favourdevlabs.cleanthes.domain.usecase.RequestReAuth
import dev.favourdevlabs.cleanthes.domain.usecase.VerifyMasterPassword
import dev.favourdevlabs.cleanthes.domain.usecase.RecordAuditEvent
import dev.favourdevlabs.cleanthes.security.session.SessionManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ExportEvent {
    data class LaunchSaveFile(
        val blob: String,
    ) : ExportEvent
}

data class ExportUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val completed: Boolean = false,
)

@HiltViewModel
class ExportViewModel
    @Inject
    constructor(
        private val exportCitadel: ExportCitadel,
        private val recordAuditEvent: RecordAuditEvent,
        private val sessionManager: SessionManager,
        private val requestReAuth: RequestReAuth,
        private val verifyMasterPassword: VerifyMasterPassword,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ExportUiState())
        val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()
        private val _challengeEvent = Channel<RequestReAuth.Challenge>(Channel.BUFFERED)
        val challengeEvent = _challengeEvent.receiveAsFlow()

        private val _events = Channel<ExportEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        private val _masterPasswordResult = Channel<Boolean>(Channel.BUFFERED)
        val masterPasswordResult = _masterPasswordResult.receiveAsFlow()

        private var pendingExportPassword: String? = null

        fun onExportConfirmed(exportPassword: String) {
            pendingExportPassword = exportPassword
            viewModelScope.launch {
                when (val challenge = requestReAuth(RequestReAuth.SensitiveAction.EXPORT)) {
                    RequestReAuth.Challenge.NotRequired -> performExport(exportPassword)
                    else -> _challengeEvent.send(challenge)
                }
            }
        }

        fun onBiometricReAuthSucceeded() {
            val password = pendingExportPassword ?: return
            viewModelScope.launch { performExport(password) }
        }

        fun submitMasterPassword(password: String) {
            viewModelScope.launch {
                val verified = verifyMasterPassword(password)
                if (verified) {
                    pendingExportPassword?.let { performExport(it) }
                }
                _masterPasswordResult.send(verified)
            }
        }

        private suspend fun performExport(exportPassword: String) {
            pendingExportPassword = null
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val blob = sessionManager.withSessionKey { key -> exportCitadel(exportPassword, key) }
                if (blob == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Session expired. Please unlock again.") }
                    return
                }
                _uiState.update { it.copy(isLoading = false) }
                _events.send(ExportEvent.LaunchSaveFile(blob))
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Export failed. Please try again.") }
            }
        }

        fun onFileSaved() {
            viewModelScope.launch {
                recordAuditEvent(RecordAuditEvent.EventType.EXPORT)
                _uiState.update { it.copy(completed = true) }
            }
        }

        fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    }
