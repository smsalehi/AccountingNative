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
import com.myaccounting.app.domain.Person
import com.myaccounting.app.domain.PersonGroup
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.components.PickerAvatar
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

/** The catch-all group the web adds for persons without a known GroupUID. */
private const val UNGROUPED_UID = "__ungrouped__"

private fun initialOf(name: String): String {
    val t = name.trim()
    return if (t.isEmpty()) "؟" else t.substring(0, 1)
}

/**
 * PersonPickerModal.tsx: person groups with an avatar/phone list; `single` picks one and closes,
 * `multi` collects UIDs and confirms with the footer.
 */
@Composable
fun PersonPickerDialog(
    groups: List<PersonGroup>,
    persons: List<Person>,
    onClose: () -> Unit,
    multi: Boolean = false,
    selectedUid: String? = null,
    onSelect: (String) -> Unit = {},
    selectedUids: List<String> = emptyList(),
    onConfirm: (List<String>) -> Unit = {},
    showHidden: Boolean = false,
) {
    var search by remember { mutableStateOf("") }
    var tempSelected by remember { mutableStateOf(selectedUids) }
    var expanded by remember {
        val init = mutableSetOf<String>()
        if (selectedUid != null) {
            persons.firstOrNull { it.uid == selectedUid }?.let {
                init += if (groups.any { g -> g.uid == it.groupUid }) it.groupUid else UNGROUPED_UID
            }
        } else {
            persons.forEach { p ->
                if (p.uid in selectedUids) init += if (groups.any { g -> g.uid == p.groupUid }) p.groupUid else UNGROUPED_UID
            }
        }
        mutableStateOf<Set<String>>(init)
    }

    val visible = remember(persons, showHidden) { if (showHidden) persons else persons.filter { it.isHidden == 0 } }
    val allGroups = remember(groups, visible) {
        val list = groups.toMutableList()
        if (visible.any { p -> groups.none { g -> g.uid == p.groupUid } }) {
            list += PersonGroup(UNGROUPED_UID, "سایر", null)
        }
        list
    }
    val byGroup = remember(allGroups, groups, visible) {
        val map = LinkedHashMap<String, MutableList<Person>>()
        allGroups.forEach { map[it.uid] = mutableListOf() }
        visible.forEach { p ->
            val key = if (groups.any { g -> g.uid == p.groupUid }) p.groupUid else UNGROUPED_UID
            map[key]?.add(p)
        }
        map
    }
    val term = search.trim().lowercase()
    fun matches(p: Person) = term.isEmpty() || p.fullName.lowercase().contains(term) || (p.mobile ?: "").contains(term)
    val filteredGroups = allGroups.filter { g -> (byGroup[g.uid] ?: emptyList()).any { matches(it) } }

    PickerShell(
        title = if (multi) "انتخاب اشخاص (فیلتر)" else "انتخاب شخص",
        subtitle = if (multi) "اشخاص مورد نظر را انتخاب کنید" else "شخص مورد نظر را انتخاب کنید",
        onClose = onClose,
        search = { TwSearchField(search, { search = it }, "جستجو در نام یا موبایل اشخاص...") },
        showFooter = multi,
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.75f)), verticalAlignment = Alignment.CenterVertically) {
                PickerLink("انتخاب همه", dk(Tw.Sky600, Tw.Sky400), FW.Semibold) { tempSelected = visible.map { it.uid } }
                PickerLink("پاک کردن", Tw.Slate400) { tempSelected = emptyList() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f)), verticalAlignment = Alignment.CenterVertically) {
                TwText(
                    if (tempSelected.isNotEmpty()) "${tempSelected.size} شخص انتخاب‌شده" else "همه اشخاص",
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
                TwText("شخصی یافت نشد.", 0.75f, 1f, Tw.Slate400, maxLines = 1)
            }
        }
        for (group in filteredGroups) {
            val groupPersons = (byGroup[group.uid] ?: emptyList()).filter { matches(it) }
            val isExpanded = group.uid in expanded
            val allChecked = multi && groupPersons.isNotEmpty() && groupPersons.all { it.uid in tempSelected }
            val someChecked = multi && groupPersons.any { it.uid in tempSelected } && !allChecked
            fun toggleGroup() {
                tempSelected = if (allChecked) tempSelected - groupPersons.map { it.uid }.toSet()
                else (tempSelected + groupPersons.map { it.uid }).distinct()
            }
            Column(Modifier.fillMaxWidth().border(cssBorder(), dk(Tw.Slate200.a(0.7f), Tw.Slate800), RoundedCornerShape(rem(0.75f)))) {
                PickerGroupRow(
                    title = group.title, count = groupPersons.size, expanded = isExpanded,
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
                        if (groupPersons.isEmpty()) {
                            Box(Modifier.fillMaxWidth().padding(vertical = rem(0.75f)), contentAlignment = Alignment.Center) {
                                TwText("شخصی در این گروه وجود ندارد", 0.6875f, 1f, Tw.Slate400, maxLines = 1)
                            }
                        }
                        for (person in groupPersons) {
                            val selected = if (!multi) selectedUid == person.uid else person.uid in tempSelected
                            if (!multi) {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .then(
                                            if (selected) Modifier.background(dk(Tw.Sky50, Tw.Sky950.a(0.5f)), RoundedCornerShape(rem(0.75f)))
                                                .border(cssBorder(), dk(Tw.Sky300, Tw.Sky800), RoundedCornerShape(rem(0.75f)))
                                            else Modifier,
                                        )
                                        .clickable(remember { MutableInteractionSource() }, null) { onSelect(person.uid); onClose() }
                                        .padding(horizontal = rem(0.75f), vertical = rem(0.625f) + cssBorder()),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(rem(0.625f)),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        PickerAvatar(initialOf(person.fullName))
                                        Column(Modifier.weight(1f)) {
                                            TwText(
                                                person.fullName, 0.75f, 1f,
                                                if (selected) dk(Tw.Sky700, Tw.Sky300) else dk(Tw.Slate700, Tw.Slate200),
                                                weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            )
                                            if (!person.mobile.isNullOrEmpty()) {
                                                TwText(person.mobile, 0.625f, 1f, Tw.Slate400, maxLines = 1)
                                            }
                                        }
                                    }
                                    if (selected) LucideIcon(R.drawable.ic_lucide_check, 1f, Tw.Sky600)
                                }
                            } else {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clickable(remember { MutableInteractionSource() }, null) {
                                            tempSelected = if (selected) tempSelected - person.uid else tempSelected + person.uid
                                        }
                                        .padding(horizontal = rem(0.75f), vertical = rem(0.5f)),
                                    horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    TwCheckbox(checked = selected) {
                                        tempSelected = if (selected) tempSelected - person.uid else tempSelected + person.uid
                                    }
                                    PickerAvatar(initialOf(person.fullName))
                                    TwText(
                                        person.fullName, 0.75f, 1f,
                                        if (selected) dk(Tw.Sky600, Tw.Sky400) else dk(Tw.Slate700, Tw.Slate200),
                                        weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
