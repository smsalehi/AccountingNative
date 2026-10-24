package com.myaccounting.app.ui.persons

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.myaccounting.app.R
import com.myaccounting.app.domain.CurrencyUnit
import com.myaccounting.app.domain.Person
import com.myaccounting.app.domain.PersonGroup
import com.myaccounting.app.ui.components.AmountInput
import com.myaccounting.app.ui.components.HairLine
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.components.TwCheckbox
import com.myaccounting.app.ui.components.TwIconButton
import com.myaccounting.app.ui.components.TwModal
import com.myaccounting.app.ui.components.TwSelectField
import com.myaccounting.app.ui.components.TwTextField
import com.myaccounting.app.ui.components.TwShadow
import com.myaccounting.app.ui.components.cssShadow
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem
import kotlin.random.Random

/** `UID: 'person_' + Date.now().toString(36) + 4 random base-36 characters` (PersonModal.tsx). */
private fun newPersonUid(now: Long = System.currentTimeMillis()): String {
    val alphabet = "0123456789abcdefghijklmnopqrstuvwxyz"
    val suffix = (1..4).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
    return "person_" + now.toString(36) + suffix
}

/**
 * PersonModal.tsx (z-50, `max-w-md max-h-[90vh]`): new/edit person with group, mobile,
 * opening balance with a طلب/بدهی direction toggle, description and the «راکد» checkbox.
 */
@Composable
fun PersonDialog(
    person: Person?,
    groups: List<PersonGroup>,
    unit: CurrencyUnit,
    onClose: () -> Unit,
    onSave: (Person) -> Unit,
    onError: (String) -> Unit = {},
) {
    var fullName by remember { mutableStateOf(person?.fullName ?: "") }
    var groupUid by remember { mutableStateOf(person?.groupUid ?: groups.firstOrNull()?.uid ?: "") }
    var mobile by remember { mutableStateOf(person?.mobile ?: "") }
    var desc by remember { mutableStateOf(person?.desc ?: "") }
    // مبلغ مانده اولیه همیشه تومان (قرارداد AmountInputWithCalculator)
    var opening by remember { mutableStateOf(kotlin.math.abs(kotlin.math.round(person?.oldBalance ?: 0.0))) }
    var receivable by remember { mutableStateOf((person?.oldBalance ?: 0.0) < 0) }
    var isHidden by remember { mutableStateOf(person?.isHidden == 1) }

    fun submit() {
        if (fullName.trim().isEmpty()) {
            onError("لطفاً نام شخص را وارد نمایید.")
            return
        }
        val amount = kotlin.math.max(0.0, kotlin.math.round(opening))
        // طلب ما از ایشان = منفی، بدهی ما به ایشان = مثبت
        val signed = if (receivable) -amount else amount
        if (person != null) {
            onSave(
                person.copy(
                    fullName = fullName.trim(),
                    groupUid = groupUid,
                    mobile = mobile.trim(),
                    desc = desc.trim(),
                    oldBalance = signed,
                    isHidden = if (isHidden) 1 else 0,
                ),
            )
        } else {
            onSave(
                Person(
                    uid = newPersonUid(),
                    fullName = fullName.trim(),
                    groupUid = groupUid,
                    tel = "",
                    mobile = mobile.trim(),
                    email = "",
                    address = "",
                    oldBalance = signed,
                    isHidden = 0,
                    desc = desc.trim(),
                ),
            )
        }
        onClose()
    }

    TwModal(
        onDismiss = onClose,
        maxWidthRem = 28f,          // max-w-md
        maxHeightFraction = 0.9f,   // max-h-[90vh]
        shapeRem = 1f,              // rounded-2xl
        fillHeight = false,
        scrimColor = Tw.Slate900.a(0.7f),
    ) {
        // ---- header (border-b border-slate-100 dark:border-slate-800 pb-3) ------------------------------
        Row(
            Modifier.fillMaxWidth().padding(bottom = rem(0.75f)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.625f)), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(rem(2.25f)).background(dk(Tw.Sky100, Tw.Sky950), RoundedCornerShape(rem(0.75f))),
                    contentAlignment = Alignment.Center,
                ) { LucideIcon(R.drawable.ic_lucide_user, 1.25f, dk(Tw.Sky600, Tw.Sky400)) }
                Column {
                    TwText(
                        if (person != null) "ویرایش شخص" else "افزودن شخص جدید",
                        1f, 1.5f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1,
                    )
                    TwText(
                        if (person != null) "نام، گروه و مانده اولیه را ویرایش کنید" else "شخص را به یک گروه اضافه کنید",
                        0.6875f, 1f, Tw.Slate400, maxLines = 1,
                    )
                }
            }
            TwIconButton(R.drawable.ic_lucide_x, onClose, Tw.Slate400, iconRem = 1.25f)
        }
        HairLine(dk(Tw.Slate100, Tw.Slate800))

        // ---- form (space-y-3.5) --------------------------------------------------------------------------
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = rem(0.875f)),
            verticalArrangement = Arrangement.spacedBy(rem(0.875f)),
        ) {
            // ---- full name -------------------------------------------------------------------------------
            Column {
                PersonFieldLabel("نام و نام خانوادگی:")
                TwTextField(
                    value = fullName, onValueChange = { fullName = it },
                    placeholder = "مثلاً: علی رضایی",
                    vPadRem = 0.5f, startPadRem = 0.75f, endPadRem = 0.75f,
                    bg = dk(Tw.Slate50, Tw.Slate800),
                )
            }

            // ---- group + mobile (`grid-cols-1 sm:grid-cols-2`; the phone is below the sm breakpoint) -----
            Column(verticalArrangement = Arrangement.spacedBy(rem(0.75f))) {
                Column {
                    PersonFieldLabel("گروه:")
                    // The select keeps a trailing «بدون گروه (سایر)» option; with no groups it is the only one.
                    val options = groups.map { it.uid to it.title } + ("" to "بدون گروه (سایر)")
                    TwSelectField(
                        selectedUid = groupUid,
                        options = options,
                        placeholder = "بدون گروه (سایر)",
                        onSelect = { groupUid = it },
                    )
                }
                Column {
                    PersonFieldLabel("موبایل (اختیاری):")
                    TwTextField(
                        value = mobile, onValueChange = { mobile = it },
                        placeholder = "۰۹...",
                        vPadRem = 0.5f, startPadRem = 0.75f, endPadRem = 0.75f,
                        bg = dk(Tw.Slate50, Tw.Slate800),
                    )
                }
            }

            // ---- opening balance + direction --------------------------------------------------------------
            Column {
                AmountInput(
                    value = opening,
                    onChange = { opening = it },
                    unit = unit,
                    label = "مانده اولیه (اختیاری)",
                    placeholder = "۰",
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = rem(0.5f))
                        .background(dk(Tw.Slate100, Tw.Slate800), RoundedCornerShape(rem(0.75f)))
                        .padding(rem(0.25f)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DirectionButton("طلب ما از ایشان", receivable, Tw.Emerald600, Tw.Emerald300, Modifier.weight(1f)) { receivable = true }
                    DirectionButton("بدهی ما به ایشان", !receivable, Tw.Rose600, Tw.Rose300, Modifier.weight(1f)) { receivable = false }
                }
            }

            // ---- description ------------------------------------------------------------------------------
            Column {
                PersonFieldLabel("توضیحات (اختیاری):")
                TwTextField(
                    value = desc, onValueChange = { desc = it },
                    vPadRem = 0.5f, startPadRem = 0.75f, endPadRem = 0.75f,
                    bg = dk(Tw.Slate50, Tw.Slate800),
                )
            }

            // ---- «راکد» checkbox (edit mode only) ----------------------------------------------------------
            if (person != null) {
                val boxShape = RoundedCornerShape(rem(0.75f))
                Row(
                    Modifier.fillMaxWidth()
                        .background(dk(Tw.Slate50.a(0.7f), Tw.Slate800.a(0.4f)), boxShape)
                        .border(cssBorder(), dk(Tw.Slate200, Tw.Slate700), boxShape)
                        .clickable(remember { MutableInteractionSource() }, null) { isHidden = !isHidden }
                        .padding(rem(0.5f)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
                ) {
                    TwCheckbox(checked = isHidden) { isHidden = !isHidden }
                    TwText("مخفی از لیست اصلی (راکد)", 0.75f, 1f, dk(Tw.Slate700, Tw.Slate200), weight = FW.Semibold, maxLines = 1)
                }
            }

            // ---- actions (pt-2 justify-end gap-2) -----------------------------------------------------------
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
                    Modifier.cssShadow(TwShadow.xs(), rem(0.75f))
                        .background(Tw.Sky600, RoundedCornerShape(rem(0.75f)))
                        .clickable(remember { MutableInteractionSource() }, null) { submit() }
                        .padding(horizontal = rem(1.25f), vertical = rem(0.5f)),
                    horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LucideIcon(R.drawable.ic_lucide_user, 1f, Tw.White)
                    TwText(if (person != null) "ذخیره تغییرات" else "افزودن شخص", 0.75f, 1f, Tw.White, weight = FW.Bold, maxLines = 1)
                }
            }
        }
    }
}

/** `label` of the form: text-xs font-bold slate-700 dark:slate-300 mb-1. */
@Composable
private fun PersonFieldLabel(text: String) {
    Box(Modifier.fillMaxWidth().padding(bottom = rem(0.25f))) {
        TwText(text, 0.75f, 1f, dk(Tw.Slate700, Tw.Slate300), weight = FW.Bold, maxLines = 1)
    }
}

/**
 * `flex-1 px-3 py-1.5 rounded-lg` half of the direction toggle: active = bg-white/dark:slate-700 with
 * the accent colour and shadow-xs; inactive = plain slate-500.
 */
@Composable
private fun DirectionButton(text: String, active: Boolean, activeLight: Color, activeDark: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .then(if (active) Modifier.cssShadow(TwShadow.xs(), rem(0.5f)) else Modifier)
            .background(if (active) dk(Tw.White, Tw.Slate700) else Color.Transparent, RoundedCornerShape(rem(0.5f)))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = rem(0.75f), vertical = rem(0.375f)),
        contentAlignment = Alignment.Center,
    ) {
        TwText(
            text, 0.75f, 1f,
            if (active) dk(activeLight, activeDark) else dk(Tw.Slate500, Tw.Slate500),
            weight = FW.Semibold, maxLines = 1, align = TextAlign.Center,
        )
    }
}
