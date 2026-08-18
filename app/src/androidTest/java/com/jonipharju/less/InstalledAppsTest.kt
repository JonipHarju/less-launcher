package com.jonipharju.less

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jonipharju.less.launcher.FakeLauncherRepository
import com.jonipharju.less.launcher.launcherAppFixture
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class InstalledAppsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun installEventAddsAppToVisibleList() {
        val repository = FakeLauncherRepository()
        val app = launcherAppFixture(label = "Clock")
        compose.setContent { InstalledApps(repository) }

        compose.runOnIdle { repository.install(app) }

        compose.onNodeWithText("Clock").assertExists()
    }

    @Test
    fun uninstallEventRemovesAppFromVisibleList() {
        val repository = FakeLauncherRepository()
        val app = launcherAppFixture(label = "Clock")
        repository.install(app)
        compose.setContent { InstalledApps(repository) }

        compose.runOnIdle { repository.uninstall(app.id) }

        compose.onNodeWithText("Clock").assertDoesNotExist()
    }

    @Test
    fun updateEventReplacesAppInVisibleList() {
        val repository = FakeLauncherRepository()
        val app = launcherAppFixture(label = "Clock")
        repository.install(app)
        compose.setContent { InstalledApps(repository) }

        compose.runOnIdle { repository.update(app.copy(label = "Alarm Clock")) }

        compose.onNodeWithText("Clock").assertDoesNotExist()
        compose.onNodeWithText("Alarm Clock").assertExists()
    }

    @Test
    fun tapLaunchesApp() {
        val repository = FakeLauncherRepository()
        val app = launcherAppFixture(label = "Clock")
        repository.install(app)
        compose.setContent { InstalledApps(repository) }

        compose.onNodeWithText("Clock").performClick()

        assertEquals(listOf(app), repository.launchedApps)
    }
}
