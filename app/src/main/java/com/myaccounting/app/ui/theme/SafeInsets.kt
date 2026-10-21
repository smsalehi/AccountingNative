package com.myaccounting.app.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The activity window's `safeDrawing` insets as plain [Dp] values.
 *
 * A Compose `Dialog` lives in its own window, and that window reports every inset as zero
 * (`WindowInsets.safeDrawing` = 0/0/0/0, verified on the emulator): content composed inside a
 * dialog cannot see the system bars. Anything that must stay clear of the status/navigation bar
 * inside a dialog therefore reads the values captured in the activity composition instead.
 */
data class SafeInsets(
    val top: Dp = 0.dp,
    val bottom: Dp = 0.dp,
    val start: Dp = 0.dp,
    val end: Dp = 0.dp,
)

val LocalSafeInsets = compositionLocalOf { SafeInsets() }

/** Horizontal padding, physical left/right (the bars are symmetric on phones). */
val SafeInsets.horizontal: Dp get() = start + end
