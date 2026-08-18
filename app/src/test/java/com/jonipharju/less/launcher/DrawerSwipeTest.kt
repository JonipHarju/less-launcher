package com.jonipharju.less.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerSwipeTest {
    @Test
    fun `swipe up opens the Drawer when it is the chosen direction`() {
        val direction = DrawerOpenDirection.SwipeUp

        assertTrue(direction.opensDrawer(dragDistance = -THRESHOLD, threshold = THRESHOLD))
        assertFalse(direction.opensDrawer(dragDistance = THRESHOLD, threshold = THRESHOLD))
    }

    @Test
    fun `swipe down opens the Drawer when it is the chosen direction`() {
        val direction = DrawerOpenDirection.SwipeDown

        assertTrue(direction.opensDrawer(dragDistance = THRESHOLD, threshold = THRESHOLD))
        assertFalse(direction.opensDrawer(dragDistance = -THRESHOLD, threshold = THRESHOLD))
    }

    @Test
    fun `a drag shorter than the threshold opens nothing`() {
        assertFalse(DrawerOpenDirection.SwipeUp.opensDrawer(dragDistance = -THRESHOLD + 1f, threshold = THRESHOLD))
        assertFalse(DrawerOpenDirection.SwipeDown.opensDrawer(dragDistance = THRESHOLD - 1f, threshold = THRESHOLD))
    }

    @Test
    fun `the inverse of the open swipe closes the Drawer`() {
        assertTrue(DrawerOpenDirection.SwipeUp.closesDrawer(dragDistance = THRESHOLD, threshold = THRESHOLD))
        assertTrue(DrawerOpenDirection.SwipeDown.closesDrawer(dragDistance = -THRESHOLD, threshold = THRESHOLD))
    }

    @Test
    fun `the open swipe repeated does not close the Drawer`() {
        assertFalse(DrawerOpenDirection.SwipeUp.closesDrawer(dragDistance = -THRESHOLD, threshold = THRESHOLD))
        assertFalse(DrawerOpenDirection.SwipeDown.closesDrawer(dragDistance = THRESHOLD, threshold = THRESHOLD))
    }
}

private const val THRESHOLD = 64f
