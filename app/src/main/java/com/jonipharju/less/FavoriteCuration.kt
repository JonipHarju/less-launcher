package com.jonipharju.less

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import com.jonipharju.less.launcher.LauncherAppId
import com.jonipharju.less.launcher.ShownFavorite
import com.jonipharju.less.launcher.moved

/** What Home draws this frame: its Favorites, and whatever is open over one of them. */
internal data class CuratedHome(
    val shown: List<ShownFavorite>,
    val curated: ShownFavorite?,
    val renaming: ShownFavorite?,
)

/**
 * What Home is in the middle of doing with its Favorites — a drag under the finger, a menu open
 * over one of them, a rename being asked about. Home draws what [state] says and reports what
 * the finger did; the sequencing between the two lives here, where it can be driven without a
 * screen.
 */
internal class FavoriteCuration(
    private val rowHeight: Float,
) {
    private var curated by mutableStateOf<LauncherAppId?>(null)
    private var renaming by mutableStateOf<LauncherAppId?>(null)

    // While a drag is in flight Home shows the order the finger is describing, not the stored
    // one, so the list rearranges under the finger rather than after it lets go.
    private var draggedOrder by mutableStateOf<List<LauncherAppId>?>(null)

    // A drag only moves a Favorite once it has covered a whole row; what it covers on the way
    // there is carried into the next row rather than thrown away.
    private var carry = 0f

    /** The order last drawn, which is what a drag starting now rearranges. */
    private var drawnOrder = emptyList<LauncherAppId>()

    /** What Home draws, given the Favorites the store is holding. */
    fun state(pinned: List<ShownFavorite>): CuratedHome {
        val shown = draggedOrder?.let { order -> pinned.inTheOrderOf(order) } ?: pinned
        drawnOrder = shown.map { it.favorite.appId }

        return CuratedHome(
            shown = shown,
            curated = shown.withId(curated),
            renaming = shown.withId(renaming),
        )
    }

    /** Opens the menu over [appId]. The press that got here is not also a drag. */
    fun curate(appId: LauncherAppId) {
        carry = 0f
        curated = appId
    }

    fun dismissMenu() {
        curated = null
    }

    /** The menu's way into the rename: the menu closes, and the same Favorite is asked about. */
    fun renameCurated() {
        renaming = curated
        curated = null
    }

    fun dismissRename() {
        renaming = null
    }

    /** [travelled] pixels of drag on [appId], which moves it once it has covered a whole row. */
    fun draggedBy(
        appId: LauncherAppId,
        travelled: Float,
    ) {
        carry += travelled
        val rows = (carry / rowHeight).toInt()
        if (rows == 0) return

        carry -= rows * rowHeight
        val order = draggedOrder ?: drawnOrder
        val from = order.indexOf(appId)
        if (from == -1) return

        draggedOrder = order.moved(from, (from + rows).coerceIn(order.indices))
    }

    /**
     * The finger has let go. The order it described is stored before Home gives it up, so that
     * the list cannot fall back to the old order for the frames the write takes.
     */
    suspend fun dragEnded(reorder: suspend (List<LauncherAppId>) -> Unit) {
        carry = 0f
        val order = draggedOrder ?: return
        reorder(order)
        draggedOrder = null
    }
}

/** The shown Favorite [appId] names, or null where it names none any more. */
private fun List<ShownFavorite>.withId(appId: LauncherAppId?): ShownFavorite? = firstOrNull { it.favorite.appId == appId }

/** The same Favorites arranged as [order] has them, for the length of a drag. */
private fun List<ShownFavorite>.inTheOrderOf(order: List<LauncherAppId>): List<ShownFavorite> =
    sortedBy { shownFavorite ->
        order.indexOf(shownFavorite.favorite.appId).takeUnless { it == -1 } ?: order.size
    }

/** One [FavoriteCuration] for as long as Home is on screen, measuring a row in pixels once. */
@Composable
internal fun rememberFavoriteCuration(): FavoriteCuration {
    val rowHeight = with(LocalDensity.current) { FavoriteRowHeight.toPx() }

    return remember(rowHeight) { FavoriteCuration(rowHeight) }
}
