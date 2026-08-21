package com.jonipharju.less.launcher

import com.jonipharju.less.launcher.proto.LauncherUserData
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigurationTest {
    private val clock = launcherAppFixture(label = "Clock")
    private val camera = launcherAppFixture(label = "Camera")
    private val browser = launcherAppFixture(label = "Browser")

    @Test
    fun `the stored proto round-trips Favorites, Hidden Apps and settings`() {
        val stored = configuredRecord()
        val roundTripped = stored.roundTripped()

        assertEquals(stored.storedFavorites(), roundTripped.storedFavorites())
        assertEquals(stored.storedHiddenApps(), roundTripped.storedHiddenApps())
        assertEquals(stored.storedSettings(), roundTripped.storedSettings())
    }

    @Test
    fun `the stored proto keeps the order and custom labels of Favorites`() {
        val roundTripped = configuredRecord().roundTripped()

        assertEquals(
            listOf(
                Favorite(clock.id, position = 0, customLabel = "Time"),
                Favorite(camera.id, position = 1),
            ),
            roundTripped.storedFavorites(),
        )
    }

    @Test
    fun `the active Theme survives the round trip`() {
        val roundTripped = configuredRecord().roundTripped()

        assertEquals("parasol", roundTripped.storedSettings().themeId)
    }

    @Test
    fun `the stored proto carries the device's own answers too`() {
        val stored =
            configuredRecord().settingsUpdated {
                it.copy(setupStep = SetupStep.Done, hasHeldHomeRole = true)
            }
        val roundTripped = stored.roundTripped()

        assertEquals(SetupStep.Done, roundTripped.storedSettings().setupStep)
        assertEquals(true, roundTripped.storedSettings().hasHeldHomeRole)
    }

    /** A record the user has made their own: two Favorites, a Hidden App, a Theme, a setting. */
    private fun configuredRecord(): LauncherUserData =
        LauncherUserData
            .getDefaultInstance()
            .choosing(Favorite(clock.id, position = 0, customLabel = "Time"))
            .choosing(Favorite(camera.id, position = 1))
            .hiding(browser.id)
            .settingsUpdated {
                it.copy(
                    themeId = "parasol",
                    homeAlignment = HomeAlignment.Centred,
                    iconModeOverride = IconMode.Tinted,
                    drawerOpenDirection = DrawerOpenDirection.SwipeDown,
                    opensKeyboardWithDrawer = false,
                )
            }

    private fun LauncherUserData.roundTripped(): LauncherUserData = LauncherUserData.parseFrom(toByteArray())
}
