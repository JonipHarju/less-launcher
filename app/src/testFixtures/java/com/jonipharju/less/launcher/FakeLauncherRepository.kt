package com.jonipharju.less.launcher

import com.jonipharju.less.launcher.proto.LauncherUserData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A [LauncherRepository] holding the same stored record a phone holds, edited through the same
 * rules. What it stands in for is the platform: the app list is put there by the test rather
 * than by `LauncherApps`, and launching an app records it instead of starting one.
 */
class FakeLauncherRepository : LauncherRepository {
    private var storedData: LauncherUserData = LauncherUserData.getDefaultInstance()
    private val mutableInstalledApps = MutableStateFlow<List<LauncherApp>>(emptyList())
    private val mutableFavorites = MutableStateFlow<List<Favorite>>(emptyList())
    private val mutableHiddenApps = MutableStateFlow<Set<LauncherAppId>>(emptySet())
    private val mutableSettings = MutableStateFlow(LauncherSettings())
    private val mutableHoldsHomeRole = MutableStateFlow(false)
    private val mutableHasReadStoredSettings = MutableStateFlow(true)
    private val mutableLaunchedApps = mutableListOf<LauncherApp>()
    private val mutableAppInfoShownFor = mutableListOf<LauncherAppId>()
    private val mutableUninstallsRequestedFor = mutableListOf<LauncherAppId>()
    private val mutableWallpapersHung = mutableListOf<Int>()
    private val everydayApps = mutableMapOf<EverydayIntent, LauncherAppId>()
    private var mutableHomeRoleRequests = 0

    override val installedApps = mutableInstalledApps.asStateFlow()
    override val favorites = mutableFavorites.asStateFlow()
    override val hiddenApps = mutableHiddenApps.asStateFlow()
    override val settings = mutableSettings.asStateFlow()
    override val hasReadStoredSettings = mutableHasReadStoredSettings.asStateFlow()
    override val holdsHomeRole = mutableHoldsHomeRole.asStateFlow()
    val launchedApps: List<LauncherApp> = mutableLaunchedApps
    val appInfoShownFor: List<LauncherAppId> = mutableAppInfoShownFor
    val uninstallsRequestedFor: List<LauncherAppId> = mutableUninstallsRequestedFor

    /** The Wallpapers this launcher has hung, in the order the user picked their Themes. */
    val wallpapersHung: List<Int> = mutableWallpapersHung
    val homeRoleRequests: Int get() = mutableHomeRoleRequests

    /** The platform has handed Less the Home Role, which Less has not asked about yet. */
    fun grantHomeRole() {
        mutableHoldsHomeRole.value = true
    }

    /** The platform has made Less the default launcher, and Less has come back and seen it. */
    fun holdHomeRole() {
        grantHomeRole()
        onForeground()
    }

    /** The user has handed the Home Role to another launcher. */
    fun releaseHomeRole() {
        mutableHoldsHomeRole.value = false
    }

    /** A cold start, before what the user stored has been read back. */
    fun withholdStoredSettings() {
        mutableHasReadStoredSettings.value = false
    }

    /** The stored settings arrive, as they do a moment after a cold start. */
    fun readStoredSettings() {
        mutableHasReadStoredSettings.value = true
    }

    /** The user has been through Setup, which is where every surface but Setup begins. */
    fun finishSetup() {
        edit { it.settingsUpdated { settings -> settings.copy(setupStep = SetupStep.Done) } }
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

    /** The package goes, and what the store does about that is the rule the phone applies. */
    fun uninstall(appId: LauncherAppId) {
        mutableInstalledApps.value = mutableInstalledApps.value.filterNot { it.id == appId }
        edit { it.forgetting(appId.packageName, appId.profileSerialNumber) }
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

    override fun onForeground() {
        if (mutableHoldsHomeRole.value) edit(LauncherUserData::recordingHomeRole)
    }

    /** Nothing is registered with the platform, so there is nothing to unregister. */
    override fun close() = Unit

    override fun launch(app: LauncherApp) {
        mutableLaunchedApps += app
    }

    override fun showAppInfo(appId: LauncherAppId) {
        mutableAppInfoShownFor += appId
    }

    /** Whether the system accepts uninstall requests. */
    var uninstallsSucceed = true

    override fun requestUninstall(appId: LauncherAppId): Boolean {
        mutableUninstallsRequestedFor += appId
        return uninstallsSucceed
    }

    override fun requestHomeRole() {
        mutableHomeRoleRequests++
    }

    override fun appAnswering(intent: EverydayIntent): LauncherAppId? = everydayApps[intent]

    override suspend fun chooseTheme(
        themeId: String,
        wallpaperAsset: Int,
    ) {
        edit { it.settingsUpdated { settings -> settings.copy(themeId = themeId) } }
        mutableWallpapersHung += wallpaperAsset
    }

    override suspend fun chooseFavorite(favorite: Favorite) = edit { it.choosing(favorite) }

    override suspend fun dismissFavorite(appId: LauncherAppId) = edit { it.dismissing(appId) }

    override suspend fun hideApp(appId: LauncherAppId) = edit { it.hiding(appId) }

    override suspend fun unhideApp(appId: LauncherAppId) = edit { it.unhiding(appId) }

    override suspend fun reorderFavorites(order: List<LauncherAppId>) = edit { it.reordered(order) }

    override suspend fun updateSettings(update: (LauncherSettings) -> LauncherSettings) = edit { it.settingsUpdated(update) }

    override suspend fun restoreConfiguration(configuration: LauncherConfiguration) = edit { it.restoring(configuration) }

    /**
     * The one write. Every edit goes through the shared rules and is read back out through the
     * shared projections, so this holds no idea of its own about what any of them mean.
     */
    private fun edit(change: (LauncherUserData) -> LauncherUserData) {
        storedData = change(storedData)
        mutableFavorites.value = storedData.storedFavorites()
        mutableHiddenApps.value = storedData.storedHiddenApps()
        mutableSettings.value = storedData.storedSettings()
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
