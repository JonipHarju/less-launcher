package com.jonipharju.less

import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.jonipharju.less.launcher.FavoritesSoftCap
import com.jonipharju.less.launcher.LauncherApp
import com.jonipharju.less.launcher.LauncherRepository
import com.jonipharju.less.launcher.VerticalSwipe
import com.jonipharju.less.launcher.closesDrawer
import com.jonipharju.less.launcher.customLabels
import com.jonipharju.less.launcher.exceedSoftCap
import com.jonipharju.less.launcher.hold
import com.jonipharju.less.launcher.pinning
import com.jonipharju.less.launcher.rankedFor
import com.jonipharju.less.launcher.withoutHidden
import kotlinx.coroutines.launch

/** The full list of installed apps, its search field, and the way back out. */
@Composable
internal fun Drawer(
    repository: LauncherRepository,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val settings by repository.settings.collectAsState()
    val installedApps by repository.installedApps.collectAsState()
    val favorites by repository.favorites.collectAsState()
    val hiddenApps by repository.hiddenApps.collectAsState()
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    // A Hidden App is out of the Drawer entirely, search included. A Favorite the user renamed
    // answers to their own name for it as well as to its real one.
    val rankedApps = installedApps.withoutHidden(hiddenApps).rankedFor(query, favorites.customLabels())
    val searchFocusRequester = remember { FocusRequester() }
    val opensKeyboard = settings.opensKeyboardWithDrawer
    val drawerOpenDirection = settings.drawerOpenDirection
    val theme = LocalTheme.current
    val iconMode = effectiveIconMode(theme, settings.iconModeOverride)

    var curated by remember { mutableStateOf<LauncherApp?>(null) }
    var crowdedHome by remember { mutableStateOf(false) }

    LaunchedEffect(searchFocusRequester, opensKeyboard) {
        if (opensKeyboard) searchFocusRequester.requestFocus()
    }

    val closeOnSwipe: (VerticalSwipe) -> Unit = { swipe ->
        if (drawerOpenDirection.closesDrawer(swipe)) onClose()
    }
    // Over the top bar the swipe is an ordinary drag; over the list it is whatever
    // scrolling the list had no room left to consume.
    val closeOnOverscroll = rememberOverscrollSwipe(closeOnSwipe)

    // The top bar and the search field stay put, so the closing swipe always has somewhere
    // to land: over the list it only survives as overscroll the list had no room to use.
    Column(modifier = Modifier.fillMaxSize().onVerticalSwipe(closeOnSwipe)) {
        DrawerTopBar(onClose = onClose, onOpenSettings = onOpenSettings)
        SearchField(
            query = query,
            onQueryChange = { query = it },
            onSearch = { rankedApps.firstOrNull()?.let(repository::launch) },
            focusRequester = searchFocusRequester,
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize().nestedScroll(closeOnOverscroll),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            items(
                items = rankedApps,
                key = { app ->
                    "${app.id.profileSerialNumber}:${app.id.packageName}/${app.id.activityName}"
                },
            ) { app ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { repository.launch(app) },
                                onLongClick = { curated = app },
                                // Split 10 + 6 so the 32dp icon sets the row height, while a row
                                // with icons off keeps the 16dp the label had on its own.
                            ).padding(horizontal = 24.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(LauncherIconGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LauncherAppIcon(app.icon, iconMode)
                    BasicText(
                        text = app.label,
                        modifier = Modifier.padding(vertical = 6.dp),
                        style =
                            TextStyle(
                                color = theme.textColor,
                                fontFamily = theme.fontFamily,
                                fontSize = theme.typeScale.app.size,
                                fontWeight = theme.typeScale.app.weight,
                            ),
                    )
                }
            }
        }
    }

    curated?.let { app ->
        AppMenu(title = app.label, onDismiss = { curated = null }) {
            if (!favorites.hold(app.id)) {
                MenuAction(label = stringResource(R.string.favorite_pin)) {
                    curated = null
                    val pin = favorites.pinning(app.id)
                    // The cap advises rather than blocks: the Favorite goes on, and the user hears about it.
                    crowdedHome = (favorites + pin).exceedSoftCap()
                    scope.launch { repository.chooseFavorite(pin) }
                }
            }
            MenuAction(label = stringResource(R.string.app_hide)) {
                curated = null
                scope.launch { repository.hideApp(app.id) }
            }
            MenuAction(label = stringResource(R.string.app_info)) {
                curated = null
                repository.showAppInfo(app.id)
            }
            MenuAction(label = stringResource(R.string.app_uninstall)) {
                curated = null
                repository.requestUninstall(app.id)
            }
        }
    }

    if (crowdedHome) {
        Notice(
            message = stringResource(R.string.favorites_soft_cap, FavoritesSoftCap),
            onDismiss = { crowdedHome = false },
        )
    }
}

@Composable
private fun DrawerTopBar(
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    TopBar {
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
    val theme = LocalTheme.current
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .focusRequester(focusRequester)
                .semantics { contentDescription = searchDescription },
        textStyle =
            TextStyle(
                color = theme.secondaryTextColor,
                fontFamily = theme.fontFamily,
                fontSize = theme.typeScale.search.size,
                fontWeight = theme.typeScale.search.weight,
            ),
        cursorBrush = SolidColor(theme.accentColor),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        decorationBox = { innerTextField ->
            Box {
                if (query.isEmpty()) {
                    BasicText(
                        text = searchDescription,
                        style =
                            TextStyle(
                                color = theme.secondaryTextColor.copy(alpha = 0.7f),
                                fontFamily = theme.fontFamily,
                                fontSize = theme.typeScale.search.size,
                                fontWeight = theme.typeScale.search.weight,
                            ),
                    )
                }
                innerTextField()
            }
        },
    )
}
