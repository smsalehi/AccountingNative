package com.myaccounting.app.domain

import java.time.ZoneId

/** Port of the notifications memo in NotificationDropdown.tsx (the bell badge + its popover). */
object Notifications {
    private const val DAY_MS = 86_400_000.0

    data class Item(
        val id: String,
        val deptUid: String,
        val title: String,
        val amount: Double,
        /** fa-IR date, e.g. «۱۴۰۳/۶/۱۵» */
        val dueDateStr: String,
        /** negative = overdue, positive = days left */
        val daysDiff: Double,
        val isOverdue: Boolean,
        val desc: String,
    )

    /** Loans with a remaining balance whose next installment is overdue or due within 2 days, minus today's dismissed ones. */
    fun items(
        db: AccountingDatabase,
        dismissed: Set<String> = emptySet(),
        nowMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Item> {
        val paid = LoanSchedule.buildPaidIndex(db.transactions)
        return db.depts.mapNotNull { d ->
            if (d.value <= 0) return@mapNotNull null
            val total = Math.max(0.0, if (d.value.isFiniteNum()) d.value else 0.0)
            if (Math.max(0.0, total - LoanSchedule.totalPaidSoFar(d, paid)) <= 0) return@mapNotNull null
            val due = LoanSchedule.nextDueDateMs(d, paid, nowMs, zone)
            val daysLeft = Math.ceil((due - nowMs) / DAY_MS)
            val id = "notif_${d.uid}"
            if (daysLeft > 2 || id in dismissed) return@mapNotNull null
            val j = JalaliDate.toJalali(due, zone)
            Item(
                id = id,
                deptUid = d.uid,
                title = d.title,
                amount = d.periodValue,
                dueDateStr = JalaliDate.toPersianDigits("${j.y}/${j.m}/${j.d}"),
                daysDiff = daysLeft,
                isOverdue = daysLeft <= 0,
                desc = d.desc.ifBlank { "قسط ماهانه تسهیلات" },
            )
        }.sortedBy { it.daysDiff }
    }

    /** Count shown on the bell badge. */
    fun urgentCount(
        db: AccountingDatabase,
        dismissed: Set<String> = emptySet(),
        nowMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Int = items(db, dismissed, nowMs, zone).size
}
