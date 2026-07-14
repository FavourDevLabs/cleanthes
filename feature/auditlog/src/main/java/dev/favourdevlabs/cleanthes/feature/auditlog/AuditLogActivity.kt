package dev.favourdevlabs.cleanthes.feature.auditlog

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.favourdevlabs.cleanthes.domain.model.AuditLogItem
import dev.favourdevlabs.cleanthes.ui.base.AuthenticatedActivity
import dev.favourdevlabs.cleanthes.ui.theme.CleanthesTheme
import dev.favourdevlabs.cleanthes.ui.theme.GoldPrimary
import dev.favourdevlabs.cleanthes.ui.theme.SurfaceModal
import dev.favourdevlabs.cleanthes.ui.theme.TextPrimary
import dev.favourdevlabs.cleanthes.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AuditLogActivity : AuthenticatedActivity() {
    private val viewModel: AuditLogViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CleanthesTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                AuditLogScreen(
                    uiState = uiState,
                    onBack = { finish() },
                )
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
private fun AuditLogScreen(
    uiState: AuditLogUiState,
    onBack: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        AuditLogToolbar(onBack = onBack)

        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GoldPrimary)
                }
            }
            uiState.entries.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AuditLogEmptyState()
                }
            }
            else -> {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(uiState.entries, key = { it.id }) { entry ->
                        AuditLogRow(entry)
                        HorizontalDivider(color = SurfaceModal, modifier = Modifier.padding(start = 56.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogToolbar(onBack: () -> Unit) {
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
                text = "Activity Log",
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
private fun AuditLogEmptyState() {
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
            text = "NO ACTIVITY YET",
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.1.em,
            color = TextPrimary,
        )
        Text(
            text = "Your citadel's history will appear here.",
            fontSize = 14.sp,
            color = TextSecondary.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun AuditLogRow(entry: AuditLogItem) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val (icon, color) = iconFor(entry.eventType)
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.height(22.dp).width(22.dp),
        )
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = labelFor(entry),
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
            )
            Text(
                text = formatTimestamp(entry.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }
}

private fun iconFor(eventType: String): Pair<ImageVector, Color> =
    when (eventType) {
        "UNLOCK_SUCCESS" -> Icons.Default.CheckCircle to GoldPrimary
        "UNLOCK_FAILURE" -> Icons.Default.Error to Color(0xFFCF6679)
        "ENTRY_VIEWED" -> Icons.Default.Visibility to TextSecondary
        "ENTRY_CREATED" -> Icons.Default.CheckCircle to GoldPrimary
        "ENTRY_EDITED" -> Icons.Default.Edit to TextSecondary
        "ENTRY_DELETED" -> Icons.Default.Delete to Color(0xFFCF6679)
        "EXPORT" -> Icons.Default.IosShare to GoldPrimary
        else -> Icons.Default.Lock to TextSecondary
    }

private fun labelFor(entry: AuditLogItem): String =
    when (entry.eventType) {
        "UNLOCK_SUCCESS" -> "Vault unlocked"
        "UNLOCK_FAILURE" -> "Failed unlock attempt"
        "ENTRY_VIEWED" -> "Viewed \u201C${entry.entryTitle ?: "entry"}\u201D"
        "ENTRY_CREATED" -> "Created \u201C${entry.entryTitle ?: "entry"}\u201D"
        "ENTRY_EDITED" -> "Edited \u201C${entry.entryTitle ?: "entry"}\u201D"
        "ENTRY_DELETED" -> "Deleted \u201C${entry.entryTitle ?: "entry"}\u201D"
        "EXPORT" -> "Vault exported"
        else -> entry.eventType
    }

private fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy \u2022 h:mm a", Locale.getDefault())
    return formatter.format(Date(timestamp))
}
