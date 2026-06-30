package dev.favourdevlabs.cleanthes.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Cleanthes is always dark. No light mode. The Stoic does not flinch.
private val DarkColorScheme =
    darkColorScheme(
        primary = GoldPrimary,
        onPrimary = OnGold,
        primaryContainer = GoldContainer,
        onPrimaryContainer = GoldBright,
        secondary = GoldDim,
        onSecondary = TextPrimary,
        secondaryContainer = SurfaceElevated,
        onSecondaryContainer = TextSecondary,
        background = SurfaceDeep,
        onBackground = TextPrimary,
        surface = SurfaceDark,
        onSurface = TextPrimary,
        surfaceVariant = SurfaceElevated,
        onSurfaceVariant = TextSecondary,
        error = Danger,
        onError = SurfaceDeep,
        outline = GoldDim.copy(alpha = 0.35f),
        outlineVariant = SurfaceModal,
        scrim = SurfaceDeep.copy(alpha = 0.85f),
    )

@Composable
fun CleanthesTheme(content: @Composable () -> Unit) {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SurfaceDeep.toArgb()
            window.navigationBarColor = SurfaceDeep.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = CleanthesTypography,
        content = content,
    )
}
