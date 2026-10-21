package com.myaccounting.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.myaccounting.app.R
import com.myaccounting.app.domain.Currency
import com.myaccounting.app.domain.CurrencyUnit
import com.myaccounting.app.domain.Notifications
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem
import kotlin.math.roundToInt

/**
 * Port of components/NotificationDropdown.tsx: a centred popover under the header
 * (`fixed top-20 left-1/2 -translate-x-1/2 w-80 … anim-pop`).
 *
 * Must be called from a full-screen parent (the app root Box): the popover is centred on that
 * parent, i.e. on the screen, like the web's `left-1/2`.
 */
@Composable
fun NotificationDropdown(
    items: List<Notifications.Item>,
    unit: CurrencyUnit,
    onDismissItem: (String) -> Unit,
    onPayInstallment: (String) -> Unit,
    onClose: () -> Unit,
) {
    val density = LocalDensity.current
    // `top-20` = 5rem from the top of the page, which starts under the status bar.
    val topPx = WindowInsets.statusBars.getTop(density) + with(density) { rem(5f).roundToPx() }
    val slidePx = with(density) { rem(0.5f).roundToPx() } // translate3d(0, 8px, 0)
    Popup(
        alignment = Alignment.TopCenter,
        offset = IntOffset(0, topPx),
        onDismissRequest = onClose,
        properties = PopupProperties(focusable = true),
    ) {
        val state = remember { MutableTransitionState(false).apply { targetState = true } }
        val popEase = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
        val pop = tween<Float>(180, easing = popEase)
        val slide = tween<androidx.compose.ui.unit.IntOffset>(180, easing = popEase)
        AnimatedVisibility(
            visibleState = state,
            enter = fadeIn(pop) + scaleIn(pop, initialScale = 0.98f) +
                slideInVertically(slide) { slidePx },
        ) { Panel(items, unit, onDismissItem, onPayInstallment) }
    }
}

@Composable
private fun Panel(
    items: List<Notifications.Item>,
    unit: CurrencyUnit,
    onDismissItem: (String) -> Unit,
    onPayInstallment: (String) -> Unit,
) {
    val bw = cssBorder()
    val shape = RoundedCornerShape(rem(1f)) // rounded-2xl
    Column(
        Modifier.width(rem(20f)) // w-80
            .cssShadow(TwShadow.xxl(), rem(1f))
            .clip(shape)
            .background(dk(Tw.White, Tw.Slate900))
            .border(bw, dk(Tw.Slate200, Tw.Slate800), shape)
            .padding(rem(1f)), // p-4
        verticalArrangement = Arrangement.spacedBy(rem(0.75f)), // space-y-3
    ) {
        Header(bw)
        if (items.isEmpty()) {
            EmptyState()
        } else {
            Column(
                Modifier.fillMaxWidth().heightIn(max = rem(18f)) // max-h-72
                    .verticalScroll(rememberScrollState())
                    .padding(start = rem(0.125f)), // pr-0.5 (physical right in RTL = start)
                verticalArrangement = Arrangement.spacedBy(rem(0.5f)), // space-y-2
            ) {
                items.forEach { item ->
                    ItemRow(item, unit, onDismissItem, onPayInstallment)
                }
            }
        }
    }
}

@Composable
private fun Header(bw: androidx.compose.ui.unit.Dp) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = rem(0.5f)), // pb-2
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f)), verticalAlignment = Alignment.CenterVertically) {
                LucideIcon(R.drawable.ic_lucide_bell, 1f, dk(Tw.Sky600, Tw.Sky400))
                TwText(
                    "یادآوری سررسید اقساط و فاکتورها", 0.75f, 1f, dk(Tw.Slate800, Tw.Slate100),
                    weight = FW.Bold, maxLines = 1,
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(bw).background(dk(Tw.Slate100, Tw.Slate800)))
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(rem(1.5f)), // p-6
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(rem(0.25f)), // space-y-1
    ) {
        Box(Modifier.size(rem(2f)).alpha(0.8f)) { LucideIcon(R.drawable.ic_lucide_circle_check, 2f, Tw.Emerald500) }
        TwText("هیچ قسط معوقه‌ای وجود ندارد", 0.75f, 1f, dk(Tw.Slate700, Tw.Slate300), weight = FW.Bold)
        TwText("تمامی سررسیدها در وضعیت منظم قرار دارند.", 0.6875f, 1.03125f, Tw.Slate400)
    }
}

@Composable
private fun ItemRow(
    item: Notifications.Item,
    unit: CurrencyUnit,
    onDismissItem: (String) -> Unit,
    onPayInstallment: (String) -> Unit,
) {
    val urgent = item.daysDiff <= 1
    val shape = RoundedCornerShape(rem(0.75f)) // rounded-xl
    val bg = if (urgent) dk(Tw.Rose50.a(0.7f), Tw.Rose950.a(0.4f)) else dk(Tw.Slate50.a(0.8f), Tw.Slate800.a(0.5f))
    val border = if (urgent) dk(Tw.Rose200, Tw.Rose900.a(0.6f)) else dk(Tw.Slate200.a(0.6f), Tw.Slate800)
    Row(
        Modifier.fillMaxWidth().background(bg, shape).border(cssBorder(), border, shape).padding(rem(0.75f)), // p-3
        horizontalArrangement = Arrangement.spacedBy(rem(0.625f)), // gap-2.5
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(rem(0.25f))) {
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.375f)), verticalAlignment = Alignment.CenterVertically) {
                TwText(
                    item.title, 0.75f, 1f,
                    if (urgent) dk(Tw.Rose700, Tw.Rose300) else dk(Tw.Slate800, Tw.Slate100),
                    Modifier.weight(1f, fill = false), weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (urgent) {
                    Box(
                        Modifier.background(Tw.Rose600, RoundedCornerShape(rem(0.375f)))
                            .padding(horizontal = rem(0.375f), vertical = rem(0.125f)),
                    ) { TwText("فوریت", 0.5625f, 0.84375f, Tw.White, weight = FW.Black, maxLines = 1) }
                }
            }
            // mt-1 meta line, text-[0.6875rem]
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f)), verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(rem(0.25f))) {
                    TwText("مبلغ قسط:", 0.6875f, 1.03125f, dk(Tw.Slate500, Tw.Slate400), maxLines = 1)
                    TwText(
                        Currency.formatCurrency(item.amount, unit), 0.6875f, 1.03125f,
                        dk(Tw.Slate700, Tw.Slate200), weight = FW.Bold, maxLines = 1,
                    )
                }
                TwText("•", 0.6875f, 1.03125f, dk(Tw.Slate500, Tw.Slate400), maxLines = 1)
                TwText("موعد: ${item.dueDateStr}", 0.6875f, 1.03125f, dk(Tw.Slate500, Tw.Slate400), maxLines = 1)
            }
        }
        // The native app has no installment payment modal yet; the button is present like in the web.
        Box(
            Modifier.background(Tw.Sky600, RoundedCornerShape(rem(0.5f)))
                .clickable(remember { MutableInteractionSource() }, null) { onPayInstallment(item.deptUid) }
                .padding(horizontal = rem(0.625f), vertical = rem(0.375f)),
        ) { TwText("پرداخت", 0.6875f, 1.03125f, Tw.White, weight = FW.Bold, maxLines = 1) }
        Box(
            Modifier.clickable(remember { MutableInteractionSource() }, null) { onDismissItem(item.id) }
                .padding(rem(0.25f)), // p-1
        ) { LucideIcon(R.drawable.ic_lucide_x, 0.875f, Tw.Slate400) }
    }
}
