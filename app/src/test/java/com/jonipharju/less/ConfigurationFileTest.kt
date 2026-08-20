package com.jonipharju.less

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jonipharju.less.launcher.FakeLauncherRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class ConfigurationFileTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `Settings does not offer to write the Configuration to a file or to read one back`() {
        compose.setContent { Settings(FakeLauncherRepository(), onClose = {}) }

        compose.onNodeWithText("Configuration").assertDoesNotExist()
        compose.onNodeWithText("Export to a file").assertDoesNotExist()
        compose.onNodeWithText("Import from a file").assertDoesNotExist()
    }
}
