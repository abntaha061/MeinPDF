package com.mohammed.pdfreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ========== Colors ==========
val Blue600 = Color(0xFF1565C0)
val Blue400 = Color(0xFF42A5F5)
val Blue200 = Color(0xFF90CAF9)
val DarkBg = Color(0xFF0A0C12)
val DarkSurface = Color(0xFF111320)
val DarkCard = Color(0xFF161926)
val DarkBorder = Color(0xFF1E2340)
val AccentBlue = Color(0xFF3B82F6)
val AccentPurple = Color(0xFF8B5CF6)
val AccentCyan = Color(0xFF06B6D4)
val Gold = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF4444)
val SuccessGreen = Color(0xFF22C55E)
val TextPrimary = Color(0xFFE2E8F0)
val TextMuted = Color(0xFF64748B)
val HighlightYellow = Color(0xFFFBBF24)
val HighlightGreen = Color(0xFF4ADE80)
val HighlightBlue = Color(0xFF60A5FA)
val HighlightPink = Color(0xFFF472B6)

// Sepia
val SepiaBg = Color(0xFFF4ECD8)
val SepiaSurface = Color(0xFFEFE2C8)
val SepiaText = Color(0xFF3D2B1F)

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = Blue600,
    onPrimaryContainer = Blue200,
    secondary = AccentPurple,
    onSecondary = Color.White,
    tertiary = AccentCyan,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextMuted,
    error = ErrorRed,
    outline = DarkBorder,
    inverseSurface = TextPrimary,
    inverseOnSurface = DarkBg
)

private val LightColorScheme = lightColorScheme(
    primary = Blue600,
    onPrimary = Color.White,
    primaryContainer = Blue200,
    onPrimaryContainer = Blue600,
    secondary = AccentPurple,
    onSecondary = Color.White,
    tertiary = AccentCyan,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF1E293B),
    surface = Color.White,
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    error = ErrorRed,
    outline = Color(0xFFE2E8F0)
)

@Composable
fun PDFReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
