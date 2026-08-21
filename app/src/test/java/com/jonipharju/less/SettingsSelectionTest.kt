package com.jonipharju.less

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
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

    /** The option is the row: a tap on its margin, before the marker, chooses it as surely as one on its label. */
    @Test
    fun theWholeRowChoosesTheOption() {
        val repository = FakeLauncherRepository()
        compose.setContent { Settings(repository, onClose = {}) }

        compose.onNodeWithText("Swipe down").performScrollTo().performTouchInput { click(Offset(1f, centerY)) }

        compose.runOnIdle { assertEquals(DrawerOpenDirection.SwipeDown, repository.settings.value.drawerOpenDirection) }
    }

    /** A reader hears one radio button, selected or not, with its marker folded in rather than beside it. */
    @Test
    fun eachOptionReadsAsOneRadioButtonWithItsState() {
        compose.setContent { Settings(FakeLauncherRepository(), onClose = {}) }

        compose
            .onNodeWithText("Swipe up")
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assert(hasText("✓"))
        compose
            .onNodeWithText("Swipe down")
            .assertIsNotSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        // The marker is never a node of its own: every node that carries it is an option.
        compose.onAllNodesWithText("✓").assertAll(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
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
