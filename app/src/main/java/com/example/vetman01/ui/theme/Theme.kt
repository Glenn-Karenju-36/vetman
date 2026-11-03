
package com.example.vetman01.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Vetman Minimal Clinical color palette.
 *
 * Clean white surfaces, soft green accents,
 * and dark text for readability.
 */
private val VetmanGreen = Color(0xFF176B54)
private val VetmanDarkGreen = Color(0xFF10513F)
private val VetmanLightGreen = Color(0xFFEAF6F0)

private val VetmanBackground = Color(0xFFF8FAF9)
private val VetmanWhite = Color(0xFFFFFFFF)
private val VetmanText = Color(0xFF182720)
private val VetmanMuted = Color(0xFF64736B)
private val VetmanBorder = Color(0xFFE2EAE5)

/**
 * Main light theme.
 * Matches the Minimal Clinical dashboard design.
 */
private val LightColorScheme = lightColorScheme(
    primary = VetmanGreen,
    onPrimary = VetmanWhite,

    primaryContainer = VetmanLightGreen,
    onPrimaryContainer = VetmanDarkGreen,

    secondary = VetmanDarkGreen,
    onSecondary = VetmanWhite,

    secondaryContainer = VetmanLightGreen,
    onSecondaryContainer = VetmanDarkGreen,

    tertiary = Color(0xFF668A7B),
    onTertiary = VetmanWhite,

    background = VetmanBackground,
    onBackground = VetmanText,

    surface = VetmanWhite,
    onSurface = VetmanText,

    surfaceVariant = VetmanLightGreen,
    onSurfaceVariant = VetmanMuted,

    outline = VetmanBorder,
    outlineVariant = VetmanBorder,

    error = Color(0xFFB3261E),
    onError = VetmanWhite
)

/**
 * Dark theme retained for future user preferences.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81D8B4),
    onPrimary = Color(0xFF003827),

    primaryContainer = Color(0xFF10513F),
    onPrimaryContainer = Color(0xFFB4F1D5),

    secondary = Color(0xFFADDCC5),
    onSecondary = Color(0xFF173B2C),

    background = Color(0xFF101916),
    onBackground = Color(0xFFE2EDE6),

    surface = Color(0xFF17221D),
    onSurface = Color(0xFFE2EDE6),

    surfaceVariant = Color(0xFF25392F),
    onSurfaceVariant = Color(0xFFB9CBC0),

    outline = Color(0xFF82998C)
)

/**
 * App-wide theme.
 *
 * Minimal Clinical uses light mode by default.
 * Dynamic Android wallpaper colors are disabled
 * to preserve Vetman's consistent branding.
 */
@Composable
fun Vetman01Theme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
