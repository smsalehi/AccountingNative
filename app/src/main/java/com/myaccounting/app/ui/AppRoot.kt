package com.myaccounting.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myaccounting.app.data.ThemeMode
import com.myaccounting.app.ui.accounts.AccountsScreen
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem
import com.myaccounting.app.viewmodel.MainViewModel

/** App shell = App.tsx layout: sticky header, page background, fixed bottom bar. Only the Accounts tab is ported. */
@Composable
fun AppRoot(vm: MainViewModel, isDark: Boolean, themeMode: ThemeMode) {
    val state by vm.state.collectAsStateWithLifecycle()
    val collapsed by vm.collapsedGroups.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.ACCOUNTS) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    Box(Modifier.fillMaxSize().background(dk(Tw.Slate50, Tw.Slate950))) {
        Column(Modifier.fillMaxSize()) {
            AppHeader(
                isDark = isDark,
                urgentCount = state.urgentNotifications,
                onBell = { vm.notPorted("اعلان‌ها") },
                onSettings = { tab = Tab.SETTINGS },
                onToggleTheme = { vm.toggleTheme(isDark) },
            )
            Box(Modifier.weight(1f)) {
                when (tab) {
                    Tab.ACCOUNTS -> AccountsScreen(state, collapsed, vm)
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
            onFab = { vm.notPorted("ثبت تراکنش جدید") },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = rem(5f)))
    }
}
