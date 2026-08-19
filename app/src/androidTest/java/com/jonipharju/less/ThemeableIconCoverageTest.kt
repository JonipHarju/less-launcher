package com.jonipharju.less

import android.content.pm.LauncherApps
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.UserManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Repeatable real-device measurement requested by issue #11. */
@RunWith(AndroidJUnit4::class)
class ThemeableIconCoverageTest {
    @Test
    fun measureInstalledAppsSupplyingAThemeableIconLayer() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val userManager = context.getSystemService(UserManager::class.java)
        val icons =
            userManager.userProfiles.flatMap { profile ->
                launcherApps.getActivityList(null, profile).map { activity ->
                    activity.getIcon(context.resources.displayMetrics.densityDpi)
                }
            }
        val themeable = icons.count { icon -> (icon as? AdaptiveIconDrawable)?.monochrome != null }

        assertTrue("The measurement needs at least one installed launchable app", icons.isNotEmpty())
        println("THEMEABLE_ICON_COVERAGE=$themeable/${icons.size} (${themeable * 100 / icons.size}%)")
    }
}
