package com.jonipharju.less

import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock

/** The app the device treats as its clock. Not a LAUNCHER category, so it needs its own query. */
internal const val CATEGORY_APP_CLOCK = "android.intent.category.APP_CLOCK"

/** The app the device treats as its calendar. Same visibility reason as [CATEGORY_APP_CLOCK]. */
internal const val CATEGORY_APP_CALENDAR = "android.intent.category.APP_CALENDAR"

/**
 * Ways to open a clock, most specific first: the alarms action, then whichever app the
 * device treats as its clock. A phone can have a perfectly good clock that does not
 * declare the alarms filter.
 */
internal fun clockOpenIntents(): List<Intent> =
    listOf(
        Intent(AlarmClock.ACTION_SHOW_ALARMS),
        Intent(Intent.ACTION_MAIN).addCategory(CATEGORY_APP_CLOCK),
    )

/**
 * Ways to open a calendar, most specific first: the calendar's content URI at [now], then
 * whichever app the device treats as its calendar.
 */
internal fun calendarOpenIntents(now: Long): List<Intent> =
    listOf(
        Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.calendar/time/$now")),
        Intent(Intent.ACTION_MAIN).addCategory(CATEGORY_APP_CALENDAR),
    )

/** Starts the first candidate that [start] accepts; false when every attempt fails. */
internal fun openFirst(
    candidates: List<Intent>,
    start: (Intent) -> Boolean,
): Boolean = candidates.any(start)
