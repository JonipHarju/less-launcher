package com.jonipharju.less

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.AlarmClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The device decides which clock or calendar can answer; Less tries the available route first. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class PlatformActionsTest {
    @Test
    fun `clock falls back to its app category when alarms do not resolve`() {
        val resolved = mutableListOf<Intent>()
        val started = mutableListOf<Intent>()
        val candidates = clockOpenIntents()

        assertTrue(
            openFirstResolved(
                candidates = candidates,
                resolves = { intent ->
                    resolved += intent
                    intent.hasCategory(CATEGORY_APP_CLOCK)
                },
                start = started::add,
            ),
        )

        assertEquals(
            listOf(AlarmClock.ACTION_SHOW_ALARMS, Intent.ACTION_MAIN),
            resolved.map(Intent::getAction),
        )
        assertEquals(listOf(candidates.last()), started)
    }

    @Test
    fun `calendar falls back to its app category when its time URI does not resolve`() {
        val resolved = mutableListOf<Intent>()
        val started = mutableListOf<Intent>()
        val candidates = calendarOpenIntents(now = 1_000L)

        assertTrue(
            openFirstResolved(
                candidates = candidates,
                resolves = { intent ->
                    resolved += intent
                    intent.hasCategory(CATEGORY_APP_CALENDAR)
                },
                start = started::add,
            ),
        )

        assertEquals(
            listOf(Intent.ACTION_VIEW, Intent.ACTION_MAIN),
            resolved.map(Intent::getAction),
        )
        assertEquals("content://com.android.calendar/time/1000", resolved.first().data.toString())
        assertEquals(listOf(candidates.last()), started)
    }

    @Test
    fun `no resolved platform action reports failure without starting anything`() {
        var starts = 0

        assertFalse(
            openFirstResolved(
                candidates = clockOpenIntents(),
                resolves = { false },
                start = { starts++ },
            ),
        )

        assertEquals(0, starts)
    }

    @Test
    fun `a handler that disappears while opening falls back without escaping`() {
        val candidates = listOf(Intent("first"), Intent("second"))
        val started = mutableListOf<Intent>()

        assertTrue(
            openFirstResolved(
                candidates = candidates,
                resolves = { true },
                start = { intent ->
                    if (intent.action == "first") throw ActivityNotFoundException()
                    started += intent
                },
            ),
        )

        assertEquals(listOf(candidates.last()), started)
    }
}
