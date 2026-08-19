package com.jonipharju.less.launcher

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeLauncherRepository : LauncherRepository {
    private val mutableInstalledApps = MutableStateFlow<List<LauncherApp>>(emptyList())
    private val mutableFavorites = MutableStateFlow<List<Favorite>>(emptyList())
    private val mutableHiddenApps = MutableStateFlow<Set<LauncherAppId>>(emptySet())
    private val mutableSettings = MutableStateFlow(LauncherSettings())
    private val mutableLaunchedApps = mutableListOf<LauncherApp>()
    private val mutableAppInfoShownFor = mutableListOf<LauncherAppId>()
    private val mutableUninstallsRequestedFor = mutableListOf<LauncherAppId>()

    override val installedApps = mutableInstalledApps.asStateFlow()
    override val favorites = mutableFavorites.asStateFlow()
    override val hiddenApps = mutableHiddenApps.asStateFlow()
    override val settings = mutableSettings.asStateFlow()
    val launchedApps: List<LauncherApp> = mutableLaunchedApps
    val appInfoShownFor: List<LauncherAppId> = mutableAppInfoShownFor
    val uninstallsRequestedFor: List<LauncherAppId> = mutableUninstallsRequestedFor

    fun install(app: LauncherApp) {
        mutableInstalledApps.value = (mutableInstalledApps.value + app).alphabetized()
    }

    fun uninstall(appId: LauncherAppId) {
        mutableInstalledApps.value = mutableInstalledApps.value.filterNot { it.id == appId }
        mutableFavorites.value = mutableFavorites.value.filterNot { it.appId == appId }
        mutableHiddenApps.value = mutableHiddenApps.value - appId
    }

    fun makeUnavailable(appId: LauncherAppId) {
        mutableInstalledApps.value = mutableInstalledApps.value.filterNot { it.id == appId }
    }

    fun makeAvailable(app: LauncherApp) {
        install(app)
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

    override fun showAppInfo(appId: LauncherAppId) {
        mutableAppInfoShownFor += appId
    }

    override fun requestUninstall(appId: LauncherAppId) {
        mutableUninstallsRequestedFor += appId
    }

    override suspend fun chooseFavorite(favorite: Favorite) {
        mutableFavorites.value =
            (mutableFavorites.value.filterNot { it.appId == favorite.appId } + favorite)
                .sortedBy(Favorite::position)
    }

    override suspend fun dismissFavorite(appId: LauncherAppId) {
        mutableFavorites.value = mutableFavorites.value.filterNot { it.appId == appId }
    }

    override suspend fun hideApp(appId: LauncherAppId) {
        mutableHiddenApps.value = mutableHiddenApps.value + appId
    }

    override suspend fun unhideApp(appId: LauncherAppId) {
        mutableHiddenApps.value = mutableHiddenApps.value - appId
    }

    override suspend fun reorderFavorites(order: List<LauncherAppId>) {
        mutableFavorites.value = mutableFavorites.value.orderedBy(order)
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
