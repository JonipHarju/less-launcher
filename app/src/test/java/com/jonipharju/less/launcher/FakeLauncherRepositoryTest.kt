package com.jonipharju.less.launcher

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * What the Fake stands in for: the installed-app list the platform hands over, which is never
 * stored, and the platform calls a test cannot let happen for real. The rules it applies to the
 * Configuration are not asserted here — they are the store's own, and [ConfigurationEditsTest]
 * covers them where both the Fake and the phone read them from.
 */
class FakeLauncherRepositoryTest {
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

    /** An uninstall reaches the stored Configuration, rather than only the list of apps. */
    @Test
    fun `uninstalling an app puts the forgetting rule to the Configuration`() =
        runBlocking {
            val repository = FakeLauncherRepository()
            val app = launcherAppFixture(label = "Clock")
            repository.install(app)
            repository.chooseFavorite(Favorite(app.id, position = 0))
            repository.hideApp(app.id)

            repository.uninstall(app.id)

            assertEquals(emptyList<Favorite>(), repository.favorites.value)
            assertEquals(emptySet<LauncherAppId>(), repository.hiddenApps.value)
        }

    @Test
    fun `an unavailable app keeps its Favorite until it returns or is dismissed`() =
        runBlocking {
            val repository = FakeLauncherRepository()
            val app = launcherAppFixture(label = "Clock")
            val favorite = Favorite(app.id, position = 0, customLabel = "Time")
            repository.install(app)
            repository.chooseFavorite(favorite)

            repository.makeUnavailable(app.id)

            assertEquals(emptyList<LauncherApp>(), repository.installedApps.value)
            assertEquals(listOf(favorite), repository.favorites.value)

            repository.makeAvailable(app)

            assertEquals(listOf(app), repository.installedApps.value)
            assertEquals(listOf(favorite), repository.favorites.value)

            repository.makeUnavailable(app.id)
            repository.dismissFavorite(app.id)
            repository.makeAvailable(app)

            assertEquals(emptyList<Favorite>(), repository.favorites.value)
        }

    @Test
    fun `an unavailable app stays hidden until it returns`() =
        runBlocking {
            val repository = FakeLauncherRepository()
            val clock = launcherAppFixture(label = "Clock")
            repository.install(clock)
            repository.hideApp(clock.id)

            repository.makeUnavailable(clock.id)
            repository.makeAvailable(clock)

            assertEquals(setOf(clock.id), repository.hiddenApps.value)
        }

    @Test
    fun `a hidden app stays installed and drops out of the Drawer's listing`() =
        runBlocking {
            val repository = FakeLauncherRepository()
            val clock = launcherAppFixture(label = "Clock")
            val camera = launcherAppFixture(label = "Camera")
            repository.install(clock)
            repository.install(camera)

            repository.hideApp(clock.id)

            assertEquals(listOf(camera, clock), repository.installedApps.value)
            assertEquals(listOf(camera), repository.installedApps.value.withoutHidden(repository.hiddenApps.value))
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
