package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = LuminaAccentPrimary,
    onPrimary = Color.White,
    primaryContainer = LuminaAccentSubtle,
    onPrimaryContainer = LuminaAccentDark,
    secondary = DeepSlate,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECE5D9),
    onSecondaryContainer = DeepSlate,
    tertiary = LuminaAccentLight,
    onTertiary = DeepSlate,
    background = PaperWhite,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF3EFE8),
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = BorderLight
)

private val DarkColorScheme = darkColorScheme(
    primary = LuminaAccentLight,
    onPrimary = VelvetDark,
    primaryContainer = VelvetCard,
    onPrimaryContainer = LuminaAccentLight,
    secondary = Color(0xFF90A4BE),
    onSecondary = VelvetDark,
    secondaryContainer = Color(0xFF222C3D),
    onSecondaryContainer = VelvetTextPrimary,
    tertiary = LuminaAccentPrimary,
    onTertiary = Color.White,
    background = VelvetDark,
    onBackground = VelvetTextPrimary,
    surface = VelvetSurface,
    onSurface = VelvetTextPrimary,
    surfaceVariant = VelvetCard,
    onSurfaceVariant = VelvetTextSecondary,
    outline = VelvetBorder,
    outlineVariant = Color(0xFF334155)
)

@Composable
fun LuminaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    LuminaTheme(darkTheme = darkTheme, content = content)
}
