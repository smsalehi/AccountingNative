package com.myaccounting.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.myaccounting.app.R
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem

enum class ConfirmVariant { DANGER, WARNING, INFO }

/** Port of components/ConfirmModal.tsx. */
@Composable
fun ConfirmDialog(
    message: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    title: String? = null,
    confirmLabel: String = "تأیید",
    cancelLabel: String = "انصراف",
    variant: ConfirmVariant = ConfirmVariant.DANGER,
) {
    val (icon, iconBg, iconColor, confirmBg) = when (variant) {
        ConfirmVariant.DANGER -> Quad(
            R.drawable.ic_lucide_trash2,
            dk(Tw.Rose100, Tw.Rose950.a(0.6f)), dk(Tw.Rose600, Tw.Rose400), Tw.Rose600,
        )
        ConfirmVariant.WARNING -> Quad(
            R.drawable.ic_lucide_triangle_alert,
            dk(Tw.Amber100, Tw.Amber950.a(0.6f)), dk(Tw.Amber600, Tw.Amber400), Tw.Amber600,
        )
        ConfirmVariant.INFO -> Quad(
            R.drawable.ic_lucide_info,
            dk(Tw.Sky100, Tw.Sky950.a(0.6f)), dk(Tw.Sky600, Tw.Sky400), Tw.Sky600,
        )
    }
    TwModal(
        onDismiss = onCancel,
        maxWidthRem = 24f, // max-w-sm
        fillHeight = false,
        scrimColor = Tw.Slate950.a(0.6f),
    ) {
        Box(Modifier.fillMaxWidth().padding(rem(1.5f))) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(rem(1.25f)),
            ) {
                Box(
                    Modifier.size(rem(3.5f)).background(iconBg, RoundedCornerShape(rem(1f))),
                    contentAlignment = Alignment.Center,
                ) { LucideIcon(icon, 1.75f, iconColor) }

                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (title != null) {
                        TwText(title, 1f, 1.5f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold)
                    }
                    TwText(
                        message, 0.875f, 1.625f, dk(Tw.Slate600, Tw.Slate400),
                        align = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(top = if (title != null) rem(0.5f) else 0.dp),
                    )
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(rem(0.75f))) {
                    Box(
                        Modifier.weight(1f)
                            .background(dk(Tw.Slate100, Tw.Slate800), RoundedCornerShape(rem(0.75f)))
                            .clickable(remember { MutableInteractionSource() }, null, onClick = onCancel)
                            .padding(vertical = rem(0.625f)),
                        contentAlignment = Alignment.Center,
                    ) { TwText(cancelLabel, 0.875f, 1.25f, dk(Tw.Slate600, Tw.Slate300), weight = FW.Semibold) }
                    Box(
                        Modifier.weight(1f)
                            .background(confirmBg, RoundedCornerShape(rem(0.75f)))
                            .clickable(remember { MutableInteractionSource() }, null, onClick = onConfirm)
                            .padding(vertical = rem(0.625f)),
                        contentAlignment = Alignment.Center,
                    ) { TwText(confirmLabel, 0.875f, 1.25f, Tw.White, weight = FW.Bold) }
                }
            }
            // close (physical top-left in the web)
            Box(Modifier.align(Alignment.TopEnd)) {
                TwIconButton(R.drawable.ic_lucide_x, onCancel, Tw.Slate400, padRem = 0.375f, iconRem = 1f)
            }
        }
    }
}

private data class Quad(val icon: Int, val iconBg: Color, val iconColor: Color, val confirmBg: Color)
