package com.jonipharju.less

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
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
import com.jonipharju.less.launcher.answering
import com.jonipharju.less.launcher.asFavorites
import com.jonipharju.less.launcher.next
import com.jonipharju.less.launcher.previous
import com.jonipharju.less.launcher.proposedFirst
import kotlinx.coroutines.launch

/**
 * What a new user meets, in order: the look, then the request to become their launcher, then
 * the Favorites Home starts with. The Theme comes first deliberately — Less shows what it is
 * before it asks the user for anything.
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

    // Asking the platform which app answers each everyday intent is a package-manager query per
    // intent, so it is redone when the installed apps change rather than on every recomposition.
    val (proposed, listed) =
        remember(installedApps) {
            val answers = installedApps.answering(repository::appAnswering)
            answers to installedApps.proposedFirst(answers)
        }
    // Null until there is an app list to propose from, so that a device still handing Less its
    // apps does not leave Setup holding an empty choice the user never made.
    var chosen by remember { mutableStateOf<Set<LauncherAppId>?>(null) }

    LaunchedEffect(listed) {
        if (chosen == null && listed.isNotEmpty()) chosen = proposed.map(LauncherApp::id).toSet()
    }

    BackHandler(enabled = step.previous() != null) { step.previous()?.let(goTo) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar {
            BasicText(
                text = stringResource(step.titleResource()),
                modifier = Modifier.padding(horizontal = 12.dp),
                style = themedTextStyle(size = 28.sp),
            )
        }

        Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            Explanation(stringResource(step.explanationResource()))

            when (step) {
                SetupStep.Theme -> ThemePicker(repository = repository, onApplyWallpaper = onApplyWallpaper)
                SetupStep.DefaultLauncher -> DefaultLauncherStep(repository)
                SetupStep.Favorites ->
                    FavoritesStep(
                        listed = listed,
                        chosen = chosen.orEmpty(),
                        onToggle = { appId, put ->
                            chosen = if (put) chosen.orEmpty() + appId else chosen.orEmpty() - appId
                        },
                    )
                // Setup is not drawn once it is done.
                SetupStep.Done -> Unit
            }
        }

        SetupFooter(
            onBack = { step.previous()?.let(goTo) },
            canGoBack = step.previous() != null,
            finishes = step == SetupStep.Favorites,
            onContinue = {
                if (step == SetupStep.Favorites) {
                    scope.launch {
                        listed.asFavorites(chosen.orEmpty()).forEach { repository.chooseFavorite(it) }
                        repository.updateSettings { it.copy(setupStep = SetupStep.Done) }
                    }
                } else {
                    goTo(step.next())
                }
            },
        )
    }
}

/** The one request Setup makes of the platform, and what it says once the platform granted it. */
@Composable
private fun DefaultLauncherStep(repository: LauncherRepository) {
    val holdsHomeRole by repository.holdsHomeRole.collectAsState()

    if (holdsHomeRole) {
        BasicText(
            text = stringResource(R.string.setup_default_launcher_held),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            style = themedTextStyle(size = 20.sp),
        )
        return
    }

    BasicText(
        text = stringResource(R.string.setup_default_launcher_request),
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = repository::requestHomeRole)
                .padding(horizontal = 24.dp, vertical = 14.dp),
        style = themedTextStyle(color = LocalTheme.current.accentColor, size = 20.sp),
    )
}

/**
 * The apps Home starts with. The everyday ones the device answered with come pre-chosen, so a
 * user who reads no further than the footer still lands on a Home that works.
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

/** The way on, and the way back to the step before it. */
@Composable
private fun SetupFooter(
    canGoBack: Boolean,
    finishes: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (canGoBack) {
            BasicText(
                text = stringResource(R.string.setup_back),
                modifier = Modifier.clickable(onClick = onBack).padding(horizontal = 12.dp, vertical = 8.dp),
                style = themedTextStyle(color = LocalTheme.current.secondaryTextColor, size = 20.sp),
            )
        }
        // The way on stays at the end of the row whether or not there is a way back.
        Spacer(modifier = Modifier.weight(1f))
        BasicText(
            text = stringResource(if (finishes) R.string.setup_finish else R.string.setup_continue),
            modifier = Modifier.clickable(onClick = onContinue).padding(horizontal = 12.dp, vertical = 8.dp),
            style = themedTextStyle(color = LocalTheme.current.accentColor, size = 20.sp),
        )
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

private fun SetupStep.titleResource() =
    when (this) {
        SetupStep.Theme -> R.string.setup_theme_title
        SetupStep.DefaultLauncher -> R.string.setup_default_launcher_title
        SetupStep.Favorites, SetupStep.Done -> R.string.setup_favorites_title
    }

private fun SetupStep.explanationResource() =
    when (this) {
        SetupStep.Theme -> R.string.setup_theme_explanation
        SetupStep.DefaultLauncher -> R.string.setup_default_launcher_explanation
        SetupStep.Favorites, SetupStep.Done -> R.string.setup_favorites_explanation
    }
