package dev.favourdevlabs.cleanthes.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.favourdevlabs.cleanthes.domain.model.CitadelHistoryItem
import dev.favourdevlabs.cleanthes.domain.model.CitadelItem
import dev.favourdevlabs.cleanthes.domain.usecase.GetCitadelEntry
import dev.favourdevlabs.cleanthes.domain.usecase.GetCitadelHistory
import dev.favourdevlabs.cleanthes.domain.usecase.RecordAuditEvent
import dev.favourdevlabs.cleanthes.domain.usecase.RequestReAuth
import dev.favourdevlabs.cleanthes.domain.usecase.RestoreCitadelHistory
import dev.favourdevlabs.cleanthes.domain.usecase.VerifyMasterPassword
import dev.favourdevlabs.cleanthes.domain.usecase.diffHistoryFields
import dev.favourdevlabs.cleanthes.security.session.SessionManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryRow(
    val item: CitadelHistoryItem,
    val changedFields: Set<GetCitadelHistory.ChangedField>,
)

data class HistoryUiState(
    val isLoading: Boolean = true,
    val shouldFinish: Boolean = false,
    val entryTitle: String = "",
    val rows: List<HistoryRow> = emptyList(),
    val selectedRow: HistoryRow? = null,
    val selectedFieldsVisible: Boolean = false,
    val currentEntry: CitadelItem? = null,
)

@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        private val getCitadelEntry: GetCitadelEntry,
        private val getCitadelHistory: GetCitadelHistory,
        private val restoreCitadelHistory: RestoreCitadelHistory,
        private val sessionManager: SessionManager,
        private val requestReAuth: RequestReAuth,
        private val verifyMasterPassword: VerifyMasterPassword,
        private val recordAuditEvent: RecordAuditEvent,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(HistoryUiState())
        val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

        private val _challengeEvent = Channel<RequestReAuth.Challenge>(Channel.BUFFERED)
        val challengeEvent: Flow<RequestReAuth.Challenge> = _challengeEvent.receiveAsFlow()

        
        private val _masterPasswordResult = Channel<Boolean>(Channel.BUFFERED)
        val masterPasswordResult: Flow<Boolean> = _masterPasswordResult.receiveAsFlow()

        private var entryId: Long = 0
        private var pendingRowToOpen: HistoryRow? = null

        fun loadHistory(entryId: Long) {
            this.entryId = entryId
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                var loadedTitle = ""
                var loadedEntry: CitadelItem? = null
                val rows = sessionManager.withSessionKey { key ->
                    val currentEntry = getCitadelEntry(entryId, key) ?: return@withSessionKey null
                    loadedTitle = currentEntry.title
                    loadedEntry = currentEntry  
                    val history = getCitadelHistory(entryId, key)
                    buildRows(history, currentEntry)
                }
                if (rows == null) {
                    _uiState.update { it.copy(shouldFinish = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, entryTitle = loadedTitle, currentEntry = loadedEntry, rows = rows) }
                }
            }
        }

        private fun buildRows(
            history: List<CitadelHistoryItem>,
            currentEntry: CitadelItem,
        ): List<HistoryRow> =
            history.mapIndexed { index, item ->
                val newerState = if (index == 0) currentEntry else history[index - 1]
                HistoryRow(item = item, changedFields = diffHistoryFields(item, newerState))
            }

        /** Called when a row is tapped. Gates reveal behind re-auth. */
        fun onRowClicked(row: HistoryRow) {
            pendingRowToOpen = row
            viewModelScope.launch {
                when (val challenge = requestReAuth(RequestReAuth.SensitiveAction.VIEW_HISTORY)) {
                    RequestReAuth.Challenge.NotRequired -> revealPendingRow()
                    else -> _challengeEvent.send(challenge)
                }
            }
        }

                fun onBiometricReAuthSucceeded() {
            viewModelScope.launch { revealPendingRow() }
        }

        fun submitMasterPassword(password: String) {
            viewModelScope.launch {
                val verified = verifyMasterPassword(password)
                if (verified) revealPendingRow()
                _masterPasswordResult.send(verified)
            }
        }

        /**
         * Records the audit event and only THEN reveals the row — the write
         * must complete before the sensitive fields become visible, so a
         * view can never happen without a log entry preceding it.
         */
        private suspend fun revealPendingRow() {
            val row = pendingRowToOpen ?: return
            pendingRowToOpen = null
            recordAuditEvent(RecordAuditEvent.EventType.ENTRY_HISTORY_VIEWED, entryId, row.item.title)
            _uiState.update { it.copy(selectedRow = row, selectedFieldsVisible = true) }
        }

        fun onDismissSelectedRow() {
            _uiState.update { it.copy(selectedRow = null, selectedFieldsVisible = false) }
        }

        /**
         * Restores, then records the audit event, and only THEN signals
         * completion (shouldFinish) — so the screen cannot exit/report
         * success before the restore is both persisted AND logged.
         */
        fun onRestoreConfirmed(historyId: Long) {
            viewModelScope.launch {
                val restoredTitle = _uiState.value.selectedRow?.item?.title
                val success = sessionManager.withSessionKey { key ->
                    restoreCitadelHistory(historyId, key) > 0
                } ?: false
                if (success) {
                    recordAuditEvent(RecordAuditEvent.EventType.ENTRY_RESTORED_FROM_HISTORY, entryId, restoredTitle)
                    _uiState.update { it.copy(shouldFinish = true) }
                }
            }
        }
    }
