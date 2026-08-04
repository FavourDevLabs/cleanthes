package dev.favourdevlabs.cleanthes.feature.export

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Color
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
import java.io.OutputStreamWriter

@AndroidEntryPoint
class ExportActivity : AuthenticatedActivity() {
    private val viewModel: ExportViewModel by viewModels()
    private var pendingBlob: String? = null

    private val createFileLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            val blob = pendingBlob
            if (uri != null && blob != null) {
                writeBlobToUri(uri, blob)
                viewModel.onFileSaved()
            }
            pendingBlob = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CleanthesTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is ExportEvent.LaunchSaveFile -> {
                                pendingBlob = event.blob
                                createFileLauncher.launch("cleanthes_export.json")
                            }
                        }
                    }
                }

                LaunchedEffect(uiState.completed) {
                    if (uiState.completed) {
                        Toast
                            .makeText(
                                this@ExportActivity,
                                "The vault has left its walls, but not undefended.",
                                Toast.LENGTH_LONG,
                            ).show()
                        finish()
                    }
                }

                ExportScreen(
                    uiState = uiState,
                    onBack = { finish() },
                    onConfirmExport = viewModel::onExportConfirmed,
                    onDismissError = viewModel::clearError,
                )
            }
        }
    }

    private fun writeBlobToUri(
        uri: Uri,
        blob: String,
    ) {
        contentResolver.openOutputStream(uri)?.use { stream ->
            OutputStreamWriter(stream).use { it.write(blob) }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
private fun ExportScreen(
    uiState: ExportUiState,
    onBack: () -> Unit,
    onConfirmExport: (String) -> Unit,
    onDismissError: () -> Unit,
) {
    var showPasswordDialog by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        ExportToolbar(onBack = onBack)

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.height(40.dp).width(40.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "WHAT LEAVES THE CITADEL",
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.08.em,
                color = TextPrimary,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text =
                    "Every entry — titles, usernames, passwords, notes, and " +
                        "authenticator secrets — will be gathered and sealed under a " +
                        "password you choose now, separate from your master password.",
                fontSize = 15.sp,
                color = TextSecondary,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text =
                    "Guard that export password as you would the vault itself. " +
                        "Lose it, and the file becomes unreadable — by you or anyone else. " +
                        "This is discipline, not inconvenience.",
                fontSize = 15.sp,
                color = TextSecondary,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { showPasswordDialog = true },
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
                    Text("PROCEED WITH EXPORT", letterSpacing = 0.08.em)
                }
            }
        }
    }

    if (showPasswordDialog) {
        ExportPasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { password ->
                showPasswordDialog = false
                onConfirmExport(password)
            },
        )
    }

    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text("Export failed", color = TextPrimary) },
            text = { Text(uiState.errorMessage, color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = onDismissError) { Text("OK", color = GoldPrimary) }
            },
            containerColor = SurfaceElevated,
        )
    }
}

@Composable
private fun ExportToolbar(onBack: () -> Unit) {
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
                text = "Export Citadel",
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
private fun ExportPasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val mismatch = confirmPassword.isNotEmpty() && password != confirmPassword

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose an export password", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "This is separate from your master password. You'll need it to import this file later.",
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
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = mismatch,
                )
                if (mismatch) {
                    Text("Passwords don't match", fontSize = 12.sp, color = Color(0xFFCF6679))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password) },
                enabled = password.isNotEmpty() && password == confirmPassword,
            ) {
                Text("Export", color = GoldPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = SurfaceElevated,
    )
}
