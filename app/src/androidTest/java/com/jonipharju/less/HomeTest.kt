package com.jonipharju.less

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jonipharju.less.launcher.FakeLauncherRepository
import com.jonipharju.less.launcher.Favorite
import com.jonipharju.less.launcher.launcherAppFixture
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun rendersClockDateAndFavoritesInPositionOrder() =
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

    @Test
    fun tapsOpenSystemAppsAndLaunchFavorite() =
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
