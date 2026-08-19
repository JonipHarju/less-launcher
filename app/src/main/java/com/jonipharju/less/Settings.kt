package com.jonipharju.less

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jonipharju.less.launcher.DrawerOpenDirection
import com.jonipharju.less.launcher.FavoritesSoftCap
import com.jonipharju.less.launcher.HomeAlignment
import com.jonipharju.less.launcher.IconMode
import com.jonipharju.less.launcher.LauncherApp
import com.jonipharju.less.launcher.LauncherAppId
import com.jonipharju.less.launcher.LauncherRepository
import com.jonipharju.less.launcher.LauncherSettings
import com.jonipharju.less.launcher.ShownFavorite
import com.jonipharju.less.launcher.exceedSoftCap
import com.jonipharju.less.launcher.hiddenAmong
import com.jonipharju.less.launcher.moved
import com.jonipharju.less.launcher.renamedTo
import com.jonipharju.less.launcher.shownAmong
import kotlinx.coroutines.launch

/** The one screen holding every option, reached from the Drawer's top bar. */
@Composable
internal fun Settings(
    repository: LauncherRepository,
    onClose: () -> Unit,
    onApplyWallpaper: (Theme) -> Unit = {},
) {
    val settings by repository.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val store: ((LauncherSettings) -> LauncherSettings) -> Unit = { change ->
        scope.launch { repository.updateSettings(change) }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar {
            BasicText(
                text = stringResource(R.string.settings),
                modifier = Modifier.padding(horizontal = 12.dp),
                style = themedTextStyle(size = 28.sp),
            )
            GlyphControl(
                glyph = "✕",
                description = stringResource(R.string.settings_close),
                onClick = onClose,
            )
        }

        GroupTitle(stringResource(R.string.settings_themes))
        ThemePicker(repository = repository, onApplyWallpaper = onApplyWallpaper)

        ChoiceGroup(
            title = stringResource(R.string.settings_drawer_opens),
            options = DrawerOpenDirection.entries,
            chosen = settings.drawerOpenDirection,
            label = { direction -> stringResource(direction.labelResource()) },
            onChoose = { direction -> store { it.copy(drawerOpenDirection = direction) } },
        )

        ChoiceGroup(
            title = stringResource(R.string.settings_home_alignment),
            options = HomeAlignment.entries,
            chosen = settings.homeAlignment,
            label = { alignment -> stringResource(alignment.labelResource()) },
            onChoose = { alignment -> store { it.copy(homeAlignment = alignment) } },
        )

        GroupTitle(stringResource(R.string.settings_keyboard))
        OnOff(
            label = stringResource(R.string.settings_open_keyboard_with_drawer),
            on = settings.opensKeyboardWithDrawer,
            onChange = { on -> store { it.copy(opensKeyboardWithDrawer = on) } },
        )

        FavoritesEditor(repository)

        HiddenApps(repository)

        ChoiceGroup(
            title = stringResource(R.string.settings_icon_mode),
            options = listOf(null, IconMode.Off, IconMode.Tinted, IconMode.Original),
            chosen = settings.iconModeOverride,
            label = { mode -> stringResource(mode.labelResource()) },
            onChoose = { mode -> store { it.copy(iconModeOverride = mode) } },
        )

        ConfigurationFile(repository)
    }
}

/** Every Favorite in one place, for the renaming and reordering that Home does one at a time. */
@Composable
private fun FavoritesEditor(repository: LauncherRepository) {
    val favorites by repository.favorites.collectAsState()
    val installedApps by repository.installedApps.collectAsState()
    val scope = rememberCoroutineScope()
    val shown = favorites.shownAmong(installedApps)
    var renaming by remember { mutableStateOf<LauncherAppId?>(null) }

    GroupTitle(stringResource(R.string.settings_favorites))

    if (shown.isEmpty()) {
        BasicText(
            text = stringResource(R.string.settings_favorites_empty),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            style = themedTextStyle(color = LocalTheme.current.secondaryTextColor, size = 16.sp),
        )
        return
    }

    if (favorites.exceedSoftCap()) {
        BasicText(
            text = stringResource(R.string.favorites_soft_cap, FavoritesSoftCap),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = themedTextStyle(color = LocalTheme.current.secondaryTextColor, size = 16.sp),
        )
    }

    val reorder: (Int, Int) -> Unit = { from, to ->
        scope.launch { repository.reorderFavorites(shown.map { it.favorite.appId }.moved(from, to)) }
    }

    shown.forEachIndexed { index, shownFavorite ->
        FavoriteEditorRow(
            shownFavorite = shownFavorite,
            canMoveUp = index > 0,
            canMoveDown = index < shown.lastIndex,
            onRename = { renaming = shownFavorite.favorite.appId },
            onMoveUp = { reorder(index, index - 1) },
            onMoveDown = { reorder(index, index + 1) },
            onUnpin = { scope.launch { repository.dismissFavorite(shownFavorite.favorite.appId) } },
        )
    }

    shown.firstOrNull { it.favorite.appId == renaming }?.let { shownFavorite ->
        RenameDialog(
            appLabel = shownFavorite.appLabel,
            currentLabel = shownFavorite.label,
            onDismiss = { renaming = null },
            onRename = { name ->
                renaming = null
                scope.launch { repository.chooseFavorite(shownFavorite.favorite.renamedTo(name)) }
            },
        )
    }
}

/**
 * Every Hidden App in one place, so that hiding stays visible and reversible: the user reads
 * off what they hid rather than having to remember it.
 */
@Composable
private fun HiddenApps(repository: LauncherRepository) {
    val installedApps by repository.installedApps.collectAsState()
    val hiddenApps by repository.hiddenApps.collectAsState()
    val scope = rememberCoroutineScope()
    val hidden = installedApps.hiddenAmong(hiddenApps)

    GroupTitle(stringResource(R.string.settings_hidden_apps))

    if (hidden.isEmpty()) {
        BasicText(
            text = stringResource(R.string.settings_hidden_apps_empty),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            style = themedTextStyle(color = LocalTheme.current.secondaryTextColor, size = 16.sp),
        )
        return
    }

    hidden.forEach { app ->
        HiddenAppRow(
            app = app,
            onUnhide = { scope.launch { repository.unhideApp(app.id) } },
        )
    }
}

/** One Hidden App, and the one control that undoes hiding it. */
@Composable
private fun HiddenAppRow(
    app: LauncherApp,
    onUnhide: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = app.label,
            modifier = Modifier.weight(1f).padding(vertical = 10.dp),
            style = themedTextStyle(size = 20.sp),
        )
        GlyphControl(
            glyph = "↩",
            description = stringResource(R.string.app_unhide, app.label),
            onClick = onUnhide,
        )
    }
}

/** One Favorite in the editor: its name to rename, and the controls that move or unpin it. */
@Composable
private fun FavoriteEditorRow(
    shownFavorite: ShownFavorite,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRename: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onUnpin: () -> Unit,
) {
    val label = shownFavorite.label

    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = label,
            modifier = Modifier.weight(1f).clickable(onClick = onRename).padding(vertical = 10.dp),
            style = themedTextStyle(size = 20.sp),
        )
        if (canMoveUp) {
            GlyphControl(
                glyph = "↑",
                description = stringResource(R.string.favorites_move_up, label),
                onClick = onMoveUp,
            )
        }
        if (canMoveDown) {
            GlyphControl(
                glyph = "↓",
                description = stringResource(R.string.favorites_move_down, label),
                onClick = onMoveDown,
            )
        }
        GlyphControl(
            glyph = "✕",
            description = stringResource(R.string.favorites_unpin, label),
            onClick = onUnpin,
        )
    }
}

/** One titled setting whose value is picked from a short, fixed list. */
@Composable
private fun <T> ChoiceGroup(
    title: String,
    options: List<T>,
    chosen: T,
    label: @Composable (T) -> String,
    onChoose: (T) -> Unit,
) {
    GroupTitle(title)
    options.forEach { option ->
        val isChosen = option == chosen
        BasicText(
            text = label(option),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = isChosen, onClick = { onChoose(option) })
                    .padding(horizontal = 24.dp, vertical = 14.dp),
            style =
                themedTextStyle(
                    color = if (isChosen) LocalTheme.current.textColor else LocalTheme.current.secondaryTextColor,
                    size = 20.sp,
                ),
        )
    }
}

/** One setting that is simply on or off, its state spelled out beside its label. */
@Composable
private fun OnOff(
    label: String,
    on: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(value = on, onValueChange = onChange)
                .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = label,
            style = themedTextStyle(size = 20.sp),
        )
        BasicText(
            text = stringResource(if (on) R.string.settings_on else R.string.settings_off),
            style = themedTextStyle(color = LocalTheme.current.secondaryTextColor, size = 20.sp),
        )
    }
}

/** The Credit is the picker: artist, title, year, collection, source — not a swatch. */
@Composable
internal fun ThemePicker(
    repository: LauncherRepository,
    onApplyWallpaper: (Theme) -> Unit,
) {
    val settings by repository.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val chosen = themeById(settings.themeId)
    val colors = LocalTheme.current

    Themes.forEach { option ->
        val isChosen = option.id == chosen.id
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    // One selected option for the whole Credit, not a tap target per line.
                    .semantics(mergeDescendants = true) {}
                    .selectable(
                        selected = isChosen,
                        role = Role.RadioButton,
                        onClick = {
                            scope.launch { repository.updateSettings { it.copy(themeId = option.id) } }
                            onApplyWallpaper(option)
                        },
                    ).padding(horizontal = 24.dp, vertical = 14.dp),
        ) {
            BasicText(
                text = option.credit.artist,
                style =
                    themedTextStyle(
                        color = if (isChosen) colors.textColor else colors.secondaryTextColor,
                        size = 20.sp,
                    ),
            )
            BasicText(
                text = option.credit.title,
                style =
                    themedTextStyle(
                        color = if (isChosen) colors.textColor else colors.secondaryTextColor,
                        size = 16.sp,
                    ),
            )
            BasicText(
                text = "${option.credit.year}  ·  ${option.credit.collection}",
                style = themedTextStyle(color = colors.secondaryTextColor, size = 14.sp),
            )
            BasicText(
                text = option.credit.source,
                style = themedTextStyle(color = colors.secondaryTextColor, size = 14.sp),
            )
        }
    }
}

private fun DrawerOpenDirection.labelResource() =
    when (this) {
        DrawerOpenDirection.SwipeUp -> R.string.settings_swipe_up
        DrawerOpenDirection.SwipeDown -> R.string.settings_swipe_down
    }

private fun HomeAlignment.labelResource() =
    when (this) {
        HomeAlignment.Left -> R.string.settings_home_alignment_left
        HomeAlignment.Centred -> R.string.settings_home_alignment_centred
    }

private fun IconMode?.labelResource() =
    when (this) {
        null -> R.string.settings_icon_mode_theme
        IconMode.Off -> R.string.settings_icon_mode_off
        IconMode.Tinted -> R.string.settings_icon_mode_tinted
        IconMode.Original -> R.string.settings_icon_mode_original
    }
