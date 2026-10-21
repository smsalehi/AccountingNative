package com.myaccounting.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.myaccounting.app.R
import com.myaccounting.app.domain.JalaliDate
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem
import java.time.LocalDate

private enum class CalendarView { DAYS, MONTHS, YEARS }

private val PERSIAN_DAY_LETTERS = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

/** PersianDatePicker.tsx `daysInMonth` (the 33-year cycle table of the original, kept verbatim). */
private fun persianDaysInMonth(year: Int, month: Int): Int {
    if (month <= 6) return 31
    if (month <= 11) return 30
    val r = year % 33
    val leap = r == 1 || r == 5 || r == 9 || r == 13 || r == 17 || r == 22 || r == 26 || r == 30
    return if (leap) 30 else 29
}

/** `new Date(gy, gm - 1, gd).getDay()` — 0 = Sunday. */
private fun firstWeekDay(year: Int, month: Int): Int {
    val g = JalaliDate.jalaliToGregorian(year, month, 1)
    return LocalDate.of(g.y, g.m, g.d).dayOfWeek.value % 7
}

/**
 * `PersianDatePicker` of the original: a 50 px read-only field that opens a 18 rem calendar popover
 * (day grid, month grid, 12-year grid and the «امروز» shortcut). The weekday letters and the leading
 * blank cells follow the original's `getDay()` arithmetic, including its off-by-one weekday column.
 */
@Composable
fun PersianDateField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "تاریخ",
) {
    var open by remember { mutableStateOf(false) }
    PickerFieldBox(
        icon = R.drawable.ic_lucide_calendar,
        iconTint = Tw.Slate400,
        value = if (value.isNotEmpty()) pickerDisplay(value) else "",
        placeholder = placeholder,
        onClick = { open = true },
    )
    if (open) {
        PersianCalendarDialog(value = value, onChange = onChange, onDismiss = { open = false })
    }
}

@Composable
private fun PersianCalendarDialog(value: String, onChange: (String) -> Unit, onDismiss: () -> Unit) {
    val today = LocalDate.now()
    val todayJ = JalaliDate.gregorianToJalali(today.year, today.monthValue, today.dayOfMonth)

    val parts = value.split("/")
    val initYear = parts.getOrNull(0)?.toIntOrNull()
    val initMonth = parts.getOrNull(1)?.toIntOrNull()

    var view by remember { mutableStateOf(CalendarView.DAYS) }
    var viewYear by remember { mutableStateOf(initYear ?: todayJ.y) }
    var viewMonth by remember { mutableStateOf(initMonth ?: todayJ.m) }
    var selectedDay by remember { mutableStateOf(parts.getOrNull(2)?.toIntOrNull() ?: todayJ.d) }
    var yearRangeStart by remember { mutableStateOf((initYear ?: todayJ.y).let { it - (it % 12) }) }

    fun pick(day: Int) {
        onChange("$viewYear/${viewMonth.toString().padStart(2, '0')}/${day.toString().padStart(2, '0')}")
        selectedDay = day
        onDismiss()
    }

    TwModal(
        onDismiss = onDismiss,
        maxWidthRem = 18f,          // w-72
        fillHeight = false,         // content-sized card
        topFraction = 0.2f,         // pt-[20vh]
        scrimColor = Tw.Slate900.a(0.6f),
        shapeRem = 0.75f,           // rounded-xl
    ) {
        Column(Modifier.fillMaxWidth().padding(rem(0.875f))) {   // p-3.5
            // ---- header ---------------------------------------------------------------------------------
            Row(
                Modifier.fillMaxWidth().padding(bottom = rem(0.75f)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavChevron(R.drawable.ic_lucide_chevron_right) {
                    when (view) {
                        CalendarView.DAYS -> if (viewMonth == 1) { viewYear--; viewMonth = 12 } else viewMonth--
                        CalendarView.MONTHS -> viewYear--
                        CalendarView.YEARS -> yearRangeStart -= 12
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(rem(0.625f)), verticalAlignment = Alignment.CenterVertically) {
                    when (view) {
                        CalendarView.DAYS -> {
                            CalendarTitle(JalaliDate.PERSIAN_MONTHS[viewMonth - 1]) { view = CalendarView.MONTHS }
                            CalendarTitle(JalaliDate.toPersianDigits(viewYear)) {
                                yearRangeStart = viewYear - (viewYear % 12); view = CalendarView.YEARS
                            }
                        }
                        CalendarView.MONTHS -> CalendarTitle(JalaliDate.toPersianDigits(viewYear)) {
                            yearRangeStart = viewYear - (viewYear % 12); view = CalendarView.YEARS
                        }
                        CalendarView.YEARS -> TwText(
                            "${JalaliDate.toPersianDigits(yearRangeStart)} — ${JalaliDate.toPersianDigits(yearRangeStart + 11)}",
                            1f, 1.5f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1,
                        )
                    }
                }
                NavChevron(R.drawable.ic_lucide_chevron_left) {
                    when (view) {
                        CalendarView.DAYS -> if (viewMonth == 12) { viewYear++; viewMonth = 1 } else viewMonth++
                        CalendarView.MONTHS -> viewYear++
                        CalendarView.YEARS -> yearRangeStart += 12
                    }
                }
            }

            when (view) {
                CalendarView.DAYS -> DayGrid(
                    year = viewYear, month = viewMonth, selectedDay = selectedDay,
                    todayYear = todayJ.y, todayMonth = todayJ.m, todayDay = todayJ.d, onPick = ::pick,
                )
                CalendarView.MONTHS -> TripleGrid(12, 4) { index ->
                    val month = index + 1
                    CellButton(
                        text = JalaliDate.PERSIAN_MONTHS[index],
                        active = month == viewMonth && viewYear == todayJ.y,
                        small = false,
                    ) { viewMonth = month; view = CalendarView.DAYS }
                }
                CalendarView.YEARS -> TripleGrid(12, 4) { index ->
                    val year = yearRangeStart + index
                    CellButton(
                        text = JalaliDate.toPersianDigits(year),
                        active = year == viewYear,
                        small = false,
                    ) { viewYear = year; view = CalendarView.MONTHS }
                }
            }

            // ---- today shortcut -------------------------------------------------------------------------
            Box(
                Modifier.fillMaxWidth().padding(top = rem(0.625f)).clickable(
                    remember { MutableInteractionSource() }, null,
                ) {
                    onChange("${todayJ.y}/${todayJ.m.toString().padStart(2, '0')}/${todayJ.d.toString().padStart(2, '0')}")
                    viewYear = todayJ.y; viewMonth = todayJ.m; selectedDay = todayJ.d
                    onDismiss()
                }.padding(vertical = rem(0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                TwText("امروز", 0.875f, 1.25f, dk(Tw.Sky600, Tw.Sky400), weight = FW.Semibold, maxLines = 1, align = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun NavChevron(icon: Int, onClick: () -> Unit) {
    Box(Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClick).padding(rem(0.375f))) {
        LucideIcon(icon, 1.25f, dk(Tw.Slate700, Tw.Slate200))
    }
}

@Composable
private fun CalendarTitle(text: String, onClick: () -> Unit) {
    Box(Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClick)) {
        TwText(text, 1f, 1.5f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1)
    }
}

/** `grid-cols-7 gap-1` — every column is 1fr, the 2.25 rem day cells are start-aligned inside it. */
@Composable
private fun DayGrid(year: Int, month: Int, selectedDay: Int, todayYear: Int, todayMonth: Int, todayDay: Int, onPick: (Int) -> Unit) {
    val total = persianDaysInMonth(year, month)
    val first = firstWeekDay(year, month)
    val weeks = ArrayList<List<Int?>>(6)
    var day = 1
    for (w in 0 until 6) {
        val week = arrayOfNulls<Int>(7)
        for (d in 0..6) {
            when {
                w == 0 && d < first -> {}
                day > total -> {}
                else -> week[d] = day++
            }
        }
        weeks += week.toList()
        if (day > total) break
    }
    val shape = RoundedCornerShape(rem(0.5f))
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = rem(0.25f)
        val cell = (maxWidth - gap * 6) / 7
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (letter in PERSIAN_DAY_LETTERS) {
                    Box(Modifier.width(cell).padding(vertical = rem(0.25f)), contentAlignment = Alignment.Center) {
                        TwText(letter, 0.75f, 1f, dk(Tw.Slate500, Tw.Slate400), weight = FW.Bold, maxLines = 1, align = TextAlign.Center)
                    }
                }
                // the columns are 1fr wide: the last cell of the header row is the remaining width
            }
            for (week in weeks) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (d in week) {
                        Box(Modifier.width(cell), contentAlignment = Alignment.CenterStart) {
                            if (d != null) {
                                val isSelected = d == selectedDay && year == todayYear && month == todayMonth
                                val isToday = year == todayYear && month == todayMonth && d == todayDay
                                val bg = when {
                                    isSelected -> Tw.Sky600
                                    isToday -> dk(Tw.Sky100, Tw.Sky900)
                                    else -> androidx.compose.ui.graphics.Color.Transparent
                                }
                                val fg = when {
                                    isSelected -> Tw.White
                                    isToday -> dk(Tw.Sky700, Tw.Sky300)
                                    else -> dk(Tw.Slate700, Tw.Slate200)
                                }
                                Box(
                                    Modifier.width(rem(2.25f)).height(rem(2.25f)).background(bg, shape)
                                        .clickable(remember { MutableInteractionSource() }, null) { onPick(d) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    TwText(JalaliDate.toPersianDigits(d), 0.875f, 1.25f, fg, weight = FW.Medium, maxLines = 1, align = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** `grid-cols-3 gap-2` of the month and year views. */
@Composable
private fun TripleGrid(count: Int, rows: Int, cell: @Composable (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(rem(0.5f))) {
        for (r in 0 until rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f))) {
                for (c in 0 until 3) {
                    val index = r * 3 + c
                    if (index < count) Box(Modifier.weight(1f)) { cell(index) }
                }
            }
        }
    }
}

@Composable
private fun CellButton(text: String, active: Boolean, small: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(rem(0.5f))
    Box(
        Modifier.fillMaxWidth().background(if (active) Tw.Sky600 else androidx.compose.ui.graphics.Color.Transparent, shape)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick).padding(vertical = rem(0.75f)),
        contentAlignment = Alignment.Center,
    ) {
        TwText(
            text, if (small) 0.8125f else 0.875f, if (small) 1.25f else 1.25f,
            if (active) Tw.White else dk(Tw.Slate700, Tw.Slate200),
            weight = FW.Semibold, maxLines = 1, align = TextAlign.Center,
        )
    }
}
