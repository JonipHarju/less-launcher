package com.jonipharju.less

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A single-character control — a close cross, a settings gear — named for the reader. */
@Composable
internal fun GlyphControl(
    glyph: String,
    description: String,
    onClick: () -> Unit,
) {
    BasicText(
        text = glyph,
        modifier =
            Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .semantics { contentDescription = description },
        style =
            TextStyle(
                color = LocalTheme.current.secondaryTextColor,
                fontSize = 22.sp,
            ),
    )
}

/** The strip along the top of a surface, holding its title and its controls at either end. */
@Composable
internal fun TopBar(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** The Theme's own type, at whatever size the surface asks for. */
@Composable
internal fun themedTextStyle(
    color: Color = LocalTheme.current.textColor,
    size: TextUnit,
): TextStyle {
    val theme = LocalTheme.current
    return TextStyle(color = color, fontFamily = theme.fontFamily, fontSize = size)
}

/**
 * A word or two the user taps to act — the way on through Setup, the standing offer in the
 * Drawer. It wears the Theme's accent so that what acts is told apart from what merely reads.
 */
@Composable
internal fun TextControl(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = LocalTheme.current.accentColor,
) {
    BasicText(
        text = label,
        modifier = modifier.clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp),
        style = themedTextStyle(color = color, size = 20.sp),
    )
}

/** The heading over one group of options. */
@Composable
internal fun GroupTitle(title: String) {
    BasicText(
        text = title,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
        style = themedTextStyle(color = LocalTheme.current.secondaryTextColor, size = 14.sp),
    )
}
