package dev.favourdevlabs.cleanthes.feature.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle


import dagger.hilt.android.AndroidEntryPoint
import dev.favourdevlabs.cleanthes.security.BiometricHelper
import dev.favourdevlabs.cleanthes.ui.base.SecureActivity
import dev.favourdevlabs.cleanthes.ui.components.CleanthesPasswordField
import dev.favourdevlabs.cleanthes.ui.components.PasswordStrengthBar
import dev.favourdevlabs.cleanthes.ui.theme.CleanthesTheme
import dev.favourdevlabs.cleanthes.ui.theme.Danger
import dev.favourdevlabs.cleanthes.ui.theme.GoldPrimary
import dev.favourdevlabs.cleanthes.ui.theme.OnGold
import dev.favourdevlabs.cleanthes.ui.theme.StrengthFair
import dev.favourdevlabs.cleanthes.ui.theme.StrengthStrong
import dev.favourdevlabs.cleanthes.ui.theme.StrengthVeryStrong
import dev.favourdevlabs.cleanthes.ui.theme.StrengthVeryWeak
import dev.favourdevlabs.cleanthes.ui.theme.StrengthWeak
import dev.favourdevlabs.cleanthes.ui.theme.Success
import dev.favourdevlabs.cleanthes.ui.theme.SurfaceModal
import dev.favourdevlabs.cleanthes.ui.theme.TextSecondary
import dev.favourdevlabs.cleanthes.ui.theme.TextMuted
import dev.favourdevlabs.cleanthes.ui.theme.TextPrimary
import dev.favourdevlabs.cleanthes.ui.theme.Warning
import kotlinx.coroutines.launch
import javax.crypto.Cipher

@AndroidEntryPoint
class SetupActivity : SecureActivity() {
    private val viewModel: SetupViewModel by viewModels()
    private val splashHandler = Handler(Looper.getMainLooper())

    // mutableStateOf at Activity scope — Compose observes these directly
    private var splashDone by mutableStateOf(false)
    private var showContent by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !splashDone }
        super.onCreate(savedInstanceState)

        // Navigation events are Activity concerns — handle them here, not in a composable
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navEvents.collect { event ->
                    when (event) {
                        SetupNavEvent.NavigateToHome -> {
                            startActivity(
                                Intent().apply {
                                    setClassName(packageName, "dev.favourdevlabs.cleanthes.feature.home.HomeActivity")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                },
                            )
                            finish()
                        }
                        SetupNavEvent.NavigateToLogin -> {
                            startActivity(
                                Intent(this@SetupActivity, LoginActivity::class.java)
                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
                            )
                            finish()
                        }
                        is SetupNavEvent.TriggerBiometricEnrollment -> triggerBiometricEnrollment(event.cipher)
                    }
                }
            }
        }

        setContent {
            CleanthesTheme {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    SetupScreen(viewModel = viewModel)
                }
            }
        }

        splashHandler.postDelayed(::onSplashComplete, 2000)
        viewModel.checkVaultExists()
    }

    override fun onDestroy() {
        super.onDestroy()
        splashHandler.removeCallbacksAndMessages(null)
    }

    private fun onSplashComplete() {
        showContent = true
        splashDone = true
    }

    private fun triggerBiometricEnrollment(cipher: Cipher) {
        BiometricHelper.authenticate(
            this,
            cipher,
            object : BiometricHelper.AuthCallback {
                override fun onSuccess(cipher: Cipher) = viewModel.onBiometricEnrollmentSuccess(cipher)

                override fun onFailure() = viewModel.onBiometricEnrollmentFailure()

                override fun onError(errorMessage: String) = viewModel.onBiometricEnrollmentError(errorMessage)
            },
        )
    }

}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
private fun SetupScreen(viewModel: SetupViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    if (uiState.showSecondGate) {
        SecondGateScreen(
            isEnrolling = uiState.isEnrollingBiometric,
            errorMessage = uiState.errorMessage,
            onEnable = viewModel::enableBiometricEnrollment,
            onSkip = viewModel::skipBiometricEnrollment,
        )
        return
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 72.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Your Inner Citadel",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "memento mori",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                letterSpacing = 0.2.em,
            )
        }

        // ── Warning card ──────────────────────────────────────────────────────
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(width = 1.dp, color = Warning, shape = RoundedCornerShape(8.dp))
                    .background(Warning.copy(alpha = 0.06f))
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "NO RECOVERY. NO REGRETS.",
                style = MaterialTheme.typography.labelMedium,
                color = Warning,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text =
                    "Lose this password and your data returns to the void — permanently.\n\n" +
                        "This is not a flaw. It is a feature. It protects you from everyone, including us.\n\n" +
                        "No backdoors. No support ticket. No second chance. Write it down. Lock it away.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            HorizontalDivider(color = SurfaceModal, thickness = 1.dp)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onAcknowledgeToggle(!uiState.acknowledged) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Checkbox(
                    checked = uiState.acknowledged,
                    onCheckedChange = viewModel::onAcknowledgeToggle,
                    colors =
                        CheckboxDefaults.colors(
                            checkedColor = Warning,
                            uncheckedColor = TextMuted,
                            checkmarkColor = OnGold,
                        ),
                )
                Text(
                    text = "I understand. There is no recovery.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }

        // ── Password + strength ───────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CleanthesPasswordField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Guard the gate",
                visible = uiState.passwordVisible,
                onVisibilityToggle = viewModel::onPasswordVisibilityToggle,
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
            )
            PasswordStrengthBar(score = uiState.strengthScore)
            AnimatedVisibility(visible = uiState.password.isNotEmpty()) {
                Text(
                    text = strengthLabel(uiState.strengthScore),
                    style = MaterialTheme.typography.labelSmall,
                    color = strengthColor(uiState.strengthScore),
                )
            }
        }

        // ── Confirm + match indicator ─────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CleanthesPasswordField(
                value = uiState.confirm,
                onValueChange = viewModel::onConfirmChange,
                label = "Speak it again. Without doubt.",
                visible = uiState.confirmVisible,
                onVisibilityToggle = viewModel::onConfirmVisibilityToggle,
                imeAction = ImeAction.Done,
                onImeAction = {
                    focusManager.clearFocus()
                    viewModel.attemptSetup()
                },
            )
            AnimatedVisibility(visible = uiState.matchState != SetupUiState.MatchState.EMPTY) {
                val (text, color) =
                    when (uiState.matchState) {
                        SetupUiState.MatchState.MATCH -> "✓ Passwords match" to Success
                        SetupUiState.MatchState.MISMATCH -> "✗ Passwords do not match" to Danger
                        SetupUiState.MatchState.EMPTY -> "" to Color.Transparent
                    }
                Text(text = text, style = MaterialTheme.typography.bodyMedium, color = color)
            }
        }

        // ── Inline error ──────────────────────────────────────────────────────
        AnimatedVisibility(visible = uiState.errorMessage != null) {
            uiState.errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Create button ─────────────────────────────────────────────────────
        Button(
            onClick = viewModel::attemptSetup,
            enabled = uiState.canCreate,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            shape = RoundedCornerShape(8.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = OnGold,
                    disabledContainerColor = GoldPrimary.copy(alpha = 0.3f),
                    disabledContentColor = OnGold.copy(alpha = 0.3f),
                ),
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = OnGold,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = "FORTIFY",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

private fun strengthLabel(score: Int): String =
    when (score) {
        1 -> "Very Weak"
        2 -> "Weak"
        3 -> "Fair"
        4 -> "Strong"
        5 -> "Very Strong"
        else -> ""
    }

private fun strengthColor(score: Int): Color =
    when (score) {
        1 -> StrengthVeryWeak
        2 -> StrengthWeak
        3 -> StrengthFair
        4 -> StrengthStrong
        5 -> StrengthVeryStrong
        else -> Color.Transparent
    }

@Composable
private fun SecondGateScreen(
    isEnrolling: Boolean,
    errorMessage: String?,
    onEnable: () -> Unit,
    onSkip: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 28.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "A Second Gate",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "The fingerprint is swift, but the password is sovereign.\nAdd a second gate to your citadel?",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )

            AnimatedVisibility(visible = errorMessage != null) {
                errorMessage?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Danger,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onEnable,
                enabled = !isEnrolling,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = GoldPrimary,
                        disabledContainerColor = GoldPrimary.copy(alpha = 0.3f),
                        disabledContentColor = OnGold.copy(alpha = 0.3f),
                    ),
            ) {
                if (isEnrolling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = OnGold,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(text = "ENABLE BIOMETRIC UNLOCK", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(12.dp))

            TextButton(
                onClick = onSkip,
                enabled = !isEnrolling,
            ) {
                Text(
                    text = "Not Now",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
    }
}
