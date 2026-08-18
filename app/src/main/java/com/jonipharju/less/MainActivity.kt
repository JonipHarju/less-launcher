package com.jonipharju.less

import android.content.ActivityNotFoundException
import android.content.Intent
import android.icu.text.DateFormat
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jonipharju.less.launcher.AndroidLauncherRepository
import com.jonipharju.less.launcher.LauncherRepository
import com.jonipharju.less.launcher.rankedFor
import kotlinx.coroutines.delay
import java.util.Date
import java.util.Locale

/** Android opens this activity when Less is selected as the default Home app. */
class MainActivity : ComponentActivity() {
    private lateinit var launcherRepository: AndroidLauncherRepository

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
            )
        }
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

    BackHandler(enabled = surface == LauncherSurface.Drawer) {
        surface = LauncherSurface.Home
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
        LauncherSurface.Drawer -> Drawer(repository)
    }
}

@Composable
internal fun Home(
    repository: LauncherRepository,
    timeText: String,
    dateText: String,
    onOpenClock: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val drawerSwipeThreshold = with(LocalDensity.current) { 64.dp.toPx() }
    val favorites by repository.favorites.collectAsState()
    val installedApps by repository.installedApps.collectAsState()
    val installedById = installedApps.associateBy { it.id }

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
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
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
                        style = TextStyle(color = Color.White, fontSize = 24.sp),
                    )
                }
            }
        }
    }
}

private fun formattedTime(
    context: android.content.Context,
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
}

/** The full list of installed apps. */
@Composable
internal fun Drawer(repository: LauncherRepository) {
    val installedApps by repository.installedApps.collectAsState()
    var query by remember { mutableStateOf("") }
    val rankedApps = installedApps.rankedFor(query)
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(searchFocusRequester) {
        searchFocusRequester.requestFocus()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 32.dp),
    ) {
        item(key = "search") {
            val searchDescription = stringResource(R.string.search_apps)
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .focusRequester(searchFocusRequester)
                        .semantics { contentDescription = searchDescription },
                textStyle = TextStyle(color = Color.LightGray, fontSize = 18.sp),
                cursorBrush = SolidColor(Color.LightGray),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions =
                    KeyboardActions(
                        onSearch = { rankedApps.firstOrNull()?.let(repository::launch) },
                    ),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            BasicText(
                                text = searchDescription,
                                style = TextStyle(color = Color.Gray, fontSize = 18.sp),
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
        items(
            items = rankedApps,
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
