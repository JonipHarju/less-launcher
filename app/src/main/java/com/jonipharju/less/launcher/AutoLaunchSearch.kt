package com.jonipharju.less.launcher

/**
 * Three characters is the whole safety property: a one- or two-letter prefix is still
 * on the way to a name, and must not fire under the finger even when only one app matches.
 */
internal const val MIN_AUTO_LAUNCH_QUERY_LENGTH = 3

/**
 * The app a forward-typed Drawer query should open on its own, or null while the user is
 * still choosing. Ranked results are already Hidden-App-free and already match a Favorite's
 * custom label; this only reads their count.
 *
 * A delete is a retreat, not a choice. A query that already launched stays quiet if the
 * user keeps typing into the same match, so recomposing or growing "out" into "outlook"
 * cannot fire again.
 */
internal fun appToLaunchForTypedQuery(
    query: String,
    ranked: List<LauncherApp>,
    previousQuery: String,
    launchedQuery: String? = null,
): LauncherApp? {
    if (query.length < MIN_AUTO_LAUNCH_QUERY_LENGTH) return null
    if (ranked.size != 1) return null
    if (!isForwardTyped(previousQuery, query)) return null
    if (launchedQuery != null && query.startsWith(launchedQuery)) return null
    return ranked.single()
}

private fun isForwardTyped(
    previousQuery: String,
    query: String,
): Boolean = query.length > previousQuery.length && query.startsWith(previousQuery)
