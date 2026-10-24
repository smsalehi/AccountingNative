package com.myaccounting.app.ui.persons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import com.myaccounting.app.R
import com.myaccounting.app.domain.Checks
import com.myaccounting.app.domain.Currency
import com.myaccounting.app.domain.CurrencyUnit
import com.myaccounting.app.domain.JalaliDate
import com.myaccounting.app.domain.Person
import com.myaccounting.app.domain.PersonGroup
import com.myaccounting.app.domain.Transaction
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.Vazirmatn
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem
import com.myaccounting.app.ui.theme.remSp
import com.myaccounting.app.viewmodel.MainViewModel

/** One person's balance, computed exactly like personStats in PersonsView.tsx. */
private class PersonStat(val income: Double, val expense: Double, val combined: Double)

private class PersonSection(val key: String, val groupUid: String?, val title: String, val persons: List<Person>)

/**
 * PersonsView.tsx ported to Compose: header, real owed/due summary from transactions,
 * search, foldable person groups and person cards with their true balance.
 */
@Composable
fun PersonsScreen(
    state: MainViewModel.UiState,
    vm: MainViewModel,
    onOpenNewTransactionForPerson: (String) -> Unit = {},
) {
    if (!state.loaded) return
    val db = state.shownDb
    val unit = state.effectiveUnit
    var search by rememberSaveable { mutableStateOf("") }
    var showHidden by rememberSaveable { mutableStateOf(false) }
    var collapsed by remember { mutableStateOf<Set<String>>(emptySet()) }
    // PersonModal.tsx isOpen + person (null = add mode).
    var personDialogOpen by remember { mutableStateOf(false) }
    var editingPerson by remember { mutableStateOf<Person?>(null) }
    val term = search.trim()

    // balance = OldBalance + income(type 0/2) - expense(type 1), attributed only via PersonUID.
    val stats: Map<String, PersonStat> = remember(db.persons, db.transactions) {
        val agg = HashMap<String, DoubleArray>() // [income, expense]
        for (t: Transaction in db.transactions) {
            val uid = t.personUid
            if (uid.isNullOrEmpty()) continue
            val v = t.totalValue
            if (!v.isFinite() || v == 0.0) continue
            val amount = Math.abs(v)
            val slot = agg.getOrPut(uid) { DoubleArray(2) }
            when (t.transactionType) {
                0, 2 -> slot[0] += amount
                1 -> slot[1] += amount
            }
        }
        db.persons.associate { p ->
            val slot = agg[p.uid]
            val income = slot?.get(0) ?: 0.0
            val expense = slot?.get(1) ?: 0.0
            val opening = if (p.oldBalance.isFinite()) p.oldBalance else 0.0
            p.uid to PersonStat(income, expense, opening + income - expense)
        }
    }

    var totalOwed = 0.0
    var totalDue = 0.0
    for (p in db.persons) {
        if (p.isHidden == 1) continue
        val c = stats[p.uid]?.combined ?: 0.0
        if (c < 0) totalOwed += -c else totalDue += c
    }

    val matchPerson: (Person) -> Boolean = { p ->
        (showHidden || p.isHidden == 0) &&
            (term.isEmpty() || p.fullName.contains(term, ignoreCase = true) || p.desc.contains(term, ignoreCase = true))
    }

    val sections: List<PersonSection> = remember(db.persons, db.personGroups, term, showHidden) {
        val grouped = db.personGroups.map { g: PersonGroup ->
            PersonSection(g.uid, g.uid, g.title, db.persons.filter { it.groupUid == g.uid && matchPerson(it) })
        }.filter { it.persons.isNotEmpty() }
        val known = db.personGroups.map { it.uid }.toSet()
        val others = db.persons.filter { (it.groupUid.isEmpty() || it.groupUid !in known) && matchPerson(it) }
        if (others.isEmpty()) grouped else grouped + PersonSection("__other", null, "سایر", others)
    }

    // «چک‌های کشیده‌شده توسط این شخص» badge data (ChecksView's checkSummaryByPerson).
    val checkSummaries = remember(db.checks, db.transactions) { Checks.pendingPersonChecksByDrawer(db) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = rem(1f), end = rem(1f), top = rem(1f), bottom = rem(4f)),
        verticalArrangement = Arrangement.spacedBy(rem(1.5f)),
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(rem(1f))) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TwText("اشخاص و طرف‌حساب‌ها", 1.25f, 1.75f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1)
                    Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f))) {
                        PillButton("شخص جدید", R.drawable.ic_lucide_plus, Tw.White, Tw.Sky600) {
                            editingPerson = null
                            personDialogOpen = true
                        }
                        PillButton(
                            "راکد",
                            if (showHidden) R.drawable.ic_lucide_eye_off else R.drawable.ic_lucide_eye,
                            dk(Tw.Slate700, Tw.Slate200),
                            dk(Tw.Slate100, Tw.Slate800),
                        ) { showHidden = !showHidden }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(rem(1f))) {
                    SummaryCard(
                        label = "مجموع طلب شما از دیگران (بدهکاران به شما)",
                        value = Currency.formatCurrency(totalOwed, unit),
                        accent = dk(Tw.Emerald600, Tw.Emerald400),
                        bg = Tw.Emerald500.a(0.10f),
                        border = Tw.Emerald500.a(0.20f),
                        icon = R.drawable.ic_lucide_arrow_down_left,
                        modifier = Modifier.weight(1f),
                    )
                    SummaryCard(
                        label = "مجموع بدهی شما به دیگران (بستانکاران)",
                        value = Currency.formatCurrency(totalDue, unit),
                        accent = dk(Tw.Rose600, Tw.Rose400),
                        bg = Tw.Rose500.a(0.10f),
                        border = Tw.Rose500.a(0.20f),
                        icon = R.drawable.ic_lucide_arrow_up_right,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        item(key = "search") {
            val outer = RoundedCornerShape(rem(1f))
            Box(
                Modifier.fillMaxWidth()
                    .background(dk(Tw.White, Tw.Slate900), outer)
                    .border(cssBorder(), dk(Tw.Slate200.a(0.8f), Tw.Slate800), outer)
                    .padding(rem(1f)),
            ) {
                Box(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier.fillMaxWidth()
                            .background(dk(Tw.Slate50, Tw.Slate800.a(0.8f)), RoundedCornerShape(rem(0.75f)))
                            .border(cssBorder(), dk(Tw.Slate200, Tw.Slate700), RoundedCornerShape(rem(0.75f)))
                            .padding(start = rem(2.5f), end = rem(0.75f), top = rem(0.5f), bottom = rem(0.5f)),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        BasicTextField(
                            value = search,
                            onValueChange = { search = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = Vazirmatn,
                                fontSize = remSp(0.75f),
                                color = dk(Tw.Slate900, Tw.Slate100),
                                textDirection = TextDirection.Rtl,
                            ),
                            cursorBrush = SolidColor(Tw.Sky500),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                Box(Modifier.fillMaxWidth()) {
                                    if (search.isEmpty()) {
                                        TwText("جستجوی نام یا مشخصات شخص...", 0.75f, 1.25f, Tw.Slate400, maxLines = 1)
                                    }
                                    inner()
                                }
                            },
                        )
                    }
                    LucideIcon(
                        R.drawable.ic_lucide_search, 1f, Tw.Slate400,
                        Modifier.align(Alignment.CenterStart).padding(start = rem(0.875f)),
                    )
                }
            }
        }

        if (sections.isEmpty()) {
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
                    LucideIcon(R.drawable.ic_lucide_users, 2.5f, Tw.Slate400)
                    TwText("شخصی یافت نشد", 0.875f, 1.25f, dk(Tw.Slate700, Tw.Slate300), weight = FW.Bold)
                    TwText(
                        if (term.isEmpty()) "هنوز شخصی ثبت نشده است." else "جستجو نتیجه‌ای نداشت.",
                        0.75f, 1f, Tw.Slate500,
                    )
                }
            }
        }

        for (section in sections) {
            item(key = "section-" + section.key) {
                val isCollapsed = term.isEmpty() && section.key in collapsed
                val sectionTotal = section.persons.sumOf { stats[it.uid]?.combined ?: 0.0 }
                val card = RoundedCornerShape(rem(1f))
                Column(
                    Modifier.fillMaxWidth()
                        .background(dk(Tw.White, Tw.Slate900), card)
                        .border(cssBorder(), dk(Tw.Slate200.a(0.8f), Tw.Slate800), card),
                ) {
                    Row(
                        Modifier.fillMaxWidth()
                            .background(dk(Tw.Slate50.a(0.8f), Tw.Slate800.a(0.5f)), RoundedCornerShape(topStart = rem(1f), topEnd = rem(1f)))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { if (section.groupUid != null) collapsed = if (isCollapsed) collapsed - section.key else collapsed + section.key }
                            .padding(horizontal = rem(1f), vertical = rem(0.875f)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(rem(0.625f)),
                        ) {
                            Box(
                                Modifier.size(rem(1.5f))
                                    .background(dk(Tw.White, Tw.Slate800), RoundedCornerShape(rem(0.5f)))
                                    .border(cssBorder(), dk(Tw.Slate200, Tw.Slate700), RoundedCornerShape(rem(0.5f))),
                                contentAlignment = Alignment.Center,
                            ) {
                                LucideIcon(
                                    if (isCollapsed) R.drawable.ic_lucide_chevron_down else R.drawable.ic_lucide_chevron_up,
                                    1f, dk(Tw.Sky600, Tw.Sky400),
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                TwText(section.title, 0.875f, 1.25f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                TwText(JalaliDate.toPersianDigits(section.persons.size) + " شخص", 0.6875f, 1f, Tw.Slate500, maxLines = 1)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            TwText("تراز گروه:", 0.625f, 0.875f, Tw.Slate400, maxLines = 1)
                            TwText(Currency.formatCurrency(sectionTotal, unit), 0.875f, 1.25f, dk(Tw.Slate900, Tw.Slate100), weight = FW.Black, maxLines = 1)
                        }
                    }

                    if (!isCollapsed) {
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = rem(0.75f), vertical = rem(0.75f)),
                            verticalArrangement = Arrangement.spacedBy(rem(1f)),
                        ) {
                            for (p in section.persons) {
                                PersonCard(
                                    p = p,
                                    stat = stats[p.uid],
                                    summary = checkSummaries[p.uid],
                                    unit = unit,
                                    onEdit = { editingPerson = p; personDialogOpen = true },
                                    onNewTransaction = { onOpenNewTransactionForPerson(p.uid) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (personDialogOpen) {
        PersonDialog(
            person = editingPerson,
            groups = db.personGroups,
            unit = unit,
            onClose = { personDialogOpen = false },
            onSave = { vm.savePerson(it) },
            onError = { vm.warn(it) },
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    accent: Color,
    bg: Color,
    border: Color,
    icon: Int,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(rem(1f))
    Row(
        modifier
            .background(bg, shape)
            .border(cssBorder(), border, shape)
            .padding(rem(1f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(rem(0.25f))) {
            TwText(label, 0.75f, 1.5f, dk(Tw.Slate700, Tw.Slate300), maxLines = 2)
            TwText(value, 1.25f, 1.75f, accent, weight = FW.Black, maxLines = 1)
        }
        Box(
            Modifier.size(rem(2.5f)).background(accent.a(0.20f), RoundedCornerShape(rem(0.75f))),
            contentAlignment = Alignment.Center,
        ) { LucideIcon(icon, 1.25f, accent) }
    }
}

@Composable
private fun PillButton(label: String, icon: Int, tint: Color, bg: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(rem(0.75f))
    Row(
        Modifier.background(bg, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = rem(0.875f), vertical = rem(0.5f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
    ) {
        LucideIcon(icon, 1f, tint)
        TwText(label, 0.75f, 1f, tint, weight = FW.Semibold, maxLines = 1)
    }
}

@Composable
private fun PersonCard(
    p: Person,
    stat: PersonStat?,
    summary: Checks.DrawerSummary?,
    unit: CurrencyUnit,
    onEdit: () -> Unit,
    onNewTransaction: () -> Unit,
) {
    val combined = stat?.combined ?: 0.0
    val isDebtor = combined < 0
    val isCreditor = combined > 0
    val shape = RoundedCornerShape(rem(1f))
    Column(
        Modifier.fillMaxWidth()
            .background(dk(Tw.White, Tw.Slate900), shape)
            .border(cssBorder(), dk(Tw.Slate200.a(0.8f), Tw.Slate800), shape)
            .padding(rem(1f)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(rem(0.625f))) {
            Box(
                Modifier.size(rem(2.5f)).background(dk(Tw.Sky100, Tw.Sky950), RoundedCornerShape(rem(0.75f))),
                contentAlignment = Alignment.Center,
            ) {
                TwText(
                    p.fullName.take(1),
                    0.875f, 1.25f,
                    dk(Tw.Sky700, Tw.Sky300),
                    weight = FW.Bold,
                    maxLines = 1,
                )
            }
            TwText(
                p.fullName, 0.875f, 1.25f, dk(Tw.Slate800, Tw.Slate100),
                weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            Box(
                Modifier.size(rem(1.5f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onEdit),
                contentAlignment = Alignment.Center,
            ) { LucideIcon(R.drawable.ic_lucide_pencil, 0.875f, Tw.Slate400) }
            LucideIcon(R.drawable.ic_lucide_grip_vertical, 1f, Tw.Slate300)
        }

        if (p.mobile.isNotEmpty() || p.desc.isNotEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(top = rem(0.875f)),
                verticalArrangement = Arrangement.spacedBy(rem(0.375f)),
            ) {
                if (p.mobile.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TwText("شماره همراه:", 0.6875f, 1f, Tw.Slate400, maxLines = 1)
                        TwText(JalaliDate.toPersianDigits(p.mobile), 0.6875f, 1f, dk(Tw.Slate700, Tw.Slate300), maxLines = 1)
                    }
                }
                if (p.desc.isNotEmpty()) {
                    TwText(p.desc, 0.6875f, 1.25f, Tw.Slate500, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        if (summary != null) {
            val bannerShape = RoundedCornerShape(rem(0.75f))
            Box(
                Modifier.fillMaxWidth().padding(top = rem(0.75f))
                    .background(dk(Tw.Amber50, Tw.Amber950.a(0.4f)), bannerShape)
                    .border(cssBorder(), dk(Tw.Amber200.a(0.7f), Tw.Amber900.a(0.6f)), bannerShape)
                    .padding(horizontal = rem(0.625f), vertical = rem(0.375f)),
            ) {
                TwText(
                    JalaliDate.toPersianDigits(summary.count) + " چک در انتظار" +
                        (if (summary.remaining > 0) " • مانده واریز " + Currency.formatCurrency(summary.remaining, unit) else " • واریز کامل"),
                    0.6875f, 1.5f, dk(Tw.Amber800, Tw.Amber300), maxLines = 2,
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = rem(1f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val btnShape = RoundedCornerShape(rem(0.75f))
            Row(
                Modifier.background(dk(Tw.Sky50, Tw.Sky950.a(0.6f)), btnShape)
                    .border(cssBorder(), dk(Tw.Sky200, Tw.Sky800), btnShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onNewTransaction)
                    .padding(horizontal = rem(0.625f), vertical = rem(0.375f)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(rem(0.25f)),
            ) {
                LucideIcon(R.drawable.ic_lucide_circle_plus, 0.875f, dk(Tw.Sky600, Tw.Sky400))
                TwText("ثبت تراکنش", 0.75f, 1f, dk(Tw.Sky600, Tw.Sky400), weight = FW.Bold, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                TwText("وضعیت حساب:", 0.625f, 0.875f, Tw.Slate400, maxLines = 1)
                TwText(
                    when {
                        isDebtor -> "طلبکار: " + Currency.formatCurrency(Math.abs(combined), unit)
                        isCreditor -> "بدهکار: " + Currency.formatCurrency(combined, unit)
                        else -> "تراز صفر (تسویه)"
                    },
                    0.75f, 1.25f,
                    when {
                        isDebtor -> dk(Tw.Emerald600, Tw.Emerald400)
                        isCreditor -> dk(Tw.Rose600, Tw.Rose400)
                        else -> Tw.Slate500
                    },
                    weight = FW.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}
