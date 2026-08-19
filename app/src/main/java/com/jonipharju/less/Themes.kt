package com.jonipharju.less

import androidx.compose.ui.graphics.Color
import com.jonipharju.less.launcher.DefaultThemeId
import com.jonipharju.less.launcher.IconMode

internal val NearBlackTheme =
    Theme(
        id = DefaultThemeId,
        name = "Near Black",
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
        fontFamily = Manrope,
        typeScale = SharedTypeScale,
        textColor = Color.White,
        secondaryTextColor = Color(0xFFE8E8E8),
        accentColor = Color(0xFFFFFFFF),
        iconMode = IconMode.Off,
        drawerTreatment = DrawerTreatment.FlatTranslucent,
    )

internal val OffWhiteTheme =
    Theme(
        id = "off-white",
        name = "Off White",
        wallpaperAsset = R.drawable.off_white_wallpaper,
        credit =
            Credit(
                artist = "Less",
                title = "Off White",
                year = "2026",
                collection = "Generated Themes",
                source = "Generated for Less",
            ),
        // The inverse of Near Black: a high-alpha plaster so dark text stays
        // above WCAG AA even when the system Wallpaper is black.
        scrim = Scrim(top = Color(0xFFF4F2EE).copy(alpha = 0.94f), bottom = Color(0xFFEFECE6).copy(alpha = 0.90f)),
        fontFamily = Manrope,
        typeScale = SharedTypeScale,
        textColor = Color(0xFF1A1916),
        secondaryTextColor = Color(0xFF3F3C36),
        accentColor = Color(0xFF1A1916),
        iconMode = IconMode.Off,
        drawerTreatment = DrawerTreatment.FlatTranslucent,
    )

internal val ParasolTheme =
    Theme(
        id = "parasol",
        name = "Parasol",
        wallpaperAsset = R.drawable.parasol_wallpaper,
        credit =
            Credit(
                artist = "Claude Monet",
                title = "Woman with a Parasol - Madame Monet and Her Son",
                year = "1875",
                collection = "National Gallery of Art, Washington",
                source = "NGA Open Access (CC0)",
            ),
        // Monet's sky is the brightest field in the set; a high-key scrim keeps
        // the clock in the sky without asking the painting to provide contrast.
        scrim = Scrim(top = Color(0xFFF3F6FC).copy(alpha = 0.94f), bottom = Color(0xFFEEF2E8).copy(alpha = 0.90f)),
        fontFamily = Manrope,
        typeScale = SharedTypeScale,
        textColor = Color(0xFF1A2744),
        secondaryTextColor = Color(0xFF2F3F32),
        accentColor = Color(0xFF2E5A8A),
        iconMode = IconMode.Original,
        drawerTreatment = DrawerTreatment.Blurred,
    )

internal val CoubronTheme =
    Theme(
        id = "coubron",
        name = "Coubron",
        wallpaperAsset = R.drawable.coubron_wallpaper,
        credit =
            Credit(
                artist = "Jean-Baptiste-Camille Corot",
                title = "The Forest of Coubron",
                year = "1872",
                collection = "National Gallery of Art, Washington",
                source = "NGA Open Access (CC0)",
            ),
        // The forest is already a dark, warm key. The scrim deepens it so cream
        // text does not depend on a sunlit patch of path happening to sit behind it.
        scrim = Scrim(top = Color(0xFF1C1810).copy(alpha = 0.94f), bottom = Color(0xFF282014).copy(alpha = 0.90f)),
        fontFamily = Manrope,
        typeScale = SharedTypeScale,
        textColor = Color(0xFFF3E6C8),
        secondaryTextColor = Color(0xFFE0D0A8),
        accentColor = Color(0xFFC45C48),
        iconMode = IconMode.Tinted,
        drawerTreatment = DrawerTreatment.Opaque,
    )

internal val MountCorcoranTheme =
    Theme(
        id = "mount-corcoran",
        name = "Mount Corcoran",
        wallpaperAsset = R.drawable.mount_corcoran_wallpaper,
        credit =
            Credit(
                artist = "Albert Bierstadt",
                title = "Mount Corcoran",
                year = "c. 1876-1877",
                collection = "National Gallery of Art, Washington",
                source = "NGA Open Access (CC0)",
            ),
        // Home sits over the painting's middle, which is storm cloud and dark timber.
        // A cold slate scrim carries that key to the top, where the lit peak would
        // otherwise leave pale text with nothing behind it.
        scrim = Scrim(top = Color(0xFF141A22).copy(alpha = 0.94f), bottom = Color(0xFF1A2430).copy(alpha = 0.90f)),
        fontFamily = Manrope,
        typeScale = SharedTypeScale,
        textColor = Color(0xFFEAF0F5),
        secondaryTextColor = Color(0xFFC3D0DC),
        accentColor = Color(0xFF8FB8D8),
        iconMode = IconMode.Tinted,
        drawerTreatment = DrawerTreatment.Blurred,
    )

internal val DepartureTheme =
    Theme(
        id = "departure",
        name = "Departure",
        wallpaperAsset = R.drawable.departure_wallpaper,
        credit =
            Credit(
                artist = "Thomas Cole",
                title = "The Departure",
                year = "1837",
                collection = "National Gallery of Art, Washington",
                source = "NGA Open Access (CC0)",
            ),
        // Cole painted a spring morning. The scrim is that light held as parchment, so
        // bark-dark text keeps its contrast over the castle without cooling the picture.
        scrim = Scrim(top = Color(0xFFF0E7D6).copy(alpha = 0.94f), bottom = Color(0xFFE7DCC6).copy(alpha = 0.90f)),
        fontFamily = Manrope,
        typeScale = SharedTypeScale,
        textColor = Color(0xFF241E14),
        secondaryTextColor = Color(0xFF473B29),
        accentColor = Color(0xFF5C6B33),
        iconMode = IconMode.Original,
        drawerTreatment = DrawerTreatment.Opaque,
    )

internal val Themes: List<Theme> =
    listOf(
        NearBlackTheme,
        OffWhiteTheme,
        ParasolTheme,
        CoubronTheme,
        MountCorcoranTheme,
        DepartureTheme,
    )

internal fun themeById(id: String): Theme = Themes.firstOrNull { it.id == id } ?: NearBlackTheme
