package com.myaccounting.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myaccounting.app.data.ThemeMode
import com.myaccounting.app.domain.CurrencyUnit
import com.myaccounting.app.domain.Transaction
import com.myaccounting.app.ui.accounts.AccountsScreen
import com.myaccounting.app.ui.components.AppToast
import com.myaccounting.app.ui.components.NotificationDropdown
import com.myaccounting.app.ui.transactions.NewTransactionDialog
import com.myaccounting.app.ui.transactions.TransactionsScreen
import com.myaccounting.app.ui.persons.PersonsScreen
import com.myaccounting.app.ui.depts.DeptsScreen
import com.myaccounting.app.ui.checks.ChecksScreen
import com.myaccounting.app.ui.settings.SettingsScreen
import com.myaccounting.app.ui.transactions.TxUi
import com.myaccounting.app.ui.theme.LocalSafeInsets
import com.myaccounting.app.ui.theme.SafeInsets
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem
import com.myaccounting.app.viewmodel.MainViewModel
import com.myaccounting.app.viewmodel.ToastMessage
import kotlinx.coroutines.delay

/** App.tsx `handleOpenNewTransaction` + `editingTransaction`: the request that opens the modal. */
private data class TxRequest(
    val type: Int,
    val accountUid: String?,
    val deptUid: String?,
    val projectUid: String?,
    val personUid: String?,
    val editing: Transaction?,
)

/** App shell = App.tsx layout: sticky header, page background, fixed bottom bar. Only the Accounts tab is ported. */
@Composable
fun AppRoot(vm: MainViewModel, isDark: Boolean, themeMode: ThemeMode) {
    val state by vm.state.collectAsStateWithLifecycle()
    val collapsed by vm.collapsedGroups.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.ACCOUNTS) }
    var showNotifs by rememberSaveable { mutableStateOf(false) }
    // App.tsx: selectedAccountFilter / selectedDeptFilter / selectedProjectFilter / txFilterRequestId.
    var selectedAccountFilter by remember { mutableStateOf<String?>(null) }
    var selectedDeptFilter by remember { mutableStateOf<String?>(null) }
    var selectedProjectFilter by remember { mutableStateOf<String?>(null) }
    var txFilterRequestId by remember { mutableIntStateOf(0) }
    // The web keeps every view mounted, so the transactions filters survive tab switches.
    val txUi = remember { TxUi() }
    LaunchedEffect(selectedAccountFilter) { txUi.applyAccountFilter(selectedAccountFilter) }
    LaunchedEffect(selectedDeptFilter) { txUi.applyDeptFilter(selectedDeptFilter) }
    LaunchedEffect(selectedProjectFilter) { txUi.applyProjectFilter(selectedProjectFilter) }
    LaunchedEffect(txFilterRequestId) {
        if (txFilterRequestId == 0) return@LaunchedEffect
        txUi.resetForRequest(selectedAccountFilter, selectedDeptFilter, selectedProjectFilter)
    }
    // App.tsx modalTxType / modalAccountUid / … : a transaction dialog request, null when closed.
    var txRequest by remember { mutableStateOf<TxRequest?>(null) }
    fun openNewTransaction(type: Int = 1, accountUid: String? = null, deptUid: String? = null, projectUid: String? = null, personUid: String? = null) {
        txRequest = TxRequest(type, accountUid, deptUid, projectUid, personUid, null)
    }

    // ToastModal.tsx: stays on screen for 3 s, then animates out (anim-pop).
    var toast by remember { mutableStateOf<ToastMessage?>(null) }
    var toastVisible by remember { mutableStateOf(false) }
    LaunchedEffect(state.toast?.id) {
        val t = state.toast ?: return@LaunchedEffect
        toast = t
        toastVisible = true
        delay(3000)
        toastVisible = false
        delay(200)
        toast = null
        vm.consumeMessage()
    }

    // Dialogs get their own window, which reports no insets at all; remember the real ones here.
    val density = LocalDensity.current
    val safeDrawing = WindowInsets.safeDrawing
    val safeInsets = SafeInsets(
        top = with(density) { safeDrawing.getTop(density).toDp() },
        bottom = with(density) { safeDrawing.getBottom(density).toDp() },
        start = with(density) { safeDrawing.getLeft(density, LayoutDirection.Ltr).toDp() },
        end = with(density) { safeDrawing.getRight(density, LayoutDirection.Ltr).toDp() },
    )

    CompositionLocalProvider(LocalSafeInsets provides safeInsets) {
    Box(Modifier.fillMaxSize().background(dk(Tw.Slate50, Tw.Slate950))) {
        Column(Modifier.fillMaxSize()) {
            AppHeader(
                isDark = isDark,
                urgentCount = state.urgentNotifications,
                onBell = { showNotifs = !showNotifs },
                onSettings = { tab = Tab.SETTINGS },
                onToggleTheme = { vm.toggleTheme(isDark) },
            )
            // The page scrolls above the fixed bottom bar (web: main + the 3.5rem spacer in App.tsx).
            Box(Modifier.weight(1f).padding(bottom = bottomNavHeight())) {
                when (tab) {
                    Tab.ACCOUNTS -> AccountsScreen(
                        state, collapsed, vm, ::openNewTransaction,
                        onSelectAccount = { uid ->
                            selectedAccountFilter = uid
                            selectedDeptFilter = null
                            selectedProjectFilter = null
                            txFilterRequestId++
                            tab = Tab.TRANSACTIONS
                        },
                    )
                    Tab.TRANSACTIONS -> TransactionsScreen(
                        state = state,
                        vm = vm,
                        ui = txUi,
                        onEditTransaction = { tx ->
                            txRequest = TxRequest(tx.transactionType, tx.sourceAccountUid, tx.deptUid, tx.projectUid, tx.personUid, tx)
                        },
                    )
                    Tab.PERSONS -> PersonsScreen(state, vm)
                    Tab.DEPTS -> DeptsScreen(state, vm)
                    Tab.SETTINGS -> SettingsScreen(vm, state, themeMode)
                    else -> Box(Modifier.fillMaxSize().padding(rem(1f)), contentAlignment = Alignment.Center) {
                        TwText("بخش «${tab.label}» هنوز در نسخه نیتیو منتقل نشده است.", 0.875f, 1.25f, dk(Tw.Slate500, Tw.Slate400))
                    }
                }
            }
        }
        BottomNav(
            active = tab,
            deptsAttention = state.deptsAttention,
            onTab = { tab = it },
            onFab = { openNewTransaction(1) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        AnimatedVisibility(
            visible = toastVisible && toast != null,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = rem(5f)),
            enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.9f),
            exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.9f),
        ) {
            toast?.let { AppToast(it) }
        }
        // NotificationDropdown.tsx: `fixed top-20 left-1/2` — centred on the screen, so it lives here.
        if (showNotifs) {
            NotificationDropdown(
                items = state.notifications,
                unit = state.effectiveUnit,
                onDismissItem = { vm.dismissNotification(it) },
                onPayInstallment = { showNotifs = false; vm.notPorted("پرداخت قسط") },
                onClose = { showNotifs = false },
            )
        }
        // NewTransactionModal.tsx: `db` is the stored database, the amount field follows formCurrency.
        txRequest?.let { req ->
            NewTransactionDialog(
                db = state.db,
                unit = if (state.effectiveUnit == CurrencyUnit.RIAL) CurrencyUnit.RIAL else CurrencyUnit.TOMAN,
                onClose = { txRequest = null },
                onSave = { tx -> vm.saveTransaction(tx, req.editing != null) },
                onAddSubject = { vm.saveSubject(it) },
                onError = { vm.warn(it) },
                initialType = req.type,
                initialAccountUid = req.accountUid,
                initialDeptUid = req.deptUid,
                initialProjectUid = req.projectUid,
                initialPersonUid = req.personUid,
                editingTransaction = req.editing,
            )
        }
    }
    }
}
