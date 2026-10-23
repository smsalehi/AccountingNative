package com.myaccounting.app.ui.settings
import com.myaccounting.app.R
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.*
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.theme.*
@Composable
fun SettingsScreen(vm: com.myaccounting.app.viewmodel.MainViewModel, state: com.myaccounting.app.viewmodel.MainViewModel.UiState, themeMode: Int) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = rem(1f), end = rem(1f), top = rem(1f), bottom = rem(4f)), verticalArrangement = Arrangement.spacedBy(rem(1.25f))) {
        item(key = "h") { TwText("تنظیمات", 1.25f, 1.75f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold) }
        item(key = "theme") { SettingsRow("تم", "تاریک / روشن", R.drawable.ic_lucide_moon, Tw.Sky600) }
        item(key = "font") { SettingsRow("مقیاس فونت", "۱۰۰٪ (مرجع)", R.drawable.ic_lucide_type, Tw.Amber600) }
        item(key = "backup") { SettingsRow("پشتیبان", "دریافت / بازیابی", R.drawable.ic_lucide_database, Tw.Emerald600) }
    }
}
@Composable private fun SettingsRow(title: String, desc: String, icon: Int, tint: Color) {
    val s = RoundedCornerShape(rem(0.75f))
    Row(Modifier.fillMaxWidth().background(dk(Tw.White, Tw.Slate900), s).border(cssBorder(), dk(Tw.Slate200.a(0.7f), Tw.Slate800), s).padding(horizontal = rem(0.875f), vertical = rem(0.75f)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(rem(0.625f))) {
        Box(Modifier.size(rem(2.5f)).background(dk(Tw.Slate100, Tw.Slate800), RoundedCornerShape(rem(0.5f))), contentAlignment = Alignment.Center) { LucideIcon(icon, 1f, tint) }
        Column(Modifier.weight(1f)) { TwText(title, 0.875f, 1.25f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold); TwText(desc, 0.625f, 1f, Tw.Slate400) }
    }
}
