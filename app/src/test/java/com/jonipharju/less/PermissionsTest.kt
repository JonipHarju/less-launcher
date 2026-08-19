package com.jonipharju.less

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * What Less asks of the user, checked against the manifest rather than trusted to review. A
 * launcher that watches which apps you open, or that can lock your device, is a different
 * product; the answer stays no, so it is written down as a test.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class PermissionsTest {
    /**
     * The platform permissions only. The build tools add an app-private one of their own for
     * unexported receivers, which asks the user for nothing and is not Less's to declare.
     */
    private val requested: List<String>
        get() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            return context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions
                .orEmpty()
                .filter { it.startsWith("android.permission.") }
        }

    @Test
    fun `Less asks for the wallpaper and nothing besides`() {
        assertEquals(listOf(Manifest.permission.SET_WALLPAPER), requested)
    }

    @Test
    fun `no usage statistics, now or ever`() {
        assertEquals(emptyList<String>(), requested.filter { "USAGE" in it })
    }

    @Test
    fun `no device administration, now or ever`() {
        assertEquals(emptyList<String>(), requested.filter { "DEVICE_ADMIN" in it || "BIND_DEVICE" in it })
    }
}
