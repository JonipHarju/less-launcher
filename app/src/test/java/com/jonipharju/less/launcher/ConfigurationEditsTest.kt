package com.jonipharju.less.launcher

import com.jonipharju.less.launcher.proto.LauncherUserData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules themselves, read off the stored record. The store and the Fake both put changes
 * down through these, so what is asserted here is what a phone does.
 */
class ConfigurationEditsTest {
    private val clock = launcherAppFixture(label = "Clock")
    private val calendar = launcherAppFixture(label = "Calendar")
    private val browser = launcherAppFixture(label = "Browser")
    private val stored = LauncherUserData.getDefaultInstance()

    @Test
    fun `chosen Favorites are stored in position order`() {
        val edited =
            stored
                .choosing(Favorite(clock.id, position = 1, customLabel = "Time"))
                .choosing(Favorite(calendar.id, position = 0))

        assertEquals(
            listOf(
                Favorite(calendar.id, position = 0),
                Favorite(clock.id, position = 1, customLabel = "Time"),
            ),
            edited.storedFavorites(),
        )
    }

    @Test
    fun `choosing an app already on Home replaces its Favorite rather than doubling it`() {
        val edited =
            stored
                .choosing(Favorite(clock.id, position = 3))
                .choosing(Favorite(clock.id, position = 3, customLabel = "Time"))

        assertEquals(listOf(Favorite(clock.id, position = 3, customLabel = "Time")), edited.storedFavorites())
    }

    @Test
    fun `dismissing a Favorite leaves the others where they were`() {
        val edited =
            stored
                .choosing(Favorite(clock.id, position = 0))
                .choosing(Favorite(calendar.id, position = 1))
                .dismissing(clock.id)

        assertEquals(listOf(Favorite(calendar.id, position = 1)), edited.storedFavorites())
    }

    @Test
    fun `reordering rewrites positions and keeps custom labels`() {
        val edited =
            stored
                .choosing(Favorite(clock.id, position = 0, customLabel = "Time"))
                .choosing(Favorite(calendar.id, position = 1))
                .reordered(listOf(calendar.id, clock.id))

        assertEquals(
            listOf(
                Favorite(calendar.id, position = 0),
                Favorite(clock.id, position = 1, customLabel = "Time"),
            ),
            edited.storedFavorites(),
        )
    }

    @Test
    fun `pinning past the soft cap still adds the Favorite`() {
        val crowded =
            (0 until FavoritesSoftCap).fold(stored) { userData, index ->
                val app = launcherAppFixture(label = "App$index")
                userData.choosing(userData.storedFavorites().pinning(app.id))
            }
        val ninth = launcherAppFixture(label = "Ninth")

        val edited = crowded.choosing(crowded.storedFavorites().pinning(ninth.id))

        assertEquals(FavoritesSoftCap + 1, edited.storedFavorites().size)
        assertTrue(edited.storedFavorites().exceedSoftCap())
        assertEquals(ninth.id, edited.storedFavorites().last().appId)
    }

    @Test
    fun `hiding an app twice hides it once`() {
        val edited = stored.hiding(clock.id).hiding(clock.id)

        assertEquals(setOf(clock.id), edited.storedHiddenApps())
    }

    @Test
    fun `unhiding an app returns it to the Drawer`() {
        val edited = stored.hiding(clock.id).hiding(calendar.id).unhiding(clock.id)

        assertEquals(setOf(calendar.id), edited.storedHiddenApps())
    }

    @Test
    fun `unhiding an app that was never hidden changes nothing`() {
        val edited = stored.hiding(clock.id).unhiding(calendar.id)

        assertEquals(setOf(clock.id), edited.storedHiddenApps())
    }

    @Test
    fun `hiding a Favorite leaves it on Home`() {
        val edited = stored.choosing(Favorite(clock.id, position = 0)).hiding(clock.id)

        assertEquals(listOf(Favorite(clock.id, position = 0)), edited.storedFavorites())
        assertEquals(setOf(clock.id), edited.storedHiddenApps())
    }

    @Test
    fun `an uninstalled package leaves behind neither Favorite nor record of being hidden`() {
        val edited =
            stored
                .choosing(Favorite(clock.id, position = 0))
                .choosing(Favorite(calendar.id, position = 1))
                .hiding(clock.id)
                .hiding(browser.id)
                .forgetting(clock.id.packageName, clock.id.profileSerialNumber)

        assertEquals(listOf(Favorite(calendar.id, position = 1)), edited.storedFavorites())
        assertEquals(setOf(browser.id), edited.storedHiddenApps())
    }

    /**
     * A Tombstone marks a loss the user did not ask for. Uninstall is a choice, so the
     * Favorite is forgotten rather than shown in place as unavailable.
     */
    @Test
    fun `a Favorite the user uninstalls leaves no Tombstone`() {
        val edited =
            stored
                .choosing(Favorite(clock.id, position = 0))
                .choosing(Favorite(calendar.id, position = 1))
                .forgetting(clock.id.packageName, clock.id.profileSerialNumber)

        val shown = edited.storedFavorites().shownAmong(listOf(calendar))

        assertEquals(listOf(calendar.id), shown.map { it.favorite.appId })
        assertTrue(shown.none { it.app == null })
    }

    /**
     * A package is uninstalled whole, so every activity of it goes — not just the one the
     * Favorite happens to name.
     */
    @Test
    fun `uninstalling a package forgets every activity of it`() {
        val second = clock.id.copy(activityName = "com.example.ClockAlarmActivity")

        val edited =
            stored
                .choosing(Favorite(clock.id, position = 0))
                .choosing(Favorite(second, position = 1))
                .forgetting(clock.id.packageName, clock.id.profileSerialNumber)

        assertEquals(emptyList<Favorite>(), edited.storedFavorites())
    }

    /** The same package in a work profile is a different app, and it stays. */
    @Test
    fun `uninstalling from one profile leaves the other profile's copy alone`() {
        val atWork = clock.id.copy(profileSerialNumber = 11)

        val edited =
            stored
                .choosing(Favorite(clock.id, position = 0))
                .choosing(Favorite(atWork, position = 1))
                .forgetting(clock.id.packageName, clock.id.profileSerialNumber)

        assertEquals(listOf(Favorite(atWork, position = 1)), edited.storedFavorites())
    }

    @Test
    fun `holding the Home Role is recorded`() {
        assertEquals(true, stored.recordingHomeRole().storedSettings().hasHeldHomeRole)
    }

    /** Handing the role to another launcher is a choice, and not a mistake to correct. */
    @Test
    fun `having held the Home Role survives losing it`() {
        val edited = stored.recordingHomeRole().settingsUpdated { it.copy(themeId = "parasol") }

        assertEquals(true, edited.storedSettings().hasHeldHomeRole)
    }

    @Test
    fun `settings are applied to whatever is stored at the time of the write`() {
        val edited =
            stored
                .settingsUpdated { it.copy(iconModeOverride = IconMode.Tinted) }
                .settingsUpdated { it.copy(homeAlignment = HomeAlignment.Centred) }

        assertEquals(
            LauncherSettings(iconModeOverride = IconMode.Tinted, homeAlignment = HomeAlignment.Centred),
            edited.storedSettings(),
        )
    }

    @Test
    fun `an unset record reads as the defaults the domain type declares`() {
        assertEquals(LauncherSettings(), stored.storedSettings())
        assertEquals(emptyList<Favorite>(), stored.storedFavorites())
        assertEquals(emptySet<LauncherAppId>(), stored.storedHiddenApps())
    }

    @Test
    fun `restoring puts a Configuration in place of everything stored`() {
        val before =
            stored
                .choosing(Favorite(browser.id, position = 0, customLabel = "Web"))
                .hiding(clock.id)

        val edited =
            before.restoring(
                LauncherConfiguration(
                    favorites = listOf(Favorite(calendar.id, position = 1), Favorite(clock.id, position = 0)),
                    hiddenApps = setOf(browser.id),
                    settings = LauncherSettings(themeId = "parasol"),
                ),
            )

        assertEquals(listOf(clock.id, calendar.id), edited.storedFavorites().map(Favorite::appId))
        assertEquals(setOf(browser.id), edited.storedHiddenApps())
        assertEquals("parasol", edited.storedSettings().themeId)
    }

    /** How far Setup got, and whether Less has held the Home Role, are the device's own answers. */
    @Test
    fun `restoring leaves the device's own answers as they stand`() {
        val before =
            stored.settingsUpdated {
                it.copy(setupStep = SetupStep.Done, hasHeldHomeRole = true)
            }

        val edited =
            before.restoring(
                LauncherConfiguration(
                    favorites = emptyList(),
                    hiddenApps = emptySet(),
                    settings = LauncherSettings(setupStep = SetupStep.Theme, hasHeldHomeRole = false),
                ),
            )

        assertEquals(SetupStep.Done, edited.storedSettings().setupStep)
        assertEquals(true, edited.storedSettings().hasHeldHomeRole)
    }
}
