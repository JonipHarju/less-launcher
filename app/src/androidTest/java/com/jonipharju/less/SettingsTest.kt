package com.jonipharju.less

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jonipharju.less.launcher.DrawerOpenDirection
import com.jonipharju.less.launcher.FakeLauncherRepository
import com.jonipharju.less.launcher.HomeAlignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsTest {
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
    fun closeControlLeavesSettings() {
        val repository = FakeLauncherRepository()
        var closes = 0
        compose.setContent { Settings(repository, onClose = { closes++ }) }

        compose.onNodeWithContentDescription("Close Settings").performClick()

        assertEquals(1, closes)
    }
}
