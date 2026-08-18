package com.jonipharju.less.launcher

/**
 * A completed vertical drag: how far it travelled in pixels — negative upwards — and the
 * distance a drag has to cover before it counts as a swipe rather than a stray touch.
 */
internal data class VerticalSwipe(
    val distance: Float,
    val threshold: Float,
)

/** Whether [swipe] went far enough, the way the user chose, to open the Drawer. */
internal fun DrawerOpenDirection.opensDrawer(swipe: VerticalSwipe): Boolean =
    when (this) {
        DrawerOpenDirection.SwipeUp -> swipe.distance <= -swipe.threshold
        DrawerOpenDirection.SwipeDown -> swipe.distance >= swipe.threshold
    }

/** Whether [swipe] is the inverse swipe, which closes the Drawer again. */
internal fun DrawerOpenDirection.closesDrawer(swipe: VerticalSwipe): Boolean = inverse().opensDrawer(swipe)

private fun DrawerOpenDirection.inverse() =
    when (this) {
        DrawerOpenDirection.SwipeUp -> DrawerOpenDirection.SwipeDown
        DrawerOpenDirection.SwipeDown -> DrawerOpenDirection.SwipeUp
    }
