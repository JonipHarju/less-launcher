package com.jonipharju.less

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jonipharju.less.launcher.FakeLauncherRepository
import com.jonipharju.less.launcher.launcherAppFixture
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Drawer wiring of the auto-launch rule: the search field is what feeds the query. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class DrawerAutoLaunchTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `typing a unique match of three characters launches that app`() {
        val repository = FakeLauncherRepository()
        val clock = launcherAppFixture(label = "Clock")
        repository.install(launcherAppFixture(label = "Camera"))
        repository.install(clock)
        compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }

        compose.onNodeWithContentDescription("Search apps").performTextInput("clo")

        assertEquals(listOf(clock), repository.launchedApps)
    }

    @Test
    fun `typing a unique match of two characters launches nothing`() {
        val repository = FakeLauncherRepository()
        val clock = launcherAppFixture(label = "Clock")
        repository.install(launcherAppFixture(label = "Camera"))
        repository.install(clock)
        compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }

        compose.onNodeWithContentDescription("Search apps").performTextInput("cl")

        assertEquals(emptyList<Any>(), repository.launchedApps)
    }

    @Test
    fun `the search action still launches the top result at two characters`() {
        val repository = FakeLauncherRepository()
        val clock = launcherAppFixture(label = "Clock")
        repository.install(launcherAppFixture(label = "Clock Radio"))
        repository.install(clock)
        compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }

        compose.onNodeWithContentDescription("Search apps").performTextInput("cl")
        compose.onNodeWithContentDescription("Search apps").performImeAction()

        assertEquals(listOf(clock), repository.launchedApps)
    }
}
