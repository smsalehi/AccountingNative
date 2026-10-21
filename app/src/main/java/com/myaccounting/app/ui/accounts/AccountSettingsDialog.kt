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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.myaccounting.app.R
import com.myaccounting.app.domain.Account
import com.myaccounting.app.domain.AccountingDatabase
import com.myaccounting.app.domain.Currency
import com.myaccounting.app.domain.CurrencyUnit
import com.myaccounting.app.domain.IranianBanks
import com.myaccounting.app.domain.JalaliDate
import com.myaccounting.app.ui.components.AmountInput
import com.myaccounting.app.ui.components.BankLogo
import com.myaccounting.app.ui.components.ConfirmDialog
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.components.TwButton
import com.myaccounting.app.ui.components.TwIconButton
import com.myaccounting.app.ui.components.TwLabel
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
import com.myaccounting.app.ui.theme.toBottomRightBrush
import kotlinx.coroutines.delay

/**
 * Port of components/AccountSettingsModal.tsx: the virtual card preview, the quick actions and the
 * full edit form (title, bank, group, dormant toggle, balances, card/account/branch, notes).
 * The parent decides whether a save is an insert or an update (same UID).
 */
@Composable
fun AccountSettingsDialog(
    account: Account,
    db: AccountingDatabase,
    unit: CurrencyUnit,
    balance: Double,
    txCount: Int,
    onClose: () -> Unit,
    onSave: (Account) -> Unit,
    onDelete: (String) -> Unit,
    onViewTransactions: () -> Unit,
    onQuickTransaction: (Int) -> Unit,
) {
    var title by remember(account.uid) { mutableStateOf(account.title) }
    var groupUid by remember(account.uid) {
        mutableStateOf(account.groupUid.ifEmpty { db.accountGroups.firstOrNull()?.uid ?: "" })
    }
    var bankUid by remember(account.uid) { mutableStateOf(account.bankUid) }
    var branch by remember(account.uid) { mutableStateOf(account.bankBranchName ?: "") }
    var cardNumber by remember(account.uid) { mutableStateOf(account.cardNumber ?: "") }
    var accountNumber by remember(account.uid) { mutableStateOf(account.bankAccountNumber ?: "") }
    var oldBalance by remember(account.uid) { mutableStateOf(account.oldBalance) }
    var minBalance by remember(account.uid) { mutableStateOf(account.minimumBalanceAlert) }
    var hidden by remember(account.uid) { mutableStateOf(account.isHidden == 1) }
    var desc by remember(account.uid) { mutableStateOf(account.desc ?: "") }
    var errorMsg by remember(account.uid) { mutableStateOf("") }
    var bankPicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    var groupMenu by remember { mutableStateOf(false) }

    val clipboard = LocalClipboardManager.current
    LaunchedEffect(copied) {
        if (copied) { delay(2000); copied = false }
    }

    val currentBank = (bankUid?.let { IranianBanks.byId[it] })
        ?: IranianBanks.findByCardNumber(cardNumber)
        ?: IranianBanks.findByQuery(title)

    fun onCardChange(raw: String) {
        val digits = JalaliDate.toEnglishDigits(raw).filter { it in '0'..'9' }.take(16)
        cardNumber = digits
        if (digits.length >= 6 && bankUid == null) {
            val detected = IranianBanks.findByCardNumber(digits)
            if (detected != null) {
                bankUid = detected.id
                if (title.isBlank() || title == "حساب جدید") title = "حساب ${detected.shortName}"
            }
        }
    }

    fun save() {
        val t = title.trim()
        if (t.isEmpty()) {
            errorMsg = "لطفاً نام حساب را وارد نمایید."
            return
        }
        onSave(
            account.copy(
                title = t,
                groupUid = groupUid,
                bankUid = bankUid,
                bankBranchName = branch.trim().ifEmpty { null },
                cardNumber = cardNumber.trim().ifEmpty { null },
                bankAccountNumber = accountNumber.trim().ifEmpty { null },
                oldBalance = oldBalance,
                minimumBalanceAlert = minBalance,
                isHidden = if (hidden) 1 else 0,
                desc = desc.trim().ifEmpty { null },
            ),
        )
    }

    val bw = cssBorder()
    val cardShape = RoundedCornerShape(rem(1f))
    val groupTitle = db.accountGroups.firstOrNull { it.uid == groupUid }?.title ?: "بدون گروه"
    // Hoisted out of drawBehind (rem() is composable).
    val circleRadius = rem(6f)
    val circleInset = rem(3f)

    TwModal(onDismiss = onClose, maxWidthRem = 42f, maxHeightFraction = 0.92f) {
        // ---- header -------------------------------------------------------------------------------------
        Row(
            Modifier.fillMaxWidth().background(dk(Tw.Slate50.a(0.5f), Tw.Slate800.a(0.4f))).padding(rem(1f)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(rem(0.625f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(rem(2.5f)).background(dk(Tw.Sky100, Tw.Sky950.a(0.7f)), RoundedCornerShape(rem(0.75f))),
                    contentAlignment = Alignment.Center,
                ) { LucideIcon(R.drawable.ic_lucide_credit_card, 1.25f, dk(Tw.Sky600, Tw.Sky400)) }
                Column(Modifier.weight(1f)) {
                    TwText("تنظیمات و جزئیات حساب", 1f, 1.5f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Black)
                    TwText(
                        "ویرایش نام، بانک، شماره کارت، شعبه، موجودی اولیه و وضعیت حساب",
                        0.75f, 1f, dk(Tw.Slate500, Tw.Slate400),
                        modifier = Modifier.padding(top = rem(0.125f)),
                    )
                }
            }
            TwIconButton(R.drawable.ic_lucide_x, onClose, Tw.Slate400)
        }
        Box(Modifier.fillMaxWidth().height(bw).background(dk(Tw.Slate100, Tw.Slate800)))

        // ---- body ---------------------------------------------------------------------------------------
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(rem(1f)),
            verticalArrangement = Arrangement.spacedBy(rem(1.5f)),
        ) {
            // virtual card
            Box(
                Modifier.fillMaxWidth().clip(cardShape)
                    .drawBehind {
                        drawRect(toBottomRightBrush(size, listOf(Tw.Slate800, Tw.Slate900, Tw.Zinc950)))
                        val r = circleRadius.toPx()
                        val inset = circleInset.toPx()
                        val bottomCenter = Offset(size.width - inset, size.height - inset)
                        drawCircle(
                            Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.05f), Color.Transparent),
                                center = bottomCenter,
                                radius = r,
                            ),
                            radius = r,
                            center = bottomCenter,
                        )
                        val topCenter = Offset(inset, inset)
                        drawCircle(
                            Brush.radialGradient(
                                listOf(Tw.Sky500.a(0.10f), Color.Transparent),
                                center = topCenter,
                                radius = r,
                            ),
                            radius = r,
                            center = topCenter,
                        )
                    }
                    .border(bw, Tw.Slate700.a(0.5f), cardShape)
                    .padding(rem(1.25f)),
            ) {
                Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(rem(0.75f)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BankLogo(currentBank?.name ?: title, bankUid)
                            Column {
                                TwText(title.ifBlank { "عنوان حساب" }, 0.875f, 1.25f, Tw.White, weight = FW.Bold, maxLines = 1)
                                TwText(
                                    currentBank?.name ?: "بانک عضو شبکه شتاب", 0.6875f, 1.03125f, Tw.Slate300,
                                    maxLines = 1,
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // EMV chip
                            Column(
                                Modifier.size(width = rem(2.25f), height = rem(1.75f))
                                    .background(Tw.Amber400.a(0.8f), RoundedCornerShape(rem(0.375f)))
                                    .border(bw, Tw.Amber300.a(0.5f), RoundedCornerShape(rem(0.375f)))
                                    .padding(rem(0.25f)),
                                verticalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                repeat(3) {
                                    Box(Modifier.fillMaxWidth().height(rem(0.125f)).background(Tw.Amber600.a(0.4f), CircleShape))
                                }
                            }
                            LucideIcon(R.drawable.ic_lucide_contactless, 1.25f, Tw.Slate300.a(0.8f))
                        }
                    }

                    // card number
                    val display = if (cardNumber.isEmpty()) "••••  ••••  ••••  ••••" else cardNumber.chunked(4).joinToString("   ")
                    TwText(
                        display, 1f, 1.5f, Tw.White.a(0.95f), weight = FW.Bold, family = FontFamily.Monospace,
                        trackingEm = 0.25f, direction = TextDirection.Ltr, align = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = rem(1.5f)),
                    )
                    if (cardNumber.isNotEmpty()) {
                        Row(
                            Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, null) {
                                clipboard.setText(AnnotatedString(cardNumber))
                                copied = true
                            },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (copied) {
                                LucideIcon(R.drawable.ic_lucide_check, 0.75f, Tw.Emerald400)
                                TwText("کپی شد!", 0.6875f, 1.03125f, Tw.Emerald400, modifier = Modifier.padding(start = rem(0.25f)))
                            } else {
                                LucideIcon(R.drawable.ic_lucide_copy, 0.75f, Tw.Sky300)
                                TwText("کپی شماره کارت", 0.6875f, 1.03125f, Tw.Sky300, modifier = Modifier.padding(start = rem(0.25f)))
                            }
                        }
                    }

                    // bottom row
                    Box(Modifier.fillMaxWidth().padding(top = rem(0.5f))) {
                        Box(Modifier.fillMaxWidth().height(bw).background(Tw.White.a(0.1f)))
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Column {
                            TwText("شماره حساب:", 0.625f, 0.9375f, Tw.Slate400)
                            TwText(
                                if (accountNumber.isNotEmpty()) JalaliDate.toPersianDigits(accountNumber) else "ثبت نشده",
                                0.75f, 1f, Tw.White.a(0.9f), weight = FW.Medium, family = FontFamily.Monospace,
                            )
                            if (branch.isNotEmpty()) {
                                TwText(
                                    "شعبه: $branch", 0.625f, 0.9375f, Tw.Slate300,
                                    modifier = Modifier.padding(top = rem(0.125f)),
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            TwText("موجودی فعلی:", 0.625f, 0.9375f, Tw.Slate400)
                            TwText(Currency.formatCurrency(balance, unit), 0.875f, 1.25f, Tw.Emerald400, weight = FW.Black)
                        }
                    }
                }
            }

            // quick actions
            Column(verticalArrangement = Arrangement.spacedBy(rem(0.5f))) {
                Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f))) {
                    QuickAction(
                        "گردش تراکنش‌ها (${JalaliDate.toPersianDigits(txCount)})", R.drawable.ic_lucide_file_text,
                        dk(Tw.Slate100, Tw.Slate800), dk(Tw.Slate700, Tw.Slate200),
                        dk(Tw.Slate200, Tw.Slate700), Modifier.weight(1f),
                    ) { onViewTransactions(); onClose() }
                    QuickAction(
                        "ثبت واریز", R.drawable.ic_lucide_arrow_down_left,
                        dk(Tw.Emerald50, Tw.Emerald950.a(0.4f)), dk(Tw.Emerald700, Tw.Emerald300),
                        dk(Tw.Emerald200, Tw.Emerald800), Modifier.weight(1f),
                    ) { onQuickTransaction(2); onClose() }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f))) {
                    QuickAction(
                        "ثبت برداشت", R.drawable.ic_lucide_arrow_up_right,
                        dk(Tw.Rose50, Tw.Rose950.a(0.4f)), dk(Tw.Rose700, Tw.Rose300),
                        dk(Tw.Rose200, Tw.Rose800), Modifier.weight(1f),
                    ) { onQuickTransaction(1); onClose() }
                    Spacer(Modifier.weight(1f))
                }
            }

            if (errorMsg.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().background(dk(Tw.Rose50, Tw.Rose950.a(0.6f)), RoundedCornerShape(rem(0.75f)))
                        .border(bw, dk(Tw.Rose200, Tw.Rose800), RoundedCornerShape(rem(0.75f)))
                        .padding(rem(0.75f)),
                    horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LucideIcon(R.drawable.ic_lucide_alert_circle, 1f, dk(Tw.Rose700, Tw.Rose300))
                    TwText(errorMsg, 0.75f, 1f, dk(Tw.Rose700, Tw.Rose300))
                }
            }

            // form
            Column(verticalArrangement = Arrangement.spacedBy(rem(0.875f))) {
                Column {
                    TwLabel("نام حساب", required = true)
                    TwTextField(
                        title, { title = it },
                        placeholder = "مثلاً: کارت حقوق ملت، جاری سپه...",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                    )
                }
                Column {
                    TwLabel("لوگو و بانک حساب")
                    Row(
                        Modifier.fillMaxWidth()
                            .background(dk(Tw.White, Tw.Slate900), RoundedCornerShape(rem(0.75f)))
                            .border(bw, dk(Tw.Slate200, Tw.Slate700), RoundedCornerShape(rem(0.75f)))
                            .clickable(remember { MutableInteractionSource() }, null) { bankPicker = true }
                            .padding(horizontal = rem(0.875f), vertical = rem(0.5f)),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BankLogo(currentBank?.name ?: title, bankUid, sizeRem = 1.5f, padRem = 0.125f)
                            TwText(
                                currentBank?.name ?: "انتخاب لوگو و بانک...", 0.75f, 1f,
                                dk(Tw.Slate800, Tw.Slate100), maxLines = 1,
                            )
                        }
                        TwText("تغییر بانک", 0.75f, 1f, dk(Tw.Sky600, Tw.Sky400), weight = FW.Bold, maxLines = 1)
                    }
                }
                Column {
                    TwLabel("گروه حساب")
                    Box {
                        Row(
                            Modifier.fillMaxWidth()
                                .background(dk(Tw.White, Tw.Slate900), RoundedCornerShape(rem(0.75f)))
                                .border(bw, dk(Tw.Slate200, Tw.Slate700), RoundedCornerShape(rem(0.75f)))
                                .clickable(remember { MutableInteractionSource() }, null) { groupMenu = true }
                                .padding(horizontal = rem(0.875f), vertical = rem(0.625f)),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TwText(groupTitle, 0.75f, 1f, dk(Tw.Slate800, Tw.Slate100), maxLines = 1)
                            LucideIcon(R.drawable.ic_lucide_chevron_down, 1f, dk(Tw.Slate500, Tw.Slate400))
                        }
                        DropdownMenu(expanded = groupMenu, onDismissRequest = { groupMenu = false }) {
                            for (g in db.accountGroups) {
                                DropdownMenuItem(
                                    text = { TwText(g.title, 0.75f, 1.25f, dk(Tw.Slate800, Tw.Slate100)) },
                                    onClick = { groupUid = g.uid; groupMenu = false },
                                )
                            }
                        }
                    }
                }
                // dormant toggle
                Row(
                    Modifier.fillMaxWidth().background(dk(Tw.Slate50.a(0.7f), Tw.Slate800.a(0.4f)), RoundedCornerShape(rem(0.75f)))
                        .border(bw, dk(Tw.Slate200, Tw.Slate700), RoundedCornerShape(rem(0.75f)))
                        .clickable(remember { MutableInteractionSource() }, null) { hidden = !hidden }
                        .padding(rem(0.625f)),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LucideIcon(
                            if (hidden) R.drawable.ic_lucide_eye_off else R.drawable.ic_lucide_eye, 1f,
                            if (hidden) Tw.Amber500 else Tw.Emerald500,
                        )
                        TwText("وضعیت راکد بودن حساب", 0.75f, 1f, dk(Tw.Slate700, Tw.Slate200), weight = FW.Semibold)
                    }
                    Box(
                        Modifier.size(rem(1f)).background(
                            if (hidden) Tw.Sky600 else dk(Tw.White, Tw.Slate900),
                            RoundedCornerShape(rem(0.25f)),
                        ).border(bw, dk(Tw.Slate300, Tw.Slate600), RoundedCornerShape(rem(0.25f))),
                        contentAlignment = Alignment.Center,
                    ) { if (hidden) LucideIcon(R.drawable.ic_lucide_check, 0.75f, Tw.White) }
                }

                AmountInput(
                    value = oldBalance, onChange = { oldBalance = it }, unit = unit,
                    label = "موجودی اولیه حساب", placeholder = "۰", allowZero = true,
                    helperText = "مبنای محاسبه است؛ موجودی فعلی = موجودی اولیه + گردش تراکنش‌ها.",
                )
                AmountInput(
                    value = minBalance, onChange = { minBalance = it }, unit = unit,
                    label = "هشدار حداقل موجودی (اختیاری)", placeholder = "۰", allowZero = true,
                    helperText = "در صورت رسیدن موجودی به زیر این حد، هشدار نمایش داده می‌شود.",
                )

                Column {
                    TwLabel("شماره کارت (۱۶ رقم)")
                    TwTextField(
                        cardNumber, { onCardChange(it) }, placeholder = "۶۰۳۷۹۹...", mono = true, ltr = true,
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    )
                }
                Column {
                    TwLabel("شماره حساب")
                    TwTextField(
                        accountNumber, { accountNumber = JalaliDate.toEnglishDigits(it) },
                        placeholder = "مثلاً: ۰۲۱۸...", mono = true, ltr = true,
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    )
                }
                Column {
                    TwLabel("نام شعبه")
                    TwTextField(branch, { branch = it }, placeholder = "مثلاً: شعبه مرکزی")
                }
                Column {
                    TwLabel("توضیحات و یادداشت حساب")
                    TwTextField(
                        desc, { desc = it },
                        placeholder = "یادداشت‌های دلخواه، شماره شبا (IR...)، کاربرد حساب یا اطلاعات تکمیلی...",
                        singleLine = false, minHeightRem = 3.5f, vPadRem = 0.5f,
                    )
                }
            }
        }

        // ---- footer -------------------------------------------------------------------------------------
        Box(Modifier.fillMaxWidth().height(bw).background(dk(Tw.Slate100, Tw.Slate800)))
        Row(
            Modifier.fillMaxWidth().background(dk(Tw.Slate50.a(0.5f), Tw.Slate800.a(0.4f))).padding(rem(1f)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TwButton(
                "حذف حساب", { confirmDelete = true },
                bg = dk(Tw.Rose50, Tw.Rose950.a(0.4f)), fg = dk(Tw.Rose600, Tw.Rose400),
                borderColor = dk(Tw.Rose200, Tw.Rose800), icon = R.drawable.ic_lucide_trash2, iconRem = 0.875f,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.5f)), verticalAlignment = Alignment.CenterVertically) {
                TwButton(
                    "انصراف", onClose,
                    bg = dk(Tw.White, Tw.Slate900), fg = dk(Tw.Slate700, Tw.Slate300),
                    borderColor = dk(Tw.Slate200, Tw.Slate700), hPadRem = 1f,
                )
                TwButton(
                    "ذخیره تغییرات", { save() },
                    bg = Tw.Sky600, fg = Tw.White, icon = R.drawable.ic_lucide_save, iconRem = 1f,
                    hPadRem = 1.25f, weight = FW.Bold, shadow = true,
                )
            }
        }

        if (bankPicker) {
            BankPickerDialog(
                selectedBankId = bankUid,
                onSelect = { bank ->
                    bankUid = bank.id
                    if (title.isBlank() || title == "حساب جدید") title = "حساب ${bank.shortName}"
                },
                onDismiss = { bankPicker = false },
            )
        }
        if (confirmDelete) {
            val message = if (txCount > 0) {
                "این حساب دارای ${JalaliDate.toPersianDigits(txCount)} تراکنش ثبت‌شده است. با حذف حساب، ممکن است تراز تراکنش‌ها نامعتبر شود. آیا همچنان از حذف اطمینان دارید؟"
            } else {
                "آیا از حذف حساب «${account.title}» اطمینان دارید؟"
            }
            ConfirmDialog(
                message = message,
                confirmLabel = "حذف شود",
                onConfirm = { confirmDelete = false; onDelete(account.uid); onClose() },
                onCancel = { confirmDelete = false },
            )
        }
    }
}

@Composable
private fun QuickAction(
    text: String,
    icon: Int,
    bg: Color,
    fg: Color,
    border: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .background(bg, RoundedCornerShape(rem(0.75f)))
            .border(cssBorder(), border, RoundedCornerShape(rem(0.75f)))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(rem(0.625f)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(icon, 0.875f, fg)
        TwText(text, 0.75f, 1f, fg, weight = FW.Semibold, maxLines = 1, modifier = Modifier.padding(start = rem(0.375f)))
    }
}
