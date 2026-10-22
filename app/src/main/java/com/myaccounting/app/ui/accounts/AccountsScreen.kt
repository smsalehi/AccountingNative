package com.myaccounting.app.ui.accounts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import com.myaccounting.app.R
import com.myaccounting.app.domain.Account
import com.myaccounting.app.domain.AccountGroup
import com.myaccounting.app.domain.Currency
import com.myaccounting.app.domain.CurrencyUnit
import com.myaccounting.app.domain.JalaliDate
import com.myaccounting.app.ui.components.BankLogo
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.components.ShadowLayer
import com.myaccounting.app.ui.components.TwShadow
import com.myaccounting.app.ui.components.cssShadow
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.StrutText
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.Vazirmatn
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem
import com.myaccounting.app.ui.theme.remSp
import com.myaccounting.app.viewmodel.MainViewModel

/** Drag & drop by the grip handles (port of the touch-based reordering in AccountsView.tsx). */
private class Dnd {
    val groupCoords = HashMap<String, LayoutCoordinates>()
    val accountCoords = HashMap<String, LayoutCoordinates>()
    var draggingUid by mutableStateOf<String?>(null)
    var draggingIsGroup = false
    var pointer = Offset.Zero

    /** elementFromPoint(...).closest('[data-dnd-uid]'): accounts sit inside groups, so they win. */
    fun targetAt(p: Offset): String? {
        accountCoords.entries.firstOrNull { it.value.isAttached && it.value.boundsInRoot().contains(p) }?.let { return it.key }
        return groupCoords.entries.firstOrNull { it.value.isAttached && it.value.boundsInRoot().contains(p) }?.key
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AccountsScreen(
    state: MainViewModel.UiState,
    collapsed: Set<String>,
    vm: MainViewModel,
    onOpenNewTransaction: (type: Int, accountUid: String?) -> Unit = { _, _ -> },
    onSelectAccount: (uid: String) -> Unit = { vm.notPorted("نمایش تراکنش‌های حساب") },
) {
    if (!state.loaded) return
    val db = state.shownDb
    val unit = state.effectiveUnit
    val groups = db.accountGroups
    var showHidden by rememberSaveable { mutableStateOf(false) }
    var editingUid by rememberSaveable { mutableStateOf<String?>(null) }
    var editingTitle by rememberSaveable { mutableStateOf("") }
    var settingsAccount by remember { mutableStateOf<Account?>(null) }
    val dnd = remember { Dnd() }
    val bankTitles = remember(db.banks) { db.banks.associate { it.uid to it.title } }
    val areAllCollapsed = groups.isNotEmpty() && groups.all { it.uid in collapsed }

    fun dropped(uid: String, isGroup: Boolean, target: String) {
        if (uid == target) return
        if (isGroup) vm.reorderGroups(uid, target) else vm.reorderAccounts(uid, target)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(rem(1f)).padding(bottom = rem(4f)),
        verticalArrangement = Arrangement.spacedBy(rem(1.5f)),
    ) {
        // ---- header -----------------------------------------------------------------------------------------
        Column(verticalArrangement = Arrangement.spacedBy(rem(1f))) {
            TwText("حساب‌ها و کارت‌های بانکی", 1.25f, 1.75f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(rem(0.5f)), verticalArrangement = Arrangement.spacedBy(rem(0.5f))) {
                PillButton(
                    bg = Tw.Emerald600, fg = Tw.White, icon = R.drawable.ic_lucide_plus, iconRem = 1f, iconTint = Tw.White,
                    text = "جدید", hPadRem = 0.875f, shadow = true,
                ) {
                    if (unit == CurrencyUnit.USD) vm.warn(Currency.USD_READONLY_MESSAGE) else {
                        settingsAccount = Account(
                            uid = "acc_${System.currentTimeMillis()}",
                            title = "حساب بانکی جدید",
                            groupUid = groups.firstOrNull()?.uid ?: "grp_default",
                        )
                    }
                }
                PillButton(
                    bg = Tw.Sky600, fg = Tw.White, icon = R.drawable.ic_lucide_arrow_left_right, iconRem = 0.875f, iconTint = Tw.White,
                    text = "انتقال", hPadRem = 0.75f, shadow = true,
                ) { onOpenNewTransaction(3, null) }
                PillButton(
                    bg = dk(Tw.Slate100, Tw.Slate800), fg = dk(Tw.Slate700, Tw.Slate200), icon = R.drawable.ic_lucide_chevrons_up_down,
                    iconRem = 0.875f, iconTint = Tw.Slate500, text = if (areAllCollapsed) "باز کردن همه" else "بستن همه", hPadRem = 0.75f, shadow = false,
                ) { vm.setCollapsedGroups(if (areAllCollapsed) emptySet() else groups.map { it.uid }.toSet()) }
                PillButton(
                    bg = dk(Tw.Slate100, Tw.Slate800), fg = dk(Tw.Slate700, Tw.Slate200),
                    icon = if (showHidden) R.drawable.ic_lucide_eye_off else R.drawable.ic_lucide_eye, iconRem = 1f,
                    iconTint = dk(Tw.Slate700, Tw.Slate200), text = "راکد", hPadRem = 0.75f, shadow = false,
                ) { showHidden = !showHidden }
            }
        }

        // ---- groups -----------------------------------------------------------------------------------------
        Column(verticalArrangement = Arrangement.spacedBy(rem(1f))) {
            for (group in groups) {
                val groupAccounts = db.accounts.filter { it.groupUid == group.uid && (showHidden || it.isHidden == 0) }
                if (groupAccounts.isEmpty()) continue
                val groupTotal = groupAccounts.sumOf { state.balances[it.uid] ?: 0.0 }
                val isCollapsed = group.uid in collapsed
                GroupCard(
                    group = group, accounts = groupAccounts, total = groupTotal, unit = unit, collapsed = isCollapsed,
                    editing = editingUid == group.uid, editingTitle = editingTitle,
                    onEditTitle = { editingTitle = it },
                    onStartEdit = { editingUid = group.uid; editingTitle = group.title },
                    onSaveEdit = { if (editingTitle.trim().isNotEmpty()) vm.renameGroup(group.uid, editingTitle); editingUid = null },
                    onCancelEdit = { editingUid = null },
                    onToggle = { vm.setCollapsedGroups(if (isCollapsed) collapsed - group.uid else collapsed + group.uid) },
                    dnd = dnd, onDrop = ::dropped,
                    bankTitles = bankTitles, balances = state.balances, ownChecks = state.ownChecksByAccount,
                    onSelectAccount = { uid -> onSelectAccount(uid) },
                    onNewTransaction = { uid -> onOpenNewTransaction(1, uid) },
                    onAccountSettings = { uid -> if (unit == CurrencyUnit.USD) vm.warn(Currency.USD_READONLY_MESSAGE) else settingsAccount = db.accounts.firstOrNull { it.uid == uid } },
                )
            }
        }
    }

    settingsAccount?.let { acc ->
        AccountSettingsDialog(
            account = acc,
            db = db,
            unit = unit,
            balance = state.balances[acc.uid] ?: 0.0,
            txCount = db.transactions.count { it.sourceAccountUid == acc.uid || it.destinationAccountUid == acc.uid },
            onClose = { settingsAccount = null },
            onSave = { vm.saveAccount(it); settingsAccount = null },
            onDelete = { vm.deleteAccount(it) },
            onViewTransactions = { settingsAccount = null; onSelectAccount(acc.uid) },
            onQuickTransaction = { type -> onOpenNewTransaction(type, acc.uid); settingsAccount = null },
        )
    }
}

@Composable
private fun PillButton(
    bg: Color, fg: Color, icon: Int, iconRem: Float, iconTint: Color, text: String, hPadRem: Float, shadow: Boolean, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(rem(0.75f))
    Row(
        Modifier.then(if (shadow) Modifier.cssShadow(TwShadow.xs(), rem(0.75f)) else Modifier)
            .background(bg, shape)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = rem(hPadRem), vertical = rem(0.5f)),
        horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LucideIcon(icon, iconRem, iconTint)
        TwText(text, 0.75f, 1f, fg, weight = FW.Semibold, maxLines = 1)
    }
}

@Composable
private fun Grip(uid: String, isGroup: Boolean, sizeRem: Float, tint: Color, dnd: Dnd, onDrop: (String, Boolean, String) -> Unit) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    LucideIcon(
        R.drawable.ic_lucide_grip_vertical, sizeRem, tint,
        Modifier.onGloballyPositioned { origin = it.positionInRoot() }.pointerInput(uid) {
            detectDragGestures(
                onDragStart = { dnd.draggingUid = uid; dnd.draggingIsGroup = isGroup },
                onDrag = { change, _ -> change.consume(); dnd.pointer = origin + change.position },
                onDragEnd = {
                    val target = dnd.targetAt(dnd.pointer)
                    dnd.draggingUid = null
                    if (target != null) onDrop(uid, isGroup, target)
                },
                onDragCancel = { dnd.draggingUid = null },
            )
        },
    )
}

@Composable
private fun GroupCard(
    group: AccountGroup, accounts: List<Account>, total: Double, unit: CurrencyUnit, collapsed: Boolean,
    editing: Boolean, editingTitle: String, onEditTitle: (String) -> Unit, onStartEdit: () -> Unit, onSaveEdit: () -> Unit, onCancelEdit: () -> Unit,
    onToggle: () -> Unit, dnd: Dnd, onDrop: (String, Boolean, String) -> Unit,
    bankTitles: Map<String, String>, balances: Map<String, Double>, ownChecks: Map<String, Double>,
    onSelectAccount: (String) -> Unit, onNewTransaction: (String) -> Unit, onAccountSettings: (String) -> Unit,
) {
    val bw = cssBorder()
    val shape = RoundedCornerShape(rem(1f))
    val dragging = dnd.draggingUid == group.uid
    val scale by animateFloatAsState(if (dragging) 0.98f else 1f, tween(150), label = "g-scale")
    val alpha by animateFloatAsState(if (dragging) 0.7f else 1f, tween(150), label = "g-alpha")
    val ring = if (dragging) listOf(ShadowLayer(0f, 0f, 2f, dk(Tw.Sky200, Tw.Sky800))) else emptyList()

    Column(
        Modifier.fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
            .onGloballyPositioned { dnd.groupCoords[group.uid] = it }
            .cssShadow(TwShadow.xs() + ring, rem(1f))
            .background(dk(Tw.White, Tw.Slate900), shape)
            .border(bw, if (dragging) dk(Tw.Sky400, Tw.Sky500) else dk(Tw.Slate200.a(0.8f), Tw.Slate800), shape)
            .padding(bw),
    ) {
        // header (bg-slate-50/80 dark:bg-slate-800/50, p-3.5, gap-3)
        Row(
            Modifier.fillMaxWidth().background(dk(Tw.Slate50.a(0.8f), Tw.Slate800.a(0.5f))).padding(rem(0.875f)),
            horizontalArrangement = Arrangement.spacedBy(rem(0.75f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, null, onClick = onToggle),
                horizontalArrangement = Arrangement.spacedBy(rem(0.625f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val boxShape = RoundedCornerShape(rem(0.5f))
                Box(
                    Modifier.cssShadow(TwShadow.xs(), rem(0.5f))
                        .background(dk(Tw.White, Tw.Slate800), boxShape)
                        .border(bw, dk(Tw.Slate200, Tw.Slate700), boxShape)
                        .padding(rem(0.25f) + bw),
                ) {
                    LucideIcon(
                        if (collapsed) R.drawable.ic_lucide_chevron_down else R.drawable.ic_lucide_chevron_up, 1f, dk(Tw.Sky600, Tw.Sky400),
                    )
                }
                Column(Modifier.weight(1f)) {
                    if (editing) {
                        GroupTitleEditor(editingTitle, onEditTitle, onSaveEdit, onCancelEdit)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(rem(0.375f)), verticalAlignment = Alignment.CenterVertically) {
                            TwText(
                                group.title, 0.875f, 1.25f, dk(Tw.Slate800, Tw.Slate100), Modifier.weight(1f, fill = false),
                                weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Box(
                                Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onStartEdit)
                                    .padding(rem(0.125f)),
                            ) { LucideIcon(R.drawable.ic_lucide_pencil, 0.75f, Tw.Slate400) }
                        }
                    }
                    // inline <span> inside a 1rem/1.5 block → line box of the parent's strut
                    StrutText(
                        "${JalaliDate.toPersianDigits(accounts.size)} حساب / کارت", 1f, 1.5f, 0.6875f, 1.03125f,
                        dk(Tw.Slate500, Tw.Slate400),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.75f)), verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) { // text-left (physical left in RTL = End)
                    TwText("موجودی گروه:", 0.625f, 0.9375f, Tw.Slate400, align = TextAlign.Left)
                    StrutText(
                        Currency.formatCurrency(total, unit), 1f, 1.5f, 0.75f, 1f, dk(Tw.Slate900, Tw.Slate100), weight = FW.Black,
                    )
                }
                val sepColor = dk(Tw.Slate200, Tw.Slate700)
                Row(
                    Modifier.drawBehind {
                        val w = bw.toPx()
                        drawRect(sepColor, Offset(size.width - w, 0f), Size(w, size.height)) // border-r
                    }.absolutePadding(right = rem(0.5f) + bw),
                    verticalAlignment = Alignment.CenterVertically,
                ) { Grip(group.uid, true, 1f, dk(Tw.Slate300, Tw.Slate600), dnd, onDrop) }
            }
        }

        if (!collapsed) {
            Box(Modifier.fillMaxWidth().height(bw).background(dk(Tw.Slate100, Tw.Slate800)))
            Column(
                Modifier.fillMaxWidth().background(dk(Tw.White, Tw.Slate900)).padding(rem(0.75f)),
                verticalArrangement = Arrangement.spacedBy(rem(0.75f)),
            ) {
                for (acc in accounts) {
                    AccountCard(
                        acc, unit, bankTitles[acc.bankUid ?: ""], balances[acc.uid] ?: 0.0, ownChecks[acc.uid] ?: 0.0, dnd, onDrop,
                        onSelectAccount, onNewTransaction, onAccountSettings,
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupTitleEditor(value: String, onChange: (String) -> Unit, onSave: () -> Unit, onCancel: () -> Unit) {
    val bw = cssBorder()
    val fr = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { fr.requestFocus() }
    val inputShape = RoundedCornerShape(rem(0.5f))
    Row(horizontalArrangement = Arrangement.spacedBy(rem(0.375f)), verticalAlignment = Alignment.CenterVertically) {
        val textColor = dk(Tw.Slate800, Tw.Slate100)
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = TextStyle(
                fontFamily = Vazirmatn, fontSize = remSp(0.875f), lineHeight = remSp(1.25f), fontWeight = FontWeight.Bold, color = textColor,
                textDirection = TextDirection.Rtl,
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
            cursorBrush = SolidColor(textColor),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (value.trim().isNotEmpty()) onSave() }),
            modifier = Modifier.weight(1f).focusRequester(fr).onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box(
                    Modifier.then(if (focused) Modifier.cssShadow(listOf(ShadowLayer(0f, 0f, 2f, Tw.Sky400)), rem(0.5f)) else Modifier)
                        .background(dk(Tw.White, Tw.Slate800), inputShape)
                        .border(bw, dk(Tw.Sky400, Tw.Sky500), inputShape)
                        .padding(horizontal = rem(0.5f) + bw, vertical = rem(0.25f) + bw),
                ) { inner() }
            },
        )
        val small = RoundedCornerShape(rem(0.375f))
        Box(
            Modifier.background(dk(Tw.Emerald100, Tw.Emerald900.a(0.4f)), small)
                .clickable(remember { MutableInteractionSource() }, null) { if (value.trim().isNotEmpty()) onSave() else onCancel() }
                .padding(rem(0.25f)),
        ) { LucideIcon(R.drawable.ic_lucide_check, 0.875f, dk(Tw.Emerald600, Tw.Emerald400)) }
        Box(
            Modifier.background(dk(Tw.Slate100, Tw.Slate800), small)
                .clickable(remember { MutableInteractionSource() }, null, onClick = onCancel)
                .padding(rem(0.25f)),
        ) { LucideIcon(R.drawable.ic_lucide_x, 0.875f, Tw.Slate500) }
    }
}

@Composable
private fun AccountCard(
    acc: Account, unit: CurrencyUnit, bankTitle: String?, balance: Double, pendingChecks: Double, dnd: Dnd,
    onDrop: (String, Boolean, String) -> Unit, onSelect: (String) -> Unit, onNewTransaction: (String) -> Unit, onSettings: (String) -> Unit,
) {
    val bw = cssBorder()
    val shape = RoundedCornerShape(rem(1f))
    val cornerPx = rem(1f)
    val dragging = dnd.draggingUid == acc.uid
    val hidden = acc.isHidden != 0
    val scale by animateFloatAsState(if (dragging) 0.95f else 1f, tween(150), label = "a-scale")
    val alphaAnim by animateFloatAsState(if (dragging) 0.7f else if (hidden) 0.6f else 1f, tween(150), label = "a-alpha")

    val bg = if (hidden && !dragging) dk(Tw.Slate50, Tw.Slate800.a(0.3f)) else dk(Tw.Slate50.a(0.7f), Tw.Slate800.a(0.6f))
    val borderColor = when {
        dragging -> dk(Tw.Sky400, Tw.Sky500)
        hidden -> dk(Tw.Slate300, Tw.Slate700)
        else -> dk(Tw.Slate200.a(0.8f), Tw.Slate700.a(0.8f))
    }
    val layers = (if (!hidden || dragging) TwShadow.xxs() else emptyList()) +
        (if (dragging) listOf(ShadowLayer(0f, 0f, 2f, dk(Tw.Sky200, Tw.Sky800))) else emptyList())

    Box(
        Modifier.fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alphaAnim }
            .onGloballyPositioned { dnd.accountCoords[acc.uid] = it }
            .cssShadow(layers, rem(1f))
            .background(bg, shape)
            .then(
                if (hidden && !dragging) Modifier.drawBehind {
                    val w = bw.toPx()
                    drawRoundRect(
                        borderColor, topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerPx.toPx() - w / 2),
                        style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 3, w * 3))),
                    )
                } else Modifier.border(bw, borderColor, shape),
            )
            .clickable(remember { MutableInteractionSource() }, null) { onSelect(acc.uid) },
    ) {
        Column(Modifier.padding(rem(0.875f) + bw)) {
            // logo + title
            Row(horizontalArrangement = Arrangement.spacedBy(rem(0.75f)), verticalAlignment = Alignment.CenterVertically) {
                BankLogo(if (!bankTitle.isNullOrEmpty()) bankTitle else acc.title, acc.bankUid)
                Column(Modifier.weight(1f).absolutePadding(right = rem(0.125f))) {
                    TwText(
                        acc.title, 0.75f, 1f, dk(Tw.Slate800, Tw.Slate100), Modifier.fillMaxWidth(),
                        weight = FW.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    val card = formatCardNumber(acc.cardNumber)
                    if (card != null) {
                        TwText(
                            card, 0.625f, 0.9375f, dk(Tw.Slate500, Tw.Slate400), Modifier.fillMaxWidth().padding(top = rem(0.125f)),
                            align = TextAlign.Right, direction = TextDirection.Ltr, family = FontFamily.Monospace,
                        )
                    } else if (!bankTitle.isNullOrEmpty()) {
                        TwText(
                            bankTitle, 0.625f, 0.9375f, Tw.Slate400, Modifier.fillMaxWidth().padding(top = rem(0.125f)),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            // bottom: border-t + pt-2.5, mt-3.5
            Box(Modifier.padding(top = rem(0.875f)).fillMaxWidth().height(bw).background(dk(Tw.Slate200.a(0.6f), Tw.Slate700.a(0.6f))))
            Row(
                Modifier.fillMaxWidth().padding(top = rem(0.625f)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val btnShape = RoundedCornerShape(rem(0.75f))
                Row(
                    Modifier.background(dk(Tw.Sky50, Tw.Sky950.a(0.6f)), btnShape)
                        .border(bw, dk(Tw.Sky200, Tw.Sky800), btnShape)
                        .clickable(remember { MutableInteractionSource() }, null) { onNewTransaction(acc.uid) }
                        .padding(horizontal = rem(0.625f) + bw, vertical = rem(0.375f) + bw),
                    horizontalArrangement = Arrangement.spacedBy(rem(0.25f)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LucideIcon(R.drawable.ic_lucide_circle_plus, 0.875f, Tw.Sky600)
                    TwText("ثبت تراکنش", 0.75f, 1f, dk(Tw.Sky600, Tw.Sky400), weight = FW.Bold, maxLines = 1)
                }
                Column {
                    TwText("موجودی", 0.625f, 0.9375f, dk(Tw.Slate500, Tw.Slate400), weight = FW.Semibold)
                    TwText(
                        Currency.formatCurrency(balance, unit), 0.75f, 1f, dk(Tw.Slate900, Tw.Slate100),
                        Modifier.padding(top = rem(0.125f)), weight = FW.Black, trackingEm = -0.025f,
                    )
                    if (pendingChecks > 0) {
                        TwText(
                            "پس از چک‌ها: ${Currency.formatCurrency(balance - pendingChecks, unit)}", 0.625f, 0.9375f,
                            dk(Tw.Amber600, Tw.Amber400), Modifier.padding(top = rem(0.125f)), weight = FW.Semibold,
                        )
                    }
                }
            }
        }

        // top-left controls: absolute top-2.5 left-2.5 flex gap-1 opacity-60
        Row(
            Modifier.align(Alignment.TopEnd) // physical LEFT edge in RTL
                .absoluteOffset(x = rem(0.625f) + bw, y = rem(0.625f) + bw)
                .alpha(0.6f),
            horizontalArrangement = Arrangement.spacedBy(rem(0.25f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val gearShape = RoundedCornerShape(rem(0.5f))
            Box(
                Modifier.cssShadow(TwShadow.xxs(), rem(0.5f))
                    .background(dk(Tw.White, Tw.Slate800), gearShape)
                    .border(bw, dk(Tw.Slate200, Tw.Slate700), gearShape)
                    .clickable(remember { MutableInteractionSource() }, null) { onSettings(acc.uid) }
                    .semantics { contentDescription = "تنظیمات حساب" }
                    .padding(rem(0.25f) + bw),
            ) { LucideIcon(R.drawable.ic_lucide_settings, 0.875f, Tw.Slate400) }
            Grip(acc.uid, false, 0.875f, Tw.Slate300, dnd, onDrop)
        }
    }
}

private fun formatCardNumber(cardNum: String?): String? {
    if (cardNum == null || cardNum.length < 4) return null
    val clean = cardNum.replace(Regex("\\s+"), "")
    return "کارت •••• ${JalaliDate.toPersianDigits(clean.takeLast(4))}"
}

