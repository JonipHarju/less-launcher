package com.jonipharju.less.launcher

import kotlinx.coroutines.flow.StateFlow

/** A launchable activity and the profile that owns it. */
data class LauncherApp(
    val id: LauncherAppId,
    val label: String,
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

data class LauncherSettings(
    val iconModeOverride: IconMode? = null,
)

enum class IconMode {
    Original,
    Tinted,
    Hidden,
}

/** The boundary between launcher behavior and Android's launcher APIs. */
interface LauncherRepository {
    val installedApps: StateFlow<List<LauncherApp>>
    val favorites: StateFlow<List<Favorite>>
    val settings: StateFlow<LauncherSettings>

    fun launch(app: LauncherApp)

    suspend fun chooseFavorite(favorite: Favorite)

    suspend fun dismissFavorite(appId: LauncherAppId)

    suspend fun updateSettings(settings: LauncherSettings)
}

internal fun Iterable<LauncherApp>.alphabetized(): List<LauncherApp> =
    sortedWith(
        compareBy<LauncherApp> { it.label.lowercase() }
            .thenBy { it.label }
            .thenBy { it.id.packageName }
            .thenBy { it.id.activityName }
            .thenBy { it.id.profileSerialNumber },
    )
