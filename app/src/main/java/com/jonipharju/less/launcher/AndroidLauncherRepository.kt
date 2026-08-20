package com.jonipharju.less.launcher

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.provider.MediaStore
import android.provider.Settings
import com.jonipharju.less.launcher.proto.LauncherUserData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** [LauncherRepository] backed by Android's profile-aware launcher APIs. */
class AndroidLauncherRepository(
    context: Context,
) : LauncherRepository,
    AutoCloseable {
    private val context = context.applicationContext
    private val launcherApps = this.context.getSystemService(LauncherApps::class.java)
    private val userManager = this.context.getSystemService(UserManager::class.java)
    private val roleManager = this.context.getSystemService(RoleManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val userDataStore = this.context.launcherUserDataStore
    private val mutableInstalledApps = MutableStateFlow<List<LauncherApp>>(emptyList())
    private val mutableHoldsHomeRole = MutableStateFlow(false)
    private val mutableHasReadStoredSettings = MutableStateFlow(false)
    private val ownProfileSerialNumber = userManager.getSerialNumberForUser(Process.myUserHandle())
    private var isClosed = false

    override val installedApps = mutableInstalledApps.asStateFlow()
    override val holdsHomeRole = mutableHoldsHomeRole.asStateFlow()
    override val favorites =
        userDataStore.data
            .map(LauncherUserData::storedFavorites)
            .stateIn(scope, SharingStarted.Eagerly, emptyList())
    override val hiddenApps =
        userDataStore.data
            .map(LauncherUserData::storedHiddenApps)
            .stateIn(scope, SharingStarted.Eagerly, emptySet())
    override val settings =
        userDataStore.data
            .map(LauncherUserData::storedSettings)
            .onEach { mutableHasReadStoredSettings.value = true }
            .stateIn(scope, SharingStarted.Eagerly, LauncherSettings())
    override val hasReadStoredSettings = mutableHasReadStoredSettings.asStateFlow()

    private val launcherCallback =
        object : LauncherApps.Callback() {
            override fun onPackageAdded(
                packageName: String,
                user: UserHandle,
            ) = refresh()

            override fun onPackageChanged(
                packageName: String,
                user: UserHandle,
            ) = refresh()

            override fun onPackageRemoved(
                packageName: String,
                user: UserHandle,
            ) {
                scope.launch {
                    forgetUninstalledPackage(packageName, user)
                    refresh()
                }
            }

            override fun onPackagesAvailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean,
            ) = refresh()

            override fun onPackagesUnavailable(
                packageNames: Array<out String>,
                user: UserHandle,
                replacing: Boolean,
            ) = refresh()
        }

    private val profileAvailabilityReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) = refresh()
        }

    init {
        refreshHomeRole()
        launcherApps.registerCallback(launcherCallback)
        context.registerReceiver(
            profileAvailabilityReceiver,
            profileAvailabilityIntentFilter(),
            Context.RECEIVER_EXPORTED,
        )
        refresh()
    }

    override fun launch(app: LauncherApp) {
        val user = userManager.getUserForSerialNumber(app.id.profileSerialNumber) ?: return
        launcherApps.startMainActivity(
            ComponentName(app.id.packageName, app.id.activityName),
            user,
            null,
            null,
        )
    }

    override fun showAppInfo(appId: LauncherAppId) {
        val user = userManager.getUserForSerialNumber(appId.profileSerialNumber) ?: return
        launcherApps.startAppDetailsActivity(
            ComponentName(appId.packageName, appId.activityName),
            user,
            null,
            null,
        )
    }

    /**
     * The platform offers no profile-aware uninstall, so this asks the system in the ordinary
     * way and lets it decide. A work app declines rather than uninstalling the personal one.
     */
    override fun requestUninstall(appId: LauncherAppId) {
        // A device without a package installer cannot uninstall anything.
        startActivitySafely(Intent(Intent.ACTION_DELETE, Uri.fromParts("package", appId.packageName, null)))
    }

    /**
     * The role request is the platform's own way of asking, and the one the user recognises.
     * Where the device does not offer it, the home-app settings screen still does.
     */
    override fun requestHomeRole() {
        val roleRequest =
            roleManager
                ?.takeIf { it.isRoleAvailable(RoleManager.ROLE_HOME) }
                ?.createRequestRoleIntent(RoleManager.ROLE_HOME)

        if (roleRequest != null && startActivitySafely(roleRequest)) return

        startActivitySafely(Intent(Settings.ACTION_HOME_SETTINGS))
    }

    /**
     * The activity answering an Everyday Intent is rarely the one the Drawer lists, so the answer
     * is matched back to the app's own entry by package rather than used as a component. Where
     * several apps answer and the user has picked no default the platform names its own chooser,
     * which matches no installed app and so falls through to the apps that actually answered.
     */
    override fun appAnswering(intent: EverydayIntent): LauncherAppId? {
        val query = intent.asIntent()
        val packageManager = context.packageManager
        val chosen = packageManager.resolveActivity(query, PackageManager.MATCH_DEFAULT_ONLY)
        val offered = packageManager.queryIntentActivities(query, PackageManager.MATCH_DEFAULT_ONLY)

        return (listOfNotNull(chosen) + offered)
            .firstNotNullOfOrNull { answer -> appNamed(answer.activityInfo.packageName) }
            ?.id
    }

    /** The Drawer's own entry for [packageName], preferring the personal profile's copy of it. */
    private fun appNamed(packageName: String): LauncherApp? {
        val candidates = mutableInstalledApps.value.filter { it.id.packageName == packageName }

        return candidates.firstOrNull { it.id.profileSerialNumber == ownProfileSerialNumber }
            ?: candidates.firstOrNull()
    }

    /**
     * Asks the platform again whether Less is the default launcher. The OS gives no signal when
     * the role changes hands, so the activity asks each time it comes back to the foreground.
     */
    fun refreshHomeRole() {
        val holdsRole = roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true
        mutableHoldsHomeRole.value = holdsRole

        // Holding it once is recorded for good, so that the Drawer's prompt does not come back
        // the day the user hands the role to another launcher.
        if (holdsRole && !settings.value.hasHeldHomeRole) {
            scope.launch { updateSettings { it.copy(hasHeldHomeRole = true) } }
        }
    }

    override suspend fun chooseFavorite(favorite: Favorite) {
        userDataStore.updateData { userData -> userData.choosing(favorite) }
    }

    override suspend fun dismissFavorite(appId: LauncherAppId) {
        userDataStore.updateData { userData -> userData.dismissing(appId) }
    }

    /** The platform names the package that went; what that leaves behind is the same everywhere. */
    private suspend fun forgetUninstalledPackage(
        packageName: String,
        user: UserHandle,
    ) {
        val profileSerialNumber = userManager.getSerialNumberForUser(user)
        if (profileSerialNumber < 0) return

        userDataStore.updateData { userData -> userData.forgetting(packageName, profileSerialNumber) }
    }

    override suspend fun hideApp(appId: LauncherAppId) {
        userDataStore.updateData { userData -> userData.hiding(appId) }
    }

    override suspend fun unhideApp(appId: LauncherAppId) {
        userDataStore.updateData { userData -> userData.unhiding(appId) }
    }

    override suspend fun reorderFavorites(order: List<LauncherAppId>) {
        userDataStore.updateData { userData -> userData.reordered(order) }
    }

    override suspend fun updateSettings(update: (LauncherSettings) -> LauncherSettings) {
        userDataStore.updateData { userData -> userData.settingsUpdated(update) }
    }

    override suspend fun restoreConfiguration(configuration: LauncherConfiguration) {
        userDataStore.updateData { userData -> userData.restoring(configuration) }
    }

    override fun close() {
        if (isClosed) return

        launcherApps.unregisterCallback(launcherCallback)
        context.unregisterReceiver(profileAvailabilityReceiver)
        scope.cancel()
        isClosed = true
    }

    private fun startActivitySafely(intent: Intent): Boolean =
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }

    private fun refresh() {
        mutableInstalledApps.value =
            userManager.userProfiles
                .flatMap(::installedAppsFor)
                .alphabetized()
    }

    private fun installedAppsFor(user: UserHandle): List<LauncherApp> {
        val profileSerialNumber = userManager.getSerialNumberForUser(user)
        if (profileSerialNumber < 0) return emptyList()

        return try {
            launcherApps
                .getActivityList(null, user)
                // Less now carries a LAUNCHER icon so other launchers can open it, which also
                // means it comes back in its own query. It is the Drawer, not an app in it.
                .filterNot { it.componentName.packageName == context.packageName }
                .map { activity ->
                    LauncherApp(
                        id =
                            LauncherAppId(
                                packageName = activity.componentName.packageName,
                                activityName = activity.componentName.className,
                                profileSerialNumber = profileSerialNumber,
                            ),
                        label = activity.label.toString(),
                        icon = activity.getIcon(context.resources.displayMetrics.densityDpi).toAppIcon(),
                    )
                }
        } catch (_: SecurityException) {
            emptyList()
        }
    }
}

/** What the user wants done, as the intent the platform resolves to whichever app does it. */
private fun EverydayIntent.asIntent(): Intent =
    when (this) {
        EverydayIntent.Phone -> Intent(Intent.ACTION_DIAL)
        EverydayIntent.Messaging -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING)
        EverydayIntent.Camera -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        EverydayIntent.Browser -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER)
    }

private fun profileAvailabilityIntentFilter() =
    IntentFilter().apply {
        addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
        addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
        addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            addAction(Intent.ACTION_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_PROFILE_UNAVAILABLE)
            addAction(Intent.ACTION_PROFILE_ACCESSIBLE)
            addAction(Intent.ACTION_PROFILE_INACCESSIBLE)
        }
    }
