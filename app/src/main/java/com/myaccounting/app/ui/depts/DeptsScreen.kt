package com.myaccounting.app.ui.depts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import com.myaccounting.app.R
import com.myaccounting.app.domain.Currency
import com.myaccounting.app.domain.CurrencyUnit
import com.myaccounting.app.domain.Dept
import com.myaccounting.app.domain.JalaliDate
import com.myaccounting.app.domain.LoanSchedule
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem
import com.myaccounting.app.viewmodel.MainViewModel

/** Per-loan numbers, the same ones `getDeptInstallmentStats` builds in DeptsView.tsx. */
private class DeptStat(
    val paidSum: Double,
    val remainingValue: Double,
    val remainingInstallments: Int,
    val percentPaid: Int,
    val isSettled: Boolean,
    val isOverdue: Boolean,
    val nextDueMs: Long,
)

/**
 * DeptsView.tsx ported to Compose: header, hidden toggle, real installment statistics
 * (paid / remaining / progress / next due) and the per-loan action buttons.
 */
@Composable
fun DeptsScreen(
    state: MainViewModel.UiState,
    vm: MainViewModel,
    onPayInstallment: (String) -> Unit = {},
    onEditTransaction: (com.myaccounting.app.domain.Transaction) -> Unit = {},
) {
    if (!state.loaded) return
    val db = state.shownDb
    val unit = state.effectiveUnit
    var showHidden by rememberSaveable { mutableStateOf(false) }
    val nowMs = remember { System.currentTimeMillis() }
    // Add New Loan Modal / Edit Dept Modal (DeptsView.tsx).
    var addDialogOpen by remember { mutableStateOf(false) }
    var editingDept by remember { mutableStateOf<Dept?>(null) }
    // Transaction History Modal for Dept.
    var historyDept by remember { mutableStateOf<Dept?>(null) }

    val paidIndex = remember(db.transactions) { LoanSchedule.buildPaidIndex(db.transactions) }
    val projectsById = remember(db.projects) { db.projects.associate { it.uid to it.title } }

    val stats: Map<String, DeptStat> = remember(db.depts, paidIndex, nowMs) {
        db.depts.associate { d ->
            val paidSum = LoanSchedule.totalPaidSoFar(d, paidIndex)
            val total = if (d.value.isFinite()) Math.max(0.0, d.value) else 0.0
            val remainingValue = Math.max(0.0, total - paidSum)
            val percent = if (total > 0) {
                Math.min(100, Math.round(paidSum / total * 100).toInt())
            } else if (paidSum > 0) 100 else 0
            val settled = remainingValue <= 0
            val due = LoanSchedule.nextDueDateMs(d, paidIndex, nowMs)
            d.uid to DeptStat(
                paidSum = paidSum,
                remainingValue = remainingValue,
                remainingInstallments = LoanSchedule.remainingInstallmentsCount(d, paidIndex),
                percentPaid = percent,
                isSettled = settled,
                isOverdue = !settled && due < nowMs,
                nextDueMs = due,
            )
        }
    }

    val displayed = db.depts.filter { showHidden || it.isHidden != 1 }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = rem(1f), end = rem(1f), top = rem(1f), bottom = rem(3f)),
        verticalArrangement = Arrangement.spacedBy(rem(1.5f)),
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TwText("اقساط و وام‌های بانکی", 1.25f, 1.75f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f))) {
                    Row(
                        Modifier.background(Tw.Amber600, RoundedCornerShape(rem(0.75f)))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { addDialogOpen = true }
                            .padding(horizontal = rem(0.875f), vertical = rem(0.5f)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
                    ) {
                        LucideIcon(R.drawable.ic_lucide_plus, 0.875f, Tw.White)
                        TwText("افزودن وام جدید", 0.75f, 1f, Tw.White, weight = FW.Semibold, maxLines = 1)
                    }
                    Row(
                        Modifier.background(dk(Tw.Slate100, Tw.Slate800), RoundedCornerShape(rem(0.75f)))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showHidden = !showHidden }
                            .padding(horizontal = rem(0.75f), vertical = rem(0.5f)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
                    ) {
                        LucideIcon(if (showHidden) R.drawable.ic_lucide_eye_off else R.drawable.ic_lucide_eye, 1f, dk(Tw.Slate700, Tw.Slate200))
                        TwText("راکد", 0.75f, 1f, dk(Tw.Slate700, Tw.Slate200), weight = FW.Semibold, maxLines = 1)
                    }
                }
            }
        }

        if (displayed.isEmpty()) {
            item(key = "empty") {
                val shape = RoundedCornerShape(rem(1f))
                Column(
                    Modifier.fillMaxWidth()
                        .background(dk(Tw.White, Tw.Slate900), shape)
                        .border(cssBorder(), dk(Tw.Slate200.a(0.8f), Tw.Slate800), shape)
                        .padding(vertical = rem(3f), horizontal = rem(1f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(rem(0.5f)),
                ) {
                    LucideIcon(R.drawable.ic_lucide_layers, 2.5f, Tw.Slate400)
                    TwText("وامی برای نمایش وجود ندارد", 0.875f, 1.25f, dk(Tw.Slate700, Tw.Slate300), weight = FW.Bold)
                    TwText(
                        if (db.depts.isNotEmpty()) "همه وام‌ها راکد و مخفی‌اند؛ دکمه «راکد» را بزنید." else "هنوز وامی ثبت نکرده‌اید.",
                        0.75f, 1.25f, Tw.Slate500,
                    )
                }
            }
        }

        items(count = displayed.size, key = { displayed[it].uid }) { index ->
            val dept = displayed[index]
            DeptCard(
                dept = dept,
                stat = stats[dept.uid],
                projectTitle = dept.projectUid?.let { projectsById[it] },
                unit = unit,
                onPay = { onPayInstallment(dept.uid) },
                onEdit = { editingDept = dept },
                onHistory = { historyDept = dept },
            )
        }
    }

    if (addDialogOpen) {
        DeptAddDialog(
            unit = unit,
            projects = db.projects,
            onClose = { addDialogOpen = false },
            onAdd = { vm.saveDept(it) },
            onError = { vm.warn(it) },
        )
    }
    editingDept?.let { dept ->
        DeptEditDialog(
            dept = dept,
            projects = db.projects,
            onClose = { editingDept = null },
            onUpdate = { vm.saveDept(it) },
            onError = { vm.warn(it) },
        )
    }
    historyDept?.let { dept ->
        val accountsById = remember(db.accounts) { db.accounts.associateBy { it.uid } }
        val banksById = remember(db.banks) { db.banks.associateBy { it.uid } }
        val subjectsById = remember(db.subjects) { db.subjects.associateBy { it.uid } }
        val st = stats[dept.uid]
        DeptHistoryDialog(
            dept = dept,
            transactions = db.transactions,
            paidSum = st?.paidSum ?: 0.0,
            remainingValue = st?.remainingValue ?: dept.value,
            unit = unit,
            accountsById = accountsById,
            banksById = banksById,
            subjectsById = subjectsById,
            onClose = { historyDept = null },
            onPayInstallment = onPayInstallment,
            onEditTransaction = {
                // مثل وب: مودال تاریخچه بسته و تراکنش برای ویرایش به App.tsx می‌رود
                historyDept = null
                onEditTransaction(it)
            },
        )
    }
}

@Composable
private fun DeptCard(
    dept: Dept,
    stat: DeptStat?,
    projectTitle: String?,
    unit: CurrencyUnit,
    onPay: () -> Unit,
    onEdit: () -> Unit,
    onHistory: () -> Unit,
) {
    val s = stat ?: DeptStat(0.0, 0.0, 0, 0, false, false, 0L)
    val shape = RoundedCornerShape(rem(0.75f))
    Column(
        Modifier.fillMaxWidth()
            .background(dk(Tw.White, Tw.Slate900), shape)
            .border(
                if (s.isOverdue) rem(0.125f) else cssBorder(),
                if (s.isOverdue) dk(Tw.Rose400, Tw.Rose500) else dk(Tw.Slate200.a(0.8f), Tw.Slate800),
                shape,
            )
            .padding(rem(1f)),
        verticalArrangement = Arrangement.spacedBy(rem(0.75f)),
    ) {
        // Header: grip + title/badges (start) and remaining amount + edit (end).
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(rem(0.75f)),
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
            ) {
                Box(Modifier.padding(top = rem(0.125f))) {
                    LucideIcon(R.drawable.ic_lucide_grip_vertical, 0.875f, Tw.Slate300)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(rem(0.125f))) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
                    ) {
                        TwText(
                            dept.title, 0.75f, 1.25f, dk(Tw.Slate800, Tw.Slate100),
                            weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (s.isSettled) Badge("تسویه شده", R.drawable.ic_lucide_circle_check, Tone.Emerald)
                        if (s.isOverdue) Badge("سررسید گذشته", R.drawable.ic_lucide_alert_circle, Tone.Rose)
                    }
                    if (!projectTitle.isNullOrEmpty()) {
                        TwText("پروژه: " + projectTitle, 0.625f, 1f, Tw.Slate400, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    TwText(
                        Currency.formatCurrency(s.remainingValue, unit),
                        0.75f, 1.25f,
                        if (s.isSettled) dk(Tw.Emerald600, Tw.Emerald400) else dk(Tw.Amber600, Tw.Amber400),
                        weight = FW.Bold, maxLines = 1,
                    )
                    TwText("مانده کل", 0.625f, 1f, Tw.Slate400, maxLines = 1)
                }
                Box(
                    Modifier.size(rem(1.75f))
                        .background(dk(Tw.Slate100, Tw.Slate800), RoundedCornerShape(rem(0.5f)))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onEdit),
                    contentAlignment = Alignment.Center,
                ) {
                    LucideIcon(R.drawable.ic_lucide_settings, 1f, Tw.Slate400)
                }
            }
        }

        // Installment counts + next due, then the progress track.
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(rem(0.375f))) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TwText(
                    if (s.isSettled) "تمامی اقساط پرداخت شده"
                    else JalaliDate.toPersianDigits(s.remainingInstallments) + " قسط مانده",
                    0.6875f, 1f, dk(Tw.Slate600, Tw.Slate300), weight = FW.Semibold, maxLines = 1,
                )
                if (!s.isSettled) {
                    TwText(
                        "سررسید بعدی: " + JalaliDate.formatJalaliDayMonth(s.nextDueMs),
                        0.625f, 1f, Tw.Slate400, maxLines = 1,
                    )
                }
            }
            Box(
                Modifier.fillMaxWidth().height(rem(0.5f))
                    .background(dk(Tw.Slate100, Tw.Slate800), RoundedCornerShape(rem(0.25f))),
            ) {
                val fraction = if (s.isSettled) 1f else (Math.max(5, s.percentPaid) / 100f)
                Box(
                    Modifier.fillMaxHeight().fillMaxWidth(fraction)
                        .background(
                            if (s.isSettled) Brush.horizontalGradient(listOf(Tw.Emerald500, Tw.Emerald500))
                            else Brush.horizontalGradient(listOf(Tw.Amber500, Tw.Amber400)),
                            RoundedCornerShape(rem(0.25f)),
                        ),
                )
            }
        }

        // Footer actions: register installment payment / open the loan's transaction list.
        Row(
            Modifier.fillMaxWidth().padding(top = rem(0.5f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                Modifier.background(dk(Tw.Amber50, Tw.Amber950.a(0.4f)), RoundedCornerShape(rem(0.5f)))
                    .border(cssBorder(), dk(Tw.Amber200, Tw.Amber800), RoundedCornerShape(rem(0.5f)))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onPay)
                    .padding(horizontal = rem(0.625f), vertical = rem(0.375f)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
            ) {
                LucideIcon(R.drawable.ic_lucide_circle_plus, 0.875f, dk(Tw.Amber600, Tw.Amber400))
                TwText("ثبت تراکنش", 0.6875f, 1f, dk(Tw.Amber700, Tw.Amber300), weight = FW.Bold, maxLines = 1)
            }
            Row(
                Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onHistory)
                    .padding(vertical = rem(0.25f)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(rem(0.25f)),
            ) {
                TwText("ریز تراکنش‌ها", 0.6875f, 1f, Tw.Slate400, maxLines = 1)
                LucideIcon(R.drawable.ic_lucide_chevron_left, 0.875f, Tw.Slate400)
            }
        }
    }
}

/** Tailwind `bg-{tone}-100 / text-{tone}-700` pill used for the settled / overdue badges. */
@Composable
private fun Badge(label: String, icon: Int, tone: Tone) {
    val bg = when (tone) {
        Tone.Emerald -> dk(Tw.Emerald100, Tw.Emerald950.a(0.6f))
        Tone.Rose -> dk(Tw.Rose100, Tw.Rose950.a(0.6f))
    }
    val fg = when (tone) {
        Tone.Emerald -> dk(Tw.Emerald700, Tw.Emerald300)
        Tone.Rose -> dk(Tw.Rose700, Tw.Rose300)
    }
    val br = when (tone) {
        Tone.Emerald -> dk(Tw.Emerald200, Tw.Emerald800)
        Tone.Rose -> dk(Tw.Rose200, Tw.Rose800)
    }
    Row(
        Modifier.background(bg, RoundedCornerShape(rem(999f)))
            .border(cssBorder(), br, RoundedCornerShape(rem(999f)))
            .padding(horizontal = rem(0.5f), vertical = rem(0.125f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(rem(0.125f)),
    ) {
        LucideIcon(icon, 0.75f, fg)
        TwText(label, 0.625f, 1f, fg, weight = FW.Bold, maxLines = 1)
    }
}

private enum class Tone { Emerald, Rose }
