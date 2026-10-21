package com.myaccounting.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontFamily

private fun Typography.withFont(f: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = f), displayMedium = displayMedium.copy(fontFamily = f), displaySmall = displaySmall.copy(fontFamily = f),
    headlineLarge = headlineLarge.copy(fontFamily = f), headlineMedium = headlineMedium.copy(fontFamily = f), headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f), titleMedium = titleMedium.copy(fontFamily = f), titleSmall = titleSmall.copy(fontFamily = f),
    bodyLarge = bodyLarge.copy(fontFamily = f), bodyMedium = bodyMedium.copy(fontFamily = f), bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f), labelMedium = labelMedium.copy(fontFamily = f), labelSmall = labelSmall.copy(fontFamily = f),
)

private val LightColors = lightColorScheme(
    primary = Tw.Sky600, onPrimary = Tw.White, primaryContainer = Tw.Sky50, onPrimaryContainer = Tw.Sky600,
    background = Tw.Slate50, onBackground = Tw.Slate900, surface = Tw.White, onSurface = Tw.Slate900,
    surfaceVariant = Tw.Slate100, onSurfaceVariant = Tw.Slate500, outline = Tw.Slate200, error = Tw.Rose600,
)
private val DarkColors = darkColorScheme(
    primary = Tw.Sky400, onPrimary = Tw.Slate900, primaryContainer = Tw.Slate800, onPrimaryContainer = Tw.Sky400,
    background = Tw.Slate950, onBackground = Tw.Slate100, surface = Tw.Slate900, onSurface = Tw.Slate100,
    surfaceVariant = Tw.Slate800, onSurfaceVariant = Tw.Slate400, outline = Tw.Slate700, error = Tw.Rose400,
)

/** Material theme used only by the not-yet-ported Settings screen; the ported screens draw with [Tw] colours directly. */
@Composable
fun AppTheme(isDark: Boolean, fontScalePercent: Int, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalIsDark provides isDark, LocalRemDp provides 16f * fontScalePercent / 100f) {
        MaterialTheme(
            colorScheme = if (isDark) DarkColors else LightColors,
            typography = Typography().withFont(Vazirmatn),
            content = content,
        )
    }
}
