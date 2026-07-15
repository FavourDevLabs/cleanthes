package dev.favourdevlabs.cleanthes.feature.settings

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.autofill.AutofillManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import dev.favourdevlabs.cleanthes.ui.base.AuthenticatedActivity
import dev.favourdevlabs.cleanthes.ui.theme.CleanthesTheme
import dev.favourdevlabs.cleanthes.ui.theme.GoldPrimary
import dev.favourdevlabs.cleanthes.ui.theme.Success
import dev.favourdevlabs.cleanthes.ui.theme.SurfaceElevated
import dev.favourdevlabs.cleanthes.ui.theme.SurfaceModal
import dev.favourdevlabs.cleanthes.ui.theme.TextPrimary
import dev.favourdevlabs.cleanthes.ui.theme.TextSecondary

@AndroidEntryPoint
class SettingsActivity : AuthenticatedActivity() {
    companion object {
        private const val PREFS_NAME = "cleanthes_prefs"
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var autofillManager: AutofillManager

    // Compose-observable state — mutated by onResume
    private var autofillActive by mutableStateOf(false)

    private val versionName: String by lazy {
        try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        autofillManager = getSystemService(AutofillManager::class.java)

        setContent {
            CleanthesTheme {
                SettingsScreen(
                    autofillActive = autofillActive,
                    versionName = versionName,
                    onAutofillClick = {
                        startActivity(
                            Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE).apply {
                                data = Uri.parse("package:$packageName")
                            },
                        )
                    },
                    onAuditLogClick = {
                        startActivity(
                            Intent().apply {
                                setClassName(packageName, "dev.favourdevlabs.cleanthes.feature.auditlog.AuditLogActivity")
                            },
                        )
                    },
                    onExportClick = {
                        startActivity(
                            Intent().apply {
                                setClassName(packageName, "dev.favourdevlabs.cleanthes.feature.export.ExportActivity")
                            },
                        )
                    },
                    onImportClick = {
                        startActivity(
                            Intent().apply {
                                setClassName(packageName, "dev.favourdevlabs.cleanthes.feature.export.ImportActivity")
                            },
                        )
                    },
                    onBack = { finish() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        autofillActive = autofillManager.hasEnabledAutofillServices()
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────
@Composable
private fun SettingsScreen(
    autofillActive: Boolean,
    versionName: String,
    onAutofillClick: () -> Unit,
    onAuditLogClick: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onBack: () -> Unit,
) {
    var showLicensesDialog by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        SettingsToolbar(onBack = onBack)

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 24.dp, bottom = 40.dp),
        ) {
            // ── AUTOFILL ──────────────────────────────────────────────────────
            SectionHeader("AUTOFILL")
            SettingsRow(
                title = "Provider",
                value = if (autofillActive) "Active ✓" else "Enable ›",
                valueColor = if (autofillActive) Success else GoldPrimary,
                showChevron = false,
                onClick = if (!autofillActive) onAutofillClick else null,
            )

            // ── SECURITY ──────────────────────────────────────────────────────
            SectionHeader("SECURITY", topPadding = 28.dp)
            SettingsRow(
                title = "Activity Log",
                value = "",
                showChevron = true,
                onClick = onAuditLogClick,
            )

            SettingsRow(
                title = "Export Vault",
                value = "",
                showChevron = true,
                onClick = onExportClick,
            )
            SettingsRow(
                title = "Import Vault",
                value = "",
                showChevron = true,
                onClick = onImportClick,
            )

            // ── ABOUT ─────────────────────────────────────────────────────────
            SectionHeader("ABOUT", topPadding = 28.dp)
            SettingsRow(
                title = "Version",
                value = versionName,
                showChevron = false,
                onClick = null,
            )
            RowDivider()
            SettingsRow(
                title = "Open-source libraries",
                value = "",
                showChevron = true,
                onClick = { showLicensesDialog = true },
            )
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    if (showLicensesDialog) {
        LicensesDialog(onDismiss = { showLicensesDialog = false })
    }
}

// ── Composable primitives ─────────────────────────────────────────────────────

@Composable
private fun SettingsToolbar(onBack: () -> Unit) {
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
                text = "Settings",
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
private fun SectionHeader(
    title: String,
    topPadding: Dp = 0.dp,
) {
    Text(
        text = title,
        style =
            MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.15.em,
            ),
        color = GoldPrimary,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = topPadding, bottom = 8.dp),
    )
}

@Composable
private fun SettingsRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextSecondary,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = valueColor,
            )
        }
        if (showChevron) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = "›",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color = SurfaceModal,
        modifier = Modifier.padding(start = 16.dp),
    )
}

@Composable
private fun LicensesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Open-source libraries",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
            )
        },
        text = {
            Text(
                text =
                    "ZXing Android Embedded\nApache 2.0 License\n\n" +
                        "AndroidX Biometric\nApache 2.0 License\n\n" +
                        "AndroidX Security Crypto\nApache 2.0 License\n\n" +
                        "Google Material Components\nApache 2.0 License",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = GoldPrimary)
            }
        },
        containerColor = SurfaceElevated,
    )
}
