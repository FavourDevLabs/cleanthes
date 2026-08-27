package dev.favourdevlabs.cleanthes.feature.detail

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.clickable
import dev.favourdevlabs.cleanthes.data.api.usecase.CheckPasswordBreach
import dev.favourdevlabs.cleanthes.domain.usecase.RequestReAuth
import dev.favourdevlabs.cleanthes.security.BiometricHelper
import kotlinx.coroutines.flow.collectLatest
import androidx.activity.viewModels
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.favourdevlabs.cleanthes.common.ClipboardHelper
import dev.favourdevlabs.cleanthes.ui.base.AuthenticatedActivity
import dev.favourdevlabs.cleanthes.ui.theme.CleanthesTheme
import dev.favourdevlabs.cleanthes.ui.theme.GoldPrimary
import dev.favourdevlabs.cleanthes.ui.theme.OnGold
import dev.favourdevlabs.cleanthes.ui.theme.SurfaceModal
import dev.favourdevlabs.cleanthes.ui.theme.TextPrimary
import dev.favourdevlabs.cleanthes.ui.theme.TextSecondary

@AndroidEntryPoint
class DetailActivity : AuthenticatedActivity() {
    companion object {
        const val EXTRA_ENTRY_ID = "extra_entry_id"
    }

    private val viewModel: DetailViewModel by viewModels()
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

                LaunchedEffect(Unit) {
                    viewModel.challengeEvent.collectLatest { challenge ->
                        when (challenge) {
                            is RequestReAuth.Challenge.Biometric -> {
                                BiometricHelper.authenticate(
                                    activity = this@DetailActivity,
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
                    MasterPasswordDialog(
                        isError = passwordError,
                        onConfirm = { password -> viewModel.submitMasterPassword(password) },
                        onDismiss = {
                            showPasswordDialog = false
                            passwordError = false
                        },
                    )
                }

                DetailScreen(
                    uiState = uiState,
                    onBack = { finish() },
                    onEdit = {
                        startActivity(
                            Intent().apply {
                                setClassName(packageName, "dev.favourdevlabs.cleanthes.feature.addedit.AddEditActivity")
                                putExtra("extra_entry_id", entryId)
                            },
                        )
                    },
                    onTogglePassword = viewModel::onRevealPasswordClicked,
                    onCheckBreach = viewModel::onCheckBreachClicked,
                    onCopy = ::copyToClipboard,
                )
            }
        }
    }

    // Single load point — fires on first resume and after returning from EditActivity
    override fun onResume() {
        super.onResume()
        if (entryId != -1L) viewModel.loadEntry(entryId)
    }

    override fun onPause() {
        super.onPause()
        viewModel.pauseTotpUpdater()
    }

    private fun copyToClipboard(
        label: String,
        value: String,
    ) {
        ClipboardHelper.copyText(this, value)
        Toast.makeText(this, "Guarded, briefly. The clipboard forgets in sixty seconds.", Toast.LENGTH_SHORT).show()
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
private fun DetailScreen(
    uiState: DetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onTogglePassword: () -> Unit,
    onCheckBreach: () -> Unit,
    onCopy: (label: String, value: String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        DetailToolbar(title = uiState.title, onBack = onBack, onEdit = onEdit)

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GoldPrimary)
            }
        } else {
            DetailContent(
                uiState = uiState,
                onTogglePassword = onTogglePassword,
                onCheckBreach = onCheckBreach,
                onCopy = onCopy,
            )
        }
    }
}

@Composable
private fun DetailToolbar(
    title: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go back",
                    tint = TextPrimary,
                )
            }
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 0.1.em),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
            )
            Button(
                onClick = onEdit,
                modifier = Modifier.height(36.dp),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        contentColor = OnGold,
                    ),
            ) {
                Text(
                    text = "EDIT",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                )
            }
        }
        HorizontalDivider(color = SurfaceModal, thickness = 1.dp)
    }
}

@Composable
private fun DetailContent(
    uiState: DetailUiState,
    onTogglePassword: () -> Unit,
    onCheckBreach: () -> Unit,
    onCopy: (label: String, value: String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 40.dp),
    ) {
        // Category
        Text(
            text = uiState.category.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.12.em),
            color = TextSecondary,
        )
        HorizontalDivider(
            color = SurfaceModal,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        )

        // Username
        Spacer(Modifier.height(16.dp))
        DetailRow(label = "USERNAME / EMAIL", value = uiState.username) {
            IconButton(onClick = { onCopy("username", uiState.username) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy username", tint = GoldPrimary)
            }
        }

        // Password
        Spacer(Modifier.height(4.dp))
        Text("PASSWORD", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = uiState.displayPassword,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onTogglePassword) {
                Icon(
                    imageVector =
                        if (uiState.passwordVisible) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                    contentDescription = "Toggle password visibility",
                    tint = TextSecondary,
                )
            }
            IconButton(onClick = { onCopy("password", uiState.password) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy password", tint = GoldPrimary)
            }
        }

        // Breach check (only after password is revealed)
        if (uiState.passwordVisible) {
            Spacer(Modifier.height(8.dp))
            BreachCheckSection(
                isChecking = uiState.isCheckingBreach,
                result = uiState.breachResult,
                onCheck = onCheckBreach,
            )
        }

        // TOTP (conditional)
        if (uiState.hasTOTP) {
            Spacer(Modifier.height(20.dp))
            TotpSection(
                code = uiState.totpCode,
                secondsRemaining = uiState.totpSecondsRemaining,
                period = uiState.totpPeriod,
                onCopy = { onCopy("totp", uiState.totpCode.replace(" ", "")) },
            )
        }

        // Website (conditional)
        if (!uiState.website.isNullOrEmpty()) {
            Spacer(Modifier.height(20.dp))
            DetailRow(
                label = "WEBSITE",
                value = uiState.website,
                valueStyle = MaterialTheme.typography.bodyLarge.copy(color = GoldPrimary),
            )
        }

        // Notes (conditional)
        if (!uiState.notes.isNullOrEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("NOTES", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(
                text = uiState.notes,
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                color = TextPrimary,
            )
        }

        // Priority (conditional)
        if (uiState.isFavorite) {
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Priority entry",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GoldPrimary,
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.12.em),
                color = TextSecondary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = value,
                style = valueStyle,
                color = valueStyle.color.takeOrElse { TextPrimary },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailingContent()
    }
}

@Composable
private fun TotpSection(
    code: String,
    secondsRemaining: Int,
    period: Int,
    onCopy: () -> Unit,
) {
    // Smooth per-second interpolation — the bar glides rather than jumps
    val animatedProgress by animateFloatAsState(
        targetValue = secondsRemaining.toFloat() / period.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
        label = "totp_progress",
    )

    Column {
        Text(
            "AUTHENTICATOR CODE",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.12.em),
            color = TextSecondary,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = code,
                fontSize = 30.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.08.em,
                color = GoldPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy authenticator code",
                    tint = GoldPrimary,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
            color = GoldPrimary,
            trackColor = SurfaceModal,
        )
    }
}

@Composable
private fun BreachCheckSection(
    isChecking: Boolean,
    result: CheckPasswordBreach.Result?,
    onCheck: () -> Unit,
) {
    when {
        isChecking -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = TextSecondary,
                )
                Spacer(Modifier.width(8.dp))
                Text("Checking...", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        result != null -> {
            Text(
                text = if (result.breached) {
                    "⚠ Found in ${result.breachCount} known breaches"
                } else {
                    "✓ Not found in known breaches"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (result.breached) MaterialTheme.colorScheme.error else TextSecondary,
            )
        }
        else -> {
            Text(
                text = "Check for breaches",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = GoldPrimary,
                modifier = Modifier.clickable(onClick = onCheck),
            )
        }
    }
}

@Composable
private fun MasterPasswordDialog(
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
