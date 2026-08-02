package dev.favourdevlabs.cleanthes.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.min

/**
 * A flat-top hexagon clip shape — used for the citadel-entry avatar/icon, evoking a
 * seal or medallion rather than a soft consumer-app circle, matching Cleanthes'
 * Stoic, disciplined visual identity.
 */
object HexagonShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val width = size.width
        val height = size.height
        val sideInset = min(width, height) * 0.25f

        val path = Path().apply {
            moveTo(sideInset, 0f)
            lineTo(width - sideInset, 0f)
            lineTo(width, height / 2f)
            lineTo(width - sideInset, height)
            lineTo(sideInset, height)
            lineTo(0f, height / 2f)
            close()
        }
        return Outline.Generic(path)
    }
}
