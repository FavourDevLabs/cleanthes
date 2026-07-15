package dev.favourdevlabs.cleanthes.feature.export

import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.favourdevlabs.cleanthes.ui.base.AuthenticatedActivity
import dev.favourdevlabs.cleanthes.ui.theme.CleanthesTheme
import dev.favourdevlabs.cleanthes.ui.theme.GoldPrimary
import dev.favourdevlabs.cleanthes.ui.theme.OnGold
import dev.favourdevlabs.cleanthes.ui.theme.SurfaceElevated
import dev.favourdevlabs.cleanthes.ui.theme.SurfaceModal
import dev.favourdevlabs.cleanthes.ui.theme.TextPrimary
import dev.favourdevlabs.cleanthes.ui.theme.TextSecondary
import java.io.BufferedReader
import java.io.InputStreamReader

@AndroidEntryPoint
class ImportActivity : AuthenticatedActivity() {
    private val viewModel: ImportViewModel by viewModels()
    private var pendingUri: Uri? = null

    private val openFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            pendingUri = uri
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CleanthesTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                ImportScreen(
                    uiState = uiState,
                    onBack = { finish() },
                    onPickFile = { openFileLauncher.launch(arrayOf("application/json")) },
                    pendingUri = pendingUri,
                    onConfirmImport = { password ->
                        val uri = pendingUri ?: return@ImportScreen
                        val blob = readBlobFromUri(uri)
                        pendingUri = null
                        if (blob != null) {
                            viewModel.onImportConfirmed(blob, password)
                        }
                    },
                    onDismissError = viewModel::clearError,
                    onDone = { finish() },
                )
            }
        }
    }

    private fun readBlobFromUri(uri: Uri): String? =
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            }
        } catch (_: Exception) {
            null
        }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
private fun ImportScreen(
    uiState: ImportUiState,
    onBack: () -> Unit,
    onPickFile: () -> Unit,
    pendingUri: Uri?,
    onConfirmImport: (String) -> Unit,
    onDismissError: () -> Unit,
    onDone: () -> Unit,
) {
    var showPasswordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(pendingUri) {
        if (pendingUri != null) showPasswordDialog = true
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        ImportToolbar(onBack = onBack)

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            if (uiState.result != null) {
                ImportResultView(result = uiState.result, onDone = onDone)
            } else {
                Icon(
                    imageVector = Icons.Default.UploadFile,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.height(40.dp).width(40.dp),
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "RESTORE FROM AN EXPORTED VAULT",
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.06.em,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text =
                        "Select a Cleanthes export file. Entries already present " +
                            "in your vault — matched by title and username — are left untouched.",
                    fontSize = 15.sp,
                    color = TextSecondary,
                    lineHeight = 22.sp,
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onPickFile,
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = OnGold,
                        ),
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = OnGold, modifier = Modifier.height(20.dp).width(20.dp))
                    } else {
                        Text("SELECT EXPORT FILE", letterSpacing = 0.08.em)
                    }
                }
            }
        }
    }

    if (showPasswordDialog) {
        ImportPasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { password ->
                showPasswordDialog = false
                onConfirmImport(password)
            },
        )
    }

    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text("Import failed", color = TextPrimary) },
            text = { Text(uiState.errorMessage, color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = onDismissError) { Text("OK", color = GoldPrimary) }
            },
            containerColor = SurfaceElevated,
        )
    }
}

@Composable
private fun ImportToolbar(onBack: () -> Unit) {
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
                text = "Import Vault",
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.05.em,
                        fontSize = 18.sp,
                    ),
                color = TextPrimary,
            )
        }
        HorizontalDivider(color = SurfaceModal)
    }
}

@Composable
private fun ImportPasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter the export password", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "The password chosen when this file was exported.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Export password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password) },
                enabled = password.isNotEmpty(),
            ) {
                Text("Import", color = GoldPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = SurfaceElevated,
    )
}

@Composable
private fun ImportResultView(
    result: dev.favourdevlabs.cleanthes.domain.usecase.ImportVault.Result,
    onDone: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = GoldPrimary,
            modifier = Modifier.height(48.dp).width(48.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "${result.imported} entrusted, ${result.skipped} already held",
            fontSize = 16.sp,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = OnGold,
                ),
        ) {
            Text("DONE", letterSpacing = 0.08.em)
        }
    }
}
