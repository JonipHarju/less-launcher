package com.jonipharju.less.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class FakeLauncherRepositoryTest {
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
