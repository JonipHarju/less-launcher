package com.jonipharju.less

import android.content.Intent
import android.provider.AlarmClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Opening the clock and the calendar tries a specific action first and falls back toward
 * whichever app the device treats as that thing, so a phone that has a clock but no alarms
 * filter still opens something.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ClockAndCalendarTest {
    @Test
    fun `the alarm action is tried before launching the clock app`() {
        val tried = mutableListOf<String>()
        openFirst(clockOpenIntents()) { intent ->
            tried += intent.action.orEmpty()
            false
        }
        assertEquals(listOf(AlarmClock.ACTION_SHOW_ALARMS, Intent.ACTION_MAIN), tried)
        assertTrue(clockOpenIntents().last().hasCategory(CATEGORY_APP_CLOCK))
    }

    @Test
    fun `a device without the alarm action still opens the clock app`() {
        assertTrue(openFirst(clockOpenIntents()) { it.hasCategory(CATEGORY_APP_CLOCK) })
    }

    @Test
    fun `the alarm action wins when it resolves`() {
        val tried = mutableListOf<String>()
        assertTrue(
            openFirst(clockOpenIntents()) { intent ->
                tried += intent.action.orEmpty()
                intent.action == AlarmClock.ACTION_SHOW_ALARMS
            },
        )
        assertEquals(listOf(AlarmClock.ACTION_SHOW_ALARMS), tried)
    }

    @Test
    fun `the calendar URI is tried before launching the calendar app`() {
        val tried = mutableListOf<String>()
        openFirst(calendarOpenIntents(now = 1_000L)) { intent ->
            tried += intent.action.orEmpty()
            false
        }
        assertEquals(listOf(Intent.ACTION_VIEW, Intent.ACTION_MAIN), tried)
        assertEquals("content://com.android.calendar/time/1000", calendarOpenIntents(1_000L).first().data.toString())
        assertTrue(calendarOpenIntents(1_000L).last().hasCategory(CATEGORY_APP_CALENDAR))
    }

    @Test
    fun `a device without the calendar URI still opens the calendar app`() {
        assertTrue(openFirst(calendarOpenIntents(1L)) { it.hasCategory(CATEGORY_APP_CALENDAR) })
    }

    @Test
    fun `when nothing resolves the sequence yields false`() {
        assertFalse(openFirst(clockOpenIntents()) { false })
        assertFalse(openFirst(calendarOpenIntents(1L)) { false })
    }
}
