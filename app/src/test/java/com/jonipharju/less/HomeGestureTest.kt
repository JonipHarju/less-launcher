package com.jonipharju.less

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jonipharju.less.launcher.DrawerOpenDirection
import com.jonipharju.less.launcher.FakeLauncherRepository
import com.jonipharju.less.launcher.Favorite
import com.jonipharju.less.launcher.LauncherApp
import com.jonipharju.less.launcher.launcherAppFixture
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Home-level wiring of the detector: a drifting tap still launches, a scroll does not. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class HomeGestureTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `a tap that drifts less than touch slop launches the Favorite`() {
        val repository = homeWith("Clock")
        val clock = repository.installedApps.value.single()

        compose.onNodeWithText("Clock").performTouchInput {
            down(center)
            moveBy(Offset(0f, viewConfiguration.touchSlop / 2f))
            up()
        }

        compose.runOnIdle { assertEquals(listOf(clock), repository.launchedApps) }
    }

    @Test
    fun `a vertical drag before the long-press timeout launches nothing`() {
        val labels = (1..20).map { "App$it" }
        val repository = homeWith(*labels.toTypedArray())

        compose.onNodeWithText("App1").performTouchInput {
            down(center)
            moveBy(Offset(0f, viewConfiguration.touchSlop * 8f))
            up()
        }

        compose.runOnIdle { assertEquals(emptyList<LauncherApp>(), repository.launchedApps) }
    }

    @Test
    fun `swiping up opens the Drawer`() {
        val repository = FakeLauncherRepository()
        repository.finishSetup()
        repository.install(launcherAppFixture(label = "Clock"))
        compose.setContent { LessLauncher(repository) }

        compose.onRoot().performTouchInput { swipeUp() }

        compose.onNodeWithText("Clock").assertExists()
    }

    @Test
    fun `swiping down opens the Drawer when that is the chosen direction`() {
        runBlocking {
            val repository = FakeLauncherRepository()
            repository.finishSetup()
            repository.install(launcherAppFixture(label = "Clock"))
            repository.updateSettings { it.copy(drawerOpenDirection = DrawerOpenDirection.SwipeDown) }
            compose.setContent { LessLauncher(repository) }

            compose.onRoot().performTouchInput { swipeUp() }
            compose.onNodeWithText("Clock").assertDoesNotExist()

            compose.onRoot().performTouchInput { swipeDown() }
            compose.onNodeWithText("Clock").assertExists()
        }
    }

    @Test
    fun `when uninstall cannot be asked a dismissible message tells the user`() {
        val repository = homeWith("Clock")
        repository.uninstallsSucceed = false

        compose.onNodeWithText("Clock").performTouchInput { longClick() }
        compose.onNodeWithText("Uninstall").performClick()

        compose.onNodeWithText("Nothing on this phone can uninstall that app.").assertExists()
        compose.onNodeWithText("Got it").performClick()
        compose.onNodeWithText("Nothing on this phone can uninstall that app.").assertDoesNotExist()
    }

    /** Home showing one Favorite per label, in the order given, and nothing else. */
    private fun homeWith(vararg labels: String): FakeLauncherRepository {
        val repository = FakeLauncherRepository()
        runBlocking {
            labels.forEachIndexed { position, label ->
                val app = launcherAppFixture(label)
                repository.install(app)
                repository.chooseFavorite(Favorite(app.id, position = position))
            }
        }
        compose.setContent {
            Home(
                repository = repository,
                timeText = "14:35",
                dateText = "Tuesday, August 18, 2026",
                onOpenClock = {},
                onOpenCalendar = {},
                onOpenDrawer = {},
            )
        }
        return repository
    }
}
