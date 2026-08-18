package com.jonipharju.less

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.jonipharju.less.launcher.FakeLauncherRepository
import com.jonipharju.less.launcher.Favorite
import com.jonipharju.less.launcher.launcherAppFixture
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xxhdpi")
class ThemeScreenshotTest {
    @Test
    fun homeUnderNearBlackTheme() = captureHome(NearBlackTheme)

    @Test
    fun drawerUnderNearBlackTheme() = captureDrawer(NearBlackTheme)

    @Test
    fun homeUnderOffWhiteTheme() = captureHome(OffWhiteTheme)

    @Test
    fun drawerUnderOffWhiteTheme() = captureDrawer(OffWhiteTheme)

    @Test
    fun homeUnderParasolTheme() = captureHome(ParasolTheme)

    @Test
    fun drawerUnderParasolTheme() = captureDrawer(ParasolTheme)

    @Test
    fun homeUnderCoubronTheme() = captureHome(CoubronTheme)

    @Test
    fun drawerUnderCoubronTheme() = captureDrawer(CoubronTheme)

    @Test
    fun homeUnderRuinedChurchTheme() = captureHome(RuinedChurchTheme)

    @Test
    fun drawerUnderRuinedChurchTheme() = captureDrawer(RuinedChurchTheme)

    @Test
    fun homeUnderSpringhouseTheme() = captureHome(SpringhouseTheme)

    @Test
    fun drawerUnderSpringhouseTheme() = captureDrawer(SpringhouseTheme)
}

private fun captureHome(theme: Theme) {
    val repository = repositoryWithApps()
    captureRoboImage {
        ThemedSurface(
            wallpaper = SurfaceWallpaper.Fixed(ImageBitmap.imageResource(theme.wallpaperAsset)),
            modifier = Modifier.requiredSize(360.dp, 800.dp),
            theme = theme,
        ) {
            Home(
                repository = repository,
                timeText = "14:35",
                dateText = "Tuesday, August 18, 2026",
                onOpenClock = {},
                onOpenCalendar = {},
                onOpenDrawer = {},
            )
        }
    }
}

private fun captureDrawer(theme: Theme) {
    val repository = repositoryWithApps()
    captureRoboImage {
        ThemedSurface(
            wallpaper = SurfaceWallpaper.Fixed(ImageBitmap.imageResource(theme.wallpaperAsset)),
            modifier = Modifier.requiredSize(360.dp, 800.dp),
            theme = theme,
        ) {
            Drawer(repository = repository, onClose = {}, onOpenSettings = {})
        }
    }
}

private fun repositoryWithApps() =
    FakeLauncherRepository().also { repository ->
        runBlocking {
            listOf("Calendar", "Camera", "Clock", "Maps").forEachIndexed { index, label ->
                val app = launcherAppFixture(label)
                repository.install(app)
                if (index < 3) repository.chooseFavorite(Favorite(app.id, index))
            }
        }
    }
