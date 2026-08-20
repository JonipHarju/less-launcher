package com.jonipharju.less.launcher

import com.jonipharju.less.launcher.proto.LauncherUserData
import com.jonipharju.less.launcher.proto.StoredFavorite
import com.jonipharju.less.launcher.proto.StoredHiddenApp

// Every edit the launcher makes to the Configuration, as one stored record in and one out.
//
// The store and the Fake both put their changes down through here, so a rule cannot hold on a
// phone and not in a test. What stays outside is what the record cannot answer: the platform
// calls the store makes, and the installed-app list, which is never stored at all.

/** The Favorites as they are stored: in position order, whatever order the record lists them in. */
internal fun LauncherUserData.storedFavorites(): List<Favorite> = favoritesList.map(StoredFavorite::toFavorite).sortedBy(Favorite::position)

internal fun LauncherUserData.storedHiddenApps(): Set<LauncherAppId> = hiddenAppsList.map(StoredHiddenApp::toAppId).toSet()

internal fun LauncherUserData.storedSettings(): LauncherSettings = settings.toLauncherSettings()

/**
 * Records that Less holds the Home Role. Holding it once is recorded for good, so that the
 * Drawer's standing prompt does not come back the day the user hands the role to another
 * launcher: that is a choice, and a launcher that keeps asking is a launcher that nags.
 */
internal fun LauncherUserData.homeRoleHeld(): LauncherUserData =
    if (storedSettings().hasHeldHomeRole) this else settingsUpdated { it.copy(hasHeldHomeRole = true) }

/** Puts [favorite] on Home. An app already there is replaced rather than pinned twice. */
internal fun LauncherUserData.choosing(favorite: Favorite): LauncherUserData =
    withFavorites(
        favoritesList.filterNot { it.hasSameAppIdAs(favorite.appId) } + favorite.toProto(),
    )

/** Takes [appId] off Home. Dismissing a Favorite that is not there changes nothing. */
internal fun LauncherUserData.dismissing(appId: LauncherAppId): LauncherUserData =
    withFavorites(favoritesList.filterNot { it.hasSameAppIdAs(appId) })

/**
 * An uninstalled app leaves nothing behind: no Favorite, because that removal was intentional,
 * and no record that it was hidden, because there is no longer anything to hide.
 *
 * A package is uninstalled whole, so this names the package and the profile rather than one
 * activity of it. The same package in another profile is a different app, and it stays.
 */
internal fun LauncherUserData.forgetting(
    packageName: String,
    profileSerialNumber: Long,
): LauncherUserData =
    withFavorites(
        favoritesList.filterNot {
            it.packageName == packageName && it.profileSerialNumber == profileSerialNumber
        },
    ).withHiddenApps(
        hiddenAppsList.filterNot {
            it.packageName == packageName && it.profileSerialNumber == profileSerialNumber
        },
    )

/** Takes [appId] out of the Drawer. Hiding it again changes nothing. */
internal fun LauncherUserData.hiding(appId: LauncherAppId): LauncherUserData =
    if (hiddenAppsList.any { it.toAppId() == appId }) {
        this
    } else {
        toBuilder().addHiddenApps(appId.toStoredHiddenApp()).build()
    }

/** Puts [appId] back in the Drawer. Unhiding an app that was never hidden changes nothing. */
internal fun LauncherUserData.unhiding(appId: LauncherAppId): LauncherUserData =
    withHiddenApps(hiddenAppsList.filterNot { it.toAppId() == appId })

/**
 * Rewrites every Favorite's position to the order [order] names, so that a drag cannot leave
 * Home half-reordered. Custom labels survive untouched.
 */
internal fun LauncherUserData.reordered(order: List<LauncherAppId>): LauncherUserData =
    withFavorites(storedFavorites().orderedBy(order).map(Favorite::toProto))

/**
 * Applies [update] to the settings this record holds, so that two settings changed in quick
 * succession cannot each overwrite the other with a stale record.
 */
internal fun LauncherUserData.settingsUpdated(update: (LauncherSettings) -> LauncherSettings): LauncherUserData =
    toBuilder().setSettings(update(storedSettings()).mergedInto(settings)).build()

/**
 * Puts [configuration] in place of everything stored, so that a half-read file cannot leave the
 * launcher part one setup and part another. What the device answers for itself — how far Setup
 * got, whether Less has held the Home Role — is left as it stands.
 */
internal fun LauncherUserData.restoring(configuration: LauncherConfiguration): LauncherUserData =
    withFavorites(configuration.favoritesInOrder().map(Favorite::toProto))
        .withHiddenApps(configuration.hiddenApps.map(LauncherAppId::toStoredHiddenApp))
        .let { restored ->
            restored
                .toBuilder()
                .setSettings(
                    configuration
                        .settingsRestoredOnto(storedSettings())
                        .mergedInto(restored.settings),
                ).build()
        }

/** Favorites are stored in position order, so that the record reads the way Home does. */
private fun LauncherUserData.withFavorites(favorites: List<StoredFavorite>): LauncherUserData =
    toBuilder()
        .clearFavorites()
        .addAllFavorites(favorites.sortedBy(StoredFavorite::getPosition))
        .build()

private fun LauncherUserData.withHiddenApps(hiddenApps: List<StoredHiddenApp>): LauncherUserData =
    toBuilder()
        .clearHiddenApps()
        .addAllHiddenApps(hiddenApps)
        .build()
