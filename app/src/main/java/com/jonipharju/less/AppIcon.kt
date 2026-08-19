package com.jonipharju.less

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.unit.dp
import com.jonipharju.less.launcher.AppIcon
import com.jonipharju.less.launcher.IconMode

internal val LauncherIconSize = 32.dp

/** The gap between an app's icon and its label, so Home and the Drawer space them alike. */
internal val LauncherIconGap = 12.dp

/** One app's icon under the active Icon Mode, drawn the same way by Home and the Drawer. */
@Composable
internal fun LauncherAppIcon(
    icon: AppIcon?,
    mode: IconMode,
) {
    if (mode == IconMode.Off || icon == null) return

    val theme = LocalTheme.current
    val image = if (mode == IconMode.Tinted) icon.themeable ?: icon.original else icon.original
    Image(
        bitmap = image,
        contentDescription = null,
        modifier = Modifier.size(LauncherIconSize),
        colorFilter =
            when {
                mode != IconMode.Tinted -> null
                icon.themeable != null -> ColorFilter.tint(theme.accentColor)
                else -> ColorFilter.colorMatrix(desaturatedTint(theme.accentColor))
            },
    )
}

/**
 * Colourises artwork an app supplies no themeable layer for: strip its colour, then scale what is
 * left by the Theme colour, so the icon's own light and dark detail survives the tint.
 */
internal fun desaturatedTint(color: Color): ColorMatrix =
    ColorMatrix().apply {
        setToScale(color.red, color.green, color.blue, color.alpha)
        timesAssign(ColorMatrix().apply { setToSaturation(0f) })
    }
