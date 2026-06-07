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
    primary = Color(0xFF2196F3),        // Primary Blue
    secondary = Color(0xFFEAF4FF),      // Light Blue
    tertiary = Color(0xFF0B66C3),       // Vibrant Accent Blue
    background = Color(0xFFFFFFFF),     // White Background
    surface = Color(0xFFFFFFFF),        // White Surface
    surfaceVariant = Color(0xFFF0F6FF), // Soft Light Blue-White Gradient Base
    onPrimary = Color.White,
    onSecondary = Color(0xFF2196F3),
    onBackground = Color(0xFF101C33),   // Deep Navy Slate for text/contrast
    onSurface = Color(0xFF101C33),      // Dark Slate body text
    onSurfaceVariant = Color(0xFF2C3E5E),
    error = Color(0xFFD32F2F)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Avoid dark mode as the default design
    dynamicColor: Boolean = false, // Preserve brand-consistent premium colors
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
