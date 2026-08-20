package com.jonipharju.less

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jonipharju.less.launcher.HomeAlignment
import com.jonipharju.less.launcher.IconMode
import com.jonipharju.less.launcher.LauncherRepository
import com.jonipharju.less.launcher.ShownFavorite
import com.jonipharju.less.launcher.VerticalSwipe
import com.jonipharju.less.launcher.opensDrawer
import com.jonipharju.less.launcher.renamedTo
import com.jonipharju.less.launcher.shownAmong
import kotlinx.coroutines.launch

/** Every Favorite occupies one row of this height, so a drag of one row moves it one place. */
internal val FavoriteRowHeight = 52.dp

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
    val scope = rememberCoroutineScope()
    val drawerOpenDirection = settings.drawerOpenDirection
    val theme = LocalTheme.current
    val iconMode = effectiveIconMode(theme, settings.iconModeOverride)

    val curation = rememberFavoriteCuration()
    val home = curation.state(favorites.shownAmong(installedApps))
    var uninstallFailed by remember { mutableStateOf(false) }

    val openDrawerOnSwipe: (VerticalSwipe) -> Unit = { swipe ->
        if (drawerOpenDirection.opensDrawer(swipe)) onOpenDrawer()
    }
    // Home scrolls once the Favorites outgrow it, so past that point the opening swipe only
    // survives as overscroll the list had no room to use.
    val openDrawerOnOverscroll = rememberOverscrollSwipe(openDrawerOnSwipe)

    Box(
        modifier = Modifier.fillMaxSize().onVerticalSwipe(openDrawerOnSwipe),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .nestedScroll(openDrawerOnOverscroll)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            horizontalAlignment = settings.homeAlignment.asHorizontalAlignment(),
        ) {
            val textAlign = settings.homeAlignment.asTextAlign()
            val iconsShown = iconMode != IconMode.Off
            val rowArrangement = settings.homeAlignment.asRowArrangement(iconsShown)
            // Centred Home centres the Favorites as one block when it shows icons. Its intrinsic
            // width is the longest row, so every icon begins in the same column.
            val centreFavoritesBlock = settings.homeAlignment == HomeAlignment.Centred && iconsShown
            BasicText(
                text = timeText,
                modifier = Modifier.clickable(onClick = onOpenClock),
                style =
                    TextStyle(
                        color = theme.textColor,
                        fontFamily = theme.fontFamily,
                        fontSize = theme.typeScale.clock.size,
                        fontWeight = theme.typeScale.clock.weight,
                        shadow = textHalo(theme.textColor),
                    ),
            )
            BasicText(
                text = dateText,
                modifier = Modifier.clickable(onClick = onOpenCalendar),
                style =
                    TextStyle(
                        color = theme.secondaryTextColor,
                        fontFamily = theme.fontFamily,
                        fontSize = theme.typeScale.date.size,
                        fontWeight = theme.typeScale.date.weight,
                        shadow = textHalo(theme.secondaryTextColor),
                    ),
            )
            Column(
                modifier = if (centreFavoritesBlock) Modifier.width(IntrinsicSize.Max) else Modifier,
            ) {
                home.shown.forEach { shownFavorite ->
                    // Keyed by the app rather than by the row it currently occupies, so that a row
                    // being dragged keeps its identity — and with it the live touch — as it moves.
                    key(shownFavorite.favorite.appId) {
                        if (shownFavorite.app == null) {
                            TombstoneRow(
                                shownFavorite = shownFavorite,
                                textAlign = textAlign,
                                onDismiss = { curation.curate(shownFavorite.favorite.appId) },
                            )
                        } else {
                            FavoriteRow(
                                shownFavorite = shownFavorite,
                                textAlign = textAlign,
                                rowArrangement = rowArrangement,
                                iconMode = iconMode,
                                onLaunch = { repository.launch(shownFavorite.app) },
                                onCurate = { curation.curate(shownFavorite.favorite.appId) },
                                onDrag = { travelled -> curation.draggedBy(shownFavorite.favorite.appId, travelled, home.shown) },
                                onDragEnd = { scope.launch { curation.dragEnded(repository::reorderFavorites) } },
                            )
                        }
                    }
                }
            }
        }
    }

    home.curated?.let { shownFavorite ->
        if (shownFavorite.app == null) {
            TombstoneMenu(
                shownFavorite = shownFavorite,
                onDismiss = curation::dismissMenu,
                onDismissTombstone = {
                    curation.dismissMenu()
                    scope.launch { repository.dismissFavorite(shownFavorite.favorite.appId) }
                },
            )
        } else {
            FavoriteMenu(
                shownFavorite = shownFavorite,
                onDismiss = curation::dismissMenu,
                onRename = curation::renameCurated,
                onUnpin = {
                    curation.dismissMenu()
                    scope.launch { repository.dismissFavorite(shownFavorite.favorite.appId) }
                },
                onShowAppInfo = {
                    curation.dismissMenu()
                    repository.showAppInfo(shownFavorite.favorite.appId)
                },
                onUninstall = {
                    curation.dismissMenu()
                    if (!repository.requestUninstall(shownFavorite.favorite.appId)) {
                        uninstallFailed = true
                    }
                },
            )
        }
    }

    home.renaming?.let { shownFavorite ->
        RenameDialog(
            appLabel = shownFavorite.appLabel,
            currentLabel = shownFavorite.label,
            onDismiss = curation::dismissRename,
            onRename = { name ->
                curation.dismissRename()
                scope.launch { repository.chooseFavorite(shownFavorite.favorite.renamedTo(name)) }
            },
        )
    }

    if (uninstallFailed) {
        Notice(
            message = stringResource(R.string.uninstall_unavailable),
            onDismiss = { uninstallFailed = false },
        )
    }
}

/** An unavailable Favorite, shown in place but offering only dismissal. */
@Composable
private fun TombstoneRow(
    shownFavorite: ShownFavorite,
    textAlign: TextAlign,
    onDismiss: () -> Unit,
) {
    BasicText(
        text = shownFavorite.label,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(FavoriteRowHeight)
                .wrapContentHeight(Alignment.CenterVertically)
                .onTapLongPressOrDrag(
                    key = shownFavorite.favorite.appId,
                    onTap = {},
                    onLongPress = onDismiss,
                    onDrag = {},
                    onDragEnd = {},
                ),
        style =
            themedAppTextStyle(
                color = LocalTheme.current.secondaryTextColor,
                textAlign = textAlign,
            ),
    )
}

/** One Favorite on Home: tap to launch it, long-press to curate it, drag to move it. */
@Composable
private fun FavoriteRow(
    shownFavorite: ShownFavorite,
    textAlign: TextAlign,
    rowArrangement: Arrangement.Horizontal,
    iconMode: IconMode,
    onLaunch: () -> Unit,
    onCurate: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(FavoriteRowHeight)
                .onTapLongPressOrDrag(
                    key = shownFavorite.favorite.appId,
                    onTap = onLaunch,
                    onLongPress = onCurate,
                    onDrag = onDrag,
                    onDragEnd = onDragEnd,
                ),
        horizontalArrangement = rowArrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LauncherAppIcon(shownFavorite.app?.icon, iconMode)
        BasicText(
            text = shownFavorite.label,
            // Shrinkable but not filling, so the arrangement can centre icon and label together.
            modifier = Modifier.weight(1f, fill = false),
            style = themedAppTextStyle(color = LocalTheme.current.textColor, textAlign = textAlign),
        )
    }
}

/** Rename, unpin, app info and uninstall, for the Favorite the user is long-pressing. */
@Composable
private fun FavoriteMenu(
    shownFavorite: ShownFavorite,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onUnpin: () -> Unit,
    onShowAppInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    AppMenu(title = shownFavorite.label, onDismiss = onDismiss) {
        MenuAction(label = stringResource(R.string.favorite_rename), onClick = onRename)
        MenuAction(label = stringResource(R.string.favorite_unpin), onClick = onUnpin)
        MenuAction(label = stringResource(R.string.app_info), onClick = onShowAppInfo)
        MenuAction(label = stringResource(R.string.app_uninstall), onClick = onUninstall)
    }
}

/** The single action available for a Tombstone. */
@Composable
private fun TombstoneMenu(
    shownFavorite: ShownFavorite,
    onDismiss: () -> Unit,
    onDismissTombstone: () -> Unit,
) {
    AppMenu(title = shownFavorite.label, onDismiss = onDismiss) {
        MenuAction(label = stringResource(R.string.tombstone_dismiss), onClick = onDismissTombstone)
    }
}

private fun HomeAlignment.asHorizontalAlignment() =
    when (this) {
        HomeAlignment.Left -> Alignment.Start
        HomeAlignment.Centred -> Alignment.CenterHorizontally
    }

/** Centred Home gives shown icons one column; without them, it centres each label. */
private fun HomeAlignment.asRowArrangement(iconsShown: Boolean): Arrangement.Horizontal =
    when (this) {
        HomeAlignment.Left -> Arrangement.spacedBy(LauncherIconGap, Alignment.Start)
        HomeAlignment.Centred ->
            Arrangement.spacedBy(
                LauncherIconGap,
                if (iconsShown) Alignment.Start else Alignment.CenterHorizontally,
            )
    }

private fun HomeAlignment.asTextAlign() =
    when (this) {
        HomeAlignment.Left -> TextAlign.Start
        HomeAlignment.Centred -> TextAlign.Center
    }

@Composable
private fun themedAppTextStyle(
    color: Color,
    textAlign: TextAlign,
): TextStyle {
    val theme = LocalTheme.current
    return TextStyle(
        color = color,
        fontFamily = theme.fontFamily,
        fontSize = theme.typeScale.app.size,
        fontWeight = theme.typeScale.app.weight,
        textAlign = textAlign,
        shadow = textHalo(color),
    )
}
