package dev.favourdevlabs.cleanthes.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.min

/**
 * A solid octagon clip shape — the outer "seal" ring for the citadel-entry
 * avatar/icon, holding an inner circular medallion. Reads as a coin or seal
 * rather than a soft consumer-app circle, matching Cleanthes' Stoic,
 * disciplined visual identity.
 */
object OctagonShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val width = size.width
        val height = size.height
        val cut = min(width, height) * 0.3f

        val path = Path().apply {
            moveTo(cut, 0f)
            lineTo(width - cut, 0f)
            lineTo(width, cut)
            lineTo(width, height - cut)
            lineTo(width - cut, height)
            lineTo(cut, height)
            lineTo(0f, height - cut)
            lineTo(0f, cut)
            close()
        }
        return Outline.Generic(path)
    }
}
