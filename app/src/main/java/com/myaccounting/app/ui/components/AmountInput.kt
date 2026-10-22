package com.myaccounting.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import com.myaccounting.app.R
import com.myaccounting.app.domain.Currency
import com.myaccounting.app.domain.CurrencyUnit
import com.myaccounting.app.domain.JalaliDate
import com.myaccounting.app.domain.NumberWords
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

/**
 * Port of components/AmountInputWithCalculator.tsx: a read-only, Persian-formatted amount that opens
 * the same immediate-execution calculator. Values in the DB are always toman (rial is ×10 on screen).
 */
@Composable
fun AmountInput(
    value: Double,
    onChange: (Double) -> Unit,
    unit: CurrencyUnit,
    label: String? = null,
    placeholder: String = "۰",
    allowZero: Boolean = false,
    helperText: String? = null,
    modifier: Modifier = Modifier,
) {
    val currency = if (unit == CurrencyUnit.RIAL) CurrencyUnit.RIAL else CurrencyUnit.TOMAN
    val displayValue = abs(if (currency == CurrencyUnit.TOMAN) Currency.jsRound(value) else Currency.jsRound(value * 10))
    var calcOpen by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(rem(0.75f))

    Column(modifier) {
        if (label != null) TwLabel(label)
        // The web input is `dir="ltr"` with the currency at `absolute left-2` and `pl-24 pr-4 text-left`:
        // the whole box must resolve its alignment in LTR, otherwise Compose puts the unit on the right.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                Modifier.fillMaxWidth().heightIn(min = rem(2.5f))
                    .background(dk(Tw.White, Tw.Slate900), shape)
                    .border(cssBorder(), dk(Tw.Slate200, Tw.Slate700), shape)
                    .clickable(remember { MutableInteractionSource() }, null) { calcOpen = true }
                    .padding(vertical = rem(0.625f)),
            ) {
                val shown = when {
                    displayValue > 0 -> JalaliDate.toPersianDigits(Currency.formatNumberWithCommas(displayValue))
                    allowZero && value == 0.0 -> JalaliDate.toPersianDigits("0")
                    else -> ""
                }
                Row(
                    Modifier.align(Alignment.CenterStart).padding(start = rem(0.5f)),
                    horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TwText(
                        if (currency == CurrencyUnit.RIAL) "ریال" else "تومان",
                        0.75f, 1f, dk(Tw.Slate400, Tw.Slate500), weight = FW.Bold,
                        direction = TextDirection.Rtl,
                    )
                }
                TwText(
                    if (shown.isNotEmpty()) shown else placeholder,
                    0.875f, 1.25f,
                    if (shown.isNotEmpty()) dk(Tw.Slate800, Tw.Slate100) else Tw.Slate400,
                    weight = FW.Bold,
                    family = FontFamily.Monospace,
                    direction = TextDirection.Ltr,
                    align = TextAlign.Left,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth().padding(start = rem(6f)).align(Alignment.CenterStart),
                )
            }
        }
        if (displayValue > 0) {
            TwText(
                NumberWords.formatAmountWords(value, currency), 0.6875f, 1.03125f, dk(Tw.Slate500, Tw.Slate400),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = rem(0.25f)),
            )
        }
        if (helperText != null) {
            TwText(
                helperText, 0.6875f, 1.03125f, Tw.Slate400,
                modifier = Modifier.fillMaxWidth().padding(top = rem(0.25f)),
            )
        }
    }

    if (calcOpen) {
        CalculatorDialog(
            displayValue = displayValue,
            currency = currency,
            onDismiss = { calcOpen = false },
            onApply = { shownAmount ->
                val dbValue = if (currency == CurrencyUnit.TOMAN) shownAmount else round(shownAmount / 10)
                onChange(dbValue)
                calcOpen = false
            },
        )
    }
}

private enum class CalcOp(val symbol: String) { PLUS("+"), MINUS("-"), TIMES("×"), DIV("÷") }

private fun jsApply(a: Double, op: CalcOp, b: Double): Double? = when (op) {
    CalcOp.PLUS -> a + b
    CalcOp.MINUS -> a - b
    CalcOp.TIMES -> a * b
    CalcOp.DIV -> if (b == 0.0) null else a / b
}

@Composable
private fun CalculatorDialog(
    displayValue: Double,
    currency: CurrencyUnit,
    onDismiss: () -> Unit,
    onApply: (Double) -> Unit,
) {
    var entry by remember { mutableStateOf(if (displayValue > 0) displayValue.toLong().toString() else "0") }
    var acc by remember { mutableStateOf<Double?>(null) }
    var pending by remember { mutableStateOf<CalcOp?>(null) }
    var fresh by remember { mutableStateOf(true) }
    var justEval by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    fun clearCalc() {
        entry = "0"; acc = null; pending = null; fresh = false; justEval = false; error = false
    }

    fun inputDigits(d: String) {
        var e = entry
        var a = acc
        var p = pending
        var f = fresh
        var j = justEval
        if (error) { e = "0"; a = null; p = null; f = false; j = false }
        val zeroKeys = d == "00" || d == "000"
        if (j || (f && p == null)) {
            a = null; p = null; j = false
            e = if (zeroKeys) "0" else d
            f = false
        } else if (f) {
            e = if (zeroKeys) "0" else d
            f = false
        } else if (e == "0") {
            e = if (zeroKeys) "0" else d
        } else if (e.replace("-", "").length + d.length <= 12) {
            e += d
        }
        entry = e; acc = a; pending = p; fresh = f; justEval = j; error = false
    }

    fun pressOp(op: CalcOp) {
        if (error) {
            clearCalc(); acc = 0.0; pending = op; entry = "0"; fresh = true
            return
        }
        var a = acc
        val e = entry.toDoubleOrNull() ?: 0.0
        if (a == null) {
            a = e
        } else if (!fresh && pending != null) {
            val r = jsApply(a, pending!!, e)
            if (r == null) { error = true; return }
            a = round(r)
        }
        acc = a; pending = op; entry = "0"; fresh = true; justEval = false
    }

    fun pressEquals() {
        if (error) return
        val a = acc
        val p = pending
        if (a != null && p != null) {
            val r = jsApply(a, p, entry.toDoubleOrNull() ?: 0.0)
            if (r == null) { error = true; return }
            val rounded = round(r)
            acc = rounded
            entry = rounded.toLong().toString()
        }
        pending = null; fresh = true; justEval = true
    }

    fun pressBack() {
        if (error || justEval) { clearCalc(); return }
        if (fresh && pending != null) {
            pending = null; acc = null; entry = "0"; fresh = false
            return
        }
        entry = if (entry.length > 1) entry.dropLast(1) else "0"
        fresh = false
    }

    fun pressPercent() {
        if (error) { clearCalc(); return }
        if (justEval) return
        val e = entry.toDoubleOrNull() ?: 0.0
        entry = if (acc != null && (pending == CalcOp.PLUS || pending == CalcOp.MINUS)) {
            round((acc!! * e) / 100).toLong().toString()
        } else {
            (e / 100).toString()
        }
        fresh = false
    }

    fun pressKey(key: String) {
        when (key) {
            "C" -> clearCalc()
            "BACK" -> pressBack()
            "=" -> pressEquals()
            "%" -> pressPercent()
            "+" -> pressOp(CalcOp.PLUS)
            "-" -> pressOp(CalcOp.MINUS)
            "×" -> pressOp(CalcOp.TIMES)
            "÷" -> pressOp(CalcOp.DIV)
            else -> inputDigits(key)
        }
    }

    val shown = if (error) 0.0 else if (fresh && !justEval && acc != null) acc!! else entry.toDoubleOrNull() ?: 0.0
    val dbEquivalent = if (currency == CurrencyUnit.TOMAN) shown else round(shown / 10)
    val formula = if (!error && pending != null && acc != null) {
        "${JalaliDate.toPersianDigits(Currency.formatNumberWithCommas(acc!!))} ${pending!!.symbol} " +
            JalaliDate.toPersianDigits(Currency.formatNumberWithCommas(entry.toDoubleOrNull() ?: 0.0))
    } else ""

    fun applyResult() {
        if (error) { onDismiss(); return }
        val a = acc
        val p = pending
        val result = if (a != null && p != null) {
            jsApply(a, p, entry.toDoubleOrNull() ?: 0.0)?.let { round(it) } ?: 0.0
        } else {
            round(entry.toDoubleOrNull() ?: 0.0)
        }
        onApply(max(0.0, result))
    }

    TwModal(
        onDismiss = onDismiss,
        maxWidthRem = 20f, // max-w-xs on phones
        fillHeight = false,
        scrimColor = Tw.Slate900.a(0.6f),
        topFraction = 0.2f,
    ) {
        // header
        Row(
            Modifier.fillMaxWidth().background(dk(Tw.Slate50.a(0.7f), Tw.Slate800.a(0.4f))).padding(rem(0.875f)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TwText("ماشین‌حساب", 0.75f, 1f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold)
            TwIconButton(R.drawable.ic_lucide_x, onDismiss, Tw.Slate400, padRem = 0.25f, iconRem = 1f)
        }
        Box(Modifier.fillMaxWidth().height(cssBorder()).background(dk(Tw.Slate100, Tw.Slate800)))

        // display
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = rem(5f)).background(Tw.Slate900).padding(rem(1f)),
                horizontalAlignment = Alignment.Start,
            ) {
                TwText(formula, 0.875f, 1.25f, Tw.Slate400, family = FontFamily.Monospace, maxLines = 1)
                Row(verticalAlignment = Alignment.Bottom) {
                    TwText(
                        if (error) "خطا" else JalaliDate.toPersianDigits(Currency.formatNumberWithCommas(shown)),
                        1.875f, 2.25f, Tw.Sky400, weight = FW.Black, family = FontFamily.Monospace, maxLines = 1,
                    )
                    TwText(" ${if (currency == CurrencyUnit.RIAL) "ریال" else "تومان"}", 0.875f, 1.25f, Tw.Slate400, maxLines = 1)
                }
                TwText(
                    if (error) "عملیات نامعتبر است (مثلاً تقسیم بر صفر)" else NumberWords.formatAmountWords(dbEquivalent, currency),
                    0.6875f, 1.03125f, Tw.Slate300, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // keypad
        Column(
            Modifier.fillMaxWidth().background(dk(Tw.Slate100.a(0.6f), Tw.Slate900)).padding(rem(0.75f)),
            verticalArrangement = Arrangement.spacedBy(rem(0.375f)),
        ) {
            @Composable
            fun RowScope.Key(key: String, bg: Color, fg: Color, textRem: Float = 1f, icon: Int? = null, weight: Int = FW.Bold) {
                Box(
                    Modifier.weight(1f).background(bg, RoundedCornerShape(rem(0.75f)))
                        .clickable(remember { MutableInteractionSource() }, null) { pressKey(key) }
                        .padding(vertical = rem(0.75f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (icon != null) LucideIcon(icon, 1.25f, fg)
                    else TwText(key, textRem, 1.5f, fg, weight = weight, align = TextAlign.Center, direction = TextDirection.Ltr, maxLines = 1)
                }
            }

            val rowBg = dk(Tw.Slate200.a(0.8f), Tw.Slate800)
            val rowFg = dk(Tw.Slate700, Tw.Slate300)
            val opBg = dk(Tw.Sky100, Tw.Sky950.a(0.7f))
            val opFg = dk(Tw.Sky700, Tw.Sky300)
            val numBg = dk(Tw.White, Tw.Slate800)
            val numFg = dk(Tw.Slate800, Tw.Slate100)

            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.375f))) {
                Key("C", dk(Tw.Rose100, Tw.Rose950.a(0.6f)), dk(Tw.Rose700, Tw.Rose300))
                Key("BACK", rowBg, rowFg, icon = R.drawable.ic_lucide_delete)
                Key("%", rowBg, rowFg)
                Key("÷", opBg, opFg, textRem = 1.125f)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.375f))) {
                Key("9", numBg, numFg, weight = FW.Bold)
                Key("8", numBg, numFg, weight = FW.Bold)
                Key("7", numBg, numFg, weight = FW.Bold)
                Key("×", opBg, opFg, textRem = 1.125f)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.375f))) {
                Key("6", numBg, numFg, weight = FW.Bold)
                Key("5", numBg, numFg, weight = FW.Bold)
                Key("4", numBg, numFg, weight = FW.Bold)
                Key("-", opBg, opFg, textRem = 1.125f)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.375f))) {
                Key("3", numBg, numFg, weight = FW.Bold)
                Key("2", numBg, numFg, weight = FW.Bold)
                Key("1", numBg, numFg, weight = FW.Bold)
                Key("+", opBg, opFg, textRem = 1.125f)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.375f))) {
                Key("00", numBg, numFg, textRem = 0.875f)
                Key("0", numBg, numFg, weight = FW.Bold)
                Key("000", numBg, numFg, textRem = 0.875f)
                Key("=", Tw.Amber500, Tw.White, textRem = 1.125f, weight = FW.Black)
            }
        }

        // action bar
        Row(
            Modifier.fillMaxWidth().padding(rem(0.75f)),
            horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TwButton(
                "تایید و درج مبلغ", { applyResult() },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                bg = Tw.Sky600, fg = Tw.White, icon = R.drawable.ic_lucide_check, iconRem = 1f,
                textRem = 0.75f, hPadRem = 1f, weight = FW.Bold, center = true, fillWidth = true, shadow = true,
            )
            TwButton(
                "انصراف", onDismiss,
                bg = dk(Tw.White, Tw.Slate900), fg = dk(Tw.Slate600, Tw.Slate300),
                borderColor = dk(Tw.Slate200, Tw.Slate700), hPadRem = 1f,
            )
        }
    }
}
