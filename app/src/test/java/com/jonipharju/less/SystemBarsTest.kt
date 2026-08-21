package com.jonipharju.less

import android.app.Activity
import android.graphics.Color
import android.view.Window
import android.view.WindowInsetsController
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jonipharju.less.launcher.FakeLauncherRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The Theme controls the system bars as well as the launcher surface behind them. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SystemBarsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `Theme keeps the navigation bar transparent and updates its icons`() {
        val repository = FakeLauncherRepository().apply { finishSetup() }
        lateinit var window: Window
        compose.setContent {
            val view = LocalView.current
            SideEffect { window = (view.context as Activity).window }
            LessLauncher(repository)
        }

        compose.runOnIdle {
            assertEquals(Color.TRANSPARENT, window.navigationBarColor)
            assertFalse(window.isNavigationBarContrastEnforced)
            assertFalse(window.systemBarsAppearance has WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
            assertFalse(window.systemBarsAppearance has WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
            runBlocking { repository.chooseTheme(OffWhiteTheme.id, OffWhiteTheme.wallpaperAsset) }
        }

        compose.runOnIdle {
            assertEquals(Color.TRANSPARENT, window.navigationBarColor)
            assertFalse(window.isNavigationBarContrastEnforced)
            assertTrue(window.systemBarsAppearance has WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
            assertTrue(window.systemBarsAppearance has WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
        }
    }
}

private infix fun Int.has(flag: Int): Boolean = this and flag != 0

private val Window.systemBarsAppearance: Int
    get() = checkNotNull(decorView.windowInsetsController).systemBarsAppearance
