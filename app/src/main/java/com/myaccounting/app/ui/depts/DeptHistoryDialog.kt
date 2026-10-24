package com.myaccounting.app.ui.depts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.myaccounting.app.R
import com.myaccounting.app.domain.Currency
import com.myaccounting.app.domain.CurrencyUnit
import com.myaccounting.app.domain.Dept
import com.myaccounting.app.domain.JalaliDate
import com.myaccounting.app.domain.LoanSchedule
import com.myaccounting.app.domain.Transaction
import com.myaccounting.app.ui.components.BankLogo
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.components.TwIconButton
import com.myaccounting.app.ui.components.TwModal
import com.myaccounting.app.ui.components.TwShadow
import com.myaccounting.app.ui.components.cssShadow
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem

private class DeptHistoryTx(val t: Transaction, val subjectTitle: String?, val accountTitle: String?, val bankName: String?, val bankUid: String?)

/**
 * DeptsView.tsx Transaction History Modal (z-60, `max-w-2xl max-h-[90vh]`, scrim slate-900/60):
 * the loan's paid-installment stats bar and its transaction list with expandable details.
 */
@Composable
fun DeptHistoryDialog(
    dept: Dept,
    transactions: List<Transaction>,
    paidSum: Double,
    remainingValue: Double,
    unit: CurrencyUnit,
    accountsById: Map<String, com.myaccounting.app.domain.Account>,
    banksById: Map<String, com.myaccounting.app.domain.Bank>,
    subjectsById: Map<String, com.myaccounting.app.domain.Subject>,
    onClose: () -> Unit,
    onPayInstallment: (String) -> Unit,
    onEditTransaction: (Transaction) -> Unit = {},
) {
    var expandedUid by remember { mutableStateOf<String?>(null) }
    val dbTxs = remember(transactions, dept.uid) {
        transactions.filter { it.deptUid == dept.uid }.sortedByDescending { it.dateTime }
    }
    val paidCount = remember(dept, transactions) {
        LoanSchedule.paidInstallmentsCount(dept, LoanSchedule.buildPaidIndex(transactions))
    }

    TwModal(
        onDismiss = onClose,
        maxWidthRem = 42f,          // max-w-2xl
        maxHeightFraction = 0.9f,   // max-h-[90vh]
        shapeRem = 1f,              // rounded-2xl
        scrimColor = Tw.Slate900.a(0.6f),
        cardBorder = null,          // overflow-hidden card, no border ring of its own content padding
    ) {
        // ---- header (p-4 border-b) -------------------------------------------------------------------------
        Row(
            Modifier.fillMaxWidth().padding(rem(1f)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.75f)), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(rem(2.5f)).background(dk(Tw.Amber100, Tw.Amber950), RoundedCornerShape(rem(0.75f))),
                    contentAlignment = Alignment.Center,
                ) { LucideIcon(R.drawable.ic_lucide_layers, 1.25f, dk(Tw.Amber700, Tw.Amber300)) }
                TwText(dept.title, 1f, 1.5f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f)), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.cssShadow(TwShadow.xs(), rem(0.75f))
                        .background(Tw.Amber600, RoundedCornerShape(rem(0.75f)))
                        .clickable(remember { MutableInteractionSource() }, null) { onClose(); onPayInstallment(dept.uid) }
                        .padding(horizontal = rem(0.875f), vertical = rem(0.5f)),
                    horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LucideIcon(R.drawable.ic_lucide_plus, 1f, Tw.White)
                    TwText("ثبت قسط", 0.75f, 1f, Tw.White, weight = FW.Bold, maxLines = 1)
                }
                TwIconButton(R.drawable.ic_lucide_x, onClose, Tw.Slate400, iconRem = 1.25f, padRem = 0.375f)
            }
        }

        // ---- stats bar (p-3 bg-slate-50 grid-cols-3) ---------------------------------------------------------
        Row(
            Modifier.fillMaxWidth()
                .background(dk(Tw.Slate50, Tw.Slate800.a(0.5f)))
                .padding(rem(0.75f)),
            horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
        ) {
            HistoryStat("کل پرداخت شده:", Currency.formatCurrency(paidSum, unit), dk(Tw.Emerald600, Tw.Emerald400), Modifier.weight(1f))
            HistoryStat("مانده فعلی:", Currency.formatCurrency(remainingValue, unit), dk(Tw.Amber600, Tw.Amber400), Modifier.weight(1f))
            HistoryStat("قسط ماهانه:", Currency.formatCurrency(dept.periodValue, unit), dk(Tw.Amber600, Tw.Amber400), Modifier.weight(1f))
        }

        // ---- transaction list --------------------------------------------------------------------------------
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f).padding(rem(1f)),
            verticalArrangement = Arrangement.spacedBy(rem(0.625f)),
        ) {
            item(key = "heading") {
                Row(Modifier.fillMaxWidth().padding(bottom = rem(0.5f)), horizontalArrangement = Arrangement.SpaceBetween) {
                    TwText("تراکنش‌های وام:", 0.75f, 1f, dk(Tw.Slate700, Tw.Slate300), weight = FW.Bold, maxLines = 1)
                    TwText(
                        "(${JalaliDate.toPersianDigits(paidCount.toString())} قسط)",
                        0.6875f, 1f, Tw.Slate400, maxLines = 1,
                    )
                }
            }
            if (dbTxs.isEmpty()) {
                item(key = "empty") {
                    Box(Modifier.fillMaxWidth().padding(vertical = rem(3f)), contentAlignment = Alignment.Center) {
                        TwText("هنوز قسطی ثبت نشده است.", 0.75f, 1f, dk(Tw.Slate500, Tw.Slate400), maxLines = 1)
                    }
                }
            }
            items(count = dbTxs.size, key = { dbTxs[it].uid }) { index ->
                val t = dbTxs[index]
                val account = t.sourceAccountUid?.let(accountsById::get)
                val subject = t.subjectUid?.let(subjectsById::get)
                val isPayment = t.transactionType == 1
                val expanded = expandedUid == t.uid
                val rowShape = RoundedCornerShape(rem(0.75f))
                Column(
                    Modifier.fillMaxWidth()
                        .background(dk(Tw.Slate50, Tw.Slate800.a(0.7f)), rowShape)
                        .border(cssBorder(), dk(Tw.Slate200.a(0.8f), Tw.Slate800), rowShape),
                ) {
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable(remember { MutableInteractionSource() }, null) { expandedUid = if (expanded) null else t.uid }
                            .padding(rem(0.75f)),
                        horizontalArrangement = Arrangement.spacedBy(rem(0.75f)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(rem(1.75f))
                                .background(if (isPayment) dk(Tw.Rose100, Tw.Rose950) else dk(Tw.Emerald100, Tw.Emerald950), RoundedCornerShape(rem(0.5f))),
                            contentAlignment = Alignment.Center,
                        ) {
                            LucideIcon(
                                R.drawable.ic_lucide_circle_check, 0.875f,
                                if (isPayment) dk(Tw.Rose600, Tw.Rose400) else dk(Tw.Emerald600, Tw.Emerald400),
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            TwText(
                                t.desc?.takeIf { it.isNotEmpty() } ?: subject?.title ?: "پرداخت قسط",
                                0.75f, 1f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Row(
                                Modifier.padding(top = rem(0.125f)),
                                horizontalArrangement = Arrangement.spacedBy(rem(0.25f)),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TwText(JalaliDate.toPersianDigits(t.persianDate), 0.625f, 0.875f, Tw.Slate400, maxLines = 1)
                                t.time?.takeIf { it.isNotEmpty() }?.let {
                                    TwText("• " + JalaliDate.toPersianDigits(it), 0.625f, 0.875f, Tw.Slate400, maxLines = 1)
                                }
                                if (account != null) {
                                    TwText("•", 0.625f, 0.875f, Tw.Slate400, maxLines = 1)
                                    BankLogo(banksById[account.bankUid]?.title ?: account.title, account.bankUid, sizeRem = 1.25f, padRem = 0.0625f)
                                    TwText(account.title, 0.625f, 0.875f, Tw.Slate400, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f)), verticalAlignment = Alignment.CenterVertically) {
                            TwText(
                                Currency.formatCurrency(Math.abs(t.totalValue), unit),
                                0.75f, 1f,
                                if (isPayment) dk(Tw.Rose600, Tw.Rose400) else dk(Tw.Emerald600, Tw.Emerald400),
                                weight = FW.Black, maxLines = 1,
                            )
                            LucideIcon(
                                R.drawable.ic_lucide_chevron_down, 0.875f, Tw.Slate400,
                            )
                        }
                    }

                    if (expanded) {
                        Column(
                            Modifier.fillMaxWidth().padding(start = rem(0.75f), end = rem(0.75f), bottom = rem(0.75f)),
                        ) {
                            Box(Modifier.fillMaxWidth().background(dk(Tw.Slate200.a(0.6f), Tw.Slate700.a(0.6f))))
                            Column(Modifier.fillMaxWidth().padding(top = rem(0.25f)), verticalArrangement = Arrangement.spacedBy(rem(0.375f))) {
                                HistoryDetail("حساب:", account?.title ?: "—")
                                HistoryDetail("سرفصل:", subject?.title ?: "—")
                                HistoryDetail(
                                    "تاریخ:",
                                    JalaliDate.toPersianDigits(t.persianDate) + (t.time?.takeIf { it.isNotEmpty() }?.let { " • " + JalaliDate.toPersianDigits(it) } ?: ""),
                                )
                                HistoryDetail("مبلغ:", Currency.formatCurrency(Math.abs(t.totalValue), unit))
                                if (!t.desc.isNullOrEmpty()) HistoryDetail("شرح:", t.desc)
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                    Row(
                                        Modifier.background(dk(Tw.Sky50, Tw.Sky950.a(0.6f)), RoundedCornerShape(rem(0.5f)))
                                            .border(cssBorder(), dk(Tw.Sky200, Tw.Sky800), RoundedCornerShape(rem(0.5f)))
                                            .clickable(remember { MutableInteractionSource() }, null) { onClose(); onEditTransaction(t) }
                                            .padding(horizontal = rem(0.75f), vertical = rem(0.375f)),
                                        horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        LucideIcon(R.drawable.ic_lucide_pencil, 0.875f, dk(Tw.Sky600, Tw.Sky400))
                                        TwText("ویرایش تراکنش", 0.6875f, 1f, dk(Tw.Sky600, Tw.Sky400), weight = FW.Bold, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---- footer (p-3 border-t) ----------------------------------------------------------------------------
        Row(
            Modifier.fillMaxWidth()
                .background(dk(Tw.Slate50.a(0.8f), Tw.Slate900.a(0.8f)))
                .padding(rem(0.75f)),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(
                Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClose)
                    .padding(horizontal = rem(1f), vertical = rem(0.5f)),
            ) { TwText("بستن", 0.75f, 1f, dk(Tw.Slate600, Tw.Slate300), weight = FW.Semibold, maxLines = 1) }
        }
    }
}

@Composable
private fun HistoryStat(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(rem(0.75f))
    Column(
        modifier
            .background(dk(Tw.White, Tw.Slate900), shape)
            .border(cssBorder(), dk(Tw.Slate200.a(0.7f), Tw.Slate800), shape)
            .padding(rem(0.5f)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TwText(label, 0.625f, 0.875f, Tw.Slate400, maxLines = 1)
        TwText(value, 0.75f, 1f, valueColor, weight = FW.Bold, maxLines = 1)
    }
}

@Composable
private fun HistoryDetail(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(rem(0.375f))) {
        TwText(label, 0.6875f, 1f, Tw.Slate400, maxLines = 1)
        TwText(value, 0.6875f, 1f, dk(Tw.Slate700, Tw.Slate200), weight = FW.Semibold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
