package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Elegant Dark Color Schemes
private val ElegantDarkDarkScheme = darkColorScheme(
    primary = ElegantDarkPrimaryDark,
    secondary = ElegantDarkSecondaryDark,
    tertiary = ElegantDarkTertiaryDark,
    background = ElegantDarkBackgroundDark,
    surface = ElegantDarkSurfaceDark,
    surfaceVariant = ElegantDarkSurfaceVariantDark,
    onPrimary = ElegantDarkOnPrimaryDark,
    onSecondary = ElegantDarkOnSecondaryDark,
    onBackground = ElegantDarkOnBackgroundDark,
    onSurface = ElegantDarkOnSurfaceDark,
    onSurfaceVariant = ElegantDarkOnSurfaceVariantDark,
    primaryContainer = ElegantDarkPrimaryContainerDark,
    onPrimaryContainer = ElegantDarkOnPrimaryContainerDark,
    secondaryContainer = ElegantDarkSecondaryContainerDark,
    onSecondaryContainer = ElegantDarkOnSecondaryContainerDark
)

private val ElegantDarkLightScheme = lightColorScheme(
    primary = ElegantDarkPrimaryLight,
    secondary = ElegantDarkSecondaryLight,
    background = ElegantDarkBackgroundLight,
    surface = ElegantDarkSurfaceLight,
    surfaceVariant = ElegantDarkSurfaceVariantLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

// Cosmic Blue Color Schemes
private val CosmicBlueDarkScheme = darkColorScheme(
    primary = CosmicBluePrimaryDark,
    secondary = CosmicBlueSecondaryDark,
    background = CosmicBlueBackgroundDark,
    surface = CosmicBlueSurfaceDark,
    onPrimary = Color(0xFF003258),
    onSecondary = Color(0xFF003354),
    onBackground = Color(0xFFE2E2E6),
    onSurface = Color(0xFFE2E2E6)
)

private val CosmicBlueLightScheme = lightColorScheme(
    primary = CosmicBluePrimaryLight,
    secondary = CosmicBlueSecondaryLight,
    background = CosmicBlueBackgroundLight,
    surface = CosmicBlueSurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E)
)

// Mystic Purple Color Schemes
private val MysticPurpleDarkScheme = darkColorScheme(
    primary = MysticPurplePrimaryDark,
    secondary = MysticPurpleSecondaryDark,
    background = MysticPurpleBackgroundDark,
    surface = MysticPurpleSurfaceDark,
    onPrimary = Color(0xFF3B1D70),
    onSecondary = Color(0xFF332D41),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5)
)

private val MysticPurpleLightScheme = lightColorScheme(
    primary = MysticPurplePrimaryLight,
    secondary = MysticPurpleSecondaryLight,
    background = MysticPurpleBackgroundLight,
    surface = MysticPurpleSurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

// Sunset Orange Color Schemes
private val SunsetOrangeDarkScheme = darkColorScheme(
    primary = SunsetOrangePrimaryDark,
    secondary = SunsetOrangeSecondaryDark,
    background = SunsetOrangeBackgroundDark,
    surface = SunsetOrangeSurfaceDark,
    onPrimary = Color(0xFF5E1700),
    onSecondary = Color(0xFF442A22),
    onBackground = Color(0xFFEDE0DC),
    onSurface = Color(0xFFEDE0DC)
)

private val SunsetOrangeLightScheme = lightColorScheme(
    primary = SunsetOrangePrimaryLight,
    secondary = SunsetOrangeSecondaryLight,
    background = SunsetOrangeBackgroundLight,
    surface = SunsetOrangeSurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF201A18),
    onSurface = Color(0xFF201A18)
)

// Mint Green Color Schemes
private val MintGreenDarkScheme = darkColorScheme(
    primary = MintGreenPrimaryDark,
    secondary = MintGreenSecondaryDark,
    background = MintGreenBackgroundDark,
    surface = MintGreenSurfaceDark,
    onPrimary = Color(0xFF003827),
    onSecondary = Color(0xFF21352A),
    onBackground = Color(0xFFE1E3DF),
    onSurface = Color(0xFFE1E3DF)
)

private val MintGreenLightScheme = lightColorScheme(
    primary = MintGreenPrimaryLight,
    secondary = MintGreenSecondaryLight,
    background = MintGreenBackgroundLight,
    surface = MintGreenSurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF191C1A),
    onSurface = Color(0xFF191C1A)
)

@Composable
fun NovaStreamTheme(
    themeMode: String = "DARK", // "DARK", "LIGHT", "SYSTEM"
    themeColor: String = "ELEGANT_DARK", // "ELEGANT_DARK", "COSMIC_BLUE", "MYSTIC_PURPLE", "SUNSET_ORANGE", "MINT_GREEN"
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when (themeColor) {
        "ELEGANT_DARK" -> if (darkTheme) ElegantDarkDarkScheme else ElegantDarkLightScheme
        "COSMIC_BLUE" -> if (darkTheme) CosmicBlueDarkScheme else CosmicBlueLightScheme
        "MYSTIC_PURPLE" -> if (darkTheme) MysticPurpleDarkScheme else MysticPurpleLightScheme
        "SUNSET_ORANGE" -> if (darkTheme) SunsetOrangeDarkScheme else SunsetOrangeLightScheme
        "MINT_GREEN" -> if (darkTheme) MintGreenDarkScheme else MintGreenLightScheme
        else -> if (darkTheme) ElegantDarkDarkScheme else ElegantDarkLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
