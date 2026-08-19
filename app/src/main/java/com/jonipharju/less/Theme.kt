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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
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
) {
    /** Home keeps the Wallpaper visible; Drawer and Settings use the full stops. */
    fun forHome(): Scrim =
        Scrim(
            top = top.copy(alpha = 0.36f),
            bottom = bottom.copy(alpha = 0.28f),
        )
}

/** A soft halo so Home text stays readable over a busy Wallpaper. */
internal fun textHalo(text: Color): Shadow {
    val glow = if (text.luminance() > 0.5f) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.6f)
    return Shadow(color = glow, offset = Offset.Zero, blurRadius = 16f)
}

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
    val id: String,
    val name: String,
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

@OptIn(ExperimentalTextApi::class)
internal val Manrope =
    FontFamily(
        Font(
            R.font.manrope,
            FontWeight.Normal,
            variationSettings = FontVariation.Settings(FontVariation.weight(400)),
        ),
        Font(
            R.font.manrope,
            FontWeight.Medium,
            variationSettings = FontVariation.Settings(FontVariation.weight(500)),
        ),
    )

internal val SharedTypeScale =
    ThemeTypeScale(
        clock = ThemeTextStyle(48.sp, FontWeight.Medium),
        date = ThemeTextStyle(18.sp, FontWeight.Normal),
        app = ThemeTextStyle(24.sp, FontWeight.Medium),
        search = ThemeTextStyle(18.sp, FontWeight.Normal),
    )

internal val LocalTheme = staticCompositionLocalOf { NearBlackTheme }

/** The global choice wins; otherwise the active Theme's authored Icon Mode is used. */
internal fun effectiveIconMode(
    theme: Theme,
    override: IconMode?,
): IconMode = override ?: theme.iconMode

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
    scrim: Scrim? = theme.scrim,
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
                        .then(
                            if (scrim != null) {
                                Modifier.background(Brush.verticalGradient(listOf(scrim.top, scrim.bottom)))
                            } else {
                                Modifier
                            },
                        ),
                content = content,
            )
        }
    }
}
