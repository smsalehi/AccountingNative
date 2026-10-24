package com.myaccounting.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.myaccounting.app.R
import com.myaccounting.app.data.AppLog
import com.myaccounting.app.ui.components.ConfirmDialog
import com.myaccounting.app.ui.components.ConfirmVariant
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.components.TwSearchField
import com.myaccounting.app.ui.components.TwSelectField
import com.myaccounting.app.ui.components.TwShadow
import com.myaccounting.app.ui.components.cssShadow
import com.myaccounting.app.ui.export.ExportDeliverer
import com.myaccounting.app.ui.export.rememberExportDeliverer
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem
import com.myaccounting.app.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.Locale

/**
 * LogsView.tsx ported: stats cards, level/category filters, search, JSON/TXT export into the
 * exports folder and clear-all with confirmation.
 */
@Composable
fun LogsTab(vm: MainViewModel) {
    val logs by AppLog.logs.collectAsState()
    var levelFilter by remember { mutableStateOf("all") }
    var categoryFilter by remember { mutableStateOf("all") }
    var query by remember { mutableStateOf("") }
    var expandedId by remember { mutableStateOf<Long?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val deliverer = rememberExportDeliverer(vm)

    val counts = remember(logs) {
        mapOf(
            "total" to logs.size,
            "error" to logs.count { it.level == AppLog.Level.ERROR },
            "warning" to logs.count { it.level == AppLog.Level.WARNING },
            "importError" to logs.count { it.category == AppLog.Category.IMPORT && it.level == AppLog.Level.ERROR },
        )
    }
    val q = query.trim()
    val filtered = remember(logs, levelFilter, categoryFilter, q) {
        logs.filter { l ->
            (levelFilter == "all" || l.level.name.lowercase() == levelFilter) &&
                (categoryFilter == "all" || l.category.name.lowercase() == categoryFilter) &&
                (q.isEmpty() || l.message.contains(q) || (l.details ?: "").contains(q))
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(rem(0.625f))) {
        // ---- stats -----------------------------------------------------------------------------------
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(rem(0.625f))) {
            StatCard("کل رویدادها", counts["total"] ?: 0, dk(Tw.Slate700, Tw.Slate200), Modifier.weight(1f))
            StatCard("خطاها", counts["error"] ?: 0, dk(Tw.Rose600, Tw.Rose400), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(rem(0.625f))) {
            StatCard("هشدارها", counts["warning"] ?: 0, dk(Tw.Amber600, Tw.Amber400), Modifier.weight(1f))
            StatCard("خطای ایمپورت", counts["importError"] ?: 0, Color(0xFF9333EA), Modifier.weight(1f))
        }

        // ---- filters ----------------------------------------------------------------------------------
        TwSearchField(value = query, onValueChange = { query = it }, placeholder = "جستجو در متن و جزئیات لاگ…")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(rem(0.5f))) {
            TwSelectField(
                selectedUid = levelFilter,
                options = listOf("all" to "همه سطوح", "info" to "اطلاع", "success" to "موفق", "warning" to "هشدار", "error" to "خطا"),
                placeholder = "همه سطوح",
                onSelect = { levelFilter = it },
                modifier = Modifier.weight(1f),
            )
            TwSelectField(
                selectedUid = categoryFilter,
                options = listOf("all" to "همه دسته‌ها") + AppLog.Category.entries.map { it.name.lowercase() to it.title },
                placeholder = "همه دسته‌ها",
                onSelect = { categoryFilter = it },
                modifier = Modifier.weight(1f),
            )
        }

        // ---- actions ----------------------------------------------------------------------------------
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(rem(0.5f))) {
            TwTextButton("خروجی JSON", Tw.Sky600, Tw.White, Modifier.weight(1f)) {
                if (logs.isEmpty()) vm.info("لاگی برای خروجی وجود ندارد.")
                else deliverer.deliver("smart_accounting_logs.json", AppLog.toJson())
            }
            TwTextButton(
                "خروجی متنی", dk(Tw.Slate100, Tw.Slate800), dk(Tw.Slate700, Tw.Slate200), Modifier.weight(1f),
                border = dk(Tw.Slate200, Tw.Slate700),
            ) {
                if (logs.isEmpty()) vm.info("لاگی برای خروجی وجود ندارد.")
                else deliverer.deliver("smart_accounting_logs.txt", AppLog.toTxt())
            }
        }
        Row(Modifier.fillMaxWidth()) {
            TwTextButton(
                "پاک‌سازی لاگ‌ها", dk(Tw.Rose50, Tw.Rose950.a(0.3f)), dk(Tw.Rose600, Tw.Rose300), Modifier.weight(1f),
                border = dk(Tw.Rose200, Tw.Rose900.a(0.5f)), icon = R.drawable.ic_lucide_trash2,
            ) { confirmClear = true }
        }
        TwText(
            "${NumberFormat.getNumberInstance(Locale("fa", "IR")).format(filtered.size)} رویداد نمایش داده می‌شود",
            0.6875f, 1f, Tw.Slate400, maxLines = 1,
        )

        // ---- list -------------------------------------------------------------------------------------
        if (filtered.isEmpty()) {
            val shape = RoundedCornerShape(rem(0.75f))
            Box(
                Modifier.fillMaxWidth().background(dk(Tw.Slate50, Tw.Slate800.a(0.6f)), shape)
                    .border(cssBorder(), dk(Tw.Slate200.a(0.7f), Tw.Slate700), shape)
                    .padding(vertical = rem(2f)),
                contentAlignment = Alignment.Center,
            ) { TwText("لاگی ثبت نشده است.", 0.75f, 1f, Tw.Slate500, maxLines = 1) }
        } else {
            LazyColumn(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(rem(0.5f)),
            ) {
                items(filtered, key = { it.id }) { e ->
                    val expanded = expandedId == e.id
                    val shape = RoundedCornerShape(rem(0.75f))
                    val accent = levelColor(e.level)
                    Column(
                        Modifier.fillMaxWidth()
                            .background(dk(Tw.Slate50, Tw.Slate800.a(0.7f)), shape)
                            .border(cssBorder(), dk(Tw.Slate200.a(0.8f), Tw.Slate800), shape)
                            .clickable(remember { MutableInteractionSource() }, null) { expandedId = if (expanded) null else e.id }
                            .padding(rem(0.75f)),
                        verticalArrangement = Arrangement.spacedBy(rem(0.375f)),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(rem(0.5f))) {
                            Box(
                                Modifier.size(rem(1.5f)).background(accent.a(0.15f), RoundedCornerShape(rem(0.5f))),
                                contentAlignment = Alignment.Center,
                            ) { LucideIcon(levelIcon(e.level), 0.875f, accent) }
                            Column(Modifier.weight(1f)) {
                                TwText(e.message, 0.75f, 1f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = if (expanded) 4 else 2, overflow = TextOverflow.Ellipsis)
                                Row(horizontalArrangement = Arrangement.spacedBy(rem(0.375f))) {
                                    TwText(e.jalali, 0.625f, 0.875f, Tw.Slate400, maxLines = 1)
                                    TwText("• " + e.category.title, 0.625f, 0.875f, Tw.Slate400, maxLines = 1)
                                }
                            }
                            LucideIcon(
                                if (expanded) R.drawable.ic_lucide_chevron_up else R.drawable.ic_lucide_chevron_down,
                                0.875f, Tw.Slate400,
                            )
                        }
                        if (expanded && !e.details.isNullOrEmpty()) {
                            TwText(e.details, 0.6875f, 1.15f, dk(Tw.Slate600, Tw.Slate300), modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            message = "همه لاگ‌ها پاک شوند؟",
            onConfirm = { AppLog.clear(); confirmClear = false },
            onCancel = { confirmClear = false },
            confirmLabel = "پاک شود",
            variant = ConfirmVariant.DANGER,
        )
    }
}

private fun levelColor(level: AppLog.Level): Color = when (level) {
    AppLog.Level.INFO -> dk(Tw.Sky600, Tw.Sky400)
    AppLog.Level.SUCCESS -> dk(Tw.Emerald600, Tw.Emerald400)
    AppLog.Level.WARNING -> dk(Tw.Amber600, Tw.Amber400)
    AppLog.Level.ERROR -> dk(Tw.Rose600, Tw.Rose400)
}

private fun levelIcon(level: AppLog.Level): Int = when (level) {
    AppLog.Level.INFO -> R.drawable.ic_lucide_info
    AppLog.Level.SUCCESS -> R.drawable.ic_lucide_circle_check
    AppLog.Level.WARNING -> R.drawable.ic_lucide_triangle_alert
    AppLog.Level.ERROR -> R.drawable.ic_lucide_circle_x
}

@Composable
private fun StatCard(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(rem(1f))
    Column(
        modifier.background(dk(Tw.Slate50, Tw.Slate800.a(0.6f)), shape)
            .border(cssBorder(), dk(Tw.Slate200.a(0.7f), Tw.Slate700.a(0.6f)), shape)
            .padding(vertical = rem(0.75f)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TwText(com.myaccounting.app.domain.JalaliDate.toPersianDigits(value.toString()), 1.125f, 1.4f, color, weight = FW.Black, maxLines = 1)
        TwText(label, 0.6875f, 1f, Tw.Slate500, maxLines = 1)
    }
}

@Composable
private fun TwTextButton(
    text: String,
    bg: Color,
    fg: Color,
    modifier: Modifier = Modifier,
    border: Color? = null,
    icon: Int? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(rem(0.75f))
    Row(
        modifier.cssShadow(TwShadow.xs(), rem(0.75f))
            .background(bg, shape)
            .then(if (border != null) Modifier.border(cssBorder(), border, shape) else Modifier)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = rem(0.875f), vertical = rem(0.5f)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { LucideIcon(it, 0.875f, fg) }
        TwText(text, 0.75f, 1f, fg, weight = FW.Bold, maxLines = 1)
    }
}
