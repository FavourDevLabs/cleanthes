package dev.favourdevlabs.cleanthes.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.caverock.androidsvg.SVG

/**
 * Decodes favicon bytes fetched from the favicon proxy into something Compose can render.
 *
 * Favicons arrive in one of two broad shapes: raster formats (PNG, JPEG, WebP — handled
 * natively by [BitmapFactory]) or SVG (which BitmapFactory cannot decode at all, so those
 * bytes are routed through AndroidSVG and rendered to a Bitmap manually).
 *
 * Returns null on any failure — corrupt bytes, unsupported format (e.g. legacy multi-image
 * ICO container), or a decode exception — so callers can fall back to a default avatar
 * without needing to know why decoding failed.
 */
fun decodeFaviconBitmap(bytes: ByteArray, contentType: String?, targetSizePx: Int = 96): ImageBitmap? {
    val looksLikeSvg =
        contentType?.contains("svg", ignoreCase = true) == true ||
            bytes.decodeToString(0, minOf(bytes.size, 256)).contains("<svg", ignoreCase = true)

    return try {
        if (looksLikeSvg) {
            decodeSvg(bytes, targetSizePx)
        } else {
            decodeRaster(bytes)
        }
    } catch (_: Exception) {
        null
    }
}

private fun decodeSvg(bytes: ByteArray, targetSizePx: Int): ImageBitmap? {
    val svg = SVG.getFromInputStream(bytes.inputStream())
    val bitmap = Bitmap.createBitmap(targetSizePx, targetSizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    svg.renderToCanvas(canvas)
    return bitmap.asImageBitmap()
}

private fun decodeRaster(bytes: ByteArray): ImageBitmap? {
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    return bitmap.asImageBitmap()
}
