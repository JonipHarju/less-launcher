package com.jonipharju.less.launcher

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeLauncherRepository : LauncherRepository {
    private val mutableInstalledApps = MutableStateFlow<List<LauncherApp>>(emptyList())
    private val mutableFavorites = MutableStateFlow<List<Favorite>>(emptyList())
    private val mutableSettings = MutableStateFlow(LauncherSettings())
    private val mutableLaunchedApps = mutableListOf<LauncherApp>()

    override val installedApps = mutableInstalledApps.asStateFlow()
    override val favorites = mutableFavorites.asStateFlow()
    override val settings = mutableSettings.asStateFlow()
    val launchedApps: List<LauncherApp> = mutableLaunchedApps

    fun install(app: LauncherApp) {
        mutableInstalledApps.value = (mutableInstalledApps.value + app).alphabetized()
    }

    fun uninstall(appId: LauncherAppId) {
        mutableInstalledApps.value = mutableInstalledApps.value.filterNot { it.id == appId }
    }

    fun update(app: LauncherApp) {
        mutableInstalledApps.value =
            mutableInstalledApps.value
                .map { installedApp ->
                    if (installedApp.id == app.id) app else installedApp
                }.alphabetized()
    }

    override fun launch(app: LauncherApp) {
        mutableLaunchedApps += app
    }

    override suspend fun chooseFavorite(favorite: Favorite) {
        mutableFavorites.value =
            (mutableFavorites.value.filterNot { it.appId == favorite.appId } + favorite)
                .sortedBy(Favorite::position)
    }

    override suspend fun dismissFavorite(appId: LauncherAppId) {
        mutableFavorites.value = mutableFavorites.value.filterNot { it.appId == appId }
    }

    override suspend fun updateSettings(update: (LauncherSettings) -> LauncherSettings) {
        mutableSettings.value = update(mutableSettings.value)
    }
}

fun launcherAppFixture(label: String) =
    LauncherApp(
        id =
            LauncherAppId(
                packageName = "com.example.${label.lowercase()}",
                activityName = "com.example.${label}Activity",
                profileSerialNumber = 0,
            ),
        label = label,
    )
