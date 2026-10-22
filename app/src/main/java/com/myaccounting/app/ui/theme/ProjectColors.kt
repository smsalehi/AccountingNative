package com.myaccounting.app.ui.theme

import androidx.compose.ui.graphics.Color

/** The colour set of one project card (`utils/projectColors.ts`). */
data class ProjectColorSet(
    val bg: Color,
    val text: Color,
    val border: Color,
    val bgDark: Color,
    val textDark: Color,
    val borderDark: Color,
)

/**
 * Port of `utils/projectColors.ts`: 10 base colours blended into a 19-colour palette; the card
 * colour is derived from the rank of the project (largest absolute balance first).
 */
object ProjectColors {

    private val BASE = listOf(
        "#783a3a", "#78613a", "#78783a", "#57783a", "#3a7857",
        "#3a6778", "#3a5778", "#3a3a78", "#783a78", "#783a57",
    )

    private fun hexToRgb(hex: String): Triple<Int, Int, Int> {
        val h = hex.removePrefix("#")
        return Triple(h.substring(0, 2).toInt(16), h.substring(2, 4).toInt(16), h.substring(4, 6).toInt(16))
    }

    private fun blend(c1: String, c2: String): String {
        val (r1, g1, b1) = hexToRgb(c1)
        val (r2, g2, b2) = hexToRgb(c2)
        return rgbToHex((r1 + r2) / 2.0, (g1 + g2) / 2.0, (b1 + b2) / 2.0)
    }

    private fun rgbToHex(r: Double, g: Double, b: Double): String {
        fun part(v: Double) = java.lang.Math.round(java.lang.Math.max(0.0, java.lang.Math.min(255.0, v)))
            .toInt().toString(16).padStart(2, '0')
        return "#" + part(r) + part(g) + part(b)
    }

    private fun darken(hex: String, factor: Double): String {
        val (r, g, b) = hexToRgb(hex)
        return rgbToHex(r * factor, g * factor, b * factor)
    }

    private fun lighten(hex: String, factor: Double): String {
        val (r, g, b) = hexToRgb(hex)
        return rgbToHex(r + (255 - r) * factor, g + (255 - g) * factor, b + (255 - b) * factor)
    }

    private fun luminance(hex: String): Double {
        val (r, g, b) = hexToRgb(hex)
        return (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
    }

    /** base[0], blend[0-1], base[1], blend[1-2], … */
    private val PALETTE: List<String> = buildList {
        for (i in BASE.indices) {
            add(BASE[i])
            if (i < BASE.size - 1) add(blend(BASE[i], BASE[i + 1]))
        }
    }

    private fun color(hex: String): Color = Color(android.graphics.Color.parseColor(hex))

    fun get(rank: Int): ProjectColorSet {
        val baseIndex = if (rank >= PALETTE.size) rank % PALETTE.size else rank
        val baseHex = PALETTE[baseIndex]

        val bgHex = baseHex
        val lum = luminance(bgHex)
        val textHex = if (lum > 0.45) "#1e293b" else "#f8fafc"
        val borderHex = darken(baseHex, 0.8)

        val bgDarkHex = darken(baseHex, 0.35)
        val borderDarkHex = lighten(bgDarkHex, 0.2)
        val textDarkHex = if (luminance(bgDarkHex) > 0.3) "#f8fafc" else "#e2e8f0"

        return ProjectColorSet(
            bg = color(bgHex),
            text = color(textHex),
            border = color(borderHex),
            bgDark = color(bgDarkHex),
            textDark = color(textDarkHex),
            borderDark = color(borderDarkHex),
        )
    }
}
