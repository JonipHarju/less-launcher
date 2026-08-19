package com.jonipharju.less

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * How a user can reach Less, checked against the manifest rather than trusted to review.
 * A launcher that can only be found on the default-apps screen cannot be found at all by
 * someone who has never opened it — see ADR-0008.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class EntryPointsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun opensFrom(category: String): Boolean {
        val intent =
            Intent(Intent.ACTION_MAIN)
                .addCategory(category)
                .setPackage(context.packageName)
        return context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()
    }

    @Test
    fun `Less offers itself as a home app`() {
        assertTrue(opensFrom(Intent.CATEGORY_HOME))
    }

    @Test
    fun `Less has an icon in whichever launcher is currently the home app`() {
        assertTrue(opensFrom(Intent.CATEGORY_LAUNCHER))
    }
}
