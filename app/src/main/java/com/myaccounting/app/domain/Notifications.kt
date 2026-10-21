package com.myaccounting.app.domain

import java.time.ZoneId

/** Count shown on the bell badge (port of the notifications memo in NotificationDropdown.tsx). */
object Notifications {
    private const val DAY_MS = 86_400_000.0

    /** Loans with a remaining balance whose next installment is overdue or due within 2 days, minus today's dismissed ones. */
    fun urgentCount(
        db: AccountingDatabase,
        dismissed: Set<String> = emptySet(),
        nowMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Int {
        val paid = LoanSchedule.buildPaidIndex(db.transactions)
        return db.depts.count { d ->
            if (d.value <= 0) return@count false
            val total = Math.max(0.0, if (d.value.isFiniteNum()) d.value else 0.0)
            if (Math.max(0.0, total - LoanSchedule.totalPaidSoFar(d, paid)) <= 0) return@count false
            val daysLeft = Math.ceil((LoanSchedule.nextDueDateMs(d, paid, nowMs, zone) - nowMs) / DAY_MS)
            daysLeft <= 2 && "notif_${d.uid}" !in dismissed
        }
    }
}
