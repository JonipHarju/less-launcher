package com.jonipharju.less

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * One press on a Favorite resolves to exactly one outcome. The row here is bare — the detector
 * and nothing else — so that what is asserted is which callbacks a finger reaches, and in what
 * order, with no Home around it to absorb the difference.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class FavoriteGestureTest {
    @get:Rule
    val compose = createComposeRule()

    private val heard = mutableListOf<String>()
    private var dragged = 0f

    @Test
    fun `a tap that drifts under slop is a tap`() {
        aRow()

        press {
            down(center)
            moveBy(Offset(0f, viewConfiguration.touchSlop / 2f))
            up()
        }

        assertEquals(listOf("tap"), heard)
    }

    @Test
    fun `travel past slop before the timeout is a scroll the row leaves alone`() {
        aRow()

        press {
            down(center)
            moveBy(Offset(0f, viewConfiguration.touchSlop * 3f))
            up()
        }

        assertEquals(emptyList<String>(), heard)
    }

    /** A finger that is merely holding still wobbles; the menu opens and no drag is reported. */
    @Test
    fun `a held finger that wobbles opens the menu and never drags`() {
        aRow()

        press {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveBy(Offset(0f, 1f))
            moveBy(Offset(0f, -2f))
            moveBy(Offset(1f, 1f))
            up()
        }

        assertEquals(listOf("long press"), heard)
    }

    @Test
    fun `a held finger that travels past slop drags and never opens the menu`() {
        aRow()

        press {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveBy(Offset(0f, viewConfiguration.touchSlop * 3f))
            moveBy(Offset(0f, viewConfiguration.touchSlop))
            up()
        }

        assertEquals(listOf("drag", "drag", "drag end"), heard)
    }

    /** The wait for slop costs the row nothing: the first report carries everything travelled so far. */
    @Test
    fun `a drag reports every pixel travelled including those before slop`() {
        aRow()
        var slop = 0f

        press {
            slop = viewConfiguration.touchSlop
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveBy(Offset(0f, slop / 2f))
            moveBy(Offset(0f, slop * 2f))
            up()
        }

        assertEquals(slop * 2.5f, dragged, 0.01f)
    }

    @Test
    fun `a drag that wanders back within slop still ends as a drag`() {
        aRow()

        press {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            moveBy(Offset(0f, viewConfiguration.touchSlop * 3f))
            moveBy(Offset(0f, -viewConfiguration.touchSlop * 3f))
            up()
        }

        assertEquals("drag end", heard.last())
        assertEquals(emptyList<String>(), heard.filter { it == "long press" })
    }

    private fun aRow() {
        compose.setContent {
            Box(
                modifier =
                    Modifier
                        .size(240.dp, FavoriteRowHeight)
                        .testTag("Favorite")
                        .onTapLongPressOrDrag(
                            key = Unit,
                            onTap = { heard += "tap" },
                            onLongPress = { heard += "long press" },
                            onDrag = { travelled ->
                                dragged += travelled
                                heard += "drag"
                            },
                            onDragEnd = { heard += "drag end" },
                        ),
            )
        }
    }

    private fun press(gesture: TouchInjectionScope.() -> Unit) {
        compose.onNodeWithTag("Favorite").performTouchInput(gesture)
        compose.waitForIdle()
    }
}
