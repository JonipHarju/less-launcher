package com.jonipharju.less

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.jonipharju.less.launcher.AppIcon
import com.jonipharju.less.launcher.FakeLauncherRepository
import com.jonipharju.less.launcher.Favorite
import com.jonipharju.less.launcher.IconMode
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
    fun homeWithIconsOff() = captureHome(CoubronTheme, IconMode.Hidden)

    @Test
    fun homeWithTintedIcons() = captureHome(CoubronTheme, IconMode.Tinted)

    @Test
    fun homeWithNativeIcons() = captureHome(CoubronTheme, IconMode.Original)

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

private fun captureHome(
    theme: Theme,
    iconModeOverride: IconMode? = null,
) {
    val repository = repositoryWithApps()
    runBlocking { repository.updateSettings { it.copy(iconModeOverride = iconModeOverride) } }
    captureRoboImage {
        ThemedSurface(
            wallpaper = SurfaceWallpaper.Fixed(ImageBitmap.imageResource(theme.wallpaperAsset)),
            modifier = Modifier.requiredSize(360.dp, 800.dp),
            theme = theme,
            scrim = theme.scrim.forHome(),
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
                val app = launcherAppFixture(label).copy(icon = screenshotIcon(index))
                repository.install(app)
                if (index < 3) repository.chooseFavorite(Favorite(app.id, index))
            }
        }
    }

private fun screenshotIcon(index: Int): AppIcon {
    val native = ImageBitmap(48, 48)
    Canvas(native).drawCircle(
        center = Offset(24f, 24f),
        radius = 22f,
        paint = Paint().apply { color = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow)[index] },
    )
    val themeable = ImageBitmap(48, 48)
    Canvas(themeable).drawCircle(center = Offset(24f, 24f), radius = 18f, paint = Paint().apply { color = Color.Black })
    return AppIcon(native = native, themeable = themeable.takeIf { index % 2 == 0 })
}
