package com.myaccounting.app.ui.checks

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.myaccounting.app.R
import com.myaccounting.app.domain.Check
import com.myaccounting.app.domain.CheckKind
import com.myaccounting.app.domain.CheckStatus
import com.myaccounting.app.domain.Checks
import com.myaccounting.app.domain.Currency
import com.myaccounting.app.domain.CurrencyUnit
import com.myaccounting.app.domain.JalaliDate
import com.myaccounting.app.ui.components.ConfirmDialog
import com.myaccounting.app.ui.components.ConfirmVariant
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem
import com.myaccounting.app.viewmodel.MainViewModel

private enum class StatusFilter { PENDING, HISTORY, ALL }

private data class PendingConfirm(
    val message: String,
    val variant: ConfirmVariant = ConfirmVariant.DANGER,
    val onConfirm: () -> Unit,
)

/** «چک‌ها» — port of ChecksView.tsx: totals, filters, foldable cards with conditions and actions. */
@Composable
fun ChecksScreen(
    state: MainViewModel.UiState,
    vm: MainViewModel,
) {
    if (!state.loaded) return
    val db = state.shownDb
    val unit = state.effectiveUnit
    var statusFilter by rememberSaveable { mutableStateOf(StatusFilter.PENDING) }
    var kindFilter by rememberSaveable { mutableStateOf("all") }
    var expandedUid by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingConfirm by remember { mutableStateOf<PendingConfirm?>(null) }

    val nowMs = remember { System.currentTimeMillis() }
    val txIndex = remember(db.transactions) { Checks.buildTxByUidIndex(db.transactions) }
    val personsById = remember(db.persons) { db.persons.associateBy { it.uid } }
    val accountsById = remember(db.accounts) { db.accounts.associateBy { it.uid } }
    val subjectsById = remember(db.subjects) { db.subjects.associateBy { it.uid } }
    val projectsById = remember(db.projects) { db.projects.associateBy { it.uid } }
    val totals = remember(db.checks, db.transactions) { Checks.pendingChecksTotals(db) }

    val visible: List<Check> = remember(db.checks, statusFilter, kindFilter) {
        db.checks
            .filter { c ->
                when (statusFilter) {
                    StatusFilter.PENDING -> c.status == CheckStatus.PENDING
                    StatusFilter.HISTORY -> c.status != CheckStatus.PENDING
                    StatusFilter.ALL -> true
                } && (kindFilter == "all" || c.kind.json == kindFilter)
            }
            .sortedWith(
                compareBy<Check> { if (it.status == CheckStatus.PENDING) 0 else 1 }
                    .thenComparator { a, b ->
                        if (a.status == CheckStatus.PENDING) {
                            Checks.checkDeadlineMs(a).compareTo(Checks.checkDeadlineMs(b))
                        } else {
                            Checks.checkDeadlineMs(b).compareTo(Checks.checkDeadlineMs(a))
                        }
                    },
            )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = rem(1f), end = rem(1f), top = rem(1f), bottom = rem(3f)),
        verticalArrangement = Arrangement.spacedBy(rem(1.25f)),
    ) {
        item(key = "header") {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TwText("چک‌ها", 1.25f, 1.75f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1)
                Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f)), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(rem(2.25f))
                            .background(dk(Tw.Slate100, Tw.Slate800), RoundedCornerShape(rem(0.75f)))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                vm.notPorted("تنظیمات یادآوری چک‌ها")
                            },
                        contentAlignment = Alignment.Center,
                    ) { LucideIcon(R.drawable.ic_lucide_bell, 1f, dk(Tw.Slate600, Tw.Slate300)) }
                    Row(
                        Modifier.background(Tw.Amber600, RoundedCornerShape(rem(0.75f)))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                vm.notPorted("چک جدید")
                            }
                            .padding(horizontal = rem(0.875f), vertical = rem(0.5f)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
                    ) {
                        LucideIcon(R.drawable.ic_lucide_plus, 0.875f, Tw.White)
                        TwText("چک جدید", 0.75f, 1f, Tw.White, weight = FW.Semibold, maxLines = 1)
                    }
                }
            }
        }

        item(key = "totals") {
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.75f))) {
                SummaryTile(
                    label = "چک‌های خودم (پاس‌نشده)",
                    value = Currency.formatCurrency(totals.ownTotal, unit),
                    valueColor = dk(Tw.Slate800, Tw.Slate100),
                    modifier = Modifier.weight(1f),
                )
                SummaryTile(
                    label = "مانده واریز برای چک اشخاص",
                    value = Currency.formatCurrency(totals.personRemaining, unit),
                    valueColor = dk(Tw.Amber600, Tw.Amber400),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item(key = "filters") {
            Column(verticalArrangement = Arrangement.spacedBy(rem(0.5f))) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
                ) {
                    FilterChip("در انتظار", statusFilter == StatusFilter.PENDING) { statusFilter = StatusFilter.PENDING }
                    FilterChip("تاریخچه", statusFilter == StatusFilter.HISTORY) { statusFilter = StatusFilter.HISTORY }
                    FilterChip("همه", statusFilter == StatusFilter.ALL) { statusFilter = StatusFilter.ALL }
                    Box(Modifier.size(width = cssBorder(), height = rem(1.25f)).background(dk(Tw.Slate200, Tw.Slate700)))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f))) {
                    FilterChip("همه چک‌ها", kindFilter == "all") { kindFilter = "all" }
                    FilterChip("خودم", kindFilter == CheckKind.OWN.json) { kindFilter = CheckKind.OWN.json }
                    FilterChip("اشخاص", kindFilter == CheckKind.PERSON.json) { kindFilter = CheckKind.PERSON.json }
                }
            }
        }

        if (visible.isEmpty()) {
            item(key = "empty") {
                val card = RoundedCornerShape(rem(1f))
                Column(
                    Modifier.fillMaxWidth()
                        .background(dk(Tw.White, Tw.Slate900), card)
                        .border(cssBorder(), dk(Tw.Slate200.a(0.8f), Tw.Slate800), card)
                        .padding(vertical = rem(3f), horizontal = rem(1f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(rem(0.5f)),
                ) {
                    LucideIcon(R.drawable.ic_lucide_wallet, 2.5f, Tw.Slate400)
                    TwText("چکی برای نمایش وجود ندارد", 0.875f, 1.25f, dk(Tw.Slate700, Tw.Slate300), weight = FW.Bold)
                    TwText("با دکمه «چک جدید» چک‌های آینده‌ات را ثبت کن.", 0.75f, 1.25f, Tw.Slate500)
                }
            }
        }

        items(count = visible.size, key = { visible[it].uid }) { index ->
            val c = visible[index]
            val funded = Checks.checkFundedAmount(c, db.transactions, txIndex)
            val remaining = Checks.checkRemaining(c, db.transactions, txIndex)
            val urgency = Checks.checkUrgency(c, db.transactions, nowMs, 3, txIndex)
            val expanded = expandedUid == c.uid
            CheckCard(
                check = c,
                unit = unit,
                funded = funded,
                remaining = remaining,
                urgency = urgency,
                nowMs = nowMs,
                expanded = expanded,
                accountTitle = c.accountUid?.let { accountsById[it]?.title } ?: "",
                drawerName = c.drawerPersonUid?.let { personsById[it]?.fullName } ?: "",
                payeeName = c.payeePersonUid?.let { personsById[it]?.fullName } ?: "",
                subjectTitle = c.subjectUid?.let { subjectsById[it]?.title } ?: "",
                projectTitle = c.projectUid?.let { projectsById[it]?.title } ?: "",
                deposits = c.deposits.mapNotNull { d ->
                    val tx = txIndex[d.transactionUid] ?: return@mapNotNull null
                    DepositRow(
                        date = JalaliDate.toPersianDigits(tx.persianDate),
                        account = accountsById[d.accountUid]?.title ?: "",
                        amount = Math.abs(tx.totalValue),
                    )
                },
                onToggle = { expandedUid = if (expanded) null else c.uid },
                onPass = {
                    pendingConfirm = PendingConfirm(
                        "پاس شدن چک به مبلغ ${Currency.formatCurrency(Math.abs(c.amount), unit)} ثبت شود؟",
                    ) { vm.passOwnCheck(c.uid) }
                },
                onSetStatus = { status ->
                    pendingConfirm = PendingConfirm(
                        when (status) {
                            CheckStatus.PASSED -> if (c.kind == CheckKind.PERSON && remaining > 0) {
                                "هنوز ${Currency.formatCurrency(remaining, unit)} از این چک واریز نشده.\nبا این حال پاس‌شده ثبت شود؟"
                            } else "این چک پاس‌شده ثبت شود؟"
                            CheckStatus.BOUNCED -> "این چک برگشتی ثبت شود؟"
                            else -> "این چک باطل شود؟"
                        },
                        variant = ConfirmVariant.WARNING,
                    ) { vm.setCheckStatus(c.uid, status) }
                },
                onRevert = {
                    pendingConfirm = PendingConfirm(
                        if (c.kind == CheckKind.OWN && c.status == CheckStatus.PASSED && !c.passedTransactionUid.isNullOrEmpty())
                            "چک به «در انتظار» برمی‌گردد و تراکنش پاس شدن آن حذف می‌شود (مبلغ به حساب برمی‌گردد)."
                        else "چک به «در انتظار» برگردد؟",
                        variant = ConfirmVariant.WARNING,
                    ) { vm.revertCheckToPending(c.uid) }
                },
                onDelete = {
                    pendingConfirm = PendingConfirm(
                        if (!c.passedTransactionUid.isNullOrEmpty() || c.deposits.isNotEmpty())
                            "این چک حذف شود؟\nتراکنش‌های پاس شدن/واریز ثبت‌شده برای آن حذف نمی‌شوند و در حساب‌ها می‌مانند."
                        else "آیا از حذف این چک مطمئنید؟",
                    ) { vm.deleteCheck(c.uid) }
                },
                onEdit = { vm.notPorted("ویرایش چک") },
                onDeposit = { vm.notPorted("ثبت واریز چک") },
            )
        }
    }

    pendingConfirm?.let { req ->
        ConfirmDialog(
            message = req.message,
            onConfirm = { req.onConfirm(); pendingConfirm = null },
            onCancel = { pendingConfirm = null },
            confirmLabel = "تأیید",
            variant = req.variant,
        )
    }
}

private data class DepositRow(val date: String, val account: String, val amount: Double)

@Composable
private fun SummaryTile(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    val card = RoundedCornerShape(rem(1f))
    Column(
        modifier.fillMaxWidth()
            .background(dk(Tw.White, Tw.Slate900), card)
            .border(cssBorder(), dk(Tw.Slate200.a(0.8f), Tw.Slate800), card)
            .padding(rem(0.75f)),
        verticalArrangement = Arrangement.spacedBy(rem(0.25f)),
    ) {
        TwText(label, 0.6875f, 1.25f, dk(Tw.Slate500, Tw.Slate400), maxLines = 2)
        TwText(value, 0.875f, 1.25f, valueColor, weight = FW.Black, maxLines = 1)
    }
}

@Composable
private fun FilterChip(label: String, active: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.background(
            if (active) Tw.Amber600 else dk(Tw.Slate100, Tw.Slate800),
            RoundedCornerShape(rem(0.75f)),
        ).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = rem(0.75f), vertical = rem(0.375f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TwText(
            label, 0.75f, 1f,
            if (active) Tw.White else dk(Tw.Slate600, Tw.Slate300),
            weight = FW.Semibold, maxLines = 1,
        )
    }
}

@Composable
private fun CheckCard(
    check: Check,
    unit: CurrencyUnit,
    funded: Double,
    remaining: Double,
    urgency: Checks.Urgency,
    nowMs: Long,
    expanded: Boolean,
    accountTitle: String,
    drawerName: String,
    payeeName: String,
    subjectTitle: String,
    projectTitle: String,
    deposits: List<DepositRow>,
    onToggle: () -> Unit,
    onPass: () -> Unit,
    onSetStatus: (CheckStatus) -> Unit,
    onRevert: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onDeposit: () -> Unit,
) {
    val isPerson = check.kind == CheckKind.PERSON
    val amount = Math.max(0.0, check.amount)
    val fullyFunded = isPerson && remaining <= 0
    val percent = if (isPerson && amount > 0) Math.min(100, Math.round(funded / amount * 100).toInt()) else 0
    val title = if (isPerson) {
        "چک " + (drawerName.ifEmpty { "شخص" })
    } else {
        "چک" + (check.checkNumber?.takeIf { it.isNotEmpty() }?.let { " شماره " + JalaliDate.toPersianDigits(it) } ?: "")
    }
    val card = RoundedCornerShape(rem(1f))

    Column(
        Modifier.fillMaxWidth()
            .background(dk(Tw.White, Tw.Slate900), card)
            .border(
                cssBorder(),
                if (urgency == Checks.Urgency.OVERDUE) dk(Tw.Rose300, Tw.Rose900) else dk(Tw.Slate200.a(0.8f), Tw.Slate800),
                card,
            ),
    ) {
        Column(
            Modifier.fillMaxWidth()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggle)
                .padding(rem(1f)),
            verticalArrangement = Arrangement.spacedBy(rem(0.625f)),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(rem(0.75f))) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(rem(0.25f))) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
                    ) {
                        TwText(
                            title, 0.875f, 1.25f, dk(Tw.Slate800, Tw.Slate100),
                            weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        StatusBadge(if (check.status == CheckStatus.PENDING && fullyFunded) "واریز کامل" else statusLabel(check.status), check.status)
                        Row(
                            Modifier.background(dk(Tw.Slate100, Tw.Slate800), RoundedCornerShape(rem(999f)))
                                .padding(horizontal = rem(0.5f), vertical = rem(0.125f)),
                        ) {
                            TwText(
                                if (isPerson) "صادرشده توسط شخص" else "خودم",
                                0.625f, 1f, dk(Tw.Slate500, Tw.Slate400), weight = FW.Semibold, maxLines = 1,
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TwText(
                            "سررسید: " + JalaliDate.toPersianDigits(check.dueDate),
                            0.6875f, 1.25f, dk(Tw.Slate500, Tw.Slate400), maxLines = 1,
                        )
                        if (isPerson) {
                            TwText(
                                " • مهلت واریز: " + JalaliDate.toPersianDigits(fundByText(check)),
                                0.6875f, 1.25f, dk(Tw.Slate500, Tw.Slate400), maxLines = 1,
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    TwText(
                        Currency.formatCurrency(amount, unit),
                        0.875f, 1.25f, dk(Tw.Slate900, Tw.Slate100), weight = FW.Black, maxLines = 1,
                    )
                    if (urgency != Checks.Urgency.DONE && !fullyFunded) {
                        Box(Modifier.padding(top = rem(0.25f))) {
                            UrgencyBadge(urgency, urgencyText(urgency, Checks.checkDeadlineMs(check), if (isPerson) "مهلت" else "سررسید", nowMs))
                        }
                    }
                }
            }

            if (isPerson && check.status == CheckStatus.PENDING) {
                Column(verticalArrangement = Arrangement.spacedBy(rem(0.25f))) {
                    Box(
                        Modifier.fillMaxWidth().height(rem(0.375f))
                            .background(dk(Tw.Slate100, Tw.Slate800), RoundedCornerShape(rem(0.25f))),
                    ) {
                        Box(
                            Modifier.fillMaxHeight().fillMaxWidth(percent / 100f)
                                .background(
                                    if (fullyFunded) Tw.Emerald500 else Tw.Amber500,
                                    RoundedCornerShape(rem(0.25f)),
                                ),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TwText(
                            Currency.formatCurrency(funded, unit) + " از " + Currency.formatCurrency(amount, unit) + " واریز شده",
                            0.6875f, 1.25f, dk(Tw.Slate500, Tw.Slate400), maxLines = 1,
                        )
                        if (remaining > 0) {
                            TwText(
                                " • مانده " + Currency.formatCurrency(remaining, unit),
                                0.6875f, 1.25f, dk(Tw.Amber600, Tw.Amber400), weight = FW.Bold, maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        if (!expanded) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = rem(0.375f)),
                horizontalArrangement = Arrangement.Center,
            ) { LucideIcon(R.drawable.ic_lucide_chevron_down, 0.875f, dk(Tw.Slate300, Tw.Slate600)) }
        } else {
            CheckDetails(
                check = check,
                unit = unit,
                isPerson = isPerson,
                remaining = remaining,
                accountTitle = accountTitle,
                drawerName = drawerName,
                payeeName = payeeName,
                subjectTitle = subjectTitle,
                projectTitle = projectTitle,
                deposits = deposits,
                onPass = onPass,
                onSetStatus = onSetStatus,
                onRevert = onRevert,
                onDelete = onDelete,
                onEdit = onEdit,
                onDeposit = onDeposit,
            )
        }
    }
}

@Composable
private fun CheckDetails(
    check: Check,
    unit: CurrencyUnit,
    isPerson: Boolean,
    remaining: Double,
    accountTitle: String,
    drawerName: String,
    payeeName: String,
    subjectTitle: String,
    projectTitle: String,
    deposits: List<DepositRow>,
    onPass: () -> Unit,
    onSetStatus: (CheckStatus) -> Unit,
    onRevert: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onDeposit: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth()
            .padding(start = rem(1f), end = rem(1f), bottom = rem(1f), top = rem(0.75f)),
        verticalArrangement = Arrangement.spacedBy(rem(0.75f)),
    ) {
        Box(Modifier.fillMaxWidth().height(cssBorder()).background(dk(Tw.Slate100, Tw.Slate800)))
        // Two-column detail grid, exactly like `grid grid-cols-2 gap-x-4 gap-y-1.5`.
        val pairs = ArrayList<Pair<String, String>>()
        if (!isPerson && accountTitle.isNotEmpty()) pairs += "حساب" to accountTitle
        if (isPerson && drawerName.isNotEmpty()) pairs += "صادرکننده" to drawerName
        if (payeeName.isNotEmpty()) pairs += "گیرنده" to payeeName
        if (!check.checkNumber.isNullOrEmpty()) pairs += "شماره چک" to JalaliDate.toPersianDigits(check.checkNumber)
        if (subjectTitle.isNotEmpty()) pairs += "سرفصل" to subjectTitle
        if (projectTitle.isNotEmpty()) pairs += "پروژه" to projectTitle
        Column(verticalArrangement = Arrangement.spacedBy(rem(0.375f))) {
            var i = 0
            while (i < pairs.size) {
                val left = pairs[i]
                val right = if (i + 1 < pairs.size) pairs[i + 1] else null
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(rem(1f))) {
                    DetailCell(left.first, left.second, Modifier.weight(1f))
                    if (right != null) DetailCell(right.first, right.second, Modifier.weight(1f))
                    else Box(Modifier.weight(1f))
                }
                i += 2
            }
        }
        if (!check.desc.isNullOrEmpty()) DetailCell("توضیحات", check.desc)

        if (isPerson && deposits.isNotEmpty()) {
            Column(
                Modifier.fillMaxWidth()
                    .background(dk(Tw.Slate50, Tw.Slate800.a(0.6f)), RoundedCornerShape(rem(0.75f)))
                    .padding(rem(0.75f)),
                verticalArrangement = Arrangement.spacedBy(rem(0.375f)),
            ) {
                TwText("واریزها", 0.6875f, 1f, dk(Tw.Slate700, Tw.Slate200), weight = FW.Bold, maxLines = 1)
                for (d in deposits) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TwText(d.date + " • " + d.account, 0.6875f, 1.25f, dk(Tw.Slate600, Tw.Slate300), maxLines = 1)
                        TwText(Currency.formatCurrency(d.amount, unit), 0.6875f, 1.25f, dk(Tw.Slate600, Tw.Slate300), weight = FW.Bold, maxLines = 1)
                    }
                }
            }
        }

        if (!isPerson && check.status == CheckStatus.PASSED && check.passedAt != null) {
            TwText(
                "پاس‌شده در " + JalaliDate.toPersianDigits(JalaliDate.formatJalaliDate(check.passedAt)) + " و از حساب کسر شده است.",
                0.6875f, 1.25f, dk(Tw.Emerald600, Tw.Emerald400), maxLines = 2,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(rem(0.5f))) {
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f))) {
                if (check.status == CheckStatus.PENDING && !isPerson) {
                    ActionBtn("پاس شد", R.drawable.ic_lucide_check, BtnTone.Green, onPass)
                }
                if (check.status == CheckStatus.PENDING && isPerson) {
                    if (remaining > 0) ActionBtn("ثبت واریز", R.drawable.ic_lucide_building2, BtnTone.Amber, onDeposit)
                    ActionBtn("پاس شد", R.drawable.ic_lucide_check, BtnTone.Green) { onSetStatus(CheckStatus.PASSED) }
                }
                if (check.status == CheckStatus.PENDING) {
                    ActionBtn("ویرایش", R.drawable.ic_lucide_pencil, BtnTone.Slate, onEdit)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f))) {
                if (check.status == CheckStatus.PENDING) {
                    ActionBtn("برگشت خورد", R.drawable.ic_lucide_alert_circle, BtnTone.Rose) { onSetStatus(CheckStatus.BOUNCED) }
                    ActionBtn("باطل شد", R.drawable.ic_lucide_x, BtnTone.Slate) { onSetStatus(CheckStatus.CANCELLED) }
                } else {
                    ActionBtn("بازگرداندن به در انتظار", R.drawable.ic_lucide_rotate_ccw, BtnTone.Slate, onRevert)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f))) {
                ActionBtn("حذف", R.drawable.ic_lucide_trash2, BtnTone.Rose, onDelete)
            }
        }
    }
}

@Composable
private fun DetailCell(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.Top) {
        TwText(label + ": ", 0.6875f, 1.25f, Tw.Slate400, maxLines = 2)
        TwText(value, 0.6875f, 1.25f, dk(Tw.Slate700, Tw.Slate200), weight = FW.Semibold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

private enum class BtnTone { Green, Amber, Rose, Slate }

@Composable
private fun ActionBtn(label: String, icon: Int, tone: BtnTone, onClick: () -> Unit) {
    val bg: Color
    val fg: Color
    val br: Color
    when (tone) {
        BtnTone.Green -> {
            bg = dk(Tw.Emerald50, Tw.Emerald950.a(0.5f)); fg = dk(Tw.Emerald700, Tw.Emerald300); br = dk(Tw.Emerald200, Tw.Emerald900)
        }
        BtnTone.Amber -> {
            bg = dk(Tw.Amber50, Tw.Amber950.a(0.5f)); fg = dk(Tw.Amber700, Tw.Amber300); br = dk(Tw.Amber200, Tw.Amber900)
        }
        BtnTone.Rose -> {
            bg = dk(Tw.Rose50, Tw.Rose950.a(0.5f)); fg = dk(Tw.Rose700, Tw.Rose300); br = dk(Tw.Rose200, Tw.Rose900)
        }
        BtnTone.Slate -> {
            bg = dk(Tw.Slate50, Tw.Slate800); fg = dk(Tw.Slate700, Tw.Slate200); br = dk(Tw.Slate200, Tw.Slate700)
        }
    }
    Row(
        Modifier.background(bg, RoundedCornerShape(rem(0.75f)))
            .border(cssBorder(), br, RoundedCornerShape(rem(0.75f)))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = rem(0.75f), vertical = rem(0.375f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(rem(0.25f)),
    ) {
        LucideIcon(icon, 0.875f, fg)
        TwText(label, 0.75f, 1f, fg, weight = FW.Bold, maxLines = 1)
    }
}

@Composable
private fun StatusBadge(label: String, status: CheckStatus) {
    val bg: Color
    val fg: Color
    when (status) {
        CheckStatus.PENDING -> { bg = dk(Tw.Sky100, Tw.Sky950.a(0.6f)); fg = dk(Tw.Sky700, Tw.Sky300) }
        CheckStatus.PASSED -> { bg = dk(Tw.Emerald100, Tw.Emerald950.a(0.6f)); fg = dk(Tw.Emerald700, Tw.Emerald300) }
        CheckStatus.BOUNCED -> { bg = dk(Tw.Rose100, Tw.Rose950.a(0.6f)); fg = dk(Tw.Rose700, Tw.Rose300) }
        CheckStatus.CANCELLED -> { bg = dk(Tw.Slate200, Tw.Slate800); fg = dk(Tw.Slate600, Tw.Slate400) }
    }
    Row(
        Modifier.background(bg, RoundedCornerShape(rem(999f)))
            .padding(horizontal = rem(0.5f), vertical = rem(0.125f)),
    ) {
        TwText(label, 0.625f, 1f, fg, weight = FW.Bold, maxLines = 1)
    }
}

@Composable
private fun UrgencyBadge(urgency: Checks.Urgency, label: String) {
    val bg: Color
    val fg: Color
    when (urgency) {
        Checks.Urgency.OVERDUE -> { bg = dk(Tw.Rose100, Tw.Rose950.a(0.6f)); fg = dk(Tw.Rose700, Tw.Rose300) }
        Checks.Urgency.TODAY -> { bg = dk(Tw.Amber100, Tw.Amber950.a(0.6f)); fg = dk(Tw.Amber700, Tw.Amber300) }
        Checks.Urgency.SOON -> { bg = dk(Tw.Amber50, Tw.Amber950.a(0.4f)); fg = dk(Tw.Amber700, Tw.Amber300) }
        Checks.Urgency.LATER -> { bg = dk(Tw.Slate100, Tw.Slate800); fg = dk(Tw.Slate600, Tw.Slate300) }
        Checks.Urgency.DONE -> { bg = dk(Tw.Slate100, Tw.Slate800); fg = dk(Tw.Slate500, Tw.Slate400) }
    }
    Row(
        Modifier.background(bg, RoundedCornerShape(rem(999f)))
            .padding(horizontal = rem(0.5f), vertical = rem(0.125f)),
    ) {
        TwText(label, 0.625f, 1f, fg, weight = FW.Bold, maxLines = 1)
    }
}

private fun statusLabel(status: CheckStatus): String = when (status) {
    CheckStatus.PENDING -> "در انتظار"
    CheckStatus.PASSED -> "پاس شد"
    CheckStatus.BOUNCED -> "برگشتی"
    CheckStatus.CANCELLED -> "باطل"
}

private const val DAY_MS = 86_400_000L

private fun urgencyText(urgency: Checks.Urgency, deadlineMs: Long, prefix: String, nowMs: Long): String {
    if (urgency == Checks.Urgency.DONE) return ""
    val today = Checks.startOfToday(nowMs)
    val d = Math.round((deadlineMs - today).toDouble() / DAY_MS).toInt()
    return when {
        d < 0 -> "$prefix ${JalaliDate.toPersianDigits(-d)} روز گذشته"
        d == 0 -> "$prefix امروز"
        d == 1 -> "$prefix فردا"
        else -> "$prefix ${JalaliDate.toPersianDigits(d)} روز دیگر"
    }
}

private fun fundByText(check: Check): String {
    val explicit = check.fundByDate
    if (!explicit.isNullOrEmpty() && Checks.jalaliStrToMs(explicit) != null) return explicit
    return JalaliDate.formatJalaliDate(Checks.checkFundByMs(check))
}
