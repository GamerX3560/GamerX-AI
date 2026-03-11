package com.gamerx.ai.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GamerXDarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = DarkBackground,
    primaryContainer = CyanDark,
    onPrimaryContainer = CyanLight,
    secondary = CyanLight,
    onSecondary = DarkBackground,
    secondaryContainer = Color(0xFF1A3A7A),
    onSecondaryContainer = Color(0xFFD0E0FF),
    tertiary = CyanPrimary,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    outlineVariant = Color(0xFF1E2540),
    error = ErrorRed,
    onError = DarkBackground,
    inverseSurface = TextPrimary,
    inverseOnSurface = DarkBackground,
    inversePrimary = CyanDark,
    surfaceTint = CyanPrimary
)

@Composable
fun GamerXAITheme(
    content: @Composable () -> Unit
) {
    val colorScheme = GamerXDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GamerXTypography,
        shapes = GamerXShapes,
        content = content
    )
}
