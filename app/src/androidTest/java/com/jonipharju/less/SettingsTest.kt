package com.jonipharju.less

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.jonipharju.less.launcher.DrawerOpenDirection
import com.jonipharju.less.launcher.FakeLauncherRepository
import com.jonipharju.less.launcher.Favorite
import com.jonipharju.less.launcher.FavoritesSoftCap
import com.jonipharju.less.launcher.HomeAlignment
import com.jonipharju.less.launcher.IconMode
import com.jonipharju.less.launcher.ShownFavorite
import com.jonipharju.less.launcher.launcherAppFixture
import com.jonipharju.less.launcher.shownAmong
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsTest {
    @Test
    fun choosingAnIconModeStoresTheGlobalOverride() {
        val repository = FakeLauncherRepository()
        compose.setContent { Settings(repository, onClose = {}) }

        compose.onNodeWithText("Tinted").performScrollTo().performClick()

        compose.runOnIdle { assertEquals(IconMode.Tinted, repository.settings.value.iconModeOverride) }
    }

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsTheStoredChoicesAsSelected() {
        val repository = FakeLauncherRepository()
        compose.setContent { Settings(repository, onClose = {}) }

        compose.onNodeWithText("Swipe up").assertIsSelected()
        compose.onNodeWithText("Left").assertIsSelected()
        compose.onNodeWithText("Open keyboard with Drawer").assertIsOn()
    }

    @Test
    fun choosingTheOtherDrawerDirectionStoresIt() {
        val repository = FakeLauncherRepository()
        compose.setContent { Settings(repository, onClose = {}) }

        compose.onNodeWithText("Swipe down").performClick()

        compose.runOnIdle {
            assertEquals(DrawerOpenDirection.SwipeDown, repository.settings.value.drawerOpenDirection)
        }
        compose.onNodeWithText("Swipe down").assertIsSelected()
    }

    @Test
    fun choosingCentredHomeAlignmentStoresIt() {
        val repository = FakeLauncherRepository()
        compose.setContent { Settings(repository, onClose = {}) }

        compose.onNodeWithText("Centred").performClick()

        compose.runOnIdle {
            assertEquals(HomeAlignment.Centred, repository.settings.value.homeAlignment)
        }
    }

    @Test
    fun turningTheKeyboardOffAndOnAgainStoresBoth() {
        val repository = FakeLauncherRepository()
        compose.setContent { Settings(repository, onClose = {}) }

        compose.onNodeWithText("Open keyboard with Drawer").performClick()

        compose.runOnIdle { assertFalse(repository.settings.value.opensKeyboardWithDrawer) }
        compose.onNodeWithText("Open keyboard with Drawer").assertIsOff().performClick()

        compose.runOnIdle { assertTrue(repository.settings.value.opensKeyboardWithDrawer) }
    }

    @Test
    fun changingOneOptionLeavesTheOthersAlone() {
        val repository = FakeLauncherRepository()
        compose.setContent { Settings(repository, onClose = {}) }

        compose.onNodeWithText("Swipe down").performClick()
        compose.onNodeWithText("Centred").performClick()

        compose.runOnIdle {
            assertEquals(DrawerOpenDirection.SwipeDown, repository.settings.value.drawerOpenDirection)
            assertEquals(HomeAlignment.Centred, repository.settings.value.homeAlignment)
        }
    }

    @Test
    fun theFavoritesEditorListsFavoritesInOrder() {
        val repository = settingsWith("Clock", "Camera")

        compose.onNodeWithText("Favorites").assertExists()
        compose.onNodeWithContentDescription("Move Camera up").assertExists()
        compose.onNodeWithContentDescription("Move Clock down").assertExists()
        // The first Favorite has nowhere above it and the last nowhere below it.
        compose.onNodeWithContentDescription("Move Clock up").assertDoesNotExist()
        compose.onNodeWithContentDescription("Move Camera down").assertDoesNotExist()
        assertEquals(listOf("Clock", "Camera"), repository.favoriteLabels())
    }

    @Test
    fun theEditorSaysSoWhenThereAreNoFavorites() {
        settingsWith()

        compose.onNodeWithText("None yet. Long-press an app in the Drawer to pin one.").assertExists()
    }

    @Test
    fun movingAFavoriteDownReordersHome() {
        val repository = settingsWith("Clock", "Camera", "Maps")

        compose.onNodeWithContentDescription("Move Clock down").performClick()

        compose.runOnIdle { assertEquals(listOf("Camera", "Clock", "Maps"), repository.favoriteLabels()) }
    }

    @Test
    fun movingAFavoriteUpReordersHome() {
        val repository = settingsWith("Clock", "Camera", "Maps")

        compose.onNodeWithContentDescription("Move Maps up").performClick()

        compose.runOnIdle { assertEquals(listOf("Clock", "Maps", "Camera"), repository.favoriteLabels()) }
    }

    @Test
    fun renamingInTheEditorShowsTheCustomLabel() {
        val repository = settingsWith("Clock")

        compose.onNodeWithText("Clock").performClick()
        compose.onNodeWithContentDescription("Name for Clock").performTextClearance()
        compose.onNodeWithContentDescription("Name for Clock").performTextInput("Time")
        compose.onNodeWithText("Save").performClick()

        compose.runOnIdle { assertEquals(listOf("Time"), repository.favoriteLabels()) }
        compose.onNodeWithContentDescription("Unpin Time").assertExists()
    }

    @Test
    fun unpinningInTheEditorTakesTheFavoriteOffHome() {
        val repository = settingsWith("Clock", "Camera")

        compose.onNodeWithContentDescription("Unpin Clock").performClick()

        compose.runOnIdle { assertEquals(listOf("Camera"), repository.favoriteLabels()) }
    }

    @Test
    fun theEditorWarnsWhileHomeIsPastTheSoftCap() {
        val labels = (1..FavoritesSoftCap + 1).map { "App$it" }
        settingsWith(*labels.toTypedArray())

        compose.onNodeWithText("More than 8 Favorites. Home takes them and starts to scroll.").assertExists()
    }

    @Test
    fun closeControlLeavesSettings() {
        val repository = FakeLauncherRepository()
        var closes = 0
        compose.setContent { Settings(repository, onClose = { closes++ }) }

        compose.onNodeWithContentDescription("Close Settings").performClick()

        assertEquals(1, closes)
    }

    /** Settings over a Home holding one Favorite per label, in the order given. */
    private fun settingsWith(vararg labels: String): FakeLauncherRepository {
        val repository = FakeLauncherRepository()
        runBlocking {
            labels.forEachIndexed { position, label ->
                val app = launcherAppFixture(label)
                repository.install(app)
                repository.chooseFavorite(Favorite(app.id, position = position))
            }
        }

        compose.setContent { Settings(repository, onClose = {}) }
        return repository
    }

    private fun FakeLauncherRepository.favoriteLabels() = favorites.value.shownAmong(installedApps.value).map(ShownFavorite::label)
}
