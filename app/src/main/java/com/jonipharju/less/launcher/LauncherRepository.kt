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
    val setupStep: SetupStep = SetupStep.Theme,
    val hasHeldHomeRole: Boolean = false,
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

    /**
     * Whether what the user stored has been read yet. Until it has, [settings] is standing in
     * with its own defaults — and one of those is that Setup has never run, so no surface may
     * act on them before this turns true.
     */
    val hasReadStoredSettings: StateFlow<Boolean>

    /** Whether Less is the default launcher — whether the platform has given it the Home Role. */
    val holdsHomeRole: StateFlow<Boolean>

    fun launch(app: LauncherApp)

    /** Opens the system's own page for [appId], where the OS explains and controls the app. */
    fun showAppInfo(appId: LauncherAppId)

    /** Asks the system to uninstall [appId]. The OS, not Less, confirms it with the user. */
    fun requestUninstall(appId: LauncherAppId)

    /**
     * Asks the platform to hand Less the Home Role. The OS owns the answer; Less only learns it
     * by asking again whether it holds the role.
     */
    fun requestHomeRole()

    /**
     * The installed app the platform answers [intent] with, or null where the device has none.
     * Resolved by what the user wants done rather than by package name, so Setup proposes
     * Favorites that exist on the device in front of it.
     */
    fun appAnswering(intent: EverydayIntent): LauncherAppId?

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

    /**
     * Puts [configuration] in place of everything stored, in one write, so that a half-read file
     * cannot leave the launcher part one setup and part another. What the device answers for
     * itself — how far Setup got, whether Less has held the Home Role — is left as it stands.
     */
    suspend fun restoreConfiguration(configuration: LauncherConfiguration)
}

internal fun Iterable<LauncherApp>.alphabetized(): List<LauncherApp> =
    sortedWith(
        compareBy<LauncherApp> { it.label.lowercase() }
            .thenBy { it.label }
            .thenBy { it.id.packageName }
            .thenBy { it.id.activityName }
            .thenBy { it.id.profileSerialNumber },
    )
