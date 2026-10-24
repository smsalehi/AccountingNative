package com.myaccounting.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myaccounting.app.R
import com.myaccounting.app.data.AppPrefs
import com.myaccounting.app.data.ThemeMode
import com.myaccounting.app.domain.Currency
import com.myaccounting.app.domain.CurrencyUnit
import com.myaccounting.app.domain.ExchangeRates
import com.myaccounting.app.domain.JalaliDate
import com.myaccounting.app.domain.Project
import com.myaccounting.app.domain.Subject
import com.myaccounting.app.ui.components.ConfirmDialog
import com.myaccounting.app.ui.components.ConfirmVariant
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.components.TwModal
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.Vazirmatn
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem
import com.myaccounting.app.ui.theme.remSp
import com.myaccounting.app.viewmodel.MainViewModel
import java.time.LocalDate
import java.util.UUID

private enum class SettingsTab { GENERAL, CATEGORIES, PROJECTS, LOGS }

private data class AskConfirm(val message: String, val label: String, val danger: Boolean = true, val action: () -> Unit)

/** Port of SettingsView.tsx (header + 4 tabs); the general tab keeps the web's display/backup controls. */
@Composable
fun SettingsScreen(
    vm: MainViewModel,
    state: MainViewModel.UiState,
    themeMode: ThemeMode,
    isDark: Boolean = themeMode == ThemeMode.DARK,
) {
    var tab by rememberSaveable { mutableStateOf(SettingsTab.GENERAL) }
    var pendingImport by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingRateUnit by remember { mutableStateOf<ExchangeRates.RateFileUnit?>(null) }
    var confirm by remember { mutableStateOf<AskConfirm?>(null) }

    val context = LocalContext.current
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "—"
    }
    val fontScale by vm.fontScale.collectAsStateWithLifecycle()

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImport = uri
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportBackup(uri)
    }
    val rateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val unit = pendingRateUnit
        if (uri != null && unit != null) vm.importRates(uri, unit)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = rem(1f), end = rem(1f), top = rem(1f), bottom = rem(4f)),
        verticalArrangement = Arrangement.spacedBy(rem(1.5f)),
    ) {
        item(key = "header") {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(rem(0.75f)),
            ) {
                Box(
                    Modifier.size(rem(2.5f))
                        .background(dk(Tw.Sky100, Tw.Sky950.a(0.6f)), RoundedCornerShape(rem(1f))),
                    contentAlignment = Alignment.Center,
                ) { LucideIcon(R.drawable.ic_lucide_settings, 1.25f, dk(Tw.Sky600, Tw.Sky400)) }
                Column(Modifier.weight(1f)) {
                    TwText("تنظیمات و مدیریت برنامه", 1.125f, 1.75f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1)
                    TwText("واحد پول، تم، نسخه پشتیبان، سرفصل‌ها و لاگ‌ها", 0.75f, 1.25f, dk(Tw.Slate500, Tw.Slate400), maxLines = 2)
                }
            }
        }

        item(key = "tabs") {
            Row(
                Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .background(dk(Tw.Slate100, Tw.Slate800), RoundedCornerShape(rem(1f)))
                    .border(cssBorder(), dk(Tw.Slate200.a(0.7f), Tw.Slate700), RoundedCornerShape(rem(1f)))
                    .padding(rem(0.25f)),
                horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
            ) {
                TabButton("عمومی و پشتیبان", R.drawable.ic_lucide_folder_kanban, tab == SettingsTab.GENERAL) { tab = SettingsTab.GENERAL }
                TabButton("سرفصل‌ها و دسته‌ها", R.drawable.ic_lucide_folder_tree, tab == SettingsTab.CATEGORIES) { tab = SettingsTab.CATEGORIES }
                TabButton("پروژه‌ها", R.drawable.ic_lucide_folder_kanban, tab == SettingsTab.PROJECTS) { tab = SettingsTab.PROJECTS }
                TabButton("لاگ‌ها و خطاها", R.drawable.ic_lucide_file_text, tab == SettingsTab.LOGS) { tab = SettingsTab.LOGS }
            }
        }

        when (tab) {
            SettingsTab.GENERAL -> {
                item(key = "backup") {
                    Panel {
                        SectionTitle("پشتیبان‌گیری و بازیابی", R.drawable.ic_lucide_folder_open, dk(Tw.Emerald600, Tw.Emerald400))
                        TwText(
                            "بکاپ به‌صورت فایل JSON ذخیره می‌شود؛ فایل‌های ‎.json، ‎.json.gz و بکاپ‌های قدیمی base64 قابل بازیابی‌اند. اطلاعات فقط روی همین دستگاه می‌ماند.",
                            0.6875f, 1.5f, dk(Tw.Slate500, Tw.Slate400),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f))) {
                            PrimaryBtn("ذخیره بکاپ (JSON)", R.drawable.ic_lucide_save, enabled = state.loaded) {
                                exportLauncher.launch("accounting_backup_${LocalDate.now()}.json")
                            }
                            GhostBtn("ایمپورت بکاپ", R.drawable.ic_lucide_folder_open, enabled = state.loaded) {
                                importLauncher.launch(arrayOf("*/*"))
                            }
                        }
                    }
                }

                item(key = "display") {
                    Panel {
                        SectionTitle("تنظیمات نمایش و تم", R.drawable.ic_lucide_settings, dk(Tw.Sky600, Tw.Sky400))

                        // Currency — تومان / ریال / دلار (دلار only when daily rates exist)
                        SettingRow("واحد پول پیش‌فرض", "نمایش مبالغ بر اساس تومان، ریال یا دلار در تمام بخش‌ها (دلار فقط نمایشی است)") {
                            Row(
                                Modifier.background(dk(Tw.Slate100, Tw.Slate800), RoundedCornerShape(rem(0.75f))).padding(rem(0.25f)),
                                horizontalArrangement = Arrangement.spacedBy(rem(0.125f)),
                            ) {
                                CurrencyUnit.entries.forEach { u ->
                                    val enabled = u != CurrencyUnit.USD || state.rateIndex != null
                                    SegBtn(
                                        label = Currency.unitLabel(u),
                                        active = state.unit == u,
                                        enabled = enabled,
                                    ) { vm.setUnit(u) }
                                }
                            }
                        }

                        SettingRow("اندازه فونت", "بزرگ‌نمایی متن‌ها بین ۸۰ تا ۱۵۰ درصد") {
                            FontScaleControl(value = fontScale, onChange = { vm.setFontScale(it) })
                        }

                        SettingRow("تم تاریک و روشن", "بهینه‌سازی برای استفاده در شب یا روز") {
                            Row(
                                Modifier.background(dk(Tw.Slate100, Tw.Slate800), RoundedCornerShape(rem(0.75f)))
                                    .border(cssBorder(), dk(Tw.Slate200, Tw.Slate700), RoundedCornerShape(rem(0.75f)))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                        vm.toggleTheme(isDark)
                                    }
                                    .padding(horizontal = rem(0.75f), vertical = rem(0.375f)),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
                            ) {
                                if (isDark) {
                                    LucideIcon(R.drawable.ic_lucide_sun, 1f, Tw.Amber400)
                                    TwText("حالت روز", 0.75f, 1f, dk(Tw.Slate700, Tw.Slate300), weight = FW.Semibold, maxLines = 1)
                                } else {
                                    LucideIcon(R.drawable.ic_lucide_moon, 1f, Tw.Slate600)
                                    TwText("حالت شب", 0.75f, 1f, dk(Tw.Slate700, Tw.Slate300), weight = FW.Semibold, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                item(key = "rates") {
                    Panel {
                        SectionTitle("نرخ روزانه دلار", R.drawable.ic_lucide_trending_up, dk(Tw.Indigo600, Tw.Indigo400))
                        val idx = state.rateIndex
                        TwText(
                            if (idx == null) "هنوز نرخی وارد نشده است؛ برای نمایش دلاری ابتدا فایل نرخ را ایمپورت کنید."
                            else "${JalaliDate.toPersianDigits(idx.count)} روز • از ${JalaliDate.toPersianDigits(idx.firstDate)} تا ${JalaliDate.toPersianDigits(idx.lastDate)} • آخرین نرخ: ${Currency.formatCurrency(idx.lastRate)}",
                            0.6875f, 1.5f, dk(Tw.Slate500, Tw.Slate400),
                        )
                        TwText(
                            "فقط روزهای جدید اضافه می‌شود؛ روزهای خالی بین دو نرخ با درون‌یابی محاسبه می‌شود.",
                            0.6875f, 1.5f, Tw.Slate400,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f))) {
                            GhostBtn("ورود فایل (ریال)", R.drawable.ic_lucide_file_spreadsheet, enabled = state.loaded) {
                                pendingRateUnit = ExchangeRates.RateFileUnit.RIAL
                                rateLauncher.launch(arrayOf("*/*"))
                            }
                            GhostBtn("ورود فایل (تومان)", R.drawable.ic_lucide_file_spreadsheet, enabled = state.loaded) {
                                pendingRateUnit = ExchangeRates.RateFileUnit.TOMAN
                                rateLauncher.launch(arrayOf("*/*"))
                            }
                        }
                    }
                }

                item(key = "danger") {
                    val shape = RoundedCornerShape(rem(1f))
                    Row(
                        Modifier.fillMaxWidth()
                            .background(dk(Tw.Rose50.a(0.6f), Tw.Rose950.a(0.2f)), shape)
                            .border(cssBorder(), dk(Tw.Rose200, Tw.Rose900.a(0.5f)), shape)
                            .padding(rem(1f)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(rem(0.75f)),
                    ) {
                        Column(Modifier.weight(1f)) {
                            TwText("پاک‌سازی کامل داده‌ها", 0.75f, 1.25f, dk(Tw.Rose800, Tw.Rose300), weight = FW.Bold)
                            TwText(
                                "حذف همه حساب‌ها، تراکنش‌ها و سرفصل‌ها و بازگشت به حالت خالی (نرخ‌های دلار حفظ می‌شود)",
                                0.6875f, 1.5f, dk(Tw.Rose600, Tw.Rose400),
                            )
                        }
                        Row(
                            Modifier.background(Tw.Rose600, RoundedCornerShape(rem(0.75f)))
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    confirm = AskConfirm(
                                        "آیا مطمئنید که می‌خواهید همه داده‌ها پاک شود و برنامه به حالت خالی برگردد؟",
                                        "پاک شود",
                                    ) { vm.resetData() }
                                }
                                .padding(horizontal = rem(0.75f), vertical = rem(0.375f)),
                        ) { TwText("پاک‌سازی", 0.75f, 1f, Tw.White, weight = FW.Semibold, maxLines = 1) }
                    }
                }

                item(key = "version") {
                    val shape = RoundedCornerShape(rem(1f))
                    Column(
                        Modifier.fillMaxWidth()
                            .background(dk(Tw.Slate50, Tw.Slate800.a(0.5f)), shape)
                            .border(cssBorder(), dk(Tw.Slate200.a(0.6f), Tw.Slate700.a(0.5f)), shape)
                            .padding(rem(1f)),
                        verticalArrangement = Arrangement.spacedBy(rem(0.5f)),
                    ) {
                        VersionRow("ورژن برنامه", versionName, dk(Tw.Slate600, Tw.Slate300))
                        VersionRow("نوع ذخیره‌سازی", "حافظه داخلی برنامه", dk(Tw.Emerald600, Tw.Emerald400))
                        VersionRow("وضعیت دیتابیس", if (state.loaded) "بارگذاری شده" else "در حال بارگذاری…", dk(Tw.Slate600, Tw.Slate300))
                    }
                }
            }

            SettingsTab.CATEGORIES -> item(key = "categories") { CategoriesTab(state, vm) }
            SettingsTab.PROJECTS -> item(key = "projects") { ProjectsTab(state, vm) }
            SettingsTab.LOGS -> item(key = "logs") {
                Panel {
                    SectionTitle("مرکز لاگ برنامه", R.drawable.ic_lucide_file_text, dk(Tw.Sky600, Tw.Sky400))
                    TwText(
                        "در نسخه نیتیو، رویدادها و خطاها در حافظه دستگاه ذخیره می‌شوند و این صفحه هنوز به لاگ‌های داخلی وصل نشده است.",
                        0.75f, 1.5f, dk(Tw.Slate500, Tw.Slate400),
                    )
                }
            }
        }
    }

    pendingImport?.let { uri ->
        ConfirmDialog(
            message = "با ایمپورت این فایل، کل اطلاعات فعلی برنامه جایگزین می‌شود. ادامه می‌دهید؟",
            onConfirm = { pendingImport = null; vm.importBackup(uri) },
            onCancel = { pendingImport = null },
            confirmLabel = "جایگزین کن",
            variant = ConfirmVariant.WARNING,
        )
    }

    confirm?.let { c ->
        ConfirmDialog(
            message = c.message,
            onConfirm = { c.action(); confirm = null },
            onCancel = { confirm = null },
            confirmLabel = c.label,
            variant = if (c.danger) ConfirmVariant.DANGER else ConfirmVariant.WARNING,
        )
    }
}

@Composable
private fun TabButton(label: String, icon: Int, active: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.background(if (active) dk(Tw.White, Tw.Slate900) else Color.Transparent, RoundedCornerShape(rem(0.75f)))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = rem(0.875f), vertical = rem(0.5f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
    ) {
        LucideIcon(icon, 0.875f, if (active) dk(Tw.Sky600, Tw.Sky400) else dk(Tw.Slate600, Tw.Slate400))
        TwText(
            label, 0.75f, 1f,
            if (active) dk(Tw.Sky600, Tw.Sky400) else dk(Tw.Slate600, Tw.Slate400),
            weight = FW.Bold, maxLines = 1,
        )
    }
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(rem(1f))
    Column(
        Modifier.fillMaxWidth()
            .background(dk(Tw.White, Tw.Slate900), shape)
            .border(cssBorder(), dk(Tw.Slate200.a(0.8f), Tw.Slate800), shape)
            .padding(rem(1.25f)),
        verticalArrangement = Arrangement.spacedBy(rem(1f)),
        content = content,
    )
}

@Composable
private fun SectionTitle(title: String, icon: Int, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(rem(0.5f))) {
        LucideIcon(icon, 1f, tint)
        TwText(title, 0.875f, 1.25f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1)
    }
}

@Composable
private fun SettingRow(label: String, desc: String, control: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(rem(0.5f))) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(rem(0.75f)),
        ) {
            Column(Modifier.weight(1f)) {
                TwText(label, 0.75f, 1.25f, dk(Tw.Slate800, Tw.Slate200), weight = FW.Bold, maxLines = 1)
                TwText(desc, 0.6875f, 1.5f, Tw.Slate400, maxLines = 2)
            }
            control()
        }
        Box(Modifier.fillMaxWidth().padding(top = rem(0.5f)).height(cssBorder()).background(dk(Tw.Slate100, Tw.Slate800)))
    }
}

@Composable
private fun VersionRow(label: String, value: String, valueColor: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TwText(label, 0.6875f, 1f, Tw.Slate400, maxLines = 1)
        TwText(value, 0.75f, 1f, valueColor, weight = FW.Bold, maxLines = 1)
    }
}

@Composable
private fun SegBtn(label: String, active: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.background(if (active) dk(Tw.White, Tw.Slate700) else Color.Transparent, RoundedCornerShape(rem(0.5f)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = rem(0.75f), vertical = rem(0.375f)),
        contentAlignment = Alignment.Center,
    ) {
        TwText(
            label, 0.75f, 1f,
            when {
                !enabled -> Tw.Slate400.a(0.4f)
                active -> dk(Tw.Sky600, Tw.Sky300)
                else -> dk(Tw.Slate500, Tw.Slate400)
            },
            weight = FW.Semibold, maxLines = 1,
        )
    }
}

@Composable
private fun PrimaryBtn(label: String, icon: Int, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        Modifier.background(if (enabled) Tw.Sky600 else Tw.Slate300, RoundedCornerShape(rem(0.75f)))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = rem(0.75f), vertical = rem(0.5f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
    ) {
        LucideIcon(icon, 0.875f, Tw.White)
        TwText(label, 0.75f, 1f, Tw.White, weight = FW.Bold, maxLines = 1)
    }
}

@Composable
private fun GhostBtn(label: String, icon: Int, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(rem(0.75f))
    Row(
        Modifier.background(Color.Transparent, shape)
            .border(cssBorder(), dk(Tw.Slate200, Tw.Slate700), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = rem(0.75f), vertical = rem(0.5f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
    ) {
        LucideIcon(icon, 0.875f, if (enabled) dk(Tw.Slate600, Tw.Slate300) else Tw.Slate400.a(0.4f))
        TwText(
            label, 0.75f, 1f,
            if (enabled) dk(Tw.Slate700, Tw.Slate200) else Tw.Slate400.a(0.4f),
            weight = FW.Semibold, maxLines = 1,
        )
    }
}

@Composable
private fun TwInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    onDone: () -> Unit = {},
) {
    val shape = RoundedCornerShape(rem(0.75f))
    Box(
        modifier.fillMaxWidth()
            .background(dk(Tw.Slate50, Tw.Slate800), shape)
            .border(cssBorder(), dk(Tw.Slate200, Tw.Slate700), shape)
            .padding(horizontal = rem(0.75f), vertical = rem(0.5f)),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = Vazirmatn,
                fontSize = remSp(0.75f),
                color = dk(Tw.Slate900, Tw.Slate100),
                textDirection = TextDirection.Rtl,
            ),
            cursorBrush = SolidColor(Tw.Sky500),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) TwText(placeholder, 0.75f, 1.25f, Tw.Slate400, maxLines = 1)
                    inner()
                }
            },
        )
    }
}

@Composable
private fun FontScaleControl(value: Int, onChange: (Int) -> Unit) {
    val focus = LocalFocusManager.current
    var draft by remember(value) { mutableStateOf(value.toString()) }
    fun commit() {
        val parsed = JalaliDate.toEnglishDigits(draft.trim()).toIntOrNull()
        if (parsed == null) {
            draft = value.toString()
            return
        }
        val clamped = parsed.coerceIn(AppPrefs.MIN_FONT_SCALE, AppPrefs.MAX_FONT_SCALE)
        draft = clamped.toString()
        onChange(clamped)
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(rem(0.375f))) {
            StepButton("−", enabled = value > AppPrefs.MIN_FONT_SCALE) { onChange((value - 1).coerceIn(AppPrefs.MIN_FONT_SCALE, AppPrefs.MAX_FONT_SCALE)) }
            Box(
                Modifier.size(width = rem(3.5f), height = rem(2f))
                    .background(dk(Tw.Slate50, Tw.Slate800), RoundedCornerShape(rem(0.5f)))
                    .border(cssBorder(), dk(Tw.Slate200, Tw.Slate700), RoundedCornerShape(rem(0.5f)))
                    .padding(horizontal = rem(0.25f)),
                contentAlignment = Alignment.Center,
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = Vazirmatn,
                        fontSize = remSp(0.75f),
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                        color = dk(Tw.Slate800, Tw.Slate100),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        textDirection = TextDirection.Ltr,
                    ),
                    cursorBrush = SolidColor(Tw.Sky500),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commit(); focus.clearFocus() }),
                    modifier = Modifier.fillMaxWidth().onFocusChanged { if (!it.isFocused) commit() },
                )
            }
            TwText("٪", 0.6875f, 1f, Tw.Slate400, weight = FW.Bold, maxLines = 1)
            StepButton("+", enabled = value < AppPrefs.MAX_FONT_SCALE) { onChange((value + 1).coerceIn(AppPrefs.MIN_FONT_SCALE, AppPrefs.MAX_FONT_SCALE)) }
        }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(rem(0.5f))
    Box(
        Modifier.size(rem(2f))
            .background(dk(Tw.Slate100, Tw.Slate800), shape)
            .border(cssBorder(), dk(Tw.Slate200, Tw.Slate700), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        TwText(label, 0.875f, 1f, if (enabled) dk(Tw.Slate700, Tw.Slate200) else Tw.Slate400.a(0.4f), weight = FW.Black, maxLines = 1, direction = TextDirection.Ltr)
    }
}

@Composable
private fun CategoriesTab(state: MainViewModel.UiState, vm: MainViewModel) {
    val db = state.shownDb
    var type by rememberSaveable { mutableStateOf(1) }
    var newTitle by rememberSaveable { mutableStateOf("") }
    var renaming by remember { mutableStateOf<Subject?>(null) }
    var deleting by remember { mutableStateOf<Subject?>(null) }

    val roots = remember(db.subjects, type) {
        db.subjects.filter { it.isCost == type && it.parentUid.isNullOrEmpty() }
    }
    val expenseCount = db.subjects.count { it.isCost == 1 }
    val incomeCount = db.subjects.count { it.isCost == 0 }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(rem(1f))) {
        Column {
            TwText("سرفصل‌ها و دسته‌بندی‌های مالی", 1.25f, 1.75f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1)
            TwText(
                "امکان تعریف سرفصل جدید، حذف، تغییر نام و پنهان‌کردن دسته‌ها",
                0.75f, 1.25f, dk(Tw.Slate500, Tw.Slate400), maxLines = 2,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f)), verticalAlignment = Alignment.CenterVertically) {
            TwInput(
                value = newTitle,
                onValueChange = { newTitle = it },
                placeholder = "عنوان سرفصل جدید...",
                modifier = Modifier.weight(1f),
                onDone = {
                    val t = newTitle.trim()
                    if (t.isNotEmpty()) {
                        vm.updateSubject(Subject(uid = UUID.randomUUID().toString(), title = t, isCost = type))
                        newTitle = ""
                    }
                },
            )
            PrimaryBtn("افزودن", R.drawable.ic_lucide_plus, enabled = newTitle.trim().isNotEmpty()) {
                val t = newTitle.trim()
                if (t.isNotEmpty()) {
                    vm.updateSubject(Subject(uid = UUID.randomUUID().toString(), title = t, isCost = type))
                    newTitle = ""
                }
            }
        }

        Row(
            Modifier.background(dk(Tw.Slate100, Tw.Slate800), RoundedCornerShape(rem(0.75f))).padding(rem(0.25f)),
            horizontalArrangement = Arrangement.spacedBy(rem(0.125f)),
        ) {
            Box(
                Modifier.background(if (type == 1) dk(Tw.White, Tw.Slate700) else Color.Transparent, RoundedCornerShape(rem(0.5f)))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { type = 1 }
                    .padding(horizontal = rem(0.75f), vertical = rem(0.375f)),
            ) {
                TwText(
                    "هزینه (" + JalaliDate.toPersianDigits(expenseCount) + ")",
                    0.75f, 1f,
                    if (type == 1) dk(Tw.Rose600, Tw.Rose400) else dk(Tw.Slate500, Tw.Slate400),
                    weight = FW.Semibold, maxLines = 1,
                )
            }
            Box(
                Modifier.background(if (type == 0) dk(Tw.White, Tw.Slate700) else Color.Transparent, RoundedCornerShape(rem(0.5f)))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { type = 0 }
                    .padding(horizontal = rem(0.75f), vertical = rem(0.375f)),
            ) {
                TwText(
                    "درآمد (" + JalaliDate.toPersianDigits(incomeCount) + ")",
                    0.75f, 1f,
                    if (type == 0) dk(Tw.Emerald600, Tw.Emerald400) else dk(Tw.Slate500, Tw.Slate400),
                    weight = FW.Semibold, maxLines = 1,
                )
            }
        }

        if (roots.isEmpty()) {
            TwText("سرفصلی در این دسته ثبت نشده است.", 0.75f, 1.5f, Tw.Slate400)
        }

        for (root in roots) {
            val children = db.subjects.filter { it.parentUid == root.uid }
            val cardShape = RoundedCornerShape(rem(1f))
            Column(
                Modifier.fillMaxWidth()
                    .background(dk(Tw.White, Tw.Slate900), cardShape)
                    .border(cssBorder(), dk(Tw.Slate200.a(0.8f), Tw.Slate800), cardShape)
                    .padding(rem(1f)),
                verticalArrangement = Arrangement.spacedBy(rem(0.75f)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(rem(0.5f))) {
                    Box(
                        Modifier.size(rem(2.25f))
                            .background(
                                if (root.isCost == 1) dk(Tw.Rose100, Tw.Rose950.a(0.6f)) else dk(Tw.Emerald100, Tw.Emerald950.a(0.6f)),
                                RoundedCornerShape(rem(0.5f)),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        LucideIcon(
                            if (root.isCost == 1) R.drawable.ic_lucide_trending_up else R.drawable.ic_lucide_trending_up,
                            1f,
                            if (root.isCost == 1) dk(Tw.Rose600, Tw.Rose400) else dk(Tw.Emerald600, Tw.Emerald400),
                        )
                    }
                    TwText(
                        root.title, 0.875f, 1.25f,
                        if (root.isHidden == 1) Tw.Slate400 else dk(Tw.Slate800, Tw.Slate100),
                        weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    RowIconBtn(R.drawable.ic_lucide_pencil, dk(Tw.Slate400, Tw.Slate500)) { renaming = root }
                    RowIconBtn(
                        if (root.isHidden == 1) R.drawable.ic_lucide_eye_off else R.drawable.ic_lucide_eye,
                        if (root.isHidden == 1) dk(Tw.Amber600, Tw.Amber400) else dk(Tw.Slate400, Tw.Slate500),
                    ) { vm.updateSubject(root.copy(isHidden = if (root.isHidden == 1) 0 else 1)) }
                    RowIconBtn(R.drawable.ic_lucide_trash2, dk(Tw.Rose600, Tw.Rose400)) { deleting = root }
                }
                if (children.isNotEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().padding(start = rem(0.75f)),
                        verticalArrangement = Arrangement.spacedBy(rem(0.375f)),
                    ) {
                        for (child in children) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
                            ) {
                                Box(Modifier.size(rem(0.25f)).background(dk(Tw.Slate300, Tw.Slate600), RoundedCornerShape(rem(999f))))
                                TwText(child.title, 0.75f, 1.25f, dk(Tw.Slate600, Tw.Slate300), maxLines = 1, modifier = Modifier.weight(1f))
                                RowIconBtn(R.drawable.ic_lucide_pencil, Tw.Slate400) { renaming = child }
                                RowIconBtn(R.drawable.ic_lucide_trash2, dk(Tw.Rose600, Tw.Rose400)) { deleting = child }
                            }
                        }
                    }
                }
            }
        }
    }

    renaming?.let { subject ->
        PromptDialog(
            title = "تغییر نام سرفصل",
            initial = subject.title,
            confirmLabel = "ذخیره",
            onDismiss = { renaming = null },
        ) { newName ->
            vm.updateSubject(subject.copy(title = newName))
            renaming = null
        }
    }

    deleting?.let { subject ->
        ConfirmDialog(
            message = "سرفصل «${subject.title}» حذف شود؟",
            onConfirm = { vm.deleteSubject(subject.uid); deleting = null },
            onCancel = { deleting = null },
            confirmLabel = "حذف شود",
        )
    }
}

@Composable
private fun RowIconBtn(icon: Int, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(rem(1.75f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { LucideIcon(icon, 0.875f, tint) }
}

@Composable
private fun ProjectsTab(state: MainViewModel.UiState, vm: MainViewModel) {
    val db = state.shownDb
    var newTitle by rememberSaveable { mutableStateOf("") }
    var renaming by remember { mutableStateOf<Project?>(null) }
    var deleting by remember { mutableStateOf<Project?>(null) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(rem(1f))) {
        Column {
            TwText("مدیریت پروژه‌ها", 1.25f, 1.75f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1)
            TwText("حذف، تغییر نام و افزودن پروژه‌ها", 0.75f, 1.25f, dk(Tw.Slate500, Tw.Slate400))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f)), verticalAlignment = Alignment.CenterVertically) {
            TwInput(
                value = newTitle,
                onValueChange = { newTitle = it },
                placeholder = "نام پروژه جدید...",
                modifier = Modifier.weight(1f),
                onDone = {
                    val t = newTitle.trim()
                    if (t.isNotEmpty()) {
                        vm.addProject(Project(uid = UUID.randomUUID().toString(), title = t))
                        newTitle = ""
                    }
                },
            )
            PrimaryBtn("افزودن", R.drawable.ic_lucide_plus, enabled = newTitle.trim().isNotEmpty()) {
                val t = newTitle.trim()
                if (t.isNotEmpty()) {
                    vm.addProject(Project(uid = UUID.randomUUID().toString(), title = t))
                    newTitle = ""
                }
            }
        }
        if (db.projects.isEmpty()) TwText("هنوز پروژه‌ای ثبت نشده است.", 0.75f, 1.5f, Tw.Slate400)
        for (p in db.projects) {
            val shape = RoundedCornerShape(rem(1f))
            Row(
                Modifier.fillMaxWidth()
                    .background(dk(Tw.White, Tw.Slate900), shape)
                    .border(cssBorder(), dk(Tw.Slate200.a(0.8f), Tw.Slate800), shape)
                    .padding(horizontal = rem(1f), vertical = rem(0.75f)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
            ) {
                LucideIcon(R.drawable.ic_lucide_folder_kanban, 1f, dk(Tw.Sky600, Tw.Sky400))
                TwText(p.title, 0.875f, 1.25f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Semibold, maxLines = 1, modifier = Modifier.weight(1f))
                RowIconBtn(R.drawable.ic_lucide_pencil, Tw.Slate400) { renaming = p }
                RowIconBtn(R.drawable.ic_lucide_trash2, dk(Tw.Rose600, Tw.Rose400)) { deleting = p }
            }
        }
    }

    renaming?.let { project ->
        PromptDialog(
            title = "تغییر نام پروژه",
            initial = project.title,
            confirmLabel = "ذخیره",
            onDismiss = { renaming = null },
        ) { newName ->
            vm.renameProject(project.uid, newName)
            renaming = null
        }
    }

    deleting?.let { project ->
        ConfirmDialog(
            message = "پروژه «${project.title}» حذف شود؟",
            onConfirm = { vm.deleteProject(project.uid); deleting = null },
            onCancel = { deleting = null },
            confirmLabel = "حذف شود",
        )
    }
}

@Composable
private fun PromptDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    TwModal(onDismiss = onDismiss, maxWidthRem = 24f, fillHeight = false, scrimColor = Tw.Slate950.a(0.6f)) {
        Column(Modifier.fillMaxWidth().padding(rem(1f)), verticalArrangement = Arrangement.spacedBy(rem(1f))) {
            TwText(title, 1f, 1.5f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1)
            TwInput(value, { value = it }, "نام...", onDone = { if (value.trim().isNotEmpty()) onConfirm(value.trim()) })
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f)), verticalAlignment = Alignment.CenterVertically) {
                PrimaryBtn(confirmLabel, R.drawable.ic_lucide_check, enabled = value.trim().isNotEmpty()) { onConfirm(value.trim()) }
                GhostBtn("انصراف", R.drawable.ic_lucide_x) { onDismiss() }
            }
        }
    }
}
