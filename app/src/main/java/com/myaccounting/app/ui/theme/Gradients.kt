package com.myaccounting.app.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cbrt
import kotlin.math.pow

/**
 * Tailwind v4 gradients interpolate in OKLab (`linear-gradient(to top right in oklab, …)`).
 * Compose interpolates in sRGB, so the OKLab ramp is pre-computed into many stops.
 */
private fun srgbToLinear(c: Float): Float = if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
private fun linearToSrgb(c: Float): Float {
    val v = c.coerceIn(0f, 1f)
    return if (v <= 0.0031308f) v * 12.92f else 1.055f * v.pow(1f / 2.4f) - 0.055f
}

private fun toOklab(c: Color): FloatArray {
    val r = srgbToLinear(c.red); val g = srgbToLinear(c.green); val b = srgbToLinear(c.blue)
    val l = cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b)
    val m = cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b)
    val s = cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b)
    return floatArrayOf(
        0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s,
        1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s,
        0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s,
    )
}

private fun fromOklab(lab: FloatArray, alpha: Float): Color {
    val l_ = lab[0] + 0.3963377774f * lab[1] + 0.2158037573f * lab[2]
    val m_ = lab[0] - 0.1055613458f * lab[1] - 0.0638541728f * lab[2]
    val s_ = lab[0] - 0.0894841775f * lab[1] - 1.2914855480f * lab[2]
    val l = l_ * l_ * l_; val m = m_ * m_ * m_; val s = s_ * s_ * s_
    val r = 4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s
    val g = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s
    val b = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s
    return Color(linearToSrgb(r), linearToSrgb(g), linearToSrgb(b), alpha)
}

/** Colour at position t∈[0,1] along evenly spaced [colors] (from / via / to), mixed in OKLab. */
fun oklabAt(colors: List<Color>, t: Float): Color {
    if (colors.size == 1) return colors[0]
    val seg = (colors.size - 1) * t.coerceIn(0f, 1f)
    val i = seg.toInt().coerceAtMost(colors.size - 2)
    val f = seg - i
    val a = toOklab(colors[i]); val b = toOklab(colors[i + 1])
    return fromOklab(FloatArray(3) { a[it] + (b[it] - a[it]) * f }, colors[i].alpha + (colors[i + 1].alpha - colors[i].alpha) * f)
}

/** `bg-gradient-to-tr` — from the bottom-left corner to the top-right corner (45° for a square), OKLab interpolated. */
fun toTopRightBrush(size: Size, vararg colors: Color, steps: Int = 32): Brush {
    val list = colors.toList()
    val stops = Array(steps + 1) { i -> (i / steps.toFloat()) to oklabAt(list, i / steps.toFloat()) }
    return Brush.linearGradient(colorStops = stops, start = Offset(0f, size.height), end = Offset(size.width, 0f))
}
