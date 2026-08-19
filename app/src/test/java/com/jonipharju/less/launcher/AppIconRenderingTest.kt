package com.jonipharju.less.launcher

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.IntRect
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import android.graphics.Color as PaintColor

/** The side of a layer an adaptive icon is authored at, and so the size of the test's artwork. */
private const val LAYER_SIDE = 108

/** The share of each side of its layer the test glyph paints, centred on transparency. */
private const val GLYPH_SHARE_OF_LAYER = 1f / 3f

/**
 * An adaptive icon lays a layer out half again the size of the bitmap it draws, so a glyph covering
 * a third of its layer covers half of each side of that bitmap — a quarter of its area. A layer
 * filling the whole icon mask would cover pi/4, about 79%, instead.
 */
private const val GLYPH_SHARE_OF_BITMAP = (GLYPH_SHARE_OF_LAYER * 1.5f) * (GLYPH_SHARE_OF_LAYER * 1.5f)

private const val TOLERANCE = 0.005f

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class AppIconRenderingTest {
    @Test
    fun `a Themeable Layer reaches the launcher as its glyph and not as a filled mask`() {
        val themeable = requireNotNull(themedIcon().themeable)

        // Tinted Icon Mode keeps only this alpha and repaints it in the accent colour, so a layer
        // that covers the icon mask reaches the user as a flat disc with no glyph left in it.
        assertEquals(
            "the share of the bitmap the Themeable Layer paints",
            GLYPH_SHARE_OF_BITMAP,
            themeable.paintedShare(),
            TOLERANCE,
        )
    }

    @Test
    fun `a Themeable Layer lands where the same artwork lands in the original`() {
        val icon = themedIcon()

        assertEquals(
            "the Themeable Layer's glyph and the original's own artwork are the same shape, so a" +
                " themed icon has to come out the size the original does",
            icon.original.regionWhere { it.red > 0.5f },
            requireNotNull(icon.themeable).regionWhere { it.alpha > 0.5f },
        )
    }

    @Test
    fun `an app supplying no Themeable Layer gets none to tint`() {
        assertNull("an adaptive icon without a monochrome layer", AdaptiveIconDrawable(null, Glyph()).toAppIcon().themeable)
        assertNull("artwork that is not an adaptive icon at all", Glyph().toAppIcon().themeable)
    }

    @Test
    fun `both layers of an icon are rendered at one size`() {
        val icon = themedIcon()
        val themeable = requireNotNull(icon.themeable)

        assertEquals(icon.original.width, themeable.width)
        assertEquals(icon.original.height, themeable.height)
    }
}

/** An app supplying a Themeable Layer whose glyph is the shape its own artwork draws. */
private fun themedIcon(): AppIcon = AdaptiveIconDrawable(null, Glyph(PaintColor.RED), Glyph()).toAppIcon()

/**
 * One icon layer: an opaque square across [GLYPH_SHARE_OF_LAYER] of its bounds, centred on
 * transparency, standing in for the glyph an app's artwork and Themeable Layer carry.
 */
private class Glyph(
    private val color: Int = PaintColor.BLACK,
) : Drawable() {
    override fun getIntrinsicWidth() = LAYER_SIDE

    override fun getIntrinsicHeight() = LAYER_SIDE

    override fun draw(canvas: Canvas) {
        val insetX = bounds.width() * (1f - GLYPH_SHARE_OF_LAYER) / 2f
        val insetY = bounds.height() * (1f - GLYPH_SHARE_OF_LAYER) / 2f
        canvas.drawRect(
            bounds.left + insetX,
            bounds.top + insetY,
            bounds.right - insetX,
            bounds.bottom - insetY,
            Paint().apply { color = this@Glyph.color },
        )
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Drawable.getOpacity is deprecated, but stays abstract")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}

/** The share of the bitmap's pixels that carry any paint at all — the alpha a tint would keep. */
private fun ImageBitmap.paintedShare(): Float {
    val pixels = toPixelMap()
    val painted = (0 until height).sumOf { y -> (0 until width).count { x -> pixels[x, y].alpha > 0.5f } }

    return painted.toFloat() / (width * height)
}

/** The smallest box holding every pixel [matches] accepts, so two renderings can be compared. */
private fun ImageBitmap.regionWhere(matches: (Color) -> Boolean): IntRect {
    val pixels = toPixelMap()
    val found = (0 until height).flatMap { y -> (0 until width).mapNotNull { x -> (x to y).takeIf { matches(pixels[x, y]) } } }
    require(found.isNotEmpty()) { "no pixel of the ${width}x$height bitmap matched" }

    return IntRect(
        left = found.minOf { it.first },
        top = found.minOf { it.second },
        right = found.maxOf { it.first } + 1,
        bottom = found.maxOf { it.second } + 1,
    )
}
