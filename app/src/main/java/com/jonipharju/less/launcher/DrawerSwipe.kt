package com.jonipharju.less.launcher

/**
 * Whether a vertical drag of [dragDistance] pixels — negative upwards — travelled far
 * enough past [threshold], in the direction the user chose, to open the Drawer.
 */
internal fun DrawerOpenDirection.opensDrawer(
    dragDistance: Float,
    threshold: Float,
): Boolean =
    when (this) {
        DrawerOpenDirection.SwipeUp -> dragDistance <= -threshold
        DrawerOpenDirection.SwipeDown -> dragDistance >= threshold
    }

/** Whether the same drag is the inverse swipe, which closes the Drawer again. */
internal fun DrawerOpenDirection.closesDrawer(
    dragDistance: Float,
    threshold: Float,
): Boolean = inverse().opensDrawer(dragDistance, threshold)

private fun DrawerOpenDirection.inverse() =
    when (this) {
        DrawerOpenDirection.SwipeUp -> DrawerOpenDirection.SwipeDown
        DrawerOpenDirection.SwipeDown -> DrawerOpenDirection.SwipeUp
    }
