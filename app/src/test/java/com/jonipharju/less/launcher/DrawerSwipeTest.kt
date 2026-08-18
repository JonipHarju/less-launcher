package com.jonipharju.less.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerSwipeTest {
    @Test
    fun `swipe up opens the Drawer when it is the chosen direction`() {
        val direction = DrawerOpenDirection.SwipeUp

        assertTrue(direction.opensDrawer(upwards(THRESHOLD)))
        assertFalse(direction.opensDrawer(downwards(THRESHOLD)))
    }

    @Test
    fun `swipe down opens the Drawer when it is the chosen direction`() {
        val direction = DrawerOpenDirection.SwipeDown

        assertTrue(direction.opensDrawer(downwards(THRESHOLD)))
        assertFalse(direction.opensDrawer(upwards(THRESHOLD)))
    }

    @Test
    fun `a drag shorter than the threshold opens nothing`() {
        assertFalse(DrawerOpenDirection.SwipeUp.opensDrawer(upwards(THRESHOLD - 1f)))
        assertFalse(DrawerOpenDirection.SwipeDown.opensDrawer(downwards(THRESHOLD - 1f)))
    }

    @Test
    fun `the inverse of the open swipe closes the Drawer`() {
        assertTrue(DrawerOpenDirection.SwipeUp.closesDrawer(downwards(THRESHOLD)))
        assertTrue(DrawerOpenDirection.SwipeDown.closesDrawer(upwards(THRESHOLD)))
    }

    @Test
    fun `the open swipe repeated does not close the Drawer`() {
        assertFalse(DrawerOpenDirection.SwipeUp.closesDrawer(upwards(THRESHOLD)))
        assertFalse(DrawerOpenDirection.SwipeDown.closesDrawer(downwards(THRESHOLD)))
    }
}

private const val THRESHOLD = 64f

private fun upwards(distance: Float) = VerticalSwipe(distance = -distance, threshold = THRESHOLD)

private fun downwards(distance: Float) = VerticalSwipe(distance = distance, threshold = THRESHOLD)
