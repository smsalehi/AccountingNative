package com.myaccounting.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Jalali (Shamsi) calendar — line-by-line port of src/utils/persianDate.ts
 * (gregorianToJalali, Julian-Day based jalaliToGregorian, jalCal leap table, month arithmetic, digits, currency text).
 */
object JalaliDate {
    val PERSIAN_MONTHS = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    /** index = java.util.Date#getDay() style: 0 = Sunday */
    val PERSIAN_WEEKDAYS = listOf("یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه", "شنبه")

    data class Ymd(val y: Int, val m: Int, val d: Int)

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Ymd {
        val gDM = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 + gd + gDM[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + days / 31
            jd = 1 + (days % 31)
        } else {
            jm = 7 + (days - 186) / 30
            jd = 1 + ((days - 186) % 30)
        }
        return Ymd(jy, jm, jd)
    }

    // Truncating div/mod (as jalaali-js `~~`); Kotlin's Int `/` and `%` already truncate.
    private fun jdiv(a: Int, b: Int): Int = a / b
    private fun jmod(a: Int, b: Int): Int = a % b

    private fun j2d(jy: Int, jm: Int, jd: Int): Int {
        val r = jalCal(jy)
        return g2d(r.gy, 3, r.march) + (jm - 1) * 31 - jdiv(jm, 7) * (jm - 7) + jd - 1
    }

    private fun g2d(gy: Int, gm: Int, gd: Int): Int {
        var d = jdiv((gy + jdiv(gm - 8, 6) + 100100) * 1461, 4) + jdiv(153 * jmod(gm + 9, 12) + 2, 5) + gd - 34840408
        d = d - jdiv(jdiv(gy + 100100 + jdiv(gm - 8, 6), 100) * 3, 4) + 752
        return d
    }

    private fun d2g(jdn: Int): Ymd {
        var j = 4 * jdn + 139361631
        j = j + jdiv(jdiv(4 * jdn + 183187720, 146097) * 3, 4) * 4 - 3908
        val i = jdiv(jmod(j, 1461), 4) * 5 + 308
        val gd = jdiv(jmod(i, 153), 5) + 1
        val gm = (jdiv(i, 153) % 12) + 1
        val gy = jdiv(j, 1461) - 100100 + jdiv(8 - gm, 6)
        return Ymd(gy, gm, gd)
    }

    private val BREAKS = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635, 2060, 2097, 2192,
        2262, 2324, 2394, 2456, 3178,
    )

    private class JalCal(val leap: Int, val gy: Int, val march: Int)

    private fun jalCal(jy: Int, withoutLeap: Boolean = true): JalCal {
        val bl = BREAKS.size
        val gy = jy + 621
        var leapJ = -14
        var jp = BREAKS[0]
        var jm: Int
        var jump = 0
        var n: Int
        var i = 1
        while (i < bl) {
            jm = BREAKS[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ = leapJ + Math.floorDiv(jump, 33) * 8 + Math.floorDiv(jump % 33, 4)
            jp = jm
            i++
        }
        n = jy - jp
        leapJ = leapJ + Math.floorDiv(n, 33) * 8 + Math.floorDiv((n % 33) + 3, 4)
        if (jump % 33 == 4 && jump - n == 4) leapJ += 1
        val leapG = Math.floorDiv(gy, 4) - Math.floorDiv((Math.floorDiv(gy, 100) + 1) * 3, 4) - 150
        val march = 20 + leapJ - leapG
        if (withoutLeap) return JalCal(0, gy, march)
        if (jump - n < 6) n = n - jump + Math.floorDiv(jump + 4, 33) * 33
        var leap = ((n + 1) % 33 - 1) % 4
        if (leap == -1) leap = 4
        return JalCal(leap, gy, march)
    }

    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Ymd = d2g(j2d(jy, jm, jd))

    fun getJalaliMonthLength(jy: Int, jm: Int): Int {
        if (jm <= 6) return 31
        if (jm <= 11) return 30
        return if (jalCal(jy, false).leap == 0) 30 else 29
    }

    fun addJalaliMonths(jy: Int, jm: Int, n: Int): Pair<Int, Int> {
        val total = jy * 12 + (jm - 1) + n
        val year = Math.floorDiv(total, 12)
        val month = total - year * 12 + 1
        return Pair(year, month)
    }

    /** Jalali date → epoch ms of local midnight in [zone] */
    fun jalaliToGregorianMs(jy: Int, jm: Int, jd: Int, zone: ZoneId = ZoneId.systemDefault()): Long {
        val g = jalaliToGregorian(jy, jm, jd)
        return LocalDate.of(g.y, g.m, g.d).atStartOfDay(zone).toInstant().toEpochMilli()
    }


    private fun localDate(ms: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()

    fun toJalali(ms: Long, zone: ZoneId = ZoneId.systemDefault()): Ymd {
        val ld = localDate(ms, zone)
        return gregorianToJalali(ld.year, ld.monthValue, ld.dayOfMonth)
    }

    /** "1403/06/15" */
    fun formatJalaliDate(ms: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val j = toJalali(ms, zone)
        return "${j.y}/${j.m.toString().padStart(2, '0')}/${j.d.toString().padStart(2, '0')}"
    }

    private fun weekdayIndex(ld: LocalDate): Int = ld.dayOfWeek.value % 7 // Sunday = 0

    /** «یکشنبه، ۱۵ شهریور ۱۴۰۳» */
    fun getFullJalaliDate(ms: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val ld = localDate(ms, zone)
        val j = gregorianToJalali(ld.year, ld.monthValue, ld.dayOfMonth)
        return "${PERSIAN_WEEKDAYS[weekdayIndex(ld)]}، ${toPersianDigits(j.d)} ${PERSIAN_MONTHS[j.m - 1]} ${toPersianDigits(j.y)}"
    }

    /** «یکشنبه، ۱۵ شهریور» */
    fun getShortJalaliDate(ms: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val ld = localDate(ms, zone)
        val j = gregorianToJalali(ld.year, ld.monthValue, ld.dayOfMonth)
        return "${PERSIAN_WEEKDAYS[weekdayIndex(ld)]}، ${toPersianDigits(j.d)} ${PERSIAN_MONTHS[j.m - 1]}"
    }

    /** «۲۵ آبان» */
    fun formatJalaliDayMonth(ms: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val j = toJalali(ms, zone)
        return "${toPersianDigits(j.d)} ${PERSIAN_MONTHS[j.m - 1]}"
    }

    private const val FA = "۰۱۲۳۴۵۶۷۸۹"

    fun toPersianDigits(input: Any?): String {
        val s = input?.toString() ?: ""
        val sb = StringBuilder(s.length)
        for (c in s) sb.append(if (c in '0'..'9') FA[c - '0'] else c)
        return sb.toString()
    }

    fun toEnglishDigits(input: Any?): String {
        val s = input?.toString() ?: ""
        val sb = StringBuilder(s.length)
        for (c in s) {
            val idx = FA.indexOf(c)
            sb.append(if (idx >= 0) ('0' + idx) else c)
        }
        return sb.toString()
    }

    /** Standardise to YYYY/MM/DD with English digits (comparison key) */
    fun normalizePersianDate(dateStr: Any?): String {
        if (dateStr == null || dateStr == "") return ""
        val strVal = dateStr.toString().trim()
        if (strVal.isEmpty()) return ""
        val en = toEnglishDigits(strVal)
        val parts = Regex("[/ -]").split(en).map { it.padStart(2, '0') }
        if (parts.size == 3) {
            val y = if (parts[0].length == 2) "14${parts[0]}" else parts[0]
            return "$y/${parts[1]}/${parts[2]}"
        }
        return en
    }
}
