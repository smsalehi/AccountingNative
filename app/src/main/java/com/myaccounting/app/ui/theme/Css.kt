package com.myaccounting.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * CSS-unit helpers so the native UI uses the SAME box model as the original (Tailwind) UI.
 *
 * - 1 rem  = 16dp × (font-scale % / 100)   (original: html font-size = 16px × fontScale/100, see applyFontScale in App.tsx)
 * - 1 CSS px = 1dp (WebView: device px / devicePixelRatio)
 * - a "1px" border is floor(density) device pixels wide (Chrome snaps borders down to whole device pixels)
 */
val LocalRemDp = compositionLocalOf { 16f }
val LocalIsDark = compositionLocalOf { true }

/** Tailwind spacing/size in rem: rem(1f) == p-4 == 1rem */
@Composable @ReadOnlyComposable
fun rem(r: Float): Dp = (r * LocalRemDp.current).dp

/** Tailwind spacing unit: tw(4) == 1rem (p-4, gap-4 …), tw(0.5) == p-0.5 */
@Composable @ReadOnlyComposable
fun tw(n: Float): Dp = rem(n * 0.25f)

@Composable @ReadOnlyComposable
fun remSp(r: Float): TextUnit = with(LocalDensity.current) { (r * LocalRemDp.current).dp.toSp() }

/** Width of a CSS `border` (1px): whole device pixels, like Chrome. */
@Composable @ReadOnlyComposable
fun cssBorder(): Dp = with(LocalDensity.current) { (Math.floor(density.toDouble()).toFloat().coerceAtLeast(1f) / density).dp }

@Composable @ReadOnlyComposable
fun dk(light: Color, dark: Color): Color = if (LocalIsDark.current) dark else light

fun Color.a(alpha: Float): Color = copy(alpha = alpha)

/** Vazirmatn vertical metrics: (ascent - descent) / 2 / unitsPerEm = (2100 - 1100) / 2 / 2048 */
const val VAZIR_HALF_LEADING_SHIFT = 0.2441f
