package com.myaccounting.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.myaccounting.app.R
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem

/**
 * The shared card of the pickers (`w-full max-w-lg max-h-[85vh] rounded-2xl bg-white dark:bg-slate-900
 * border shadow-2xl`): a header with title/subtitle and a close button, an optional header button,
 * a search row, a scrolling body and an optional footer.
 */
@Composable
fun PickerShell(
    title: String,
    subtitle: String,
    onClose: () -> Unit,
    maxWidthRem: Float = 32f,          // max-w-lg
    maxHeightFraction: Float = 0.85f,  // max-h-[85vh]
    headerTrailing: @Composable () -> Unit = {},
    showHeaderTrailing: Boolean = false,
    search: @Composable () -> Unit = {},
    footer: @Composable () -> Unit = {},
    showFooter: Boolean = false,
    bodyModifier: Modifier = Modifier,
    bodySpacing: Dp? = null,
    body: @Composable ColumnScope.() -> Unit,
) {
    TwModal(
        onDismiss = onClose,
        maxWidthRem = maxWidthRem,
        maxHeightFraction = maxHeightFraction,
        shapeRem = 1f,                 // rounded-2xl
    ) {
        // ---- header -------------------------------------------------------------------------------------
        Row(
            Modifier.fillMaxWidth().padding(rem(1f)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                TwText(title, 1f, 1.5f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1)
                TwText(subtitle, 0.6875f, 1f, Tw.Slate400, Modifier.padding(top = rem(0.125f)), maxLines = 1)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f)), verticalAlignment = Alignment.CenterVertically) {
                if (showHeaderTrailing) headerTrailing()
                TwIconButton(R.drawable.ic_lucide_x, onClose, Tw.Slate400)
            }
        }
        HairLine(dk(Tw.Slate100, Tw.Slate800))

        Box(Modifier.fillMaxWidth().padding(horizontal = rem(1f), vertical = rem(0.5f))) { search() }

        // ---- body ---------------------------------------------------------------------------------------
        Column(
            Modifier.weight(1f).then(bodyModifier),
            verticalArrangement = if (bodySpacing == null) Arrangement.Top else Arrangement.spacedBy(bodySpacing),
            content = body,
        )

        if (showFooter) {
            HairLine(dk(Tw.Slate100, Tw.Slate800))
            Row(
                Modifier.fillMaxWidth().background(dk(Tw.Slate50.a(0.8f), Tw.Slate900.a(0.8f))).padding(rem(0.75f)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) { footer() }
        }
    }
}

/** A 1 CSS px divider (the `border-b` of the picker headers/footers). */
@Composable
fun HairLine(color: Color) {
    Box(Modifier.fillMaxWidth().height(cssBorder()).background(color))
}

/** `text-[0.6875rem] font-semibold hover:underline` links of the picker footers. */
@Composable
fun PickerLink(text: String, color: Color, weight: Int = FW.Normal, onClick: () -> Unit) {
    Box(Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClick)) {
        TwText(text, 0.6875f, 1f, color, weight = weight, maxLines = 1)
    }
}

/** The groups row of the pickers: `rounded-xl border` with a chevron, the title and a count pill. */
@Composable
fun PickerGroupRow(
    title: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().background(dk(Tw.Slate50.a(0.7f), Tw.Slate800.a(0.5f))).padding(rem(0.75f)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, null, onClick = onToggle),
            horizontalArrangement = Arrangement.spacedBy(rem(0.625f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LucideIcon(
                if (expanded) R.drawable.ic_lucide_chevron_up else R.drawable.ic_lucide_chevron_down, 0.875f, Tw.Slate400,
            )
            TwText(title, 0.75f, 1f, dk(Tw.Slate700, Tw.Slate200), weight = FW.Bold, maxLines = 1)
            Box(
                Modifier.background(dk(Tw.Slate200.a(0.7f), Tw.Slate700), CircleShape)
                    .padding(horizontal = rem(0.375f), vertical = rem(0.125f)),
            ) {
                TwText(count.toString(), 0.625f, 0.9375f, dk(Tw.Slate500, Tw.Slate400), maxLines = 1)
            }
        }
        trailing()
    }
}

/** The `w-8 h-8 rounded-xl bg-sky-100 … font-bold text-xs` monogram of the person picker. */
@Composable
fun PickerAvatar(letter: String) {
    Box(
        Modifier.size(rem(2f)).background(dk(Tw.Sky100, Tw.Sky950), RoundedCornerShape(rem(0.75f))),
        contentAlignment = Alignment.Center,
    ) {
        TwText(letter, 0.75f, 1f, dk(Tw.Sky700, Tw.Sky300), weight = FW.Bold, maxLines = 1)
    }
}

/** The `w-3.5 h-3.5 rounded text-sky-600` checkbox of the multi-select pickers. */
@Composable
fun TwCheckbox(checked: Boolean, indeterminate: Boolean = false, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(rem(0.25f))
    val bw = cssBorder()
    Box(
        Modifier.size(rem(0.875f))
            .background(if (checked || indeterminate) Tw.Sky600 else dk(Tw.White, Tw.Slate900), shape)
            .then(if (!checked && !indeterminate) Modifier.border(bw, dk(Tw.Slate300, Tw.Slate600), shape) else Modifier)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        when {
            checked -> LucideIcon(R.drawable.ic_lucide_check, 0.625f, Tw.White)
            indeterminate -> Box(Modifier.size(width = rem(0.5f), height = rem(0.125f)).background(Tw.White, RoundedCornerShape(rem(0.0625f))))
        }
    }
}
