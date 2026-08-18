package com.jonipharju.less.launcher

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeLauncherRepositoryTest {
    @Test
    fun `settings are updated through repository`() =
        runBlocking {
            val repository = FakeLauncherRepository()

            repository.updateSettings { it.copy(iconModeOverride = IconMode.Tinted) }
            repository.updateSettings { it.copy(homeAlignment = HomeAlignment.Centred) }

            assertEquals(
                LauncherSettings(iconModeOverride = IconMode.Tinted, homeAlignment = HomeAlignment.Centred),
                repository.settings.value,
            )
        }

    @Test
    fun `chosen Favorites appear in position order`() =
        runBlocking {
            val repository = FakeLauncherRepository()
            val clock = launcherAppFixture(label = "Clock")
            val calendar = launcherAppFixture(label = "Calendar")

            repository.chooseFavorite(Favorite(clock.id, position = 1, customLabel = "Time"))
            repository.chooseFavorite(Favorite(calendar.id, position = 0))

            assertEquals(
                listOf(
                    Favorite(calendar.id, position = 0),
                    Favorite(clock.id, position = 1, customLabel = "Time"),
                ),
                repository.favorites.value,
            )
        }

    @Test
    fun `reordering Favorites rewrites their positions and keeps their labels`() =
        runBlocking {
            val repository = FakeLauncherRepository()
            val clock = launcherAppFixture(label = "Clock")
            val calendar = launcherAppFixture(label = "Calendar")
            repository.chooseFavorite(Favorite(clock.id, position = 0, customLabel = "Time"))
            repository.chooseFavorite(Favorite(calendar.id, position = 1))

            repository.reorderFavorites(listOf(calendar.id, clock.id))

            assertEquals(
                listOf(
                    Favorite(calendar.id, position = 0),
                    Favorite(clock.id, position = 1, customLabel = "Time"),
                ),
                repository.favorites.value,
            )
        }

    @Test
    fun `renaming a Favorite leaves its position alone`() =
        runBlocking {
            val repository = FakeLauncherRepository()
            val clock = launcherAppFixture(label = "Clock")
            repository.chooseFavorite(Favorite(clock.id, position = 3))

            val renamed =
                repository.favorites.value
                    .single()
                    .copy(customLabel = "Time")
            repository.chooseFavorite(renamed)

            assertEquals(listOf(Favorite(clock.id, position = 3, customLabel = "Time")), repository.favorites.value)
        }

    @Test
    fun `pinning past the soft cap still adds the Favorite`() =
        runBlocking {
            val repository = FakeLauncherRepository()
            repeat(FavoritesSoftCap) { index ->
                val app = launcherAppFixture(label = "App$index")
                repository.chooseFavorite(repository.favorites.value.pinning(app.id))
            }
            val ninth = launcherAppFixture(label = "Ninth")

            repository.chooseFavorite(repository.favorites.value.pinning(ninth.id))

            assertEquals(FavoritesSoftCap + 1, repository.favorites.value.size)
            assertTrue(repository.favorites.value.exceedSoftCap())
            assertEquals(
                ninth.id,
                repository.favorites.value
                    .last()
                    .appId,
            )
        }

    @Test
    fun `app info and uninstall are asked of the system`() {
        val repository = FakeLauncherRepository()
        val clock = launcherAppFixture(label = "Clock")

        repository.showAppInfo(clock.id)
        repository.requestUninstall(clock.id)

        assertEquals(listOf(clock.id), repository.appInfoShownFor)
        assertEquals(listOf(clock.id), repository.uninstallsRequestedFor)
    }

    @Test
    fun `installed app appears in observable state`() {
        val repository = FakeLauncherRepository()
        val app = launcherAppFixture(label = "Clock")

        repository.install(app)

        assertEquals(listOf(app), repository.installedApps.value)
    }

    @Test
    fun `uninstalled app disappears from observable state`() {
        val repository = FakeLauncherRepository()
        val app = launcherAppFixture(label = "Clock")
        repository.install(app)

        repository.uninstall(app.id)

        assertEquals(emptyList<LauncherApp>(), repository.installedApps.value)
    }

    @Test
    fun `updated app replaces its previous state`() {
        val repository = FakeLauncherRepository()
        val app = launcherAppFixture(label = "Clock")
        repository.install(app)
        val updatedApp = app.copy(label = "Alarm Clock")

        repository.update(updatedApp)

        assertEquals(listOf(updatedApp), repository.installedApps.value)
    }

    @Test
    fun `installed apps are listed alphabetically`() {
        val repository = FakeLauncherRepository()
        val zebra = launcherAppFixture(label = "Zebra")
        val clock = launcherAppFixture(label = "Clock")

        repository.install(zebra)
        repository.install(clock)

        assertEquals(listOf(clock, zebra), repository.installedApps.value)
    }

    @Test
    fun `launch records the selected app`() {
        val repository = FakeLauncherRepository()
        val app = launcherAppFixture(label = "Clock")

        repository.launch(app)

        assertEquals(listOf(app), repository.launchedApps)
    }
}
