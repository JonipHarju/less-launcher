package com.jonipharju.less

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Panels are the one place Less paints an opaque background: they sit over the Wallpaper as
 * their own surface, and the text on them has to stay readable whatever the Wallpaper is.
 */
private val PanelBackground = Color(0xF2101010)

/** The short list of actions a long press offers for one app. */
@Composable
internal fun AppMenu(
    title: String,
    onDismiss: () -> Unit,
    actions: @Composable ColumnScope.() -> Unit,
) {
    Panel(onDismiss = onDismiss) {
        BasicText(
            text = title,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            style = TextStyle(color = Color.Gray, fontSize = 16.sp),
        )
        actions()
    }
}

/** One line of an [AppMenu]. */
@Composable
internal fun MenuAction(
    label: String,
    onClick: () -> Unit,
) {
    BasicText(
        text = label,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 14.dp),
        style = TextStyle(color = Color.White, fontSize = 20.sp),
    )
}

/** A message the user reads and dismisses. It advises; nothing waits on the answer. */
@Composable
internal fun Notice(
    message: String,
    onDismiss: () -> Unit,
) {
    Panel(onDismiss = onDismiss) {
        BasicText(
            text = message,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            style = TextStyle(color = Color.White, fontSize = 18.sp),
        )
        MenuAction(label = stringResource(R.string.notice_dismiss), onClick = onDismiss)
    }
}

/**
 * Asks what a Favorite should be called on Home, starting from what it is called now. An
 * empty name is not a name: it hands the Favorite back its real app name.
 */
@Composable
internal fun RenameDialog(
    appLabel: String,
    currentLabel: String,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    var name by remember(currentLabel) { mutableStateOf(currentLabel) }
    val nameFocusRequester = remember { FocusRequester() }
    val nameDescription = stringResource(R.string.rename_name, appLabel)

    LaunchedEffect(nameFocusRequester) { nameFocusRequester.requestFocus() }

    Panel(onDismiss = onDismiss) {
        BasicText(
            text = stringResource(R.string.rename_title, appLabel),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            style = TextStyle(color = Color.Gray, fontSize = 16.sp),
        )
        BasicTextField(
            value = name,
            onValueChange = { name = it },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .focusRequester(nameFocusRequester)
                    .semantics { contentDescription = nameDescription },
            textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
            cursorBrush = SolidColor(Color.LightGray),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onRename(name) }),
        )
        MenuAction(label = stringResource(R.string.rename_save), onClick = { onRename(name) })
    }
}

/** The one shape every panel takes: a dismissible column over a background of its own. */
@Composable
private fun Panel(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(PanelBackground)
                    .padding(vertical = 12.dp),
            content = content,
        )
    }
}
