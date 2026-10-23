package com.myaccounting.app.ui.checks
import androidx.compose.ui.Modifier
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import com.myaccounting.app.ui.theme.*
@Composable
fun ChecksScreen(s: com.myaccounting.app.viewmodel.MainViewModel.UiState, v: com.myaccounting.app.viewmodel.MainViewModel) {
    if (!s.loaded) return
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = rem(1f), end = rem(1f), top = rem(1f), bottom = rem(4f)), verticalArrangement = Arrangement.spacedBy(rem(1.25f))) {
        item(key = "h") { TwText("چک‌ها", 1.1f, 1.65f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold) }
    }
}
