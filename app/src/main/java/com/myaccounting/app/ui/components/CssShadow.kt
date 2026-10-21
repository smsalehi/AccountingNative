package com.myaccounting.app.ui.components

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp

/** One CSS box-shadow layer: offset-y, blur, spread (all CSS px = dp) and its colour. */
class ShadowLayer(val y: Float, val blur: Float, val spread: Float, val color: Color, val x: Float = 0f)

/** Tailwind v4 shadow scale; [color] replaces the default black when given (e.g. shadow-sky-500/25). */
object TwShadow {
    private val black05 = Color(0f, 0f, 0f, 0.05f)
    private val black10 = Color(0f, 0f, 0f, 0.10f)
    private val black25 = Color(0f, 0f, 0f, 0.25f)
    fun xxs(c: Color = black05) = listOf(ShadowLayer(1f, 0f, 0f, c))
    fun xs(c: Color = black05) = listOf(ShadowLayer(1f, 2f, 0f, c))
    fun md(c: Color = black10) = listOf(ShadowLayer(4f, 6f, -1f, c), ShadowLayer(2f, 4f, -2f, c))
    fun lg(c: Color = black10) = listOf(ShadowLayer(10f, 15f, -3f, c), ShadowLayer(4f, 6f, -4f, c))
    /** shadow-2xl */
    fun xxl(c: Color = black25) = listOf(ShadowLayer(25f, 50f, -12f, c))
}

/**
 * CSS-style `box-shadow` for a rounded rectangle: Gaussian blur (σ = blur/2), spread, offset, and — like CSS —
 * only drawn OUTSIDE the element's own box. Place it before `background`.
 */
fun Modifier.cssShadow(layers: List<ShadowLayer>, cornerRadius: Dp): Modifier = drawBehind {
    val r = cornerRadius.toPx()
    val element = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r))) }
    drawIntoCanvas { canvas ->
        for (l in layers) {
            val sp = l.spread * density
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = l.color.toArgb()
                val sigma = l.blur * density / 2f
                if (sigma > 0.5f) maskFilter = BlurMaskFilter((sigma - 0.5f) / 0.57735f, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.save()
            canvas.clipPath(element, ClipOp.Difference)
            val rr = (r + sp).coerceAtLeast(0f)
            canvas.nativeCanvas.drawRoundRect(
                -sp + l.x * density, -sp + l.y * density,
                size.width + sp + l.x * density, size.height + sp + l.y * density,
                rr, rr, paint,
            )
            canvas.restore()
        }
    }
}
