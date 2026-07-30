package dev.favourdevlabs.cleanthes.feature.settings

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.favourdevlabs.cleanthes.ui.base.AuthenticatedActivity
import dev.favourdevlabs.cleanthes.ui.components.CleanthesPasswordField
import dev.favourdevlabs.cleanthes.ui.theme.CleanthesTheme
import dev.favourdevlabs.cleanthes.ui.theme.Danger
import dev.favourdevlabs.cleanthes.ui.theme.GoldPrimary
import dev.favourdevlabs.cleanthes.ui.theme.OnGold
import dev.favourdevlabs.cleanthes.ui.theme.SurfaceModal
import dev.favourdevlabs.cleanthes.ui.theme.Success
import dev.favourdevlabs.cleanthes.ui.theme.TextPrimary
import dev.favourdevlabs.cleanthes.ui.theme.TextSecondary

@AndroidEntryPoint
class SetupDecoyActivity : AuthenticatedActivity() {

    private val viewModel: SetupDecoyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CleanthesTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                SetupDecoyScreen(
                    uiState = uiState,
                    onBack = { finish() },
                    onPasswordChange = viewModel::onPasswordChange,
                    onConfirmChange = viewModel::onConfirmChange,
                    onPasswordVisibilityToggle = viewModel::onPasswordVisibilityToggle,
                    onConfirmVisibilityToggle = viewModel::onConfirmVisibilityToggle,
                    onCreate = viewModel::attemptCreateDecoy,
                    onDone = { finish() },
                )
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
private fun SetupDecoyScreen(
    uiState: SetupDecoyUiState,
    onBack: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onConfirmVisibilityToggle: () -> Unit,
    onCreate: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SetupDecoyToolbar(onBack = onBack)

        if (uiState.completed) {
            SetupDecoyResultView(onDone = onDone)
        } else {
            SetupDecoyForm(
                uiState = uiState,
                onPasswordChange = onPasswordChange,
                onConfirmChange = onConfirmChange,
                onPasswordVisibilityToggle = onPasswordVisibilityToggle,
                onConfirmVisibilityToggle = onConfirmVisibilityToggle,
                onCreate = onCreate,
            )
        }
    }
}

@Composable
private fun SetupDecoyToolbar(onBack: () -> Unit) {
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
                text = "Set Up Decoy Vault",
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
private fun SetupDecoyForm(
    uiState: SetupDecoyUiState,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onConfirmVisibilityToggle: () -> Unit,
    onCreate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = GoldPrimary,
            modifier = Modifier.height(40.dp).width(40.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "A SECOND CITADEL",
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.08.em,
            color = TextPrimary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Fortune may compel you to open the gate against your will. " +
                "Give it a second face — one that yields nothing.",
            fontSize = 15.sp,
            color = TextSecondary,
            lineHeight = 22.sp,
        )
        Spacer(Modifier.height(28.dp))

        CleanthesPasswordField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = "The false gate's word",
            visible = uiState.passwordVisible,
            onVisibilityToggle = onPasswordVisibilityToggle,
            imeAction = ImeAction.Next,
        )
        Spacer(Modifier.height(12.dp))
        CleanthesPasswordField(
            value = uiState.confirm,
            onValueChange = onConfirmChange,
            label = "Speak it again",
            visible = uiState.confirmVisible,
            onVisibilityToggle = onConfirmVisibilityToggle,
            imeAction = ImeAction.Done,
            onImeAction = onCreate,
        )
        AnimatedVisibility(visible = uiState.matchState != SetupDecoyUiState.MatchState.EMPTY) {
            val (text, color) = when (uiState.matchState) {
                SetupDecoyUiState.MatchState.MATCH -> "✓ Passwords match" to Success
                SetupDecoyUiState.MatchState.MISMATCH -> "✗ Passwords do not match" to Danger
                SetupDecoyUiState.MatchState.EMPTY -> "" to TextSecondary
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        AnimatedVisibility(visible = uiState.errorMessage != null) {
            uiState.errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onCreate,
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
                Text("SEAL THE SECOND GATE", letterSpacing = 0.08.em)
            }
        }
    }
}

@Composable
private fun SetupDecoyResultView(onDone: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 60.dp, start = 24.dp, end = 24.dp),
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = GoldPrimary,
            modifier = Modifier.height(48.dp).width(48.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "The second gate stands sealed",
            fontSize = 16.sp,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace,
        )
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
