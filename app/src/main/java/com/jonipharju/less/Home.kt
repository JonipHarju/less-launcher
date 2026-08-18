package com.jonipharju.less

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jonipharju.less.launcher.HomeAlignment
import com.jonipharju.less.launcher.LauncherRepository
import com.jonipharju.less.launcher.opensDrawer

/** The clock, the date, and the Favorites, over the Wallpaper. */
@Composable
internal fun Home(
    repository: LauncherRepository,
    timeText: String,
    dateText: String,
    onOpenClock: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val settings by repository.settings.collectAsState()
    val favorites by repository.favorites.collectAsState()
    val installedApps by repository.installedApps.collectAsState()
    val installedById = installedApps.associateBy { it.id }
    val drawerOpenDirection = settings.drawerOpenDirection

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .onVerticalSwipe { dragDistance, threshold ->
                    if (drawerOpenDirection.opensDrawer(dragDistance, threshold)) onOpenDrawer()
                },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = settings.homeAlignment.asHorizontalAlignment(),
        ) {
            val textAlign = settings.homeAlignment.asTextAlign()
            BasicText(
                text = timeText,
                modifier = Modifier.clickable(onClick = onOpenClock),
                style = TextStyle(color = Color.White, fontSize = 48.sp),
            )
            BasicText(
                text = dateText,
                modifier = Modifier.clickable(onClick = onOpenCalendar),
                style = TextStyle(color = Color.LightGray, fontSize = 18.sp),
            )
            favorites.forEach { favorite ->
                val app = installedById[favorite.appId]
                if (app != null) {
                    BasicText(
                        text = favorite.customLabel ?: app.label,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { repository.launch(app) }
                                .padding(vertical = 12.dp),
                        style = TextStyle(color = Color.White, fontSize = 24.sp, textAlign = textAlign),
                    )
                }
            }
        }
    }
}

private fun HomeAlignment.asHorizontalAlignment() =
    when (this) {
        HomeAlignment.Left -> Alignment.Start
        HomeAlignment.Centred -> Alignment.CenterHorizontally
    }

private fun HomeAlignment.asTextAlign() =
    when (this) {
        HomeAlignment.Left -> TextAlign.Start
        HomeAlignment.Centred -> TextAlign.Center
    }
