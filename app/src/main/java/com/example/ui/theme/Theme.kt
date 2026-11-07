package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppColorTheme(
    val id: String,
    val titleBn: String,
    val subtitle: String,
    val previewPrimary: Color,
    val previewSecondary: Color,
    val previewTertiary: Color
) {
    CYBER_EMERALD(
        id = "cyber_emerald",
        titleBn = "সাইবার নিয়ন (Cyber Tactical)",
        subtitle = "এমারেল্ড গ্রিন ও ইলেকট্রিক সায়ান (HUD)",
        previewPrimary = Color(0xFF00E676),
        previewSecondary = Color(0xFF00E5FF),
        previewTertiary = Color(0xFFFFD600)
    ),
    ROYAL_BLUE(
        id = "royal_blue",
        titleBn = "রয়্যাল স্যাফায়ার ও গোল্ড",
        subtitle = "রয়্যাল ব্লু, ব্রাইট গোল্ড ও ক্রাইমসন",
        previewPrimary = Color(0xFF448AFF),
        previewSecondary = Color(0xFFFFD600),
        previewTertiary = Color(0xFFFF4081)
    ),
    SUNSET_VIOLET(
        id = "sunset_violet",
        titleBn = "সানসেট ভায়োলেট ও অরেঞ্জ",
        subtitle = "ইলেকট্রিক ভায়োলেট ও ভাইব্রেন্ট অরেঞ্জ",
        previewPrimary = Color(0xFFB388FF),
        previewSecondary = Color(0xFFFF9100),
        previewTertiary = Color(0xFFFF4081)
    ),
    MILITARY_CAMO(
        id = "military_camo",
        titleBn = "ট্যাকটিক্যাল ক্যামো ও ব্রাস",
        subtitle = "মিলিটারি অলিভ গ্রিন ও ডেজার্ট ব্রাস",
        previewPrimary = Color(0xFF66BB6A),
        previewSecondary = Color(0xFFFFB300),
        previewTertiary = Color(0xFFA1887F)
    ),
    NEON_CRIMSON(
        id = "neon_crimson",
        titleBn = "হাই-কনট্রাস্ট ক্রাইমসন (AMOLED)",
        subtitle = "লেজার রেড, নিয়ন সায়ান ও পিচ ব্ল্যাক",
        previewPrimary = Color(0xFFFF1744),
        previewSecondary = Color(0xFF00E5FF),
        previewTertiary = Color(0xFFFFD600)
    )
}

enum class DarkThemeOption(val titleBn: String) {
    DARK("ডার্ক মোড (Vibrant Tactical)"),
    LIGHT("লাইট মোড (Bright & Clean)"),
    SYSTEM("সিস্টেম ডিফল্ট (Auto)")
}

// 1. Cyber Emerald
private val CyberEmeraldDarkScheme = darkColorScheme(
    primary = CyberEmeraldPrimaryDark,
    onPrimary = CyberEmeraldOnPrimaryDark,
    primaryContainer = CyberEmeraldPrimaryContainerDark,
    onPrimaryContainer = CyberEmeraldOnPrimaryContainerDark,
    secondary = CyberCyanSecondaryDark,
    onSecondary = CyberCyanOnSecondaryDark,
    secondaryContainer = CyberCyanSecondaryContainerDark,
    onSecondaryContainer = CyberCyanOnSecondaryContainerDark,
    tertiary = CyberAmberTertiaryDark,
    onTertiary = CyberAmberOnTertiaryDark,
    tertiaryContainer = CyberAmberTertiaryContainerDark,
    onTertiaryContainer = CyberAmberOnTertiaryContainerDark,
    background = CyberDarkBackground,
    onBackground = CyberDarkOnSurface,
    surface = CyberDarkSurface,
    onSurface = CyberDarkOnSurface,
    surfaceVariant = CyberDarkSurfaceVariant,
    onSurfaceVariant = CyberDarkOnSurfaceVariant,
    outline = Color(0xFF2E4B3D),
    outlineVariant = Color(0xFF1D3328)
)

private val CyberEmeraldLightScheme = lightColorScheme(
    primary = CyberEmeraldPrimaryLight,
    onPrimary = CyberEmeraldOnPrimaryLight,
    primaryContainer = CyberEmeraldPrimaryContainerLight,
    onPrimaryContainer = CyberEmeraldOnPrimaryContainerLight,
    secondary = CyberCyanSecondaryLight,
    onSecondary = CyberCyanOnSecondaryLight,
    secondaryContainer = CyberCyanSecondaryContainerLight,
    onSecondaryContainer = CyberCyanOnSecondaryContainerLight,
    tertiary = CyberAmberTertiaryLight,
    onTertiary = CyberAmberOnTertiaryLight,
    tertiaryContainer = CyberAmberTertiaryContainerLight,
    onTertiaryContainer = CyberAmberOnTertiaryContainerLight,
    background = CyberLightBackground,
    onBackground = CyberLightOnSurface,
    surface = CyberLightSurface,
    onSurface = CyberLightOnSurface,
    surfaceVariant = CyberLightSurfaceVariant,
    onSurfaceVariant = CyberLightOnSurfaceVariant,
    outline = Color(0xFF7FA892),
    outlineVariant = Color(0xFFB7D4C3)
)

// 2. Royal Blue
private val RoyalBlueDarkScheme = darkColorScheme(
    primary = RoyalBluePrimaryDark,
    onPrimary = RoyalBlueOnPrimaryDark,
    primaryContainer = RoyalBluePrimaryContainerDark,
    onPrimaryContainer = RoyalBlueOnPrimaryContainerDark,
    secondary = RoyalBlueSecondaryDark,
    onSecondary = RoyalBlueOnSecondaryDark,
    secondaryContainer = RoyalBlueSecondaryContainerDark,
    onSecondaryContainer = RoyalBlueOnSecondaryContainerDark,
    tertiary = RoyalBlueTertiaryDark,
    onTertiary = RoyalBlueOnTertiaryDark,
    tertiaryContainer = RoyalBlueTertiaryContainerDark,
    onTertiaryContainer = RoyalBlueOnTertiaryContainerDark,
    background = RoyalBlueDarkBackground,
    onBackground = RoyalBlueDarkOnSurface,
    surface = RoyalBlueDarkSurface,
    onSurface = RoyalBlueDarkOnSurface,
    surfaceVariant = RoyalBlueDarkSurfaceVariant,
    onSurfaceVariant = RoyalBlueDarkOnSurfaceVariant,
    outline = Color(0xFF28446B),
    outlineVariant = Color(0xFF1A2E4B)
)

private val RoyalBlueLightScheme = lightColorScheme(
    primary = RoyalBluePrimaryLight,
    onPrimary = RoyalBlueOnPrimaryLight,
    primaryContainer = RoyalBluePrimaryContainerLight,
    onPrimaryContainer = RoyalBlueOnPrimaryContainerLight,
    secondary = RoyalBlueSecondaryLight,
    onSecondary = RoyalBlueOnSecondaryLight,
    secondaryContainer = RoyalBlueSecondaryContainerLight,
    onSecondaryContainer = RoyalBlueOnSecondaryContainerLight,
    tertiary = RoyalBlueTertiaryLight,
    onTertiary = RoyalBlueOnTertiaryLight,
    tertiaryContainer = RoyalBlueTertiaryContainerLight,
    onTertiaryContainer = RoyalBlueOnTertiaryContainerLight,
    background = RoyalBlueLightBackground,
    onBackground = RoyalBlueLightOnSurface,
    surface = RoyalBlueLightSurface,
    onSurface = RoyalBlueLightOnSurface,
    surfaceVariant = RoyalBlueLightSurfaceVariant,
    onSurfaceVariant = RoyalBlueLightOnSurfaceVariant,
    outline = Color(0xFF829AB8),
    outlineVariant = Color(0xFFC4D5EB)
)

// 3. Sunset Violet
private val SunsetVioletDarkScheme = darkColorScheme(
    primary = SunsetVioletPrimaryDark,
    onPrimary = SunsetVioletOnPrimaryDark,
    primaryContainer = SunsetVioletPrimaryContainerDark,
    onPrimaryContainer = SunsetVioletOnPrimaryContainerDark,
    secondary = SunsetVioletSecondaryDark,
    onSecondary = SunsetVioletOnSecondaryDark,
    secondaryContainer = SunsetVioletSecondaryContainerDark,
    onSecondaryContainer = SunsetVioletOnSecondaryContainerDark,
    tertiary = SunsetVioletTertiaryDark,
    onTertiary = SunsetVioletOnTertiaryDark,
    tertiaryContainer = SunsetVioletTertiaryContainerDark,
    onTertiaryContainer = SunsetVioletOnTertiaryContainerDark,
    background = SunsetVioletDarkBackground,
    onBackground = SunsetVioletDarkOnSurface,
    surface = SunsetVioletDarkSurface,
    onSurface = SunsetVioletDarkOnSurface,
    surfaceVariant = SunsetVioletDarkSurfaceVariant,
    onSurfaceVariant = SunsetVioletDarkOnSurfaceVariant,
    outline = Color(0xFF4C3364),
    outlineVariant = Color(0xFF332046)
)

private val SunsetVioletLightScheme = lightColorScheme(
    primary = SunsetVioletPrimaryLight,
    onPrimary = SunsetVioletOnPrimaryLight,
    primaryContainer = SunsetVioletPrimaryContainerLight,
    onPrimaryContainer = SunsetVioletOnPrimaryContainerLight,
    secondary = SunsetVioletSecondaryLight,
    onSecondary = SunsetVioletOnSecondaryLight,
    secondaryContainer = SunsetVioletSecondaryContainerLight,
    onSecondaryContainer = SunsetVioletOnSecondaryContainerLight,
    tertiary = SunsetVioletTertiaryLight,
    onTertiary = SunsetVioletOnTertiaryLight,
    tertiaryContainer = SunsetVioletTertiaryContainerLight,
    onTertiaryContainer = SunsetVioletOnTertiaryContainerLight,
    background = SunsetVioletLightBackground,
    onBackground = SunsetVioletLightOnSurface,
    surface = SunsetVioletLightSurface,
    onSurface = SunsetVioletLightOnSurface,
    surfaceVariant = SunsetVioletLightSurfaceVariant,
    onSurfaceVariant = SunsetVioletLightOnSurfaceVariant,
    outline = Color(0xFFA185B8),
    outlineVariant = Color(0xFFDCBFEF)
)

// 4. Camo
private val CamoDarkScheme = darkColorScheme(
    primary = CamoPrimaryDark,
    onPrimary = CamoOnPrimaryDark,
    primaryContainer = CamoPrimaryContainerDark,
    onPrimaryContainer = CamoOnPrimaryContainerDark,
    secondary = CamoSecondaryDark,
    onSecondary = CamoOnSecondaryDark,
    secondaryContainer = CamoSecondaryContainerDark,
    onSecondaryContainer = CamoOnSecondaryContainerDark,
    tertiary = CamoTertiaryDark,
    onTertiary = CamoOnTertiaryDark,
    tertiaryContainer = CamoTertiaryContainerDark,
    onTertiaryContainer = CamoOnTertiaryContainerDark,
    background = CamoDarkBackground,
    onBackground = CamoDarkOnSurface,
    surface = CamoDarkSurface,
    onSurface = CamoDarkOnSurface,
    surfaceVariant = CamoDarkSurfaceVariant,
    onSurfaceVariant = CamoDarkOnSurfaceVariant,
    outline = Color(0xFF3B4E3B),
    outlineVariant = Color(0xFF263326)
)

private val CamoLightScheme = lightColorScheme(
    primary = CamoPrimaryLight,
    onPrimary = CamoOnPrimaryLight,
    primaryContainer = CamoPrimaryContainerLight,
    onPrimaryContainer = CamoOnPrimaryContainerLight,
    secondary = CamoSecondaryLight,
    onSecondary = CamoOnSecondaryLight,
    secondaryContainer = CamoSecondaryContainerLight,
    onSecondaryContainer = CamoOnSecondaryContainerLight,
    tertiary = CamoTertiaryLight,
    onTertiary = CamoOnTertiaryLight,
    tertiaryContainer = CamoTertiaryContainerLight,
    onTertiaryContainer = CamoOnTertiaryContainerLight,
    background = CamoLightBackground,
    onBackground = CamoLightOnSurface,
    surface = CamoLightSurface,
    onSurface = CamoLightOnSurface,
    surfaceVariant = CamoLightSurfaceVariant,
    onSurfaceVariant = CamoLightOnSurfaceVariant,
    outline = Color(0xFF869E86),
    outlineVariant = Color(0xFFC8DEC8)
)

// 5. Neon Crimson
private val CrimsonDarkScheme = darkColorScheme(
    primary = CrimsonPrimaryDark,
    onPrimary = CrimsonOnPrimaryDark,
    primaryContainer = CrimsonPrimaryContainerDark,
    onPrimaryContainer = CrimsonOnPrimaryContainerDark,
    secondary = CrimsonSecondaryDark,
    onSecondary = CrimsonOnSecondaryDark,
    secondaryContainer = CrimsonSecondaryContainerDark,
    onSecondaryContainer = CrimsonOnSecondaryContainerDark,
    tertiary = CrimsonTertiaryDark,
    onTertiary = CrimsonOnTertiaryDark,
    tertiaryContainer = CrimsonTertiaryContainerDark,
    onTertiaryContainer = CrimsonOnTertiaryContainerDark,
    background = CrimsonDarkBackground,
    onBackground = CrimsonDarkOnSurface,
    surface = CrimsonDarkSurface,
    onSurface = CrimsonDarkOnSurface,
    surfaceVariant = CrimsonDarkSurfaceVariant,
    onSurfaceVariant = CrimsonDarkOnSurfaceVariant,
    outline = Color(0xFF522828),
    outlineVariant = Color(0xFF381B1B)
)

private val CrimsonLightScheme = lightColorScheme(
    primary = CrimsonPrimaryLight,
    onPrimary = CrimsonOnPrimaryLight,
    primaryContainer = CrimsonPrimaryContainerLight,
    onPrimaryContainer = CrimsonOnPrimaryContainerLight,
    secondary = CrimsonSecondaryLight,
    onSecondary = CrimsonSecondaryLight,
    secondaryContainer = CrimsonSecondaryContainerLight,
    onSecondaryContainer = CrimsonOnSecondaryContainerLight,
    tertiary = CrimsonTertiaryLight,
    onTertiary = CrimsonOnTertiaryLight,
    tertiaryContainer = CrimsonTertiaryContainerLight,
    onTertiaryContainer = CrimsonOnTertiaryContainerLight,
    background = CrimsonLightBackground,
    onBackground = CrimsonLightOnSurface,
    surface = CrimsonLightSurface,
    onSurface = CrimsonLightOnSurface,
    surfaceVariant = CrimsonLightSurfaceVariant,
    onSurfaceVariant = CrimsonLightOnSurfaceVariant,
    outline = Color(0xFFB57D7D),
    outlineVariant = Color(0xFFEDD2D2)
)

fun getColorSchemeForTheme(
    theme: AppColorTheme,
    isDark: Boolean
): ColorScheme {
    return when (theme) {
        AppColorTheme.CYBER_EMERALD -> if (isDark) CyberEmeraldDarkScheme else CyberEmeraldLightScheme
        AppColorTheme.ROYAL_BLUE -> if (isDark) RoyalBlueDarkScheme else RoyalBlueLightScheme
        AppColorTheme.SUNSET_VIOLET -> if (isDark) SunsetVioletDarkScheme else SunsetVioletLightScheme
        AppColorTheme.MILITARY_CAMO -> if (isDark) CamoDarkScheme else CamoLightScheme
        AppColorTheme.NEON_CRIMSON -> if (isDark) CrimsonDarkScheme else CrimsonLightScheme
    }
}

@Composable
fun MyApplicationTheme(
    colorTheme: AppColorTheme = AppColorTheme.CYBER_EMERALD,
    darkThemeOption: DarkThemeOption = DarkThemeOption.DARK,
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (darkThemeOption) {
        DarkThemeOption.DARK -> true
        DarkThemeOption.LIGHT -> false
        DarkThemeOption.SYSTEM -> isSystemDark
    }

    val colorScheme = getColorSchemeForTheme(colorTheme, isDark)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
