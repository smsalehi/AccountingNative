package com.myaccounting.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myaccounting.app.domain.JalaliDate
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem

/**
 * The read-only trigger of the original date/time pickers:
 * `relative` + `w-4 h-4 text-slate-400 absolute right-3.5 top-1/2 -translate-y-1/2` icon over an
 * `h-[50px] pl-3 pr-10 rounded-xl border text-sm text-center` field.
 */
@Composable
fun PickerFieldBox(
    @DrawableRes icon: Int,
    iconTint: Color,
    value: String,
    placeholder: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(rem(0.75f))
    val bw = cssBorder()
    Box(Modifier.fillMaxWidth().height(50.dp)) {
        Box(
            Modifier.fillMaxSize()
                .background(dk(Tw.Slate50, Tw.Slate800), shape)
                .border(bw, dk(Tw.Slate200, Tw.Slate700), shape)
                .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
                .absolutePadding(left = rem(0.75f), right = rem(2.5f))
                .padding(vertical = bw),
            contentAlignment = Alignment.Center,
        ) {
            if (value.isNotEmpty()) {
                TwText(value, 0.875f, 1.25f, dk(Tw.Slate900, Tw.Slate100), weight = FW.Normal, maxLines = 1, align = TextAlign.Center)
            } else {
                TwText(placeholder, 0.875f, 1.25f, Tw.Slate400, maxLines = 1, align = TextAlign.Center)
            }
        }
        Box(Modifier.align(Alignment.CenterEnd).absolutePadding(right = rem(0.875f))) {
            LucideIcon(icon, 1f, iconTint)
        }
    }
}

/** The visible text of the picker fields: `toPersianStr(value)`. */
fun pickerDisplay(value: String): String = JalaliDate.toPersianDigits(value)
