package com.jonipharju.less

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
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

/**
 * The Drawer Open Direction is stored, so each of its values is driven here: the swipe that
 * opens the Drawer, the one that does not, and the inverse that closes it again. Camera is in
 * the Drawer and not on Home, so its label is the Drawer being open.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class DrawerOpenDirectionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `swipe up over a Favorite opens the Drawer and launches nothing when it is the stored direction`() {
        val repository = lessOpeningBy(DrawerOpenDirection.SwipeUp)

        swipeOverTheFavorite(rows = -2f)

        compose.onNodeWithText("Camera").assertExists()
        compose.runOnIdle { assertEquals(emptyList<LauncherApp>(), repository.launchedApps) }
    }

    @Test
    fun `swipe down over a Favorite opens the Drawer and launches nothing when it is the stored direction`() {
        val repository = lessOpeningBy(DrawerOpenDirection.SwipeDown)

        swipeOverTheFavorite(rows = 2f)

        compose.onNodeWithText("Camera").assertExists()
        compose.runOnIdle { assertEquals(emptyList<LauncherApp>(), repository.launchedApps) }
    }

    @Test
    fun `swipe down does not open the Drawer when swipe up is stored`() {
        val repository = lessOpeningBy(DrawerOpenDirection.SwipeUp)

        swipeOverTheFavorite(rows = 2f)

        compose.onNodeWithText("Camera").assertDoesNotExist()
        compose.runOnIdle { assertEquals(emptyList<LauncherApp>(), repository.launchedApps) }
    }

    @Test
    fun `swipe up does not open the Drawer when swipe down is stored`() {
        val repository = lessOpeningBy(DrawerOpenDirection.SwipeDown)

        swipeOverTheFavorite(rows = -2f)

        compose.onNodeWithText("Camera").assertDoesNotExist()
        compose.runOnIdle { assertEquals(emptyList<LauncherApp>(), repository.launchedApps) }
    }

    @Test
    fun `swipe down closes a Drawer that swipe up opened`() {
        lessOpeningBy(DrawerOpenDirection.SwipeUp)
        compose.onRoot().performTouchInput { swipeUp() }
        compose.onNodeWithText("Camera").assertExists()

        compose.onRoot().performTouchInput { swipeDown() }

        compose.onNodeWithText("Camera").assertDoesNotExist()
    }

    @Test
    fun `swipe up closes a Drawer that swipe down opened`() {
        lessOpeningBy(DrawerOpenDirection.SwipeDown)
        compose.onRoot().performTouchInput { swipeDown() }
        compose.onNodeWithText("Camera").assertExists()

        compose.onRoot().performTouchInput { swipeUp() }

        compose.onNodeWithText("Camera").assertDoesNotExist()
    }

    /** Less past Setup, Clock its one Favorite, Camera in the Drawer, opening by [direction]. */
    private fun lessOpeningBy(direction: DrawerOpenDirection): FakeLauncherRepository {
        val repository = FakeLauncherRepository()
        val clock = launcherAppFixture("Clock")
        repository.install(clock)
        repository.install(launcherAppFixture("Camera"))
        runBlocking {
            repository.chooseFavorite(Favorite(clock.id, position = 0))
            repository.updateSettings { it.copy(drawerOpenDirection = direction) }
        }
        repository.finishSetup()

        compose.setContent { LessLauncher(repository) }
        return repository
    }

    /** A swipe that starts on the Favorite and travels [rows] rows, upwards when negative. */
    private fun swipeOverTheFavorite(rows: Float) {
        compose.onNodeWithText("Clock").performTouchInput {
            down(center)
            moveBy(Offset(0f, FavoriteRowHeight.toPx() * rows))
            up()
        }
    }
}
