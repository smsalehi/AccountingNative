package com.myaccounting.app.domain

import java.time.LocalDate
import java.time.ZoneId

/**
 * Parity harness: prints the same values that tools/golden/gold.ts prints from the ORIGINAL TypeScript utilities.
 * Run both with TZ=Asia/Tehran and diff the output (see tools/golden/README.md).
 */
private val TZ: ZoneId = ZoneId.of("Asia/Tehran")
private val lines = ArrayList<String>()
private fun out(k: String, v: Any?) { lines.add("$k=${Json.stringify(v)}") }
private fun r6(x: Double) = Currency.jsRound(x * 1e6) / 1e6
private fun obj(vararg p: Pair<String, Any?>): Map<String, Any?> = linkedMapOf(*p)
private fun anchor(a: LoanSchedule.DueAnchor?): Any? = a?.let { obj("y" to it.y, "m" to it.m, "d" to it.d) }
private fun rateRows(l: List<ExchangeRate>) = l.map { obj("Date" to it.date, "Rate" to it.rate) }

fun main() {
    println(goldenLines().joinToString("\n"))
}

fun goldenLines(): List<String> {
    lines.clear()
    // 1. Jalali
    val acc = ArrayList<String>()
    var d = LocalDate.of(2015, 3, 1)
    val end = LocalDate.of(2035, 3, 31)
    while (!d.isAfter(end)) {
        val j = JalaliDate.gregorianToJalali(d.year, d.monthValue, d.dayOfMonth)
        val g = JalaliDate.jalaliToGregorian(j.y, j.m, j.d)
        acc.add("${j.y}/${j.m}/${j.d}>${g.y}-${g.m}-${g.d}")
        d = d.plusDays(3)
    }
    out("jalali_roundtrip", acc)
    out("month_lengths", (1380..1480).map { y -> (1..12).joinToString("") { m -> JalaliDate.getJalaliMonthLength(y, m).toString() } })
    out("add_months", listOf(listOf(1403, 12, 1), listOf(1403, 1, -1), listOf(1400, 6, 25), listOf(1399, 12, 13), listOf(1401, 3, 0)).map { (y, m, n) ->
        val (yy, mm) = JalaliDate.addJalaliMonths(y, m, n); obj("year" to yy, "month" to mm)
    })
    val stamps = listOf(1700000000000L, 1735689600000L, 1760000000000L, 1577836800000L, 1711929600000L)
    out("format_dates", stamps.map { listOf(JalaliDate.formatJalaliDate(it, TZ), JalaliDate.getFullJalaliDate(it, TZ), JalaliDate.getShortJalaliDate(it, TZ)) })
    out("digits", listOf(JalaliDate.toPersianDigits("abc 123 4.5"), JalaliDate.toEnglishDigits("۱۴۰۳/۰۶/۱۵ ٣"), JalaliDate.normalizePersianDate("۱۴۰۳/۶/۵"), JalaliDate.normalizePersianDate("03-6-5"), JalaliDate.normalizePersianDate("")))

    // 2. currency
    val amts = listOf(0.0, 1.0, -1.0, 999.0, 1234567.0, -1234567.5, 0.5, 2.5, 123456789012.0, 1234.567, -0.004, 99999.995, Double.NaN)
    out("currency", amts.flatMap { a -> CurrencyUnit.values().flatMap { u -> listOf(Currency.formatCurrency(a, u, true), Currency.formatCurrency(a, u, false)) } })

    // 3. exchange rates
    val rates = listOf(
        ExchangeRate("2024-01-01", 50000.0), ExchangeRate("2024-01-03", 52000.0), ExchangeRate("2024-01-10", 60000.0),
        ExchangeRate("2024-02-29", 61000.0), ExchangeRate("2023-02-29", 1.0), ExchangeRate("bad", 5.0), ExchangeRate("2024-03-01", -4.0),
    )
    val idx = ExchangeRates.buildRateIndex(rates)!!
    out("rate_index", listOf(idx.days.toList(), idx.rates.toList(), idx.count, idx.firstDate, idx.lastDate, idx.lastRate))
    val d0 = ExchangeRates.dateKeyToDay("2024-01-01")!!
    out("rate_at_day", listOf(-3, 0, 1, 2, 3, 4, 8, 9, 10, 11, 58, 59, 60, 200).map { r6(ExchangeRates.rateAtDay(idx, d0 + it)) })
    out("day_key", listOf("2024-02-29", "2023-02-29", "0050-01-01", "2024-13-01", "1970-01-02").map { ExchangeRates.dateKeyToDay(it) })
    out("rate_at_ms", listOf(1704067200000L, 1704150000000L, 1704153600000L + 3600000L, 1706000000000L, 1999999999999L).map { listOf(ExchangeRates.msToDay(it, TZ), r6(ExchangeRates.rateAtMs(idx, it, TZ))) })
    val fileA = "<Ticker>,<Per>,<DTYYYYMMDD>,<TIME>,<Open>,<High>,<Low>,<Close>,<Vol>\nDOLLAR,D,20240101,000000,500000,510000,490000,505000,1\nDOLLAR,D,20240103,000000,0,0,0,\"520,000\",1\nDOLLAR,D,20240104,000000,0,0,0,abc,1\nDOLLAR,D,20241399,000000,0,0,0,1,1\n"
    val fileB = "date;rate\n۱۴۰۲/۰۱/۰۱;5\n2024-1-5;71000\n2024/01/06;-1\n"
    val fileC = "20240101,500000\n20240102,510000\n"
    for ((n, f, u) in listOf(Triple("A", fileA, ExchangeRates.RateFileUnit.RIAL), Triple("B", fileB, ExchangeRates.RateFileUnit.TOMAN), Triple("C", fileC, ExchangeRates.RateFileUnit.RIAL))) {
        val p = ExchangeRates.parseRateFile(f, u)
        out("parse_$n", listOf(rateRows(p.rows), p.totalLines, p.invalid, p.columns))
    }
    val mg = ExchangeRates.mergeRates(listOf(ExchangeRate("2024-01-01", 50000.0), ExchangeRate("2024-01-05", 70000.0)), listOf(ExchangeRate("2024-01-01", 50500.0), ExchangeRate("2024-01-02", 1.0), ExchangeRate("2024-01-05", 70000.0)), true)
    out("merge_over", listOf(rateRows(mg.merged), mg.added, mg.updated, mg.skippedExisting, mg.firstNewDate, mg.lastNewDate))
    val mg2 = ExchangeRates.mergeRates(listOf(ExchangeRate("2024-01-01", 50000.0)), listOf(ExchangeRate("2024-01-01", 50500.0), ExchangeRate("2024-01-09", 3.0)), false)
    out("merge_keep", listOf(rateRows(mg2.merged), mg2.added, mg2.updated, mg2.skippedExisting, mg2.firstNewDate, mg2.lastNewDate))

    // 4. balances
    fun tx(uid: String, type: Int, total: Double, dt: Long, src: String? = null, dst: String? = null, dept: String? = null) =
        Transaction(uid = uid, transactionType = type, totalValue = total, dateTime = dt, sourceAccountUid = src, destinationAccountUid = dst, deptUid = dept)
    val db = AccountingDatabase(
        accounts = listOf(Account("a1", "A1", "g1", 1000.0), Account("a2", "A2", "g1", 500.0), Account("a3", "A3", "g1", 0.0)),
        persons = listOf(Person("p1", "P", "", oldBalance = 700.0)),
        transactions = listOf(
            tx("t1", 1, 100.0, 1704110000000), tx("t2", 2, 250.0, 1704200000000), tx("t3", 3, 0.0, 1704300000000),
            tx("t4", 1, -40.0, 1704400000000, src = "a2"), tx("t5", 3, 70.0, 1704500000000, src = "a1", dst = "a3"),
            tx("t6", 0, 30.0, 0, src = "a3"), tx("t7", 9, 30.0, 1, src = "a3"), tx("t8", 1, 12.0, 1704600000000, src = "zzz"),
            tx("t9", 1, 60.0, 1704700000000, dept = "d1"), tx("t10", 1, 60.0, 1704800000000, dept = "d1"), tx("t11", 1, 5.0, 1704900000000, dept = "d2"),
        ),
        transactionDetails = listOf(
            TransactionDetail("x1", "t1", "a1", 100.0, 0), TransactionDetail("x2", "t2", "a2", 250.0, 0), TransactionDetail("x3", "t3", "a1", 300.0, 0),
            TransactionDetail("x4", "t3", "a3", 300.0, 1), TransactionDetail("x5", "t3", "nope", 300.0, 1), TransactionDetail("x6", "t1", "a2", 0.0, 0),
        ),
    )
    out("balances", Balance.computeAccountBalances(db).entries.map { listOf(it.key, it.value) })
    val usd = ExchangeRates.buildUsdViewDb(db, idx, TZ)
    out("usd_balances", Balance.computeAccountBalances(usd).entries.map { listOf(it.key, r6(it.value)) })
    out("usd_tx", usd.transactions.map { r6(it.totalValue) })
    out("usd_persons", usd.persons.map { r6(it.oldBalance) })

    // 5. loans
    val base = 1705305600000L // Date.UTC(2024, 0, 15, 8, 0, 0) → verified below
    val depts = listOf(
        Dept("d1", "L1", 600.0, base, 30.0, 100.0, firstDueDate = "1402/10/25"),
        Dept("d2", "L2", 1000.0, base, 30.0, 100.0, dueDay = 5),
        Dept("d3", "L3", 1000.0, base, 15.0, 100.0, totalInstallments = 12),
        Dept("d4", "L4", 1000.0, base, 0.0, 0.0),
        Dept("d5", "L5", 100.0, base, 30.0, 100.0, firstDueDate = "۱۴۰۲/۱۲/۳۰"),
        Dept("d6", "L6", 100.0, base, 30.0, 100.0, firstDueDate = "1403/12/30"),
        Dept("d7", "L7", 100.0, base, 30.0, 100.0, dueDay = 31),
    )
    val paid = LoanSchedule.buildPaidIndex(db.transactions)
    out("paid_index", paid.entries.map { listOf(it.key, it.value) })
    out("loans", depts.map { listOf(it.uid, LoanSchedule.resolveTotalInstallments(it), LoanSchedule.paidInstallmentsCount(it, paid), LoanSchedule.remainingInstallmentsCount(it, paid), anchor(LoanSchedule.resolveAnchor(it, zone = TZ)), LoanSchedule.nextDueDateMs(it, paid, zone = TZ), LoanSchedule.isLoanSettled(it, paid)) })
    out("due_parse", listOf("1403/12/25", " ۱۴۰۳/۱/۳۱ ", "1403/7/31", "1299/1/1", "1403/13/1", "1403/12/30", "1404/12/30", "abc", "1403/12", "1403//5").map { anchor(LoanSchedule.parseDueDateStr(it)) })

    // 6. checks
    val now = 1718011800000L // Date.UTC(2024, 5, 10, 9, 30, 0)
    val day = 86400000L
    fun jd(ms: Long) = JalaliDate.formatJalaliDate(ms, TZ)
    val checkTxs = listOf(tx("c1", 1, -300.0, 0), tx("c2", 1, 200.0, 0))
    fun dep(uid: String, txUid: String) = CheckDeposit(uid, "a1", 1.0, 1, txUid)
    val checks = listOf(
        Check("k1", CheckKind.OWN, 1000.0, jd(now + day * 5), accountUid = "a1", createdAt = 1),
        Check("k2", CheckKind.OWN, 500.0, jd(now - day * 2), accountUid = "a1", createdAt = 1),
        Check("k3", CheckKind.OWN, 700.0, jd(now), accountUid = "a2", createdAt = 1),
        Check("k4", CheckKind.OWN, 700.0, jd(now + day * 2), accountUid = "a2", status = CheckStatus.PASSED, createdAt = 1),
        Check("k5", CheckKind.PERSON, 1000.0, jd(now + day * 4), drawerPersonUid = "p1", createdAt = 1, deposits = listOf(dep("dd1", "c1"), dep("dd2", "gone"))),
        Check("k6", CheckKind.PERSON, 500.0, jd(now + day * 30), fundByDate = jd(now + day * 3), drawerPersonUid = "p1", createdAt = 1, deposits = listOf(dep("dd3", "c2"))),
        Check("k7", CheckKind.PERSON, 200.0, jd(now + day * 1), drawerPersonUid = "p2", createdAt = 1, deposits = listOf(dep("dd4", "c2"))),
        Check("k8", CheckKind.OWN, 5.0, "garbage", accountUid = "a3", createdAt = 1),
    )
    out("check_urgency", checks.map { listOf(it.uid, Checks.checkUrgency(it, checkTxs, now, zone = TZ).name.lowercase(), r6(Checks.checkRemaining(it, checkTxs)), r6(Checks.checkFundedAmount(it, checkTxs)), Checks.checkDeadlineMs(it, TZ)) })
    val cdb = db.copy(checks = checks, transactions = checkTxs)
    val tot = Checks.pendingChecksTotals(cdb)
    out("check_totals", obj("ownTotal" to tot.ownTotal, "personRemaining" to tot.personRemaining))
    out("check_own_by_acc", Checks.pendingOwnChecksByAccount(cdb).entries.map { listOf(it.key, it.value) })
    out("check_by_drawer", Checks.pendingPersonChecksByDrawer(cdb).entries.map { listOf(it.key, obj("count" to it.value.count, "remaining" to it.value.remaining, "total" to it.value.total)) })

    // 7. normalize
    val messy = """{"version":1,
      "Accounts":[{"UID":"a1","Title":"x","OldBalance":"1500","GroupUID":"gone"},{"UID":"a2","OldBalance":"abc"}],
      "Subjects":[{"UID":"s1","Title":"S","IsCost":1,"ParentUID":null},null],
      "Transactions":[
        {"UID":"t1","TransactionType":"1","TotalValue":"100","DateTime":"1704110000000","PersianDate":14030101},
        {"UID":"t2","TransactionType":3,"TotalValue":0,"DateTime":5},
        {"UID":"t3","TransactionType":2,"TotalValue":0,"DateTime":6},
        {"UID":"t4","TransactionType":77,"TotalValue":"x"}, 7],
      "TransactionDetails":[
        {"UID":"d1","TransactionUID":"t1","AccountUID":"a1","Value":100,"SubType":0},
        {"UID":"d2","TransactionUID":"t2","AccountUID":"a1","Value":900,"SubType":0},
        {"UID":"d3","TransactionUID":"t2","AccountUID":"a2","Value":800,"SubType":1},
        {"UID":"d4","TransactionUID":"t3","AccountUID":"a2","Value":40,"SubType":0},
        {"UID":"d5","TransactionUID":"t3","AccountUID":"a1","Value":-60,"SubType":0}],
      "Checks":[{"UID":"k1","Kind":"own","Amount":"10","DueDate":"1403/01/01","Status":"pending"},{"UID":"k2","Amount":"x"},{"Kind":"own","Amount":1}],
      "ExchangeRates":[{"Date":"2024-01-01","Rate":5},{"Date":"2024-02-30","Rate":5},{"Date":"2024-01-02","Rate":0},{"Date":"2024-01-03","Rate":"7"}]}"""
    val norm = DatabaseCodec.fromJson(messy)
    val nd = norm.db
    out("norm_counts", listOf(nd.accounts.size, nd.transactions.size, nd.transactionDetails.size, nd.checks.size, nd.exchangeRates.size, nd.accountGroups.size, nd.accountGroups.firstOrNull()?.uid))
    out("norm_tx", nd.transactions.map { listOf(it.uid, it.transactionType, it.totalValue, it.dateTime, it.persianDate, it.sourceAccountUid, it.destinationAccountUid) })
    out("norm_acc", nd.accounts.map { listOf(it.uid, it.oldBalance, it.groupUid) })
    out("norm_bal", Balance.computeAccountBalances(nd).entries.map { listOf(it.key, it.value) })
    out("norm_checks_rates", listOf(nd.checks.map { it.uid }, rateRows(nd.exchangeRates)))
    out("norm_repaired_count", norm.repaired.size)

    // 8. round-trip: toJson → fromJson must be lossless for the modelled fields
    val again = DatabaseCodec.fromJson(DatabaseCodec.toJson(nd)).db
    out("roundtrip_equal", listOf(again.copy(exportDate = "") == nd.copy(exportDate = "")))

    return lines.toList()
}
