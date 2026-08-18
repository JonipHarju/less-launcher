package com.jonipharju.less

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.jonipharju.less.launcher.VerticalSwipe
import kotlin.math.abs

/** How far a drag has to travel before it counts as a swipe rather than a stray touch. */
private val SwipeThreshold = 64.dp

/** Reports every completed vertical drag, whether or not it went far enough to be a swipe. */
internal fun Modifier.onVerticalSwipe(onSwipe: (VerticalSwipe) -> Unit): Modifier =
    pointerInput(onSwipe) {
        val threshold = SwipeThreshold.toPx()
        var dragDistance = 0f
        detectVerticalDragGestures(
            onDragStart = { dragDistance = 0f },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                dragDistance += dragAmount
            },
            onDragEnd = { onSwipe(VerticalSwipe(dragDistance, threshold)) },
        )
    }

/**
 * Reports drags a scrollable child could not consume because it had already reached its
 * edge, so that a swipe over a list still reads as a swipe rather than as a dead scroll.
 */
@Composable
internal fun rememberOverscrollSwipe(onSwipe: (VerticalSwipe) -> Unit): NestedScrollConnection {
    val threshold = with(LocalDensity.current) { SwipeThreshold.toPx() }

    return remember(onSwipe, threshold) {
        object : NestedScrollConnection {
            private var dragDistance = 0f

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero

                // Any scroll the list still had room for means this drag is not a swipe.
                if (consumed.y != 0f) dragDistance = 0f
                dragDistance += available.y
                if (abs(dragDistance) >= threshold) {
                    onSwipe(VerticalSwipe(dragDistance, threshold))
                    dragDistance = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity {
                dragDistance = 0f
                return Velocity.Zero
            }
        }
    }
}
