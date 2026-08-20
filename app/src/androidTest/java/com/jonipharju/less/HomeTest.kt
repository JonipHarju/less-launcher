package com.jonipharju.less

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.jonipharju.less.launcher.AppIcon
import com.jonipharju.less.launcher.FakeLauncherRepository
import com.jonipharju.less.launcher.Favorite
import com.jonipharju.less.launcher.HomeAlignment
import com.jonipharju.less.launcher.IconMode
import com.jonipharju.less.launcher.LauncherApp
import com.jonipharju.less.launcher.ShownFavorite
import com.jonipharju.less.launcher.launcherAppFixture
import com.jonipharju.less.launcher.shownAmong
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun rendersClockDateAndFavoritesInPositionOrder() {
        runBlocking {
            val repository = FakeLauncherRepository()
            val clock = launcherAppFixture("Clock")
            val calendar = launcherAppFixture("Calendar")
            repository.install(clock)
            repository.install(calendar)
            repository.chooseFavorite(Favorite(clock.id, position = 1, customLabel = "Time"))
            repository.chooseFavorite(Favorite(calendar.id, position = 0))

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

            compose.onNodeWithText("14:35").assertExists()
            compose.onNodeWithText("Tuesday, August 18, 2026").assertExists()
            compose.onNodeWithText("Calendar").assertExists()
            compose.onNodeWithText("Time").assertExists()
        }
    }

    @Test
    fun aTapThatDriftsLessThanTouchSlopStillLaunches() {
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
    fun aDragBeforeTheLongPressTimeoutScrollsHomeAndLaunchesNothing() {
        val labels = (1..20).map { "App$it" }
        val repository = homeWith(*labels.toTypedArray())
        val beforeDrag = compose.onNodeWithText("App1").getUnclippedBoundsInRoot().top

        compose.onNodeWithText("App1").performTouchInput {
            down(center)
            moveBy(Offset(0f, -viewConfiguration.touchSlop * 8f))
            up()
        }

        compose.runOnIdle { assertEquals(emptyList<LauncherApp>(), repository.launchedApps) }
        val afterDrag = compose.onNodeWithText("App1").getUnclippedBoundsInRoot().top
        assertTrue("Expected $afterDrag above $beforeDrag", afterDrag < beforeDrag)
    }

    @Test
    fun theDrawerOpenDirectionOnAFavoriteOpensTheDrawerAndLaunchesNothing() {
        var drawerOpens = 0
        val repository = homeWith("Clock", onOpenDrawer = { drawerOpens++ })

        compose.onNodeWithText("Clock").performTouchInput {
            down(center)
            moveBy(Offset(0f, -FavoriteRowHeight.toPx() * 1.5f))
            up()
        }

        compose.runOnIdle {
            assertEquals(1, drawerOpens)
            assertEquals(emptyList<LauncherApp>(), repository.launchedApps)
        }
    }

    @Test
    fun tappingATombstoneLaunchesNothingAndALongPressOpensCuration() {
        val repository = homeWith("Clock")
        val clock = repository.installedApps.value.single()
        compose.runOnIdle { repository.makeUnavailable(clock.id) }

        compose.onNodeWithText("Clock").performTouchInput {
            down(center)
            moveBy(Offset(0f, viewConfiguration.touchSlop / 2f))
            up()
        }
        compose.runOnIdle { assertEquals(emptyList<LauncherApp>(), repository.launchedApps) }

        compose.onNodeWithText("Clock").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveBy(Offset(0f, 1f))
            up()
        }
        compose.onNodeWithText("Dismiss Tombstone").assertExists()
    }

    @Test
    fun tapsOpenSystemAppsAndLaunchFavorite() {
        runBlocking {
            val repository = FakeLauncherRepository()
            val clock = launcherAppFixture("Clock")
            repository.install(clock)
            repository.chooseFavorite(Favorite(clock.id, position = 0))
            var clockOpens = 0
            var calendarOpens = 0
            compose.setContent {
                Home(
                    repository = repository,
                    timeText = "14:35",
                    dateText = "Tuesday, August 18, 2026",
                    onOpenClock = { clockOpens++ },
                    onOpenCalendar = { calendarOpens++ },
                    onOpenDrawer = {},
                )
            }

            compose.onNodeWithText("14:35").performClick()
            compose.onNodeWithText("Tuesday, August 18, 2026").performClick()
            compose.onNodeWithText("Clock").performClick()

            assertEquals(1, clockOpens)
            assertEquals(1, calendarOpens)
            assertEquals(listOf(clock), repository.launchedApps)
        }
    }

    @Test
    fun showsNoInstalledAppsAndNoSearchField() {
        val repository = FakeLauncherRepository()
        repository.install(launcherAppFixture("Camera"))

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

        compose.onNodeWithText("Camera").assertDoesNotExist()
        compose.onNodeWithContentDescription("Search apps").assertDoesNotExist()
    }

    @Test
    fun longPressOffersRenameUnpinAppInfoAndUninstall() {
        homeWith("Clock")

        compose.onNodeWithText("Clock").performTouchInput { longClick() }

        compose.onNodeWithText("Rename").assertExists()
        compose.onNodeWithText("Unpin from Home").assertExists()
        compose.onNodeWithText("App info").assertExists()
        compose.onNodeWithText("Uninstall").assertExists()
    }

    @Test
    fun unpinningTakesTheFavoriteOffHome() {
        homeWith("Clock", "Camera")

        compose.onNodeWithText("Clock").performTouchInput { longClick() }
        compose.onNodeWithText("Unpin from Home").performClick()

        compose.onNodeWithText("Clock").assertDoesNotExist()
        compose.onNodeWithText("Camera").assertExists()
    }

    @Test
    fun anUnavailableFavoriteIsInertDismissibleAndRestoresInPlace() {
        val repository = homeWith("Clock", "Camera")
        val clock = repository.installedApps.value.first { it.label == "Clock" }

        compose.runOnIdle { repository.makeUnavailable(clock.id) }
        compose.onNodeWithText("Clock").assertExists().performClick()
        assertEquals(emptyList<LauncherApp>(), repository.launchedApps)

        compose.runOnIdle { repository.makeAvailable(clock) }
        compose.onNodeWithText("Clock").performClick()
        assertEquals(listOf(clock), repository.launchedApps)

        compose.runOnIdle { repository.makeUnavailable(clock.id) }
        compose.onNodeWithText("Clock").performTouchInput { longClick() }
        compose.onNodeWithText("Dismiss Tombstone").performClick()
        compose.onNodeWithText("Clock").assertDoesNotExist()
        compose.onNodeWithText("Camera").assertExists()
    }

    @Test
    fun appInfoAndUninstallAreAskedOfTheSystem() {
        val repository = homeWith("Clock")
        val clock = repository.installedApps.value.single()

        compose.onNodeWithText("Clock").performTouchInput { longClick() }
        compose.onNodeWithText("App info").performClick()
        compose.onNodeWithText("Clock").performTouchInput { longClick() }
        compose.onNodeWithText("Uninstall").performClick()

        assertEquals(listOf(clock.id), repository.appInfoShownFor)
        assertEquals(listOf(clock.id), repository.uninstallsRequestedFor)
    }

    @Test
    fun aRenamedFavoriteShowsItsCustomLabelOnHome() {
        homeWith("Clock")

        compose.onNodeWithText("Clock").performTouchInput { longClick() }
        compose.onNodeWithText("Rename").performClick()
        compose.onNodeWithContentDescription("Name for Clock").performTextClearance()
        compose.onNodeWithContentDescription("Name for Clock").performTextInput("Time")
        compose.onNodeWithText("Save").performClick()

        compose.onNodeWithText("Time").assertExists()
        compose.onNodeWithText("Clock").assertDoesNotExist()
    }

    @Test
    fun emptyingTheNameHandsBackTheAppsOwnName() {
        val repository = homeWith("Clock")
        compose.runOnIdle {
            runBlocking {
                repository.chooseFavorite(
                    repository.favorites.value
                        .single()
                        .copy(customLabel = "Time"),
                )
            }
        }

        compose.onNodeWithText("Time").performTouchInput { longClick() }
        compose.onNodeWithText("Rename").performClick()
        compose.onNodeWithContentDescription("Name for Clock").performTextClearance()
        compose.onNodeWithText("Save").performClick()

        compose.onNodeWithText("Clock").assertExists()
    }

    @Test
    fun draggingAFavoriteMovesItAndTheOrderPersists() {
        val repository = homeWith("Clock", "Camera", "Maps")

        compose.onNodeWithText("Clock").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveBy(Offset(0f, FavoriteRowHeight.toPx() * 1.5f))
            up()
        }

        compose.runOnIdle {
            assertEquals(
                listOf("Camera", "Clock", "Maps"),
                repository.favorites.value.labelsAmong(repository.installedApps.value),
            )
        }
    }

    @Test
    fun aDragCarriesOnPastTheFirstRowItMoves() {
        val repository = homeWith("Clock", "Camera", "Maps")

        // Two separate moves, so the second is delivered after the first has already
        // rearranged Home. A row that lost its touch when it moved would stop after one.
        compose.onNodeWithText("Clock").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveBy(Offset(0f, FavoriteRowHeight.toPx() * 1.2f))
        }
        compose.waitForIdle()
        compose.onNodeWithText("Clock").performTouchInput {
            moveBy(Offset(0f, FavoriteRowHeight.toPx() * 1.2f))
            up()
        }

        compose.runOnIdle {
            assertEquals(
                listOf("Camera", "Maps", "Clock"),
                repository.favorites.value.labelsAmong(repository.installedApps.value),
            )
        }
    }

    @Test
    fun aLongPressThatBarelyMovesStillOpensTheMenu() {
        homeWith("Clock", "Camera")

        compose.onNodeWithText("Clock").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveBy(Offset(0f, 1f))
            up()
        }

        compose.onNodeWithText("Unpin from Home").assertExists()
    }

    @Test
    fun homeScrollsOnceFavoritesOutgrowIt() {
        val labels = (1..20).map { "App$it" }
        homeWith(*labels.toTypedArray())

        compose.onNodeWithText("App1").assertExists()
        compose.onNodeWithText(labels.last()).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun centredHomeKeepsFavoriteIconsInOneColumn() {
        val repository = FakeLauncherRepository()
        val icon = AppIcon(original = ImageBitmap(1, 1), themeable = null)
        val shortLabel = launcherAppFixture("Maps").copy(icon = icon)
        val longLabel = launcherAppFixture("Very long favorite label").copy(icon = icon)
        runBlocking {
            repository.install(shortLabel)
            repository.install(longLabel)
            repository.chooseFavorite(Favorite(shortLabel.id, position = 0))
            repository.chooseFavorite(Favorite(longLabel.id, position = 1))
            repository.updateSettings { it.copy(homeAlignment = HomeAlignment.Centred) }
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

        val shortLabelLeft = compose.onNodeWithText(shortLabel.label).getUnclippedBoundsInRoot().left
        val longLabelLeft = compose.onNodeWithText(longLabel.label).getUnclippedBoundsInRoot().left

        assertEquals("Favorite labels should begin after one shared icon column", shortLabelLeft, longLabelLeft)
    }

    @Test
    fun centredHomeWithIconsOffCentresEachFavoriteLabel() {
        val repository = FakeLauncherRepository()
        val icon = AppIcon(original = ImageBitmap(1, 1), themeable = null)
        val shortLabel = launcherAppFixture("Maps").copy(icon = icon)
        val longLabel = launcherAppFixture("Very long favorite label").copy(icon = icon)
        runBlocking {
            repository.install(shortLabel)
            repository.install(longLabel)
            repository.chooseFavorite(Favorite(shortLabel.id, position = 0))
            repository.chooseFavorite(Favorite(longLabel.id, position = 1))
            repository.updateSettings {
                it.copy(homeAlignment = HomeAlignment.Centred, iconModeOverride = IconMode.Off)
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

        val shortLabelBounds = compose.onNodeWithText(shortLabel.label).getUnclippedBoundsInRoot()
        val longLabelBounds = compose.onNodeWithText(longLabel.label).getUnclippedBoundsInRoot()
        val shortLabelCentre = shortLabelBounds.left + (shortLabelBounds.right - shortLabelBounds.left) / 2
        val longLabelCentre = longLabelBounds.left + (longLabelBounds.right - longLabelBounds.left) / 2

        assertEquals("Icon Mode off must not leave an icon column behind", shortLabelCentre, longLabelCentre)
    }

    @Test
    fun centredHomeDoesNotIndentATombstoneForAnIcon() {
        val repository = FakeLauncherRepository()
        val available = launcherAppFixture("Maps").copy(icon = AppIcon(ImageBitmap(1, 1), null))
        val unavailable = launcherAppFixture("Calendar")
        runBlocking {
            repository.install(available)
            repository.install(unavailable)
            repository.chooseFavorite(Favorite(available.id, position = 0))
            repository.chooseFavorite(Favorite(unavailable.id, position = 1))
            repository.updateSettings { it.copy(homeAlignment = HomeAlignment.Centred) }
        }
        repository.makeUnavailable(unavailable.id)
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

        val clockBounds = compose.onNodeWithText("14:35").getUnclippedBoundsInRoot()
        val tombstoneBounds = compose.onNodeWithText(unavailable.label).getUnclippedBoundsInRoot()
        val clockCentre = clockBounds.left + (clockBounds.right - clockBounds.left) / 2
        val tombstoneCentre = tombstoneBounds.left + (tombstoneBounds.right - tombstoneBounds.left) / 2

        assertEquals("A Tombstone must not reserve space for an icon", clockCentre, tombstoneCentre)
    }

    @Test
    fun centringTakesEffectWithoutLeavingHome() {
        val repository = FakeLauncherRepository()
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
        val whenLeftAligned = compose.onNodeWithText("14:35").getUnclippedBoundsInRoot().left

        compose.runOnIdle {
            runBlocking { repository.updateSettings { it.copy(homeAlignment = HomeAlignment.Centred) } }
        }

        val whenCentred = compose.onNodeWithText("14:35").getUnclippedBoundsInRoot().left
        assertTrue("Expected $whenCentred to sit right of $whenLeftAligned", whenCentred > whenLeftAligned)
    }

    /** Home showing one Favorite per label, in the order given, and nothing else. */
    private fun homeWith(
        vararg labels: String,
        onOpenDrawer: () -> Unit = {},
    ): FakeLauncherRepository {
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
                onOpenDrawer = onOpenDrawer,
            )
        }
        return repository
    }

    private fun List<Favorite>.labelsAmong(installedApps: List<LauncherApp>) = shownAmong(installedApps).map(ShownFavorite::label)
}
