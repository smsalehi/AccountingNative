package com.myaccounting.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.LocalIsDark
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.Vazirmatn
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem
import com.myaccounting.app.ui.theme.remSp

/**
 * Modal shell of the original app: `fixed inset-0 z-50 grid place-items-center p-3 bg-slate-900/70`,
 * a max-w-2xl rounded-3xl card with a 92vh cap. Uses a real Dialog so it also covers the bottom bar.
 */
@Composable
fun TwModal(
    onDismiss: () -> Unit,
    maxWidthRem: Float = 42f,       // max-w-2xl
    maxHeightFraction: Float = 0.92f,
    scrimColor: Color = Tw.Slate900.a(0.7f), // bg-slate-900/70
    topFraction: Float = 0f,        // pt-[20vh] of the calculator modal
    fillHeight: Boolean = true,     // max-h + flex column, or a content-sized card
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val view = LocalView.current
        DisposableEffect(Unit) {
            // No platform dim: the scrim below is the web's exact colour/alpha.
            val window = (view.parent as? DialogWindowProvider)?.window
            window?.setDimAmount(0f)
            onDispose { }
        }
        val none = remember { MutableInteractionSource() }
        val shape = RoundedCornerShape(rem(1.5f))
        val bw = cssBorder()
        Box(
            Modifier.fillMaxSize().background(scrimColor)
                .clickable(none, null, onClick = onDismiss),
        ) {
            BoxWithConstraints(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(rem(0.75f)),
            ) {
                val topPad = maxHeight * topFraction
                Box(Modifier.fillMaxSize().padding(top = topPad), contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.widthIn(max = rem(maxWidthRem)).fillMaxWidth()
                            .then(if (fillHeight) Modifier.fillMaxHeight(maxHeightFraction) else Modifier)
                            .cssShadow(TwShadow.xxl(), rem(1.5f))
                            .clip(shape)
                            .background(dk(Tw.White, Tw.Slate900))
                            .border(bw, dk(Tw.Slate200, Tw.Slate800), shape)
                            .clickable(remember { MutableInteractionSource() }, null) { }
                            .imePadding(),
                        content = content,
                    )
                }
            }
        }
    }
}

/** `label` of the original forms: text-xs font-semibold slate-700 dark:slate-300 mb-1.5. */
@Composable
fun TwLabel(text: String, required: Boolean = false, modifier: Modifier = Modifier) {
    Row(modifier.padding(bottom = rem(0.375f))) {
        TwText(text, 0.75f, 1f, dk(Tw.Slate700, Tw.Slate300), weight = FW.Semibold, maxLines = 1)
        if (required) TwText(" *", 0.75f, 1f, Tw.Rose500, weight = FW.Semibold, maxLines = 1)
    }
}

/**
 * The app's text input: `px-3.5 py-2.5 rounded-xl border bg-white dark:bg-slate-900 text-xs
 * focus:ring-2 focus:ring-sky-500` (mono / dir=ltr variants for numbers).
 */
@Composable
fun TwTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    mono: Boolean = false,
    ltr: Boolean = false,
    singleLine: Boolean = true,
    minHeightRem: Float = 0f,
    textSizeRem: Float = 0.75f,
    lineRem: Float = 1f,
    vPadRem: Float = 0.625f,
    startPadRem: Float = 0.875f,
    endPadRem: Float = 0.875f,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(rem(0.75f))
    val bw = cssBorder()
    val borderW = if (focused) bw * 2f else bw
    val borderColor = if (focused) Tw.Sky500 else dk(Tw.Slate200, Tw.Slate700)
    val focusManager = LocalFocusManager.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth()
            .heightIn(min = if (minHeightRem > 0f) rem(minHeightRem) else 0.dp)
            .onFocusChanged { focused = it.isFocused }
            .background(dk(Tw.White, Tw.Slate900), shape)
            .border(borderW, borderColor, shape)
            .padding(start = rem(startPadRem), end = rem(endPadRem), top = rem(vPadRem), bottom = rem(vPadRem)),
        textStyle = TextStyle(
            fontFamily = if (mono) FontFamily.Monospace else Vazirmatn,
            fontSize = remSp(textSizeRem),
            lineHeight = remSp(lineRem),
            fontWeight = FontWeight.Normal,
            color = dk(Tw.Slate800, Tw.Slate100),
            textDirection = if (ltr) TextDirection.Ltr else TextDirection.Rtl,
            textAlign = if (ltr) TextAlign.Left else TextAlign.Start,
        ),
        singleLine = singleLine,
        cursorBrush = SolidColor(Tw.Sky500),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    TwText(
                        placeholder, textSizeRem, lineRem, Tw.Slate400,
                        maxLines = if (singleLine) 1 else 2,
                        align = if (ltr) TextAlign.Left else TextAlign.Start,
                        direction = if (ltr) TextDirection.Ltr else TextDirection.Rtl,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                inner()
            }
        },
    )
}

/**
 * A button of the original app: rounded-xl, horizontal/vertical padding, optional icon and shadow.
 * Hover/active classes are dropped (touch screen) but the base colours are kept.
 */
@Composable
fun TwButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bg: Color,
    fg: Color,
    borderColor: Color? = null,
    shadow: Boolean = false,
    icon: Int? = null,
    iconRem: Float = 0.875f,
    textRem: Float = 0.75f,
    hPadRem: Float = 0.875f,
    vPadRem: Float = 0.625f,
    weight: Int = FW.Semibold,
    shapeRem: Float = 0.75f,
    center: Boolean = false,
    fillWidth: Boolean = false,
) {
    val shape = RoundedCornerShape(rem(shapeRem))
    Row(
        modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .then(if (shadow) Modifier.cssShadow(TwShadow.xs(), rem(shapeRem)) else Modifier)
            .background(bg, shape)
            .then(if (borderColor != null) Modifier.border(cssBorder(), borderColor, shape) else Modifier)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = rem(hPadRem), vertical = rem(vPadRem)),
        horizontalArrangement = if (center) Arrangement.Center else Arrangement.spacedBy(rem(0.375f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) LucideIcon(icon, iconRem, fg)
        TwText(text, textRem, 1f, fg, weight = weight, maxLines = 1)
    }
}

/** Icon-only square button (`p-2 rounded-xl`). */
@Composable
fun TwIconButton(icon: Int, onClick: () -> Unit, tint: Color, modifier: Modifier = Modifier, padRem: Float = 0.5f, iconRem: Float = 1.25f) {
    Box(
        modifier.clip(RoundedCornerShape(rem(0.75f))).clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(rem(padRem)),
    ) { LucideIcon(icon, iconRem, tint) }
}

/** The little round check bubble used by the bank picker grid. */
@Composable
fun TwCheckBubble(sizeRem: Float = 1.25f, iconRem: Float = 0.75f) {
    Box(Modifier.size(rem(sizeRem)).background(Tw.Sky600, CircleShape), contentAlignment = Alignment.Center) {
        LucideIcon(com.myaccounting.app.R.drawable.ic_lucide_check, iconRem, Tw.White)
    }
}
