package com.jonipharju.less

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class NearBlackThemeTest {
    @Test
    fun scrimKeepsThemeTextLegibleOverWhite() {
        val backgrounds =
            listOf(NearBlackTheme.scrim.top, NearBlackTheme.scrim.bottom)
                .map { it.compositeOver(Color.White) }
        val textColors = listOf(NearBlackTheme.textColor, NearBlackTheme.secondaryTextColor)

        backgrounds.forEach { background ->
            textColors.forEach { text ->
                assertTrue(contrastRatio(text, background) >= 4.5f)
            }
        }
    }
}

private fun contrastRatio(
    first: Color,
    second: Color,
): Float {
    val lighter = maxOf(first.luminance(), second.luminance())
    val darker = minOf(first.luminance(), second.luminance())
    return (lighter + 0.05f) / (darker + 0.05f)
}
