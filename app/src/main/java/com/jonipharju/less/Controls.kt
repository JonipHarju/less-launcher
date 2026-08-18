package com.jonipharju.less

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
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
        style = TextStyle(color = Color.LightGray, fontSize = 22.sp),
    )
}
