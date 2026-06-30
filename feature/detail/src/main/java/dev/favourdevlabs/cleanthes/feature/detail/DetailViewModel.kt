package dev.favourdevlabs.cleanthes.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.favourdevlabs.cleanthes.domain.model.VaultItem
import dev.favourdevlabs.cleanthes.domain.usecase.GetVaultEntry
import dev.favourdevlabs.cleanthes.domain.otp.TOTPGenerator
import dev.favourdevlabs.cleanthes.security.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val shouldFinish: Boolean = false,
    val title: String = "",
    val category: String = "",
    val username: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val website: String? = null,
    val notes: String? = null,
    val isFavorite: Boolean = false,
    val hasTOTP: Boolean = false,
    val totpCode: String = "",
    val totpPeriod: Int = 30,
    val totpSecondsRemaining: Int = 30,
) {
    val displayPassword: String
        get() = if (passwordVisible) password else "••••••••••••"
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val getVaultEntry: GetVaultEntry,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var currentItem: VaultItem? = null
    private var totpJob: Job? = null

    fun loadEntry(entryId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val key = sessionManager.getSessionKey() ?: run {
                    _uiState.update { it.copy(shouldFinish = true) }
                    return@launch
                }
                val item = getVaultEntry(entryId, key)
                if (item == null) {
                    _uiState.update { it.copy(shouldFinish = true) }
                    return@launch
                }
                currentItem = item
                _uiState.update {
                    it.copy(
                        isLoading       = false,
                        title           = item.title,
                        category        = item.category,
                        username        = item.username,
                        password        = item.password,
                        passwordVisible = false,
                        website         = item.website?.takeIf { w -> w.isNotEmpty() },
                        notes           = item.notes?.takeIf { n -> n.isNotEmpty() },
                        isFavorite      = item.isFavorite,
                        hasTOTP         = item.hasTOTP(),
                        totpPeriod      = item.totpPeriod,
                    )
                }
                if (item.hasTOTP()) startTotpUpdater(item) else stopTotpUpdater()
            } catch (_: Exception) {
                _uiState.update { it.copy(shouldFinish = true) }
            }
        }
    }

    fun togglePasswordVisibility() =
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun pauseTotpUpdater() = stopTotpUpdater()

    fun resumeTotpUpdater() = currentItem?.let { if (it.hasTOTP()) startTotpUpdater(it) }

    private fun startTotpUpdater(item: VaultItem) {
        stopTotpUpdater()
        totpJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val secret = item.totpSecret ?: break
                    val code = withContext(Dispatchers.IO) {
                        TOTPGenerator.generate(secret, item.totpDigits, item.totpPeriod, item.totpAlgorithm)
                    }
                    val secsLeft = TOTPGenerator.getSecondsRemaining(item.totpPeriod)
                    val display = if (code.length == 6) {
                        "${code.substring(0, 3)} ${code.substring(3)}"
                    } else code
                    _uiState.update { it.copy(totpCode = display, totpSecondsRemaining = secsLeft) }
                } catch (_: Exception) {
                    _uiState.update { it.copy(totpCode = "ERR") }
                }
                delay(1000)
            }
        }
    }

    private fun stopTotpUpdater() {
        totpJob?.cancel()
        totpJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTotpUpdater()
    }
}

