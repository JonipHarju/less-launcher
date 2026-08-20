package com.jonipharju.less

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.icu.text.DateFormat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import com.jonipharju.less.launcher.AndroidLauncherRepository
import com.jonipharju.less.launcher.LauncherRepository
import com.jonipharju.less.launcher.isRunning
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import java.util.Date
import java.util.Locale

/** Android opens this activity when Less is selected as the default Home app. */
class MainActivity : ComponentActivity() {
    private lateinit var launcherRepository: LauncherRepository

    /**
     * The manifest declares one intent filter, the home one, so every intent delivered to an
     * already-running Less is the user pressing home and asking for Home.
     */
    private val homeRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        // Light system-bar appearance would otherwise opt into the platform's navigation-bar
        // contrast scrim, which paints a bright band over the Wallpaper on light Themes.
        window.isNavigationBarContrastEnforced = false
        super.onCreate(savedInstanceState)
        launcherRepository = AndroidLauncherRepository(applicationContext)
        setContent {
            LessLauncher(
                repository = launcherRepository,
                onOpenClock = { openFirst(clockOpenIntents(), ::startActivitySafely) },
                onOpenCalendar = { now -> openFirst(calendarOpenIntents(now), ::startActivitySafely) },
                homeRequests = homeRequests,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homeRequests.tryEmit(Unit)
    }

    override fun onResume() {
        super.onResume()
        launcherRepository.onForeground()
    }

    override fun onDestroy() {
        launcherRepository.close()
        super.onDestroy()
    }

    private fun startActivitySafely(intent: Intent): Boolean =
        try {
            startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
}

@Composable
internal fun LessLauncher(
    repository: LauncherRepository,
    onOpenClock: () -> Boolean = { true },
    onOpenCalendar: (Long) -> Boolean = { true },
    homeRequests: Flow<Unit> = emptyFlow(),
) {
    var surface by remember { mutableStateOf(LauncherSurface.Home) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var notice by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    val settings by repository.settings.collectAsState()
    val hasReadStoredSettings by repository.hasReadStoredSettings.collectAsState()
    // A launcher that has run before must not flash Setup on the way in: until the store has
    // answered, `settings` is only defaults, and one of those defaults is that Setup never ran.
    val inSetup = hasReadStoredSettings && settings.setupStep.isRunning()
    val theme = settings.theme()
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val lightBars = theme.textColor.luminance() < 0.5f
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = lightBars
            isAppearanceLightNavigationBars = lightBars
        }
        // Kept off here as well as at the edge-to-edge setup: asking for light navigation
        // icons re-opts into the platform contrast scrim unless we refuse it every time.
        window.isNavigationBarContrastEnforced = false
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L - (now % 60_000L))
            now = System.currentTimeMillis()
        }
    }

    LaunchedEffect(homeRequests) {
        homeRequests.collect { surface = LauncherSurface.Home }
    }

    BackHandler(enabled = !inSetup && surface != LauncherSurface.Home) {
        surface = if (surface == LauncherSurface.Settings) LauncherSurface.Drawer else LauncherSurface.Home
    }

    ThemedSurface(
        wallpaper = SurfaceWallpaper.System,
        theme = theme,
        // Setup wears the full Scrim: it is a surface to read, not a Wallpaper to admire.
        scrim = if (surface == LauncherSurface.Home && !inSetup) theme.scrim.forHome() else theme.scrim,
    ) {
        if (!hasReadStoredSettings) {
            // The Scrim over the Wallpaper, and nothing else, for the frames it takes to read.
        } else if (inSetup) {
            Setup(repository = repository)
        } else {
            when (surface) {
                LauncherSurface.Home ->
                    Home(
                        repository = repository,
                        timeText = formattedTime(context, now),
                        dateText = formattedDate(now),
                        onOpenClock = {
                            if (!onOpenClock()) notice = R.string.clock_unavailable
                        },
                        onOpenCalendar = {
                            if (!onOpenCalendar(now)) notice = R.string.calendar_unavailable
                        },
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
    }

    notice?.let { message ->
        Notice(message = stringResource(message), onDismiss = { notice = null })
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
