package com.myaccounting.app.domain

import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Port of src/utils/currency.ts and the formatting half of src/utils/persianDate.ts. */
object Currency {
    const val USD_READONLY_MESSAGE =
        "نمای دلاری فقط برای نمایش است؛ برای افزودن یا ویرایش این بخش، واحد را روی تومان یا ریال بگذارید."

    fun unitLabel(unit: CurrencyUnit): String = when (unit) {
        CurrencyUnit.USD -> "دلار"
        CurrencyUnit.RIAL -> "ریال"
        CurrencyUnit.TOMAN -> "تومان"
    }

    /** JS Math.round semantics: floor(x + 0.5) (round half toward +infinity). */
    fun jsRound(x: Double): Double = Math.floor(x + 0.5)

    private fun safe(v: Double): Double = if (v.isNaN() || v.isInfinite()) 0.0 else v

    /** Numeric value for exports (toman: rounded; rial: ×10; usd: 2 decimals). */
    fun toDisplayNumber(value: Double, unit: CurrencyUnit): Double {
        val s = safe(value)
        return when (unit) {
            CurrencyUnit.USD -> jsRound(s * 100) / 100
            CurrencyUnit.RIAL -> jsRound(s * 10)
            CurrencyUnit.TOMAN -> jsRound(s)
        }
    }

    /** value.toLocaleString('en-US') of |value| (grouping ',', up to 3 fraction digits). */
    fun formatNumberWithCommas(value: Double): String {
        val s = Math.abs(safe(value))
        val df = DecimalFormat("#,##0.###", DecimalFormatSymbols(Locale.US))
        df.roundingMode = RoundingMode.HALF_UP
        return df.format(s)
    }

    /**
     * Amounts are stored in toman. For USD the incoming amount must already be in dollars.
     * The minus sign is intentionally placed AFTER the number (same as the original, RTL-visual order).
     */
    fun formatCurrency(amount: Double, unit: CurrencyUnit = CurrencyUnit.TOMAN, persianNumber: Boolean = true): String {
        val s = safe(amount)
        if (unit == CurrencyUnit.USD) {
            val rounded = jsRound(s * 100) / 100
            val df = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))
            df.roundingMode = RoundingMode.HALF_UP
            val formatted = df.format(Math.abs(rounded))
            val result = if (persianNumber) JalaliDate.toPersianDigits(formatted).replace('.', '٫') else formatted
            val prefix = if (rounded < 0) "-" else ""
            return "$result$prefix ${unitLabel(CurrencyUnit.USD)}"
        }
        val displayAmount = if (unit == CurrencyUnit.TOMAN) jsRound(s) else jsRound(s * 10)
        val formatted = formatNumberWithCommas(displayAmount)
        val result = if (persianNumber) JalaliDate.toPersianDigits(formatted) else formatted
        val prefix = if (s < 0) "-" else ""
        return "$result$prefix ${unitLabel(unit)}"
    }
}
