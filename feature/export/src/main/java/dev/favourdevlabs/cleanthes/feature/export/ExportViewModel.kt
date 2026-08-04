package dev.favourdevlabs.cleanthes.feature.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.favourdevlabs.cleanthes.domain.usecase.ExportCitadel
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
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ExportUiState())
        val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

        private val _events = Channel<ExportEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        fun onExportConfirmed(exportPassword: String) {
            val key =
                sessionManager.getSessionKey() ?: run {
                    _uiState.update { it.copy(errorMessage = "Session expired. Please unlock again.") }
                    return
                }
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            viewModelScope.launch {
                try {
                    val blob = exportCitadel(exportPassword, key)
                    _uiState.update { it.copy(isLoading = false) }
                    _events.send(ExportEvent.LaunchSaveFile(blob))
                } catch (_: Exception) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Export failed. Please try again.") }
                }
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
