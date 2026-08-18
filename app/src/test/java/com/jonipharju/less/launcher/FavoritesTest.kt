package com.jonipharju.less.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesTest {
    @Test
    fun `pinning places the app after the Favorites already on Home`() {
        val clock = launcherAppFixture("Clock")
        val camera = launcherAppFixture("Camera")
        val favorites = listOf(Favorite(clock.id, position = 0), Favorite(camera.id, position = 1))

        assertEquals(2, favorites.pinning(launcherAppFixture("Maps").id).position)
    }

    @Test
    fun `pinning onto an empty Home starts at the first position`() {
        assertEquals(0, emptyList<Favorite>().pinning(launcherAppFixture("Clock").id).position)
    }

    @Test
    fun `reordering renumbers positions densely from zero`() {
        val clock = launcherAppFixture("Clock")
        val camera = launcherAppFixture("Camera")
        val maps = launcherAppFixture("Maps")
        val favorites =
            listOf(
                Favorite(clock.id, position = 0),
                Favorite(camera.id, position = 4),
                Favorite(maps.id, position = 9),
            )

        val reordered = favorites.orderedBy(listOf(maps.id, clock.id, camera.id))

        assertEquals(
            listOf(
                Favorite(maps.id, position = 0),
                Favorite(clock.id, position = 1),
                Favorite(camera.id, position = 2),
            ),
            reordered,
        )
    }

    @Test
    fun `reordering keeps a custom label with its Favorite`() {
        val clock = launcherAppFixture("Clock")
        val camera = launcherAppFixture("Camera")
        val favorites =
            listOf(
                Favorite(clock.id, position = 0, customLabel = "Time"),
                Favorite(camera.id, position = 1),
            )

        val reordered = favorites.orderedBy(listOf(camera.id, clock.id))

        assertEquals(listOf(null, "Time"), reordered.map(Favorite::customLabel))
    }

    @Test
    fun `a Favorite the order leaves out keeps its place after the ones it names`() {
        val clock = launcherAppFixture("Clock")
        val camera = launcherAppFixture("Camera")
        val maps = launcherAppFixture("Maps")
        val favorites =
            listOf(
                Favorite(clock.id, position = 0),
                Favorite(camera.id, position = 1),
                Favorite(maps.id, position = 2),
            )

        val reordered = favorites.orderedBy(listOf(maps.id))

        assertEquals(listOf(maps.id, clock.id, camera.id), reordered.map(Favorite::appId))
    }

    @Test
    fun `moving lifts an item out and drops it at the new index`() {
        assertEquals(listOf("b", "c", "a"), listOf("a", "b", "c").moved(from = 0, to = 2))
        assertEquals(listOf("c", "a", "b"), listOf("a", "b", "c").moved(from = 2, to = 0))
    }

    @Test
    fun `moving outside the list leaves it alone`() {
        val items = listOf("a", "b", "c")

        assertEquals(items, items.moved(from = 0, to = 3))
        assertEquals(items, items.moved(from = -1, to = 1))
        assertEquals(items, items.moved(from = 1, to = 1))
    }

    @Test
    fun `the soft cap is exceeded only by the ninth Favorite`() {
        val eight = favorites(count = 8)

        assertFalse(eight.exceedSoftCap())
        assertTrue(favorites(count = 9).exceedSoftCap())
    }

    @Test
    fun `shown Favorites come in position order under the label Home shows`() {
        val clock = launcherAppFixture("Clock")
        val camera = launcherAppFixture("Camera")
        val favorites =
            listOf(
                Favorite(clock.id, position = 1, customLabel = "Time"),
                Favorite(camera.id, position = 0),
            )

        val shown = favorites.shownAmong(listOf(clock, camera))

        assertEquals(listOf("Camera", "Time"), shown.map(ShownFavorite::label))
    }

    @Test
    fun `a Favorite whose app is not installed is not shown`() {
        val clock = launcherAppFixture("Clock")
        val uninstalled = launcherAppFixture("Camera")
        val favorites =
            listOf(Favorite(clock.id, position = 0), Favorite(uninstalled.id, position = 1))

        val shown = favorites.shownAmong(listOf(clock))

        assertEquals(listOf(clock), shown.map(ShownFavorite::app))
    }

    @Test
    fun `custom labels are collected against the apps that carry them`() {
        val clock = launcherAppFixture("Clock")
        val camera = launcherAppFixture("Camera")
        val favorites =
            listOf(
                Favorite(clock.id, position = 0, customLabel = "Time"),
                Favorite(camera.id, position = 1),
            )

        assertEquals(mapOf(clock.id to "Time"), favorites.customLabels())
    }

    private fun favorites(count: Int) =
        (0 until count).map { index ->
            Favorite(launcherAppFixture("App$index").id, position = index)
        }
}
