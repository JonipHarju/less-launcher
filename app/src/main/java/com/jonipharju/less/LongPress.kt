package com.jonipharju.less

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs
import kotlin.math.max

/**
 * A tap, a long press, and the vertical drag a long press turns into, read by one detector so
 * that launching, curating, and moving a Favorite never race each other for the same touch.
 *
 * A press that ends without travelling is the long press; one that travels is the drag. The
 * menu therefore opens on release rather than under the finger, which is what lets the same
 * gesture continue into a drag.
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
            var pressWasLong = false
            val up =
                try {
                    withTimeout(viewConfiguration.longPressTimeoutMillis) { waitForUpOrCancellation() }
                } catch (_: PointerEventTimeoutCancellationException) {
                    pressWasLong = true
                    null
                }

            if (!pressWasLong) {
                // Released within the timeout is a tap; anything else was cancelled out from under us.
                up?.let { release ->
                    release.consume()
                    onTap()
                }
                return@awaitEachGesture
            }

            var travelled = 0f
            var farthest = 0f
            drag(down.id) { change ->
                travelled += change.positionChange().y
                farthest = max(farthest, abs(travelled))
                onDrag(change.positionChange().y)
                change.consume()
            }

            if (farthest > viewConfiguration.touchSlop) onDragEnd() else onLongPress()
        }
    }
