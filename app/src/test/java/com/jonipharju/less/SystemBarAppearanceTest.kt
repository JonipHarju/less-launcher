package com.jonipharju.less

import android.app.Activity
import android.view.Window
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.core.view.WindowCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jonipharju.less.launcher.FakeLauncherRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The system bars borrow the Theme's text colour for their icons and otherwise stay out of
 * the way: ADR-0001 has Less drawing over the Wallpaper, not a platform-owned band.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SystemBarAppearanceTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the navigation bar does not paint a contrast scrim of its own`() {
        val window = launchWith(FakeLauncherRepository().also { it.finishSetup() })

        compose.runOnIdle { assertFalse(window.isNavigationBarContrastEnforced) }
    }

    @Test
    fun `a light Theme keeps dark system bar icons`() {
        val repository = FakeLauncherRepository().also { it.finishSetup() }
        runBlocking { repository.updateSettings { it.copy(themeId = OffWhiteTheme.id) } }
        val window = launchWith(repository)

        compose.runOnIdle {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            assertTrue(controller.isAppearanceLightStatusBars)
            assertTrue(controller.isAppearanceLightNavigationBars)
        }
    }

    @Test
    fun `a dark Theme keeps light system bar icons`() {
        val window = launchWith(FakeLauncherRepository().also { it.finishSetup() })

        compose.runOnIdle {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            assertFalse(controller.isAppearanceLightStatusBars)
            assertFalse(controller.isAppearanceLightNavigationBars)
        }
    }

    @Test
    fun `switching Theme updates the bars without a restart`() {
        val repository = FakeLauncherRepository().also { it.finishSetup() }
        val window = launchWith(repository)

        compose.runOnIdle {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            assertFalse(controller.isAppearanceLightStatusBars)
        }

        compose.runOnIdle { runBlocking { repository.updateSettings { it.copy(themeId = OffWhiteTheme.id) } } }

        compose.runOnIdle {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            assertTrue(controller.isAppearanceLightStatusBars)
            assertTrue(controller.isAppearanceLightNavigationBars)
            assertFalse(window.isNavigationBarContrastEnforced)
        }
    }

    private fun launchWith(repository: FakeLauncherRepository): Window {
        lateinit var window: Window
        compose.setContent {
            window = (LocalView.current.context as Activity).window
            LessLauncher(repository)
        }
        return window
    }
}
