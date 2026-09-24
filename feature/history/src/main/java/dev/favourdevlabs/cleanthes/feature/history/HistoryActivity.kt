package dev.favourdevlabs.cleanthes.feature.history

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.favourdevlabs.cleanthes.domain.usecase.GetCitadelHistory
import dev.favourdevlabs.cleanthes.domain.usecase.RequestReAuth
import dev.favourdevlabs.cleanthes.domain.usecase.diffHistoryFields
import dev.favourdevlabs.cleanthes.security.BiometricHelper
import dev.favourdevlabs.cleanthes.ui.base.AuthenticatedActivity
import dev.favourdevlabs.cleanthes.ui.theme.CleanthesTheme
import dev.favourdevlabs.cleanthes.ui.theme.GoldPrimary
import dev.favourdevlabs.cleanthes.ui.theme.SurfaceModal
import dev.favourdevlabs.cleanthes.ui.theme.TextPrimary
import dev.favourdevlabs.cleanthes.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class HistoryActivity : AuthenticatedActivity() {
    companion object {
        const val EXTRA_ENTRY_ID = "extra_entry_id"
    }

    private val viewModel: HistoryViewModel by viewModels()
    private var entryId = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        entryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1L)

        setContent {
            CleanthesTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(uiState.shouldFinish) {
                    if (uiState.shouldFinish) finish()
                }

                var showPasswordDialog by remember { mutableStateOf(false) }
                var passwordError by remember { mutableStateOf(false) }
                var showRestoreConfirm by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    viewModel.challengeEvent.collectLatest { challenge ->
                        when (challenge) {
                            is RequestReAuth.Challenge.Biometric -> {
                                BiometricHelper.authenticate(
                                    activity = this@HistoryActivity,
                                    cipher = challenge.cipher,
                                    callback =
                                        object : BiometricHelper.AuthCallback {
                                            override fun onSuccess(cipher: javax.crypto.Cipher) =
                                                viewModel.onBiometricReAuthSucceeded()

                                            override fun onFailure() {}

                                            override fun onError(errorMessage: String) {}
                                        },
                                )
                            }
                            RequestReAuth.Challenge.MasterPassword -> showPasswordDialog = true
                            RequestReAuth.Challenge.NotRequired -> {}
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.masterPasswordResult.collectLatest { verified ->
                        if (verified) {
                            showPasswordDialog = false
                            passwordError = false
                        } else {
                            passwordError = true
                        }
                    }
                }

                if (showPasswordDialog) {
                    HistoryMasterPasswordDialog(
                        isError = passwordError,
                        onConfirm = { password -> viewModel.submitMasterPassword(password) },
                        onDismiss = {
                            showPasswordDialog = false
                            passwordError = false
                        },
                    )
                }

                uiState.selectedRow?.let { row ->
                    HistorySnapshotDialog(
                        row = row,
                        onDismiss = viewModel::onDismissSelectedRow,
                        onRestoreClicked = { showRestoreConfirm = true },
                    )
                }

                if (showRestoreConfirm) {
                    val row = uiState.selectedRow
                    val current = uiState.currentEntry
                    if (row != null && current != null) {
                        HistoryRestoreConfirmDialog(
                            changedFields = diffHistoryFields(row.item, current),
                            onConfirm = {
                                showRestoreConfirm = false
                                viewModel.onRestoreConfirmed(row.item.id)
                            },
                            onDismiss = { showRestoreConfirm = false },
                        )
                    }
                }

                HistoryScreen(
                    uiState = uiState,
                    onBack = { finish() },
                    onRowClicked = viewModel::onRowClicked,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (entryId != -1L) viewModel.loadHistory(entryId)
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
private fun HistoryScreen(
    uiState: HistoryUiState,
    onBack: () -> Unit,
    onRowClicked: (HistoryRow) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        HistoryToolbar(title = uiState.entryTitle, onBack = onBack)

        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GoldPrimary)
                }
            }
            uiState.rows.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    HistoryEmptyState()
                }
            }
            else -> {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(uiState.rows, key = { it.item.id }) { row ->
                        HistoryRowItem(row = row, onClick = { onRowClicked(row) })
                        HorizontalDivider(color = SurfaceModal, modifier = Modifier.padding(start = 56.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryToolbar(
    title: String,
    onBack: () -> Unit,
) {
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go back",
                    tint = TextPrimary,
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = title.ifEmpty { "VERSION HISTORY" }.uppercase(),
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.05.em,
                        fontSize = 18.sp,
                    ),
                color = TextPrimary,
                maxLines = 1,
            )
        }
        HorizontalDivider(color = SurfaceModal)
    }
}

@Composable
private fun HistoryEmptyState() {
    Column(
        modifier = Modifier.padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = GoldPrimary.copy(alpha = 0.2f),
            modifier = Modifier.height(56.dp).width(56.dp),
        )
        Text(
            text = "NO HISTORY YET",
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.1.em,
            color = TextPrimary,
        )
        Text(
            text = "Edits to this entry will appear here.",
            fontSize = 14.sp,
            color = TextSecondary.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun HistoryRowItem(
    row: HistoryRow,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .let { it },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.height(22.dp).width(22.dp),
        )
        Spacer(Modifier.width(18.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(vertical = 0.dp),
        ) {
            Text(
                text = changedFieldsLabel(row.changedFields),
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
            )
            Text(
                text = formatTimestamp(row.item.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
        TextButton(onClick = onClick) {
            Text("VIEW", color = GoldPrimary)
        }
    }
}

private fun changedFieldsLabel(fields: Set<GetCitadelHistory.ChangedField>): String {
    if (fields.isEmpty()) return "No changes recorded"
    return fields.joinToString(", ") { labelFor(it) } + " changed"
}

private fun labelFor(field: GetCitadelHistory.ChangedField): String =
    when (field) {
        GetCitadelHistory.ChangedField.TITLE -> "Title"
        GetCitadelHistory.ChangedField.USERNAME -> "Username"
        GetCitadelHistory.ChangedField.PASSWORD -> "Password"
        GetCitadelHistory.ChangedField.WEBSITE -> "Website"
        GetCitadelHistory.ChangedField.NOTES -> "Notes"
        GetCitadelHistory.ChangedField.TOTP -> "TOTP"
    }

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy \u2022 h:mm a", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

// ── Dialogs ────────────────────────────────────────────────────────────────────

@Composable
private fun HistorySnapshotDialog(
    row: HistoryRow,
    onDismiss: () -> Unit,
    onRestoreClicked: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Snapshot from ${formatTimestamp(row.item.timestamp)}") },
        text = {
            Column {
                SnapshotField("Title", row.item.title)
                SnapshotField("Username", row.item.username)
                SnapshotField("Password", row.item.password)
                row.item.website?.takeIf { it.isNotEmpty() }?.let { SnapshotField("Website", it) }
                row.item.notes?.takeIf { it.isNotEmpty() }?.let { SnapshotField("Notes", it) }
                row.item.totpSecret?.takeIf { it.isNotEmpty() }?.let { SnapshotField("TOTP secret", it) }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onRestoreClicked) { Text("Restore this version") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun SnapshotField(
    label: String,
    value: String,
) {
    Column(Modifier.padding(bottom = 10.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1.em),
            color = TextSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
        )
    }
}

@Composable
private fun HistoryRestoreConfirmDialog(
    changedFields: Set<GetCitadelHistory.ChangedField>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restore this version?") },
        text = {
            val fieldList = changedFields.joinToString(", ") { labelFor(it) }
            Text(
                if (changedFields.isEmpty()) {
                    "This will restore this snapshot. No fields currently differ from the live entry."
                } else {
                    "This will replace your current $fieldList with this version."
                },
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
            ) { Text("Restore") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun HistoryMasterPasswordDialog(
    isError: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm master password") },
        text = {
            Column {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Master password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = isError,
                    singleLine = true,
                )
                if (isError) {
                    Text(
                        text = "Incorrect password",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password) }) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
