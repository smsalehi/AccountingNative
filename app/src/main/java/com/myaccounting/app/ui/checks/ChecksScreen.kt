package com.myaccounting.app.ui.checks
import com.myaccounting.app.ui.theme.rem
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import androidx.compose.ui.graphics.Color
package com.myaccounting.app.ui.checks
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
@Composable
fun ChecksScreen(s: com.myaccounting.app.viewmodel.MainViewModel.UiState, v: com.myaccounting.app.viewmodel.MainViewModel) {
    if (!s.loaded) return
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = rem(1f), end = rem(1f), top = rem(1f), bottom = rem(4f))) {
        item(key = "h") { com.myaccounting.app.ui.theme.TwText("چک‌ها و یادداشت‌ها", 1.1f, 1.65f, com.myaccounting.app.ui.theme.dk(com.myaccounting.app.ui.theme.Tw.Slate800, com.myaccounting.app.ui.theme.Tw.Slate100), weight = com.myaccounting.app.ui.theme.FW.Bold) }
    }
}
