package com.jonipharju.less

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.jonipharju.less.launcher.IconMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemesTest {
    @Test
    fun aUserIconModeOverridesTheActiveTheme() {
        assertEquals(IconMode.Original, effectiveIconMode(CoubronTheme, IconMode.Original))
        assertEquals(IconMode.Tinted, effectiveIconMode(CoubronTheme, null))
    }

    @Test
    fun sixThemesShipAndEachCarriesAFullCredit() {
        assertEquals(
            listOf("near-black", "off-white", "parasol", "coubron", "mount-corcoran", "departure"),
            Themes.map(Theme::id),
        )
        Themes.forEach { theme ->
            assertTrue(theme.name.isNotBlank())
            assertTrue(theme.credit.artist.isNotBlank())
            assertTrue(theme.credit.title.isNotBlank())
            assertTrue(theme.credit.year.isNotBlank())
            assertTrue(theme.credit.collection.isNotBlank())
            assertTrue(theme.credit.source.isNotBlank())
        }
    }

    @Test
    fun artworkThemesCreditTheNationalGalleryWorks() {
        assertEquals(
            Credit(
                artist = "Claude Monet",
                title = "Woman with a Parasol - Madame Monet and Her Son",
                year = "1875",
                collection = "National Gallery of Art, Washington",
                source = "NGA Open Access (CC0)",
            ),
            themeById("parasol").credit,
        )
        assertEquals(
            Credit(
                artist = "Jean-Baptiste-Camille Corot",
                title = "The Forest of Coubron",
                year = "1872",
                collection = "National Gallery of Art, Washington",
                source = "NGA Open Access (CC0)",
            ),
            themeById("coubron").credit,
        )
        assertEquals(
            Credit(
                artist = "Albert Bierstadt",
                title = "Mount Corcoran",
                year = "c. 1876-1877",
                collection = "National Gallery of Art, Washington",
                source = "NGA Open Access (CC0)",
            ),
            themeById("mount-corcoran").credit,
        )
        assertEquals(
            Credit(
                artist = "Thomas Cole",
                title = "The Departure",
                year = "1837",
                collection = "National Gallery of Art, Washington",
                source = "NGA Open Access (CC0)",
            ),
            themeById("departure").credit,
        )
    }

    @Test
    fun anUnknownThemeIdFallsBackToNearBlack() {
        assertEquals(NearBlackTheme, themeById("no-such-theme"))
    }

    @Test
    fun everyScrimKeepsThemeTextLegibleOverWhiteAndBlack() {
        Themes.forEach { theme ->
            val backgrounds =
                listOf(theme.scrim.top, theme.scrim.bottom).flatMap { stop ->
                    listOf(stop.compositeOver(Color.White), stop.compositeOver(Color.Black))
                }
            val textColors = listOf(theme.textColor, theme.secondaryTextColor)

            backgrounds.forEach { background ->
                textColors.forEach { text ->
                    assertTrue(
                        "${theme.name}: contrast of $text over $background was ${contrastRatio(text, background)}",
                        contrastRatio(text, background) >= 4.5f,
                    )
                }
            }
        }
    }

    @Test
    fun homeScrimIsLighterThanTheDrawerSoTheWallpaperStillReads() {
        Themes.forEach { theme ->
            val home = theme.scrim.forHome()
            assertTrue("${theme.name}: Home scrim should be lighter than the Drawer", home.top.alpha < theme.scrim.top.alpha)
            assertTrue("${theme.name}: Home scrim should still cushion the text", home.top.alpha >= 0.25f)
            assertTrue(home.bottom.alpha < theme.scrim.bottom.alpha)
            assertTrue(home.bottom.alpha >= 0.2f)
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
