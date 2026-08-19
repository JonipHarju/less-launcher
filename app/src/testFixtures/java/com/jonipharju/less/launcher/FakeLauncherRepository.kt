package com.jonipharju.less.launcher

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeLauncherRepository : LauncherRepository {
    private val mutableInstalledApps = MutableStateFlow<List<LauncherApp>>(emptyList())
    private val mutableFavorites = MutableStateFlow<List<Favorite>>(emptyList())
    private val mutableHiddenApps = MutableStateFlow<Set<LauncherAppId>>(emptySet())
    private val mutableSettings = MutableStateFlow(LauncherSettings())
    private val mutableHoldsHomeRole = MutableStateFlow(false)
    private val mutableLaunchedApps = mutableListOf<LauncherApp>()
    private val mutableAppInfoShownFor = mutableListOf<LauncherAppId>()
    private val mutableUninstallsRequestedFor = mutableListOf<LauncherAppId>()
    private val everydayApps = mutableMapOf<EverydayIntent, LauncherAppId>()
    private var mutableHomeRoleRequests = 0

    override val installedApps = mutableInstalledApps.asStateFlow()
    override val favorites = mutableFavorites.asStateFlow()
    override val hiddenApps = mutableHiddenApps.asStateFlow()
    override val settings = mutableSettings.asStateFlow()
    override val holdsHomeRole = mutableHoldsHomeRole.asStateFlow()
    val launchedApps: List<LauncherApp> = mutableLaunchedApps
    val appInfoShownFor: List<LauncherAppId> = mutableAppInfoShownFor
    val uninstallsRequestedFor: List<LauncherAppId> = mutableUninstallsRequestedFor
    val homeRoleRequests: Int get() = mutableHomeRoleRequests

    /** The platform has made Less the default launcher, which Less records as the real one does. */
    fun holdHomeRole() {
        mutableHoldsHomeRole.value = true
        mutableSettings.value = mutableSettings.value.copy(hasHeldHomeRole = true)
    }

    /** The user has handed the Home Role to another launcher. */
    fun releaseHomeRole() {
        mutableHoldsHomeRole.value = false
    }

    /** The user has been through Setup, which is where every surface but Setup begins. */
    fun finishSetup() {
        mutableSettings.value = mutableSettings.value.copy(setupStep = SetupStep.Done)
    }

    /** The device answers [intent] with [appId]. */
    fun answer(
        intent: EverydayIntent,
        appId: LauncherAppId,
    ) {
        everydayApps[intent] = appId
    }

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

    override fun requestHomeRole() {
        mutableHomeRoleRequests++
    }

    override fun appAnswering(intent: EverydayIntent): LauncherAppId? = everydayApps[intent]

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
