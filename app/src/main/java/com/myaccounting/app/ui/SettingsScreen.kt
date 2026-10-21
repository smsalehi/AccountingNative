package com.myaccounting.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myaccounting.app.data.ThemeMode
import com.myaccounting.app.domain.Currency
import com.myaccounting.app.domain.CurrencyUnit
import com.myaccounting.app.domain.ExchangeRates
import com.myaccounting.app.domain.JalaliDate
import com.myaccounting.app.viewmodel.MainViewModel
import java.time.LocalDate

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel, state: MainViewModel.UiState, theme: ThemeMode) {
    var pendingImport by remember { mutableStateOf<android.net.Uri?>(null) }
    var rateUnit by remember { mutableStateOf(ExchangeRates.RateFileUnit.RIAL) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) pendingImport = uri }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if (uri != null) vm.exportBackup(uri) }
    val rateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.importRates(uri, rateUnit) }

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("جایگزینی اطلاعات") },
            text = { Text("با ایمپورت این فایل، کل اطلاعات فعلی برنامه جایگزین می‌شود. ادامه می‌دهید؟") },
            confirmButton = { TextButton(onClick = { pendingImport = null; vm.importBackup(uri) }) { Text("جایگزین کن") } },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("انصراف") } },
        )
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Section("پشتیبان‌گیری") {
            Text("فرمت‌های قابل ایمپورت: ‎.json، ‎.json.gz و بکاپ‌های قدیمی base64. اطلاعات فقط روی همین دستگاه ذخیره می‌شود.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { importLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("ایمپورت بکاپ") }
            OutlinedButton(
                onClick = { exportLauncher.launch("accounting_backup_${LocalDate.now()}.json") },
                enabled = state.loaded,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("ذخیره بکاپ (JSON)") }
        }

        Section("واحد پول") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CurrencyUnit.values().forEach { u ->
                    FilterChip(selected = state.unit == u, onClick = { vm.setUnit(u) }, label = { Text(Currency.unitLabel(u)) })
                }
            }
            Text("نمای دلاری فقط برای نمایش است و با نرخ روز هر تراکنش محاسبه می‌شود.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Section("نرخ روزانه دلار") {
            val idx = state.rateIndex
            Text(
                if (idx == null) "هنوز نرخی وارد نشده است."
                else "${JalaliDate.toPersianDigits(idx.count)} روز • از ${idx.firstDate} تا ${idx.lastDate} • آخرین نرخ: ${Currency.formatCurrency(idx.lastRate)}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("فقط روزهای جدید اضافه می‌شود؛ روزهای خالیِ بین دو نرخ به‌صورت میانگین/درون‌یابی محاسبه می‌شود.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { rateUnit = ExchangeRates.RateFileUnit.RIAL; rateLauncher.launch(arrayOf("*/*")) }, enabled = state.loaded) { Text("ورود فایل (ریال)") }
                OutlinedButton(onClick = { rateUnit = ExchangeRates.RateFileUnit.TOMAN; rateLauncher.launch(arrayOf("*/*")) }, enabled = state.loaded) { Text("ورود فایل (تومان)") }
            }
        }

        Section("اندازه متن و عناصر") {
            val fontScale by vm.fontScale.collectAsStateWithLifecycle()
            Text("همه اندازه‌ها بر پایه rem هستند؛ مقدار فعلی: ${JalaliDate.toPersianDigits(fontScale)}٪", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(90, 100, 105, 110, 120).forEach { p ->
                    FilterChip(selected = fontScale == p, onClick = { vm.setFontScale(p) }, label = { Text(JalaliDate.toPersianDigits(p)) })
                }
            }
        }

        Section("نمایش") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.values().forEach { m ->
                    FilterChip(selected = theme == m, onClick = { vm.setTheme(m) }, label = { Text(m.label) })
                }
            }
        }
    }
}
