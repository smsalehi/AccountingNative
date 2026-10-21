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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.myaccounting.app.R
import com.myaccounting.app.domain.JalaliDate
import com.myaccounting.app.domain.Subject
import com.myaccounting.app.ui.components.CategoryIcon
import com.myaccounting.app.ui.components.CategoryIconSize
import com.myaccounting.app.ui.components.HairLine
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.components.PickerShell
import com.myaccounting.app.ui.components.TwSearchField
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem
import androidx.compose.foundation.layout.size

/**
 * CategoryPickerModal.tsx: the سرفصل/زیرفصل tree of the current direction, with a search view and an
 * inline «سرفصل جدید» action that opens [NewSubjectDialog].
 */
@Composable
fun CategoryPickerDialog(
    subjects: List<Subject>,
    transactionType: Int,
    onClose: () -> Unit,
    onSelect: (String) -> Unit,
    onAddSubject: (Subject) -> Unit = {},
    selectedSubjectUid: String? = null,
    defaultIsCost: Int = if (transactionType == 1) 1 else 0,
    onError: (String) -> Unit = {},
) {
    var search by remember { mutableStateOf("") }
    var newSubjectOpen by remember { mutableStateOf(false) }
    var expandedRoots by remember { mutableStateOf(setOf<String>()) }

    val relevantIsCost = if (transactionType == 1) 1 else 0
    val allRelevant = subjects.filter { it.isCost == relevantIsCost }
    val roots = allRelevant.filter { it.parentUid.isNullOrEmpty() }
    val childMap = remember(allRelevant) { allRelevant.filter { !it.parentUid.isNullOrEmpty() }.groupBy { it.parentUid!! } }
    val term = search.trim().lowercase()
    val searchResults = if (term.isEmpty()) null else subjects.filter { it.title.lowercase().contains(term) }

    PickerShell(
        title = "انتخاب سرفصل و زیرفصل",
        subtitle = "دسته‌بندی اصلی یا زیرمجموعه مورد نظر خود را انتخاب کنید",
        onClose = onClose,
        showHeaderTrailing = true,
        headerTrailing = {
            Row(
                Modifier.background(dk(Tw.Sky50, Tw.Sky950.a(0.6f)), RoundedCornerShape(rem(0.75f)))
                    .border(cssBorder(), dk(Tw.Sky200, Tw.Sky800), RoundedCornerShape(rem(0.75f)))
                    .clickable(remember { MutableInteractionSource() }, null) { newSubjectOpen = true }
                    .padding(horizontal = rem(0.75f), vertical = rem(0.375f)),
                horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LucideIcon(R.drawable.ic_lucide_plus, 0.875f, dk(Tw.Sky600, Tw.Sky400))
                TwText("سرفصل جدید", 0.75f, 1f, dk(Tw.Sky600, Tw.Sky400), weight = FW.Bold, maxLines = 1)
            }
        },
        search = { TwSearchField(search, { search = it }, "جستجو در بین سرفصل‌ها و زیرفصل‌ها...") },
        showFooter = true,
        footer = {
            Row(
                Modifier.clickable(remember { MutableInteractionSource() }, null) { newSubjectOpen = true },
                horizontalArrangement = Arrangement.spacedBy(rem(0.25f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LucideIcon(R.drawable.ic_lucide_plus, 0.875f, dk(Tw.Sky600, Tw.Sky400))
                TwText("ایجاد موضوع مالی تازه", 0.75f, 1f, dk(Tw.Sky600, Tw.Sky400), weight = FW.Semibold, maxLines = 1)
            }
            Box(
                Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClose)
                    .padding(horizontal = rem(1f), vertical = rem(0.5f)),
            ) { TwText("بستن", 0.75f, 1f, dk(Tw.Slate600, Tw.Slate300), weight = FW.Semibold, maxLines = 1) }
        },
        bodyModifier = Modifier.verticalScroll(rememberScrollState()).padding(rem(1f)),
        bodySpacing = if (searchResults != null) rem(0.375f) else rem(0.5f),
    ) {
        if (searchResults != null) {
            TwText("نتایج جستجو:", 0.6875f, 1f, Tw.Slate400, Modifier.padding(bottom = rem(0.5f)), weight = FW.Semibold, maxLines = 1)
            if (searchResults.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = rem(2f)), contentAlignment = Alignment.Center) {
                    TwText("موردی یافت نشد.", 0.75f, 1f, Tw.Slate400, maxLines = 1)
                }
            }
            for (sub in searchResults) {
                val parent = sub.parentUid?.let { p -> subjects.firstOrNull { it.uid == p } }
                val isSelected = selectedSubjectUid == sub.uid
                val shape = RoundedCornerShape(rem(0.75f))
                Row(
                    Modifier.fillMaxWidth()
                        .then(
                            if (isSelected) Modifier.background(dk(Tw.Sky50, Tw.Sky950.a(0.5f)), shape)
                                .border(cssBorder(), dk(Tw.Sky300, Tw.Sky800), shape)
                            else Modifier.background(dk(Tw.Slate50.a(0.7f), Tw.Slate800.a(0.6f)), shape),
                        )
                        .clickable(remember { MutableInteractionSource() }, null) { onSelect(sub.uid); onClose() }
                        .padding(horizontal = rem(0.75f), vertical = rem(0.625f)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(rem(0.625f)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CategoryIcon(sub.title, sub.icon, sub.isCost, CategoryIconSize.SM)
                        Column(Modifier.weight(1f)) {
                            TwText(
                                sub.title, 0.75f, 1f,
                                if (isSelected) dk(Tw.Sky700, Tw.Sky300) else dk(Tw.Slate700, Tw.Slate200),
                                weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            if (parent != null) {
                                TwText("دسته اصلی: ${parent.title}", 0.625f, 1f, Tw.Slate400, maxLines = 1)
                            }
                        }
                    }
                    if (isSelected) LucideIcon(R.drawable.ic_lucide_check, 1f, Tw.Sky600)
                }
            }
            return@PickerShell
        }

        if (roots.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = rem(2f)), contentAlignment = Alignment.Center) {
                TwText("هنوز سرفصلی تعریف نشده است.", 0.75f, 1f, Tw.Slate400, maxLines = 1)
            }
            return@PickerShell
        }

        for (root in roots) {
            val children = childMap[root.uid] ?: emptyList()
            val hasChildren = children.isNotEmpty()
            val isExpanded = root.uid in expandedRoots
            val isSelected = selectedSubjectUid == root.uid
            Column(
                Modifier.fillMaxWidth()
                    .border(cssBorder(), dk(Tw.Slate200.a(0.7f), Tw.Slate800), RoundedCornerShape(rem(0.75f))),
            ) {
                Row(
                    Modifier.fillMaxWidth()
                        .background(if (isSelected) dk(Tw.Sky50, Tw.Sky950.a(0.4f)) else Color.Transparent)
                        .padding(rem(0.75f)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, null) {
                            if (hasChildren) expandedRoots = if (isExpanded) expandedRoots - root.uid else expandedRoots + root.uid
                            else { onSelect(root.uid); onClose() }
                        },
                        horizontalArrangement = Arrangement.spacedBy(rem(0.625f)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (hasChildren) {
                            LucideIcon(
                                if (isExpanded) R.drawable.ic_lucide_chevron_up else R.drawable.ic_lucide_chevron_down,
                                0.875f, Tw.Slate400,
                            )
                        } else {
                            Box(Modifier.size(rem(0.875f)))
                        }
                        CategoryIcon(root.title, root.icon, root.isCost, CategoryIconSize.SM)
                        Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f)), verticalAlignment = Alignment.CenterVertically) {
                            TwText(
                                root.title, 0.75f, 1f,
                                if (isSelected) dk(Tw.Sky700, Tw.Sky300) else dk(Tw.Slate800, Tw.Slate100),
                                weight = FW.Bold, maxLines = 1,
                            )
                            if (hasChildren) {
                                TwText(
                                    "(${JalaliDate.toPersianDigits(children.size)} زیرفصل)", 0.625f, 1f, Tw.Slate400, maxLines = 1,
                                )
                            }
                        }
                    }
                    if (hasChildren) {
                        Box(
                            Modifier.clickable(remember { MutableInteractionSource() }, null) { onSelect(root.uid); onClose() },
                        ) {
                            if (isSelected) LucideIcon(R.drawable.ic_lucide_check, 1f, dk(Tw.Sky600, Tw.Sky400))
                            else TwText("انتخاب کل", 0.6875f, 1f, Tw.Slate400, maxLines = 1)
                        }
                    } else if (isSelected) {
                        LucideIcon(R.drawable.ic_lucide_check, 1f, dk(Tw.Sky600, Tw.Sky400))
                    }
                }
                if (hasChildren && isExpanded) {
                    HairLine(dk(Tw.Slate100, Tw.Slate800.a(0.8f)))
                    Box(Modifier.fillMaxWidth().background(dk(Tw.Slate50.a(0.5f), Tw.Slate900.a(0.4f)))) {
                        Column(
                            Modifier.fillMaxWidth().padding(start = rem(0.5f), end = rem(0.5f), bottom = rem(0.5f)),
                            verticalArrangement = Arrangement.spacedBy(rem(0.25f)),
                        ) {
                            for (child in children) {
                                val childSelected = selectedSubjectUid == child.uid
                                Row(
                                    Modifier.fillMaxWidth()
                                        .background(
                                            if (childSelected) dk(Tw.Sky100, Tw.Sky900.a(0.6f)) else Tw.Transparent,
                                            RoundedCornerShape(rem(0.5f)),
                                        )
                                        .clickable(remember { MutableInteractionSource() }, null) { onSelect(child.uid); onClose() }
                                        .padding(horizontal = rem(0.625f), vertical = rem(0.375f)),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        TwText("└", 0.75f, 1f, dk(Tw.Slate300, Tw.Slate600), maxLines = 1)
                                        CategoryIcon(child.title, child.icon, child.isCost, CategoryIconSize.SM)
                                        TwText(
                                            child.title, 0.75f, 1f,
                                            if (childSelected) dk(Tw.Sky800, Tw.Sky200) else dk(Tw.Slate600, Tw.Slate300),
                                            weight = if (childSelected) FW.Bold else FW.Normal, maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    if (childSelected) LucideIcon(R.drawable.ic_lucide_check, 0.875f, Tw.Sky600)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (newSubjectOpen) {
        NewSubjectDialog(
            subjects = subjects,
            initialIsCost = defaultIsCost,
            onClose = { newSubjectOpen = false },
            onError = onError,
            onSave = { newSub ->
                onAddSubject(newSub)
                onSelect(newSub.uid)
                newSubjectOpen = false
                onClose()
            },
        )
    }
}
