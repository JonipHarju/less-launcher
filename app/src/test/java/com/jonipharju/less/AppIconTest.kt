package com.jonipharju.less

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val Accent = Color(0.2f, 0.6f, 0.9f)

/** Compose packs an sRGB [Color] at 8 bits a channel, so assertions cannot be tighter than 1/255. */
private const val TOLERANCE = 0.005f

class AppIconTest {
    @Test
    fun theBrightestPartOfAnIconBecomesTheThemeColour() {
        assertChannels(Accent, desaturatedTint(Accent).applyTo(Color.White))
    }

    @Test
    fun theDarkestPartOfAnIconStaysBlack() {
        assertChannels(Color.Black, desaturatedTint(Accent).applyTo(Color.Black))
    }

    @Test
    fun colourfulArtworkKeepsItsLightAndDarkDetail() {
        val filter = desaturatedTint(Accent)
        // Red is a dark colour by luminance and green a bright one, so the two must not flatten together.
        val red = filter.applyTo(Color.Red)
        val green = filter.applyTo(Color.Green)

        assertEquals("red keeps the theme hue", Accent.tintRatio(), red.tintRatio(), TOLERANCE)
        assertEquals("green keeps the theme hue", Accent.tintRatio(), green.tintRatio(), TOLERANCE)
        assertTrue("A dark source colour must stay darker than a bright one", red.green < green.green)
    }

    @Test
    fun transparentPartsOfAnIconStayTransparent() {
        assertEquals(0.4f, desaturatedTint(Accent).applyTo(Color.White.copy(alpha = 0.4f)).alpha, TOLERANCE)
    }
}

/** The channel arithmetic Compose applies for a [ColorMatrix], so the matrix itself can be asserted on. */
private fun ColorMatrix.applyTo(color: Color): Color {
    fun channel(row: Int) =
        this[row, 0] * color.red +
            this[row, 1] * color.green +
            this[row, 2] * color.blue +
            this[row, 3] * color.alpha +
            this[row, 4]
    return Color(channel(0), channel(1), channel(2), channel(3))
}

/** The ratio between two channels, so a tint's hue can be compared independently of its brightness. */
private fun Color.tintRatio() = red / maxOf(green, 0.0001f)

private fun assertChannels(
    expected: Color,
    actual: Color,
) {
    assertEquals("red", expected.red, actual.red, TOLERANCE)
    assertEquals("green", expected.green, actual.green, TOLERANCE)
    assertEquals("blue", expected.blue, actual.blue, TOLERANCE)
    assertEquals("alpha", expected.alpha, actual.alpha, TOLERANCE)
}
