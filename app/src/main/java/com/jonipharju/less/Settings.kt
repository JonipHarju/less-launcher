package com.jonipharju.less

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jonipharju.less.launcher.DrawerOpenDirection
import com.jonipharju.less.launcher.HomeAlignment
import com.jonipharju.less.launcher.LauncherRepository
import com.jonipharju.less.launcher.LauncherSettings
import kotlinx.coroutines.launch

/** The one screen holding every option, reached from the Drawer's top bar. */
@Composable
internal fun Settings(
    repository: LauncherRepository,
    onClose: () -> Unit,
) {
    val settings by repository.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val apply: (LauncherSettings) -> Unit = { changed -> scope.launch { repository.updateSettings(changed) } }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
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

        SettingGroup(title = stringResource(R.string.settings_drawer_opens)) {
            DrawerOpenDirection.entries.forEach { direction ->
                Choice(
                    label = stringResource(direction.labelResource()),
                    selected = settings.drawerOpenDirection == direction,
                    onSelect = { apply(settings.copy(drawerOpenDirection = direction)) },
                )
            }
        }

        SettingGroup(title = stringResource(R.string.settings_home_alignment)) {
            HomeAlignment.entries.forEach { alignment ->
                Choice(
                    label = stringResource(alignment.labelResource()),
                    selected = settings.homeAlignment == alignment,
                    onSelect = { apply(settings.copy(homeAlignment = alignment)) },
                )
            }
        }

        SettingGroup(title = stringResource(R.string.settings_keyboard)) {
            Switch(
                label = stringResource(R.string.settings_open_keyboard_with_drawer),
                checked = settings.opensKeyboardWithDrawer,
                onCheckedChange = { checked -> apply(settings.copy(opensKeyboardWithDrawer = checked)) },
            )
        }
    }
}

@Composable
private fun SettingGroup(
    title: String,
    options: @Composable () -> Unit,
) {
    BasicText(
        text = title,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
        style = TextStyle(color = Color.Gray, fontSize = 14.sp),
    )
    options()
}

@Composable
private fun Choice(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    BasicText(
        text = label,
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = selected, onClick = onSelect)
                .padding(horizontal = 24.dp, vertical = 14.dp),
        style = TextStyle(color = if (selected) Color.White else Color.Gray, fontSize = 20.sp),
    )
}

@Composable
private fun Switch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(value = checked, onValueChange = onCheckedChange)
                .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = label,
            style = TextStyle(color = Color.White, fontSize = 20.sp),
        )
        BasicText(
            text = stringResource(if (checked) R.string.on else R.string.off),
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
