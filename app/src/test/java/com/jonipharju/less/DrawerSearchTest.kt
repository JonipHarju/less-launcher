package com.jonipharju.less

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jonipharju.less.launcher.FakeLauncherRepository
import com.jonipharju.less.launcher.LauncherApp
import com.jonipharju.less.launcher.launcherAppFixture
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The Drawer's search field, driven a character at a time the way a keyboard drives it. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class DrawerSearchTest {
    @get:Rule
    val compose = createComposeRule()

    private val clock = launcherAppFixture("Clock")
    private val camera = launcherAppFixture("Camera")

    @Test
    fun `typing on past the sole match opens it once`() {
        val repository = drawerWith(clock, camera)

        type("c", "l", "o")
        compose.runOnIdle { assertEquals(listOf(clock), repository.launchedApps) }

        type("c", "k")
        compose.runOnIdle { assertEquals(listOf(clock), repository.launchedApps) }
        compose.onNodeWithText("Clock").assertExists()
    }

    @Test
    fun `a query short of three characters opens nothing however few apps match`() {
        val repository = drawerWith(clock, camera)

        type("c", "l")

        compose.runOnIdle { assertEquals(emptyList<LauncherApp>(), repository.launchedApps) }
    }

    private fun type(vararg characters: String) {
        characters.forEach { character ->
            compose.onNodeWithContentDescription("Search apps").performTextInput(character)
        }
    }

    private fun drawerWith(vararg apps: LauncherApp): FakeLauncherRepository {
        val repository = FakeLauncherRepository()
        apps.forEach(repository::install)
        compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }
        return repository
    }
}
