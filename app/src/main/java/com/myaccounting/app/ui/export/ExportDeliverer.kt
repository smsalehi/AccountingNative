package com.myaccounting.app.ui.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.myaccounting.app.data.ExportStore
import com.myaccounting.app.domain.AccountingDatabase
import com.myaccounting.app.domain.CurrencyUnit
import com.myaccounting.app.domain.ExportFiles
import com.myaccounting.app.domain.Transaction
import com.myaccounting.app.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * exportDelivery.ts deliverExportFile for native: the first export asks for the target folder once
 * (SAF tree picker) with the same info toast, then every export is written directly into it.
 */
class ExportDeliverer(
    private val context: Context,
    private val vm: MainViewModel,
    private val scope: CoroutineScope,
    private val launchFolderPicker: () -> Unit,
    private val setPending: (PendingExport?) -> Unit,
) {
    data class PendingExport(val fileName: String, val content: String)

    /**
     * TransactionsView.tsx exports both buttons through the *filtered* variants
     * (exportTransactionsToExcelFiltered / exportTransactionsPDFFiltered).
     * [transactions] must be the raw (toman) rows; [db] is the raw stored database.
     */
    fun exportTransactionsExcel(transactions: List<Transaction>, db: AccountingDatabase, unit: CurrencyUnit) {
        deliver(
            ExportFiles.todayFileName("گزارش_تراکنش‌ها_فیلتر", "csv"),
            ExportFiles.transactionsCsvFiltered(transactions, db, unit),
        )
    }

    fun exportTransactionsPdf(transactions: List<Transaction>, db: AccountingDatabase, unit: CurrencyUnit) {
        deliver(
            ExportFiles.todayFileName("گزارش_تراکنش‌ها_فیلتر", "html"),
            ExportFiles.transactionsPdfHtmlFiltered(transactions, db, unit),
        )
    }

    /** deliverExportFile: never throws, toasts exactly like the original. */
    fun deliver(fileName: String, content: String) {
        scope.launch {
            val saved = vm.exportsFolderUri()
            if (saved.isEmpty()) {
                vm.info("در پنجره بعدی، محل ذخیره فایل‌ها را انتخاب کنید.")
                setPending(PendingExport(fileName, content))
                launchFolderPicker()
            } else {
                writeTo(Uri.parse(saved), PendingExport(fileName, content))
            }
        }
    }

    /** Called after a fresh pick: persist the permission + uri, then flush the pending export. */
    fun onFolderPicked(uri: Uri?, pending: PendingExport?) {
        if (uri == null) {
            setPending(null)
            vm.warn("بدون انتخاب پوشه، فایلی ذخیره نشد.")
            return
        }
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (_: Exception) {
            // Some providers only grant one direction; keep going with what we have.
        }
        vm.saveExportsFolder(uri.toString())
        val p = pending
        setPending(null)
        if (p != null) {
            scope.launch { writeTo(uri, p) }
        }
    }

    private suspend fun writeTo(folder: Uri, p: PendingExport) {
        val ok = withContext(Dispatchers.IO) {
            ExportStore.write(context, folder, p.fileName, p.content.toByteArray(Charsets.UTF_8))
        }
        if (ok) vm.success("ذخیره شد: ${p.fileName}") else vm.warn("ذخیره فایل ناموفق بود. لطفاً دوباره تلاش کنید.")
    }
}

@Composable
fun rememberExportDeliverer(vm: MainViewModel): ExportDeliverer {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<ExportDeliverer.PendingExport?>(null) }
    var delivererRef by remember { mutableStateOf<ExportDeliverer?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val p = pending
        pending = null
        delivererRef?.onFolderPicked(uri, p)
    }
    val deliverer = remember {
        ExportDeliverer(
            context = context,
            vm = vm,
            scope = scope,
            launchFolderPicker = { picker.launch(null) },
            setPending = { pending = it },
        )
    }
    delivererRef = deliverer
    return deliverer
}
