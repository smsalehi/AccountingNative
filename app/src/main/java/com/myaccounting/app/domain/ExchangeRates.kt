package com.myaccounting.app.domain

import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Port of src/utils/exchangeRates.ts — daily USD rate (toman per 1 USD).
 * Rules: only real file days are stored; one empty day between two known days = their average (linear interpolation
 * in general); after the last known day the last rate is used; before the first known day the first rate is used.
 */
object ExchangeRates {
    private val DATE_KEY = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$")

    /** 'YYYY-MM-DD' → epoch day; null for invalid dates (e.g. Feb 29 of a non-leap year). */
    fun dateKeyToDay(key: String): Long? {
        val m = DATE_KEY.matchEntire(key) ?: return null
        val y = m.groupValues[1].toInt(); val mo = m.groupValues[2].toInt(); val d = m.groupValues[3].toInt()
        if (y < 100) return null // JS Date.UTC maps years 0..99 to 1900+, so the original rejects them
        return try { LocalDate.of(y, mo, d).toEpochDay() } catch (e: DateTimeException) { null }
    }

    fun dayToDateKey(day: Long): String = LocalDate.ofEpochDay(day).toString()

    /** Transaction timestamp → day number based on the LOCAL date (what the user sees). */
    fun msToDay(ms: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(ms).atZone(zone).toLocalDate().toEpochDay()

    class RateIndex(
        val days: LongArray, val rates: DoubleArray, val count: Int,
        val firstDate: String, val lastDate: String, val lastRate: Double,
    )

    fun buildRateIndex(list: List<ExchangeRate>?): RateIndex? {
        if (list.isNullOrEmpty()) return null
        val byDay = java.util.TreeMap<Long, Double>()
        for (r in list) {
            val day = dateKeyToDay(r.date) ?: continue
            if (!r.rate.isFiniteNum() || r.rate <= 0) continue
            byDay[day] = r.rate
        }
        if (byDay.isEmpty()) return null
        val days = byDay.keys.toLongArray()
        val rates = DoubleArray(days.size) { byDay[days[it]]!! }
        return RateIndex(days, rates, days.size, dayToDateKey(days.first()), dayToDateKey(days.last()), rates.last())
    }

    fun rateAtDay(idx: RateIndex, day: Long): Double {
        val days = idx.days; val rates = idx.rates; val n = days.size
        if (day <= days[0]) return rates[0]
        if (day >= days[n - 1]) return rates[n - 1]
        var lo = 0; var hi = n - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) shr 1
            if (days[mid] <= day) lo = mid else hi = mid - 1
        }
        if (days[lo] == day) return rates[lo]
        val d0 = days[lo]; val d1 = days[lo + 1]
        return rates[lo] + ((rates[lo + 1] - rates[lo]) * (day - d0)) / (d1 - d0)
    }

    fun rateAtMs(idx: RateIndex, ms: Long, zone: ZoneId = ZoneId.systemDefault()): Double = rateAtDay(idx, msToDay(ms, zone))

    // ---- file parsing --------------------------------------------------------------

    enum class RateFileUnit { RIAL, TOMAN }

    class ParsedRateFile(val rows: List<ExchangeRate>, val totalLines: Int, val invalid: Int, val columns: String)

    private fun normalizeDigits(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) sb.append(
            when (c) {
                in '۰'..'۹' -> '0' + (c - '۰')
                in '٠'..'٩' -> '0' + (c - '٠')
                else -> c
            },
        )
        return sb.toString()
    }

    private val COMPACT = Regex("^(\\d{4})(\\d{2})(\\d{2})$")
    private val SEP = Regex("^(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})$")

    private fun parseDateCell(raw: String): String? {
        val s = normalizeDigits(raw.trim().replace(Regex("^\"|\"$"), ""))
        val y: String; val m: String; val d: String
        val c = COMPACT.matchEntire(s)
        val sp = SEP.matchEntire(s)
        when {
            c != null -> { y = c.groupValues[1]; m = c.groupValues[2]; d = c.groupValues[3] }
            sp != null -> { y = sp.groupValues[1]; m = sp.groupValues[2]; d = sp.groupValues[3] }
            else -> return null
        }
        val key = "$y-${m.padStart(2, '0')}-${d.padStart(2, '0')}"
        return if (dateKeyToDay(key) == null) null else key
    }

    private fun normHeader(h: String): String = h.replace(Regex("[<>\"\\s]"), "").lowercase()

    /** MetaStock/TSE style (Dollar_D.txt). Output rate is always toman (rial files are divided by 10). */
    fun parseRateFile(text: String, fileUnit: RateFileUnit): ParsedRateFile {
        val lines = text.removePrefix("\uFEFF").split(Regex("\\r?\\n")).map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return ParsedRateFile(emptyList(), 0, 0, "")

        val delim = when {
            lines[0].contains(',') -> ','
            lines[0].contains(';') -> ';'
            lines[0].contains('\t') -> '\t'
            else -> ','
        }
        var dateIdx: Int; var rateIdx: Int; var start = 0
        var columns: String
        val headerCells = lines[0].split(delim).map(::normHeader)
        val dIdx = headerCells.indexOfFirst { it == "dtyyyymmdd" || it == "date" || it == "تاریخ" }
        val rIdx = headerCells.indexOfFirst { it in setOf("close", "last", "price", "rate", "نرخ", "قیمت") }
        if (dIdx >= 0 && rIdx >= 0) {
            dateIdx = dIdx; rateIdx = rIdx; start = 1
            columns = "${headerCells[dIdx]} / ${headerCells[rIdx]}"
        } else {
            val first = lines[0].split(delim)
            if (first.size >= 8) { dateIdx = 2; rateIdx = 7 } else { dateIdx = 0; rateIdx = 1 }
            columns = "ستون ${dateIdx + 1} / ستون ${rateIdx + 1}"
            if (parseDateCell(first.getOrElse(dateIdx) { "" }) == null) start = 1
        }

        var totalLines = 0; var invalid = 0
        val byDate = HashMap<String, Double>()
        for (i in start until lines.size) {
            totalLines++
            val cells = lines[i].split(delim)
            val key = parseDateCell(cells.getOrElse(dateIdx) { "" })
            val raw = jsNumber(normalizeDigits(cells.getOrElse(rateIdx) { "" }.replace(Regex("[\",\\s]"), "")))
            if (key == null || !raw.isFiniteNum() || raw <= 0) { invalid++; continue }
            byDate[key] = if (fileUnit == RateFileUnit.RIAL) raw / 10 else raw
        }
        val rows = byDate.entries.sortedBy { it.key }.map { ExchangeRate(it.key, it.value) }
        return ParsedRateFile(rows, totalLines, invalid, columns)
    }

    // ---- merge ---------------------------------------------------------------------

    class MergeResult(
        val merged: List<ExchangeRate>, val added: Int, val updated: Int, val skippedExisting: Int,
        val firstNewDate: String?, val lastNewDate: String?,
    )

    /** Only new days are added; with overwrite=true differing existing days are replaced too. */
    fun mergeRates(existing: List<ExchangeRate>, incoming: List<ExchangeRate>, overwrite: Boolean): MergeResult {
        val map = HashMap<String, Double>()
        for (r in existing) if (r.rate.isFiniteNum()) map[r.date] = r.rate
        var added = 0; var updated = 0; var skipped = 0
        var first: String? = null; var last: String? = null
        for (r in incoming) {
            val cur = map[r.date]
            if (cur == null) {
                map[r.date] = r.rate; added++
                if (first == null || r.date < first) first = r.date
                if (last == null || r.date > last) last = r.date
            } else if (overwrite && Math.abs(cur - r.rate) > 1e-9) {
                map[r.date] = r.rate; updated++
            } else skipped++
        }
        val merged = map.entries.sortedBy { it.key }.map { ExchangeRate(it.key, it.value) }
        return MergeResult(merged, added, updated, skipped, first, last)
    }

    // ---- display-only USD view -----------------------------------------------------

    private fun orZero(v: Double): Double = if (v.isFiniteNum()) v else 0.0

    /**
     * Display-only copy of the database where all amounts are dollars: each transaction (and its legs) uses the rate of ITS
     * day; opening balances use the rate of the first transaction's day (or the last rate if none). Loans (Depts) are untouched.
     * Never persist this view.
     */
    fun buildUsdViewDb(db: AccountingDatabase, idx: RateIndex, zone: ZoneId = ZoneId.systemDefault()): AccountingDatabase {
        var minMs = Long.MAX_VALUE
        for (t in db.transactions) if (t.dateTime > 0 && t.dateTime < minMs) minMs = t.dateTime
        val opening = if (minMs != Long.MAX_VALUE) rateAtMs(idx, minMs, zone) else idx.lastRate
        val txRate = HashMap<String, Double>()
        val txs = db.transactions.map { t ->
            val r = if (t.dateTime > 0) rateAtMs(idx, t.dateTime, zone) else opening
            txRate[t.uid] = r
            t.copy(totalValue = orZero(t.totalValue) / r)
        }
        val details = db.transactionDetails.map { d -> d.copy(value = orZero(d.value) / (txRate[d.transactionUid] ?: opening)) }
        val accounts = db.accounts.map { it.copy(oldBalance = orZero(it.oldBalance) / opening) }
        val persons = db.persons.map { it.copy(oldBalance = orZero(it.oldBalance) / opening) }
        return db.copy(transactions = txs, transactionDetails = details, accounts = accounts, persons = persons)
    }

    fun tomanToUsdLatest(toman: Double, idx: RateIndex): Double = orZero(toman) / idx.lastRate
}
