package com.myaccounting.app.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myaccounting.app.R
import com.myaccounting.app.domain.Subject
import com.myaccounting.app.domain.newSubjectUid
import com.myaccounting.app.ui.components.CategoryIcons
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.components.PresetIcon
import com.myaccounting.app.ui.components.TwModal
import com.myaccounting.app.ui.components.TwText
import com.myaccounting.app.ui.components.TwTextField
import com.myaccounting.app.ui.components.TwIconButton
import com.myaccounting.app.ui.components.TwSelectField
import com.myaccounting.app.ui.components.HairLine
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem

/**
 * NewSubjectModal.tsx (z-70, `max-w-md max-h-[90vh]`): defines a سرفصل or a زیرفصل with an explicit
 * cost/income direction, an optional parent and one of the 29 preset icons.
 */
@Composable
fun NewSubjectDialog(
    subjects: List<Subject>,
    onClose: () -> Unit,
    onSave: (Subject) -> Unit,
    initialIsCost: Int = 1,
    initialParentUid: String? = null,
    onError: (String) -> Unit = {},
) {
    var title by remember { mutableStateOf("") }
    var isCost by remember { mutableStateOf(initialIsCost) }
    var parentUid by remember { mutableStateOf(initialParentUid ?: "") }
    var iconId by remember { mutableStateOf("other") }

    val availableParents = subjects.filter { it.isCost == isCost && it.parentUid.isNullOrEmpty() }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    fun submit() {
        if (title.trim().isEmpty()) {
            onError("لطفاً عنوان سرفصل را وارد نمایید.")
            return
        }
        onSave(
            Subject(
                uid = newSubjectUid(),
                title = title.trim(),
                isCost = isCost,
                parentUid = parentUid.ifEmpty { null },
                isHidden = 0,
                icon = iconId,
            ),
        )
        onClose()
    }

    TwModal(
        onDismiss = onClose,
        maxWidthRem = 28f,              // max-w-md
        maxHeightFraction = 0.9f,       // max-h-[90vh]
        shapeRem = 1f,                  // rounded-2xl
        scrimColor = Tw.Slate900.a(0.6f),
    ) {
        // ---- header -------------------------------------------------------------------------------------
        Row(
            Modifier.fillMaxWidth().padding(rem(1f)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f)), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(rem(2f)).background(dk(Tw.Sky100, Tw.Sky950), RoundedCornerShape(rem(0.5f))),
                    contentAlignment = Alignment.Center,
                ) {
                    LucideIcon(R.drawable.ic_lucide_folder_tree, 1f, dk(Tw.Sky600, Tw.Sky400))
                }
                Column {
                    TwText(
                        if (parentUid.isNotEmpty()) "افزودن زیرفصل جدید" else "افزودن سرفصل جدید",
                        0.875f, 1.25f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1,
                    )
                    TwText("تعریف دسته‌بندی جدید با انتخاب آیکون اختصاصی", 0.6875f, 1f, Tw.Slate400, maxLines = 1)
                }
            }
            TwIconButton(R.drawable.ic_lucide_x, onClose, Tw.Slate400)
        }
        HairLine(dk(Tw.Slate100, Tw.Slate800))

        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(rem(1f)),
            verticalArrangement = Arrangement.spacedBy(rem(1f)),
        ) {
            // ---- direction ------------------------------------------------------------------------------
            Column {
                FieldLabel("نوع سرفصل:", bottomRem = 0.375f)
                Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f))) {
                    DirectionButton("سرفصل هزینه", isCost == 1, Tw.Rose500, Modifier.weight(1f)) {
                        isCost = 1; parentUid = ""
                    }
                    DirectionButton("سرفصل درآمد", isCost == 0, Tw.Emerald500, Modifier.weight(1f)) {
                        isCost = 0; parentUid = ""
                    }
                }
            }

            // ---- title ----------------------------------------------------------------------------------
            Column {
                FieldLabel("عنوان سرفصل یا زیرفصل:", bottomRem = 0.25f)
                TwTextField(
                    value = title, onValueChange = { title = it },
                    placeholder = "مثلاً: خریدهای آنلاین، حق‌المشاوره، سوخت و بنزین...",
                    modifier = Modifier.focusRequester(focus),
                    vPadRem = 0.5f, startPadRem = 0.75f, endPadRem = 0.75f,
                    bg = dk(Tw.Slate50, Tw.Slate800),
                )
            }

            // ---- parent ---------------------------------------------------------------------------------
            Column {
                FieldLabel("دسته اصلی (والد):", bottomRem = 0.25f)
                TwSelectField(
                    selectedUid = parentUid,
                    options = availableParents.map { it.uid to it.title },
                    placeholder = "بدون والد (این مورد یک سرفصل اصلی خواهد بود)",
                    onSelect = { parentUid = it },
                    optionPrefix = "زیرمجموعه: ",
                )
            }

            // ---- icon -----------------------------------------------------------------------------------
            Column {
                FieldLabel("انتخاب آیکون سرفصل:", bottomRem = 0.375f)
                Box(
                    Modifier.fillMaxWidth().heightIn(max = rem(11f))   // max-h-44
                        .verticalScroll(rememberScrollState())
                        .background(dk(Tw.Slate50, Tw.Slate800.a(0.6f)), RoundedCornerShape(rem(0.75f)))
                        .border(cssBorder(), dk(Tw.Slate200.a(0.7f), Tw.Slate700), RoundedCornerShape(rem(0.75f)))
                        .padding(rem(0.5f)),
                ) {
                    val cols = 5
                    val gap = rem(0.5f)
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        CategoryIcons.PRESETS.chunked(cols).forEach { rowItems ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                                rowItems.forEach { item ->
                                    PresetIconCell(item, iconId == item.id, Modifier.weight(1f)) { iconId = item.id }
                                }
                                repeat(cols - rowItems.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }

            // ---- actions --------------------------------------------------------------------------------
            HairLine(dk(Tw.Slate100, Tw.Slate800))
            Row(
                Modifier.fillMaxWidth().padding(top = rem(0.5f)),
                horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClose)
                        .padding(horizontal = rem(1f), vertical = rem(0.5f)),
                ) { TwText("انصراف", 0.75f, 1f, dk(Tw.Slate600, Tw.Slate300), weight = FW.Semibold, maxLines = 1) }
                Row(
                    Modifier.background(Tw.Sky600, RoundedCornerShape(rem(0.75f)))
                        .clickable(remember { MutableInteractionSource() }, null) { submit() }
                        .padding(horizontal = rem(1.25f), vertical = rem(0.5f)),
                    horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LucideIcon(R.drawable.ic_lucide_plus, 1f, Tw.White)
                    TwText("ذخیره سرفصل", 0.75f, 1f, Tw.White, weight = FW.Bold, maxLines = 1)
                }
            }
        }
    }
}

/** `label` of the form: text-xs font-bold slate-700 dark:slate-300 mb-1 / mb-1.5. */
@Composable
private fun FieldLabel(text: String, bottomRem: Float) {
    Box(Modifier.fillMaxWidth().padding(bottom = rem(bottomRem))) {
        TwText(text, 0.75f, 1f, dk(Tw.Slate700, Tw.Slate300), weight = FW.Bold, maxLines = 1)
    }
}

/** `py-2 rounded-xl text-xs font-bold` toggle of the direction grid. */
@Composable
private fun DirectionButton(text: String, active: Boolean, activeColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.background(
            if (active) activeColor else dk(Tw.Slate100, Tw.Slate800),
            RoundedCornerShape(rem(0.75f)),
        ).clickable(remember { MutableInteractionSource() }, null, onClick = onClick).padding(vertical = rem(0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        TwText(
            text, 0.75f, 1f,
            if (active) Tw.White else dk(Tw.Slate600, Tw.Slate400),
            weight = FW.Bold, maxLines = 1,
        )
    }
}

/** One cell of the icon grid: `p-2 rounded-xl border` with a `w-5 h-5` icon and a 9 px label. */
@Composable
private fun PresetIconCell(item: PresetIcon, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(rem(0.75f))
    // ring-2 ring-sky-300: a 2 px sky-300 ring *outside* the border box.
    Box(
        modifier.then(
            if (selected) Modifier.background(Tw.Sky300, RoundedCornerShape(rem(0.75f) + 2.dp)).padding(2.dp)
            else Modifier,
        ),
    ) {
        Column(
            Modifier.fillMaxWidth()
                .background(if (selected) Tw.Sky500 else dk(Tw.White, Tw.Slate800), shape)
                .border(cssBorder(), if (selected) Tw.Sky500 else dk(Tw.Slate200.a(0.8f), Tw.Slate700), shape)
                .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
                .padding(rem(0.5f)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.padding(bottom = rem(0.25f))) {
                LucideIcon(item.icon, 1.25f, if (selected) Tw.White else dk(Tw.Slate700, Tw.Slate300))
            }
            TwText(
                item.title, 0.5625f, 0.703f,
                if (selected) Tw.White else dk(Tw.Slate700, Tw.Slate300),
                weight = FW.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis, align = TextAlign.Center,
            )
        }
    }
}
