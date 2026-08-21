package com.jonipharju.less

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.jonipharju.less.launcher.FakeLauncherRepository
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xxhdpi")
class SettingsScreenshotTest {
    @Test
    fun settingsShowsTheActiveThemeMarker() {
        captureRoboImage {
            ThemedSurface(
                wallpaper = SurfaceWallpaper.Fixed(ImageBitmap.imageResource(NearBlackTheme.wallpaperAsset)),
                modifier = Modifier.requiredSize(360.dp, 800.dp),
                theme = NearBlackTheme,
            ) {
                Settings(repository = FakeLauncherRepository(), onClose = {})
            }
        }
    }
}
