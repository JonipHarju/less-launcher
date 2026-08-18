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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jonipharju.less.launcher.DrawerOpenDirection
import com.jonipharju.less.launcher.FavoritesSoftCap
import com.jonipharju.less.launcher.HomeAlignment
import com.jonipharju.less.launcher.LauncherAppId
import com.jonipharju.less.launcher.LauncherRepository
import com.jonipharju.less.launcher.LauncherSettings
import com.jonipharju.less.launcher.ShownFavorite
import com.jonipharju.less.launcher.exceedSoftCap
import com.jonipharju.less.launcher.moved
import com.jonipharju.less.launcher.renamedTo
import com.jonipharju.less.launcher.shownAmong
import kotlinx.coroutines.launch

/** The one screen holding every option, reached from the Drawer's top bar. */
@Composable
internal fun Settings(
    repository: LauncherRepository,
    onClose: () -> Unit,
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
                style = TextStyle(color = Color.White, fontSize = 28.sp),
            )
            GlyphControl(
                glyph = "✕",
                description = stringResource(R.string.settings_close),
                onClick = onClose,
            )
        }

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
            style = TextStyle(color = Color.Gray, fontSize = 16.sp),
        )
        return
    }

    if (favorites.exceedSoftCap()) {
        BasicText(
            text = stringResource(R.string.favorites_soft_cap, FavoritesSoftCap),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            style = TextStyle(color = Color.Gray, fontSize = 16.sp),
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
            style = TextStyle(color = Color.White, fontSize = 20.sp),
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
            style = TextStyle(color = if (isChosen) Color.White else Color.Gray, fontSize = 20.sp),
        )
    }
}

@Composable
private fun GroupTitle(title: String) {
    BasicText(
        text = title,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
        style = TextStyle(color = Color.Gray, fontSize = 14.sp),
    )
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
            style = TextStyle(color = Color.White, fontSize = 20.sp),
        )
        BasicText(
            text = stringResource(if (on) R.string.settings_on else R.string.settings_off),
            style = TextStyle(color = Color.LightGray, fontSize = 20.sp),
        )
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
