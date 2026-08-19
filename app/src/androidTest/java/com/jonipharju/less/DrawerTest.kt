package com.jonipharju.less

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
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
import com.jonipharju.less.launcher.Favorite
import com.jonipharju.less.launcher.FavoritesSoftCap
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
    fun longPressOffersPinAppInfoAndUninstall() {
        val repository = FakeLauncherRepository()
        repository.install(launcherAppFixture(label = "Clock"))
        compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }

        compose.onNodeWithText("Clock").performTouchInput { longClick() }

        compose.onNodeWithText("Pin to Home").assertExists()
        compose.onNodeWithText("App info").assertExists()
        compose.onNodeWithText("Uninstall").assertExists()
    }

    @Test
    fun pinningPutsTheAppOnHome() {
        val repository = FakeLauncherRepository()
        val clock = launcherAppFixture(label = "Clock")
        repository.install(clock)
        compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }

        compose.onNodeWithText("Clock").performTouchInput { longClick() }
        compose.onNodeWithText("Pin to Home").performClick()

        compose.runOnIdle {
            assertEquals(listOf(Favorite(clock.id, position = 0)), repository.favorites.value)
        }
    }

    @Test
    fun pinIsNotOfferedForAnAppAlreadyOnHome() {
        runBlocking {
            val repository = FakeLauncherRepository()
            val clock = launcherAppFixture(label = "Clock")
            repository.install(clock)
            repository.chooseFavorite(Favorite(clock.id, position = 0))
            compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }

            compose.onNodeWithText("Clock").performTouchInput { longClick() }

            compose.onNodeWithText("Pin to Home").assertDoesNotExist()
            compose.onNodeWithText("App info").assertExists()
        }
    }

    @Test
    fun pinningANinthFavoriteWarnsAndStillPins() {
        runBlocking {
            val repository = FakeLauncherRepository()
            repeat(FavoritesSoftCap) { index ->
                val app = launcherAppFixture(label = "App$index")
                repository.install(app)
                repository.chooseFavorite(Favorite(app.id, position = index))
            }
            val ninth = launcherAppFixture(label = "Ninth")
            repository.install(ninth)
            compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }

            compose.onNodeWithText("Ninth").performTouchInput { longClick() }
            compose.onNodeWithText("Pin to Home").performClick()

            compose.onNodeWithText("More than 8 Favorites. Home takes them and starts to scroll.").assertExists()
            compose.runOnIdle {
                assertEquals(FavoritesSoftCap + 1, repository.favorites.value.size)
                assertEquals(
                    ninth.id,
                    repository.favorites.value
                        .last()
                        .appId,
                )
            }
        }
    }

    @Test
    fun pinningAnEighthFavoriteDoesNotWarn() {
        runBlocking {
            val repository = FakeLauncherRepository()
            repeat(FavoritesSoftCap - 1) { index ->
                val app = launcherAppFixture(label = "App$index")
                repository.install(app)
                repository.chooseFavorite(Favorite(app.id, position = index))
            }
            val eighth = launcherAppFixture(label = "Eighth")
            repository.install(eighth)
            compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }

            compose.onNodeWithText("Eighth").performTouchInput { longClick() }
            compose.onNodeWithText("Pin to Home").performClick()

            compose
                .onNodeWithText("More than 8 Favorites. Home takes them and starts to scroll.")
                .assertDoesNotExist()
            compose.runOnIdle { assertEquals(FavoritesSoftCap, repository.favorites.value.size) }
        }
    }

    @Test
    fun searchMatchesARenamedFavoritesCustomLabel() {
        runBlocking {
            val repository = FakeLauncherRepository()
            val messages = launcherAppFixture(label = "Messages")
            repository.install(messages)
            repository.install(launcherAppFixture(label = "Maps"))
            repository.chooseFavorite(Favorite(messages.id, position = 0, customLabel = "Texts"))
            compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }

            compose.onNodeWithContentDescription("Search apps").performTextInput("texts")

            compose.onNodeWithText("Messages").assertExists()
            compose.onNodeWithText("Maps").assertDoesNotExist()
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

    @Test
    fun longPressOffersToHideAnApp() {
        val repository = FakeLauncherRepository()
        repository.install(launcherAppFixture(label = "Clock"))
        compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }

        compose.onNodeWithText("Clock").performTouchInput { longClick() }

        compose.onNodeWithText("Hide").assertExists()
    }

    @Test
    fun hidingAnAppTakesItOutOfTheDrawer() {
        val repository = FakeLauncherRepository()
        val clock = launcherAppFixture(label = "Clock")
        repository.install(clock)
        repository.install(launcherAppFixture(label = "Camera"))
        compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }

        compose.onNodeWithText("Clock").performTouchInput { longClick() }
        compose.onNodeWithText("Hide").performClick()

        compose.onNodeWithText("Clock").assertDoesNotExist()
        compose.onNodeWithText("Camera").assertExists()
        compose.runOnIdle { assertEquals(setOf(clock.id), repository.hiddenApps.value) }
    }

    @Test
    fun aHiddenAppIsNotFoundBySearch() {
        runBlocking {
            val repository = FakeLauncherRepository()
            val clock = launcherAppFixture(label = "Clock")
            repository.install(clock)
            repository.install(launcherAppFixture(label = "Clock Radio"))
            repository.hideApp(clock.id)
            compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }

            compose.onNodeWithContentDescription("Search apps").performTextInput("clock")

            compose.onNodeWithText("Clock").assertDoesNotExist()
            compose.onNodeWithText("Clock Radio").assertExists()
        }
    }

    @Test
    fun enterLaunchesTheTopResultAmongTheAppsStillListed() {
        runBlocking {
            val repository = FakeLauncherRepository()
            val clock = launcherAppFixture(label = "Clock")
            val clockRadio = launcherAppFixture(label = "Clock Radio")
            repository.install(clock)
            repository.install(clockRadio)
            repository.hideApp(clock.id)
            compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }

            compose.onNodeWithContentDescription("Search apps").performTextInput("clock")
            compose.onNodeWithContentDescription("Search apps").performImeAction()

            assertEquals(listOf(clockRadio), repository.launchedApps)
        }
    }

    @Test
    fun unhidingAnAppPutsItBackInTheDrawer() {
        runBlocking {
            val repository = FakeLauncherRepository()
            val clock = launcherAppFixture(label = "Clock")
            repository.install(clock)
            repository.hideApp(clock.id)
            compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }
            compose.onNodeWithText("Clock").assertDoesNotExist()

            compose.runOnIdle { runBlocking { repository.unhideApp(clock.id) } }

            compose.onNodeWithText("Clock").assertExists()
        }
    }

    @Test
    fun hidingAFavoriteLeavesItOnHome() {
        runBlocking {
            val repository = FakeLauncherRepository()
            val clock = launcherAppFixture(label = "Clock")
            repository.install(clock)
            repository.chooseFavorite(Favorite(clock.id, position = 0))
            compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }

            compose.onNodeWithText("Clock").performTouchInput { longClick() }
            compose.onNodeWithText("Hide").performClick()

            compose.runOnIdle {
                assertEquals(listOf(Favorite(clock.id, position = 0)), repository.favorites.value)
                assertEquals(setOf(clock.id), repository.hiddenApps.value)
            }
        }
    }
}
