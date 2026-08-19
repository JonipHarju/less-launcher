package com.jonipharju.less.launcher

/**
 * The installed apps the Drawer lists: everything but the Hidden Apps. Hiding is cosmetic —
 * the app stays installed and launchable, it just stops being listed here.
 */
internal fun List<LauncherApp>.withoutHidden(hidden: Set<LauncherAppId>): List<LauncherApp> = filterNot { it.id in hidden }

/**
 * The Hidden Apps themselves, so that Settings can show what was hidden and offer it back. An
 * id whose app is no longer installed lists nothing: there is nothing left to unhide.
 */
internal fun List<LauncherApp>.hiddenAmong(hidden: Set<LauncherAppId>): List<LauncherApp> = filter { it.id in hidden }.alphabetized()
