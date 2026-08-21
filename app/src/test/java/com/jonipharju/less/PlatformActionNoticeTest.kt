package com.jonipharju.less

import android.content.Context
import android.icu.text.DateFormat
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jonipharju.less.launcher.FakeLauncherRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Date
import java.util.Locale
import android.text.format.DateFormat as AndroidDateFormat

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class PlatformActionNoticeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `a clock action with no handler shows a dismissible Less message`() {
        val repository = FakeLauncherRepository().also { it.finishSetup() }
        compose.setContent { LessLauncher(repository, onOpenClock = { false }) }

        compose.onNodeWithText(displayedTime()).performClick()

        compose.onNodeWithText("No clock app answers on this phone.").assertExists()
        compose.onNodeWithText("Got it").performClick()
        compose.onNodeWithText("No clock app answers on this phone.").assertDoesNotExist()
    }

    @Test
    fun `a calendar action with no handler shows a dismissible Less message`() {
        val repository = FakeLauncherRepository().also { it.finishSetup() }
        compose.setContent { LessLauncher(repository, onOpenCalendar = { false }) }

        compose.onNodeWithText(displayedDate()).performClick()

        compose.onNodeWithText("No calendar app answers on this phone.").assertExists()
        compose.onNodeWithText("Got it").performClick()
        compose.onNodeWithText("No calendar app answers on this phone.").assertDoesNotExist()
    }

    private fun displayedTime(): String {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val skeleton = if (AndroidDateFormat.is24HourFormat(context)) "Hm" else "hm"
        return DateFormat.getInstanceForSkeleton(skeleton, Locale.getDefault()).format(Date())
    }

    private fun displayedDate(): String = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault()).format(Date())
}
