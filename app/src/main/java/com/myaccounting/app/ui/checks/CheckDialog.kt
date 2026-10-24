package com.myaccounting.app.ui.checks

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
import androidx.compose.ui.text.style.TextOverflow
import com.myaccounting.app.R
import com.myaccounting.app.domain.Account
import com.myaccounting.app.domain.AccountGroup
import com.myaccounting.app.domain.Bank
import com.myaccounting.app.domain.Check
import com.myaccounting.app.domain.CheckKind
import com.myaccounting.app.domain.Checks
import com.myaccounting.app.domain.CurrencyUnit
import com.myaccounting.app.domain.Person
import com.myaccounting.app.domain.PersonGroup
import com.myaccounting.app.domain.Subject
import com.myaccounting.app.ui.components.AmountInput
import com.myaccounting.app.ui.components.BankLogo
import com.myaccounting.app.ui.components.CategoryIcon
import com.myaccounting.app.ui.components.HairLine
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.components.PickerAvatar
import com.myaccounting.app.ui.components.TwIconButton
import com.myaccounting.app.ui.components.TwModal
import com.myaccounting.app.ui.components.TwSelectField
import com.myaccounting.app.ui.components.TwSelectorField
import com.myaccounting.app.ui.components.TwShadow
import com.myaccounting.app.ui.components.TwTextField
import com.myaccounting.app.ui.components.cssShadow
import com.myaccounting.app.ui.transactions.AccountPickerDialog
import com.myaccounting.app.ui.transactions.CategoryPickerDialog
import com.myaccounting.app.ui.transactions.PersonPickerDialog
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem
import kotlin.random.Random

/** `UID: 'chk_' + Date.now().toString(36) + 4 random base-36 characters` (CheckFormModal). */
private fun newCheckUid(now: Long = System.currentTimeMillis()): String {
    val alphabet = "0123456789abcdefghijklmnopqrstuvwxyz"
    val suffix = (1..4).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
    return "chk_" + now.toString(36) + suffix
}

/**
 * ChecksView.tsx CheckFormModal + ModalShell (z-50, `max-w-md max-h-[92vh]`, scrim slate-900/60):
 * new/edit check with kind toggle, amount, due date, account/drawer, cost subject and the
 * optional payee / check number / project group.
 */
@Composable
fun CheckDialog(
    editing: Check?,
    db: com.myaccounting.app.domain.AccountingDatabase,
    unit: CurrencyUnit,
    onClose: () -> Unit,
    onSubmit: (Check) -> Unit,
    onAddSubject: (Subject) -> Unit = {},
    onError: (String) -> Unit = {},
) {
    val accounts = db.accounts.filter { it.isHidden != 1 }
    val persons = db.persons.filter { it.isHidden != 1 }

    var kind by remember { mutableStateOf(editing?.kind ?: CheckKind.OWN) }
    var amount by remember { mutableStateOf(editing?.amount ?: 0.0) }
    var dueDate by remember { mutableStateOf(editing?.dueDate ?: "") }
    var accountUid by remember { mutableStateOf(editing?.accountUid ?: "") }
    var drawerUid by remember { mutableStateOf(editing?.drawerPersonUid ?: "") }
    var payeeUid by remember { mutableStateOf(editing?.payeePersonUid ?: "") }
    var checkNumber by remember { mutableStateOf(editing?.checkNumber ?: "") }
    var subjectUid by remember { mutableStateOf(editing?.subjectUid ?: "") }
    var projectUid by remember { mutableStateOf(editing?.projectUid ?: "") }
    var desc by remember { mutableStateOf(editing?.desc ?: "") }
    var categoryOpen by remember { mutableStateOf(false) }
    var accountOpen by remember { mutableStateOf(false) }
    var drawerOpen by remember { mutableStateOf(false) }
    var payeeOpen by remember { mutableStateOf(false) }
    var showOptionalFields by remember {
        mutableStateOf(editing != null && (!editing.payeePersonUid.isNullOrEmpty() || !editing.checkNumber.isNullOrEmpty() || !editing.projectUid.isNullOrEmpty()))
    }

    val selectedAccount: Account? = accounts.firstOrNull { it.uid == accountUid }
    val accountsById = remember(db.accounts) { db.accounts.associateBy { it.uid } }
    val banksById = remember(db.banks) { db.banks.associateBy { it.uid } }
    val selectedAccountBank = selectedAccount?.let { banksById[it.bankUid]?.title }
    val drawerPerson = persons.firstOrNull { it.uid == drawerUid }
    val payeePerson = persons.firstOrNull { it.uid == payeeUid }

    val subjectsById = remember(db.subjects) { db.subjects.associateBy { it.uid } }
    val currentSubject = subjectsById[subjectUid]
    val currentParentSubject = currentSubject?.parentUid?.let { subjectsById[it] }

    val amberAction = dk(Tw.Amber600, Tw.Amber400)

    fun submit() {
        if (!(amount > 0)) { onError("لطفاً مبلغ چک را وارد کنید."); return }
        if (Checks.jalaliStrToMs(dueDate) == null) { onError("لطفاً تاریخ سررسید را انتخاب کنید."); return }
        if (kind == CheckKind.OWN && accountUid.isEmpty()) { onError("لطفاً حساب چک را انتخاب کنید."); return }
        if (kind == CheckKind.PERSON && drawerUid.isEmpty()) { onError("لطفاً صادرکننده چک را انتخاب کنید."); return }
        if (subjectUid.isEmpty()) { onError("لطفاً سرفصل هزینه را انتخاب کنید."); return }
        val base = editing ?: Check(
            uid = newCheckUid(),
            kind = kind,
            amount = 0.0,
            dueDate = "",
            status = com.myaccounting.app.domain.CheckStatus.PENDING,
            createdAt = System.currentTimeMillis(),
        )
        onSubmit(
            base.copy(
                kind = kind,
                amount = kotlin.math.round(amount),
                dueDate = dueDate,
                fundByDate = null,
                accountUid = if (kind == CheckKind.OWN) accountUid else null,
                drawerPersonUid = if (kind == CheckKind.PERSON) drawerUid else null,
                payeePersonUid = payeeUid.ifEmpty { null },
                checkNumber = checkNumber.trim().ifEmpty { null },
                subjectUid = subjectUid.ifEmpty { null },
                projectUid = projectUid.ifEmpty { null },
                desc = desc.trim().ifEmpty { null },
            ),
        )
        onClose()
    }

    TwModal(
        onDismiss = onClose,
        maxWidthRem = 28f,          // max-w-md
        maxHeightFraction = 0.92f,  // max-h-[92vh]
        shapeRem = 1f,              // rounded-2xl
        scrimColor = Tw.Slate900.a(0.6f),
    ) {
        // ---- header (border-b border-slate-100 dark:border-slate-800 pb-3) ------------------------------
        Row(
            Modifier.fillMaxWidth().padding(bottom = rem(0.75f)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                TwText(
                    if (editing != null) "ویرایش چک" else "ثبت چک جدید",
                    1f, 1.5f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1,
                )
                TwText("تا پاس نشدن، روی موجودی حساب اثر ندارد", 0.6875f, 1f, Tw.Slate400, maxLines = 1)
            }
            TwIconButton(R.drawable.ic_lucide_x, onClose, Tw.Slate400, iconRem = 1.25f)
        }
        HairLine(dk(Tw.Slate100, Tw.Slate800))

        // ---- form (space-y-3.5) --------------------------------------------------------------------------
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = rem(0.875f)),
            verticalArrangement = Arrangement.spacedBy(rem(0.875f)),
        ) {
            // ---- kind (only when adding) ------------------------------------------------------------------
            if (editing == null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(rem(0.5f))) {
                    KindButton("چک خودم", kind == CheckKind.OWN, Modifier.weight(1f)) { kind = CheckKind.OWN }
                    KindButton("چک صادرشده توسط شخص", kind == CheckKind.PERSON, Modifier.weight(1f)) { kind = CheckKind.PERSON }
                }
            }

            // ---- amount ------------------------------------------------------------------------------------
            AmountInput(
                value = amount,
                onChange = { amount = it },
                unit = unit,
                label = "مبلغ چک",
                placeholder = "۰",
            )

            // ---- due date -----------------------------------------------------------------------------------
            Column {
                CheckFieldLabel("تاریخ سررسید:")
                com.myaccounting.app.ui.components.PersianDateField(
                    value = dueDate,
                    onChange = { dueDate = it },
                    placeholder = "انتخاب تاریخ",
                )
            }

            // ---- account / drawer ---------------------------------------------------------------------------
            if (kind == CheckKind.OWN) {
                Column {
                    CheckFieldLabel("حساب:")
                    TwSelectorField(
                        text = selectedAccount?.title?.ifEmpty { "انتخاب شده" } ?: "انتخاب حساب...",
                        placeholder = selectedAccount == null,
                        action = "تغییر",
                        onClick = { accountOpen = true },
                        actionColor = amberAction,
                    ) {
                        if (selectedAccount != null) {
                            BankLogo(selectedAccountBank ?: selectedAccount.title, selectedAccount.bankUid, sizeRem = 1.5f, padRem = 0.125f)
                        } else {
                            Box(
                                Modifier.size(rem(1.75f)).background(dk(Tw.Slate200, Tw.Slate700), RoundedCornerShape(rem(0.5f))),
                                contentAlignment = Alignment.Center,
                            ) { LucideIcon(R.drawable.ic_lucide_building, 0.875f, Tw.Slate500) }
                        }
                    }
                }
            } else {
                Column {
                    CheckFieldLabel("صادرکننده:")
                    TwSelectorField(
                        text = drawerPerson?.fullName ?: "انتخاب شخص...",
                        placeholder = drawerPerson == null,
                        action = "تغییر",
                        onClick = { drawerOpen = true },
                        actionColor = amberAction,
                    ) {
                        if (drawerPerson != null) {
                            PickerAvatar(drawerPerson.fullName.trim().take(1).ifEmpty { "؟" })
                        } else {
                            Box(
                                Modifier.size(rem(1.75f)).background(dk(Tw.Slate200, Tw.Slate700), RoundedCornerShape(rem(0.5f))),
                                contentAlignment = Alignment.Center,
                            ) { LucideIcon(R.drawable.ic_lucide_folder_open, 0.875f, Tw.Slate500) }
                        }
                    }
                }
            }

            // ---- cost subject --------------------------------------------------------------------------------
            Column {
                CheckFieldLabel("سرفصل هزینه:")
                TwSelectorField(
                    text = when {
                        currentSubject == null -> "برای انتخاب کلیک کنید..."
                        currentParentSubject != null -> "${currentParentSubject.title} / ${currentSubject.title}"
                        else -> currentSubject.title
                    },
                    placeholder = currentSubject == null,
                    action = "تغییر سرفصل",
                    onClick = { categoryOpen = true },
                    actionColor = amberAction,
                ) {
                    if (currentSubject != null) {
                        CategoryIcon(currentSubject.title, currentSubject.icon, currentSubject.isCost, com.myaccounting.app.ui.components.CategoryIconSize.SM)
                    } else {
                        Box(
                            Modifier.size(rem(1.75f)).background(dk(Tw.Slate200, Tw.Slate700), RoundedCornerShape(rem(0.5f))),
                            contentAlignment = Alignment.Center,
                        ) { LucideIcon(R.drawable.ic_lucide_folder_open, 0.875f, Tw.Slate500) }
                    }
                }
            }

            // ---- description ---------------------------------------------------------------------------------
            Column {
                CheckFieldLabel("توضیحات (اختیاری):")
                TwTextField(
                    value = desc, onValueChange = { desc = it },
                    vPadRem = 0.5f, startPadRem = 0.75f, endPadRem = 0.75f,
                    bg = dk(Tw.Slate50, Tw.Slate800),
                )
            }

            // ---- optional fields (payee, check number, project) ------------------------------------------------
            Row(
                Modifier.clickable(remember { MutableInteractionSource() }, null) { showOptionalFields = !showOptionalFields },
                horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LucideIcon(
                    if (showOptionalFields) R.drawable.ic_lucide_chevron_up else R.drawable.ic_lucide_chevron_down,
                    0.875f, dk(Tw.Slate500, Tw.Slate400),
                )
                TwText("گزینه‌های بیشتر (گیرنده، شماره چک، پروژه)", 0.6875f, 1f, dk(Tw.Slate500, Tw.Slate400), weight = FW.Semibold, maxLines = 1)
            }

            if (showOptionalFields) {
                Column(
                    Modifier.fillMaxWidth().padding(top = rem(0.375f)),
                    verticalArrangement = Arrangement.spacedBy(rem(0.75f)),
                ) {
                    Column {
                        CheckFieldLabel("گیرنده چک (اختیاری):")
                        Row(horizontalArrangement = Arrangement.spacedBy(rem(0.375f)), verticalAlignment = Alignment.CenterVertically) {
                            TwSelectorField(
                                text = payeePerson?.fullName ?: "—",
                                placeholder = payeePerson == null,
                                action = "تغییر",
                                onClick = { payeeOpen = true },
                                actionColor = amberAction,
                                modifier = Modifier.weight(1f),
                            ) {
                                if (payeePerson != null) {
                                    PickerAvatar(payeePerson.fullName.trim().take(1).ifEmpty { "؟" })
                                }
                            }
                            if (payeePerson != null) {
                                Box(
                                    Modifier.clickable(remember { MutableInteractionSource() }, null) { payeeUid = "" }
                                        .padding(rem(0.5f)),
                                ) { LucideIcon(R.drawable.ic_lucide_x, 1f, Tw.Slate400) }
                            }
                        }
                    }
                    Column {
                        CheckFieldLabel("شماره چک (اختیاری):")
                        TwTextField(
                            value = checkNumber, onValueChange = { checkNumber = it },
                            vPadRem = 0.5f, startPadRem = 0.75f, endPadRem = 0.75f,
                            bg = dk(Tw.Slate50, Tw.Slate800),
                        )
                    }
                    Column {
                        CheckFieldLabel("پروژه (اختیاری):")
                        TwSelectField(
                            selectedUid = projectUid,
                            options = db.projects.map { it.uid to it.title },
                            placeholder = "عمومی / بدون پروژه",
                            onSelect = { projectUid = it },
                        )
                    }
                }
            }

            // ---- actions (pt-2 justify-end gap-2) ---------------------------------------------------------------
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
                Row(
                    Modifier.cssShadow(TwShadow.xs(), rem(0.75f))
                        .background(Tw.Amber600, RoundedCornerShape(rem(0.75f)))
                        .clickable(remember { MutableInteractionSource() }, null) { submit() }
                        .padding(horizontal = rem(1.25f), vertical = rem(0.5f)),
                    horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LucideIcon(R.drawable.ic_lucide_check, 1f, Tw.White)
                    TwText(if (editing != null) "ذخیره" else "ثبت چک", 0.75f, 1f, Tw.White, weight = FW.Bold, maxLines = 1)
                }
            }
        }
    }

    // ---- sub pickers -------------------------------------------------------------------------------------
    if (categoryOpen) {
        CategoryPickerDialog(
            subjects = db.subjects,
            transactionType = 1,
            onClose = { categoryOpen = false },
            onSelect = { subjectUid = it; categoryOpen = false },
            onAddSubject = onAddSubject,
            selectedSubjectUid = subjectUid.ifEmpty { null },
            onError = onError,
        )
    }
    if (accountOpen) {
        AccountPickerDialog(
            groups = db.accountGroups,
            accounts = accounts,
            banks = db.banks,
            onClose = { accountOpen = false },
            onSelect = { accountUid = it; accountOpen = false },
            selectedUid = accountUid.ifEmpty { null },
        )
    }
    if (drawerOpen) {
        PersonPickerDialog(
            groups = db.personGroups,
            persons = db.persons,
            onClose = { drawerOpen = false },
            selectedUid = drawerUid.ifEmpty { null },
            onSelect = { drawerUid = it; drawerOpen = false },
        )
    }
    if (payeeOpen) {
        PersonPickerDialog(
            groups = db.personGroups,
            persons = db.persons,
            onClose = { payeeOpen = false },
            selectedUid = payeeUid.ifEmpty { null },
            onSelect = { payeeUid = it; payeeOpen = false },
        )
    }
}

/** `grid-cols-2 gap-2` kind toggle: `px-3 py-2 rounded-xl text-xs font-bold border`; active = amber-600. */
@Composable
private fun KindButton(text: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .background(if (active) Tw.Amber600 else dk(Tw.Slate50, Tw.Slate800), RoundedCornerShape(rem(0.75f)))
            .border(cssBorder(), if (active) Tw.Amber600 else dk(Tw.Slate200, Tw.Slate700), RoundedCornerShape(rem(0.75f)))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = rem(0.75f), vertical = rem(0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        TwText(
            text, 0.75f, 1f,
            if (active) Tw.White else dk(Tw.Slate600, Tw.Slate300),
            weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** `label` of the form: text-xs font-bold slate-700 dark:slate-300 mb-1. */
@Composable
private fun CheckFieldLabel(text: String) {
    Box(Modifier.fillMaxWidth().padding(bottom = rem(0.25f))) {
        TwText(text, 0.75f, 1f, dk(Tw.Slate700, Tw.Slate300), weight = FW.Bold, maxLines = 1)
    }
}
