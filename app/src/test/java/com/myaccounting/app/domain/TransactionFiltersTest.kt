package com.myaccounting.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/**
 * The filter/grouping logic of TransactionsView.tsx (the pure part), so the Compose port and the
 * original web app agree on which rows are visible and what the summary numbers are.
 */
class TransactionFiltersTest {

    private val zone = ZoneId.of("Asia/Tehran")

    private fun tx(
        uid: String,
        type: Int = 1,
        value: Double = 1000.0,
        date: String = "1405/07/30",
        time: String? = "10:00",
        source: String? = "acc_1",
        dest: String? = null,
        subject: String? = null,
        person: String? = null,
        project: String? = null,
        dept: String? = null,
        desc: String? = null,
    ) = Transaction(
        uid = uid,
        transactionType = type,
        totalValue = value,
        persianDate = date,
        time = time,
        dateTime = 1_700_000_000_000L,
        sourceAccountUid = source,
        destinationAccountUid = dest,
        subjectUid = subject,
        personUid = person,
        projectUid = project,
        deptUid = dept,
        desc = desc,
    )

    private val db = AccountingDatabase(
        accounts = listOf(
            Account(uid = "acc_1", title = "ملت جاری"),
            Account(uid = "acc_2", title = "حکمت"),
        ),
        subjects = listOf(
            Subject(uid = "s1", title = "قبوض و شارژ", isCost = 1),
            Subject(uid = "s2", title = "حقوق و دستمزد", isCost = 0),
        ),
        persons = listOf(Person(uid = "p1", fullName = "علی رضایی")),
        projects = listOf(Project(uid = "pr1", title = "پروژه نمونه")),
        transactions = listOf(
            tx("t1", type = 1, value = -350_000.0, date = "1405/07/30", subject = "s1", desc = "بنزین"),
            tx("t2", type = 2, value = 45_000_000.0, date = "1405/07/29", subject = "s2", source = "acc_2", person = "p1"),
            tx("t3", type = 3, value = -5_000_000.0, date = "1405/07/28", source = "acc_1", dest = "acc_2", project = "pr1"),
            tx("t4", type = 1, value = -890_000.0, date = "1405/07/28", subject = "s1", dept = "d1", desc = "شارژ ساختمان"),
        ),
    )

    @Test
    fun typeFilterKeepsOnlyTheChosenKind() {
        val all = TransactionFilters.apply(db, TransactionFilters.Criteria())
        assertEquals(4, all.size)

        val expenses = TransactionFilters.apply(db, TransactionFilters.Criteria(type = "expense"))
        assertEquals(listOf("t1", "t4"), expenses.map { it.uid })

        val incomes = TransactionFilters.apply(db, TransactionFilters.Criteria(type = "income"))
        assertEquals(listOf("t2"), incomes.map { it.uid })

        val transfers = TransactionFilters.apply(db, TransactionFilters.Criteria(type = "transfer"))
        assertEquals(listOf("t3"), transfers.map { it.uid })
    }

    @Test
    fun accountFilterMatchesSourceOrDestination() {
        val bySource = TransactionFilters.apply(db, TransactionFilters.Criteria(accountUids = setOf("acc_2")))
        assertEquals(listOf("t2", "t3"), bySource.map { it.uid })
    }

    @Test
    fun projectFilterHasAnUnassignedBucket() {
        val projects = TransactionFilters.apply(db, TransactionFilters.Criteria(projectUids = setOf("pr1")))
        assertEquals(listOf("t3"), projects.map { it.uid })

        val unassigned = TransactionFilters.apply(db, TransactionFilters.Criteria(projectUids = setOf("unassigned")))
        assertEquals(listOf("t1", "t2", "t4"), unassigned.map { it.uid })
    }

    @Test
    fun searchLooksAtDescSubjectAccountAndPerson() {
        assertEquals(
            listOf("t1"),
            TransactionFilters.apply(db, TransactionFilters.Criteria(search = "بنزین")).map { it.uid },
        )
        assertEquals(
            listOf("t2"),
            TransactionFilters.apply(db, TransactionFilters.Criteria(search = "رضایی")).map { it.uid },
        )
        assertEquals(
            listOf("t2", "t3"),
            TransactionFilters.apply(db, TransactionFilters.Criteria(search = "حکمت")).map { it.uid },
        )
        assertEquals(
            listOf("t1", "t4"),
            TransactionFilters.apply(db, TransactionFilters.Criteria(search = "قبوض")).map { it.uid },
        )
    }

    @Test
    fun deptFilterKeepsOnlyThatDept() {
        val result = TransactionFilters.apply(db, TransactionFilters.Criteria(deptUid = "d1"))
        assertEquals(listOf("t4"), result.map { it.uid })
    }

    @Test
    fun groupsAreSortedNewestFirstAndCountTheDailyTotals() {
        val groups = TransactionFilters.groupByDay(TransactionFilters.apply(db, TransactionFilters.Criteria()))
        assertEquals(listOf("1405/07/30", "1405/07/29", "1405/07/28"), groups.map { it.date })
        assertEquals(1, groups[0].transactions.size)
        assertEquals(2, groups[2].transactions.size)
        // The 1405/07/28 group: one transfer (-5,000,000) and one expense (-890,000).
        assertEquals(890_000.0, groups[2].totalExpense, 0.0)
        assertEquals(0.0, groups[2].totalIncome, 0.0)
        assertEquals(-890_000.0, groups[2].dailyBalance, 0.0)
    }

    @Test
    fun totalsSumIncomeAndExpensesWithAbsoluteValues() {
        val totals = TransactionFilters.totals(TransactionFilters.apply(db, TransactionFilters.Criteria()))
        assertEquals(45_000_000.0, totals.income, 0.0)
        assertEquals(6_240_000.0, totals.expense, 0.0)
        assertEquals(38_760_000.0, totals.balance, 0.0)
    }

    @Test
    fun projectSummariesCountTransactionsAndSortByBalance() {
        val summaries = TransactionFilters.projectSummaries(db)
        assertEquals(1, summaries.items.size)
        val item = summaries.items[0]
        assertEquals("pr1", item.project.uid)
        assertEquals(1, item.txCount)
        assertEquals(0.0, item.incomes, 0.0)
        assertEquals(0.0, item.expenses, 0.0)
        // A transfer has no income/expense direction, so the project balance stays 0.
        assertEquals(0.0, item.balance, 0.0)
        // Everything without a project counts as unassigned.
        assertEquals(6_240_000.0, summaries.unassignedExpenses, 0.0)
        assertEquals(45_000_000.0, summaries.unassignedIncomes, 0.0)
    }

    @Test
    fun dateBoundsFollowThePreset() {
        val now = JalaliDate.toJalali(1_767_000_000_000L, zone)   // 2026-01-01
        val today = TransactionFilters.bounds(TransactionFilters.Criteria(preset = TransactionFilters.DatePreset.TODAY), 1_767_000_000_000L, zone)
        assertEquals(today.start, today.end)
        assertTrue(today.start.matches(Regex("\\d{4}/\\d{2}/\\d{2}")))

        val last7 = TransactionFilters.bounds(TransactionFilters.Criteria(preset = TransactionFilters.DatePreset.LAST7), 1_767_000_000_000L, zone)
        assertTrue(last7.start < today.start)

        val month = TransactionFilters.bounds(TransactionFilters.Criteria(preset = TransactionFilters.DatePreset.THIS_MONTH), 1_767_000_000_000L, zone)
        assertTrue(month.start.endsWith("/01"))
        assertTrue(month.start.startsWith(month.end.substring(0, 7)))

        val all = TransactionFilters.bounds(TransactionFilters.Criteria())
        assertEquals("", all.start)
        assertEquals("", all.end)
        assertEquals(now.y, 1404)   // 2026-01-01 is still in the Jalali year 1404
    }

    @Test
    fun customRangeFiltersByNormalizedDate() {
        val result = TransactionFilters.apply(
            db,
            TransactionFilters.Criteria(
                preset = TransactionFilters.DatePreset.CUSTOM,
                customStart = "1405/07/29",
                customEnd = "1405/07/30",
            ),
        )
        assertEquals(listOf("t1", "t2"), result.map { it.uid })
    }

    @Test
    fun millionTomanUsesPersianDigitsAndTheRtlSignOrder() {
        assertEquals("۴۵ م.ت", TransactionFilters.millionToman(45_000_000.0, CurrencyUnit.TOMAN))
        assertEquals("۱- م.ت", TransactionFilters.millionToman(-1_400_000.0, CurrencyUnit.TOMAN))
    }
}
