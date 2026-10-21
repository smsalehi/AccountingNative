package com.myaccounting.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.myaccounting.app.R
import com.myaccounting.app.domain.JalaliDate
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem

/** `stroke="rgb(14 165 233)"` of the clock hands and the centre dot (Tailwind v3 sky-500). */
private val CLOCK_ACCENT = Color(0xFF0EA5E9)
private const val CX = 120f
private const val CY = 120f
private const val R_OUTER = 92f
private const val R_INNER = 62f

private fun clockPos(index: Int, radius: Float): Offset {
    val angle = (index / 12.0) * 2 * Math.PI - Math.PI / 2
    return Offset(CX + radius * Math.cos(angle).toFloat(), CY + radius * Math.sin(angle).toFloat())
}

private fun pad2(v: Int) = v.toString().padStart(2, '0')

/**
 * `PersianTimePicker` of the original: an analog clock face with an outer ring (hours 1–12, minutes
 * every 5) and an inner ring for the afternoon hours, plus the «الان / انصراف / تایید» footer.
 * Positions, radii and font sizes are the SVG's own numbers (1 unit = 1 CSS px = 1 dp).
 */
@Composable
fun PersianTimeField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "ساعت",
) {
    var open by remember { mutableStateOf(false) }
    PickerFieldBox(
        icon = R.drawable.ic_lucide_clock,
        iconTint = Tw.Slate400,
        value = if (value.isNotEmpty()) pickerDisplay(value) else "",
        placeholder = placeholder,
        onClick = { open = true },
    )
    if (open) {
        PersianClockDialog(value = value, onChange = onChange, onDismiss = { open = false })
    }
}

@Composable
private fun PersianClockDialog(value: String, onChange: (String) -> Unit, onDismiss: () -> Unit) {
    val parts = value.split(":")
    var hour by remember { mutableStateOf(parts.getOrNull(0)?.toIntOrNull() ?: java.time.LocalTime.now().hour) }
    var minute by remember { mutableStateOf(parts.getOrNull(1)?.toIntOrNull() ?: java.time.LocalTime.now().minute) }
    var phase by remember { mutableStateOf('h') }   // 'h' or 'm'
    val minuteActive = phase == 'm'

    TwModal(
        onDismiss = onDismiss,
        maxWidthRem = 20f,          // w-80
        fillHeight = false,
        topFraction = 0.2f,         // pt-[20vh]
        scrimColor = Tw.Slate900.a(0.6f),
        shapeRem = 1f,              // rounded-2xl
        cardBg = dk(Tw.White, Tw.Slate800),
        cardBorder = null,          // the time card has no border
    ) {
        // ---- title --------------------------------------------------------------------------------------
        Box(Modifier.fillMaxWidth().padding(start = rem(1.25f), end = rem(1.25f), top = rem(1f), bottom = rem(0.5f))) {
            TwText("انتخاب ساعت", 0.75f, 1f, dk(Tw.Slate500, Tw.Slate400), weight = FW.Bold, maxLines = 1, trackingEm = 0.025f)
        }

        // ---- MM : HH boxes ------------------------------------------------------------------------------
        Row(
            Modifier.fillMaxWidth().padding(start = rem(1.25f), end = rem(1.25f), bottom = rem(1f)),
            horizontalArrangement = Arrangement.spacedBy(rem(0.25f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimeBox(JalaliDate.toPersianDigits(pad2(minute)), minuteActive) { phase = 'm' }
            TwText(":", 1.875f, 2.25f, dk(Tw.Slate400, Tw.Slate500), weight = FW.Bold, maxLines = 1)
            TimeBox(JalaliDate.toPersianDigits(pad2(hour)), !minuteActive) { phase = 'h' }
        }

        // ---- clock face ---------------------------------------------------------------------------------
        Box(Modifier.fillMaxWidth().padding(start = rem(1f), end = rem(1f), bottom = rem(1f)), contentAlignment = Alignment.Center) {
            ClockFace(hour = hour, minute = minute, phase = phase, onHour = { hour = it; phase = 'm' }, onMinute = { minute = it })
        }

        // ---- footer -------------------------------------------------------------------------------------
        Box(Modifier.fillMaxWidth().height(cssBorder()).background(dk(Tw.Slate200, Tw.Slate700)))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = rem(1.25f), vertical = rem(0.75f)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.clickable(remember { MutableInteractionSource() }, null) {
                    val t = java.time.LocalTime.now()
                    hour = t.hour
                    val m = Math.round(t.minute / 5.0).toInt() * 5
                    minute = if (m >= 60) 0 else m
                },
                horizontalArrangement = Arrangement.spacedBy(rem(0.25f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LucideIcon(R.drawable.ic_lucide_chevron_down, 0.875f, dk(Tw.Slate500, Tw.Slate400))
                TwText("الان", 0.75f, 1f, dk(Tw.Slate500, Tw.Slate400), weight = FW.Bold, maxLines = 1)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f)), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clickable(remember { MutableInteractionSource() }, null) { onDismiss() }
                        .padding(horizontal = rem(1f), vertical = rem(0.5f)),
                ) { TwText("انصراف", 0.875f, 1.25f, dk(Tw.Sky600, Tw.Sky400), weight = FW.Bold, maxLines = 1) }
                Box(
                    Modifier.background(Tw.Sky600, RoundedCornerShape(rem(0.5f)))
                        .clickable(remember { MutableInteractionSource() }, null) {
                            onChange("${pad2(hour)}:${pad2(minute)}")
                            onDismiss()
                        }
                        .padding(horizontal = rem(1f), vertical = rem(0.5f)),
                ) { TwText("تایید", 0.875f, 1.25f, Tw.White, weight = FW.Bold, maxLines = 1) }
            }
        }
    }
}

/** `w-16 h-14 rounded-xl text-3xl font-black` of the hour/minute display. */
@Composable
private fun TimeBox(text: String, active: Boolean, onClick: () -> Unit) {
    val bg = if (active) dk(Tw.Sky100, Tw.Sky900.a(0.5f)) else dk(Tw.Slate100, Tw.Slate700)
    val fg = if (active) dk(Tw.Sky600, Tw.Sky400) else dk(Tw.Slate800, Tw.Slate100)
    Box(
        Modifier.width(rem(4f)).height(rem(3.5f)).background(bg, RoundedCornerShape(rem(0.75f)))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        TwText(text, 1.875f, 2.25f, fg, weight = FW.Black, maxLines = 1, align = TextAlign.Center)
    }
}

@Composable
private fun ClockFace(hour: Int, minute: Int, phase: Char, onHour: (Int) -> Unit, onMinute: (Int) -> Unit) {
    val discColor = dk(Tw.Slate100, Tw.Slate700.a(0.5f))
    val outerLabel = dk(Tw.Slate700, Tw.Slate200)
    val innerLabel = dk(Tw.Slate500, Tw.Slate400)
    val hourTarget = if (hour in 1..12) clockPos(if (hour == 12) 0 else hour, R_OUTER) else clockPos(hour - 12, R_INNER)
    val minuteTarget = clockPos(minute / 5, R_OUTER)

    Box(Modifier.size(240.dp)) {
        Canvas(Modifier.size(240.dp)) {
            val s = size.minDimension / 240f
            drawCircle(discColor, radius = (R_OUTER + 18f) * s, center = Offset(CX * s, CY * s))
            val target = if (phase == 'h') hourTarget else minuteTarget
            drawLine(
                CLOCK_ACCENT,
                start = Offset(CX * s, CY * s),
                end = Offset(target.x * s, target.y * s),
                strokeWidth = 2f * s,
                cap = StrokeCap.Round,
            )
            drawCircle(CLOCK_ACCENT, radius = 4f * s, center = Offset(CX * s, CY * s))
        }

        if (phase == 'h') {
            for (h in 1..12) HourLabel(h, clockPos(h, R_OUTER), 16f, hour == h || (hour == 0 && h == 12), outerLabel, 0.8125f, onHour)
            for ((i, h) in listOf(13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 0).withIndex()) {
                HourLabel(h, clockPos(i, R_INNER), 14f, hour == h, innerLabel, 0.6875f, onHour)
            }
        } else {
            for (i in 0 until 12) {
                val m = i * 5
                val pos = clockPos(i, R_OUTER)
                val selected = m == minute
                Box(
                    Modifier.absoluteOffset(x = (pos.x - 16f).dp, y = (pos.y - 16f).dp).size(32.dp)
                        .background(if (selected) CLOCK_ACCENT else Color.Transparent, CircleShape)
                        .clickable(remember { MutableInteractionSource() }, null) { onMinute(m) },
                    contentAlignment = Alignment.Center,
                ) {
                    TwText(
                        JalaliDate.toPersianDigits(pad2(m)), 0.8125f, 1f,
                        if (selected) Tw.White else outerLabel, weight = FW.Bold, maxLines = 1, align = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun HourLabel(value: Int, pos: Offset, radius: Float, selected: Boolean, labelColor: Color, textRem: Float, onPick: (Int) -> Unit) {
    Box(
        Modifier.absoluteOffset(x = (pos.x - radius).dp, y = (pos.y - radius).dp).size((radius * 2).dp)
            .background(if (selected) CLOCK_ACCENT else Color.Transparent, CircleShape)
            .clickable(remember { MutableInteractionSource() }, null) { onPick(value) },
        contentAlignment = Alignment.Center,
    ) {
        TwText(
            JalaliDate.toPersianDigits(pad2(value)), textRem, 1f,
            if (selected) Tw.White else labelColor, weight = FW.Bold, maxLines = 1, align = TextAlign.Center,
        )
    }
}
