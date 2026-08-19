package com.jonipharju.less

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jonipharju.less.launcher.LauncherApp
import com.jonipharju.less.launcher.LauncherAppId
import com.jonipharju.less.launcher.LauncherRepository
import com.jonipharju.less.launcher.SetupStep
import com.jonipharju.less.launcher.asFavorites
import com.jonipharju.less.launcher.next
import com.jonipharju.less.launcher.previous
import com.jonipharju.less.launcher.proposedFavorites
import com.jonipharju.less.launcher.proposedFirst
import kotlinx.coroutines.launch

/**
 * What a new user meets, in order: the look, then the request to hold the Home Role, then the
 * Favorites Home starts with. The Theme comes first deliberately — Less shows what it is before
 * it asks the user for anything.
 */
@Composable
internal fun Setup(
    repository: LauncherRepository,
    onApplyWallpaper: (Theme) -> Unit = {},
) {
    val settings by repository.settings.collectAsState()
    val installedApps by repository.installedApps.collectAsState()
    val step = settings.setupStep
    val scope = rememberCoroutineScope()
    val goTo: (SetupStep) -> Unit = { destination ->
        scope.launch { repository.updateSettings { it.copy(setupStep = destination) } }
    }

    // Asking the platform which app answers each Everyday Intent is a package-manager query per
    // intent, so it is redone when the installed apps change rather than on every recomposition.
    val (proposed, listed) =
        remember(installedApps) {
            val answers = installedApps.proposedFavorites(repository::appAnswering)
            answers to installedApps.proposedFirst(answers)
        }
    // Null until the user touches the picker, so that the proposal stands on its own and a device
    // still handing Less its app list cannot leave Setup holding a choice the user never made.
    var edited by remember { mutableStateOf<Set<LauncherAppId>?>(null) }
    val chosen = edited ?: proposed.map(LauncherApp::id).toSet()

    BackHandler(enabled = step.previous() != null) { step.previous()?.let(goTo) }

    when (step) {
        SetupStep.Theme ->
            SetupStepFrame(
                title = R.string.setup_theme_title,
                explanation = R.string.setup_theme_explanation,
                step = step,
                onGoTo = goTo,
                onFinish = null,
            ) {
                ThemePicker(repository = repository, onApplyWallpaper = onApplyWallpaper)
            }

        SetupStep.HomeRole ->
            SetupStepFrame(
                title = R.string.setup_home_role_title,
                explanation = R.string.setup_home_role_explanation,
                step = step,
                onGoTo = goTo,
                onFinish = null,
            ) {
                HomeRoleStep(repository)
            }

        SetupStep.Favorites ->
            SetupStepFrame(
                title = R.string.setup_favorites_title,
                explanation = R.string.setup_favorites_explanation,
                step = step,
                onGoTo = goTo,
                onFinish = {
                    scope.launch {
                        listed.asFavorites(chosen).forEach { repository.chooseFavorite(it) }
                        repository.updateSettings { it.copy(setupStep = SetupStep.Done) }
                    }
                },
            ) {
                FavoritesStep(
                    listed = listed,
                    chosen = chosen,
                    onToggle = { appId, put -> edited = if (put) chosen + appId else chosen - appId },
                )
            }

        // Setup is not drawn once it is done.
        SetupStep.Done -> Unit
    }
}

/**
 * The shape every step takes: what it is called, what it is for, the step itself, and the way
 * on. [onFinish] is the last step's way out of Setup; every other step simply moves to the next.
 */
@Composable
private fun SetupStepFrame(
    @StringRes title: Int,
    @StringRes explanation: Int,
    step: SetupStep,
    onGoTo: (SetupStep) -> Unit,
    onFinish: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopBar {
            BasicText(
                text = stringResource(title),
                modifier = Modifier.padding(horizontal = 12.dp),
                style = themedTextStyle(size = 28.sp),
            )
        }

        Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Explanation(stringResource(explanation))
            content()
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            step.previous()?.let { earlier ->
                TextControl(
                    label = stringResource(R.string.setup_back),
                    onClick = { onGoTo(earlier) },
                    color = LocalTheme.current.secondaryTextColor,
                )
            }
            // The way on stays at the end of the row whether or not there is a way back.
            Spacer(modifier = Modifier.weight(1f))
            TextControl(
                label = stringResource(if (onFinish != null) R.string.setup_finish else R.string.setup_continue),
                onClick = onFinish ?: { onGoTo(step.next()) },
            )
        }
    }
}

/** The one request Setup makes of the platform, and what it says once the platform granted it. */
@Composable
private fun HomeRoleStep(repository: LauncherRepository) {
    val holdsHomeRole by repository.holdsHomeRole.collectAsState()

    if (holdsHomeRole) {
        BasicText(
            text = stringResource(R.string.setup_home_role_held),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            style = themedTextStyle(size = 20.sp),
        )
        return
    }

    TextControl(
        label = stringResource(R.string.setup_home_role_request),
        onClick = repository::requestHomeRole,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * The apps Home starts with. The ones the device answered the Everyday Intents with come
 * pre-chosen, so a user who reads no further than the footer still lands on a Home that works.
 */
@Composable
private fun FavoritesStep(
    listed: List<LauncherApp>,
    chosen: Set<LauncherAppId>,
    onToggle: (LauncherAppId, Boolean) -> Unit,
) {
    if (listed.isEmpty()) {
        Explanation(stringResource(R.string.setup_favorites_empty))
        return
    }

    listed.forEach { app ->
        val isChosen = app.id in chosen
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .toggleable(value = isChosen, onValueChange = { put -> onToggle(app.id, put) })
                    .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = app.label,
                style =
                    themedTextStyle(
                        color = if (isChosen) LocalTheme.current.textColor else LocalTheme.current.secondaryTextColor,
                        size = 20.sp,
                    ),
            )
            BasicText(
                text = if (isChosen) "✓" else "",
                style = themedTextStyle(color = LocalTheme.current.accentColor, size = 20.sp),
            )
        }
    }
}

@Composable
private fun Explanation(text: String) {
    BasicText(
        text = text,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        style = themedTextStyle(color = LocalTheme.current.secondaryTextColor, size = 16.sp),
    )
}
