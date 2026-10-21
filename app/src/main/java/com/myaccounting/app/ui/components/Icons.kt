package com.myaccounting.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.myaccounting.app.ui.theme.rem

/** A lucide icon (same SVG geometry as lucide-react 0.546) drawn at a Tailwind size such as `w-4` = 1rem. */
@Composable
fun LucideIcon(@DrawableRes id: Int, sizeRem: Float, tint: Color, modifier: Modifier = Modifier) {
    Icon(painterResource(id), contentDescription = null, tint = tint, modifier = modifier.size(rem(sizeRem)))
}
