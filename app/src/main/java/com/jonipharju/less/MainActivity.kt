package com.jonipharju.less

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jonipharju.less.launcher.AndroidLauncherRepository
import com.jonipharju.less.launcher.LauncherRepository

/** Android opens this activity when Less is selected as the default Home app. */
class MainActivity : ComponentActivity() {
    private lateinit var launcherRepository: AndroidLauncherRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcherRepository = AndroidLauncherRepository(applicationContext)
        setContent {
            LessLauncher(launcherRepository)
        }
    }

    override fun onDestroy() {
        launcherRepository.close()
        super.onDestroy()
    }
}

@Composable
internal fun LessLauncher(repository: LauncherRepository) {
    var surface by remember { mutableStateOf(LauncherSurface.Home) }

    BackHandler(enabled = surface == LauncherSurface.Drawer) {
        surface = LauncherSurface.Home
    }

    when (surface) {
        LauncherSurface.Home -> Home(onOpenDrawer = { surface = LauncherSurface.Drawer })
        LauncherSurface.Drawer -> Drawer(repository)
    }
}

@Composable
private fun Home(onOpenDrawer: () -> Unit) {
    val drawerSwipeThreshold = with(LocalDensity.current) { 64.dp.toPx() }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(onOpenDrawer, drawerSwipeThreshold) {
                    var dragDistance = 0f
                    detectVerticalDragGestures(
                        onDragStart = { dragDistance = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dragDistance += dragAmount
                        },
                        onDragEnd = {
                            if (dragDistance <= -drawerSwipeThreshold) onOpenDrawer()
                        },
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = stringResource(R.string.app_name),
            style =
                TextStyle(
                    color = Color.White,
                    fontSize = 32.sp,
                ),
        )
    }
}

private enum class LauncherSurface {
    Home,
    Drawer,
}

/** The full list of installed apps. */
@Composable
internal fun Drawer(repository: LauncherRepository) {
    val installedApps by repository.installedApps.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 32.dp),
    ) {
        items(
            items = installedApps,
            key = { app ->
                "${app.id.profileSerialNumber}:${app.id.packageName}/${app.id.activityName}"
            },
        ) { app ->
            BasicText(
                text = app.label,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { repository.launch(app) }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                style =
                    TextStyle(
                        color = Color.White,
                        fontSize = 24.sp,
                    ),
            )
        }
    }
}
