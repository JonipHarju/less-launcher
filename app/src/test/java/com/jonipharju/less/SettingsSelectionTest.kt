package com.jonipharju.less

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
import org.robolectric.annotation.GraphicsMode

/**
 * A chosen option has to read as chosen on its own, not only by comparison with its neighbours.
 * Colour alone was too weak on several Themes; the marker is the extra signal.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xxhdpi")
class SettingsSelectionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the chosen option in a group carries a marker the others do not`() {
        compose.setContent { Settings(FakeLauncherRepository(), onClose = {}) }

        compose.onNode(markedChoice("Swipe up"), useUnmergedTree = true).assertExists()
        compose.onNode(markedChoice("Swipe down"), useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Swipe up").assertIsSelected()
        compose.onNodeWithText("Swipe down").assertExists()
    }

    @Test
    fun `choosing another option moves the marker and does not shift the labels`() {
        val repository = FakeLauncherRepository()
        compose.setContent { Settings(repository, onClose = {}) }

        compose.onNodeWithText("Swipe down").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(DrawerOpenDirection.SwipeDown, repository.settings.value.drawerOpenDirection)
        }

        compose.onNode(markedChoice("Swipe down"), useUnmergedTree = true).assertExists()
        compose.onNode(markedChoice("Swipe up"), useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Swipe down").assertIsSelected()

        val up = compose.onNodeWithText("Swipe up").getUnclippedBoundsInRoot()
        val down = compose.onNodeWithText("Swipe down").getUnclippedBoundsInRoot()
        assertEquals(up.left, down.left)
    }

    @Test
    fun `the Theme picker marks the active Theme the same way`() {
        val repository = FakeLauncherRepository()
        compose.setContent { Settings(repository, onClose = {}) }

        compose.onNode(markedChoice("Near Black"), useUnmergedTree = true).assertExists()
        compose.onNode(markedChoice("Off White"), useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Near Black").assertIsSelected()

        compose.onNodeWithText("Claude Monet").performScrollTo().performClick()
        compose.waitForIdle()

        compose.onNode(markedChoice("Claude Monet"), useUnmergedTree = true).assertExists()
        compose.onNode(markedChoice("Near Black"), useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("Claude Monet").assertIsSelected()
    }

    /** The option row itself, not the Settings screen that happens to contain both a label and a mark. */
    private fun markedChoice(label: String): SemanticsMatcher =
        isSelectable() and hasAnyDescendant(hasText(label)) and hasAnyDescendant(hasText("●"))
}
