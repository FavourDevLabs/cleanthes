package dev.favourdevlabs.cleanthes.feature.settings

import android.os.Bundle
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.filled.Key
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
import dev.favourdevlabs.cleanthes.security.BiometricHelper
import dev.favourdevlabs.cleanthes.ui.base.AuthenticatedActivity
import dev.favourdevlabs.cleanthes.ui.theme.CleanthesTheme
import dev.favourdevlabs.cleanthes.ui.theme.GoldPrimary
import dev.favourdevlabs.cleanthes.ui.theme.OnGold
import dev.favourdevlabs.cleanthes.ui.theme.SurfaceElevated
import dev.favourdevlabs.cleanthes.ui.theme.SurfaceModal
import dev.favourdevlabs.cleanthes.ui.theme.TextPrimary
import dev.favourdevlabs.cleanthes.ui.theme.TextSecondary
import javax.crypto.Cipher

@AndroidEntryPoint
class RotateKeyActivity : AuthenticatedActivity() {

    private val viewModel: RotateKeyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CleanthesTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is RotateKeyEvent.TriggerBiometricEnrollment ->
                                triggerBiometricEnrollment(event.cipher)
                        }
                    }
                }

                RotateKeyScreen(
                    uiState = uiState,
                    onBack = { finish() },
                    onConfirmRotation = viewModel::onRotationConfirmed,
                    onDismissError = viewModel::clearError,
                    onDone = { finish() },
                )
            }
        }
    }

    private fun triggerBiometricEnrollment(cipher: Cipher) {
        BiometricHelper.authenticate(
            this,
            cipher,
            object : BiometricHelper.AuthCallback {
                override fun onSuccess(cipher: Cipher) = viewModel.onBiometricEnrollmentSuccess(cipher)

                override fun onFailure() = viewModel.onBiometricEnrollmentFailure()

                override fun onError(errorMessage: String) = viewModel.onBiometricEnrollmentFailure()
            },
        )
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
private fun RotateKeyScreen(
    uiState: RotateKeyUiState,
    onBack: () -> Unit,
    onConfirmRotation: (String) -> Unit,
    onDismissError: () -> Unit,
    onDone: () -> Unit,
) {
    var showPasswordDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        RotateKeyToolbar(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            if (uiState.completed) {
                RotateKeyResultView(biometricReenrollFailed = uiState.biometricReenrollFailed, onDone = onDone)
            } else {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.height(40.dp).width(40.dp),
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "FORGE A NEW KEY",
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.08.em,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "This generates a fresh encryption key and re-seals every " +
                        "entry in your vault under it. The old key becomes worthless " +
                        "the moment this finishes.",
                    fontSize = 15.sp,
                    color = TextSecondary,
                    lineHeight = 22.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Prerequisites: your master password, and a few moments " +
                        "undisturbed — do not close the app while entries are being " +
                        "re-sealed. If biometric unlock is enabled, you'll be asked " +
                        "to re-confirm it once rotation completes.",
                    fontSize = 15.sp,
                    color = TextSecondary,
                    lineHeight = 22.sp,
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { showPasswordDialog = true },
                    enabled = !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = OnGold,
                    ),
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = OnGold, modifier = Modifier.height(20.dp).width(20.dp))
                    } else {
                        Text("BEGIN ROTATION", letterSpacing = 0.08.em)
                    }
                }
            }
        }
    }

    if (showPasswordDialog) {
        RotateKeyPasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { password ->
                showPasswordDialog = false
                onConfirmRotation(password)
            },
        )
    }

    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text("Rotation failed", color = TextPrimary) },
            text = { Text(uiState.errorMessage, color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = onDismissError) { Text("OK", color = GoldPrimary) }
            },
            containerColor = SurfaceElevated,
        )
    }
}

@Composable
private fun RotateKeyToolbar(onBack: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
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
                text = "Rotate Vault Key",
                style = MaterialTheme.typography.titleMedium.copy(
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
private fun RotateKeyPasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm your master password", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Required to authorize the rotation.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Master password") },
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
                Text("Rotate", color = GoldPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = SurfaceElevated,
    )
}

@Composable
private fun RotateKeyResultView(
    biometricReenrollFailed: Boolean,
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
            text = "The citadel stands renewed",
            fontSize = 16.sp,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
        )
        if (biometricReenrollFailed) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Biometric unlock could not be re-confirmed. Re-enable it " +
                    "from Settings when convenient.",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 20.sp,
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GoldPrimary,
                contentColor = OnGold,
            ),
        ) {
            Text("DONE", letterSpacing = 0.08.em)
        }
    }
}
