package com.jonipharju.less

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.AlarmClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The device decides which clock or calendar answers; Less asks in order and opens the first
 * that does. The apps here are registered with the platform the way installed ones are and
 * resolved through it, so what these tests exercise is the route a real tap takes.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class PlatformActionsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val packageManager get() = context.packageManager
    private val started = mutableListOf<Intent>()

    @Test
    fun `the clock opens through its list of alarms`() {
        install("com.example.clock", answering(AlarmClock.ACTION_SHOW_ALARMS), answering(AlarmClock.ACTION_SET_ALARM))

        assertTrue(open(clockOpenIntents()))

        assertEquals(listOf(AlarmClock.ACTION_SHOW_ALARMS), started.map(Intent::getAction))
    }

    @Test
    fun `a clock that only sets alarms still opens`() {
        install("com.example.clock", answering(AlarmClock.ACTION_SET_ALARM))

        assertTrue(open(clockOpenIntents()))

        assertEquals(listOf(AlarmClock.ACTION_SET_ALARM), started.map(Intent::getAction))
    }

    /** The AOSP clock guards its alarm screens with a permission of its own; Less holds none but the Wallpaper's. */
    @Test
    fun `a clock that guards its alarm screens opens through its own front door`() {
        install("com.example.clock", answering(AlarmClock.ACTION_SHOW_ALARMS), answering(AlarmClock.ACTION_SET_ALARM))

        assertTrue(open(clockOpenIntents(), start = ::startUnlessGuarded))

        val frontDoor = started.single()
        assertEquals(Intent.ACTION_MAIN, frontDoor.action)
        assertTrue(frontDoor.hasCategory(Intent.CATEGORY_LAUNCHER))
        assertEquals("com.example.clock", frontDoor.component?.packageName)
    }

    @Test
    fun `a guarded clock with no front door reports failure`() {
        install("com.example.clock", answering(AlarmClock.ACTION_SHOW_ALARMS), frontDoor = false)

        assertFalse(open(clockOpenIntents(), start = ::startUnlessGuarded))

        assertEquals(emptyList<Intent>(), started)
    }

    @Test
    fun `a clock that disappears between resolving and starting yields to the next`() {
        install("com.example.clock", answering(AlarmClock.ACTION_SHOW_ALARMS))
        install("com.example.otherclock", answering(AlarmClock.ACTION_SET_ALARM))

        assertTrue(
            open(clockOpenIntents()) { intent ->
                if (intent.action == AlarmClock.ACTION_SHOW_ALARMS) throw ActivityNotFoundException()
                started += intent
            },
        )

        assertEquals(listOf(AlarmClock.ACTION_SET_ALARM), started.map(Intent::getAction))
    }

    @Test
    fun `with no clock app nothing opens and failure is reported`() {
        assertFalse(open(clockOpenIntents()))

        assertEquals(emptyList<Intent>(), started)
    }

    @Test
    fun `the calendar opens at today through its time URI`() {
        install("com.example.calendar", answeringCalendarTime(), answeringCalendarCategory())

        assertTrue(open(calendarOpenIntents(now = 1_000L)))

        assertEquals("content://com.android.calendar/time/1000", started.single().data.toString())
    }

    @Test
    fun `a calendar without the time URI opens through its app category`() {
        install("com.example.calendar", answeringCalendarCategory())

        assertTrue(open(calendarOpenIntents(now = 1_000L)))

        assertTrue(started.single().hasCategory(Intent.CATEGORY_APP_CALENDAR))
    }

    @Test
    fun `with no calendar app nothing opens and failure is reported`() {
        assertFalse(open(calendarOpenIntents(now = 1_000L)))

        assertEquals(emptyList<Intent>(), started)
    }

    /** Opens through the platform the way MainActivity does, with [start] standing in for starting an activity. */
    private fun open(
        candidates: List<Intent>,
        start: (Intent) -> Unit = started::add,
    ): Boolean =
        openFirstResolved(
            candidates = candidates,
            handlerOf = { intent -> intent.resolveActivity(packageManager)?.packageName },
            frontDoorOf = packageManager::getLaunchIntentForPackage,
            start = start,
        )

    /** What Android does when an activity is guarded by a permission the caller does not hold. */
    private fun startUnlessGuarded(intent: Intent) {
        if (intent.action != Intent.ACTION_MAIN) {
            throw SecurityException("Permission Denial: starting $intent requires com.android.alarm.permission.SET_ALARM")
        }
        started += intent
    }

    /** An app on the device answering each of [answers], with an icon in the launcher unless [frontDoor] is off. */
    private fun install(
        packageName: String,
        vararg answers: IntentFilter,
        frontDoor: Boolean = true,
    ) {
        val shadow = shadowOf(packageManager)
        if (frontDoor) {
            val main = ComponentName(packageName, "$packageName.Main")
            shadow.addActivityIfNotPresent(main)
            shadow.addIntentFilterForActivity(
                main,
                IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
            )
        }
        answers.forEachIndexed { index, filter ->
            val handler = ComponentName(packageName, "$packageName.Handler$index")
            shadow.addActivityIfNotPresent(handler)
            shadow.addIntentFilterForActivity(handler, filter)
        }
    }

    private fun answering(action: String) = IntentFilter(action).apply { addCategory(Intent.CATEGORY_DEFAULT) }

    private fun answeringCalendarTime() =
        IntentFilter(Intent.ACTION_VIEW).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addDataScheme("content")
            addDataAuthority("com.android.calendar", null)
        }

    private fun answeringCalendarCategory() =
        IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addCategory(Intent.CATEGORY_APP_CALENDAR)
        }
}
