package com.jonipharju.less.launcher

import android.app.backup.BackupAgentHelper
import kotlinx.coroutines.runBlocking

/**
 * What Less does with a Configuration the platform has just restored onto this phone.
 *
 * Automatic backup carries the whole stored file, and the file holds one answer that belongs to
 * the phone rather than to the user: whether Less has held the Home Role. The phone being
 * restored onto has given Less nothing, so that answer is cleared and the Drawer's standing
 * prompt comes back — otherwise Less would arrive believing it had once been the default
 * launcher, never ask to be made one again, and leave the user no way in.
 *
 * The agent registers no helpers of its own: [android.app.backup.BackupAgentHelper] is here for
 * the restore callback, and the manifest's `fullBackupOnly` keeps the transfer itself the
 * ordinary automatic backup of the files the extraction rules name.
 */
class ConfigurationRestoreAgent : BackupAgentHelper() {
    override fun onRestoreFinished() {
        super.onRestoreFinished()

        // The agent runs before any surface does, and there is nothing to keep it responsive for.
        runBlocking {
            applicationContext.launcherUserDataStore.updateData { userData ->
                userData
                    .toBuilder()
                    .setSettings(userData.settings.toBuilder().clearHasHeldHomeRole())
                    .build()
            }
        }
    }
}
