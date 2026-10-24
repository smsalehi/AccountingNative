package com.myaccounting.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myaccounting.app.data.ThemeMode
import com.myaccounting.app.ui.AppRoot
import com.myaccounting.app.ui.theme.AppTheme
import com.myaccounting.app.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onStop() {
        super.onStop()
        // autoBackup.ts: auto-backup on app close (google daily + local raw, both 20%-guarded).
        viewModel.runAutoBackups(MainViewModel.AutoTrigger.EXIT)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val mode by viewModel.theme.collectAsStateWithLifecycle()
            val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
            val isDark = when (mode) { ThemeMode.DARK -> true; ThemeMode.LIGHT -> false; ThemeMode.SYSTEM -> isSystemInDarkTheme() }

            // status-bar icons follow the theme; the header paints the bar area itself (edge-to-edge)
            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    navigationBarStyle = if (isDark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                )
                onDispose { }
            }

            // Persian UI: right-to-left (original: <html lang="fa" dir="rtl">)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                AppTheme(isDark, fontScale) { AppRoot(viewModel, isDark, mode) }
            }
        }
    }
}
