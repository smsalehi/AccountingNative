package com.myaccounting.app.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myaccounting.app.R
import com.myaccounting.app.domain.Account
import com.myaccounting.app.domain.Currency
import com.myaccounting.app.domain.CurrencyUnit
import com.myaccounting.app.domain.JalaliDate
import com.myaccounting.app.domain.Person
import com.myaccounting.app.domain.Subject
import com.myaccounting.app.domain.Transaction
import com.myaccounting.app.domain.TransactionFilters
import com.myaccounting.app.domain.TransactionFilters.DatePreset
import com.myaccounting.app.ui.components.BankLogo
import com.myaccounting.app.ui.components.CategoryIcon
import com.myaccounting.app.ui.components.CategoryIconSize
import com.myaccounting.app.ui.components.ConfirmDialog
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.LocalIsDark
import com.myaccounting.app.ui.theme.ProjectColors
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem
import com.myaccounting.app.viewmodel.MainViewModel

/** One transaction with everything the row needs (the web builds the same object in `flatItems`). */
private class TxRowData(
    val t: Transaction,
    val subject: Subject?,
    val parentSubject: Subject?,
    val srcAccount: Account?,
    val destAccount: Account?,
    val projectTitle: String?,
    val deptTitle: String?,
    val person: Person?,
    val bankNameSrc: String?,
    val bankNameDest: String?,
)

/** Default display window on the transactions tab (days) and its hard ceiling (10 years). */
private const val DEFAULT_WINDOW_DAYS = 7
private const val MAX_WINDOW_DAYS = 3650

/**
 * `TransactionsView.tsx` ported to Compose: the page header, the collapsible project balance
 * cards, the filter card (search, date presets, the multi-select pickers, the type badges), the
 * summary metrics and the day-grouped transaction list.
 */
@Composable
fun TransactionsScreen(
    state: MainViewModel.UiState,
    vm: MainViewModel,
    ui: TxUi = remember { TxUi() },
    onEditTransaction: (tx: Transaction) -> Unit = {},
    onExportExcel: (List<Transaction>) -> Unit = {},
    onExportPdf: (List<Transaction>) -> Unit = {},
) {
    if (!state.loaded) return
    val db = state.shownDb
    val unit = state.effectiveUnit

    val accountsById = remember(db.accounts) { db.accounts.associateBy { it.uid } }
    val subjectsById = remember(db.subjects) { db.subjects.associateBy { it.uid } }
    val projectsById = remember(db.projects) { db.projects.associateBy { it.uid } }
    val personsById = remember(db.persons) { db.persons.associateBy { it.uid } }
    val deptsById = remember(db.depts) { db.depts.associateBy { it.uid } }
    val bankTitlesById = remember(db.banks) { db.banks.associate { it.uid to it.title } }

    val criteria = TransactionFilters.Criteria(
        search = ui.search,
        type = ui.type,
        accountUids = ui.accountUids.toSet(),
        projectUids = ui.projectUids.toSet(),
        subjectUids = ui.subjectUids.toSet(),
        personUids = ui.personUids.toSet(),
        deptUid = ui.deptUid,
        preset = ui.preset,
        customStart = ui.customStart,
        customEnd = ui.customEnd,
    )
    val filteredAll = remember(db.transactions, criteria) { TransactionFilters.apply(db, criteria) }
    // پیش‌فرض فقط هفته اخیر نمایش داده می‌شود؛ اسکرول به پایین ماه‌به‌ماه قدیمی‌ترها را لود می‌کند.
    // وقتی کاربر خودش بازه تاریخی انتخاب کرده باشد، پنجره اعمال نمی‌شود.
    val explicitRange = ui.preset != DatePreset.ALL || ui.customStart.isNotEmpty() || ui.customEnd.isNotEmpty()
    var windowDays by rememberSaveable { mutableIntStateOf(DEFAULT_WINDOW_DAYS) }
    val filtered = remember(filteredAll, windowDays, explicitRange) {
        if (explicitRange || windowDays >= MAX_WINDOW_DAYS) filteredAll
        else {
            val cutoff = java.util.Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                add(java.util.Calendar.DAY_OF_MONTH, -(windowDays - 1))
            }.timeInMillis
            filteredAll.filter { it.dateTime >= cutoff }
        }
    }
    val groups = remember(filtered) { TransactionFilters.groupByDay(filtered) }
    // آمار خلاصه همیشه روی همه نتایج فیلترشده است (مثل وب)، نه فقط پنجره نمایشی
    val totals = remember(filteredAll) { TransactionFilters.totals(filteredAll) }
    val summaries = remember(db.projects, db.transactions) { TransactionFilters.projectSummaries(db) }
    val hasOlder = !explicitRange && windowDays < MAX_WINDOW_DAYS && filtered.size < filteredAll.size

    val rowsByUid = remember(filtered, accountsById, subjectsById, projectsById, personsById, deptsById, bankTitlesById) {
        filtered.associate { t ->
            val subject = t.subjectUid?.let(subjectsById::get)
            t.uid to TxRowData(
                t = t,
                subject = subject,
                parentSubject = subject?.parentUid?.let(subjectsById::get),
                srcAccount = t.sourceAccountUid?.let(accountsById::get),
                destAccount = t.destinationAccountUid?.let(accountsById::get),
                projectTitle = t.projectUid?.let(projectsById::get)?.title,
                deptTitle = t.deptUid?.let(deptsById::get)?.title,
                person = t.personUid?.let(personsById::get),
                bankNameSrc = t.sourceAccountUid?.let { bankTitlesById[it] },
                bankNameDest = t.destinationAccountUid?.let { bankTitlesById[it] },
            )
        }
    }

    var deleteUid by remember { mutableStateOf<String?>(null) }

    // اسکرول به نزدیکی انتهای لیست → ۳۰ روز قدیمی‌تر لود می‌شود
    val listState = rememberLazyListState()
    LaunchedEffect(listState, filtered.size, hasOlder) {
        if (!hasOlder) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { last ->
                val total = listState.layoutInfo.totalItemsCount
                if (last != null && total > 15 && last >= total - 3) windowDays = (windowDays + 30).coerceAtMost(MAX_WINDOW_DAYS)
            }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = rem(1f), end = rem(1f), top = rem(1f), bottom = rem(4f),
        ),
    ) {
        item(key = "header") {
            TxHeader(
                onExportExcel = { onExportExcel(filteredAll) },
                onExportPdf = { onExportPdf(filteredAll) },
            )
            Spacer(Modifier.height(rem(1.25f)))
        }
        item(key = "projects") {
            ProjectsSection(
                summaries = summaries.items,
                expanded = !ui.projectsCollapsed,
                onToggle = { ui.projectsCollapsed = !ui.projectsCollapsed },
            )
            Spacer(Modifier.height(rem(1.25f)))
        }
        item(key = "filters") {
            TxFilterCard(db, ui)
            Spacer(Modifier.height(rem(1.25f)))
        }
        item(key = "summary") {
            SummaryBar(totals, unit)
            Spacer(Modifier.height(rem(1.25f)))
        }

        if (groups.isEmpty()) {
            item(key = "empty") { EmptyState() }
        } else {
            item(key = "count") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TwText(
                        if (hasOlder) "نمایش ${fa(filtered.size)} از ${fa(filteredAll.size)} تراکنش" else "نمایش ${fa(filteredAll.size)} تراکنش",
                        0.6875f, 1f, Tw.Slate400, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(rem(0.75f)))
            }
            for ((gi, g) in groups.withIndex()) {
                item(key = "h-$gi-${g.date}") { DayHeader(g, unit) }
                val groupRows = g.transactions.mapNotNull { rowsByUid[it.uid] }
                for ((ri, r) in groupRows.withIndex()) {
                    item(key = "r-$gi-${r.t.uid}") {
                        TxRow(
                            row = r,
                            unit = unit,
                            isFirst = ri == 0,
                            isLast = ri == groupRows.lastIndex,
                            isExpanded = ui.expandedTx == r.t.uid,
                            onToggle = { ui.expandedTx = if (ui.expandedTx == r.t.uid) null else r.t.uid },
                            onEdit = { onEditTransaction(r.t) },
                            onDelete = { deleteUid = r.t.uid },
                        )
                    }
                }
                item(key = "gap-$gi") { Spacer(Modifier.height(rem(1f))) }
            }
            if (hasOlder) {
                item(key = "load-more") {
                    Box(
                        Modifier.fillMaxWidth()
                            .clickable(remember { MutableInteractionSource() }, null) {
                                windowDays = (windowDays + 30).coerceAtMost(MAX_WINDOW_DAYS)
                            }
                            .padding(vertical = rem(0.75f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        TwText(
                            "نمایش تراکنش‌های قدیمی‌تر…",
                            0.75f, 1f, dk(Tw.Sky600, Tw.Sky400), weight = FW.Bold, maxLines = 1,
                        )
                    }
                }
            }
        }
    }

    if (ui.accountDialog) {
        AccountPickerDialog(
            groups = db.accountGroups,
            accounts = db.accounts,
            banks = db.banks,
            onClose = { ui.accountDialog = false },
            multi = true,
            selectedUids = ui.accountUids,
            onConfirm = { uids ->
                ui.accountUids = uids
                ui.accountDialog = false
            },
        )
    }

    if (deleteUid != null) {
        ConfirmDialog(
            message = "آیا مطمئن هستید که این تراکنش حذف شود؟",
            confirmLabel = "حذف شود",
            onConfirm = {
                vm.deleteTransaction(deleteUid!!)
                deleteUid = null
            },
            onCancel = { deleteUid = null },
        )
    }
}

/* ------------------------------------------------------------------ header */

@Composable
private fun TxHeader(onExportExcel: () -> Unit, onExportPdf: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(rem(0.5f))) {
        TwText(
            "ریز تراکنش‌ها", 1f, 1.5f, dk(Tw.Slate800, Tw.Slate100),
            weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(rem(0.375f))) {
            ExportButton("خروجی اکسل", R.drawable.ic_lucide_file_spreadsheet, Tw.Emerald600, onExportExcel)
            ExportButton("خروجی PDF", R.drawable.ic_lucide_printer, Tw.Sky600, onExportPdf)
        }
    }
}

@Composable
private fun ExportButton(label: String, icon: Int, tint: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(rem(1f))
    Row(
        Modifier.background(dk(Tw.Slate100, Tw.Slate800), shape)
            .border(cssBorder(), dk(Tw.Slate200, Tw.Slate700), shape)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = rem(0.75f), vertical = rem(0.25f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
    ) {
        LucideIcon(icon, 0.875f, tint)
        TwText(
            label, 0.75f, 1f, dk(Tw.Slate700, Tw.Slate200),
            weight = FW.Semibold, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/* ------------------------------------------------------------------ project cards */

@Composable
private fun ProjectsSection(
    summaries: List<TransactionFilters.ProjectSummary>,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(rem(1f))) {
        Row(
            Modifier.fillMaxWidth()
                .clickable(remember { MutableInteractionSource() }, null, onClick = onToggle)
                .padding(horizontal = rem(0.25f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
            ) {
                LucideIcon(R.drawable.ic_lucide_folder_kanban, 1f, Tw.Sky600)
                TwText(
                    "پروژه‌ها و تراز مالی", 0.875f, 1.25f, dk(Tw.Slate800, Tw.Slate100),
                    weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                LucideIcon(
                    R.drawable.ic_lucide_chevron_down, 0.875f, Tw.Slate400,
                    Modifier.rotate(if (expanded) 180f else 0f),
                )
            }
            TwText(
                "${fa(summaries.size)} پروژه", 0.75f, 1f, Tw.Slate400,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(rem(0.5f))) {
                for ((chunkIndex, chunk) in summaries.chunked(3).withIndex()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f))) {
                        for ((i, s) in chunk.withIndex()) {
                            ProjectCard(s, chunkIndex * 3 + i, Modifier.weight(1f))
                        }
                        for (i in 0 until (3 - chunk.size)) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(summary: TransactionFilters.ProjectSummary, rank: Int, modifier: Modifier = Modifier) {
    val dark = LocalIsDark.current
    val colors = ProjectColors.get(rank)
    val shape = RoundedCornerShape(rem(0.75f))
    Column(
        modifier
            .clip(shape)
            .background(if (dark) colors.bgDark else colors.bg)
            .border(cssBorder(), if (dark) colors.borderDark else colors.border, shape)
            .padding(rem(0.75f)),
        verticalArrangement = Arrangement.spacedBy(rem(0.25f)),
    ) {
        TwText(
            summary.project.title, 0.875f, 1.25f,
            if (dark) colors.textDark else colors.text,
            weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        TwText(
            TransactionFilters.millionToman(summary.balance, CurrencyUnit.TOMAN),
            0.75f, 1f,
            if (dark) colors.textDark else colors.text,
            weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/* ------------------------------------------------------------------ summary */

@Composable
private fun SummaryBar(totals: TransactionFilters.Totals, unit: CurrencyUnit) {
    Column(verticalArrangement = Arrangement.spacedBy(rem(0.75f))) {
        SummaryCard(
            "مجموع درآمد (این لیست)",
            Currency.formatCurrency(totals.income, unit),
            R.drawable.ic_lucide_arrow_up_right,
            dk(Tw.Emerald600, Tw.Emerald400),
            dk(Tw.Emerald50, Tw.Emerald950.a(0.5f)),
        )
        SummaryCard(
            "مجموع هزینه (این لیست)",
            Currency.formatCurrency(totals.expense, unit),
            R.drawable.ic_lucide_arrow_down_left,
            dk(Tw.Rose600, Tw.Rose400),
            dk(Tw.Rose50, Tw.Rose950.a(0.5f)),
        )
        SummaryCard(
            "تراز این لیست",
            Currency.formatCurrency(totals.balance, unit),
            R.drawable.ic_lucide_arrow_left_right,
            dk(Tw.Sky600, Tw.Sky400),
            dk(Tw.Sky50, Tw.Sky950.a(0.5f)),
            valueColor = if (totals.balance >= 0) dk(Tw.Slate800, Tw.Slate100) else dk(Tw.Rose600, Tw.Rose400),
        )
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    icon: Int,
    iconColor: Color,
    iconBg: Color,
    valueColor: Color = dk(Tw.Slate900, Tw.Slate100),
) {
    val shape = RoundedCornerShape(rem(0.75f))
    Row(
        Modifier.fillMaxWidth()
            .background(dk(Tw.White, Tw.Slate900), shape)
            .border(cssBorder(), dk(Tw.Slate200.a(0.8f), Tw.Slate800), shape)
            .padding(rem(0.875f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            TwText(label, 0.6875f, 1f, Tw.Slate400, weight = FW.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            TwText(value, 0.875f, 1.25f, valueColor, weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier.size(rem(2f)).background(iconBg, RoundedCornerShape(rem(0.5f))),
            contentAlignment = Alignment.Center,
        ) {
            LucideIcon(icon, 1f, iconColor)
        }
    }
}

/* ------------------------------------------------------------------ empty state */

@Composable
private fun EmptyState() {
    val shape = RoundedCornerShape(rem(1f))
    Column(
        Modifier.fillMaxWidth()
            .background(dk(Tw.White, Tw.Slate900), shape)
            .border(cssBorder(), dk(Tw.Slate200.a(0.8f), Tw.Slate800), shape)
            .padding(rem(3f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(rem(0.5f)),
    ) {
        Box(
            Modifier.size(rem(3f)).background(dk(Tw.Slate100, Tw.Slate800), RoundedCornerShape(rem(99f))),
            contentAlignment = Alignment.Center,
        ) {
            LucideIcon(R.drawable.ic_lucide_filter, 1.5f, Tw.Slate400)
        }
        TwText(
            "هیچ تراکنشی یافت نشد", 0.875f, 1f, dk(Tw.Slate700, Tw.Slate300),
            weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        TwText(
            "تراکنشی مطابق فیلترهای مشخص شده وجود ندارد.", 0.75f, 1f, Tw.Slate500,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
    }
}

/* ------------------------------------------------------------------ day header */

@Composable
private fun DayHeader(group: TransactionFilters.DayGroup, unit: CurrencyUnit) {
    val shape = RoundedCornerShape(topStart = rem(1f), topEnd = rem(1f))
    Column(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(dk(Tw.White, Tw.Slate900), shape)
            .border(cssBorder(), dk(Tw.Slate200.a(0.8f), Tw.Slate800), shape),
    ) {
        Row(
            Modifier.fillMaxWidth()
                .background(dk(Tw.Slate50, Tw.Slate800.a(0.6f)))
                .padding(horizontal = rem(1f), vertical = rem(0.75f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(rem(0.625f)),
            ) {
                Box(
                    Modifier.size(rem(1.75f)).background(dk(Tw.Sky100, Tw.Sky950.a(0.8f)), RoundedCornerShape(rem(0.5f))),
                    contentAlignment = Alignment.Center,
                ) {
                    LucideIcon(R.drawable.ic_lucide_calendar, 0.875f, dk(Tw.Sky600, Tw.Sky400))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TwText(
                        JalaliDate.toPersianDigits(group.date), 0.75f, 1f, dk(Tw.Slate800, Tw.Slate100),
                        weight = FW.Black, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    TwText(
                        " (${fa(group.transactions.size)} تراکنش)", 0.625f, 1f, Tw.Slate400,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            val positive = group.dailyBalance >= 0
            val chipFg = if (positive) dk(Tw.Emerald700, Tw.Emerald300) else dk(Tw.Rose700, Tw.Rose300)
            Box(
                Modifier.background(
                    if (positive) dk(Tw.Emerald100, Tw.Emerald950.a(0.8f)) else dk(Tw.Rose100, Tw.Rose950.a(0.8f)),
                    RoundedCornerShape(rem(0.5f)),
                ).padding(horizontal = rem(0.625f), vertical = rem(0.25f)),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
                ) {
                    TwText("تراز روز:", 0.625f, 1f, chipFg, maxLines = 1)
                    TwText(
                        Currency.formatCurrency(group.dailyBalance, unit), 0.75f, 1f, chipFg,
                        weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ transaction row */

@Composable
private fun TxRow(
    row: TxRowData,
    unit: CurrencyUnit,
    isFirst: Boolean,
    isLast: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val t = row.t
    val transfer = t.transactionType == 3
    val income = t.transactionType == 2
    val expense = t.transactionType == 1
    val bg = if (isExpanded) dk(Tw.Sky50.a(0.4f), Tw.Sky950.a(0.2f)) else dk(Tw.White, Tw.Slate900)
    val borderColor = dk(Tw.Slate200.a(0.8f), Tw.Slate800)
    val bottomShape = RoundedCornerShape(bottomStart = rem(1f), bottomEnd = rem(1f))

    Column(
        Modifier.fillMaxWidth()
            .then(if (isLast) Modifier.clip(bottomShape) else Modifier)
            .background(bg)
            .then(
                if (isLast) Modifier.border(cssBorder(), borderColor, bottomShape)
                else Modifier.drawBehind {
                    val w = 1.dp.toPx()
                    drawRect(borderColor, size = Size(w, size.height))
                    drawRect(borderColor, topLeft = Offset(size.width - w, 0f), size = Size(w, size.height))
                    if (!isFirst) drawRect(borderColor, size = Size(size.width, w))
                },
            ),
    ) {
        Row(
            Modifier.fillMaxWidth()
                .clickable(remember { MutableInteractionSource() }, null, onClick = onToggle)
                .padding(start = rem(1f), end = rem(0.75f), top = rem(0.625f), bottom = rem(0.625f)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(rem(0.625f)),
        ) {
            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(rem(0.625f)),
                ) {
                    if (transfer) {
                        Box(
                            Modifier.size(rem(1.75f)).background(dk(Tw.Sky100, Tw.Sky950), RoundedCornerShape(rem(0.75f))),
                            contentAlignment = Alignment.Center,
                        ) {
                            LucideIcon(R.drawable.ic_lucide_arrow_left_right, 0.875f, dk(Tw.Sky600, Tw.Sky400))
                        }
                    } else {
                        CategoryIcon(
                            title = row.subject?.title ?: "",
                            iconId = row.subject?.icon,
                            isCost = if (expense) 1 else 0,
                            size = CategoryIconSize.SM,
                        )
                    }
                    Row(
                        Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
                    ) {
                        TwText(
                            t.desc ?: row.subject?.title ?: if (transfer) "انتقال وجه" else "تراکنش",
                            0.875f, 1.25f, dk(Tw.Slate800, Tw.Slate100),
                            weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (!t.time.isNullOrEmpty()) {
                            TwText(
                                JalaliDate.toPersianDigits(t.time), 0.625f, 1f, Tw.Slate400,
                                weight = FW.Medium, maxLines = 1,
                            )
                        }
                    }
                }
                if (row.srcAccount != null) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
                    ) {
                        BankLogo(
                            row.bankNameSrc ?: row.srcAccount.title,
                            row.srcAccount.bankUid,
                            sizeRem = 1.5f,
                            padRem = 0.125f,
                        )
                        TwText(
                            row.srcAccount.title, 0.625f, 1f, Tw.Slate400,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (transfer && row.destAccount != null) {
                            TwText("➔", 0.625f, 1f, Tw.Slate300, modifier = Modifier.rotate(180f))
                            BankLogo(
                                row.bankNameDest ?: row.destAccount.title,
                                row.destAccount.bankUid,
                                sizeRem = 1.5f,
                                padRem = 0.125f,
                            )
                            TwText(
                                row.destAccount.title, 0.625f, 1f, Tw.Slate400,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
            ) {
                TwText(
                    Currency.formatCurrency(kotlin.math.abs(t.totalValue), unit),
                    0.875f, 1.25f,
                    if (income) dk(Tw.Emerald600, Tw.Emerald400)
                    else if (expense) dk(Tw.Rose600, Tw.Rose400)
                    else dk(Tw.Sky600, Tw.Sky400),
                    weight = FW.Black, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Box(Modifier.size(rem(1.25f)), contentAlignment = Alignment.Center) {
                    LucideIcon(
                        R.drawable.ic_lucide_chevron_down, 1f,
                        if (isExpanded) dk(Tw.Sky600, Tw.Sky400) else Tw.Slate400,
                        Modifier.rotate(if (isExpanded) 180f else 0f),
                    )
                }
            }
        }

        if (isExpanded) {
            Column(
                Modifier.fillMaxWidth()
                    .background(dk(Tw.Slate50.a(0.8f), Tw.Slate800.a(0.4f)))
                    .padding(start = rem(1f), end = rem(1f), top = rem(0.75f), bottom = rem(1f)),
                verticalArrangement = Arrangement.spacedBy(rem(0.75f)),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(rem(0.625f))) {
                    if (row.srcAccount != null) {
                        DetailTile(
                            icon = {
                                BankLogo(
                                    row.bankNameSrc ?: row.srcAccount.title,
                                    row.srcAccount.bankUid,
                                    sizeRem = 1.75f,
                                    padRem = 0.25f,
                                )
                            },
                            label = if (transfer) "حساب مبدا:" else "حساب / کارت:",
                            value = row.srcAccount.title,
                        )
                    }
                    if (transfer && row.destAccount != null) {
                        DetailTile(
                            icon = {
                                BankLogo(
                                    row.bankNameDest ?: row.destAccount.title,
                                    row.destAccount.bankUid,
                                    sizeRem = 1.75f,
                                    padRem = 0.25f,
                                )
                            },
                            label = "حساب مقصد:",
                            value = row.destAccount.title,
                        )
                    }
                    if (row.subject != null) {
                        DetailTile(
                            icon = {
                                CategoryIcon(
                                    title = row.subject.title,
                                    iconId = row.subject.icon,
                                    isCost = row.subject.isCost,
                                    size = CategoryIconSize.SM,
                                )
                            },
                            label = "سرفصل مالی:",
                            value = (row.parentSubject?.title?.plus(" / ") ?: "") + row.subject.title,
                        )
                    }
                    if (row.person != null) {
                        DetailTile(
                            icon = {
                                Box(
                                    Modifier.size(rem(1.75f)).background(dk(Tw.Emerald100, Tw.Emerald950), RoundedCornerShape(rem(0.5f))),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    LucideIcon(R.drawable.ic_lucide_user, 0.875f, dk(Tw.Emerald600, Tw.Emerald400))
                                }
                            },
                            label = "طرف حساب:",
                            value = row.person.fullName,
                        )
                    }
                    if (!row.projectTitle.isNullOrEmpty()) {
                        DetailTile(
                            icon = {
                                Box(
                                    Modifier.size(rem(1.75f)).background(dk(Tw.Indigo100, Tw.Indigo950), RoundedCornerShape(rem(0.5f))),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    LucideIcon(R.drawable.ic_lucide_briefcase, 0.875f, dk(Tw.Indigo600, Tw.Indigo400))
                                }
                            },
                            label = "پروژه:",
                            value = row.projectTitle,
                        )
                    }
                    if (!row.deptTitle.isNullOrEmpty()) {
                        DetailTile(
                            icon = {
                                Box(
                                    Modifier.size(rem(1.75f)).background(dk(Tw.Amber100, Tw.Amber950), RoundedCornerShape(rem(0.5f))),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    LucideIcon(R.drawable.ic_lucide_tag, 0.875f, dk(Tw.Amber600, Tw.Amber400))
                                }
                            },
                            label = "وام مرتبط:",
                            value = row.deptTitle,
                        )
                    }
                    DetailTile(
                        icon = {
                            Box(
                                Modifier.size(rem(1.75f)).background(dk(Tw.Slate100, Tw.Slate800), RoundedCornerShape(rem(0.5f))),
                                contentAlignment = Alignment.Center,
                            ) {
                                LucideIcon(R.drawable.ic_lucide_clock, 0.875f, Tw.Slate500)
                            }
                        },
                        label = "تاریخ و زمان:",
                        value = JalaliDate.toPersianDigits(t.persianDate) +
                            (if (!t.time.isNullOrEmpty()) " - ساعت " + JalaliDate.toPersianDigits(t.time) else ""),
                    )
                }

                if (!t.desc.isNullOrEmpty()) {
                    Column(
                        Modifier.fillMaxWidth()
                            .background(dk(Tw.White, Tw.Slate900), RoundedCornerShape(rem(0.75f)))
                            .border(cssBorder(), dk(Tw.Slate200.a(0.7f), Tw.Slate800), RoundedCornerShape(rem(0.75f)))
                            .padding(rem(0.625f)),
                    ) {
                        TwText("یادداشت و شرح کامل:", 0.625f, 1f, Tw.Slate400, weight = FW.Medium, maxLines = 1)
                        TwText(
                            t.desc, 0.75f, 1.25f, dk(Tw.Slate700, Tw.Slate300),
                            maxLines = 6, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ActionButton(
                        label = "حذف تراکنش",
                        icon = R.drawable.ic_lucide_trash2,
                        bg = dk(Tw.Rose50, Tw.Rose950.a(0.6f)),
                        fg = dk(Tw.Rose600, Tw.Rose400),
                        onClick = onDelete,
                    )
                    ActionButton(
                        label = "ویرایش تراکنش",
                        icon = R.drawable.ic_lucide_pencil,
                        bg = Tw.Sky600,
                        fg = Tw.White,
                        bold = true,
                        onClick = onEdit,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailTile(icon: @Composable () -> Unit, label: String, value: String) {
    Row(
        Modifier.fillMaxWidth()
            .background(dk(Tw.White, Tw.Slate900), RoundedCornerShape(rem(0.75f)))
            .border(cssBorder(), dk(Tw.Slate200.a(0.7f), Tw.Slate800), RoundedCornerShape(rem(0.75f)))
            .padding(rem(0.5f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
    ) {
        icon()
        Column(Modifier.weight(1f)) {
            TwText(label, 0.625f, 1f, Tw.Slate400, weight = FW.Medium, maxLines = 1)
            TwText(
                value, 0.75f, 1f, dk(Tw.Slate800, Tw.Slate200),
                weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: Int,
    bg: Color,
    fg: Color,
    bold: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(rem(0.75f))
    Row(
        Modifier.background(bg, shape)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = if (bold) rem(1f) else rem(0.75f), vertical = rem(0.375f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
    ) {
        LucideIcon(icon, 0.875f, fg)
        TwText(
            label, 0.75f, 1f, fg,
            weight = if (bold) FW.Bold else FW.Semibold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}
