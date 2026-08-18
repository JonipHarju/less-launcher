package com.jonipharju.less.launcher

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeLauncherRepository : LauncherRepository {
    private val mutableInstalledApps = MutableStateFlow<List<LauncherApp>>(emptyList())
    private val mutableLaunchedApps = mutableListOf<LauncherApp>()

    override val installedApps = mutableInstalledApps.asStateFlow()
    val launchedApps: List<LauncherApp> = mutableLaunchedApps

    fun install(app: LauncherApp) {
        mutableInstalledApps.value = (mutableInstalledApps.value + app).alphabetized()
    }

    fun uninstall(appId: LauncherAppId) {
        mutableInstalledApps.value = mutableInstalledApps.value.filterNot { it.id == appId }
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
