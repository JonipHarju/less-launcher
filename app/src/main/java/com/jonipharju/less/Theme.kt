package com.jonipharju.less

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.jonipharju.less.launcher.IconMode

/** The artist and work recorded for a Theme's Wallpaper. */
internal data class Credit(
    val artist: String,
    val title: String,
    val year: String,
    val collection: String,
    val source: String,
)

/** A Theme's authored gradient between the Wallpaper and all of its content. */
internal data class Scrim(
    val top: Color,
    val bottom: Color,
)

/** The size and weight a Theme gives one kind of text. */
internal data class ThemeTextStyle(
    val size: TextUnit,
    val weight: FontWeight,
)

/** The authored text scale for Home and the Drawer. */
internal data class ThemeTypeScale(
    val clock: ThemeTextStyle,
    val date: ThemeTextStyle,
    val app: ThemeTextStyle,
    val search: ThemeTextStyle,
)

internal enum class DrawerTreatment {
    Blurred,
    FlatTranslucent,
    Opaque,
}

/** Authored appearance data. A Theme cannot change layout or launcher behaviour. */
internal data class Theme(
    @DrawableRes val wallpaperAsset: Int,
    val credit: Credit,
    val scrim: Scrim,
    val fontFamily: FontFamily,
    val typeScale: ThemeTypeScale,
    val textColor: Color,
    val secondaryTextColor: Color,
    val accentColor: Color,
    val iconMode: IconMode,
    val drawerTreatment: DrawerTreatment,
)

internal val NearBlackTheme =
    Theme(
        wallpaperAsset = R.drawable.near_black_wallpaper,
        credit =
            Credit(
                artist = "Less",
                title = "Near Black",
                year = "2026",
                collection = "Generated Themes",
                source = "Generated for Less",
            ),
        // Every stop is at least 90% opaque black. Even a white Wallpaper therefore
        // reaches at most 10% luminance, leaving both text colours above WCAG AA contrast.
        scrim = Scrim(top = Color.Black.copy(alpha = 0.94f), bottom = Color.Black.copy(alpha = 0.90f)),
        fontFamily = FontFamily.SansSerif,
        typeScale =
            ThemeTypeScale(
                clock = ThemeTextStyle(48.sp, FontWeight.Light),
                date = ThemeTextStyle(18.sp, FontWeight.Normal),
                app = ThemeTextStyle(24.sp, FontWeight.Normal),
                search = ThemeTextStyle(18.sp, FontWeight.Normal),
            ),
        textColor = Color.White,
        secondaryTextColor = Color(0xFFE8E8E8),
        accentColor = Color(0xFFFFFFFF),
        iconMode = IconMode.Hidden,
        drawerTreatment = DrawerTreatment.FlatTranslucent,
    )

internal val LocalTheme = staticCompositionLocalOf { NearBlackTheme }

/** The Wallpaper supplied to a themed surface by Android or by a deterministic test. */
internal sealed interface SurfaceWallpaper {
    /** The system-owned Wallpaper already visible through Less's transparent window. */
    data object System : SurfaceWallpaper

    data class Fixed(
        val bitmap: ImageBitmap,
    ) : SurfaceWallpaper
}

/** Renders Theme content over the caller-provided Wallpaper. */
@Composable
internal fun ThemedSurface(
    wallpaper: SurfaceWallpaper,
    modifier: Modifier = Modifier,
    theme: Theme = NearBlackTheme,
    content: @Composable BoxScope.() -> Unit,
) {
    CompositionLocalProvider(LocalTheme provides theme) {
        Box(modifier = modifier.fillMaxSize()) {
            if (wallpaper is SurfaceWallpaper.Fixed) {
                Image(
                    bitmap = wallpaper.bitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(theme.scrim.top, theme.scrim.bottom))),
                content = content,
            )
        }
    }
}
