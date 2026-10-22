package com.myaccounting.app.ui.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myaccounting.app.R
import com.myaccounting.app.domain.AccountingDatabase
import com.myaccounting.app.domain.JalaliDate
import com.myaccounting.app.domain.Subject
import com.myaccounting.app.domain.TransactionFilters.DatePreset
import com.myaccounting.app.ui.components.CategoryIcon
import com.myaccounting.app.ui.components.CategoryIconSize
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.components.PersianDateField
import com.myaccounting.app.ui.components.TwCheckbox
import com.myaccounting.app.ui.components.TwShadow
import com.myaccounting.app.ui.components.cssShadow
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.LocalIsDark
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.Vazirmatn
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem
import com.myaccounting.app.ui.theme.remSp

/** `border-t` of a block that is spaced by the parent's space-y. */
private fun Modifier.topDivider(color: Color): Modifier = drawBehind {
    drawRect(color, topLeft = Offset.Zero, size = Size(size.width, 1.dp.toPx()))
}

/** Persian digits of a count («۳ پروژه»). */
@Composable
fun fa(n: Int): String = JalaliDate.toPersianDigits(n)

/**
 * The «Advanced Filter Toolbar» card of TransactionsView.tsx (search, date presets, the three
 * multi-select pickers, the persons picker and the type badges with the reset button).
 */
@Composable
fun TxFilterCard(db: AccountingDatabase, ui: TxUi) {
    val shape = RoundedCornerShape(rem(1f))
    val bw = cssBorder()
    Column(
        Modifier.fillMaxWidth()
            .background(dk(Tw.White, Tw.Slate900), shape)
            .border(bw, dk(Tw.Slate200.a(0.8f), Tw.Slate800), shape)
            .cssShadow(TwShadow.xs(), rem(1f))
            .padding(rem(1f)),
        verticalArrangement = Arrangement.spacedBy(rem(0.75f)),
    ) {
        TxSearchField(ui)
        TxPresetRow(ui)
        if (ui.preset == DatePreset.CUSTOM) TxCustomRange(ui)

        Column(
            Modifier.padding(top = rem(0.25f)),
            verticalArrangement = Arrangement.spacedBy(rem(0.625f)),
        ) {
            TxMultiButton(
                icon = R.drawable.ic_lucide_credit_card,
                iconTint = Tw.Sky500,
                label = if (ui.accountUids.isEmpty()) "همه حساب‌ها و کارت‌ها"
                else "${fa(ui.accountUids.size)} حساب انتخاب‌شده",
                active = ui.accountUids.isNotEmpty(),
                activeBg = dk(Tw.Sky50, Tw.Sky950.a(0.5f)),
                activeBorder = dk(Tw.Sky300, Tw.Sky700),
                activeText = dk(Tw.Sky900, Tw.Sky200),
                chevronOpen = false,
                onClick = { ui.accountDialog = true },
            )
            Box {
                TxMultiButton(
                    icon = R.drawable.ic_lucide_briefcase,
                    iconTint = Tw.Indigo500,
                    label = if (ui.projectUids.isEmpty()) "همه پروژه‌ها"
                    else "${fa(ui.projectUids.size)} پروژه انتخاب‌شده",
                    active = ui.projectUids.isNotEmpty(),
                    activeBg = dk(Tw.Indigo50, Tw.Indigo950.a(0.5f)),
                    activeBorder = dk(Tw.Indigo300, Tw.Indigo700),
                    activeText = dk(Tw.Indigo900, Tw.Indigo200),
                    chevronOpen = ui.projectMenu,
                    onClick = {
                        ui.projectMenu = !ui.projectMenu
                        ui.subjectMenu = false
                        ui.personMenu = false
                    },
                )
                ProjectFilterMenu(db, ui)
            }
            Box {
                TxMultiButton(
                    icon = R.drawable.ic_lucide_tag,
                    iconTint = Tw.Amber500,
                    label = if (ui.subjectUids.isEmpty()) "همه موضوعات (سرفصل‌ها)"
                    else "${fa(ui.subjectUids.size)} موضوع انتخاب‌شده",
                    active = ui.subjectUids.isNotEmpty(),
                    activeBg = dk(Tw.Amber50, Tw.Amber950.a(0.5f)),
                    activeBorder = dk(Tw.Amber300, Tw.Amber700),
                    activeText = dk(Tw.Amber900, Tw.Amber200),
                    chevronOpen = ui.subjectMenu,
                    onClick = {
                        ui.subjectMenu = !ui.subjectMenu
                        ui.projectMenu = false
                        ui.personMenu = false
                    },
                )
                SubjectFilterMenu(db, ui)
            }
        }

        Box {
            TxMultiButton(
                icon = R.drawable.ic_lucide_user,
                iconTint = Tw.Emerald500,
                label = if (ui.personUids.isEmpty()) "همه اشخاص"
                else "${fa(ui.personUids.size)} شخص انتخاب‌شده",
                active = ui.personUids.isNotEmpty(),
                activeBg = dk(Tw.Emerald50, Tw.Emerald950.a(0.5f)),
                activeBorder = dk(Tw.Emerald300, Tw.Emerald700),
                activeText = dk(Tw.Emerald900, Tw.Emerald200),
                chevronOpen = ui.personMenu,
                onClick = {
                    ui.personMenu = !ui.personMenu
                    ui.accountDialog = false
                    ui.projectMenu = false
                    ui.subjectMenu = false
                },
            )
            PersonFilterMenu(db, ui)
        }

        TxTypeRow(ui)
    }
}

/* ------------------------------------------------------------------ search + presets */

@Composable
private fun TxSearchField(ui: TxUi) {
    val shape = RoundedCornerShape(rem(0.75f))
    val bw = cssBorder()
    Box(Modifier.fillMaxWidth()) {
        BasicTextField(
            value = ui.search,
            onValueChange = { ui.search = it },
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = Vazirmatn,
                fontSize = remSp(0.75f),
                lineHeight = remSp(1f),
                color = dk(Tw.Slate900, Tw.Slate100),
                textDirection = TextDirection.Rtl,
            ),
            cursorBrush = SolidColor(Tw.Sky500),
            decorationBox = { inner ->
                Box(
                    Modifier.fillMaxWidth()
                        .background(dk(Tw.Slate50, Tw.Slate800), shape)
                        .border(bw, dk(Tw.Slate200, Tw.Slate700), shape)
                        .padding(start = rem(2.5f), end = rem(0.75f), top = rem(0.5f), bottom = rem(0.5f)),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (ui.search.isEmpty()) {
                        TwText(
                            "جستجو در شرح، تاریخ، سرفصل، حساب یا طرف‌حساب...",
                            0.75f, 1f, Tw.Slate400, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                }
            },
        )
        LucideIcon(
            R.drawable.ic_lucide_search, 1f, Tw.Slate400,
            Modifier.align(Alignment.CenterEnd).padding(end = rem(0.875f)),
        )
    }
}

@Composable
private fun TxPresetRow(ui: TxUi) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = rem(0.25f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(rem(0.25f)),
    ) {
        Row(
            Modifier.padding(end = rem(0.25f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(rem(0.25f)),
        ) {
            LucideIcon(R.drawable.ic_lucide_calendar, 0.75f, Tw.Slate400)
            TwText("بازه زمانی:", 0.6875f, 1f, Tw.Slate400, weight = FW.Semibold, maxLines = 1)
        }
        for (p in DatePreset.entries) {
            val active = ui.preset == p
            val shape = RoundedCornerShape(rem(0.5f))
            Box(
                Modifier.background(if (active) Tw.Sky600 else dk(Tw.Slate100, Tw.Slate800), shape)
                    .then(if (active) Modifier.cssShadow(TwShadow.xxs(), rem(0.5f)) else Modifier)
                    .clickable(remember { MutableInteractionSource() }, null) { ui.preset = p }
                    .padding(horizontal = rem(0.625f), vertical = rem(0.375f)),
            ) {
                TwText(
                    p.label, 0.75f, 1f,
                    if (active) Tw.White else dk(Tw.Slate600, Tw.Slate300),
                    weight = FW.Semibold, maxLines = 1,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TxCustomRange(ui: TxUi) {
    val shape = RoundedCornerShape(rem(0.75f))
    val bw = cssBorder()
    Column(
        Modifier.fillMaxWidth()
            .background(dk(Tw.Slate50, Tw.Slate800.a(0.6f)), shape)
            .border(bw, dk(Tw.Slate200.a(0.8f), Tw.Slate700), shape)
            .padding(rem(0.75f)),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(rem(0.75f)),
            verticalArrangement = Arrangement.spacedBy(rem(0.5f)),
        ) {
            TwText(
                "تعیین بازه تاریخی شمسی:", 0.75f, 1f, dk(Tw.Slate500, Tw.Slate400),
                weight = FW.Medium, maxLines = 1,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
            ) {
                TwText("از:", 0.6875f, 1f, Tw.Slate400, maxLines = 1)
                Box(Modifier.width(rem(9f))) {
                    PersianDateField(ui.customStart, { ui.customStart = it }, placeholder = "تاریخ شروع")
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
            ) {
                TwText("تا:", 0.6875f, 1f, Tw.Slate400, maxLines = 1)
                Box(Modifier.width(rem(9f))) {
                    PersianDateField(ui.customEnd, { ui.customEnd = it }, placeholder = "تاریخ پایان")
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ multi selects */

@Composable
private fun TxMultiButton(
    icon: Int,
    iconTint: Color,
    label: String,
    active: Boolean,
    activeBg: Color,
    activeBorder: Color,
    activeText: Color,
    chevronOpen: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(rem(0.75f))
    val bw = cssBorder()
    Row(
        Modifier.fillMaxWidth()
            .background(if (active) activeBg else dk(Tw.Slate50, Tw.Slate800), shape)
            .border(bw, if (active) activeBorder else dk(Tw.Slate200, Tw.Slate700), shape)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = rem(0.75f), vertical = rem(0.5f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
        ) {
            LucideIcon(icon, 0.875f, iconTint)
            TwText(
                label, 0.75f, 1f,
                if (active) activeText else dk(Tw.Slate700, Tw.Slate300),
                weight = if (active) FW.Semibold else FW.Normal,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        LucideIcon(
            R.drawable.ic_lucide_chevron_down, 0.875f, Tw.Slate400,
            Modifier.rotate(if (chevronOpen) 180f else 0f),
        )
    }
}

@Composable
private fun MenuHeader(accent: Color, onAll: () -> Unit, onClear: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .padding(bottom = rem(0.375f))
            .topDivider(dk(Tw.Slate100, Tw.Slate800)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onAll)) {
            TwText("انتخاب همه", 0.6875f, 1f, accent, weight = FW.Semibold, maxLines = 1)
        }
        Box(Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClear)) {
            TwText("پاک کردن", 0.6875f, 1f, Tw.Slate400, weight = FW.Medium, maxLines = 1)
        }
    }
}

@Composable
private fun MenuRow(
    checked: Boolean,
    label: String,
    accent: Color,
    onToggle: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(remember { MutableInteractionSource() }, null, onClick = onToggle)
            .padding(rem(0.375f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
    ) {
        TwCheckbox(checked = checked, onToggle = onToggle)
        leading?.invoke()
        TwText(
            label, 0.75f, 1f,
            if (checked) accent else dk(Tw.Slate700, Tw.Slate300),
            weight = if (checked) FW.Bold else FW.Normal,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

@Composable
private fun DropdownSurface(
    expanded: Boolean,
    onDismiss: () -> Unit,
    widthRem: Float,
    content: @Composable () -> Unit,
) {
    val dark = LocalIsDark.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.width(rem(widthRem)),
        shape = RoundedCornerShape(rem(0.75f)),
        containerColor = if (dark) Tw.Slate900 else Tw.White,
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(cssBorder(), dk(Tw.Slate200, Tw.Slate800)),
    ) {
        Column(
            Modifier.padding(rem(0.625f)),
            verticalArrangement = Arrangement.spacedBy(rem(0.5f)),
        ) { content() }
    }
}

@Composable
private fun ProjectFilterMenu(db: AccountingDatabase, ui: TxUi) {
    DropdownSurface(ui.projectMenu, { ui.projectMenu = false }, 13.75f) {
        MenuHeader(
            accent = dk(Tw.Indigo600, Tw.Indigo400),
            onAll = { ui.projectUids = db.projects.map { it.uid } + "unassigned" },
            onClear = { ui.projectUids = emptyList() },
        )
        Column(
            Modifier.fillMaxWidth().heightIn(max = rem(12f)).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(rem(0.25f)),
        ) {
            MenuRow(
                checked = "unassigned" in ui.projectUids,
                label = "عمومی / بدون پروژه",
                accent = dk(Tw.Indigo600, Tw.Indigo400),
                onToggle = { ui.toggle(TxUi.Kind.PROJECT, "unassigned") },
            )
            for (p in db.projects) {
                MenuRow(
                    checked = p.uid in ui.projectUids,
                    label = p.title,
                    accent = dk(Tw.Indigo600, Tw.Indigo400),
                    onToggle = { ui.toggle(TxUi.Kind.PROJECT, p.uid) },
                )
            }
        }
    }
}

@Composable
private fun SubjectFilterMenu(db: AccountingDatabase, ui: TxUi) {
    val accent = dk(Tw.Amber600, Tw.Amber400)
    val term = ui.subjectSearch.trim()
    val subjects = remember(db.subjects, term) {
        if (term.isEmpty()) db.subjects
        else db.subjects.filter { it.title.contains(term, ignoreCase = true) }
    }
    DropdownSurface(ui.subjectMenu, { ui.subjectMenu = false }, 16.25f) {
        MenuHeader(
            accent = accent,
            onAll = { ui.subjectUids = subjects.map { it.uid } },
            onClear = { ui.subjectUids = emptyList() },
        )
        val shape = RoundedCornerShape(rem(0.5f))
        BasicTextField(
            value = ui.subjectSearch,
            onValueChange = { ui.subjectSearch = it },
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = Vazirmatn,
                fontSize = remSp(0.75f),
                lineHeight = remSp(1f),
                color = dk(Tw.Slate800, Tw.Slate100),
                textDirection = TextDirection.Rtl,
            ),
            cursorBrush = SolidColor(Tw.Amber500),
            decorationBox = { inner ->
                Box(
                    Modifier.fillMaxWidth()
                        .background(dk(Tw.Slate50, Tw.Slate800), shape)
                        .border(cssBorder(), dk(Tw.Slate200, Tw.Slate700), shape)
                        .padding(horizontal = rem(0.625f), vertical = rem(0.25f)),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (ui.subjectSearch.isEmpty()) TwText("جستجوی سرفصل...", 0.75f, 1f, Tw.Slate400, maxLines = 1)
                    inner()
                }
            },
        )
        Column(
            Modifier.fillMaxWidth().heightIn(max = rem(14f)).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(rem(0.5f)),
        ) {
            for (cost in listOf(1, 0)) {
                val items = subjects.filter { it.isCost == cost }
                if (items.isEmpty()) continue
                val collapsed = if (term.isNotEmpty()) false
                else if (cost == 1) !ui.subjectExpanded else !ui.subjectIncomeExpanded
                val picked = items.count { it.uid in ui.subjectUids }
                SubjectGroup(
                    title = if (cost == 1) "سرفصل‌های هزینه" else "سرفصل‌های درآمد",
                    items = items,
                    collapsed = collapsed,
                    picked = picked,
                    accent = accent,
                    selectedUids = ui.subjectUids,
                    onToggleCollapse = {
                        if (cost == 1) ui.subjectExpanded = !ui.subjectExpanded
                        else ui.subjectIncomeExpanded = !ui.subjectIncomeExpanded
                    },
                    onToggleGroup = {
                        val uids = items.map { it.uid }
                        val all = uids.all { it in ui.subjectUids }
                        ui.subjectUids = if (all) ui.subjectUids - uids.toSet() else (ui.subjectUids + uids).distinct()
                    },
                    onToggle = { uid -> ui.toggle(TxUi.Kind.SUBJECT, uid) },
                )
            }
        }
    }
}

@Composable
private fun SubjectGroup(
    title: String,
    items: List<Subject>,
    collapsed: Boolean,
    picked: Int,
    accent: Color,
    selectedUids: List<String>,
    onToggleCollapse: () -> Unit,
    onToggleGroup: () -> Unit,
    onToggle: (String) -> Unit,
) {
    val shape = RoundedCornerShape(rem(0.5f))
    Column(
        Modifier.fillMaxWidth()
            .clip(shape)
            .border(cssBorder(), dk(Tw.Slate200.a(0.7f), Tw.Slate800), shape),
    ) {
        Row(
            Modifier.fillMaxWidth()
                .background(dk(Tw.Slate50, Tw.Slate800.a(0.6f)))
                .clickable(remember { MutableInteractionSource() }, null, onClick = onToggleCollapse)
                .padding(horizontal = rem(0.5f), vertical = rem(0.375f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
        ) {
            TwCheckbox(
                checked = picked == items.size && items.isNotEmpty(),
                indeterminate = picked in 1 until items.size,
                onToggle = onToggleGroup,
            )
            LucideIcon(
                R.drawable.ic_lucide_chevron_down, 0.875f, Tw.Slate400,
                Modifier.rotate(if (collapsed) 0f else 180f),
            )
            TwText(title, 0.75f, 1f, dk(Tw.Slate700, Tw.Slate200), weight = FW.Bold, maxLines = 1)
            Box(
                Modifier.background(dk(Tw.Slate200.a(0.7f), Tw.Slate700), RoundedCornerShape(rem(99f)))
                    .padding(horizontal = rem(0.375f), vertical = rem(0.125f)),
            ) {
                TwText(fa(items.size), 0.625f, 1f, dk(Tw.Slate500, Tw.Slate400), maxLines = 1)
            }
            if (picked > 0) {
                TwText("${fa(picked)} انتخاب‌شده", 0.625f, 1f, accent, weight = FW.Bold, maxLines = 1)
            }
        }
        if (!collapsed) {
            Column(Modifier.padding(rem(0.25f)), verticalArrangement = Arrangement.spacedBy(rem(0.25f))) {
                for (s in items) {
                    val checked = s.uid in selectedUids
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable(remember { MutableInteractionSource() }, null) { onToggle(s.uid) }
                            .padding(rem(0.375f)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
                    ) {
                        TwCheckbox(checked = checked, onToggle = { onToggle(s.uid) })
                        CategoryIcon(title = s.title, iconId = s.icon, isCost = s.isCost, size = CategoryIconSize.XS)
                        TwText(
                            s.title, 0.75f, 1f,
                            if (checked) accent else dk(Tw.Slate700, Tw.Slate300),
                            weight = if (checked) FW.Bold else FW.Normal,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonFilterMenu(db: AccountingDatabase, ui: TxUi) {
    val accent = dk(Tw.Emerald600, Tw.Emerald400)
    val persons = db.persons.filter { it.isHidden == 0 }
    DropdownSurface(ui.personMenu, { ui.personMenu = false }, 16.25f) {
        MenuHeader(
            accent = accent,
            onAll = { ui.personUids = persons.map { it.uid } },
            onClear = { ui.personUids = emptyList() },
        )
        Column(
            Modifier.fillMaxWidth().heightIn(max = rem(14f)).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(rem(0.25f)),
        ) {
            for (p in persons) {
                MenuRow(
                    checked = p.uid in ui.personUids,
                    label = p.fullName,
                    accent = accent,
                    onToggle = { ui.toggle(TxUi.Kind.PERSON, p.uid) },
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ type badges */

@Composable
private fun TxTypeRow(ui: TxUi) {
    val anyActive = ui.search.isNotBlank() || ui.type != "all" || ui.accountUids.isNotEmpty() ||
        ui.projectUids.isNotEmpty() || ui.subjectUids.isNotEmpty() || ui.personUids.isNotEmpty() ||
        ui.deptUid != "all" || ui.preset != DatePreset.ALL
    Column(Modifier.fillMaxWidth().padding(top = rem(0.5f)).topDivider(dk(Tw.Slate100, Tw.Slate800))) {
        Row(
            Modifier.fillMaxWidth().padding(top = rem(0.5f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
        ) {
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
            ) {
                TxTypeBadge("همه", ui.type == "all", Tw.Slate800, Tw.Slate100, Tw.White, Tw.Slate900) { ui.type = "all" }
                TxTypeBadge("هزینه‌ها", ui.type == "expense", Tw.Rose500, Tw.Rose50, Tw.White, Tw.Rose600) { ui.type = "expense" }
                TxTypeBadge("درآمدها", ui.type == "income", Tw.Emerald500, Tw.Emerald50, Tw.White, Tw.Emerald600) { ui.type = "income" }
                TxTypeBadge("انتقال‌ها", ui.type == "transfer", Tw.Sky500, Tw.Sky50, Tw.White, Tw.Sky600) { ui.type = "transfer" }
            }
            if (anyActive) {
                Box(
                    Modifier.clickable(remember { MutableInteractionSource() }, null) { ui.reset() }
                        .padding(horizontal = rem(0.75f), vertical = rem(0.375f)),
                ) {
                    LucideIcon(R.drawable.ic_lucide_rotate_ccw, 0.875f, dk(Tw.Rose600, Tw.Rose400))
                }
            }
        }
    }
}

@Composable
private fun TxTypeBadge(
    label: String,
    active: Boolean,
    activeBg: Color,
    idleBg: Color,
    activeFg: Color,
    idleFg: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(rem(0.5f))
    Box(
        Modifier.background(if (active) activeBg else idleBg, shape)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = rem(0.75f), vertical = rem(0.375f)),
    ) {
        TwText(
            label, 0.75f, 1f,
            if (active) activeFg else idleFg,
            weight = FW.Semibold, maxLines = 1,
        )
    }
}
