package com.myaccounting.app.ui.persons
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.*
import com.myaccounting.app.R
import com.myaccounting.app.domain.*
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.theme.*

@Composable
fun PersonsScreen(s: com.myaccounting.app.viewmodel.MainViewModel.UiState, v: com.myaccounting.app.viewmodel.MainViewModel) {
    if (!s.loaded) return
    val db = s.shownDb
    var search by remember { mutableStateOf("") }
    var showHidden by remember { mutableStateOf(false) }
    var collapsed by remember { mutableStateOf<Set<String>>(emptySet()) }
    val term = search.trim().lowercase()
    val groups = remember(db.personGroups, db.persons, showHidden, term) {
        val g = db.personGroups.map { group ->
            val persons = db.persons.filter {
                (if (showHidden) true else it.isHidden == 0) && (term.isEmpty() || it.fullName.lowercase().contains(term) || (it.desc ?: "").lowercase().contains(term)) && it.groupUid == group.uid
            }
            Triple(group.uid, group.title, persons)
        }.filter { it.third.isNotEmpty() }
        val others = db.persons.filter {
            (if (showHidden) true else it.isHidden == 0) && (term.isEmpty() || it.fullName.lowercase().contains(term) || (it.desc ?: "").lowercase().contains(term)) && it.groupUid.isNullOrEmpty()
        }
        val result = g.map { Triple(it.first, it.second, it.third) }.toMutableList()
        if (others.isNotEmpty()) result.add(Triple("__other", "سایر", others))
        result
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = rem(1f), end = rem(1f), top = rem(1f), bottom = rem(4f)), verticalArrangement = Arrangement.spacedBy(rem(1.25f))) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(rem(0.75f))) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    TwText("اشخاص و طرف‌حساب‌ها", 1.1f, 1.65f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1)
                    Row(horizontalArrangement = Arrangement.spacedBy(rem(0.375f))) {
                        ActionPill("شخص جدید", R.drawable.ic_lucide_plus, Tw.Sky600) { v.notPorted("شخص جدید") }
                        ActionPill("راکد", R.drawable.ic_lucide_eye, Tw.Slate500) { showHidden = !showHidden }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(rem(0.75f))) {
                    SummaryCard("مجموع طلب شما", "۱,۲۵۰,۰۰۰ م.ت", Tw.Emerald600, Tw.Emerald50, R.drawable.ic_lucide_arrow_down_left)
                    SummaryCard("مجموع بدهی شما", "۸۹۰,۰۰۰ م.ت", Tw.Rose600, Tw.Rose50, R.drawable.ic_lucide_arrow_up_right)
                }
            }
        }
        item(key = "search") { SearchField(search) { search = it } }
        for ((gid, gtitle, persons) in groups) {
            item(key = "group-$gid") {
                Column(verticalArrangement = Arrangement.spacedBy(rem(0.625f))) {
                    val isColl = gid in collapsed
                    Row(Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, null) { if (gid != "__other") collapsed = if (isColl) collapsed - gid else collapsed + gid }.padding(horizontal = rem(0.25f), vertical = rem(0.5f)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(rem(0.5f))) {
                            TwText(gtitle, 0.875f, 1.25f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1)
                            TwText("${fa(persons.size)} شخص", 0.625f, 1f, Tw.Slate400, maxLines = 1)
                        }
                        LucideIcon(R.drawable.ic_lucide_chevron_down, 0.875f, Tw.Slate400, Modifier.rotate(if (isColl) 0f else 180f))
                    }
                    if (!isColl) {
                        Column(verticalArrangement = Arrangement.spacedBy(rem(0.375f))) {
                            for (p in persons) { PersonRow(p) }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun SearchField(value: String, onChange: (String) -> Unit) {
    val shape = RoundedCornerShape(rem(0.75f))
    Box(Modifier.fillMaxWidth()) {
        BasicTextField(value = value, onValueChange = onChange, singleLine = true, textStyle = TextStyle(fontFamily = Vazirmatn, fontSize = remSp(0.75f), color = dk(Tw.Slate900, Tw.Slate100), textDirection = TextDirection.Rtl), cursorBrush = SolidColor(Tw.Sky500), decorationBox = { inner -> Box(Modifier.fillMaxWidth().background(dk(Tw.Slate50, Tw.Slate800), shape).border(cssBorder(), dk(Tw.Slate200, Tw.Slate700), shape).padding(start = rem(2.5f), end = rem(0.75f), top = rem(0.5f), bottom = rem(0.5f)), contentAlignment = Alignment.CenterStart) { if (value.isEmpty()) TwText("جستجوی نام یا مشخصات شخص...", 0.75f, 1f, Tw.Slate400, maxLines = 1); inner() } })
    }
}

@Composable private fun ActionPill(label: String, icon: Int, tint: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(rem(0.75f))
    Row(Modifier.background(tint, shape).clickable(remember { MutableInteractionSource() }, null, onClick = onClick).padding(horizontal = rem(0.875f), vertical = rem(0.375f)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(rem(0.375f))) {
        LucideIcon(icon, 1f, Tw.White)
        TwText(label, 0.75f, 1f, Tw.White, weight = FW.Semibold, maxLines = 1)
    }
}

@Composable private fun SummaryCard(label: String, value: String, accent: Color, bg: Color, icon: Int) {
    val shape = RoundedCornerShape(rem(1f))
    Row(Modifier.fillMaxWidth().background(bg, shape).border(cssBorder(), dk(Tw.Slate200.a(0.4f), Tw.Slate800.a(0.4f)), shape).padding(rem(0.875f)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) { TwText(label, 0.75f, 1f, dk(Tw.Slate700, Tw.Slate300), maxLines = 1); TwText(value, 1.1f, 1.5f, accent, weight = FW.Black, maxLines = 1) }
        Box(Modifier.size(rem(2.5f)).background(dk(Tw.Slate200, Tw.Slate700), RoundedCornerShape(rem(0.5f))), contentAlignment = Alignment.Center) { LucideIcon(icon, 1f, accent) }
    }
}

@Composable private fun PersonRow(p: Person) {
    val shape = RoundedCornerShape(rem(0.75f))
    Row(Modifier.fillMaxWidth().background(dk(Tw.White, Tw.Slate900), shape).border(cssBorder(), dk(Tw.Slate200.a(0.7f), Tw.Slate800), shape).padding(horizontal = rem(0.75f), vertical = rem(0.5f)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(rem(0.625f))) {
        Box(Modifier.size(rem(2.5f)).background(dk(Tw.Slate100, Tw.Slate800), RoundedCornerShape(rem(0.5f))), contentAlignment = Alignment.Center) { LucideIcon(R.drawable.ic_lucide_user, 1f, Tw.Slate400) }
        Column(Modifier.weight(1f)) { TwText(p.fullName, 0.875f, 1.25f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1); TwText(p.desc ?: "—", 0.625f, 1f, Tw.Slate400, maxLines = 1) }
    }
}

private fun fa(n: Int): String = JalaliDate.toPersianDigits(n)
