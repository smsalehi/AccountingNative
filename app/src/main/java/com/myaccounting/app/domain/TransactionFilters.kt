package com.myaccounting.app.domain

import java.time.ZoneId

/**
 * Pure logic of components/TransactionsView.tsx: filter → day groups → totals → project summaries.
 * Kept out of the composable so it can be unit-tested against the original behaviour.
 */
object TransactionFilters {

    enum class DatePreset(val label: String) {
        ALL("همه"),
        TODAY("امروز"),
        LAST7("۷ روز اخیر"),
        THIS_MONTH("این ماه"),
        LAST_MONTH("ماه گذشته"),
        THIS_YEAR("امسال"),
        CUSTOM("سفارشی"),
    }

    data class Criteria(
        val search: String = "",
        /** all | expense | income | transfer */
        val type: String = "all",
        val accountUids: Set<String> = emptySet(),
        val projectUids: Set<String> = emptySet(),
        val subjectUids: Set<String> = emptySet(),
        val personUids: Set<String> = emptySet(),
        val deptUid: String = "all",
        val preset: DatePreset = DatePreset.ALL,
        val customStart: String = "",
        val customEnd: String = "",
    ) {
        val anyActive: Boolean
            get() = search.isNotBlank() || type != "all" || accountUids.isNotEmpty() ||
                projectUids.isNotEmpty() || subjectUids.isNotEmpty() || personUids.isNotEmpty() ||
                deptUid != "all" || preset != DatePreset.ALL
    }

    data class Bounds(val start: String = "", val end: String = "")

    private const val DAY_MS = 24L * 60 * 60 * 1000

    private fun pad(n: Int) = if (n < 10) "0$n" else "$n"

    /** TransactionsView `computedStartDate` / `computedEndDate`. */
    fun bounds(
        criteria: Criteria,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Bounds {
        val preset = criteria.preset
        if (preset == DatePreset.ALL) return Bounds()
        val today = JalaliDate.toJalali(now, zone)
        val todayStr = JalaliDate.formatJalaliDate(now, zone)
        return when (preset) {
            DatePreset.TODAY -> Bounds(todayStr, todayStr)
            DatePreset.LAST7 -> Bounds(JalaliDate.formatJalaliDate(now - 6 * DAY_MS, zone), todayStr)
            DatePreset.THIS_MONTH -> Bounds(
                "${today.y}/${pad(today.m)}/01",
                "${today.y}/${pad(today.m)}/${if (today.m <= 6) 31 else 30}",
            )
            DatePreset.LAST_MONTH -> {
                val (jy, jm) = JalaliDate.addJalaliMonths(today.y, today.m, -1)
                Bounds("$jy/${pad(jm)}/01", "$jy/${pad(jm)}/${if (jm <= 6) 31 else 30}")
            }
            DatePreset.THIS_YEAR -> Bounds("${today.y}/01/01", "${today.y}/12/29")
            DatePreset.CUSTOM -> Bounds(criteria.customStart.trim(), criteria.customEnd.trim())
            DatePreset.ALL -> Bounds()
        }
    }

    private fun typeMatches(type: String, txType: Int): Boolean = when (type) {
        "expense" -> txType == 1
        "income" -> txType == 2
        "transfer" -> txType == 3
        else -> true
    }

    /** TransactionsView `filteredTransactions`. */
    fun apply(
        db: AccountingDatabase,
        criteria: Criteria,
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Transaction> {
        val term = criteria.search.trim().lowercase()
        val bound = bounds(criteria, now, zone)
        val startNorm = if (bound.start.isNotEmpty()) JalaliDate.normalizePersianDate(bound.start) else ""
        val endNorm = if (bound.end.isNotEmpty()) JalaliDate.normalizePersianDate(bound.end) else ""

        val subjects = db.subjects.associateBy { it.uid }
        val accounts = db.accounts.associateBy { it.uid }
        val persons = db.persons.associateBy { it.uid }

        return db.transactions.filter { t ->
            if (!typeMatches(criteria.type, t.transactionType)) return@filter false

            if (criteria.accountUids.isNotEmpty()) {
                val src = t.sourceAccountUid
                val dst = t.destinationAccountUid
                val matchSrc = src != null && src in criteria.accountUids
                val matchDst = dst != null && dst in criteria.accountUids
                if (!matchSrc && !matchDst) return@filter false
            }

            if (criteria.deptUid != "all" && t.deptUid != criteria.deptUid) return@filter false

            if (criteria.projectUids.isNotEmpty()) {
                val unassigned = t.projectUid.isNullOrEmpty()
                val matchUnassigned = "unassigned" in criteria.projectUids && unassigned
                val matchProject = !unassigned && t.projectUid in criteria.projectUids
                if (!matchUnassigned && !matchProject) return@filter false
            }

            if (criteria.subjectUids.isNotEmpty()) {
                if (t.subjectUid == null || t.subjectUid !in criteria.subjectUids) return@filter false
            }

            if (criteria.personUids.isNotEmpty()) {
                if (t.personUid == null || t.personUid !in criteria.personUids) return@filter false
            }

            if (startNorm.isNotEmpty() || endNorm.isNotEmpty()) {
                val n = JalaliDate.normalizePersianDate(t.persianDate)
                if (startNorm.isNotEmpty() && n < startNorm) return@filter false
                if (endNorm.isNotEmpty() && n > endNorm) return@filter false
            }

            if (term.isNotEmpty()) {
                val descMatch = t.desc?.lowercase()?.contains(term) == true
                val dateMatch = t.persianDate.contains(term)
                val subjectMatch = t.subjectUid?.let { subjects[it]?.title?.lowercase()?.contains(term) } == true
                val accountMatch = t.sourceAccountUid?.let { accounts[it]?.title?.lowercase()?.contains(term) } == true
                val personMatch = t.personUid?.let { persons[it]?.fullName?.lowercase()?.contains(term) } == true
                return@filter descMatch || dateMatch || subjectMatch || accountMatch || personMatch
            }

            true
        }
    }

    data class DayGroup(
        val date: String,
        val transactions: List<Transaction>,
        val totalIncome: Double,
        val totalExpense: Double,
        val dailyBalance: Double,
    )

    /** TransactionsView `dailyGroups`: newest day first, newest transaction first inside a day. */
    fun groupByDay(transactions: List<Transaction>): List<DayGroup> {
        val sorted = transactions.sortedWith(
            compareByDescending<Transaction> { JalaliDate.normalizePersianDate(it.persianDate) }
                .thenByDescending { it.dateTime },
        )
        val groups = LinkedHashMap<String, MutableList<Transaction>>()
        for (tx in sorted) {
            val key = tx.persianDate.ifEmpty { "نامشخص" }
            groups.getOrPut(key) { mutableListOf() }.add(tx)
        }
        return groups.map { (date, list) ->
            var income = 0.0
            var expense = 0.0
            for (tx in list) {
                // The DB stores expenses with a negative sign; the direction comes from the type, not the sign.
                if (tx.transactionType == 2) income += kotlin.math.abs(tx.totalValue)
                if (tx.transactionType == 1) expense += kotlin.math.abs(tx.totalValue)
            }
            DayGroup(date, list, income, expense, income - expense)
        }
    }

    data class Totals(val income: Double, val expense: Double, val balance: Double)

    fun totals(transactions: List<Transaction>): Totals {
        var income = 0.0
        var expense = 0.0
        for (tx in transactions) {
            val a = kotlin.math.abs(tx.totalValue)
            if (tx.transactionType == 2) income += a
            if (tx.transactionType == 1) expense += a
        }
        return Totals(income, expense, income - expense)
    }

    data class ProjectSummary(
        val project: Project,
        val txCount: Int,
        val expenses: Double,
        val incomes: Double,
        val balance: Double,
    )

    data class ProjectSummaries(
        val items: List<ProjectSummary>,
        val unassignedExpenses: Double,
        val unassignedIncomes: Double,
        val unassignedBalance: Double,
    )

    /** TransactionsView `projectSummaries`: one pass over all transactions, sorted by |balance|. */
    fun projectSummaries(db: AccountingDatabase): ProjectSummaries {
        class Agg(var count: Int = 0, var expenses: Double = 0.0, var incomes: Double = 0.0)

        val byProject = HashMap<String, Agg>()
        var unassignedExpense = 0.0
        var unassignedIncome = 0.0
        for (tx in db.transactions) {
            val amount = kotlin.math.abs(tx.totalValue)
            val uid = tx.projectUid
            if (!uid.isNullOrEmpty()) {
                val agg = byProject.getOrPut(uid) { Agg() }
                agg.count++
                if (tx.transactionType == 1) agg.expenses += amount
                if (tx.transactionType == 2) agg.incomes += amount
            } else {
                if (tx.transactionType == 1) unassignedExpense += amount
                if (tx.transactionType == 2) unassignedIncome += amount
            }
        }
        val items = db.projects.map { project ->
            val agg = byProject[project.uid] ?: Agg()
            ProjectSummary(
                project = project,
                txCount = agg.count,
                expenses = agg.expenses,
                incomes = agg.incomes,
                balance = agg.incomes - agg.expenses,
            )
        }.sortedByDescending { kotlin.math.abs(it.balance) }
        return ProjectSummaries(items, unassignedExpense, unassignedIncome, unassignedIncome - unassignedExpense)
    }

    /** TransactionsView `formatMillionToman` — «۱۲ م.ت» with the sign *after* the digits (RTL). */
    fun millionToman(amount: Double, unit: CurrencyUnit): String {
        if (unit == CurrencyUnit.USD) return Currency.formatCurrency(amount, CurrencyUnit.USD)
        val million = Currency.jsRound(kotlin.math.abs(amount) / 1_000_000.0)
        val prefix = if (amount < 0) "-" else ""
        return "${JalaliDate.toPersianDigits(Currency.formatNumberWithCommas(million))}$prefix م.ت"
    }
}
