package dev.favourdevlabs.cleanthes.feature.auditlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.favourdevlabs.cleanthes.domain.model.AuditLogItem
import dev.favourdevlabs.cleanthes.domain.usecase.GetAuditLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuditLogUiState(
    val isLoading: Boolean = true,
    val entries: List<AuditLogItem> = emptyList(),
)

@HiltViewModel
class AuditLogViewModel
    @Inject
    constructor(
        private val getAuditLog: GetAuditLog,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AuditLogUiState())
        val uiState: StateFlow<AuditLogUiState> = _uiState.asStateFlow()

        init {
            loadLog()
        }

        private fun loadLog() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                try {
                    val entries = getAuditLog()
                    _uiState.update { it.copy(isLoading = false, entries = entries) }
                } catch (_: Exception) {
                    _uiState.update { it.copy(isLoading = false, entries = emptyList()) }
                }
            }
        }
    }
