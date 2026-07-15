package dev.favourdevlabs.cleanthes.feature.home

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.favourdevlabs.cleanthes.data.api.usecase.GetFaviconIcon
import dev.favourdevlabs.cleanthes.domain.model.VaultItem
import dev.favourdevlabs.cleanthes.domain.usecase.DeleteVaultEntry
import dev.favourdevlabs.cleanthes.domain.usecase.GetVaultEntries
import dev.favourdevlabs.cleanthes.domain.usecase.RecordAuditEvent
import dev.favourdevlabs.cleanthes.domain.usecase.SaveVaultEntry
import dev.favourdevlabs.cleanthes.security.session.SessionManager
import dev.favourdevlabs.cleanthes.ui.components.decodeFaviconBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val entries: List<VaultItem> = emptyList(),
    val categories: List<String> = emptyList(),
    val entryCount: Int = 0,
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val pendingDeleteIds: Set<Long> = emptySet(),
    val errorMessage: String? = null,
    val icons: Map<Long, ImageBitmap> = emptyMap(),
) {
    val filteredEntries: List<VaultItem>
        get() =
            entries
                .filter { it.id !in pendingDeleteIds }
                .filter { entry ->
                    (
                        selectedCategory == "All" ||
                            entry.category.equals(selectedCategory, ignoreCase = true)
                    ) &&
                        (
                            searchQuery.isEmpty() ||
                                entry.title.lowercase().contains(searchQuery.lowercase()) ||
                                entry.username.lowercase().contains(searchQuery.lowercase())
                        )
                }
}

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val getVaultEntries: GetVaultEntries,
        private val deleteVaultEntry: DeleteVaultEntry,
        private val recordAuditEvent: RecordAuditEvent,
        private val saveVaultEntry: SaveVaultEntry,
        private val sessionManager: SessionManager,
        private val getFaviconIcon: GetFaviconIcon,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState())
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

        fun loadEntries() {
            val key =
                sessionManager.getSessionKey() ?: run {
                    _uiState.update { it.copy(errorMessage = "Session expired. Please unlock again.") }
                    return
                }
            _uiState.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                try {
                    val result = getVaultEntries(key)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            entries = result.entries,
                            categories = result.categories,
                            entryCount = result.entries.size,
                        )
                    }
                    loadIconsFor(result.entries)
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Failed to load entries: ${e.message}")
                    }
                }
            }
        }

        /**
         * Fetches and decodes a favicon for each entry that has a website set, one independent
         * coroutine per entry, updating [HomeUiState.icons] incrementally as each arrives —
         * rather than waiting for every icon before showing any of them.
         */
        private fun loadIconsFor(entries: List<VaultItem>) {
            entries.forEach { entry ->
                val website = entry.website
                android.util.Log.d("FaviconDebug", "entry '${entry.title}' has website='$website'")
                if (website.isNullOrBlank()) return@forEach

                viewModelScope.launch {
                    val result = getFaviconIcon(website)
                    val bytes = result.bytes
                    if (!result.found || bytes == null) return@launch

                    val bitmap =
                        withContext(Dispatchers.Default) {
                            decodeFaviconBitmap(bytes, result.contentType)
                        } ?: return@launch

                    _uiState.update { it.copy(icons = it.icons + (entry.id to bitmap)) }
                }
            }
        }

        fun setSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query.trim()) }

        fun setCategory(category: String) = _uiState.update { it.copy(selectedCategory = category) }

        fun onEntrySwipedToDelete(entryId: Long) = _uiState.update { it.copy(pendingDeleteIds = it.pendingDeleteIds + entryId) }

        fun undoDelete(entryId: Long) = _uiState.update { it.copy(pendingDeleteIds = it.pendingDeleteIds - entryId) }

        fun confirmDelete(entryId: Long) {
            val entryTitle =
                _uiState.value.filteredEntries
                    .find { it.id == entryId }
                    ?.title
            _uiState.update { it.copy(pendingDeleteIds = it.pendingDeleteIds - entryId) }
            viewModelScope.launch {
                try {
                    deleteVaultEntry(entryId)
                    recordAuditEvent(RecordAuditEvent.EventType.ENTRY_DELETED, entryId, entryTitle)
                    loadEntries()
                } catch (_: Exception) {
                    _uiState.update { it.copy(errorMessage = "Failed to delete entry.") }
                }
            }
        }

        fun clearError() = _uiState.update { it.copy(errorMessage = null) }

        fun toggleFavorite(
            item: VaultItem,
            plainPassword: String,
        ) {
            val key = sessionManager.getSessionKey() ?: return
            viewModelScope.launch {
                try {
                    saveVaultEntry(
                        SaveVaultEntry.Params.Edit(
                            item = item.copy(isFavorite = !item.isFavorite),
                            plainPassword = plainPassword,
                            key = key,
                        ),
                    )
                    loadEntries()
                } catch (_: Exception) {
                    _uiState.update { it.copy(errorMessage = "Failed to update entry.") }
                }
            }
        }
    }
