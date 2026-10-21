package com.myaccounting.app.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.myaccounting.app.R
import com.myaccounting.app.domain.IranianBank
import com.myaccounting.app.domain.IranianBanks
import com.myaccounting.app.ui.components.BankLogo
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.components.TwCheckBubble
import com.myaccounting.app.ui.components.TwIconButton
import com.myaccounting.app.ui.components.TwModal
import com.myaccounting.app.ui.components.TwShadow
import com.myaccounting.app.ui.components.cssShadow
import com.myaccounting.app.ui.components.TwTextField
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem

/** Port of components/BankPickerModal.tsx. */
@Composable
fun BankPickerDialog(
    selectedBankId: String?,
    onSelect: (IranianBank) -> Unit,
    onDismiss: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("all") }

    val filtered = IranianBanks.all.filter { bank ->
        val inCategory = category == "all" || bank.category == category
        if (!inCategory) return@filter false
        val q = search.trim().lowercase()
        if (q.isEmpty()) return@filter true
        bank.name.lowercase().contains(q) || bank.shortName.lowercase().contains(q) ||
            bank.keywords.any { it.lowercase().contains(q) } || bank.cardPrefixes.any { it.contains(q) }
    }

    val categories = listOf(
        Triple("all", "همه بانک‌ها", R.drawable.ic_lucide_building2),
        Triple("neobank", "نئوبانک‌ها و کارت‌های مدرن", R.drawable.ic_lucide_smartphone),
        Triple("state", "بانک‌های بزرگ دولتی", R.drawable.ic_lucide_shield_check),
        Triple("private", "بانک‌های خصوصی", R.drawable.ic_lucide_sparkles),
        Triple("gharzolhasane", "قرض‌الحسنه", R.drawable.ic_lucide_wallet),
        Triple("institute", "موسسات اعتباری", R.drawable.ic_lucide_building2),
    )

    TwModal(onDismiss = onDismiss, maxWidthRem = 42f, maxHeightFraction = 0.88f) {
        // header
        Row(
            Modifier.fillMaxWidth().padding(rem(1f)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                TwText("انتخاب بانک و لوگوی حساب", 1f, 1.5f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Black)
                TwText(
                    "لوگوی تمام بانک‌ها و نئوبانک‌های فعال ایران را جستجو یا انتخاب کنید",
                    0.75f, 1f, dk(Tw.Slate500, Tw.Slate400),
                    modifier = Modifier.padding(top = rem(0.125f)),
                )
            }
            TwIconButton(R.drawable.ic_lucide_x, onDismiss, Tw.Slate400)
        }
        Box(Modifier.fillMaxWidth().height(cssBorder()).background(dk(Tw.Slate100, Tw.Slate800)))

        // search + categories
        Column(
            Modifier.fillMaxWidth().background(dk(Tw.Slate50.a(0.5f), Tw.Slate800.a(0.3f))).padding(rem(1f)),
            verticalArrangement = Arrangement.spacedBy(rem(0.75f)),
        ) {
            Box(Modifier.fillMaxWidth()) {
                TwTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = "جستجوی نام بانک، مثلاً ملی، بلو، رسالت، ۶۰۳۷۹۹...",
                    startPadRem = 2.5f,
                    endPadRem = if (search.isNotEmpty()) 3.5f else 1f,
                )
                LucideIcon(
                    R.drawable.ic_lucide_search, 1f, Tw.Slate400,
                    Modifier.align(Alignment.CenterStart).padding(start = rem(0.875f)),
                )
                if (search.isNotEmpty()) {
                    TwText(
                        "پاک کردن", 0.75f, 1f, Tw.Slate400,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = rem(0.75f))
                            .clickable(remember { MutableInteractionSource() }, null) { search = "" },
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
            ) {
                for ((id, label, icon) in categories) {
                    val active = category == id
                    val shape = RoundedCornerShape(rem(0.75f))
                    Row(
                        Modifier
                            .then(if (active) Modifier.cssShadow(TwShadow.xs(), rem(0.75f)) else Modifier)
                            .background(if (active) Tw.Sky600 else dk(Tw.White, Tw.Slate800), shape)
                            .then(if (!active) Modifier.border(cssBorder(), dk(Tw.Slate200, Tw.Slate700), shape) else Modifier)
                            .clickable(remember { MutableInteractionSource() }, null) { category = id }
                            .padding(horizontal = rem(0.75f), vertical = rem(0.375f)),
                        horizontalArrangement = Arrangement.spacedBy(rem(0.25f)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LucideIcon(icon, 0.875f, if (active) Tw.White else dk(Tw.Slate600, Tw.Slate300))
                        TwText(
                            label, 0.75f, 1f, if (active) Tw.White else dk(Tw.Slate600, Tw.Slate300),
                            weight = FW.Medium, maxLines = 1,
                        )
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(cssBorder()).background(dk(Tw.Slate100, Tw.Slate800)))

        // grid
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f).padding(rem(3f)), contentAlignment = Alignment.Center) {
                TwText("بانکی با این مشخصات پیدا نشد.", 0.75f, 1f, Tw.Slate400)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(rem(1f)),
                horizontalArrangement = Arrangement.spacedBy(rem(0.625f)),
                verticalArrangement = Arrangement.spacedBy(rem(0.625f)),
            ) {
                items(filtered, key = { it.id }) { bank ->
                    val selected = selectedBankId == bank.id
                    val shape = RoundedCornerShape(rem(1f))
                    Row(
                        Modifier
                            .then(if (selected) Modifier.cssShadow(TwShadow.xs(), rem(1f)) else Modifier)
                            .background(if (selected) dk(Tw.Sky50.a(0.7f), Tw.Sky950.a(0.4f)) else dk(Tw.White, Tw.Slate900), shape)
                            .border(
                                if (selected) cssBorder() * 2f else cssBorder(),
                                if (selected) Tw.Sky500 else dk(Tw.Slate200.a(0.8f), Tw.Slate800),
                                shape,
                            )
                            .clickable(remember { MutableInteractionSource() }, null) {
                                onSelect(bank)
                                onDismiss()
                            }
                            .padding(rem(0.75f)),
                        horizontalArrangement = Arrangement.spacedBy(rem(0.75f)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BankLogo(bank.name, bank.id)
                        Column(Modifier.weight(1f)) {
                            TwText(
                                bank.shortName, 0.75f, 1f, dk(Tw.Slate800, Tw.Slate100),
                                weight = FW.Bold, maxLines = 1,
                            )
                            TwText(
                                bank.name, 0.6875f, 1.03125f, Tw.Slate400, maxLines = 1,
                                modifier = Modifier.padding(top = rem(0.125f)),
                            )
                        }
                        if (selected) TwCheckBubble()
                    }
                }
            }
        }

        // footer
        Box(Modifier.fillMaxWidth().height(cssBorder()).background(dk(Tw.Slate100, Tw.Slate800)))
        Row(
            Modifier.fillMaxWidth().background(dk(Tw.Slate50.a(0.5f), Tw.Slate800.a(0.3f))).padding(rem(0.875f)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TwText(
                "تعداد بانک‌های موجود: ${com.myaccounting.app.domain.JalaliDate.toPersianDigits(IranianBanks.all.size)} بانک و موسسه",
                0.75f, 1f, dk(Tw.Slate500, Tw.Slate400),
            )
            Row(
                Modifier
                    .background(dk(Tw.White, Tw.Slate800), RoundedCornerShape(rem(0.75f)))
                    .border(cssBorder(), dk(Tw.Slate200, Tw.Slate700), RoundedCornerShape(rem(0.75f)))
                    .clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss)
                    .padding(horizontal = rem(1f), vertical = rem(0.375f)),
            ) {
                TwText("بستن", 0.75f, 1f, dk(Tw.Slate700, Tw.Slate200), weight = FW.Medium)
            }
        }
    }
}
