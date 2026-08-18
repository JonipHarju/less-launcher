package com.jonipharju.less.launcher

import java.text.Normalizer

/** Returns apps matching [query], ordered from strongest match to weakest. */
internal fun Iterable<LauncherApp>.rankedFor(query: String): List<LauncherApp> {
    val normalizedQuery = query.normalizedForSearch().trim()
    if (normalizedQuery.isEmpty()) return alphabetized()

    return mapNotNull { app ->
        app.matchClass(normalizedQuery)?.let { matchClass -> RankedApp(app, matchClass) }
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

private fun LauncherApp.matchClass(query: String): MatchClass? {
    val searchableLabel = label.normalizedForSearch()
    val words = searchableLabel.split(WORD_SEPARATOR).filter(String::isNotEmpty)

    return when {
        searchableLabel == query -> MatchClass.Exact
        searchableLabel.startsWith(query) -> MatchClass.NameStart
        words.any { it.startsWith(query) } -> MatchClass.WordStart
        searchableLabel.contains(query) -> MatchClass.Substring
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
