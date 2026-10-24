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

    // --- یادآوری چک‌ها (checkNotifications.ts) ---------------------------------------------------

    data class CheckReminderPrefs(val enabled: Boolean = true, val hour: Int = DEFAULT_HOUR) {
        companion object { const val DEFAULT_HOUR = 9 }
    }

    data class PlannedReminder(val id: Int, val title: String, val body: String, val atMs: Long, val checkUid: String)

    private val OFFSETS_DAYS = intArrayOf(3, 1, 0)

    /** شناسه عددی ۳۲بیتیِ پایدار از یک کلید متنی (FNV-1a، همان web). */
    fun reminderNotificationId(key: String): Int {
        // FNV-1a offset basis 2166136261 = 0x811C9DC5 (fits Int as a negative value)
        var h = 0x811C9DC5.toInt()
        for (ch in key) {
            h = h xor ch.code
            h *= 16777619
        }
        return ((h.toLong() and 0xFFFFFFFFL).toInt() % 2000000000) + 1
    }

    private fun whenLabel(daysBefore: Int): String = when (daysBefore) {
        0 -> "امروز"
        1 -> "فردا"
        else -> "${JalaliDate.toPersianDigits(daysBefore.toString())} روز دیگر"
    }

    /**
     * برنامه یادآوری‌ها (تابع خالص): برای هر چک در انتظار ۳ روز قبل، ۱ روز قبل و روز مهلت،
     * ساعت مشخص‌شده. مهلت: own = سررسید، person = مهلت واریز (تا وقتی مانده واریز > ۰).
     */
    fun planCheckNotifications(
        checks: List<Check>,
        transactions: List<Transaction>,
        persons: List<Person>,
        prefs: CheckReminderPrefs,
        unit: CurrencyUnit,
        nowMs: Long = System.currentTimeMillis(),
    ): List<PlannedReminder> {
        if (!prefs.enabled) return emptyList()
        val txIndex = Checks.buildTxByUidIndex(transactions)
        val personIndex = persons.associate { it.uid to it.fullName }
        val out = ArrayList<PlannedReminder>()
        for (c in checks) {
            if (c.status != CheckStatus.PENDING) continue
            val remaining = Checks.checkRemaining(c, transactions, txIndex)
            if (remaining <= 0) continue
            val deadline = Checks.checkDeadlineMs(c)
            if (deadline <= 0L) continue
            val label = if (c.kind == CheckKind.PERSON) {
                "چک " + (c.drawerPersonUid?.let(personIndex::get)?.ifEmpty { null } ?: "شخص")
            } else {
                "چک" +
                    (c.checkNumber?.takeIf { it.isNotEmpty() }?.let { " شماره " + JalaliDate.toPersianDigits(it) } ?: "") +
                    (c.payeePersonUid?.let(personIndex::get)?.takeIf { it.isNotEmpty() }?.let { " ($it)" } ?: "")
            }
            val amountText = Currency.formatCurrency(remaining, unit)
            for (days in OFFSETS_DAYS) {
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = deadline
                cal.add(java.util.Calendar.DAY_OF_MONTH, -days)
                cal.set(java.util.Calendar.HOUR_OF_DAY, prefs.hour)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val at = cal.timeInMillis
                if (at <= nowMs + 5000) continue
                val title = if (c.kind == CheckKind.PERSON) "مهلت واریز چک" else "سررسید چک"
                val body = if (c.kind == CheckKind.PERSON) {
                    "$label: ${whenLabel(days)} آخرین مهلت واریز است — مانده $amountText"
                } else {
                    "$label: ${whenLabel(days)} سررسید است — $amountText"
                }
                out.add(PlannedReminder(reminderNotificationId("${c.uid}:$days"), title, body, at, c.uid))
            }
        }
        return out
    }
}
