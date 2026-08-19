package com.jonipharju.less.launcher

import com.google.protobuf.InvalidProtocolBufferException
import com.jonipharju.less.launcher.proto.ExportedConfiguration
import com.jonipharju.less.launcher.proto.LauncherUserData
import com.jonipharju.less.launcher.proto.StoredFavorite
import com.jonipharju.less.launcher.proto.StoredHiddenApp
import com.jonipharju.less.launcher.proto.LauncherSettings as StoredLauncherSettings

/**
 * Everything the user chose: their Favorites with order and custom labels, their Hidden Apps,
 * their settings and the Theme those settings name.
 *
 * It says nothing about the device it came from. Whether Setup has run and whether Less has
 * held the Home Role are the device's own answers, so a Configuration written on one phone
 * cannot send another phone back through Setup.
 */
data class LauncherConfiguration(
    val favorites: List<Favorite>,
    val hiddenApps: Set<LauncherAppId>,
    val settings: LauncherSettings,
)

/**
 * The version stamped on every exported file. A file carrying any other version is refused:
 * Less would rather read nothing than read a later format wrongly.
 */
internal val ConfigurationFormatVersion = 1

/**
 * What the user has made of this launcher, as it stands right now. Read after the store has
 * answered — every surface offering it sits behind [LauncherRepository.hasReadStoredSettings],
 * because before that the flows are standing in with defaults and would export an empty file.
 */
internal fun LauncherRepository.configuration(): LauncherConfiguration =
    LauncherConfiguration(
        favorites = favorites.value,
        hiddenApps = hiddenApps.value,
        settings = settings.value,
    )

/** The Configuration as the bytes of the exported file. */
internal fun LauncherConfiguration.encoded(): ByteArray =
    ExportedConfiguration
        .newBuilder()
        .setFormatVersion(ConfigurationFormatVersion)
        .setConfiguration(
            LauncherUserData
                .newBuilder()
                .addAllFavorites(favorites.map(Favorite::toProto))
                .addAllHiddenApps(hiddenApps.map(LauncherAppId::toStoredHiddenApp))
                .setSettings(settings.exportable())
                .build(),
        ).build()
        .toByteArray()

/**
 * The Favorites an import writes: in position order, whatever order the file listed them in.
 * Both the store and the fake put them down through here, so neither is the more forgiving.
 */
internal fun LauncherConfiguration.favoritesInOrder(): List<Favorite> = favorites.sortedBy(Favorite::position)

/**
 * The settings an import leaves behind: the ones the file names, over the two [current] answers
 * the device gives for itself — how far Setup got, and whether Less has held the Home Role.
 */
internal fun LauncherConfiguration.settingsRestoredOnto(current: LauncherSettings): LauncherSettings =
    settings.copy(setupStep = current.setupStep, hasHeldHomeRole = current.hasHeldHomeRole)

/**
 * The Configuration [bytes] hold, or null where they hold none — a file the user picked by
 * mistake, a truncated copy, a format from a later Less. Nothing is imported from a null.
 */
internal fun configurationFrom(bytes: ByteArray): LauncherConfiguration? {
    val exported =
        try {
            ExportedConfiguration.parseFrom(bytes)
        } catch (_: InvalidProtocolBufferException) {
            return null
        }

    if (exported.formatVersion != ConfigurationFormatVersion) return null

    val userData = exported.configuration
    return LauncherConfiguration(
        favorites = userData.favoritesList.map(StoredFavorite::toFavorite).sortedBy(Favorite::position),
        hiddenApps = userData.hiddenAppsList.map(StoredHiddenApp::toAppId).toSet(),
        settings = userData.settings.toLauncherSettings(),
    )
}

/**
 * The user's settings without the device's own state: an exported file records what the user
 * chose, not how far this phone got through Setup or whether it ever held the Home Role.
 */
private fun LauncherSettings.exportable(): StoredLauncherSettings =
    mergedInto(StoredLauncherSettings.getDefaultInstance())
        .toBuilder()
        .clearSetupStep()
        .clearHasHeldHomeRole()
        .build()
