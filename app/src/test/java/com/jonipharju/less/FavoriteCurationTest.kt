package com.jonipharju.less

import com.jonipharju.less.launcher.Favorite
import com.jonipharju.less.launcher.LauncherAppId
import com.jonipharju.less.launcher.ShownFavorite
import com.jonipharju.less.launcher.launcherAppFixture
import com.jonipharju.less.launcher.shownAmong
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * What Home is in the middle of doing with its Favorites, driven the way a finger drives it —
 * without a screen, because none of this is drawing.
 */
class FavoriteCurationTest {
    private val rowHeight = 52f
    private val clock = launcherAppFixture(label = "Clock")
    private val calendar = launcherAppFixture(label = "Calendar")
    private val camera = launcherAppFixture(label = "Camera")

    /** Three Favorites on Home, in the order Home draws them. */
    private val pinned: List<ShownFavorite> =
        listOf(
            Favorite(clock.id, position = 0),
            Favorite(calendar.id, position = 1),
            Favorite(camera.id, position = 2),
        ).shownAmong(listOf(clock, calendar, camera))

    private fun curation() = FavoriteCuration(rowHeight)

    private fun List<ShownFavorite>.ids() = map { it.favorite.appId }

    private fun HomeFavorites.curatedId() = curated?.favorite?.appId

    private fun HomeFavorites.renamingId() = renaming?.favorite?.appId

    @Test
    fun `at rest Home draws the order it was given`() {
        val curation = curation()

        assertEquals(listOf(clock.id, calendar.id, camera.id), curation.state(pinned).shown.ids())
    }

    @Test
    fun `a drag shorter than a row moves nothing`() {
        val curation = curation()

        curation.draggedBy(clock.id, rowHeight - 1f, pinned)

        assertEquals(listOf(clock.id, calendar.id, camera.id), curation.state(pinned).shown.ids())
    }

    @Test
    fun `a drag of one row moves the Favorite one place`() {
        val curation = curation()

        curation.draggedBy(clock.id, rowHeight, pinned)

        assertEquals(listOf(calendar.id, clock.id, camera.id), curation.state(pinned).shown.ids())
    }

    /** What a drag covers on the way to a row is carried into the next one, not thrown away. */
    @Test
    fun `travel short of a row is carried into the next`() {
        val curation = curation()

        curation.draggedBy(clock.id, rowHeight * 0.6f, pinned)
        curation.draggedBy(clock.id, rowHeight * 0.6f, pinned)

        assertEquals(listOf(calendar.id, clock.id, camera.id), curation.state(pinned).shown.ids())
    }

    @Test
    fun `a drag upwards moves the Favorite the other way`() {
        val curation = curation()

        curation.draggedBy(camera.id, -rowHeight, pinned)

        assertEquals(listOf(clock.id, camera.id, calendar.id), curation.state(pinned).shown.ids())
    }

    @Test
    fun `a drag past the end of Home stops at the end`() {
        val curation = curation()

        curation.draggedBy(clock.id, rowHeight * 9, pinned)

        assertEquals(listOf(calendar.id, camera.id, clock.id), curation.state(pinned).shown.ids())
    }

    @Test
    fun `letting go stores the order the finger described`() =
        runBlocking {
            val curation = curation()
            curation.draggedBy(clock.id, rowHeight, pinned)
            val stored = mutableListOf<List<LauncherAppId>>()

            curation.dragEnded { stored += it }

            assertEquals(listOf(listOf(calendar.id, clock.id, camera.id)), stored)
        }

    /** Once the store has the new order, Home goes back to drawing what the store says. */
    @Test
    fun `the order under the finger is given up only once it is stored`() =
        runBlocking {
            val curation = curation()
            curation.draggedBy(clock.id, rowHeight, pinned)

            curation.dragEnded { assertEquals(listOf(calendar.id, clock.id, camera.id), curation.state(pinned).shown.ids()) }

            assertEquals(listOf(clock.id, calendar.id, camera.id), curation.state(pinned).shown.ids())
        }

    @Test
    fun `letting go without having moved anything stores nothing`() =
        runBlocking {
            val curation = curation()
            curation.draggedBy(clock.id, rowHeight - 1f, pinned)
            var stores = 0

            curation.dragEnded { stores++ }

            assertEquals(0, stores)
        }

    @Test
    fun `curating opens the menu on that Favorite and dismissing closes it`() {
        val curation = curation()

        curation.curate(calendar.id)
        assertEquals(calendar.id, curation.state(pinned).curatedId())

        curation.dismissMenu()
        assertNull(curation.state(pinned).curated)
    }

    @Test
    fun `renaming from the menu closes the menu and asks about the same Favorite`() {
        val curation = curation()
        curation.curate(calendar.id)

        curation.renameCurated()

        val state = curation.state(pinned)
        assertNull(state.curated)
        assertEquals(calendar.id, state.renamingId())
    }

    @Test
    fun `dismissing the rename asks about nothing`() {
        val curation = curation()
        curation.curate(calendar.id)
        curation.renameCurated()

        curation.dismissRename()

        assertNull(curation.state(pinned).renaming)
    }

    /** The long press that opens the menu is the start of the gesture that could have dragged. */
    @Test
    fun `curating leaves no travel behind for the next drag`() {
        val curation = curation()
        curation.draggedBy(clock.id, rowHeight * 0.9f, pinned)

        curation.curate(clock.id)
        curation.draggedBy(clock.id, rowHeight * 0.9f, pinned)

        assertEquals(listOf(clock.id, calendar.id, camera.id), curation.state(pinned).shown.ids())
    }

    /**
     * A gesture can die without letting go — the row leaves Home mid-drag — and what it had
     * travelled must not turn up under the next Favorite the user drags.
     */
    @Test
    fun `travel from an abandoned drag is not carried into the next one`() {
        val curation = curation()
        curation.draggedBy(clock.id, rowHeight * 0.9f, pinned)

        curation.draggedBy(camera.id, rowHeight * 0.9f, pinned)

        assertEquals(listOf(clock.id, calendar.id, camera.id), curation.state(pinned).shown.ids())
    }

    @Test
    fun `letting go leaves no travel behind for the next drag`() =
        runBlocking {
            val curation = curation()
            curation.draggedBy(clock.id, rowHeight * 0.9f, pinned)

            curation.dragEnded { }
            curation.draggedBy(clock.id, rowHeight * 0.9f, pinned)

            assertEquals(listOf(clock.id, calendar.id, camera.id), curation.state(pinned).shown.ids())
        }

    @Test
    fun `a Favorite that has gone is curated no longer`() {
        val curation = curation()
        curation.curate(camera.id)

        val withoutCamera = curation.state(pinned.filterNot { it.favorite.appId == camera.id })

        assertNull(withoutCamera.curated)
    }
}
