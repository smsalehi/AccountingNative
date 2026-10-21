package com.myaccounting.app.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.myaccounting.app.domain.AccountingDatabase
import com.myaccounting.app.domain.Balance
import com.myaccounting.app.domain.CurrencyUnit
import com.myaccounting.app.domain.JalaliDate
import com.myaccounting.app.domain.Subject
import com.myaccounting.app.domain.Transaction
import com.myaccounting.app.domain.Transactions
import com.myaccounting.app.ui.components.AmountInput
import com.myaccounting.app.ui.components.BankLogo
import com.myaccounting.app.ui.components.CategoryIcon
import com.myaccounting.app.ui.components.CategoryIconSize
import com.myaccounting.app.ui.components.ConfirmDialog
import com.myaccounting.app.ui.components.ConfirmVariant
import com.myaccounting.app.ui.components.HairLine
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.components.PersianDateField
import com.myaccounting.app.ui.components.PersianTimeField
import com.myaccounting.app.ui.components.PickerAvatar
import com.myaccounting.app.ui.components.TwIconButton
import com.myaccounting.app.ui.components.TwModal
import com.myaccounting.app.ui.components.TwSelectField
import com.myaccounting.app.ui.components.TwSelectorField
import com.myaccounting.app.ui.components.TwTextField
import com.myaccounting.app.ui.components.TwText
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.round

/**
 * NewTransactionModal.tsx: the expense / income / transfer form of the app — type tabs, the amount
 * (calculator), category, source & destination accounts, date, time, description and the collapsed
 * «گزینه‌های بیشتر» block (project, person, loan), plus the duplicate and negative-balance confirms.
 */
@Composable
fun NewTransactionDialog(
    db: AccountingDatabase,
    unit: CurrencyUnit,
    onClose: () -> Unit,
    onSave: (Transaction) -> Unit,
    onAddSubject: (Subject) -> Unit = {},
    onError: (String) -> Unit = {},
    initialType: Int = 1,
    initialAccountUid: String? = null,
    initialDeptUid: String? = null,
    initialProjectUid: String? = null,
    initialPersonUid: String? = null,
    editingTransaction: Transaction? = null,
) {
    val editing = editingTransaction != null
    val nowMs = remember { System.currentTimeMillis() }

    // ---- form state (initialised exactly like the web's useEffect on open) -------------------------
    val originalSign = remember { if (editing && (editingTransaction!!.totalValue < 0)) -1 else 1 }
    var type by remember { mutableStateOf(if (editing) editingTransaction!!.transactionType else initialType) }
    var amount by remember {
        mutableStateOf(
            when {
                editing -> abs(round(editingTransaction!!.totalValue))
                initialDeptUid != null ->
                    db.depts.firstOrNull { it.uid == initialDeptUid }?.let { if (it.periodValue > 0) round(it.periodValue) else 0.0 } ?: 0.0
                else -> 0.0
            },
        )
    }
    var subjectUid by remember {
        mutableStateOf(if (editing) editingTransaction!!.subjectUid?.takeIf { it.isNotEmpty() } else null)
    }
    var sourceAccountUid by remember {
        mutableStateOf(
            when {
                editing -> editingTransaction!!.sourceAccountUid ?: db.accounts.firstOrNull()?.uid ?: ""
                initialAccountUid != null && db.accounts.any { it.uid == initialAccountUid } -> initialAccountUid
                else -> ""
            },
        )
    }
    var destAccountUid by remember { mutableStateOf(if (editing) editingTransaction!!.destinationAccountUid ?: "" else "") }
    var persianDate by remember {
        mutableStateOf(
            if (editing) editingTransaction!!.persianDate.ifEmpty { JalaliDate.formatJalaliDate(nowMs) }
            else JalaliDate.formatJalaliDate(nowMs),
        )
    }
    val today = remember { LocalDate.now() }
    var timeStr by remember {
        val t = java.time.LocalTime.now()
        mutableStateOf(
            if (editing) editingTransaction!!.time?.ifEmpty { "12:00" } ?: "12:00"
            else String.format("%02d:%02d", t.hour, t.minute),
        )
    }
    var projectUid by remember { mutableStateOf(if (editing) editingTransaction!!.projectUid ?: "" else initialProjectUid ?: "") }
    var deptUid by remember { mutableStateOf(if (editing) editingTransaction!!.deptUid ?: "" else initialDeptUid ?: "") }
    var personUid by remember { mutableStateOf(if (editing) editingTransaction!!.personUid ?: "" else initialPersonUid ?: "") }
    var desc by remember {
        val loanDept = db.depts.firstOrNull { it.uid == initialDeptUid }
        val person = db.persons.firstOrNull { it.uid == initialPersonUid }
        mutableStateOf(
            when {
                editing -> editingTransaction!!.desc ?: ""
                initialDeptUid != null && loanDept != null && loanDept.periodValue > 0 -> "پرداخت قسط وام: ${loanDept.title}"
                initialDeptUid != null -> "پرداخت قسط وام"
                initialPersonUid != null && person != null -> "تراکنش مربوط به ${person.fullName}"
                else -> ""
            },
        )
    }
    var showOptionalFields by remember { mutableStateOf(false) }

    var categoryOpen by remember { mutableStateOf(false) }
    var sourceOpen by remember { mutableStateOf(false) }
    var destOpen by remember { mutableStateOf(false) }
    var personOpen by remember { mutableStateOf(false) }
    var confirmDuplicate by remember { mutableStateOf(false) }
    var confirmNegative by remember { mutableStateOf(false) }
    var pendingTx by remember { mutableStateOf<Transaction?>(null) }
    var pendingClose by remember { mutableStateOf(true) }

    val currentSubject = db.subjects.firstOrNull { it.uid == subjectUid }
    val currentParentSubject = currentSubject?.parentUid?.let { p -> db.subjects.firstOrNull { it.uid == p } }
    val sourceAccount = db.accounts.firstOrNull { it.uid == sourceAccountUid }
    val destAccount = db.accounts.firstOrNull { it.uid == destAccountUid }
    val selectedPerson = db.persons.firstOrNull { it.uid == personUid }
    val bankTitle = remember(db.banks) { db.banks.associate { it.uid to it.title } }

    fun buildTx(): Transaction {
        val parts = timeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 12
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val totalTomans = round(amount)
        val signed = if (editing) totalTomans * originalSign else totalTomans
        val millis = LocalDate.now().atTime(h, m).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return Transaction(
            uid = editingTransaction?.uid ?: Transactions.newUid(),
            subjectUid = if (type == 3) null else subjectUid,
            transactionType = type,
            totalValue = signed,
            dateTime = millis,
            persianDate = persianDate,
            time = timeStr,
            sourceAccountUid = sourceAccountUid,
            destinationAccountUid = if (type == 3) destAccountUid else null,
            projectUid = projectUid.ifEmpty { null },
            deptUid = deptUid.ifEmpty { null },
            personUid = personUid.ifEmpty { null },
            desc = desc.ifEmpty { Transactions.defaultDescription(type) },
            isHidden = editingTransaction?.isHidden ?: 0,
        )
    }

    /** App.tsx handleSaveTransaction: a negative source balance asks for a confirmation first. */
    fun commit(tx: Transaction, closeAfterSave: Boolean) {
        if (tx.transactionType == 1 || tx.transactionType == 3) {
            val balances = Balance.computeAccountBalances(db)
            val current = balances[tx.sourceAccountUid] ?: 0.0
            if (current - abs(tx.totalValue) < 0) {
                pendingTx = tx
                pendingClose = closeAfterSave
                confirmNegative = true
                return
            }
        }
        onSave(tx)
        if (closeAfterSave) {
            onClose()
        } else if (!editing) {
            amount = 0.0
            desc = ""
        }
    }

    fun submit(closeAfterSave: Boolean) {
        val totalTomans = round(amount)
        if (!(totalTomans > 0)) {
            onError("لطفاً مبلغ معتبری وارد نمایید.")
            return
        }
        if (sourceAccountUid.isEmpty()) {
            onError("لطفاً حساب بانکی را انتخاب کنید.")
            return
        }
        if (type != 3 && subjectUid.isNullOrEmpty() && deptUid.isEmpty() && personUid.isEmpty()) {
            onError("لطفاً یک سرفصل مالی انتخاب کنید.")
            return
        }
        if (!editing && Transactions.isDuplicate(
                db = db,
                editingUid = null,
                totalTomans = totalTomans,
                subjectUid = if (type == 3) null else subjectUid,
                sourceAccountUid = sourceAccountUid,
                persianDate = persianDate,
                personUid = personUid.ifEmpty { null },
                deptUid = deptUid.ifEmpty { null },
            )
        ) {
            pendingTx = buildTx()
            pendingClose = closeAfterSave
            confirmDuplicate = true
            return
        }
        commit(buildTx(), closeAfterSave)
    }

    TwModal(
        onDismiss = onClose,
        maxWidthRem = 32f,             // max-w-lg
        maxHeightFraction = 0.9f,      // max-h-[90vh]
        shapeRem = 1f,                 // rounded-2xl
        scrimColor = Tw.Slate900.a(0.6f),
    ) {
        // ---- header -------------------------------------------------------------------------------------
        Row(
            Modifier.fillMaxWidth().padding(rem(1f)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                TwText(
                    if (editing) "ویرایش تراکنش" else "ثبت تراکنش جدید",
                    1f, 1.5f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1,
                )
                TwText(
                    if (editing) "تغییر و اصلاح مشخصات تراکنش ثبت شده" else "ثبت دقیق هزینه، درآمد یا جابجایی بین حساب‌ها",
                    0.6875f, 1f, Tw.Slate400, maxLines = 1,
                )
            }
            TwIconButton(R.drawable.ic_lucide_x, onClose, Tw.Slate400)
        }
        HairLine(dk(Tw.Slate100, Tw.Slate800))

        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(rem(1.25f)),
            verticalArrangement = Arrangement.spacedBy(rem(1f)),
        ) {
            // ---- type tabs ------------------------------------------------------------------------------
            Row(
                Modifier.fillMaxWidth().background(dk(Tw.Slate100, Tw.Slate800), RoundedCornerShape(rem(0.75f)))
                    .padding(rem(0.25f)),
                horizontalArrangement = Arrangement.spacedBy(rem(0.25f)),
            ) {
                TypeTab("هزینه", R.drawable.ic_lucide_arrow_down_left, Tw.Rose500, type == 1, Modifier.weight(1f)) {
                    type = 1; subjectUid = null
                }
                TypeTab("درآمد", R.drawable.ic_lucide_arrow_up_right, Tw.Emerald500, type == 2, Modifier.weight(1f)) {
                    type = 2; subjectUid = null
                }
                TypeTab("انتقال", R.drawable.ic_lucide_arrow_left_right, Tw.Sky500, type == 3, Modifier.weight(1f)) {
                    type = 3
                }
            }

            // ---- amount ---------------------------------------------------------------------------------
            Column {
                FieldLabel("مبلغ (${if (unit == CurrencyUnit.RIAL) "ریال" else "تومان"}):")
                AmountInput(value = amount, onChange = { amount = it }, unit = unit, placeholder = "۰")
            }

            // ---- subject --------------------------------------------------------------------------------
            if (type != 3) {
                Column {
                    FieldLabel("سرفصل و دسته‌بندی موضوع:")
                    TwSelectorField(
                        text = when {
                            currentSubject == null -> "برای انتخاب کلیک کنید..."
                            currentParentSubject != null -> "${currentParentSubject.title} / ${currentSubject.title}"
                            else -> currentSubject.title
                        },
                        placeholder = currentSubject == null,
                        action = "تغییر سرفصل",
                        onClick = { categoryOpen = true },
                    ) {
                        if (currentSubject != null) {
                            CategoryIcon(currentSubject.title, currentSubject.icon, currentSubject.isCost, CategoryIconSize.SM)
                        } else {
                            Box(
                                Modifier.size(rem(1.75f)).background(dk(Tw.Slate200, Tw.Slate700), RoundedCornerShape(rem(0.5f))),
                                contentAlignment = Alignment.Center,
                            ) { LucideIcon(R.drawable.ic_lucide_folder_open, 0.875f, Tw.Slate500) }
                        }
                    }
                }
            }

            // ---- accounts -------------------------------------------------------------------------------
            Column(verticalArrangement = Arrangement.spacedBy(rem(0.75f))) {
                Column {
                    FieldLabel(if (type == 3) "حساب مبدا:" else "حساب بانکی / کارت:")
                    TwSelectorField(
                        text = sourceAccount?.title?.ifEmpty { "انتخاب شده" } ?: "انتخاب حساب بانکی",
                        placeholder = sourceAccount == null,
                        action = "تغییر",
                        onClick = { sourceOpen = true },
                    ) {
                        if (sourceAccount != null) {
                            BankLogo(bankTitle[sourceAccount.bankUid] ?: sourceAccount.title, sourceAccount.bankUid, sizeRem = 1.5f, padRem = 0.125f)
                        } else {
                            LucideIcon(R.drawable.ic_lucide_credit_card, 1f, Tw.Sky500)
                        }
                    }
                }
                if (type == 3) {
                    Column {
                        FieldLabel("حساب مقصد:")
                        TwSelectorField(
                            text = destAccount?.title?.ifEmpty { "انتخاب شده" } ?: "انتخاب حساب مقصد",
                            placeholder = destAccount == null,
                            action = "تغییر",
                            onClick = { destOpen = true },
                        ) {
                            if (destAccount != null) {
                                BankLogo(bankTitle[destAccount.bankUid] ?: destAccount.title, destAccount.bankUid, sizeRem = 1.5f, padRem = 0.125f)
                            } else {
                                LucideIcon(R.drawable.ic_lucide_credit_card, 1f, Tw.Emerald500)
                            }
                        }
                    }
                }
            }

            // ---- date & time ----------------------------------------------------------------------------
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(rem(0.75f))) {
                Column(Modifier.weight(1f)) {
                    FieldLabel("تاریخ:", icon = R.drawable.ic_lucide_calendar, iconTint = Tw.Slate400)
                    PersianDateField(value = persianDate, onChange = { persianDate = it }, placeholder = "تاریخ")
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel("ساعت:", icon = R.drawable.ic_lucide_clock, iconTint = Tw.Sky500)
                    PersianTimeField(value = timeStr, onChange = { timeStr = it }, placeholder = "ساعت")
                }
            }

            // ---- description ----------------------------------------------------------------------------
            Column {
                FieldLabel("شرح / یادداشت:")
                TwTextFieldPlain(
                    value = desc, onValueChange = { desc = it },
                    placeholder = "مثلاً: خرید سوپرمارکت، واریز فریلنس، اقساط ...",
                )
            }

            // ---- optional fields ------------------------------------------------------------------------
            Column {
                Row(
                    Modifier.clickable(remember { MutableInteractionSource() }, null) { showOptionalFields = !showOptionalFields },
                    horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LucideIcon(
                        if (showOptionalFields) R.drawable.ic_lucide_chevron_up else R.drawable.ic_lucide_chevron_down,
                        0.875f, dk(Tw.Slate500, Tw.Slate400),
                    )
                    TwText(
                        "گزینه‌های بیشتر (پروژه، طرف حساب، وام)", 0.6875f, 1f,
                        dk(Tw.Slate500, Tw.Slate400), weight = FW.Semibold, maxLines = 1,
                    )
                }
                if (showOptionalFields) {
                    Column(
                        Modifier.fillMaxWidth().padding(top = rem(0.75f)),
                        verticalArrangement = Arrangement.spacedBy(rem(0.75f)),
                    ) {
                        Column {
                            FieldLabel("پروژه مرتبط:", icon = R.drawable.ic_lucide_briefcase, iconTint = Tw.Sky500)
                            TwSelectField(
                                selectedUid = projectUid,
                                options = db.projects.map { it.uid to it.title },
                                placeholder = "بدون پروژه",
                                onSelect = { projectUid = it },
                            )
                        }
                        Column {
                            FieldLabel("طرف حساب:", icon = R.drawable.ic_lucide_user, iconTint = Tw.Emerald500)
                            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.375f)), verticalAlignment = Alignment.CenterVertically) {
                                TwSelectorField(
                                    text = selectedPerson?.fullName ?: "بدون شخص",
                                    placeholder = selectedPerson == null,
                                    action = "تغییر",
                                    onClick = { personOpen = true },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    if (selectedPerson != null) {
                                        PickerAvatar(selectedPerson.fullName.trim().take(1).ifEmpty { "؟" })
                                    } else {
                                        LucideIcon(R.drawable.ic_lucide_user, 1f, Tw.Emerald500)
                                    }
                                }
                                if (selectedPerson != null) {
                                    Box(
                                        Modifier.clickable(remember { MutableInteractionSource() }, null) { personUid = "" }
                                            .padding(rem(0.5f)),
                                    ) { LucideIcon(R.drawable.ic_lucide_x, 1f, Tw.Slate400) }
                                }
                            }
                        }
                        Column {
                            FieldLabel("ارتباط با وام:")
                            TwSelectField(
                                selectedUid = deptUid,
                                options = db.depts.map { it.uid to it.title },
                                placeholder = "بدون ارتباط با وام",
                                onSelect = { newUid ->
                                    deptUid = newUid
                                    val selectedDept = db.depts.firstOrNull { it.uid == newUid }
                                    if (selectedDept != null && selectedDept.periodValue > 0) {
                                        amount = round(selectedDept.periodValue)
                                        if (desc.isEmpty() || desc.startsWith("پرداخت قسط وام")) {
                                            desc = "پرداخت قسط وام: ${selectedDept.title}"
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }

            // ---- actions --------------------------------------------------------------------------------
            Row(
                Modifier.fillMaxWidth().padding(top = rem(0.5f)),
                horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClose)
                        .padding(horizontal = rem(1f), vertical = rem(0.5f)),
                ) { TwText("انصراف", 0.75f, 1f, dk(Tw.Slate600, Tw.Slate300), weight = FW.Semibold, maxLines = 1) }
                if (!editing) {
                    ActionButton("ثبت", Tw.Slate600, hPadRem = 1f, vPadRem = 0.625f) { submit(false) }
                }
                ActionButton(
                    if (editing) "ذخیره تغییرات" else "ثبت و بستن", Tw.Sky600, hPadRem = 1.5f, vPadRem = 0.625f,
                ) { submit(true) }
            }
        }
    }

    // ---- sub modals -------------------------------------------------------------------------------------
    if (categoryOpen) {
        CategoryPickerDialog(
            subjects = db.subjects,
            transactionType = if (type == 3) 1 else type,
            selectedSubjectUid = subjectUid,
            onSelect = { subjectUid = it },
            onAddSubject = onAddSubject,
            onClose = { categoryOpen = false },
            onError = onError,
        )
    }
    if (personOpen) {
        PersonPickerDialog(
            groups = db.personGroups,
            persons = db.persons,
            selectedUid = personUid.ifEmpty { null },
            onSelect = { personUid = it },
            onClose = { personOpen = false },
        )
    }
    if (sourceOpen) {
        AccountPickerDialog(
            groups = db.accountGroups,
            accounts = db.accounts,
            banks = db.banks,
            selectedUid = sourceAccountUid.ifEmpty { null },
            onSelect = { sourceAccountUid = it },
            onClose = { sourceOpen = false },
        )
    }
    if (destOpen) {
        AccountPickerDialog(
            groups = db.accountGroups,
            accounts = db.accounts.filter { it.uid != sourceAccountUid },
            banks = db.banks,
            selectedUid = destAccountUid.ifEmpty { null },
            onSelect = { destAccountUid = it },
            onClose = { destOpen = false },
        )
    }

    if (confirmDuplicate) {
        ConfirmDialog(
            message = "تراکنش مشابهی ثبت شده است.\nآیا مطمئنید که می‌خواهید این تراکنش تکراری ثبت شود؟",
            variant = ConfirmVariant.WARNING,
            confirmLabel = "ثبت شود",
            onConfirm = {
                confirmDuplicate = false
                pendingTx?.let { commit(it, pendingClose) }
                pendingTx = null
            },
            onCancel = { confirmDuplicate = false; pendingTx = null },
        )
    }
    if (confirmNegative) {
        ConfirmDialog(
            message = "موجودی حساب منفی می‌شود!\nآیا ادامه می‌دهید؟",
            variant = ConfirmVariant.WARNING,
            confirmLabel = "بله، ادامه بده",
            onConfirm = {
                confirmNegative = false
                pendingTx?.let { tx ->
                    onSave(tx)
                    if (pendingClose) onClose()
                }
                pendingTx = null
            },
            onCancel = { confirmNegative = false; pendingTx = null },
        )
    }
}

/** `flex items-center justify-center gap-1.5 py-2 rounded-lg text-xs font-bold` tab. */
@Composable
private fun TypeTab(
    text: String,
    icon: Int,
    activeColor: Color,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier.background(if (active) activeColor else Color.Transparent, RoundedCornerShape(rem(0.5f)))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(vertical = rem(0.5f)),
        horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f))
        LucideIcon(icon, 0.875f, if (active) Tw.White else dk(Tw.Slate600, Tw.Slate400))
        TwText(
            text, 0.75f, 1f,
            if (active) Tw.White else dk(Tw.Slate600, Tw.Slate400),
            weight = FW.Bold, maxLines = 1,
        )
        Spacer(Modifier.weight(1f))
    }
}

/** `px-4 py-2.5 rounded-xl text-xs font-bold` primary / secondary action of the action row. */
@Composable
private fun ActionButton(text: String, bg: Color, hPadRem: Float, vPadRem: Float, onClick: () -> Unit) {
    Row(
        Modifier.background(bg, RoundedCornerShape(rem(0.75f)))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = rem(hPadRem), vertical = rem(vPadRem)),
        horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(R.drawable.ic_lucide_check, 1f, Tw.White)
        TwText(text, 0.75f, 1f, Tw.White, weight = FW.Bold, maxLines = 1)
    }
}

/** `label` of the form: text-xs font-bold slate-700 dark:slate-300 mb-1, with an optional icon. */
@Composable
private fun FieldLabel(text: String, icon: Int? = null, iconTint: Color = Tw.Slate400) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = rem(0.25f)),
        horizontalArrangement = Arrangement.spacedBy(rem(0.25f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) LucideIcon(icon, 0.875f, iconTint)
        TwText(text, 0.75f, 1f, dk(Tw.Slate700, Tw.Slate300), weight = FW.Bold, maxLines = 1)
    }
}

/** The plain `px-3 py-2 rounded-xl bg-slate-50 dark:bg-slate-800 border` input of the description. */
@Composable
private fun TwTextFieldPlain(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    TwTextField(
        value = value, onValueChange = onValueChange, placeholder = placeholder,
        vPadRem = 0.5f, startPadRem = 0.75f, endPadRem = 0.75f, bg = dk(Tw.Slate50, Tw.Slate800),
    )
}
