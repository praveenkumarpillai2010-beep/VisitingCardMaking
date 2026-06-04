package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD4AF37),       // Premium Gold
    secondary = Color(0xFF00FFCC),     // Neon Teal
    tertiary = Color(0xFFFF5E7E),      // Neon Rose
    background = Color(0xFF0A0C16),    // Luxury dark space-blue
    surface = Color(0xFF131722),       // Card surface dark
    surfaceVariant = Color(0xFF1E2433),// Elevated surfaces
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFECEFF1),
    error = Color(0xFFFF5252)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFB8860B),       // Dark Goldenrod
    secondary = Color(0xFF008080),     // Soft Teal
    tertiary = Color(0xFFC2185B),      // Rich Crimson
    background = Color(0xFFF9FAFC),    // Off-white slate
    surface = Color(0xFFFFFFFF),       // Clear white surface
    surfaceVariant = Color(0xFFECEFF1),// Elevated light surface
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A1C1E),  // Dark text
    onSurface = Color(0xFF1A1C1E),     // Dark text
    onSurfaceVariant = Color(0xFF37474F),
    error = Color(0xFFD32F2F)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Preserve brand-consistent premium colors
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
