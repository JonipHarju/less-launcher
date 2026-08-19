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
        iconMode = IconMode.Hidden,
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
        iconMode = IconMode.Hidden,
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

internal val RuinedChurchTheme =
    Theme(
        id = "ruined-church",
        name = "Ruined Church",
        wallpaperAsset = R.drawable.ruined_church_wallpaper,
        credit =
            Credit(
                artist = "Carl Blechen",
                title = "A Ruined Church in the Forest",
                year = "c. 1834",
                collection = "National Gallery of Art, Washington",
                source = "NGA Open Access (CC0)",
            ),
        // Blechen's drawing lives on cream paper. The scrim is that paper, so
        // umber ink stays readable whether or not the user applied the wallpaper.
        scrim = Scrim(top = Color(0xFFE8DCC8).copy(alpha = 0.94f), bottom = Color(0xFFE4D4B8).copy(alpha = 0.90f)),
        fontFamily = Manrope,
        typeScale = SharedTypeScale,
        textColor = Color(0xFF2A2218),
        secondaryTextColor = Color(0xFF4A3C2C),
        accentColor = Color(0xFF3D2E22),
        iconMode = IconMode.Hidden,
        drawerTreatment = DrawerTreatment.FlatTranslucent,
    )

internal val SpringhouseTheme =
    Theme(
        id = "springhouse",
        name = "Springhouse",
        wallpaperAsset = R.drawable.springhouse_wallpaper,
        credit =
            Credit(
                artist = "William Russell Birch",
                title = "View from the Springhouse at Echo",
                year = "c. 1808",
                collection = "National Gallery of Art, Washington",
                source = "NGA Open Access (CC0)",
            ),
        // Birch's wash is already a pale sheet. The scrim holds that key so the
        // graphite-dark text never lands on a dark system Wallpaper unprotected.
        scrim = Scrim(top = Color(0xFFE6E0D4).copy(alpha = 0.94f), bottom = Color(0xFFDED6C8).copy(alpha = 0.90f)),
        fontFamily = Manrope,
        typeScale = SharedTypeScale,
        textColor = Color(0xFF2C2A24),
        secondaryTextColor = Color(0xFF4A463C),
        accentColor = Color(0xFF4A6740),
        iconMode = IconMode.Original,
        drawerTreatment = DrawerTreatment.Blurred,
    )

internal val Themes: List<Theme> =
    listOf(
        NearBlackTheme,
        OffWhiteTheme,
        ParasolTheme,
        CoubronTheme,
        RuinedChurchTheme,
        SpringhouseTheme,
    )

internal fun themeById(id: String): Theme = Themes.firstOrNull { it.id == id } ?: NearBlackTheme
