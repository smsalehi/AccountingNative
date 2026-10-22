package com.myaccounting.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.myaccounting.app.R
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem
import androidx.compose.ui.unit.dp

/**
 * The `select` of the original forms (`px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border
 * text-xs`): the chosen option's text with a chevron at the end, and a native dropdown menu.
 * [options] is `uid to title`; the empty uid is [placeholder].
 */
@Composable
fun TwSelectField(
    selectedUid: String,
    options: List<Pair<String, String>>,
    placeholder: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    optionPrefix: String = "",
) {
    var open by remember { mutableStateOf(false) }
    val selectedTitle = options.firstOrNull { it.first == selectedUid }?.second
    val shape = RoundedCornerShape(rem(0.75f))
    val bw = cssBorder()
    Box(modifier) {
        Row(
            Modifier.fillMaxWidth().background(dk(Tw.Slate50, Tw.Slate800), shape)
                .border(bw, dk(Tw.Slate200, Tw.Slate700), shape)
                .clickable(remember { MutableInteractionSource() }, null) { open = true }
                .padding(start = rem(0.75f), end = rem(0.75f), top = rem(0.5f), bottom = rem(0.5f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TwText(
                if (selectedTitle != null) optionPrefix + selectedTitle else placeholder,
                0.75f, 1f, dk(Tw.Slate800, Tw.Slate100),
                Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            LucideIcon(R.drawable.ic_lucide_chevron_down, 1f, dk(Tw.Slate400, Tw.Slate500))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { TwText(placeholder, 0.75f, 1f, dk(Tw.Slate800, Tw.Slate100), maxLines = 2) },
                onClick = { onSelect(""); open = false },
            )
            for (option in options) {
                DropdownMenuItem(
                    text = {
                        TwText(
                            optionPrefix + option.second, 0.75f, 1f, dk(Tw.Slate800, Tw.Slate100),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onClick = { onSelect(option.first); open = false },
                )
            }
        }
    }
}

/**
 * The picker field of the transaction modal: `p-3 h-[50px] rounded-xl bg-slate-50 dark:bg-slate-800/80
 * border border-slate-200 dark:border-slate-700` with a leading visual, a label and a sky action hint
 * (`تغییر` + chevron-left).
 */
@Composable
fun TwSelectorField(
    text: String,
    placeholder: Boolean,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(rem(0.75f))
    Row(
        modifier.fillMaxWidth().height(50.dp)
            .background(dk(Tw.Slate50, Tw.Slate800.a(0.8f)), shape)
            .border(cssBorder(), dk(Tw.Slate200, Tw.Slate700), shape)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(rem(0.75f)),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(rem(0.625f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            TwText(
                text, 0.75f, 1f,
                if (placeholder) Tw.Slate400 else dk(Tw.Slate800, Tw.Slate100),
                weight = if (placeholder) FW.Normal else FW.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(rem(0.125f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TwText(action, 0.6875f, 1f, dk(Tw.Sky600, Tw.Sky400), weight = FW.Semibold, maxLines = 1)
            LucideIcon(R.drawable.ic_lucide_chevron_left, 0.875f, dk(Tw.Sky600, Tw.Sky400))
        }
    }
}
