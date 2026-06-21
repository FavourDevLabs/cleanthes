package dev.favourdevlabs.cleanthes.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.favourdevlabs.cleanthes.R
import dev.favourdevlabs.cleanthes.data.entities.VaultEntry
import dev.favourdevlabs.cleanthes.ui.addedit.AddEditActivity
import dev.favourdevlabs.cleanthes.ui.base.AuthenticatedActivity
import dev.favourdevlabs.cleanthes.ui.components.cleanthesOutlinedTextFieldColors
import dev.favourdevlabs.cleanthes.ui.detail.DetailActivity
import dev.favourdevlabs.cleanthes.ui.settings.SettingsActivity
import dev.favourdevlabs.cleanthes.ui.theme.CleanthesTheme
import dev.favourdevlabs.cleanthes.ui.theme.GoldBright
import dev.favourdevlabs.cleanthes.ui.theme.GoldPrimary
import dev.favourdevlabs.cleanthes.ui.theme.OnGold
import dev.favourdevlabs.cleanthes.ui.theme.SurfaceElevated
import dev.favourdevlabs.cleanthes.ui.theme.SurfaceModal
import dev.favourdevlabs.cleanthes.ui.theme.TextMuted
import dev.favourdevlabs.cleanthes.ui.theme.TextPrimary
import dev.favourdevlabs.cleanthes.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeActivity : AuthenticatedActivity() {
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CleanthesTheme {
                HomeScreen(
                    viewModel = viewModel,
                    onEntryClick = { entry ->
                        startActivity(
                            Intent(this, DetailActivity::class.java).apply {
                                putExtra(DetailActivity.EXTRA_ENTRY_ID, entry.id)
                            },
                        )
                    },
                    onCopyPassword = ::copyToClipboard,
                    onAddNew = { startActivity(Intent(this, AddEditActivity::class.java)) },
                    onSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                    onLock = {
                        sessionManager.clearSession()
                        redirectToLogin()
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isFinishing) viewModel.loadEntries()
    }

    private fun copyToClipboard(password: String) {
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("password", password))
        Toast.makeText(this, "Password copied to clipboard", Toast.LENGTH_SHORT).show()
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    viewModel: HomeViewModel,
    onEntryClick: (VaultEntry) -> Unit,
    onCopyPassword: (String) -> Unit,
    onAddNew: () -> Unit,
    onSettings: () -> Unit,
    onLock: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var searchVisible by remember { mutableStateOf(false) }

    // Error → snackbar (one-shot)
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SurfaceElevated,
                    contentColor = TextPrimary,
                    actionColor = GoldPrimary,
                )
            }
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            // ── Toolbar ───────────────────────────────────────────────────────
            HomeToolbar(
                entryCount = uiState.entryCount,
                searchVisible = searchVisible,
                onSearchToggle = {
                    searchVisible = !searchVisible
                    if (!searchVisible) viewModel.setSearchQuery("")
                },
                onSettings = onSettings,
                onLock = onLock,
            )

            // ── Search bar (animated) ─────────────────────────────────────────
            AnimatedVisibility(
                visible = searchVisible,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = {
                        Text(
                            stringResource(R.string.home_search_hint),
                            color = TextMuted,
                        )
                    },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, "Clear search", tint = TextMuted)
                            }
                        }
                    },
                    colors = cleanthesOutlinedTextFieldColors(),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            // ── Category chips ────────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                val allCategories = listOf("All") + uiState.categories
                items(allCategories) { category ->
                    val selected = uiState.selectedCategory == category
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setCategory(category) },
                        label = { Text(category) },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                containerColor = SurfaceElevated,
                                labelColor = TextSecondary,
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = OnGold,
                            ),
                        border =
                            FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = SurfaceModal,
                                selectedBorderColor = Color.Transparent,
                            ),
                    )
                }
            }

            HorizontalDivider(color = SurfaceModal)

            // ── List / empty / loading ────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            color = GoldPrimary,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    uiState.filteredEntries.isEmpty() -> {
                        EmptyState(
                            onAddNew = onAddNew,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    else -> {
                        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                            items(
                                items = uiState.filteredEntries,
                                key = { it.id },
                            ) { entry ->
                                val dismissState =
                                    rememberSwipeToDismissBoxState(
                                        confirmValueChange = { value ->
                                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                                // Screen-level scope — survives item leaving composition
                                                scope.launch {
                                                    viewModel.onEntrySwipedToDelete(entry.id)
                                                    val result =
                                                        snackbarHostState.showSnackbar(
                                                            message = "\"${entry.title}\" deleted",
                                                            actionLabel = "UNDO",
                                                            duration = SnackbarDuration.Long,
                                                        )
                                                    when (result) {
                                                        SnackbarResult.ActionPerformed ->
                                                            viewModel.undoDelete(entry.id)
                                                        SnackbarResult.Dismissed ->
                                                            viewModel.confirmDelete(entry.id)
                                                    }
                                                }
                                                true
                                            } else {
                                                false
                                            }
                                        },
                                    )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = false,
                                    enableDismissFromEndToStart = true,
                                    backgroundContent = { SwipeDeleteBackground(dismissState) },
                                ) {
                                    EntryCard(
                                        entry = entry,
                                        onEntryClick = onEntryClick,
                                        onCopyClick = onCopyPassword,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Fixed bottom Entrust button (only when list has entries) ──────
            if (uiState.filteredEntries.isNotEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ExtendedFloatingActionButton(
                        onClick = onAddNew,
                        containerColor = GoldPrimary,
                        contentColor = OnGold,
                        elevation = FloatingActionButtonDefaults.elevation(4.dp),
                        icon = {
                            Icon(Icons.Default.Key, contentDescription = null)
                        },
                        text = {
                            Text(
                                text = "Entrust",
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.05.em,
                            )
                        },
                    )
                }
            }
        }
    }
}

// ── Composable primitives ─────────────────────────────────────────────────────

@Composable
private fun HomeToolbar(
    entryCount: Int,
    searchVisible: Boolean,
    onSearchToggle: () -> Unit,
    onSettings: () -> Unit,
    onLock: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name).uppercase(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.1.em,
                color = TextPrimary,
            )
            if (entryCount > 0) {
                Text(
                    text = "$entryCount ${if (entryCount == 1) "entry" else "entries"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = 0.6f),
                )
            }
        }
        IconButton(onClick = onSearchToggle) {
            Icon(
                imageVector = if (searchVisible) Icons.Default.Close else Icons.Default.Search,
                contentDescription = if (searchVisible) "Close search" else "Search",
                tint = GoldPrimary,
            )
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = GoldPrimary)
        }
        IconButton(onClick = onLock) {
            Icon(Icons.Default.Lock, contentDescription = "Lock vault", tint = GoldPrimary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeDeleteBackground(state: SwipeToDismissBoxState) {
    val isActive = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(if (isActive) Color(0xFFB71C1C) else Color.Transparent)
                .padding(end = 20.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        if (isActive) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun EntryCard(
    entry: VaultEntry,
    onEntryClick: (VaultEntry) -> Unit,
    onCopyClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable { onEntryClick(entry) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        border = BorderStroke(1.dp, SurfaceModal),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar
            Box(modifier = Modifier.size(44.dp)) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(avatarColor(entry)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text =
                            entry.title
                                .firstOrNull()
                                ?.uppercaseChar()
                                ?.toString() ?: "?",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                // TOTP indicator dot
                if (entry.hasTOTP()) {
                    Box(
                        modifier =
                            Modifier
                                .size(10.dp)
                                .align(Alignment.TopEnd)
                                .clip(CircleShape)
                                .background(GoldBright)
                                .then(
                                    Modifier.clip(CircleShape),
                                ),
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = entry.username ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(onClick = { onCopyClick(entry.encryptedPassword ?: "") }) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy password",
                    tint = GoldPrimary,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    onAddNew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = GoldPrimary.copy(alpha = 0.2f),
            modifier = Modifier.size(72.dp),
        )
        Text(
            text = "BEGIN WITH ORDER",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.12.em,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier =
                Modifier
                    .width(28.dp)
                    .height(2.dp)
                    .background(GoldPrimary),
        )
        Text(
            text = "What you guard\nreveals what you value.",
            fontSize = 15.sp,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.01.em,
            color = TextSecondary.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            lineHeight = 23.sp,
        )
        Spacer(Modifier.height(8.dp))
        ExtendedFloatingActionButton(
            onClick = onAddNew,
            containerColor = GoldPrimary,
            contentColor = OnGold,
            elevation = FloatingActionButtonDefaults.elevation(4.dp),
            icon = {
                Icon(Icons.Default.Key, contentDescription = null)
            },
            text = {
                Text(
                    text = "Entrust",
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.05.em,
                )
            },
        )
    }
}

// Category avatar color — mirrors VaultEntryViewHolder.categoryColor()
private fun avatarColor(entry: VaultEntry): Color {
    if (entry.isFavorite) return GoldBright
    val palette =
        listOf(
            Color(0xFF2E86AB),
            Color(0xFFA23B72),
            Color(0xFFF18F01),
            Color(0xFF3DAA6E),
        )
    return palette[Math.abs(entry.category.hashCode()) % palette.size]
}
