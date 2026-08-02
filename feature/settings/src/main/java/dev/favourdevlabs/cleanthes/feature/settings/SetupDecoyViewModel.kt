package dev.favourdevlabs.cleanthes.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.favourdevlabs.cleanthes.data.api.usecase.InitialiseCitadel
import dev.favourdevlabs.cleanthes.data.api.usecase.LoadCitadelCredentials
import dev.favourdevlabs.cleanthes.domain.model.CitadelProfile
import dev.favourdevlabs.cleanthes.security.KeyDerivation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal const val MIN_DECOY_PASSWORD_LENGTH = 8

data class SetupDecoyUiState(
    val password: String = "",
    val confirm: String = "",
    val passwordVisible: Boolean = false,
    val confirmVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val completed: Boolean = false,
) {
    val matchState: MatchState get() = when {
        confirm.isEmpty()   -> MatchState.EMPTY
        password == confirm -> MatchState.MATCH
        else                -> MatchState.MISMATCH
    }

    enum class MatchState { EMPTY, MATCH, MISMATCH }
}

@HiltViewModel
class SetupDecoyViewModel @Inject constructor(
    private val initialiseCitadel: InitialiseCitadel,
    private val loadCitadelCredentials: LoadCitadelCredentials,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupDecoyUiState())
    val uiState: StateFlow<SetupDecoyUiState> = _uiState.asStateFlow()

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, errorMessage = null) }

    fun onConfirmChange(value: String) =
        _uiState.update { it.copy(confirm = value, errorMessage = null) }

    fun onPasswordVisibilityToggle() =
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun onConfirmVisibilityToggle() =
        _uiState.update { it.copy(confirmVisible = !it.confirmVisible) }

    fun attemptCreateDecoy() {
        val state = _uiState.value
        val password = state.password
        val confirm = state.confirm

        val error = when {
            password.length < MIN_DECOY_PASSWORD_LENGTH ->
                "Decoy password must be at least $MIN_DECOY_PASSWORD_LENGTH characters"
            !password.any { it.isDigit() } ->
                "Decoy password must contain at least one number"
            !password.any { it in "!@#\$%^&*()_+-=[]{}|;':\",./<>?" } ->
                "Decoy password must contain a special character"
            password != confirm ->
                "Decoy passwords do not match"
            else -> null
        }

        if (error != null) { _uiState.update { it.copy(errorMessage = error) }; return }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch { performCreateDecoy(password) }
    }

    private suspend fun performCreateDecoy(password: String) {
        try {
            val realCreds = loadCitadelCredentials(CitadelProfile.REAL)
            val realSalt = realCreds.authSalt
            val realHash = realCreds.masterHash

            if (realSalt != null && realHash != null) {
                val matchesReal = withContext(Dispatchers.IO) {
                    KeyDerivation.verifyMasterPassword(password.toCharArray(), realSalt, realHash)
                }
                if (matchesReal) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "The decoy must be a stranger to your true gate — choose a different password",
                        )
                    }
                    return
                }
            }

            initialiseCitadel(password, CitadelProfile.DECOY)
            _uiState.update { it.copy(isLoading = false, completed = true) }
        } catch (_: Exception) {
            _uiState.update {
                it.copy(isLoading = false, errorMessage = "The second citadel could not be sealed. Try again.")
            }
        }
    }
}
