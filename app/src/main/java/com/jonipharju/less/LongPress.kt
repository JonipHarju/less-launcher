package com.jonipharju.less

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs
import kotlin.math.max

/**
 * What a press became once we know whether the long-press timeout elapsed and how far the
 * pointer travelled. Scroll is the yield: the row did not own that pointer, and none of the
 * four callbacks fire.
 *
 * Travel against [touchSlop] is what separates a drifting tap from a scroll, so a press that
 * was consumed or left the row without travelling past slop is still a tap. A released pointer
 * and a consumed one used to be the same null, which is how Home dropped taps.
 */
internal enum class PressOutcome {
    Tap,
    LongPress,
    Drag,
    Scroll,
}

internal fun pressOutcome(
    timedOut: Boolean,
    travel: Float,
    touchSlop: Float,
): PressOutcome {
    val pastSlop = travel > touchSlop
    return when {
        !timedOut && !pastSlop -> PressOutcome.Tap
        !timedOut && pastSlop -> PressOutcome.Scroll
        timedOut && !pastSlop -> PressOutcome.LongPress
        else -> PressOutcome.Drag
    }
}

/**
 * A tap, a long press, and the vertical drag a long press turns into, read by one detector so
 * that launching, curating, and moving a Favorite never race each other for the same touch.
 *
 * A press that ends without travelling is the long press; one that travels is the drag. The
 * menu therefore opens on release rather than under the finger, which is what lets the same
 * gesture continue into a drag. Exactly one of [onLongPress] and [onDrag] hears from a press:
 * nothing is reported as drag until the finger has travelled past slop, and once it has, the
 * press is a drag for good and ends in [onDragEnd] rather than in the menu.
 */
internal fun Modifier.onTapLongPressOrDrag(
    key: Any?,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
): Modifier =
    pointerInput(key) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val slop = viewConfiguration.touchSlop
            val origin = down.position
            var travel = 0f
            var timedOut = false

            try {
                withTimeout(viewConfiguration.longPressTimeoutMillis) {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: return@withTimeout
                        travel = max(travel, (change.position - origin).getDistance())
                        if (travel > slop) {
                            // Home scrolling and the Drawer Open Direction own travel past slop.
                            return@withTimeout
                        }
                        if (change.changedToUpIgnoreConsumed()) {
                            change.consume()
                            return@withTimeout
                        }
                    }
                }
            } catch (_: PointerEventTimeoutCancellationException) {
                timedOut = true
            }

            when (pressOutcome(timedOut, travel, slop)) {
                PressOutcome.Tap -> {
                    onTap()
                    return@awaitEachGesture
                }
                PressOutcome.Scroll -> return@awaitEachGesture
                PressOutcome.LongPress,
                PressOutcome.Drag,
                -> {
                    // Take the pointer so a move after the timeout is our drag, not Home scrolling.
                    currentEvent.changes.forEach { change ->
                        if (change.id == down.id) change.consume()
                    }
                }
            }

            var travelled = 0f
            var reported = 0f
            var dragging = false
            drag(down.id) { change ->
                travelled += change.positionChange().y
                change.consume()
                // A finger that is merely holding still wobbles; the row hears nothing of that.
                // The first report carries the whole travel, so none of it is lost to the wait.
                if (!dragging) {
                    dragging = pressOutcome(timedOut = true, travel = abs(travelled), touchSlop = slop) == PressOutcome.Drag
                }
                if (dragging) {
                    onDrag(travelled - reported)
                    reported = travelled
                }
            }

            if (dragging) onDragEnd() else onLongPress()
        }
    }
