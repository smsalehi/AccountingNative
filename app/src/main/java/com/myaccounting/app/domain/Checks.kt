package com.myaccounting.app.domain

import java.time.Instant
import java.time.ZoneId

/** Port of src/utils/checks.ts — shared check logic (own / person kinds, deadlines, urgency, totals). */
object Checks {
    private const val DAY_MS = 86_400_000L

    enum class Urgency { DONE, OVERDUE, TODAY, SOON, LATER }

    fun startOfToday(nowMs: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

    fun jalaliStrToMs(raw: String?, zone: ZoneId = ZoneId.systemDefault()): Long? {
        val a = LoanSchedule.parseDueDateStr(raw ?: "") ?: return null
        return JalaliDate.jalaliToGregorianMs(a.y, a.m, a.d, zone)
    }

    fun checkDueMs(c: Check, zone: ZoneId = ZoneId.systemDefault()): Long = jalaliStrToMs(c.dueDate, zone) ?: 0L

    /** Effective fund-by deadline: explicit value, otherwise one day before the due date. */
    fun checkFundByMs(c: Check, zone: ZoneId = ZoneId.systemDefault()): Long =
        jalaliStrToMs(c.fundByDate, zone) ?: (checkDueMs(c, zone) - DAY_MS)

    fun checkDeadlineMs(c: Check, zone: ZoneId = ZoneId.systemDefault()): Long =
        if (c.kind == CheckKind.PERSON) checkFundByMs(c, zone) else checkDueMs(c, zone)

    fun buildTxByUidIndex(transactions: List<Transaction>): Map<String, Transaction> {
        val m = HashMap<String, Transaction>()
        for (t in transactions) if (t.uid.isNotEmpty()) m[t.uid] = t
        return m
    }

    /** Funded amount of a person check = Σ live |TotalValue| of the linked deposit transactions (deleted tx → 0). */
    fun checkFundedAmount(c: Check, transactions: List<Transaction>, txIndex: Map<String, Transaction>? = null): Double {
        if (c.kind != CheckKind.PERSON) return 0.0
        val byUid = txIndex ?: buildTxByUidIndex(transactions)
        var sum = 0.0
        for (d in c.deposits) {
            val tx = byUid[d.transactionUid] ?: continue
            val v = Math.abs(tx.totalValue)
            if (v.isFiniteNum()) sum += v
        }
        return sum
    }

    private fun amountOf(c: Check): Double = Math.max(0.0, if (c.amount.isFiniteNum()) c.amount else 0.0)

    fun checkRemaining(c: Check, transactions: List<Transaction>, txIndex: Map<String, Transaction>? = null): Double {
        val amount = amountOf(c)
        if (c.kind == CheckKind.PERSON) return Math.max(0.0, amount - checkFundedAmount(c, transactions, txIndex))
        return if (c.status == CheckStatus.PENDING) amount else 0.0
    }

    fun checkUrgency(
        c: Check, transactions: List<Transaction>, nowMs: Long = System.currentTimeMillis(), soonDays: Int = 3,
        txIndex: Map<String, Transaction>? = null, zone: ZoneId = ZoneId.systemDefault(),
    ): Urgency {
        if (c.status != CheckStatus.PENDING) return Urgency.DONE
        if (c.kind == CheckKind.PERSON && checkRemaining(c, transactions, txIndex) <= 0) return Urgency.LATER
        val today = startOfToday(nowMs, zone)
        val deadline = checkDeadlineMs(c, zone)
        return when {
            deadline < today -> Urgency.OVERDUE
            deadline < today + DAY_MS -> Urgency.TODAY
            deadline <= today + soonDays * DAY_MS -> Urgency.SOON
            else -> Urgency.LATER
        }
    }

    fun countChecksNeedingAttention(db: AccountingDatabase, nowMs: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Int {
        val idx = buildTxByUidIndex(db.transactions)
        return db.checks.count {
            val u = checkUrgency(it, db.transactions, nowMs, 3, idx, zone)
            u == Urgency.OVERDUE || u == Urgency.TODAY || u == Urgency.SOON
        }
    }

    /** Pending own checks per account (for "projected balance"). */
    fun pendingOwnChecksByAccount(db: AccountingDatabase): Map<String, Double> {
        val map = LinkedHashMap<String, Double>()
        for (c in db.checks) {
            val acc = c.accountUid
            if (c.kind != CheckKind.OWN || c.status != CheckStatus.PENDING || acc.isNullOrEmpty()) continue
            map[acc] = (map[acc] ?: 0.0) + amountOf(c)
        }
        return map
    }

    data class DrawerSummary(val count: Int, val remaining: Double, val total: Double)

    fun pendingPersonChecksByDrawer(db: AccountingDatabase): Map<String, DrawerSummary> {
        val map = LinkedHashMap<String, DrawerSummary>()
        val idx = buildTxByUidIndex(db.transactions)
        for (c in db.checks) {
            val drawer = c.drawerPersonUid
            if (c.kind != CheckKind.PERSON || c.status != CheckStatus.PENDING || drawer.isNullOrEmpty()) continue
            val cur = map[drawer] ?: DrawerSummary(0, 0.0, 0.0)
            map[drawer] = DrawerSummary(cur.count + 1, cur.remaining + checkRemaining(c, db.transactions, idx), cur.total + amountOf(c))
        }
        return map
    }

    data class PendingTotals(val ownTotal: Double, val personRemaining: Double)

    fun pendingChecksTotals(db: AccountingDatabase): PendingTotals {
        val idx = buildTxByUidIndex(db.transactions)
        var own = 0.0
        var person = 0.0
        for (c in db.checks) {
            if (c.status != CheckStatus.PENDING) continue
            if (c.kind == CheckKind.OWN) own += amountOf(c) else person += checkRemaining(c, db.transactions, idx)
        }
        return PendingTotals(own, person)
    }
}
