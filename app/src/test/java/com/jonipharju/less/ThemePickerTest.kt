package com.jonipharju.less

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jonipharju.less.launcher.FakeLauncherRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        compose.onNodeWithText("Carl Blechen").assertExists()
        compose.onNodeWithText("A Ruined Church in the Forest").assertExists()
        compose.onNodeWithText("William Russell Birch").assertExists()
        compose.onNodeWithText("View from the Springhouse at Echo").assertExists()
        compose.onNodeWithText("Near Black").assertExists()
        compose.onNodeWithText("Off White").assertExists()
    }

    @Test
    fun pickingAThemeStoresItAndLeavesTheSystemWallpaperAlone() {
        val repository = FakeLauncherRepository()
        val applied = mutableListOf<Theme>()
        compose.setContent {
            Settings(repository, onClose = {}, onApplyWallpaper = { applied += it })
        }

        compose.onNodeWithText("Claude Monet").performScrollTo().performClick()

        compose.runOnIdle {
            assertEquals("parasol", repository.settings.value.themeId)
            assertTrue(applied.isEmpty())
        }
        compose.onNodeWithText("Claude Monet").assertIsSelected()
    }

    @Test
    fun setAsWallpaperAppliesTheActiveTheme() =
        runBlocking {
            val repository = FakeLauncherRepository()
            repository.updateSettings { it.copy(themeId = ParasolTheme.id) }
            val applied = mutableListOf<Theme>()
            compose.setContent {
                Settings(repository, onClose = {}, onApplyWallpaper = { applied += it })
            }

            compose.onNodeWithText("Set as Wallpaper").performScrollTo().performClick()

            compose.runOnIdle { assertEquals(listOf(ParasolTheme), applied) }
        }
}
