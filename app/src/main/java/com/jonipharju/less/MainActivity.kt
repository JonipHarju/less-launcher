package com.jonipharju.less

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jonipharju.less.launcher.AndroidLauncherRepository
import com.jonipharju.less.launcher.LauncherRepository

/** Android opens this activity when Less is selected as the default Home app. */
class MainActivity : ComponentActivity() {
    private lateinit var launcherRepository: AndroidLauncherRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcherRepository = AndroidLauncherRepository(applicationContext)
        setContent {
            InstalledApps(launcherRepository)
        }
    }

    override fun onDestroy() {
        launcherRepository.close()
        super.onDestroy()
    }
}

/** The installed apps shown on Home until the Drawer becomes a separate surface. */
@Composable
internal fun InstalledApps(repository: LauncherRepository) {
    val installedApps by repository.installedApps.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 32.dp),
    ) {
        items(
            items = installedApps,
            key = { app -> app.id },
        ) { app ->
            BasicText(
                text = app.label,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { repository.launch(app) }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                style =
                    TextStyle(
                        color = Color.White,
                        fontSize = 24.sp,
                    ),
            )
        }
    }
}
