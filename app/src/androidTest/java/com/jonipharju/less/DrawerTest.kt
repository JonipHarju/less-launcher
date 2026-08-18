package com.jonipharju.less

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import com.jonipharju.less.launcher.DrawerOpenDirection
import com.jonipharju.less.launcher.FakeLauncherRepository
import com.jonipharju.less.launcher.launcherAppFixture
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
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
        compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }

        compose.runOnIdle { repository.install(app) }

        compose.onNodeWithText("Clock").assertExists()
    }

    @Test
    fun uninstallEventRemovesAppFromVisibleList() {
        val repository = FakeLauncherRepository()
        val app = launcherAppFixture(label = "Clock")
        repository.install(app)
        compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }

        compose.runOnIdle { repository.uninstall(app.id) }

        compose.onNodeWithText("Clock").assertDoesNotExist()
    }

    @Test
    fun updateEventReplacesAppInVisibleList() {
        val repository = FakeLauncherRepository()
        val app = launcherAppFixture(label = "Clock")
        repository.install(app)
        compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }

        compose.runOnIdle { repository.update(app.copy(label = "Alarm Clock")) }

        compose.onNodeWithText("Clock").assertDoesNotExist()
        compose.onNodeWithText("Alarm Clock").assertExists()
    }

    @Test
    fun tapLaunchesApp() {
        val repository = FakeLauncherRepository()
        val app = launcherAppFixture(label = "Clock")
        repository.install(app)
        compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }

        compose.onNodeWithText("Clock").performClick()

        assertEquals(listOf(app), repository.launchedApps)
    }

    @Test
    fun searchIsFocusedAndFiltersWithoutLaunching() {
        val repository = FakeLauncherRepository()
        repository.install(launcherAppFixture(label = "Camera"))
        repository.install(launcherAppFixture(label = "Clock"))
        compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }

        compose.onNodeWithContentDescription("Search apps").assertIsFocused().performTextInput("clo")

        compose.onNodeWithText("Camera").assertDoesNotExist()
        compose.onNodeWithText("Clock").assertExists()
        assertEquals(emptyList<Any>(), repository.launchedApps)
    }

    @Test
    fun enterLaunchesTopSearchResult() {
        val repository = FakeLauncherRepository()
        val clock = launcherAppFixture(label = "Clock")
        repository.install(launcherAppFixture(label = "Clock Radio"))
        repository.install(clock)
        compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }

        compose.onNodeWithContentDescription("Search apps").performTextInput("clock")
        compose.onNodeWithContentDescription("Search apps").performImeAction()

        assertEquals(listOf(clock), repository.launchedApps)
    }

    @Test
    fun reopeningDrawerClearsQuery() {
        val repository = FakeLauncherRepository()
        repository.install(launcherAppFixture(label = "Camera"))
        repository.install(launcherAppFixture(label = "Clock"))
        lateinit var backDispatcher: OnBackPressedDispatcher
        compose.setContent {
            backDispatcher =
                checkNotNull(LocalOnBackPressedDispatcherOwner.current).onBackPressedDispatcher
            LessLauncher(repository)
        }
        compose.onRoot().performTouchInput { swipeUp() }
        compose.onNodeWithContentDescription("Search apps").performTextInput("clo")
        compose.onNodeWithText("Camera").assertDoesNotExist()

        compose.runOnIdle { backDispatcher.onBackPressed() }
        compose.onRoot().performTouchInput { swipeUp() }

        compose.onNodeWithText("Camera").assertExists()
        compose.onNodeWithText("Clock").assertExists()
    }

    @Test
    fun closeControlDismissesDrawerToHome() {
        val repository = FakeLauncherRepository()
        repository.install(launcherAppFixture(label = "Clock"))
        compose.setContent { LessLauncher(repository) }
        compose.onRoot().performTouchInput { swipeUp() }

        compose.onNodeWithContentDescription("Close Drawer").performClick()

        compose.onNodeWithText("Clock").assertDoesNotExist()
    }

    @Test
    fun inverseSwipeDismissesDrawerToHome() {
        val repository = FakeLauncherRepository()
        repository.install(launcherAppFixture(label = "Clock"))
        compose.setContent { LessLauncher(repository) }
        compose.onRoot().performTouchInput { swipeUp() }
        compose.onNodeWithText("Clock").assertExists()

        compose.onRoot().performTouchInput { swipeDown() }

        compose.onNodeWithText("Clock").assertDoesNotExist()
    }

    @Test
    fun theStoredDirectionDecidesWhichSwipeOpensTheDrawer() {
        runBlocking {
            val repository = FakeLauncherRepository()
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
    fun turningOffTheKeyboardLeavesSearchUnfocused() {
        runBlocking {
            val repository = FakeLauncherRepository()
            repository.install(launcherAppFixture(label = "Clock"))
            repository.updateSettings { it.copy(opensKeyboardWithDrawer = false) }

            compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }

            compose.onNodeWithContentDescription("Search apps").assertIsNotFocused()
        }
    }

    @Test
    fun gearOpensSettings() {
        val repository = FakeLauncherRepository()
        compose.setContent { LessLauncher(repository) }
        compose.onRoot().performTouchInput { swipeUp() }

        compose.onNodeWithContentDescription("Open Settings").performClick()

        compose.onNodeWithText("Home alignment").assertExists()
    }

    @Test
    fun backFromSettingsReturnsToTheDrawer() {
        val repository = FakeLauncherRepository()
        repository.install(launcherAppFixture(label = "Clock"))
        lateinit var backDispatcher: OnBackPressedDispatcher
        compose.setContent {
            backDispatcher =
                checkNotNull(LocalOnBackPressedDispatcherOwner.current).onBackPressedDispatcher
            LessLauncher(repository)
        }
        compose.onRoot().performTouchInput { swipeUp() }
        compose.onNodeWithContentDescription("Open Settings").performClick()

        compose.runOnIdle { backDispatcher.onBackPressed() }

        compose.onNodeWithText("Clock").assertExists()
    }

    @Test
    fun pressingHomeReturnsToHomeRatherThanTheDrawer() {
        val repository = FakeLauncherRepository()
        repository.install(launcherAppFixture(label = "Clock"))
        val homeRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        compose.setContent { LessLauncher(repository, homeRequests = homeRequests) }
        compose.onRoot().performTouchInput { swipeUp() }
        compose.onNodeWithText("Clock").assertExists()

        compose.runOnIdle { homeRequests.tryEmit(Unit) }

        compose.onNodeWithText("Clock").assertDoesNotExist()
    }
}
