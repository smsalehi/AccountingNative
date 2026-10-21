package com.myaccounting.app.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import com.myaccounting.app.R

@OptIn(ExperimentalTextApi::class)
val Vazirmatn: FontFamily = FontFamily(
    (1..9).map { it * 100 }.map { w ->
        Font(R.font.vazirmatn, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

/** Tailwind font-weight classes */
object FW {
    const val Normal = 400; const val Medium = 500; const val Semibold = 600; const val Bold = 700; const val Black = 900
}

/**
 * A text exactly like a CSS text with `font-size: sizeRem` and `line-height: lineRem` (both in rem):
 * fixed line box, glyphs centred in it (CSS half-leading), inherited `direction: rtl`.
 *
 * Trim.Both is what makes the line box *exactly* `lineRem`, like a CSS line box. With Trim.None the
 * box grows to Vazirmatn's natural ascent+descent (+≈8 px at 0.75 rem, +≈10 px at 1.25 rem), which
 * made buttons and the title taller than the reference and pushed everything below them down.
 */
@Composable
fun TwText(
    text: String,
    sizeRem: Float,
    lineRem: Float,
    color: Color,
    modifier: Modifier = Modifier,
    weight: Int = FW.Normal,
    trackingEm: Float = 0f,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    align: TextAlign = TextAlign.Start,
    direction: TextDirection = TextDirection.Rtl,
    family: FontFamily = Vazirmatn,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        softWrap = maxLines != 1,
        style = TextStyle(
            fontFamily = family,
            fontSize = remSp(sizeRem),
            lineHeight = remSp(lineRem),
            fontWeight = FontWeight(weight),
            letterSpacing = trackingEm.em,
            textAlign = align,
            textDirection = direction,
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
        ),
    )
}

/**
 * An INLINE `<span>` inside a block `<div>`: the line box is at least as tall as the parent's strut
 * (parent font-size × its line-height) and the span sits on the strut's baseline.
 */
@Composable
fun StrutText(
    text: String,
    parentRem: Float,
    parentLineRem: Float,
    sizeRem: Float,
    lineRem: Float,
    color: Color,
    modifier: Modifier = Modifier,
    weight: Int = FW.Normal,
    trackingEm: Float = 0f,
) {
    val boxLine = maxOf(parentLineRem, lineRem)
    val shift = (boxLine - lineRem) / 2f + VAZIR_HALF_LEADING_SHIFT * (parentRem - sizeRem)
    Box(modifier.height(rem(boxLine))) {
        TwText(text, sizeRem, lineRem, color, Modifier.offset(y = rem(shift)), weight, trackingEm)
    }
}
