package dev.favourdevlabs.cleanthes.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry
import dev.favourdevlabs.cleanthes.data.repository.VaultRepository
import dev.favourdevlabs.cleanthes.ui.auth.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val isLoading: Boolean = false,
    val entries: List<VaultEntry> = emptyList(),
    val categories: List<String> = emptyList(),
    val entryCount: Int = 0,
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val pendingDeleteIds: Set<Long> = emptySet(),
    val errorMessage: String? = null,
) {
    // Derived — composable reads this, never the raw entries list
    val filteredEntries: List<VaultEntry>
        get() = entries
            .filter { it.id !in pendingDeleteIds }
            .filter { entry ->
                (selectedCategory == "All" ||
                        entry.category.equals(selectedCategory, ignoreCase = true)) &&
                (searchQuery.isEmpty() ||
                        entry.title.lowercase().contains(searchQuery.lowercase()) ||
                        (entry.username?.lowercase()?.contains(searchQuery.lowercase()) == true))
            }
}

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = VaultRepository.getInstance(app)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadEntries() {
        val key = SessionManager.getSessionKey() ?: run {
            _uiState.update { it.copy(errorMessage = "Session expired. Please unlock again.") }
            return
        }
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val (all, cats) = withContext(Dispatchers.IO) {
                    Pair(repository.getAllEntries(key), repository.getAllCategories())
                }
                _uiState.update {
                    it.copy(
                        isLoading  = false,
                        entries    = all,
                        categories = cats,
                        entryCount = all.size,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Failed to load entries: ${e.message}")
                }
            }
        }
    }

    fun setSearchQuery(query: String) =
        _uiState.update { it.copy(searchQuery = query.trim()) }

    fun setCategory(category: String) =
        _uiState.update { it.copy(selectedCategory = category) }

    // Called immediately on swipe — removes item from visible list before snackbar shows
    fun onEntrySwipedToDelete(entryId: Long) =
        _uiState.update { it.copy(pendingDeleteIds = it.pendingDeleteIds + entryId) }

    // Called on UNDO — item re-appears, no DB operation
    fun undoDelete(entryId: Long) =
        _uiState.update { it.copy(pendingDeleteIds = it.pendingDeleteIds - entryId) }

    // Called on snackbar dismissed — commits DB delete
    fun confirmDelete(entryId: Long) {
        _uiState.update { it.copy(pendingDeleteIds = it.pendingDeleteIds - entryId) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repository.deleteEntry(entryId) }
                loadEntries()
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete entry.") }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun toggleFavorite(entry: VaultEntry, plainPassword: String) {
        val key = SessionManager.getSessionKey() ?: return
        entry.isFavorite = !entry.isFavorite
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repository.updateEntry(entry, plainPassword, key) }
                loadEntries()
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update entry.") }
            }
        }
    }
}
