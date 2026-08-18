package com.jonipharju.less.launcher

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [LauncherRepository] backed by Android's profile-aware launcher APIs. */
class AndroidLauncherRepository(
    context: Context,
) : LauncherRepository,
    AutoCloseable {
    private val context = context.applicationContext
    private val launcherApps = this.context.getSystemService(LauncherApps::class.java)
    private val userManager = this.context.getSystemService(UserManager::class.java)
    private val mutableInstalledApps = MutableStateFlow<List<LauncherApp>>(emptyList())
    private var isClosed = false

    override val installedApps = mutableInstalledApps.asStateFlow()

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
            ) = refresh()

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

    override fun close() {
        if (isClosed) return

        launcherApps.unregisterCallback(launcherCallback)
        context.unregisterReceiver(profileAvailabilityReceiver)
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
                )
            }
        } catch (_: SecurityException) {
            emptyList()
        }
    }
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
