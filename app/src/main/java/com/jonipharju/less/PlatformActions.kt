package com.jonipharju.less

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock

/**
 * Ways to open a clock, by the platform's AlarmClock contract: the list of alarms first, then the
 * alarm-setting screen every clock app has answered since long before that list existed. Android
 * has no clock category to fall back on the way it has a calendar one.
 */
internal fun clockOpenIntents(): List<Intent> =
    listOf(
        Intent(AlarmClock.ACTION_SHOW_ALARMS),
        Intent(AlarmClock.ACTION_SET_ALARM),
    )

/** Ways to open a calendar, from its time URI toward the device's calendar app. */
internal fun calendarOpenIntents(now: Long): List<Intent> =
    listOf(
        Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.calendar/time/$now")),
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR),
    )

/**
 * Starts the first candidate the device answers, and reports whether anything opened.
 *
 * [handlerOf] names the package Android resolves for an intent, or null for none. A handler can
 * disappear between resolving and starting, and a clock app commonly guards its alarm screens
 * with a permission of its own that Less does not hold; either way the next candidate is tried.
 * When every candidate is refused, the app that answered them still has a front door of its own
 * — [frontDoorOf], its launcher entry — which no permission guards, and that opens instead.
 */
internal fun openFirstResolved(
    candidates: List<Intent>,
    handlerOf: (Intent) -> String?,
    frontDoorOf: (String) -> Intent?,
    start: (Intent) -> Unit,
): Boolean {
    val answering = LinkedHashSet<String>()
    candidates.forEach { candidate ->
        val handler = handlerOf(candidate) ?: return@forEach
        answering += handler
        if (started(candidate, start)) return true
    }
    answering.forEach { packageName ->
        val frontDoor = frontDoorOf(packageName) ?: return@forEach
        if (started(frontDoor, start)) return true
    }
    return false
}

private fun started(
    intent: Intent,
    start: (Intent) -> Unit,
): Boolean =
    try {
        start(intent)
        true
    } catch (_: ActivityNotFoundException) {
        // The package went between resolving and starting.
        false
    } catch (_: SecurityException) {
        // The activity is guarded by a permission Less does not hold.
        false
    }
