package com.jonipharju.less.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/** The icon the platform hands out for an app, as the two layers Icon Mode draws from. */
internal fun Drawable.toAppIcon(): AppIcon {
    val size = IntSize(intrinsicWidth.coerceAtLeast(1), intrinsicHeight.coerceAtLeast(1))
    val monochrome = (this as? AdaptiveIconDrawable)?.monochrome

    return AppIcon(
        original = drawnInto(size, Rect(0, 0, size.width, size.height)),
        themeable = monochrome?.drawnInto(size, adaptiveViewportOf(size)),
    )
}

/**
 * Where an adaptive icon lays a layer out: proud of every edge, so that what it draws is the middle
 * of a layer half again the size of the bitmap. A Themeable Layer drawn at plain bounds keeps that
 * safe-zone margin instead, and its glyph reads about a third smaller than the original beside it.
 *
 * Handing the layer back to an [AdaptiveIconDrawable] would give it this geometry too, but that
 * paints its whole mask opaque before drawing a layer into it. Tinted Icon Mode keeps only the
 * alpha, so every app supplying a Themeable Layer came out as the same flat accent-coloured disc.
 *
 * The icon mask goes with it: a Themeable Layer is drawn unclipped, where the original is rounded
 * off. A layer is authored to keep its glyph inside the safe zone, so there is nothing out at the
 * corners for the mask to cut — and an unclipped alpha is the whole point of drawing it this way.
 */
private fun adaptiveViewportOf(size: IntSize): Rect {
    val marginX = (size.width * AdaptiveIconDrawable.getExtraInsetFraction()).roundToInt()
    val marginY = (size.height * AdaptiveIconDrawable.getExtraInsetFraction()).roundToInt()

    return Rect(-marginX, -marginY, size.width + marginX, size.height + marginY)
}

/**
 * Draws a copy of this drawable, laid out across [bounds], into a bitmap [size] — so that whatever
 * [bounds] puts outside the bitmap is cropped away, and the icon this layer came from is untouched.
 */
private fun Drawable.drawnInto(
    size: IntSize,
    bounds: Rect,
): ImageBitmap {
    val copy = constantState?.newDrawable()?.mutate() ?: this
    copy.bounds = bounds

    return Bitmap
        .createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        .also { bitmap -> copy.draw(Canvas(bitmap)) }
        .asImageBitmap()
}
