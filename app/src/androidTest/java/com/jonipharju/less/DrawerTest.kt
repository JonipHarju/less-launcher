package com.jonipharju.less

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import com.jonipharju.less.launcher.FakeLauncherRepository
import com.jonipharju.less.launcher.launcherAppFixture
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DrawerTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun installedAppsAppearOnlyAfterOpeningDrawer() {
        val repository = FakeLauncherRepository()
        repository.install(launcherAppFixture(label = "Clock"))
        compose.setContent { LessLauncher(repository) }

        compose.onNodeWithText("Clock").assertDoesNotExist()
        compose.onRoot().performTouchInput { swipeUp() }
        compose.onNodeWithText("Clock").assertExists()
    }

    @Test
    fun backDismissesDrawerToHome() {
        val repository = FakeLauncherRepository()
        repository.install(launcherAppFixture(label = "Clock"))
        lateinit var backDispatcher: OnBackPressedDispatcher
        compose.setContent {
            backDispatcher =
                checkNotNull(LocalOnBackPressedDispatcherOwner.current).onBackPressedDispatcher
            LessLauncher(repository)
        }
        compose.onRoot().performTouchInput { swipeUp() }

        compose.runOnIdle { backDispatcher.onBackPressed() }

        compose.onNodeWithText("Clock").assertDoesNotExist()
    }

    @Test
    fun installEventAddsAppToVisibleList() {
        val repository = FakeLauncherRepository()
        val app = launcherAppFixture(label = "Clock")
        compose.setContent { Drawer(repository) }

        compose.runOnIdle { repository.install(app) }

        compose.onNodeWithText("Clock").assertExists()
    }

    @Test
    fun uninstallEventRemovesAppFromVisibleList() {
        val repository = FakeLauncherRepository()
        val app = launcherAppFixture(label = "Clock")
        repository.install(app)
        compose.setContent { Drawer(repository) }

        compose.runOnIdle { repository.uninstall(app.id) }

        compose.onNodeWithText("Clock").assertDoesNotExist()
    }

    @Test
    fun updateEventReplacesAppInVisibleList() {
        val repository = FakeLauncherRepository()
        val app = launcherAppFixture(label = "Clock")
        repository.install(app)
        compose.setContent { Drawer(repository) }

        compose.runOnIdle { repository.update(app.copy(label = "Alarm Clock")) }

        compose.onNodeWithText("Clock").assertDoesNotExist()
        compose.onNodeWithText("Alarm Clock").assertExists()
    }

    @Test
    fun tapLaunchesApp() {
        val repository = FakeLauncherRepository()
        val app = launcherAppFixture(label = "Clock")
        repository.install(app)
        compose.setContent { Drawer(repository) }

        compose.onNodeWithText("Clock").performClick()

        assertEquals(listOf(app), repository.launchedApps)
    }
}
