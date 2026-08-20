package com.jonipharju.less

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jonipharju.less.launcher.DrawerOpenDirection
import com.jonipharju.less.launcher.FakeLauncherRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SettingsSelectionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun eachStoredChoiceHasOneVisibleSelectionMarker() {
        compose.setContent { Settings(FakeLauncherRepository(), onClose = {}) }

        // Theme, Drawer Open Direction, Home Alignment, and Icon Mode each expose one choice.
        compose.onAllNodesWithText("✓", useUnmergedTree = true).assertCountEquals(4)
    }

    @Test
    fun selectionMarkersReserveTheSameLabelSpace() {
        compose.setContent { Settings(FakeLauncherRepository(), onClose = {}) }
        val swipeUpLeft =
            compose
                .onNodeWithText("Swipe up", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot.left
        val swipeDownLeft =
            compose
                .onNodeWithText("Swipe down", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot.left

        assertEquals(
            swipeUpLeft,
            swipeDownLeft,
            0f,
        )
    }

    @Test
    fun choosingAnOptionMovesTheMarkerToTheNewSelection() {
        val repository = FakeLauncherRepository()
        compose.setContent { Settings(repository, onClose = {}) }

        compose.onNodeWithText("Swipe down").performScrollTo().performClick()

        compose.runOnIdle { assertEquals(DrawerOpenDirection.SwipeDown, repository.settings.value.drawerOpenDirection) }
        compose.onNodeWithText("Swipe down").assertIsSelected()
        compose.onAllNodesWithText("✓", useUnmergedTree = true).assertCountEquals(4)
    }
}
