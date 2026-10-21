package com.myaccounting.app.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.style.TextOverflow
import com.myaccounting.app.R
import com.myaccounting.app.domain.Account
import com.myaccounting.app.domain.AccountGroup
import com.myaccounting.app.domain.Bank
import com.myaccounting.app.domain.JalaliDate
import com.myaccounting.app.ui.components.BankLogo
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.components.PickerGroupRow
import com.myaccounting.app.ui.components.PickerLink
import com.myaccounting.app.ui.components.PickerShell
import com.myaccounting.app.ui.components.TwCheckbox
import com.myaccounting.app.ui.components.TwSearchField
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem

/**
 * AccountPickerModal.tsx: groups of accounts with a search box; `single` picks one and closes,
 * `multi` collects UIDs and confirms with the footer (`انتخاب همه` / `پاک کردن` / تأیید).
 */
@Composable
fun AccountPickerDialog(
    groups: List<AccountGroup>,
    accounts: List<Account>,
    banks: List<Bank>,
    onClose: () -> Unit,
    onSelect: (String) -> Unit = {},
    selectedUid: String? = null,
    multi: Boolean = false,
    selectedUids: List<String> = emptyList(),
    onConfirm: (List<String>) -> Unit = {},
    showHidden: Boolean = false,
) {
    var search by remember { mutableStateOf("") }
    var tempSelected by remember { mutableStateOf(selectedUids) }
    var expanded by remember {
        val init = mutableSetOf<String>()
        if (selectedUid != null) accounts.firstOrNull { it.uid == selectedUid }?.let { init += it.groupUid }
        else accounts.filter { it.uid in selectedUids }.forEach { init += it.groupUid }
        mutableStateOf<Set<String>>(init)
    }

    val bankTitles = remember(banks) { banks.associate { it.uid to it.title } }
    val visible = remember(accounts, showHidden) { if (showHidden) accounts else accounts.filter { it.isHidden == 0 } }
    val byGroup = remember(groups, visible) {
        val map = LinkedHashMap<String, MutableList<Account>>()
        groups.forEach { map[it.uid] = mutableListOf() }
        visible.forEach { map[it.groupUid]?.add(it) }
        map
    }
    val term = search.trim().lowercase()
    val filteredGroups = groups.filter { g -> (byGroup[g.uid] ?: emptyList()).any { term.isEmpty() || it.title.lowercase().contains(term) } }

    PickerShell(
        title = if (multi) "انتخاب حساب‌ها (فیلتر)" else "انتخاب حساب بانکی",
        subtitle = if (multi) "حساب‌های مورد نظر را انتخاب کنید" else "حساب مورد نظر را انتخاب کنید",
        onClose = onClose,
        search = { TwSearchField(search, { search = it }, "جستجو در نام حساب‌ها...") },
        showFooter = multi,
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.75f)), verticalAlignment = Alignment.CenterVertically) {
                PickerLink("انتخاب همه", dk(Tw.Sky600, Tw.Sky400), FW.Semibold) { tempSelected = visible.map { it.uid } }
                PickerLink("پاک کردن", Tw.Slate400) { tempSelected = emptyList() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f)), verticalAlignment = Alignment.CenterVertically) {
                TwText(
                    if (tempSelected.isNotEmpty()) "${JalaliDate.toPersianDigits(tempSelected.size)} حساب انتخاب‌شده" else "همه حساب‌ها",
                    0.6875f, 1f, Tw.Slate400, maxLines = 1,
                )
                Box(
                    Modifier.background(Tw.Sky600, RoundedCornerShape(rem(0.75f)))
                        .clickable(remember { MutableInteractionSource() }, null) { onConfirm(tempSelected); onClose() }
                        .padding(horizontal = rem(1f), vertical = rem(0.5f)),
                ) { TwText("تأیید", 0.75f, 1f, Tw.White, weight = FW.Bold, maxLines = 1) }
            }
        },
        bodyModifier = Modifier.verticalScroll(rememberScrollState()).padding(start = rem(1f), end = rem(1f), bottom = rem(0.5f)),
        bodySpacing = rem(0.5f),
    ) {
        if (filteredGroups.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = rem(2f)), contentAlignment = Alignment.Center) {
                TwText("حسابی یافت نشد.", 0.75f, 1f, Tw.Slate400, maxLines = 1)
            }
        }
        for (group in filteredGroups) {
            val groupAccounts = (byGroup[group.uid] ?: emptyList()).filter { term.isEmpty() || it.title.lowercase().contains(term) }
            val isExpanded = group.uid in expanded
            val allChecked = multi && groupAccounts.isNotEmpty() && groupAccounts.all { it.uid in tempSelected }
            val someChecked = multi && groupAccounts.any { it.uid in tempSelected } && !allChecked
            val bw = cssBorder()
            fun toggleGroup() {
                tempSelected = if (allChecked) tempSelected - groupAccounts.map { it.uid }.toSet()
                else (tempSelected + groupAccounts.map { it.uid }).distinct()
            }
            Column(Modifier.fillMaxWidth().border(bw, dk(Tw.Slate200.a(0.7f), Tw.Slate800), RoundedCornerShape(rem(0.75f)))) {
                PickerGroupRow(
                    title = group.title, count = groupAccounts.size, expanded = isExpanded,
                    onToggle = { expanded = if (isExpanded) expanded - group.uid else expanded + group.uid },
                    trailing = {
                        if (multi) {
                            Row(
                                Modifier.clickable(remember { MutableInteractionSource() }, null) { toggleGroup() },
                                horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TwText("همه", 0.625f, 0.9375f, Tw.Slate400, maxLines = 1)
                                TwCheckbox(checked = allChecked, indeterminate = someChecked) { toggleGroup() }
                            }
                        }
                    },
                )
                if (isExpanded) {
                    Column(Modifier.fillMaxWidth().padding(rem(0.5f)), verticalArrangement = Arrangement.spacedBy(rem(0.125f))) {
                        if (groupAccounts.isEmpty()) {
                            Box(Modifier.fillMaxWidth().padding(vertical = rem(0.75f)), contentAlignment = Alignment.Center) {
                                TwText("حسابی در این گروه وجود ندارد", 0.6875f, 1f, Tw.Slate400, maxLines = 1)
                            }
                        }
                        for (acc in groupAccounts) AccountRow(acc, bankTitles[acc.bankUid], multi, selectedUid == acc.uid, acc.uid in tempSelected) {
                            if (multi) tempSelected = if (acc.uid in tempSelected) tempSelected - acc.uid else tempSelected + acc.uid
                            else { onSelect(acc.uid); onClose() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountRow(acc: Account, bankTitle: String?, multi: Boolean, selected: Boolean, checked: Boolean, onClick: () -> Unit) {
    val rowShape = RoundedCornerShape(rem(0.75f))
    if (!multi) {
        Row(
            Modifier.fillMaxWidth()
                .then(
                    if (selected) Modifier.background(dk(Tw.Sky50, Tw.Sky950.a(0.5f)), rowShape)
                        .border(cssBorder(), dk(Tw.Sky300, Tw.Sky800), rowShape)
                    else Modifier,
                )
                .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
                .padding(horizontal = rem(0.75f), vertical = rem(0.625f) + cssBorder()),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(rem(0.625f)), verticalAlignment = Alignment.CenterVertically) {
                BankLogo(bankTitle ?: acc.title, acc.bankUid, sizeRem = 1.5f, padRem = 0.125f)
                TwText(
                    acc.title, 0.75f, 1f, if (selected) dk(Tw.Sky700, Tw.Sky300) else dk(Tw.Slate700, Tw.Slate200),
                    weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected) LucideIcon(R.drawable.ic_lucide_check, 1f, Tw.Sky600)
        }
    } else {
        Row(
            Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
                .padding(horizontal = rem(0.75f), vertical = rem(0.5f)),
            horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TwCheckbox(checked = checked, onToggle = onClick)
            BankLogo(bankTitle ?: acc.title, acc.bankUid, sizeRem = 1.5f, padRem = 0.125f)
            TwText(
                acc.title, 0.75f, 1f, if (checked) dk(Tw.Sky600, Tw.Sky400) else dk(Tw.Slate700, Tw.Slate200),
                weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
