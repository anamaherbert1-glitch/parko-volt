package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = ParkoBlue,
    onPrimary = Color.White,
    primaryContainer = ParkoBlueLight,
    onPrimaryContainer = ParkoBlueDark,
    secondary = ParkoEmerald,
    onSecondary = Color.White,
    secondaryContainer = ParkoEmeraldLight,
    onSecondaryContainer = ParkoEmeraldDark,
    tertiary = ParkoAmber,
    onTertiary = Color.White,
    tertiaryContainer = ParkoAmberLight,
    onTertiaryContainer = ParkoAmberDark,
    error = ParkoRed,
    onError = Color.White,
    errorContainer = ParkoRedLight,
    onErrorContainer = ParkoRedDark,
    background = ParkoBackground,
    onBackground = ParkoCharcoal,
    surface = ParkoSurface,
    onSurface = ParkoCharcoal,
    surfaceVariant = ParkoSurfaceVariant,
    onSurfaceVariant = ParkoSlate,
    outline = ParkoOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = ParkoBlueMid,
    onPrimary = Color.White,
    primaryContainer = ParkoBlueDark,
    onPrimaryContainer = ParkoBlueSoft,
    secondary = ParkoEmerald,
    onSecondary = ParkoCharcoal,
    secondaryContainer = ParkoEmeraldDark,
    onSecondaryContainer = ParkoEmeraldLight,
    tertiary = ParkoAmber,
    onTertiary = ParkoCharcoal,
    tertiaryContainer = ParkoAmberDark,
    onTertiaryContainer = ParkoAmberLight,
    error = ParkoRed,
    onError = Color.White,
    errorContainer = ParkoRedDark,
    onErrorContainer = ParkoRedLight,
    background = ParkoDarkBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = ParkoDarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = ParkoDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155)
)

val ParkoShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ParkoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ParkoTypography,
        shapes = ParkoShapes,
        content = content
    )
}
