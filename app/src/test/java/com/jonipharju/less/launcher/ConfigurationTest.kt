package com.jonipharju.less.launcher

import com.jonipharju.less.launcher.proto.ExportedConfiguration
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConfigurationTest {
    @Test
    fun `an exported Configuration comes back with Favorites, Hidden Apps and settings`() =
        runBlocking {
            val exported = configuredRepository()
            val imported = FakeLauncherRepository()

            imported.restoreConfiguration(configurationFrom(exported.configuration().encoded())!!)

            assertEquals(exported.favorites.value, imported.favorites.value)
            assertEquals(exported.hiddenApps.value, imported.hiddenApps.value)
            assertEquals(exported.settings.value, imported.settings.value)
        }

    @Test
    fun `an exported Configuration keeps the order and custom labels of Favorites`() =
        runBlocking {
            val exported = configuredRepository()
            val imported = FakeLauncherRepository()

            imported.restoreConfiguration(configurationFrom(exported.configuration().encoded())!!)

            assertEquals(
                listOf(
                    Favorite(clock.id, position = 0, customLabel = "Time"),
                    Favorite(camera.id, position = 1),
                ),
                imported.favorites.value,
            )
        }

    @Test
    fun `the active Theme survives the round trip`() =
        runBlocking {
            val exported = configuredRepository()
            val imported = FakeLauncherRepository()

            imported.restoreConfiguration(configurationFrom(exported.configuration().encoded())!!)

            assertEquals("parasol", imported.settings.value.themeId)
        }

    @Test
    fun `importing replaces what the device held before`() =
        runBlocking {
            val imported = FakeLauncherRepository()
            imported.chooseFavorite(Favorite(browser.id, position = 0, customLabel = "Web"))
            imported.hideApp(clock.id)

            imported.restoreConfiguration(configurationFrom(configuredRepository().configuration().encoded())!!)

            assertEquals(listOf(clock.id, camera.id), imported.favorites.value.map(Favorite::appId))
            assertEquals(setOf(browser.id), imported.hiddenApps.value)
        }

    @Test
    fun `a Configuration naming apps the device does not have imports them as Tombstones`() =
        runBlocking {
            val imported = FakeLauncherRepository()
            imported.install(clock)

            imported.restoreConfiguration(configurationFrom(configuredRepository().configuration().encoded())!!)

            assertEquals(listOf(clock.id, camera.id), imported.favorites.value.map(Favorite::appId))
            assertEquals(
                listOf(clock.id, camera.id),
                imported.favorites.value
                    .shownAmong(imported.installedApps.value)
                    .map { it.favorite.appId },
            )
            assertEquals(
                listOf("Time", "Camera"),
                imported.favorites.value
                    .shownAmong(imported.installedApps.value)
                    .map { it.label },
            )
        }

    @Test
    fun `a file that is not a Configuration reads as nothing`() {
        assertNull(configurationFrom("not a configuration".toByteArray()))
        assertNull(configurationFrom(ByteArray(0)))
    }

    @Test
    fun `a Configuration written in a format this Less does not know reads as nothing`() =
        runBlocking {
            val exported = configuredRepository().configuration()

            val fromLater =
                ExportedConfiguration
                    .parseFrom(exported.encoded())
                    .toBuilder()
                    .setFormatVersion(ConfigurationFormatVersion + 1)
                    .build()
                    .toByteArray()

            assertNull(configurationFrom(fromLater))
        }

    @Test
    fun `a file that is not a Configuration leaves the device's own Configuration alone`() =
        runBlocking {
            val repository = configuredRepository()

            val read = configurationFrom("not a configuration".toByteArray())
            read?.let { repository.restoreConfiguration(it) }

            assertEquals(listOf(clock.id, camera.id), repository.favorites.value.map(Favorite::appId))
            assertEquals(setOf(browser.id), repository.hiddenApps.value)
            assertEquals("parasol", repository.settings.value.themeId)
        }

    @Test
    fun `importing does not send the user back through Setup`() =
        runBlocking {
            val exported = FakeLauncherRepository()
            val imported = FakeLauncherRepository()
            imported.finishSetup()
            imported.holdHomeRole()

            imported.restoreConfiguration(configurationFrom(exported.configuration().encoded())!!)

            assertEquals(SetupStep.Done, imported.settings.value.setupStep)
            assertEquals(true, imported.settings.value.hasHeldHomeRole)
        }

    @Test
    fun `an exported Configuration says nothing about the device it came from`() =
        runBlocking {
            val exported = configuredRepository()
            exported.finishSetup()
            exported.holdHomeRole()

            val read = configurationFrom(exported.configuration().encoded())!!

            assertEquals(SetupStep.Theme, read.settings.setupStep)
            assertEquals(false, read.settings.hasHeldHomeRole)
        }

    private val clock = launcherAppFixture(label = "Clock")
    private val camera = launcherAppFixture(label = "Camera")
    private val browser = launcherAppFixture(label = "Browser")

    /** A launcher the user has made their own: two Favorites, a Hidden App, a Theme, a setting. */
    private fun configuredRepository() =
        FakeLauncherRepository().apply {
            runBlocking {
                install(clock)
                install(camera)
                install(browser)
                chooseFavorite(Favorite(clock.id, position = 0, customLabel = "Time"))
                chooseFavorite(Favorite(camera.id, position = 1))
                hideApp(browser.id)
                updateSettings {
                    it.copy(
                        themeId = "parasol",
                        homeAlignment = HomeAlignment.Centred,
                        iconModeOverride = IconMode.Tinted,
                        drawerOpenDirection = DrawerOpenDirection.SwipeDown,
                        opensKeyboardWithDrawer = false,
                    )
                }
            }
        }
}
