package com.jonipharju.less.launcher

import java.text.Normalizer

/**
 * Returns apps matching [query], ordered from strongest match to weakest. An app the user has
 * renamed as a Favorite is matched under [customLabels] as well as under its real name, so
 * that their own vocabulary finds it.
 */
internal fun Iterable<LauncherApp>.rankedFor(
    query: String,
    customLabels: Map<LauncherAppId, String> = emptyMap(),
): List<LauncherApp> {
    val normalizedQuery = query.normalizedForSearch().trim()
    if (normalizedQuery.isEmpty()) return alphabetized()

    return mapNotNull { app ->
        app.matchClass(normalizedQuery, customLabels[app.id])?.let { matchClass -> RankedApp(app, matchClass) }
    }.sortedWith(
        compareBy<RankedApp> { it.matchClass }
            .thenBy { it.app.label.normalizedForSearch() }
            .thenBy { it.app.label }
            .thenBy { it.app.id.packageName }
            .thenBy { it.app.id.activityName }
            .thenBy { it.app.id.profileSerialNumber },
    ).map(RankedApp::app)
}

private data class RankedApp(
    val app: LauncherApp,
    val matchClass: MatchClass,
)

private enum class MatchClass {
    Exact,
    NameStart,
    WordStart,
    Substring,
    Initials,
}

/** The strongest class any of the app's names matches under, or null where none of them do. */
private fun LauncherApp.matchClass(
    query: String,
    customLabel: String?,
): MatchClass? = listOfNotNull(label, customLabel).mapNotNull { name -> name.matchClass(query) }.minOrNull()

private fun String.matchClass(query: String): MatchClass? {
    val searchableName = normalizedForSearch()
    val words = searchableName.split(WORD_SEPARATOR).filter(String::isNotEmpty)

    return when {
        searchableName == query -> MatchClass.Exact
        searchableName.startsWith(query) -> MatchClass.NameStart
        words.any { it.startsWith(query) } -> MatchClass.WordStart
        searchableName.contains(query) -> MatchClass.Substring
        words.size > 1 && words.joinToString(separator = "") { it.take(1) }.startsWith(query) ->
            MatchClass.Initials
        else -> null
    }
}

private fun String.normalizedForSearch(): String =
    Normalizer
        .normalize(this, Normalizer.Form.NFD)
        .replace(COMBINING_MARK, "")
        .lowercase()

private val COMBINING_MARK = Regex("\\p{M}+")
private val WORD_SEPARATOR = Regex("[^\\p{L}\\p{N}]+")
