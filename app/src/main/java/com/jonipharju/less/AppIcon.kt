package com.jonipharju.less

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.jonipharju.less.launcher.AppIcon
import com.jonipharju.less.launcher.IconMode

internal val LauncherIconSize = 32.dp

/** One app's Icon Mode treatment, shared by Home and the Drawer. */
@Composable
internal fun LauncherAppIcon(
    icon: AppIcon?,
    mode: IconMode,
    label: String,
) {
    if (mode == IconMode.Hidden || icon == null) return

    val theme = LocalTheme.current
    val image = if (mode == IconMode.Tinted) icon.themeable ?: icon.native else icon.native
    Image(
        bitmap = image,
        contentDescription = "$label icon",
        modifier = Modifier.size(LauncherIconSize),
        colorFilter = if (mode == IconMode.Tinted) ColorFilter.tint(theme.accentColor) else null,
    )
}
