package com.myaccounting.app.ui.checks

import androidx.compose.foundation.background
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
import com.myaccounting.app.R
import com.myaccounting.app.domain.AccountingDatabase
import com.myaccounting.app.domain.Check
import com.myaccounting.app.domain.Checks
import com.myaccounting.app.domain.Currency
import com.myaccounting.app.domain.CurrencyUnit
import com.myaccounting.app.ui.components.AmountInput
import com.myaccounting.app.ui.components.BankLogo
import com.myaccounting.app.ui.components.HairLine
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.components.TwIconButton
import com.myaccounting.app.ui.components.TwModal
import com.myaccounting.app.ui.components.TwSelectorField
import com.myaccounting.app.ui.components.TwShadow
import com.myaccounting.app.ui.components.cssShadow
import com.myaccounting.app.ui.transactions.AccountPickerDialog
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem

/**
 * ChecksView.tsx DepositModal: record a partial deposit of a person check from an account
 * (each deposit creates an expense transaction on that account).
 */
@Composable
fun DepositDialog(
    check: Check,
    db: AccountingDatabase,
    unit: CurrencyUnit,
    balances: Map<String, Double>,
    onClose: () -> Unit,
    onSubmit: (accountUid: String, amount: Double) -> Unit,
    onError: (String) -> Unit = {},
) {
    val accounts = db.accounts.filter { it.isHidden != 1 }
    val remaining = Checks.checkRemaining(check, db.transactions)
    var accountUid by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf(remaining) }
    var accountOpen by remember { mutableStateOf(false) }

    val drawer = db.persons.firstOrNull { it.uid == check.drawerPersonUid }?.fullName ?: "شخص"
    val accountsById = remember(db.accounts) { db.accounts.associateBy { it.uid } }
    val banksById = remember(db.banks) { db.banks.associateBy { it.uid } }
    val depositAccount = accountsById[accountUid]
    val amberAction = dk(Tw.Amber600, Tw.Amber400)

    fun submit() {
        if (accountUid.isEmpty()) { onError("لطفاً حساب مبدأ را انتخاب کنید."); return }
        if (!(amount > 0)) { onError("لطفاً مبلغ واریز را وارد کنید."); return }
        if (amount > remaining) { onError("مبلغ واریز بیشتر از مانده چک است."); return }
        onSubmit(accountUid, kotlin.math.round(amount))
        onClose()
    }

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
                TwText("ثبت واریز", 1f, 1.5f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1)
                TwText(
                    "برای چک $drawer • مانده ${Currency.formatCurrency(remaining, unit)}",
                    0.6875f, 1f, Tw.Slate400, maxLines = 1,
                )
            }
            TwIconButton(R.drawable.ic_lucide_x, onClose, Tw.Slate400, iconRem = 1.25f)
        }
        HairLine(dk(Tw.Slate100, Tw.Slate800))

        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = rem(0.875f)),
        ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(rem(0.875f))) {
                // ---- از حساب --------------------------------------------------------------------------------
                Column {
                    DepositFieldLabel("از حساب:")
                    TwSelectorField(
                        text = depositAccount?.title?.ifEmpty { "انتخاب شده" } ?: "انتخاب حساب...",
                        placeholder = depositAccount == null,
                        action = "تغییر",
                        onClick = { accountOpen = true },
                        actionColor = amberAction,
                    ) {
                        if (depositAccount != null) {
                            BankLogo(banksById[depositAccount.bankUid]?.title ?: depositAccount.title, depositAccount.bankUid, sizeRem = 1.5f, padRem = 0.125f)
                        } else {
                            Box(
                                Modifier.size(rem(1.75f)).background(dk(Tw.Slate200, Tw.Slate700), RoundedCornerShape(rem(0.5f))),
                                contentAlignment = Alignment.Center,
                            ) { LucideIcon(R.drawable.ic_lucide_building, 0.875f, Tw.Slate500) }
                        }
                    }
                    if (depositAccount != null) {
                        TwText(
                            "موجودی: ${Currency.formatCurrency(balances[depositAccount.uid] ?: 0.0, unit)}",
                            0.6875f, 1f, Tw.Slate400, maxLines = 1,
                            modifier = Modifier.fillMaxWidth().padding(top = rem(0.25f)),
                        )
                    }
                }

                // ---- amount -----------------------------------------------------------------------------------
                AmountInput(
                    value = amount,
                    onChange = { amount = it },
                    unit = unit,
                    label = "مبلغ واریز",
                    placeholder = "۰",
                )

                TwText(
                    "می‌توانی چند بار از حساب‌های مختلف واریز کنی؛ هر واریز یک تراکنش هزینه از همان حساب ثبت می‌کند.",
                    0.6875f, 1.25f, Tw.Slate400, modifier = Modifier.fillMaxWidth(),
                )

                // ---- actions -----------------------------------------------------------------------------------
                Row(
                    Modifier.fillMaxWidth().padding(top = rem(0.5f)),
                    horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
                            .padding(horizontal = rem(1f), vertical = rem(0.5f)),
                    ) { TwText("انصراف", 0.75f, 1f, dk(Tw.Slate600, Tw.Slate300), weight = FW.Semibold, maxLines = 1) }
                    Row(
                        Modifier.cssShadow(TwShadow.xs(), rem(0.75f))
                            .background(Tw.Amber600, RoundedCornerShape(rem(0.75f)))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { submit() }
                            .padding(horizontal = rem(1.25f), vertical = rem(0.5f)),
                        horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LucideIcon(R.drawable.ic_lucide_building, 1f, Tw.White)
                        TwText("ثبت واریز", 0.75f, 1f, Tw.White, weight = FW.Bold, maxLines = 1)
                    }
                }
            }
        }
    }

    if (accountOpen) {
        AccountPickerDialog(
            groups = db.accountGroups,
            accounts = accounts,
            banks = db.banks,
            onClose = { accountOpen = false },
            onSelect = { accountUid = it; accountOpen = false },
            selectedUid = accountUid.ifEmpty { null },
        )
    }
}

@Composable
private fun DepositFieldLabel(text: String) {
    Box(Modifier.fillMaxWidth().padding(bottom = rem(0.25f))) {
        TwText(text, 0.75f, 1f, dk(Tw.Slate700, Tw.Slate300), weight = FW.Bold, maxLines = 1)
    }
}
