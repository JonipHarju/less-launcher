package com.jonipharju.less.launcher

import com.jonipharju.less.launcher.proto.StoredFavorite
import com.jonipharju.less.launcher.proto.StoredHiddenApp
import com.jonipharju.less.launcher.proto.DrawerOpenDirection as StoredDrawerOpenDirection
import com.jonipharju.less.launcher.proto.HomeAlignment as StoredHomeAlignment
import com.jonipharju.less.launcher.proto.IconMode as StoredIconMode
import com.jonipharju.less.launcher.proto.LauncherSettings as StoredLauncherSettings
import com.jonipharju.less.launcher.proto.SetupStep as StoredSetupStep

// What the launcher stores, and what it means. Both the store and the exported Configuration
// read the same records, so the translation between proto and domain lives in one place.

/** A Favorite as it is stored: the app it names, where it sits, and what the user calls it. */
internal fun Favorite.toProto(): StoredFavorite =
    StoredFavorite
        .newBuilder()
        .setPackageName(appId.packageName)
        .setActivityName(appId.activityName)
        .setProfileSerialNumber(appId.profileSerialNumber)
        .setPosition(position)
        .also { builder -> customLabel?.let(builder::setCustomLabel) }
        .build()

internal fun StoredFavorite.toFavorite() =
    Favorite(
        appId = LauncherAppId(packageName, activityName, profileSerialNumber),
        position = position,
        customLabel = if (hasCustomLabel()) customLabel else null,
    )

internal fun LauncherAppId.toStoredHiddenApp(): StoredHiddenApp =
    StoredHiddenApp
        .newBuilder()
        .setPackageName(packageName)
        .setActivityName(activityName)
        .setProfileSerialNumber(profileSerialNumber)
        .build()

internal fun StoredHiddenApp.toAppId() = LauncherAppId(packageName, activityName, profileSerialNumber)

internal fun StoredFavorite.hasSameAppIdAs(appId: LauncherAppId) =
    packageName == appId.packageName &&
        activityName == appId.activityName &&
        profileSerialNumber == appId.profileSerialNumber

internal fun StoredHiddenApp.hasSameAppIdAs(appId: LauncherAppId) =
    packageName == appId.packageName &&
        activityName == appId.activityName &&
        profileSerialNumber == appId.profileSerialNumber

/** Every unset field falls back to the default the domain type declares, not to a repeated literal. */
internal fun StoredLauncherSettings.toLauncherSettings(): LauncherSettings {
    val defaults = LauncherSettings()
    return LauncherSettings(
        iconModeOverride = if (hasIconModeOverride()) iconModeOverride.toIconMode() else defaults.iconModeOverride,
        drawerOpenDirection = drawerOpenDirection.toDrawerOpenDirection() ?: defaults.drawerOpenDirection,
        homeAlignment = homeAlignment.toHomeAlignment() ?: defaults.homeAlignment,
        opensKeyboardWithDrawer =
            if (hasOpensKeyboardWithDrawer()) opensKeyboardWithDrawer else defaults.opensKeyboardWithDrawer,
        themeId = themeId.takeIf { it.isNotEmpty() } ?: defaults.themeId,
        setupStep = setupStep.toSetupStep() ?: defaults.setupStep,
        hasHeldHomeRole = if (hasHasHeldHomeRole()) hasHeldHomeRole else defaults.hasHeldHomeRole,
    )
}

/** Writes onto [stored] rather than over it, so a field this type does not model survives. */
internal fun LauncherSettings.mergedInto(stored: StoredLauncherSettings): StoredLauncherSettings =
    stored
        .toBuilder()
        .clearIconModeOverride()
        .setDrawerOpenDirection(drawerOpenDirection.toProto())
        .setHomeAlignment(homeAlignment.toProto())
        .setOpensKeyboardWithDrawer(opensKeyboardWithDrawer)
        .setThemeId(themeId)
        .setSetupStep(setupStep.toProto())
        .setHasHeldHomeRole(hasHeldHomeRole)
        .also { builder -> iconModeOverride?.let { builder.iconModeOverride = it.toProto() } }
        .build()

internal fun DrawerOpenDirection.toProto() =
    when (this) {
        DrawerOpenDirection.SwipeUp -> StoredDrawerOpenDirection.DRAWER_OPEN_DIRECTION_SWIPE_UP
        DrawerOpenDirection.SwipeDown -> StoredDrawerOpenDirection.DRAWER_OPEN_DIRECTION_SWIPE_DOWN
    }

internal fun StoredDrawerOpenDirection.toDrawerOpenDirection(): DrawerOpenDirection? =
    when (this) {
        StoredDrawerOpenDirection.DRAWER_OPEN_DIRECTION_SWIPE_UP -> DrawerOpenDirection.SwipeUp
        StoredDrawerOpenDirection.DRAWER_OPEN_DIRECTION_SWIPE_DOWN -> DrawerOpenDirection.SwipeDown
        StoredDrawerOpenDirection.DRAWER_OPEN_DIRECTION_UNSPECIFIED,
        StoredDrawerOpenDirection.UNRECOGNIZED,
        -> null
    }

internal fun SetupStep.toProto() =
    when (this) {
        SetupStep.Theme -> StoredSetupStep.SETUP_STEP_THEME
        SetupStep.HomeRole -> StoredSetupStep.SETUP_STEP_HOME_ROLE
        SetupStep.Favorites -> StoredSetupStep.SETUP_STEP_FAVORITES
        SetupStep.Done -> StoredSetupStep.SETUP_STEP_DONE
    }

internal fun StoredSetupStep.toSetupStep(): SetupStep? =
    when (this) {
        StoredSetupStep.SETUP_STEP_THEME -> SetupStep.Theme
        StoredSetupStep.SETUP_STEP_HOME_ROLE -> SetupStep.HomeRole
        StoredSetupStep.SETUP_STEP_FAVORITES -> SetupStep.Favorites
        StoredSetupStep.SETUP_STEP_DONE -> SetupStep.Done
        StoredSetupStep.SETUP_STEP_UNSPECIFIED,
        StoredSetupStep.UNRECOGNIZED,
        -> null
    }

internal fun HomeAlignment.toProto() =
    when (this) {
        HomeAlignment.Left -> StoredHomeAlignment.HOME_ALIGNMENT_LEFT
        HomeAlignment.Centred -> StoredHomeAlignment.HOME_ALIGNMENT_CENTRED
    }

internal fun StoredHomeAlignment.toHomeAlignment(): HomeAlignment? =
    when (this) {
        StoredHomeAlignment.HOME_ALIGNMENT_LEFT -> HomeAlignment.Left
        StoredHomeAlignment.HOME_ALIGNMENT_CENTRED -> HomeAlignment.Centred
        StoredHomeAlignment.HOME_ALIGNMENT_UNSPECIFIED,
        StoredHomeAlignment.UNRECOGNIZED,
        -> null
    }

internal fun IconMode.toProto() =
    when (this) {
        IconMode.Original -> StoredIconMode.ICON_MODE_ORIGINAL
        IconMode.Tinted -> StoredIconMode.ICON_MODE_TINTED
        IconMode.Off -> StoredIconMode.ICON_MODE_OFF
    }

internal fun StoredIconMode.toIconMode(): IconMode? =
    when (this) {
        StoredIconMode.ICON_MODE_ORIGINAL -> IconMode.Original
        StoredIconMode.ICON_MODE_TINTED -> IconMode.Tinted
        StoredIconMode.ICON_MODE_OFF -> IconMode.Off
        StoredIconMode.ICON_MODE_UNSPECIFIED,
        StoredIconMode.UNRECOGNIZED,
        -> null
    }
