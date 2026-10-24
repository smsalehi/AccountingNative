package com.myaccounting.app.ui.depts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.myaccounting.app.domain.Checks
import com.myaccounting.app.domain.JalaliDate
import com.myaccounting.app.domain.LoanSchedule
import com.myaccounting.app.ui.checks.ChecksScreen
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.components.TwShadow
import com.myaccounting.app.ui.components.cssShadow
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem

import com.myaccounting.app.viewmodel.MainViewModel

/**
 * The depts tab's segmented switch (`وام‌ها | چک‌ها`) from App.tsx, with the red
 * attention badges the web shows for overdue loans and checks.
 */
@Composable
fun DeptsChecksTab(
    state: MainViewModel.UiState,
    vm: MainViewModel,
    onPayInstallment: (String) -> Unit = {},
    onEditTransaction: (com.myaccounting.app.domain.Transaction) -> Unit = {},
) {
    var sub by rememberSaveable { mutableStateOf("loans") }
    val db = state.shownDb
    val overdueLoans = remember(db) { LoanSchedule.countVisibleOverdueLoans(db) }
    val checksAttention = remember(db) { Checks.countChecksNeedingAttention(db) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth()
                .padding(start = rem(1f), end = rem(1f), top = rem(1f))
                .background(dk(Tw.Slate100, Tw.Slate800), RoundedCornerShape(rem(1f)))
                .padding(rem(0.25f)),
            horizontalArrangement = Arrangement.spacedBy(rem(0.25f)),
        ) {
            SubTab("وام‌ها", overdueLoans, sub == "loans", Modifier.weight(1f)) { sub = "loans" }
            SubTab("چک‌ها", checksAttention, sub == "checks", Modifier.weight(1f)) { sub = "checks" }
        }
        Box(Modifier.weight(1f)) {
            if (sub == "loans") {
                DeptsScreen(state, vm, onPayInstallment = onPayInstallment, onEditTransaction = onEditTransaction)
            } else {
                ChecksScreen(state, vm)
            }
        }
    }
}

@Composable
private fun SubTab(label: String, badge: Int, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(rem(0.75f))
    Box(
        modifier
            .then(if (active) Modifier.cssShadow(TwShadow.xs(), rem(0.75f)) else Modifier)
            .background(if (active) dk(Tw.White, Tw.Slate900) else androidx.compose.ui.graphics.Color.Transparent, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = rem(0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TwText(
                label, 0.75f, 1f,
                if (active) dk(Tw.Amber600, Tw.Amber400) else dk(Tw.Slate500, Tw.Slate400),
                weight = FW.Bold, maxLines = 1,
            )
            if (badge > 0) {
                Box(
                    Modifier.padding(start = rem(0.375f))
                        .background(Tw.Rose500, RoundedCornerShape(rem(999f)))
                        .padding(horizontal = rem(0.25f), vertical = rem(0.0625f)),
                ) {
                    TwText(JalaliDate.toPersianDigits(badge), 0.5625f, 0.75f, Tw.White, weight = FW.Bold, maxLines = 1)
                }
            }
        }
    }
}
