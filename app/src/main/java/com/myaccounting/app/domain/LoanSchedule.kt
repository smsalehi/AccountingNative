package com.myaccounting.app.domain

import java.time.ZoneId

/** Port of src/utils/loanSchedule.ts (shared by the loans screen and notifications). */
object LoanSchedule {
    private const val DAY_MS = 86_400_000L

    data class DueAnchor(val y: Int, val m: Int, val d: Int)

    /** Total installments: valid manual value, else ceil(Value / PeriodValue), else null. */
    fun resolveTotalInstallments(dept: Dept): Int? {
        val manual = dept.totalInstallments ?: 0
        if (manual > 0) return manual
        val total = dept.value.takeIf { it.isFiniteNum() } ?: 0.0
        val period = dept.periodValue.takeIf { it.isFiniteNum() } ?: 0.0
        if (total > 0 && period > 0) return Math.ceil(total / period).toInt()
        return null
    }

    /** Σ|TotalValue| of type-1 transactions linked to each loan. */
    fun buildPaidIndex(transactions: List<Transaction>): Map<String, Double> {
        val m = HashMap<String, Double>()
        for (t in transactions) {
            val dept = t.deptUid
            if (dept.isNullOrEmpty() || t.transactionType != 1) continue
            if (t.totalValue.isFiniteNum()) m[dept] = (m[dept] ?: 0.0) + Math.abs(t.totalValue)
        }
        return m
    }

    fun totalPaidSoFar(dept: Dept, paid: Map<String, Double>): Double = paid[dept.uid] ?: 0.0

    fun paidInstallmentsCount(dept: Dept, paid: Map<String, Double>): Int {
        val period = if (dept.periodValue.isFiniteNum()) dept.periodValue else 0.0
        if (period <= 0) return 0
        return Math.floor(totalPaidSoFar(dept, paid) / period).toInt()
    }

    fun remainingInstallmentsCount(dept: Dept, paid: Map<String, Double>): Int {
        val paidCount = paidInstallmentsCount(dept, paid)
        val total = resolveTotalInstallments(dept)
        if (total != null) return Math.max(0, total - paidCount)
        val period = if (dept.periodValue.isFiniteNum()) dept.periodValue else 0.0
        val remaining = Math.max(0.0, dept.value - totalPaidSoFar(dept, paid))
        if (period <= 0) return 0
        return Math.ceil(remaining / period).toInt()
    }

    private fun validDueDay(dept: Dept): Int? {
        val d = dept.dueDay ?: 0
        return if (d in 1..31) d else null
    }

    /** Parse a Jalali "1403/12/25" (Persian digits and stray spaces tolerated). */
    fun parseDueDateStr(raw: String?): DueAnchor? {
        if (raw == null) return null
        val parts = raw.trim().split('/')
        if (parts.size != 3) return null
        val nums = parts.map { jsNumber(JalaliDate.toEnglishDigits(it.trim())) }
        if (nums.any { !it.isFiniteNum() || it != Math.rint(it) }) return null
        val y = nums[0].toInt(); val m = nums[1].toInt(); val d = nums[2].toInt()
        if (y < 1300 || y > 1500 || m < 1 || m > 12 || d < 1) return null
        if (d > JalaliDate.getJalaliMonthLength(y, m)) return null
        return DueAnchor(y, m, d)
    }

    /** 1) valid FirstDueDate; 2) legacy DueDay → first such day on/after the start date; 3) null = rolling. */
    fun resolveAnchor(dept: Dept, nowMs: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): DueAnchor? {
        parseDueDateStr(dept.firstDueDate)?.let { return it }
        val dueDay = validDueDay(dept) ?: return null
        val startMs = if (dept.paymentStartDate != 0L) dept.paymentStartDate else nowMs
        val j = JalaliDate.toJalali(startMs, zone)
        val shift = if (dueDay < j.d) 1 else 0
        val (year, month) = JalaliDate.addJalaliMonths(j.y, j.m, shift)
        return DueAnchor(year, month, dueDay)
    }

    private fun rollingDueMs(dept: Dept, paidCount: Int): Long {
        val days = if (dept.periodDays.isFiniteNum() && dept.periodDays != 0.0) dept.periodDays else 30.0
        return (dept.paymentStartDate + paidCount * days * DAY_MS).toLong()
    }

    fun anchoredDueMs(anchor: DueAnchor, paidCount: Int, zone: ZoneId = ZoneId.systemDefault()): Long {
        val (year, month) = JalaliDate.addJalaliMonths(anchor.y, anchor.m, Math.max(0, paidCount))
        val day = Math.min(anchor.d, JalaliDate.getJalaliMonthLength(year, month))
        return JalaliDate.jalaliToGregorianMs(year, month, day, zone)
    }

    fun nextDueDateMs(dept: Dept, paid: Map<String, Double>, nowMs: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Long {
        val count = paidInstallmentsCount(dept, paid)
        val anchor = resolveAnchor(dept, nowMs, zone) ?: return rollingDueMs(dept, count)
        return anchoredDueMs(anchor, count, zone)
    }

    fun isLoanSettled(dept: Dept, paid: Map<String, Double>): Boolean =
        Math.max(0.0, (if (dept.value.isFiniteNum()) dept.value else 0.0) - totalPaidSoFar(dept, paid)) <= 0

    fun isLoanOverdue(dept: Dept, paid: Map<String, Double>, nowMs: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Boolean {
        if (isLoanSettled(dept, paid)) return false
        return nextDueDateMs(dept, paid, nowMs, zone) < nowMs
    }

    fun countVisibleOverdueLoans(db: AccountingDatabase, paid: Map<String, Double>? = null, nowMs: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Int {
        val idx = paid ?: buildPaidIndex(db.transactions)
        return db.depts.count { it.isHidden != 1 && isLoanOverdue(it, idx, nowMs, zone) }
    }
}
