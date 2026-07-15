package dev.favourdevlabs.cleanthes.feature.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.favourdevlabs.cleanthes.domain.usecase.ImportVault
import dev.favourdevlabs.cleanthes.security.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImportUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val result: ImportVault.Result? = null,
)

@HiltViewModel
class ImportViewModel
    @Inject
    constructor(
        private val importVault: ImportVault,
        private val sessionManager: SessionManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ImportUiState())
        val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

        fun onImportConfirmed(
            encryptedBlob: String,
            exportPassword: String,
        ) {
            val key =
                sessionManager.getSessionKey() ?: run {
                    _uiState.update { it.copy(errorMessage = "Session expired. Please unlock again.") }
                    return
                }
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            viewModelScope.launch {
                try {
                    val result = importVault(encryptedBlob, exportPassword, key)
                    _uiState.update { it.copy(isLoading = false, result = result) }
                } catch (_: Exception) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Import failed — check your export password and file.")
                    }
                }
            }
        }

        fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    }
