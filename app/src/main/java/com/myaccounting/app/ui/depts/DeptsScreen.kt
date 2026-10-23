package com.myaccounting.app.ui.depts
import androidx.compose.ui.Modifier
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import com.myaccounting.app.ui.theme.*
import com.myaccounting.app.domain.*

@Composable
fun DeptsScreen(s: com.myaccounting.app.viewmodel.MainViewModel.UiState, v: com.myaccounting.app.viewmodel.MainViewModel) {
    if (!s.loaded) return
    val db = s.shownDb
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = rem(1f), end = rem(1f), top = rem(1f), bottom = rem(4f)), verticalArrangement = Arrangement.spacedBy(rem(1.25f))) {
        item(key = "h") { Column(verticalArrangement = Arrangement.spacedBy(rem(0.75f))) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                TwText("وام‌ها و اقساط", 1.1f, 1.65f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1)
                ActionPill("وام جدید", R.drawable.ic_lucide_plus, Tw.Sky600) { v.notPorted("وام جدید") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.75f))) {
                SummaryCard("کل بدهی", "۱۲,۵۰۰,۰۰۰ م.ت", Tw.Rose600, Tw.Rose50, R.drawable.ic_lucide_layers)
                SummaryCard("مانده پرداخت", "۴,۸۰۰,۰۰۰ م.ت", Tw.Sky600, Tw.Sky50, R.drawable.ic_lucide_circle_dollar_sign)
            }
        }}
        for (d in db.depts) { item(key = d.uid) { DeptCard(d) } }
    }
}

@Composable private fun DeptCard(d: Dept) {
    val s = RoundedCornerShape(rem(0.75f))
    Row(Modifier.fillMaxWidth().background(dk(Tw.White, Tw.Slate900), s).border(cssBorder(), dk(Tw.Slate200.a(0.7f), Tw.Slate800), s).padding(rem(0.875f)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(rem(0.625f))) {
        Box(Modifier.size(rem(2.5f)).background(dk(Tw.Indigo100, Tw.Indigo950), RoundedCornerShape(rem(0.5f))), contentAlignment = Alignment.Center) { LucideIcon(R.drawable.ic_lucide_layers, 1f, Tw.Indigo600) }
        Column(Modifier.weight(1f)) {
            TwText(d.title, 0.875f, 1.25f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1)
            TwText("کل: ${fa(d.value.toInt())} ت | قسط: ${fa(d.periodValue.toInt())} ت | دوره: ${d.periodDays} روز", 0.625f, 1f, Tw.Slate400, maxLines = 1)
        }
    }
}
@Composable private fun ActionPill(label: String, icon: Int, tint: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(rem(0.75f))
    Row(Modifier.background(tint, shape).clickable(remember { MutableInteractionSource() }, null, onClick = onClick).padding(horizontal = rem(0.875f), vertical = rem(0.375f)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(rem(0.375f))) {
        LucideIcon(icon, 1f, Tw.White); TwText(label, 0.75f, 1f, Tw.White, weight = FW.Semibold, maxLines = 1)
    }
}
@Composable private fun SummaryCard(label: String, value: String, accent: Color, bg: Color, icon: Int) {
    val shape = RoundedCornerShape(rem(1f))
    Row(Modifier.fillMaxWidth().background(bg, shape).border(cssBorder(), dk(Tw.Slate200.a(0.4f), Tw.Slate800.a(0.4f)), shape).padding(rem(0.875f)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) { TwText(label, 0.75f, 1f, dk(Tw.Slate700, Tw.Slate300), maxLines = 1); TwText(value, 1.1f, 1.5f, accent, weight = FW.Black, maxLines = 1) }
        Box(Modifier.size(rem(2.5f)).background(dk(Tw.Slate200, Tw.Slate700), RoundedCornerShape(rem(0.5f))), contentAlignment = Alignment.Center) { LucideIcon(icon, 1f, accent) }
    }
}
private fun fa(n: Int): String = JalaliDate.toPersianDigits(n)
