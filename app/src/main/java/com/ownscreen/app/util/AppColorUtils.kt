package com.ownscreen.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.ownscreen.app.ui.theme.nord10
import com.ownscreen.app.ui.theme.nord11
import com.ownscreen.app.ui.theme.nord12
import com.ownscreen.app.ui.theme.nord13
import com.ownscreen.app.ui.theme.nord14
import com.ownscreen.app.ui.theme.nord15
import com.ownscreen.app.ui.theme.nord7
import com.ownscreen.app.ui.theme.nord8
import com.ownscreen.app.ui.theme.nord9

/**
 * Maps an app icon's dominant color onto the Nord theme's accent palette, so widget rows can show
 * a small themed color dot for each app instead of its raw (visually noisy, off-brand) launcher icon.
 */
object AppColorUtils {

    // Only the Frost + Aurora accent swatches are candidates — Polar Night/Snow Storm are
    // neutrals meant for backgrounds/text, not per-app identity dots.
    private val NORD_ACCENTS = listOf(nord7, nord8, nord9, nord10, nord11, nord12, nord13, nord14, nord15)

    /**
     * Averages icon pixel colors — skipping near-transparent, near-white/black, and low-saturation
     * (gray) pixels, which are almost always padding/background rather than the icon's actual brand
     * color — then snaps the result to the closest Nord accent so every dot stays on-theme.
     */
    fun nordAccentFor(icon: Bitmap): Color {
        // Downscaling first keeps pixel sampling fast and cheap regardless of the source icon's resolution.
        val sample = Bitmap.createScaledBitmap(icon, 16, 16, true)
        val pixels = IntArray(sample.width * sample.height)
        sample.getPixels(pixels, 0, sample.width, 0, 0, sample.width, sample.height)
        if (sample !== icon) sample.recycle()

        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0L
        for (pixel in pixels) {
            if (((pixel ushr 24) and 0xFF) < 128) continue // skip transparent padding

            val r = (pixel ushr 16) and 0xFF
            val g = (pixel ushr 8) and 0xFF
            val b = pixel and 0xFF
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            if (min > 235 || max < 20 || (max - min) < 12) continue // near-white/black/gray

            rSum += r
            gSum += g
            bSum += b
            count++
        }

        // Fully neutral/monochrome icon (or nothing survived filtering) — fall back to a plain
        // average of every opaque pixel instead of a hardcoded default color.
        val avg = if (count > 0) {
            Color(red = (rSum / count) / 255f, green = (gSum / count) / 255f, blue = (bSum / count) / 255f)
        } else {
            averageOpaque(pixels) ?: nord9
        }

        return NORD_ACCENTS.minBy { distanceSquared(it, avg) }
    }

    private fun averageOpaque(pixels: IntArray): Color? {
        var rSum = 0L; var gSum = 0L; var bSum = 0L; var count = 0L
        for (pixel in pixels) {
            if (((pixel ushr 24) and 0xFF) < 128) continue
            rSum += (pixel ushr 16) and 0xFF
            gSum += (pixel ushr 8) and 0xFF
            bSum += pixel and 0xFF
            count++
        }
        if (count == 0L) return null
        return Color(red = (rSum / count) / 255f, green = (gSum / count) / 255f, blue = (bSum / count) / 255f)
    }

    private fun distanceSquared(a: Color, b: Color): Float {
        val dr = a.red - b.red
        val dg = a.green - b.green
        val db = a.blue - b.blue
        return dr * dr + dg * dg + db * db
    }

    /** Renders a solid-filled circle bitmap for use in a widget's [android.widget.ImageView] row dot. */
    fun dotBitmap(color: Color, sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb() }
        val radius = sizePx / 2f
        canvas.drawCircle(radius, radius, radius, paint)
        return bitmap
    }
}
