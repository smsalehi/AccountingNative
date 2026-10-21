package com.myaccounting.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.myaccounting.app.R
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.rem
import com.myaccounting.app.viewmodel.ToastKind
import com.myaccounting.app.viewmodel.ToastMessage

/**
 * Port of ToastModal.tsx: a pill at the bottom of the screen with an icon and the message.
 * Colours follow the web variants (success emerald / error rose / warning amber / info sky).
 */
@Composable
fun AppToast(message: ToastMessage, modifier: Modifier = Modifier) {
    val (bg, icon) = when (message.kind) {
        ToastKind.SUCCESS -> Tw.Emerald600 to R.drawable.ic_lucide_circle_check
        ToastKind.ERROR -> Tw.Rose600 to R.drawable.ic_lucide_circle_x
        ToastKind.WARNING -> Tw.Amber500 to R.drawable.ic_lucide_triangle_alert
        ToastKind.INFO -> Tw.Sky600 to R.drawable.ic_lucide_info
    }
    Row(
        modifier
            // shadow-lg shadow-emerald-600/30 etc.
            .cssShadow(TwShadow.lg(bg.a(0.3f)), rem(0.75f))
            .background(bg, RoundedCornerShape(rem(0.75f)))
            .padding(horizontal = rem(1f), vertical = rem(0.625f)),
        horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(icon, 1f, Color.White)
        TwText(
            message.text, 0.75f, 1f, Color.White,
            weight = FW.Bold,
            modifier = Modifier.widthIn(max = rem(20f)),
        )
    }
}
