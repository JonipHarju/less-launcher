package com.jonipharju.less

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The detector that reads a tap, a long press and a drag off one pointer, including the
 * cases a zero-travel click never reaches: a thumb that drifted, a pointer another
 * handler consumed, a press that left the row without travelling past slop.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class TapLongPressOrDragTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `a tap that drifts less than touch slop still fires onTap`() {
        val probe = GestureProbe()
        compose.setContent { ScrollingRow(probe) }

        compose.onNodeWithTag("row").performTouchInput {
            down(center)
            moveBy(Offset(0f, viewConfiguration.touchSlop / 2f))
            up()
        }

        compose.runOnIdle { probe.assertOnly(taps = 1) }
    }

    @Test
    fun `leaving the row within slop is still a tap`() {
        val probe = GestureProbe()
        compose.setContent { ScrollingRow(probe) }

        compose.onNodeWithTag("row").performTouchInput {
            down(Offset(center.x, 1f))
            moveBy(Offset(0f, -viewConfiguration.touchSlop / 2f))
            up()
        }

        compose.runOnIdle { probe.assertOnly(taps = 1) }
    }

    @Test
    fun `a press past the long-press timeout released in place fires onLongPress`() {
        val probe = GestureProbe()
        compose.setContent { ScrollingRow(probe) }

        compose.onNodeWithTag("row").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            up()
        }

        compose.runOnIdle { probe.assertOnly(longPresses = 1) }
    }

    @Test
    fun `a press past the timeout then dragged vertically fires onDrag and onDragEnd`() {
        val probe = GestureProbe()
        compose.setContent { ScrollingRow(probe) }

        compose.onNodeWithTag("row").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveBy(Offset(0f, viewConfiguration.touchSlop * 3f))
            up()
        }

        compose.runOnIdle {
            assertEquals(0, probe.taps)
            assertEquals(0, probe.longPresses)
            assertTrue(probe.drags > 0)
            assertEquals(1, probe.dragEnds)
        }
    }

    @Test
    fun `a vertical drag before the timeout yields and fires nothing`() {
        val probe = GestureProbe()
        compose.setContent { ScrollingRow(probe) }

        compose.onNodeWithTag("row").performTouchInput {
            down(center)
            moveBy(Offset(0f, viewConfiguration.touchSlop * 3f))
            up()
        }

        compose.runOnIdle { probe.assertOnly() }
    }

    /**
     * Every combination of timeout and travel resolves to one named outcome. There is no
     * leftover path that means "do nothing" — that was the bug: a cancelled pointer and a
     * released one were the same null.
     */
    @Test
    fun `every timeout and travel combination resolves to an outcome`() {
        val slop = 8f
        val cases =
            listOf(
                Triple(false, 0f, PressOutcome.Tap),
                Triple(false, 4f, PressOutcome.Tap),
                Triple(false, slop, PressOutcome.Tap),
                Triple(false, slop + 1f, PressOutcome.Scroll),
                Triple(true, 0f, PressOutcome.LongPress),
                Triple(true, 4f, PressOutcome.LongPress),
                Triple(true, slop, PressOutcome.LongPress),
                Triple(true, slop + 1f, PressOutcome.Drag),
            )
        for ((timedOut, travel, expected) in cases) {
            assertEquals(expected, pressOutcome(timedOut, travel, slop))
        }
    }
}

/** Counts the four callbacks so a test can say which one fired, and that the others did not. */
private class GestureProbe {
    var taps = 0
    var longPresses = 0
    var drags = 0
    var dragEnds = 0

    fun assertOnly(
        taps: Int = 0,
        longPresses: Int = 0,
        drags: Int = 0,
        dragEnds: Int = 0,
    ) {
        assertEquals("taps", taps, this.taps)
        assertEquals("longPresses", longPresses, this.longPresses)
        assertEquals("drags", drags, this.drags)
        assertEquals("dragEnds", dragEnds, this.dragEnds)
    }
}

/** The detector inside a vertically scrolling column, which is how Home's Favorites sit. */
@Composable
private fun ScrollingRow(probe: GestureProbe) {
    Column(Modifier.verticalScroll(rememberScrollState()).height(200.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(FavoriteRowHeight)
                .testTag("row")
                .onTapLongPressOrDrag(
                    key = "row",
                    onTap = { probe.taps++ },
                    onLongPress = { probe.longPresses++ },
                    onDrag = { probe.drags++ },
                    onDragEnd = { probe.dragEnds++ },
                ),
        )
        Box(Modifier.height(400.dp))
    }
}
