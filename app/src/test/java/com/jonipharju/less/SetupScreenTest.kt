package com.jonipharju.less

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jonipharju.less.launcher.EverydayIntent
import com.jonipharju.less.launcher.FakeLauncherRepository
import com.jonipharju.less.launcher.Favorite
import com.jonipharju.less.launcher.LauncherApp
import com.jonipharju.less.launcher.SetupStep
import com.jonipharju.less.launcher.launcherAppFixture
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SetupScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val phone = launcherAppFixture(label = "Phone")
    private val messages = launcherAppFixture(label = "Messages")
    private val camera = launcherAppFixture(label = "Camera")
    private val browser = launcherAppFixture(label = "Browser")
    private val notes = launcherAppFixture(label = "Notes")

    /** A device with the everyday apps a phone comes with, and one it does not. */
    private fun deviceOutOfTheBox() =
        FakeLauncherRepository().apply {
            listOf(phone, messages, camera, browser, notes).forEach(::install)
            answer(EverydayIntent.Phone, phone.id)
            answer(EverydayIntent.Messaging, messages.id)
            answer(EverydayIntent.Camera, camera.id)
            answer(EverydayIntent.Browser, browser.id)
        }

    @Test
    fun firstLaunchShowsTheThemePickerBeforeAnythingElse() {
        val repository = deviceOutOfTheBox()

        compose.setContent { LessLauncher(repository) }

        compose.onNodeWithText("Pick a look").assertExists()
        compose.onNodeWithText("Claude Monet").assertExists()
        // Not Home, and not the app the Drawer would list.
        compose.onNodeWithText("Notes").assertDoesNotExist()
    }

    @Test
    fun aLauncherThatHasRunBeforeNeverFlashesSetupOnTheWayIn() {
        val repository = deviceOutOfTheBox()
        // The stored settings say nothing yet, and their defaults say Setup has never run.
        repository.withholdStoredSettings()
        compose.setContent { LessLauncher(repository) }

        compose.onNodeWithText("Pick a look").assertDoesNotExist()
        compose.onNodeWithText("Continue").assertDoesNotExist()

        compose.runOnIdle { repository.readStoredSettings() }

        compose.onNodeWithText("Pick a look").assertExists()
    }

    @Test
    fun theThemePickedDuringSetupIsTheThemeSetupItselfWears() {
        val repository = deviceOutOfTheBox()
        val applied = mutableListOf<Theme>()
        compose.setContent { LessLauncher(repository, onApplyWallpaper = { applied += it }) }

        compose.onNodeWithText("Claude Monet").performScrollTo().performClick()

        compose.runOnIdle {
            assertEquals("parasol", repository.settings.value.themeId)
            assertEquals(listOf(ParasolTheme), applied)
        }
    }

    @Test
    fun theHomeRoleRequestFollowsTheThemePicker() {
        val repository = deviceOutOfTheBox()
        compose.setContent { LessLauncher(repository) }

        compose.onNodeWithText("Continue").performClick()

        compose.onNodeWithText("Make Less your launcher").assertExists()
        compose.runOnIdle { assertEquals(SetupStep.HomeRole, repository.settings.value.setupStep) }
    }

    @Test
    fun theHomeRoleStepAsksThePlatformForTheRole() {
        val repository = deviceOutOfTheBox()
        compose.setContent { LessLauncher(repository) }
        compose.onNodeWithText("Continue").performClick()

        compose.onNodeWithText("Set Less as default").performClick()

        compose.runOnIdle { assertEquals(1, repository.homeRoleRequests) }
    }

    @Test
    fun theHomeRoleStepStopsAskingOnceThePlatformHasAnswered() {
        val repository = deviceOutOfTheBox()
        compose.setContent { LessLauncher(repository) }
        compose.onNodeWithText("Continue").performClick()

        compose.runOnIdle { repository.holdHomeRole() }

        compose.onNodeWithText("Set Less as default").assertDoesNotExist()
        compose.onNodeWithText("Less is your launcher.").assertExists()
    }

    @Test
    fun theFavoritesPickerComesPreSeededWithTheAppsAnsweringTheEverydayIntents() {
        val repository = deviceOutOfTheBox()
        compose.setContent { LessLauncher(repository) }
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Continue").performClick()

        compose.onNodeWithText("Phone").assertIsOn()
        compose.onNodeWithText("Messages").assertIsOn()
        compose.onNodeWithText("Camera").assertIsOn()
        compose.onNodeWithText("Browser").assertIsOn()
        compose.onNodeWithText("Notes").assertIsOff()
    }

    @Test
    fun homeIsUsableStraightOutOfSetupWithNoEmptyState() {
        val repository = deviceOutOfTheBox()
        compose.setContent { LessLauncher(repository) }
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Continue").performClick()

        compose.onNodeWithText("Done").performClick()

        compose.runOnIdle {
            assertEquals(SetupStep.Done, repository.settings.value.setupStep)
            assertEquals(
                listOf(phone, messages, camera, browser).map(LauncherApp::id),
                repository.favorites.value.map(Favorite::appId),
            )
        }
        compose.onNodeWithText("Phone").assertExists()
        compose.onNodeWithText("Pick a look").assertDoesNotExist()
    }

    @Test
    fun anAppTheUserAddsInThePickerJoinsHome() {
        val repository = deviceOutOfTheBox()
        compose.setContent { LessLauncher(repository) }
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Continue").performClick()

        compose.onNodeWithText("Notes").performScrollTo().performClick()
        compose.onNodeWithText("Done").performClick()

        compose.runOnIdle {
            assertEquals(
                listOf(phone, messages, camera, browser, notes).map(LauncherApp::id),
                repository.favorites.value.map(Favorite::appId),
            )
        }
    }

    @Test
    fun anAppTheUserDropsInThePickerStaysOffHome() {
        val repository = deviceOutOfTheBox()
        compose.setContent { LessLauncher(repository) }
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Continue").performClick()

        compose.onNodeWithText("Camera").performScrollTo().performClick()
        compose.onNodeWithText("Done").performClick()

        compose.runOnIdle {
            assertEquals(
                listOf(phone, messages, browser).map(LauncherApp::id),
                repository.favorites.value.map(Favorite::appId),
            )
        }
    }

    @Test
    fun aStepCanBeGoneBackOver() {
        val repository = deviceOutOfTheBox()
        compose.setContent { LessLauncher(repository) }
        compose.onNodeWithText("Continue").performClick()

        compose.onNodeWithText("Back").performClick()

        compose.onNodeWithText("Pick a look").assertExists()
        compose.runOnIdle { assertEquals(SetupStep.Theme, repository.settings.value.setupStep) }
    }
}
