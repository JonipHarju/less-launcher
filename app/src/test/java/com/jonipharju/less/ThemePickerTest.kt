package com.jonipharju.less

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jonipharju.less.launcher.FakeLauncherRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ThemePickerTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun thePickerShowsEachThemeWithItsCredit() {
        compose.setContent { Settings(FakeLauncherRepository(), onClose = {}) }

        compose.onNodeWithText("Claude Monet").assertExists()
        compose.onNodeWithText("Woman with a Parasol - Madame Monet and Her Son").assertExists()
        compose.onNodeWithText("Jean-Baptiste-Camille Corot").assertExists()
        compose.onNodeWithText("The Forest of Coubron").assertExists()
        compose.onNodeWithText("Albert Bierstadt").assertExists()
        compose.onNodeWithText("Mount Corcoran").assertExists()
        compose.onNodeWithText("Thomas Cole").assertExists()
        compose.onNodeWithText("The Departure").assertExists()
        compose.onNodeWithText("Near Black").assertExists()
        compose.onNodeWithText("Off White").assertExists()
    }

    @Test
    fun pickingAThemeStoresItAndHangsItsWallpaper() {
        val repository = FakeLauncherRepository()
        compose.setContent { Settings(repository, onClose = {}) }

        compose.onNodeWithText("Claude Monet").performScrollTo().performClick()

        compose.runOnIdle {
            assertEquals("parasol", repository.settings.value.themeId)
            assertEquals(listOf(ParasolTheme.wallpaperAsset), repository.wallpapersHung)
        }
        compose.onNodeWithText("Claude Monet").assertIsSelected()
    }
}
