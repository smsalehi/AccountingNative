package com.myaccounting.app.ui.depts

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
import androidx.compose.ui.text.input.KeyboardType
import com.myaccounting.app.R
import com.myaccounting.app.domain.CurrencyUnit
import com.myaccounting.app.domain.Dept
import com.myaccounting.app.ui.components.AmountInput
import com.myaccounting.app.ui.components.HairLine
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.components.TwCheckbox
import com.myaccounting.app.ui.components.TwIconButton
import com.myaccounting.app.ui.components.TwModal
import com.myaccounting.app.ui.components.TwSelectField
import com.myaccounting.app.ui.components.TwShadow
import com.myaccounting.app.ui.components.TwTextField
import com.myaccounting.app.ui.components.PersianDateField
import com.myaccounting.app.ui.components.cssShadow
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem
import kotlin.math.ceil

/** `UID: 'dept_' + Date.now().toString(36) + 4 random base-36 characters` (DeptsView.tsx). */
private fun newDeptUid(now: Long = System.currentTimeMillis()): String {
    val alphabet = "0123456789abcdefghijklmnopqrstuvwxyz"
    val suffix = (1..4).map { alphabet.random() }.joinToString("")
    return "dept_" + now.toString(36) + suffix
}

/** DeptsView.tsx parseOptionalCount: an integer > 0, or null. */
private fun parseOptionalCount(raw: String): Int? = raw.trim().toIntOrNull()?.takeIf { it > 0 }

/** حدس خودکار تعداد اقساط از روی مبالغ: سقف(کل / قسط) */
private fun autoCountHint(total: Double, installment: Double): Long? =
    if (total > 0 && installment > 0) ceil(total / installment).toLong() else null

/**
 * DeptsView.tsx Add New Loan Modal (z-50, `max-w-md`, scrim slate-900/60): title, total/installment
 * amounts, optional installment count with an automatic hint, first due date and the optional
 * period / project / description group.
 */
@Composable
fun DeptAddDialog(
    unit: CurrencyUnit,
    projects: List<com.myaccounting.app.domain.Project>,
    onClose: () -> Unit,
    onAdd: (Dept) -> Unit,
    onError: (String) -> Unit = {},
) {
    var title by remember { mutableStateOf("") }
    var totalValueStr by remember { mutableStateOf("") }
    var installmentValueStr by remember { mutableStateOf("") }
    var totalCountStr by remember { mutableStateOf("") }
    var firstDueDate by remember { mutableStateOf("") }
    var periodDaysStr by remember { mutableStateOf("") }
    var projectUid by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var showMore by remember { mutableStateOf(false) }

    fun parseAmount(raw: String): Double? {
        val d = raw.replace(",", "").toDoubleOrNull()
        return if (d != null && d > 0) d else null
    }

    fun submit() {
        if (title.trim().isEmpty()) { onError("لطفاً عنوان وام را وارد نمایید."); return }
        val rawTotal = parseAmount(totalValueStr) ?: run { onError("لطفاً مبلغ کل وام را وارد نمایید."); return }
        val rawInstallment = parseAmount(installmentValueStr) ?: run { onError("لطفاً مبلغ هر قسط را وارد نمایید."); return }
        // ورودی فرم با واحد نمایشی است؛ دیتابیس همیشه تومان (rial = ×10)
        val totalTomans = if (unit == CurrencyUnit.RIAL) kotlin.math.round(rawTotal / 10) else rawTotal
        val installmentTomans = if (unit == CurrencyUnit.RIAL) kotlin.math.round(rawInstallment / 10) else rawInstallment
        onAdd(
            Dept(
                uid = newDeptUid(),
                title = title.trim(),
                value = totalTomans,
                paymentStartDate = System.currentTimeMillis(),
                periodDays = periodDaysStr.trim().toDoubleOrNull() ?: 30.0,
                periodValue = installmentTomans,
                reminderDaysBeforeDueDate = 3,
                eventUid = null,
                projectUid = projectUid.ifEmpty { null },
                isHidden = 0,
                desc = desc.trim(),
                totalInstallments = parseOptionalCount(totalCountStr),
                firstDueDate = firstDueDate.trim().ifEmpty { null },
            ),
        )
        onClose()
    }

    val autoCount = autoCountHint(totalValueStr.replace(",", "").toDoubleOrNull() ?: 0.0, installmentValueStr.replace(",", "").toDoubleOrNull() ?: 0.0)
    val unitLabel = if (unit == CurrencyUnit.RIAL) "ریال" else "تومان"

    TwModal(
        onDismiss = onClose,
        maxWidthRem = 28f,
        maxHeightFraction = 0.92f,
        shapeRem = 1f,
        scrimColor = Tw.Slate900.a(0.6f),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = rem(0.75f)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                TwText("افزودن وام یا تسهیلات جدید", 1f, 1.5f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1)
                TwText("ثبت وام بانکی، قرض‌الحسنه یا تسهیلات خرید", 0.6875f, 1f, Tw.Slate400, maxLines = 1)
            }
            TwIconButton(R.drawable.ic_lucide_x, onClose, Tw.Slate400, iconRem = 1.25f)
        }
        HairLine(dk(Tw.Slate100, Tw.Slate800))

        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = rem(0.875f)),
            verticalArrangement = Arrangement.spacedBy(rem(0.875f)),
        ) {
            Column {
                DeptFieldLabel("عنوان وام یا تسهیلات:")
                TwTextField(
                    value = title, onValueChange = { title = it },
                    placeholder = "مثلاً: وام مسکن ملی، تسهیلات رسالت، خرید خودرو",
                    vPadRem = 0.5f, startPadRem = 0.75f, endPadRem = 0.75f,
                    bg = dk(Tw.Slate50, Tw.Slate800),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(rem(0.75f))) {
                Column {
                    DeptFieldLabel("مبلغ کل مانده وام ($unitLabel):")
                    TwTextField(
                        value = totalValueStr, onValueChange = { totalValueStr = it.filterNumber() },
                        placeholder = "مثلاً: ۵۰,۰۰۰,۰۰۰",
                        vPadRem = 0.5f, startPadRem = 0.75f, endPadRem = 0.75f,
                        bg = dk(Tw.Slate50, Tw.Slate800),
                        keyboardType = KeyboardType.Number,
                    )
                }
                Column {
                    DeptFieldLabel("مبلغ هر قسط ($unitLabel):")
                    TwTextField(
                        value = installmentValueStr, onValueChange = { installmentValueStr = it.filterNumber() },
                        placeholder = "مثلاً: ۲,۵۰۰,۰۰۰",
                        vPadRem = 0.5f, startPadRem = 0.75f, endPadRem = 0.75f,
                        bg = dk(Tw.Slate50, Tw.Slate800),
                        keyboardType = KeyboardType.Number,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(rem(0.75f))) {
                Column {
                    DeptFieldLabel("تعداد اقساط (اختیاری):")
                    TwTextField(
                        value = totalCountStr, onValueChange = { totalCountStr = it.filterNumber() },
                        placeholder = if (autoCount != null) "خودکار: ${com.myaccounting.app.domain.JalaliDate.toPersianDigits(autoCount.toString())} قسط" else "مثلاً: ۱۲",
                        vPadRem = 0.5f, startPadRem = 0.75f, endPadRem = 0.75f,
                        bg = dk(Tw.Slate50, Tw.Slate800),
                        keyboardType = KeyboardType.Number,
                    )
                }
                Column {
                    DeptFieldLabel("تاریخ اولین سررسید:")
                    PersianDateField(
                        value = firstDueDate,
                        onChange = { firstDueDate = it },
                        placeholder = "مثلاً: ۱۴۰۳/۱۲/۲۵",
                    )
                    TwText("سررسیدهای بعدی همان روز هر ماه • خالی = دوره‌ای", 0.625f, 0.875f, Tw.Slate400, maxLines = 1, modifier = Modifier.padding(top = rem(0.25f)))
                }
            }

            Row(
                Modifier.clickable(remember { MutableInteractionSource() }, null) { showMore = !showMore },
                horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LucideIcon(
                    if (showMore) R.drawable.ic_lucide_chevron_up else R.drawable.ic_lucide_chevron_down,
                    0.875f, dk(Tw.Slate500, Tw.Slate400),
                )
                TwText("گزینه‌های بیشتر (دوره، پروژه، توضیحات)", 0.6875f, 1f, dk(Tw.Slate500, Tw.Slate400), weight = FW.Semibold, maxLines = 1)
            }

            if (showMore) {
                Column(
                    Modifier.fillMaxWidth().padding(top = rem(0.375f)),
                    verticalArrangement = Arrangement.spacedBy(rem(0.875f)),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(rem(0.75f))) {
                        Column(Modifier.weight(1f)) {
                            DeptFieldLabel("دوره اقساط (به روز):")
                            TwTextField(
                                value = periodDaysStr, onValueChange = { periodDaysStr = it.filterNumber() },
                                placeholder = "۳۰",
                                vPadRem = 0.5f, startPadRem = 0.75f, endPadRem = 0.75f,
                                bg = dk(Tw.Slate50, Tw.Slate800),
                                keyboardType = KeyboardType.Number,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            DeptFieldLabel("پروژه مرتبط (اختیاری):")
                            TwSelectField(
                                selectedUid = projectUid,
                                options = projects.map { it.uid to it.title },
                                placeholder = "عمومی / بدون پروژه",
                                onSelect = { projectUid = it },
                            )
                        }
                    }
                    Column {
                        DeptFieldLabel("توضیحات و شماره تسهیلات (اختیاری):")
                        TwTextField(
                            value = desc, onValueChange = { desc = it },
                            placeholder = "مثلاً: شماره پرونده ۱۲۳۴۵ بانک ملت شعبه مرکزی",
                            vPadRem = 0.5f, startPadRem = 0.75f, endPadRem = 0.75f,
                            bg = dk(Tw.Slate50, Tw.Slate800),
                        )
                    }
                }
            }

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
                        .background(Tw.Amber600, RoundedCornerShape(rem(0.75f)))
                        .clickable(remember { MutableInteractionSource() }, null) { submit() }
                        .padding(horizontal = rem(1.25f), vertical = rem(0.5f)),
                    horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LucideIcon(R.drawable.ic_lucide_check, 1f, Tw.White)
                    TwText("ثبت وام", 0.75f, 1f, Tw.White, weight = FW.Bold, maxLines = 1)
                }
            }
        }
    }
}

/**
 * DeptsView.tsx Edit Dept Modal: title, total/installment via the toman amount input, installment
 * count + first due date, and the optional period / project / description / راکد group.
 */
@Composable
fun DeptEditDialog(
    dept: Dept,
    projects: List<com.myaccounting.app.domain.Project>,
    onClose: () -> Unit,
    onUpdate: (Dept) -> Unit,
    onError: (String) -> Unit = {},
) {
    var title by remember { mutableStateOf(dept.title) }
    var totalValue by remember { mutableStateOf(if (dept.value.isFinite()) dept.value else 0.0) }
    var installmentValue by remember { mutableStateOf(if (dept.periodValue.isFinite()) dept.periodValue else 0.0) }
    var totalCountStr by remember { mutableStateOf(dept.totalInstallments?.toString() ?: "") }
    var firstDueDate by remember { mutableStateOf(dept.firstDueDate ?: "") }
    var periodDaysStr by remember { mutableStateOf(dept.periodDays.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() }) }
    var projectUid by remember { mutableStateOf(dept.projectUid ?: "") }
    var desc by remember { mutableStateOf(dept.desc) }
    var isHidden by remember { mutableStateOf(dept.isHidden == 1) }
    var showMore by remember { mutableStateOf(false) }

    fun submit() {
        if (title.trim().isEmpty()) { onError("لطفاً عنوان وام را وارد نمایید."); return }
        if (!(totalValue > 0)) { onError("لطفاً مبلغ کل وام را وارد نمایید."); return }
        if (!(installmentValue > 0)) { onError("لطفاً مبلغ هر قسط را وارد نمایید."); return }
        onUpdate(
            dept.copy(
                title = title.trim(),
                value = kotlin.math.round(totalValue),
                periodValue = kotlin.math.round(installmentValue),
                periodDays = periodDaysStr.trim().toDoubleOrNull() ?: 30.0,
                projectUid = projectUid.ifEmpty { null },
                desc = desc.trim(),
                isHidden = if (isHidden) 1 else 0,
                totalInstallments = parseOptionalCount(totalCountStr),
                firstDueDate = firstDueDate.trim().ifEmpty { null },
            ),
        )
        onClose()
    }

    val autoCount = autoCountHint(totalValue, installmentValue)

    TwModal(
        onDismiss = onClose,
        maxWidthRem = 28f,
        maxHeightFraction = 0.92f,
        shapeRem = 1f,
        scrimColor = Tw.Slate900.a(0.6f),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = rem(0.75f)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                TwText("ویرایش وام", 1f, 1.5f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1)
                TwText("اطلاعات وام را ویرایش کنید", 0.6875f, 1f, Tw.Slate400, maxLines = 1)
            }
            TwIconButton(R.drawable.ic_lucide_x, onClose, Tw.Slate400, iconRem = 1.25f)
        }
        HairLine(dk(Tw.Slate100, Tw.Slate800))

        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = rem(0.875f)),
            verticalArrangement = Arrangement.spacedBy(rem(0.875f)),
        ) {
            Column {
                DeptFieldLabel("عنوان وام:")
                TwTextField(
                    value = title, onValueChange = { title = it },
                    vPadRem = 0.5f, startPadRem = 0.75f, endPadRem = 0.75f,
                    bg = dk(Tw.Slate50, Tw.Slate800),
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(rem(0.75f))) {
                Column(Modifier.weight(1f)) {
                    AmountInput(value = totalValue, onChange = { totalValue = it }, unit = CurrencyUnit.TOMAN, label = "مبلغ کل مانده", placeholder = "۰")
                }
                Column(Modifier.weight(1f)) {
                    AmountInput(value = installmentValue, onChange = { installmentValue = it }, unit = CurrencyUnit.TOMAN, label = "مبلغ هر قسط", placeholder = "۰")
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(rem(0.75f))) {
                Column(Modifier.weight(1f)) {
                    DeptFieldLabel("تعداد اقساط (اختیاری):")
                    TwTextField(
                        value = totalCountStr, onValueChange = { totalCountStr = it.filterNumber() },
                        placeholder = if (autoCount != null) "خودکار: ${com.myaccounting.app.domain.JalaliDate.toPersianDigits(autoCount.toString())} قسط" else "مثلاً: ۱۲",
                        vPadRem = 0.5f, startPadRem = 0.75f, endPadRem = 0.75f,
                        bg = dk(Tw.Slate50, Tw.Slate800),
                        keyboardType = KeyboardType.Number,
                    )
                }
                Column(Modifier.weight(1f)) {
                    DeptFieldLabel("تاریخ اولین سررسید:")
                    PersianDateField(
                        value = firstDueDate,
                        onChange = { firstDueDate = it },
                        placeholder = "مثلاً: ۱۴۰۳/۱۲/۲۵",
                    )
                    if (dept.dueDay != null && firstDueDate.isEmpty()) {
                        TwText(
                            "روز ماهانه فعلی: ${com.myaccounting.app.domain.JalaliDate.toPersianDigits(dept.dueDay.toString())}م — با انتخاب تاریخ جایگزین می‌شود.",
                            0.625f, 0.875f, Tw.Slate400, maxLines = 1, modifier = Modifier.padding(top = rem(0.25f)),
                        )
                    }
                }
            }

            Row(
                Modifier.clickable(remember { MutableInteractionSource() }, null) { showMore = !showMore },
                horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LucideIcon(
                    if (showMore) R.drawable.ic_lucide_chevron_up else R.drawable.ic_lucide_chevron_down,
                    0.875f, dk(Tw.Slate500, Tw.Slate400),
                )
                TwText("گزینه‌های بیشتر (دوره، پروژه، توضیحات، راکد)", 0.6875f, 1f, dk(Tw.Slate500, Tw.Slate400), weight = FW.Semibold, maxLines = 1)
            }

            if (showMore) {
                Column(
                    Modifier.fillMaxWidth().padding(top = rem(0.375f)),
                    verticalArrangement = Arrangement.spacedBy(rem(0.875f)),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(rem(0.75f))) {
                        Column(Modifier.weight(1f)) {
                            DeptFieldLabel("دوره اقساط (روز):")
                            TwTextField(
                                value = periodDaysStr, onValueChange = { periodDaysStr = it.filterNumber() },
                                vPadRem = 0.5f, startPadRem = 0.75f, endPadRem = 0.75f,
                                bg = dk(Tw.Slate50, Tw.Slate800),
                                keyboardType = KeyboardType.Number,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            DeptFieldLabel("پروژه مرتبط:")
                            TwSelectField(
                                selectedUid = projectUid,
                                options = projects.map { it.uid to it.title },
                                placeholder = "عمومی / بدون پروژه",
                                onSelect = { projectUid = it },
                            )
                        }
                    }
                    Column {
                        DeptFieldLabel("توضیحات:")
                        TwTextField(
                            value = desc, onValueChange = { desc = it },
                            vPadRem = 0.5f, startPadRem = 0.75f, endPadRem = 0.75f,
                            bg = dk(Tw.Slate50, Tw.Slate800),
                        )
                    }
                    val boxShape = RoundedCornerShape(rem(0.75f))
                    Row(
                        Modifier.fillMaxWidth()
                            .background(dk(Tw.Slate50.a(0.7f), Tw.Slate800.a(0.4f)), boxShape)
                            .border(com.myaccounting.app.ui.theme.cssBorder(), dk(Tw.Slate200, Tw.Slate700), boxShape)
                            .clickable(remember { MutableInteractionSource() }, null) { isHidden = !isHidden }
                            .padding(rem(0.5f)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
                    ) {
                        TwCheckbox(checked = isHidden) { isHidden = !isHidden }
                        TwText("وضعیت راکد (مخفی از لیست اصلی)", 0.75f, 1f, dk(Tw.Slate700, Tw.Slate200), weight = FW.Semibold, maxLines = 1)
                    }
                }
            }

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
                        .background(Tw.Amber600, RoundedCornerShape(rem(0.75f)))
                        .clickable(remember { MutableInteractionSource() }, null) { submit() }
                        .padding(horizontal = rem(1.25f), vertical = rem(0.5f)),
                    horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LucideIcon(R.drawable.ic_lucide_save, 1f, Tw.White)
                    TwText("ذخیره تغییرات", 0.75f, 1f, Tw.White, weight = FW.Bold, maxLines = 1)
                }
            }
        }
    }
}

/** Keeps only digits, comma and dot (the web's type=number input). */
private fun String.filterNumber(): String = filter { it.isDigit() || it == ',' || it == '.' }

/** `label` of the loan forms: text-xs font-bold slate-700 dark:slate-300 mb-1. */
@Composable
private fun DeptFieldLabel(text: String) {
    Box(Modifier.fillMaxWidth().padding(bottom = rem(0.25f))) {
        TwText(text, 0.75f, 1f, dk(Tw.Slate700, Tw.Slate300), weight = FW.Bold, maxLines = 1)
    }
}
