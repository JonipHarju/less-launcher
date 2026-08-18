package com.jonipharju.less

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.icu.text.DateFormat
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.jonipharju.less.launcher.AndroidLauncherRepository
import com.jonipharju.less.launcher.LauncherRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import java.util.Date
import java.util.Locale

/** Android opens this activity when Less is selected as the default Home app. */
class MainActivity : ComponentActivity() {
    private lateinit var launcherRepository: AndroidLauncherRepository

    /**
     * The manifest declares one intent filter, the home one, so every intent delivered to an
     * already-running Less is the user pressing home and asking for Home.
     */
    private val homeRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcherRepository = AndroidLauncherRepository(applicationContext)
        setContent {
            LessLauncher(
                repository = launcherRepository,
                onOpenClock = { startActivitySafely(Intent(AlarmClock.ACTION_SHOW_ALARMS)) },
                onOpenCalendar = { now ->
                    startActivitySafely(
                        Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.calendar/time/$now")),
                    )
                },
                homeRequests = homeRequests,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homeRequests.tryEmit(Unit)
    }

    override fun onDestroy() {
        launcherRepository.close()
        super.onDestroy()
    }

    private fun startActivitySafely(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Some devices do not provide a clock or calendar activity.
        }
    }
}

@Composable
internal fun LessLauncher(
    repository: LauncherRepository,
    onOpenClock: () -> Unit = {},
    onOpenCalendar: (Long) -> Unit = {},
    homeRequests: Flow<Unit> = emptyFlow(),
) {
    var surface by remember { mutableStateOf(LauncherSurface.Home) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L - (now % 60_000L))
            now = System.currentTimeMillis()
        }
    }

    LaunchedEffect(homeRequests) {
        homeRequests.collect { surface = LauncherSurface.Home }
    }

    BackHandler(enabled = surface != LauncherSurface.Home) {
        surface = if (surface == LauncherSurface.Settings) LauncherSurface.Drawer else LauncherSurface.Home
    }

    when (surface) {
        LauncherSurface.Home ->
            Home(
                repository = repository,
                timeText = formattedTime(context, now),
                dateText = formattedDate(now),
                onOpenClock = onOpenClock,
                onOpenCalendar = { onOpenCalendar(now) },
                onOpenDrawer = { surface = LauncherSurface.Drawer },
            )
        LauncherSurface.Drawer ->
            Drawer(
                repository = repository,
                onClose = { surface = LauncherSurface.Home },
                onOpenSettings = { surface = LauncherSurface.Settings },
            )
        LauncherSurface.Settings ->
            Settings(
                repository = repository,
                onClose = { surface = LauncherSurface.Drawer },
            )
    }
}

private fun formattedTime(
    context: Context,
    now: Long,
): String {
    val skeleton =
        if (android.text.format.DateFormat
                .is24HourFormat(context)
        ) {
            "Hm"
        } else {
            "hm"
        }
    return DateFormat.getInstanceForSkeleton(skeleton, Locale.getDefault()).format(Date(now))
}

private fun formattedDate(now: Long): String = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault()).format(Date(now))

private enum class LauncherSurface {
    Home,
    Drawer,
    Settings,
}
