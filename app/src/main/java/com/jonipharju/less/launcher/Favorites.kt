package com.jonipharju.less.launcher

/**
 * How many Favorites Home holds before it stops being a short list. The cap advises: the
 * Favorite past it is still pinned, and the user is told rather than stopped.
 */
internal val FavoritesSoftCap = 8

/** Whether Home holds more Favorites than the soft cap advises. */
internal fun List<Favorite>.exceedSoftCap(): Boolean = size > FavoritesSoftCap

/** The Favorite that pinning [appId] would add, placed after the ones already on Home. */
internal fun List<Favorite>.pinning(appId: LauncherAppId): Favorite =
    Favorite(appId = appId, position = (maxOfOrNull(Favorite::position) ?: -1) + 1)

/** Whether [appId] is already on Home. */
internal fun List<Favorite>.hold(appId: LauncherAppId): Boolean = any { it.appId == appId }

/**
 * The same Favorites in the order [order] names, renumbered from zero so that positions stay
 * dense. A Favorite [order] leaves out keeps its own order, after the ones it names.
 */
internal fun List<Favorite>.orderedBy(order: List<LauncherAppId>): List<Favorite> =
    sortedBy { favorite ->
        order.indexOf(favorite.appId).takeUnless { it == -1 } ?: order.size
    }.renumbered()

/** The same items with the one at [from] lifted out and dropped at [to]. */
internal fun <T> List<T>.moved(
    from: Int,
    to: Int,
): List<T> {
    if (from !in indices || to !in indices || from == to) return this

    return toMutableList().apply { add(to, removeAt(from)) }
}

/**
 * The Favorite under the name [name]. A name the user emptied out is not a name: it hands the
 * Favorite back the app's own, rather than leaving Home with a blank row.
 */
internal fun Favorite.renamedTo(name: String): Favorite = copy(customLabel = name.trim().ifEmpty { null })

/** Positions rewritten as zero upwards in list order, so that no two Favorites share one. */
private fun List<Favorite>.renumbered(): List<Favorite> = mapIndexed { index, favorite -> favorite.copy(position = index) }

/** A Favorite, the installed app it points at, and the label Home shows for the pair. */
internal data class ShownFavorite(
    val favorite: Favorite,
    val app: LauncherApp?,
) {
    /** The custom label the user gave this Favorite, or the app's own name where they gave none. */
    val label: String
        get() =
            favorite.customLabel
                ?: app?.label
                ?: favorite.appId.packageName
                    .substringAfterLast('.')
                    .replaceFirstChar { it.uppercase() }

    /** The app's own name where known, falling back to the Tombstone's visible name. */
    val appLabel: String get() = app?.label ?: label
}

/**
 * The Favorites in position order, paired with their installed app where it is available.
 * A Favorite whose app is unavailable remains in place as a Tombstone.
 */
internal fun List<Favorite>.shownAmong(installedApps: List<LauncherApp>): List<ShownFavorite> {
    val installedById = installedApps.associateBy(LauncherApp::id)

    return sortedBy(Favorite::position).map { favorite ->
        ShownFavorite(favorite, installedById[favorite.appId])
    }
}

/** The custom label each Favorite carries, for the surfaces that match or show one. */
internal fun List<Favorite>.customLabels(): Map<LauncherAppId, String> =
    mapNotNull { favorite -> favorite.customLabel?.let { favorite.appId to it } }.toMap()
