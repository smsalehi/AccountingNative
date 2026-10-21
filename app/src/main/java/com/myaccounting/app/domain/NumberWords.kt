package com.myaccounting.app.domain

/** Port of src/utils/numberToWords.ts (Persian words for an amount, used by the amount calculator). */
object NumberWords {
    private val ones = listOf("", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه")
    private val teens = listOf("ده", "یازده", "دوازده", "سیزده", "چهارده", "پانزده", "شانزده", "هفده", "هجده", "نوزده")
    private val tens = listOf("", "ده", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود")
    private val hundreds = listOf("", "صد", "دویست", "سیصد", "چهارصد", "پانصد", "ششصد", "هفتصد", "هشتصد", "نهصد")
    private val thousands = listOf("", "هزار", "میلیون", "میلیارد", "تریلیون")

    private fun threeDigits(num: Int): String {
        val parts = mutableListOf<String>()
        val h = num / 100
        val remainder = num % 100
        val t = remainder / 10
        val o = remainder % 10
        if (h > 0) parts += hundreds[h]
        if (remainder in 10..19) {
            parts += teens[remainder - 10]
        } else {
            if (t > 0) parts += tens[t]
            if (o > 0) parts += ones[o]
        }
        return parts.joinToString(" و ")
    }

    fun numberToWords(value: Double): String {
        if (value == 0.0) return "صفر"
        if (value.isNaN()) return ""
        val negative = value < 0
        var num = kotlin.math.abs(kotlin.math.round(value)).toLong()
        val groups = mutableListOf<Long>()
        while (num > 0) {
            groups += num % 1000
            num /= 1000
        }
        val parts = mutableListOf<String>()
        for (i in groups.indices.reversed()) {
            val g = groups[i].toInt()
            if (g > 0) {
                val words = threeDigits(g)
                val scale = thousands.getOrNull(i) ?: ""
                parts += if (scale.isNotEmpty()) "$words $scale" else words
            }
        }
        val result = parts.joinToString(" و ")
        return if (negative) "منفی $result" else result
    }

    /** formatAmountWords(amount, unit) — the unit is only تومان/ریال (the DB is always toman). */
    fun formatAmountWords(amount: Double, unit: CurrencyUnit = CurrencyUnit.TOMAN): String {
        if (amount == 0.0) return "صفر تومان"
        val valToman = if (unit == CurrencyUnit.TOMAN) kotlin.math.round(amount) else kotlin.math.round(amount * 10)
        val words = numberToWords(valToman)
        val label = if (unit == CurrencyUnit.TOMAN) "تومان" else "ریال"
        return "$words $label"
    }
}
