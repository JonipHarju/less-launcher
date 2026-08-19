package com.jonipharju.less.launcher

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.StateFlow

/** Original artwork and, when supplied by the app, its themeable monochrome layer. */
data class AppIcon(
    val original: ImageBitmap,
    val themeable: ImageBitmap?,
)

/** A launchable activity and the profile that owns it. */
data class LauncherApp(
    val id: LauncherAppId,
    val label: String,
    val icon: AppIcon? = null,
)

/** Platform-neutral identity for an installed launchable activity. */
data class LauncherAppId(
    val packageName: String,
    val activityName: String,
    val profileSerialNumber: Long,
)

/** An app deliberately placed at a position on Home. */
data class Favorite(
    val appId: LauncherAppId,
    val position: Int,
    val customLabel: String? = null,
)

val DefaultThemeId = "near-black"

data class LauncherSettings(
    val iconModeOverride: IconMode? = null,
    val drawerOpenDirection: DrawerOpenDirection = DrawerOpenDirection.SwipeUp,
    val homeAlignment: HomeAlignment = HomeAlignment.Left,
    val opensKeyboardWithDrawer: Boolean = true,
    val themeId: String = DefaultThemeId,
)

enum class IconMode {
    Original,
    Tinted,
    Off,
}

/** The swipe on Home that opens the Drawer. The inverse swipe closes it again. */
enum class DrawerOpenDirection {
    SwipeUp,
    SwipeDown,
}

/** Where Home lays out its clock, date, and Favorites across the width of the screen. */
enum class HomeAlignment {
    Left,
    Centred,
}

/** The boundary between launcher behavior and Android's launcher APIs. */
interface LauncherRepository {
    val installedApps: StateFlow<List<LauncherApp>>
    val favorites: StateFlow<List<Favorite>>

    /** The apps the user has excluded from the Drawer. They stay installed and launchable. */
    val hiddenApps: StateFlow<Set<LauncherAppId>>
    val settings: StateFlow<LauncherSettings>

    fun launch(app: LauncherApp)

    /** Opens the system's own page for [appId], where the OS explains and controls the app. */
    fun showAppInfo(appId: LauncherAppId)

    /** Asks the system to uninstall [appId]. The OS, not Less, confirms it with the user. */
    fun requestUninstall(appId: LauncherAppId)

    suspend fun chooseFavorite(favorite: Favorite)

    suspend fun dismissFavorite(appId: LauncherAppId)

    /** Takes [appId] out of the Drawer. Hiding it again changes nothing. */
    suspend fun hideApp(appId: LauncherAppId)

    /** Puts [appId] back in the Drawer. Unhiding an app that was never hidden changes nothing. */
    suspend fun unhideApp(appId: LauncherAppId)

    /**
     * Rewrites every Favorite's position to the order [order] names, in one write, so that a
     * drag cannot leave Home half-reordered. Custom labels survive untouched.
     */
    suspend fun reorderFavorites(order: List<LauncherAppId>)

    /**
     * Applies [update] to whatever is stored at the time of the write, so that two settings
     * changed in quick succession cannot each overwrite the other with a stale record.
     */
    suspend fun updateSettings(update: (LauncherSettings) -> LauncherSettings)
}

internal fun Iterable<LauncherApp>.alphabetized(): List<LauncherApp> =
    sortedWith(
        compareBy<LauncherApp> { it.label.lowercase() }
            .thenBy { it.label }
            .thenBy { it.id.packageName }
            .thenBy { it.id.activityName }
            .thenBy { it.id.profileSerialNumber },
    )
