package com.jonipharju.less.launcher

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.ui.graphics.asImageBitmap
import com.jonipharju.less.launcher.proto.StoredFavorite
import com.jonipharju.less.launcher.proto.StoredHiddenApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.jonipharju.less.launcher.proto.DrawerOpenDirection as StoredDrawerOpenDirection
import com.jonipharju.less.launcher.proto.HomeAlignment as StoredHomeAlignment
import com.jonipharju.less.launcher.proto.IconMode as StoredIconMode
import com.jonipharju.less.launcher.proto.LauncherSettings as StoredLauncherSettings

/** [LauncherRepository] backed by Android's profile-aware launcher APIs. */
class AndroidLauncherRepository(
    context: Context,
) : LauncherRepository,
    AutoCloseable {
    private val context = context.applicationContext
    private val launcherApps = this.context.getSystemService(LauncherApps::class.java)
    private val userManager = this.context.getSystemService(UserManager::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val userDataStore = this.context.launcherUserDataStore
    private val mutableInstalledApps = MutableStateFlow<List<LauncherApp>>(emptyList())
    private var isClosed = false

    override val installedApps = mutableInstalledApps.asStateFlow()
    override val favorites =
        userDataStore.data
            .map { userData -> userData.favoritesList.map(StoredFavorite::toFavorite).sortedBy(Favorite::position) }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())
    override val hiddenApps =
        userDataStore.data
            .map { userData -> userData.hiddenAppsList.map(StoredHiddenApp::toAppId).toSet() }
            .stateIn(scope, SharingStarted.Eagerly, emptySet())
    override val settings =
        userDataStore.data
            .map { userData -> userData.settings.toLauncherSettings() }
            .stateIn(scope, SharingStarted.Eagerly, LauncherSettings())

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
        val intent =
            Intent(Intent.ACTION_DELETE, Uri.fromParts("package", appId.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // A device without a package installer cannot uninstall anything.
        }
    }

    override suspend fun chooseFavorite(favorite: Favorite) {
        userDataStore.updateData { userData ->
            userData
                .toBuilder()
                .clearFavorites()
                .addAllFavorites(
                    (userData.favoritesList.filterNot { it.hasSameAppIdAs(favorite.appId) } + favorite.toProto())
                        .sortedBy(StoredFavorite::getPosition),
                ).build()
        }
    }

    override suspend fun dismissFavorite(appId: LauncherAppId) {
        userDataStore.updateData { userData ->
            userData
                .toBuilder()
                .clearFavorites()
                .addAllFavorites(userData.favoritesList.filterNot { it.hasSameAppIdAs(appId) })
                .build()
        }
    }

    /**
     * An uninstalled app leaves nothing behind: no Favorite, because that removal was
     * intentional, and no record that it was hidden, because there is no longer anything to hide.
     */
    private suspend fun forgetUninstalledPackage(
        packageName: String,
        user: UserHandle,
    ) {
        val profileSerialNumber = userManager.getSerialNumberForUser(user)
        if (profileSerialNumber < 0) return

        userDataStore.updateData { userData ->
            userData
                .toBuilder()
                .clearFavorites()
                .addAllFavorites(
                    userData.favoritesList.filterNot { favorite ->
                        favorite.packageName == packageName &&
                            favorite.profileSerialNumber == profileSerialNumber
                    },
                ).clearHiddenApps()
                .addAllHiddenApps(
                    userData.hiddenAppsList.filterNot { hiddenApp ->
                        hiddenApp.packageName == packageName &&
                            hiddenApp.profileSerialNumber == profileSerialNumber
                    },
                ).build()
        }
    }

    override suspend fun hideApp(appId: LauncherAppId) {
        userDataStore.updateData { userData ->
            if (userData.hiddenAppsList.any { it.toAppId() == appId }) {
                userData
            } else {
                userData.toBuilder().addHiddenApps(appId.toStoredHiddenApp()).build()
            }
        }
    }

    override suspend fun unhideApp(appId: LauncherAppId) {
        userDataStore.updateData { userData ->
            userData
                .toBuilder()
                .clearHiddenApps()
                .addAllHiddenApps(userData.hiddenAppsList.filterNot { it.toAppId() == appId })
                .build()
        }
    }

    override suspend fun reorderFavorites(order: List<LauncherAppId>) {
        userDataStore.updateData { userData ->
            val reordered =
                userData.favoritesList
                    .map(StoredFavorite::toFavorite)
                    .orderedBy(order)
                    .map(Favorite::toProto)

            userData
                .toBuilder()
                .clearFavorites()
                .addAllFavorites(reordered)
                .build()
        }
    }

    override suspend fun updateSettings(update: (LauncherSettings) -> LauncherSettings) {
        userDataStore.updateData { userData ->
            val updated = update(userData.settings.toLauncherSettings())
            userData.toBuilder().setSettings(updated.mergedInto(userData.settings)).build()
        }
    }

    override fun close() {
        if (isClosed) return

        launcherApps.unregisterCallback(launcherCallback)
        context.unregisterReceiver(profileAvailabilityReceiver)
        scope.cancel()
        isClosed = true
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
            launcherApps.getActivityList(null, user).map { activity ->
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

private fun Drawable.toAppIcon(): AppIcon {
    val monochrome = (this as? AdaptiveIconDrawable)?.monochrome
    return AppIcon(original = rendered(), themeable = monochrome?.asAdaptiveIcon()?.rendered())
}

/**
 * A monochrome layer drawn on its own keeps the adaptive icon's safe-zone margin, so its glyph
 * covers barely a third of the bitmap — beside a full-bleed original it reads as a second, smaller
 * icon size. Putting it back inside an [AdaptiveIconDrawable] gives it the original's geometry.
 */
private fun Drawable.asAdaptiveIcon(): AdaptiveIconDrawable = AdaptiveIconDrawable(null, constantState?.newDrawable()?.mutate() ?: this)

private fun Drawable.rendered() =
    Bitmap
        .createBitmap(
            intrinsicWidth.coerceAtLeast(1),
            intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        ).also { bitmap ->
            setBounds(0, 0, bitmap.width, bitmap.height)
            draw(Canvas(bitmap))
        }.asImageBitmap()

private fun Favorite.toProto(): StoredFavorite =
    StoredFavorite
        .newBuilder()
        .setPackageName(appId.packageName)
        .setActivityName(appId.activityName)
        .setProfileSerialNumber(appId.profileSerialNumber)
        .setPosition(position)
        .also { builder -> customLabel?.let(builder::setCustomLabel) }
        .build()

private fun StoredFavorite.toFavorite() =
    Favorite(
        appId = LauncherAppId(packageName, activityName, profileSerialNumber),
        position = position,
        customLabel = if (hasCustomLabel()) customLabel else null,
    )

private fun LauncherAppId.toStoredHiddenApp(): StoredHiddenApp =
    StoredHiddenApp
        .newBuilder()
        .setPackageName(packageName)
        .setActivityName(activityName)
        .setProfileSerialNumber(profileSerialNumber)
        .build()

private fun StoredHiddenApp.toAppId() = LauncherAppId(packageName, activityName, profileSerialNumber)

private fun StoredFavorite.hasSameAppIdAs(appId: LauncherAppId) =
    packageName == appId.packageName &&
        activityName == appId.activityName &&
        profileSerialNumber == appId.profileSerialNumber

/** Every unset field falls back to the default the domain type declares, not to a repeated literal. */
private fun StoredLauncherSettings.toLauncherSettings(): LauncherSettings {
    val defaults = LauncherSettings()
    return LauncherSettings(
        iconModeOverride = if (hasIconModeOverride()) iconModeOverride.toIconMode() else defaults.iconModeOverride,
        drawerOpenDirection = drawerOpenDirection.toDrawerOpenDirection() ?: defaults.drawerOpenDirection,
        homeAlignment = homeAlignment.toHomeAlignment() ?: defaults.homeAlignment,
        opensKeyboardWithDrawer =
            if (hasOpensKeyboardWithDrawer()) opensKeyboardWithDrawer else defaults.opensKeyboardWithDrawer,
        themeId = themeId.takeIf { it.isNotEmpty() } ?: defaults.themeId,
    )
}

/** Writes onto [stored] rather than over it, so a field this type does not model survives. */
private fun LauncherSettings.mergedInto(stored: StoredLauncherSettings): StoredLauncherSettings =
    stored
        .toBuilder()
        .clearIconModeOverride()
        .setDrawerOpenDirection(drawerOpenDirection.toProto())
        .setHomeAlignment(homeAlignment.toProto())
        .setOpensKeyboardWithDrawer(opensKeyboardWithDrawer)
        .setThemeId(themeId)
        .also { builder -> iconModeOverride?.let { builder.iconModeOverride = it.toProto() } }
        .build()

private fun DrawerOpenDirection.toProto() =
    when (this) {
        DrawerOpenDirection.SwipeUp -> StoredDrawerOpenDirection.DRAWER_OPEN_DIRECTION_SWIPE_UP
        DrawerOpenDirection.SwipeDown -> StoredDrawerOpenDirection.DRAWER_OPEN_DIRECTION_SWIPE_DOWN
    }

private fun StoredDrawerOpenDirection.toDrawerOpenDirection(): DrawerOpenDirection? =
    when (this) {
        StoredDrawerOpenDirection.DRAWER_OPEN_DIRECTION_SWIPE_UP -> DrawerOpenDirection.SwipeUp
        StoredDrawerOpenDirection.DRAWER_OPEN_DIRECTION_SWIPE_DOWN -> DrawerOpenDirection.SwipeDown
        StoredDrawerOpenDirection.DRAWER_OPEN_DIRECTION_UNSPECIFIED,
        StoredDrawerOpenDirection.UNRECOGNIZED,
        -> null
    }

private fun HomeAlignment.toProto() =
    when (this) {
        HomeAlignment.Left -> StoredHomeAlignment.HOME_ALIGNMENT_LEFT
        HomeAlignment.Centred -> StoredHomeAlignment.HOME_ALIGNMENT_CENTRED
    }

private fun StoredHomeAlignment.toHomeAlignment(): HomeAlignment? =
    when (this) {
        StoredHomeAlignment.HOME_ALIGNMENT_LEFT -> HomeAlignment.Left
        StoredHomeAlignment.HOME_ALIGNMENT_CENTRED -> HomeAlignment.Centred
        StoredHomeAlignment.HOME_ALIGNMENT_UNSPECIFIED,
        StoredHomeAlignment.UNRECOGNIZED,
        -> null
    }

private fun IconMode.toProto() =
    when (this) {
        IconMode.Original -> StoredIconMode.ICON_MODE_ORIGINAL
        IconMode.Tinted -> StoredIconMode.ICON_MODE_TINTED
        IconMode.Off -> StoredIconMode.ICON_MODE_OFF
    }

private fun StoredIconMode.toIconMode(): IconMode? =
    when (this) {
        StoredIconMode.ICON_MODE_ORIGINAL -> IconMode.Original
        StoredIconMode.ICON_MODE_TINTED -> IconMode.Tinted
        StoredIconMode.ICON_MODE_OFF -> IconMode.Off
        StoredIconMode.ICON_MODE_UNSPECIFIED,
        StoredIconMode.UNRECOGNIZED,
        -> null
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
