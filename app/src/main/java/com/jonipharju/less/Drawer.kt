package com.jonipharju.less

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jonipharju.less.launcher.LauncherRepository
import com.jonipharju.less.launcher.closesDrawer
import com.jonipharju.less.launcher.rankedFor

/** The full list of installed apps, its search field, and the way back out. */
@Composable
internal fun Drawer(
    repository: LauncherRepository,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val settings by repository.settings.collectAsState()
    val installedApps by repository.installedApps.collectAsState()
    var query by remember { mutableStateOf("") }
    val rankedApps = installedApps.rankedFor(query)
    val searchFocusRequester = remember { FocusRequester() }
    val opensKeyboard = settings.opensKeyboardWithDrawer
    val drawerOpenDirection = settings.drawerOpenDirection

    LaunchedEffect(searchFocusRequester, opensKeyboard) {
        if (opensKeyboard) searchFocusRequester.requestFocus()
    }

    val closeOnSwipe: (Float, Float) -> Unit = { dragDistance, threshold ->
        if (drawerOpenDirection.closesDrawer(dragDistance, threshold)) onClose()
    }
    // Over the top bar the swipe is an ordinary drag; over the list it is whatever
    // scrolling the list had no room left to consume.
    val closeOnOverscroll = rememberOverscrollSwipe(closeOnSwipe)

    Column(modifier = Modifier.fillMaxSize().onVerticalSwipe(closeOnSwipe)) {
        DrawerTopBar(onClose = onClose, onOpenSettings = onOpenSettings)
        LazyColumn(
            modifier = Modifier.fillMaxSize().nestedScroll(closeOnOverscroll),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item(key = "search") {
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    onSearch = { rankedApps.firstOrNull()?.let(repository::launch) },
                    focusRequester = searchFocusRequester,
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
}

@Composable
private fun DrawerTopBar(
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        GlyphControl(
            glyph = "✕",
            description = stringResource(R.string.drawer_close),
            onClick = onClose,
        )
        GlyphControl(
            glyph = "⚙",
            description = stringResource(R.string.open_settings),
            onClick = onOpenSettings,
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    focusRequester: FocusRequester,
) {
    val searchDescription = stringResource(R.string.search_apps)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .focusRequester(focusRequester)
                .semantics { contentDescription = searchDescription },
        textStyle = TextStyle(color = Color.LightGray, fontSize = 18.sp),
        cursorBrush = SolidColor(Color.LightGray),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
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
