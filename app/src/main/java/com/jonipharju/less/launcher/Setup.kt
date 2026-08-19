package com.jonipharju.less.launcher

/**
 * How far a new user has got through Setup. The order is deliberate: Less shows what it looks
 * like before it asks the user for anything, and only then asks to become their launcher.
 */
enum class SetupStep {
    Theme,
    DefaultLauncher,
    Favorites,
    Done,
}

/**
 * An everyday thing a phone does, named by what the user wants rather than by a package. Setup
 * asks the platform which app answers each one, so the Favorites it proposes work on any device.
 */
enum class EverydayIntent {
    Phone,
    Messaging,
    Camera,
    Browser,
}

/** The step Setup moves to when the user finishes [this] one. */
internal fun SetupStep.next(): SetupStep = SetupStep.entries.getOrElse(ordinal + 1) { SetupStep.Done }

/** The step Setup returns to when the user backs out of [this] one, or null from the first. */
internal fun SetupStep.previous(): SetupStep? = SetupStep.entries.getOrNull(ordinal - 1)

/** Whether Setup is still running, and so is drawn in place of Home, the Drawer and Settings. */
internal fun SetupStep.isRunning(): Boolean = this != SetupStep.Done

/**
 * Whether the Drawer still asks to be made the default launcher. Less asks until it has held
 * the Home Role once; after that the prompt is gone for good, because handing the role to
 * another launcher is a choice, and a launcher that keeps asking is a launcher that nags.
 */
internal fun LauncherSettings.asksForHomeRole(holdsHomeRole: Boolean): Boolean = !hasHeldHomeRole && !holdsHomeRole

/**
 * The Favorites Setup proposes: the installed app answering each everyday intent, in the order
 * the intents are named. One app answering two of them is proposed once, and an intent no app
 * on the device answers is simply left out.
 */
internal fun List<LauncherApp>.answering(answer: (EverydayIntent) -> LauncherAppId?): List<LauncherApp> {
    val installedById = associateBy(LauncherApp::id)

    return EverydayIntent.entries
        .mapNotNull { intent -> answer(intent)?.let(installedById::get) }
        .distinct()
}

/**
 * The apps the Favorites picker lists: the proposed ones first, in the order Setup proposes
 * them, then everything else alphabetically. Home ends up in the order the picker was read.
 */
internal fun List<LauncherApp>.proposedFirst(proposed: List<LauncherApp>): List<LauncherApp> =
    proposed + filterNot { app -> proposed.any { it.id == app.id } }.alphabetized()

/** The Favorites that choosing [chosen], in listed order, puts on Home. */
internal fun List<LauncherApp>.asFavorites(chosen: Set<LauncherAppId>): List<Favorite> =
    filter { it.id in chosen }.mapIndexed { position, app -> Favorite(appId = app.id, position = position) }
