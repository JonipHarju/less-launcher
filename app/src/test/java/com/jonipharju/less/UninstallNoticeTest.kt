package com.jonipharju.less

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jonipharju.less.launcher.FakeLauncherRepository
import com.jonipharju.less.launcher.launcherAppFixture
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class UninstallNoticeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `when the Drawer cannot ask to uninstall a dismissible message tells the user`() {
        val repository = FakeLauncherRepository()
        repository.install(launcherAppFixture(label = "Clock"))
        repository.uninstallsSucceed = false
        compose.setContent { Drawer(repository, onClose = {}, onOpenSettings = {}) }

        compose.onNodeWithText("Clock").performTouchInput { longClick() }
        compose.onNodeWithText("Uninstall").performClick()

        compose.onNodeWithText("Nothing on this phone can uninstall that app.").assertExists()
        compose.onNodeWithText("Got it").performClick()
        compose.onNodeWithText("Nothing on this phone can uninstall that app.").assertDoesNotExist()
    }
}
