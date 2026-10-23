package com.myaccounting.app.ui.depts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.*
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.theme.*
import com.myaccounting.app.domain.*
@Composable
fun DeptsScreen(state: com.myaccounting.app.viewmodel.MainViewModel.UiState, vm: com.myaccounting.app.viewmodel.MainViewModel) {
    if (!state.loaded) return
    val db = state.shownDb
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = rem(1f), end = rem(1f), top = rem(1f), bottom = rem(4f)), verticalArrangement = Arrangement.spacedBy(rem(1.25f))) {
        item(key = "h") { Column(verticalArrangement = Arrangement.spacedBy(rem(0.75f))) { TwText("وام‌ها و اقساط", 1.1f, 1.65f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold); Row(horizontalArrangement = Arrangement.spacedBy(rem(0.75f))) { SummaryCard("کل بدهی", "۱۲,۵۰۰,۰۰۰ م.ت", Tw.Rose600, Tw.Rose50, R.drawable.ic_lucide_layers); SummaryCard("مانده پرداخت", "۴,۸۰۰,۰۰۰ م.ت", Tw.Sky600, Tw.Sky50, R.drawable.ic_lucide_circle_dollar_sign) } } }
        if (db.depts.isNotEmpty()) { for (d in db.depts) { item(key = d.uid) { DeptsRow(d) } } }
    }
}
@Composable private fun DeptsRow(d: Dept) { val s = RoundedCornerShape(rem(0.75f)); Row(Modifier.fillMaxWidth().background(dk(Tw.White, Tw.Slate900), s).border(cssBorder(), dk(Tw.Slate200.a(0.7f), Tw.Slate800), s).padding(rem(0.875f)), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(rem(2.5f)).background(dk(Tw.Indigo100, Tw.Indigo950), RoundedCornerShape(rem(0.5f))), contentAlignment = Alignment.Center) { LucideIcon(R.drawable.ic_lucide_layers, 1f, Tw.Indigo600) }; Column(Modifier.weight(1f)) { TwText(d.title, 0.875f, 1.25f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold); TwText("ت - ${d.paidValue ?: 0} ت پرداخت‌شده", 0.625f, 1f, Tw.Slate400) } } }
@Composable private fun SummaryCard(l: String, v: String, a: Color, bg: Color, i: Int) { val s = RoundedCornerShape(rem(1f)); Row(Modifier.fillMaxWidth().background(bg, s).border(cssBorder(), dk(Tw.Slate200.a(0.4f), Tw.Slate800.a(0.4f)), s).padding(rem(0.875f)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { TwText(l, 0.75f, 1f, dk(Tw.Slate700, Tw.Slate300)); TwText(v, 1.1f, 1.5f, a, weight = FW.Black) }; Box(Modifier.size(rem(2.5f)).background(dk(Tw.Slate200, Tw.Slate700), RoundedCornerShape(rem(0.5f))), contentAlignment = Alignment.Center) { LucideIcon(i, 1f, a) } } }
