package com.jonipharju.less

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock

/** The app the device treats as its clock. It needs its own package-visibility query. */
internal const val CATEGORY_APP_CLOCK = "android.intent.category.APP_CLOCK"

/** The app the device treats as its calendar. It needs its own package-visibility query. */
internal const val CATEGORY_APP_CALENDAR = "android.intent.category.APP_CALENDAR"

/** Ways to open a clock, from its alarms action toward the device's clock app. */
internal fun clockOpenIntents(): List<Intent> =
    listOf(
        Intent(AlarmClock.ACTION_SHOW_ALARMS),
        Intent(Intent.ACTION_MAIN).addCategory(CATEGORY_APP_CLOCK),
    )

/** Ways to open a calendar, from its time URI toward the device's calendar app. */
internal fun calendarOpenIntents(now: Long): List<Intent> =
    listOf(
        Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.calendar/time/$now")),
        Intent(Intent.ACTION_MAIN).addCategory(CATEGORY_APP_CALENDAR),
    )

/** Starts the first candidate Android resolves, then tries a fallback if that handler disappears. */
internal fun openFirstResolved(
    candidates: List<Intent>,
    resolves: (Intent) -> Boolean,
    start: (Intent) -> Unit,
): Boolean {
    candidates.forEach { candidate ->
        if (!resolves(candidate)) return@forEach
        try {
            start(candidate)
            return true
        } catch (_: ActivityNotFoundException) {
            // A package can disappear after resolution; the next candidate may still work.
        }
    }
    return false
}
