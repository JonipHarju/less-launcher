package com.jonipharju.less

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jonipharju.less.launcher.FakeLauncherRepository
import com.jonipharju.less.launcher.launcherAppFixture
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The standing offer in the Drawer to become the default launcher, and the end of it. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class DefaultLauncherPromptTest {
    @get:Rule
    val compose = createComposeRule()

    private val prompt = "Make Less your default launcher"

    private fun drawerOf(repository: FakeLauncherRepository) {
        repository.finishSetup()
        repository.install(launcherAppFixture(label = "Clock"))
        compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }
    }

    @Test
    fun theDrawerAsksWhileLessIsNotTheDefaultLauncher() {
        drawerOf(FakeLauncherRepository())

        compose.onNodeWithText(prompt).assertExists()
    }

    @Test
    fun thePromptAsksThePlatformRatherThanExplainingHow() {
        val repository = FakeLauncherRepository()
        drawerOf(repository)

        compose.onNodeWithText(prompt).performClick()

        compose.runOnIdle { assertEquals(1, repository.homeRoleRequests) }
    }

    @Test
    fun thePromptGoesOnceLessHoldsTheHomeRole() {
        val repository = FakeLauncherRepository()
        drawerOf(repository)
        compose.onNodeWithText(prompt).assertExists()

        compose.runOnIdle { repository.holdHomeRole() }

        compose.onNodeWithText(prompt).assertDoesNotExist()
    }

    @Test
    fun thePromptStaysGoneAfterTheRoleIsHandedToAnotherLauncher() {
        val repository = FakeLauncherRepository()
        drawerOf(repository)
        compose.runOnIdle { repository.holdHomeRole() }
        compose.onNodeWithText(prompt).assertDoesNotExist()

        compose.runOnIdle { repository.releaseHomeRole() }

        compose.onNodeWithText(prompt).assertDoesNotExist()
    }

    @Test
    fun aLauncherThatAlreadyHoldsTheRoleNeverAsks() {
        val repository = FakeLauncherRepository()
        repository.holdHomeRole()

        drawerOf(repository)

        compose.onNodeWithText(prompt).assertDoesNotExist()
    }
}
