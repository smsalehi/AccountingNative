package com.myaccounting.app.ui.settings

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.myaccounting.app.R
import com.myaccounting.app.data.AppPrefs
import com.myaccounting.app.data.FolderBackupStore
import com.myaccounting.app.domain.JalaliDate
import com.myaccounting.app.ui.components.ConfirmDialog
import com.myaccounting.app.ui.components.ConfirmVariant
import com.myaccounting.app.ui.components.LucideIcon
import com.myaccounting.app.ui.components.TwShadow
import com.myaccounting.app.ui.components.cssShadow
import com.myaccounting.app.ui.theme.FW
import com.myaccounting.app.ui.theme.Tw
import com.myaccounting.app.ui.theme.TwText
import com.myaccounting.app.ui.theme.a
import com.myaccounting.app.ui.theme.cssBorder
import com.myaccounting.app.ui.theme.dk
import com.myaccounting.app.ui.theme.rem
import com.myaccounting.app.viewmodel.MainViewModel
import java.util.Locale

/**
 * FolderBackup.tsx ported: two sections (google = daily gzip auto-backup, local = raw auto-backup
 * on close), each with connect/disconnect, manual backup, the 20%-guard banner and a restore list.
 */
@Composable
fun FolderBackupSections(vm: MainViewModel) {
    val google by vm.backupGoogle.collectAsState()
    val local by vm.backupLocal.collectAsState()
    val context = LocalContext.current

    var pendingKind by remember { mutableStateOf<String?>(null) }
    var runBackupAfterPick by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val kind = pendingKind
        pendingKind = null
        if (uri != null && kind != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            } catch (e: Exception) {
                com.myaccounting.app.data.AppLog.error(
                    com.myaccounting.app.data.AppLog.Category.BACKUP,
                    "ثبت مجوز پایدار پوشه بکاپ ناموفق بود", e.message,
                )
            }
            vm.saveBackupFolder(kind, uri, folderDisplayName(context, uri))
            if (runBackupAfterPick) vm.manualFolderBackup(kind, uri.toString())
        }
        runBackupAfterPick = false
    }

    fun connect(kind: String, backupNow: Boolean) {
        pendingKind = kind
        runBackupAfterPick = backupNow
        picker.launch(null)
    }

    var restoreTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // kind to fileName

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(rem(1f))) {
        BackupSection(vm, FolderBackupStore.GOOGLE, google,
            onConnect = { connect("google", false) },
            // وقتی پوشه متصل است، «بکاپ دستی» مستقیم می‌نویسد و دوباره پوشه نمی‌پرسد
            onBackupNow = { if (google.connected) vm.manualFolderBackup("google") else connect("google", true) },
            onRestore = { restoreTarget = "google" to it },
        )
        BackupSection(vm, FolderBackupStore.LOCAL, local,
            onConnect = { connect("local", false) },
            onBackupNow = { if (local.connected) vm.manualFolderBackup("local") else connect("local", true) },
            onRestore = { restoreTarget = "local" to it },
        )
    }

    restoreTarget?.let { (kind, fileName) ->
        ConfirmDialog(
            message = "بازیابی «$fileName» انجام شود؟ تمام داده‌های فعلی با محتوای این بکاپ جایگزین می‌شود.",
            onConfirm = { vm.restoreFromFolder(kind, fileName); restoreTarget = null },
            onCancel = { restoreTarget = null },
            confirmLabel = "بازیابی",
            variant = ConfirmVariant.WARNING,
        )
    }
}

private fun folderDisplayName(context: android.content.Context, treeUri: Uri): String = try {
    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
    context.contentResolver.query(docUri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else ""
    } ?: ""
} catch (_: Exception) {
    ""
}

@Composable
private fun BackupSection(
    vm: MainViewModel,
    cfg: FolderBackupStore.Cfg,
    st: AppPrefs.BackupFolderState,
    onConnect: () -> Unit,
    onBackupNow: () -> Unit,
    onRestore: (String) -> Unit,
) {
    val card = RoundedCornerShape(rem(1f))
    var showFiles by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val files = remember(showFiles, st.uri, st.lastSync) {
        if (showFiles && st.connected) FolderBackupStore.listBackupFiles(context, Uri.parse(st.uri)) else emptyList()
    }

    Column(
        Modifier.fillMaxWidth()
            .background(dk(Tw.White, Tw.Slate900), card)
            .border(cssBorder(), dk(Tw.Slate200.a(0.8f), Tw.Slate800), card)
            .padding(rem(1f)),
        verticalArrangement = Arrangement.spacedBy(rem(0.625f)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(rem(0.625f))) {
            Box(
                Modifier.size(rem(2f))
                    .background(if (cfg.kind == FolderBackupStore.Kind.GOOGLE) dk(Tw.Sky100, Tw.Sky950) else dk(Tw.Emerald100, Tw.Emerald950), RoundedCornerShape(rem(0.625f))),
                contentAlignment = Alignment.Center,
            ) {
                LucideIcon(
                    if (cfg.kind == FolderBackupStore.Kind.GOOGLE) R.drawable.ic_lucide_cloud else R.drawable.ic_lucide_save,
                    1f,
                    if (cfg.kind == FolderBackupStore.Kind.GOOGLE) dk(Tw.Sky600, Tw.Sky400) else dk(Tw.Emerald600, Tw.Emerald400),
                )
            }
            Column(Modifier.weight(1f)) {
                TwText(cfg.title, 0.875f, 1.25f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Bold, maxLines = 1)
                TwText(cfg.subtitle, 0.6875f, 1f, Tw.Slate400, maxLines = 2)
            }
        }

        // ---- status ---------------------------------------------------------------------------------
        if (st.connected) {
            StatusRow("پوشه:", st.folderName.ifEmpty { "متصل" })
            if (st.lastSync.isNotEmpty()) StatusRow("آخرین بکاپ:", st.lastSync)
            if (st.blocked.isNotEmpty()) {
                val banner = RoundedCornerShape(rem(0.75f))
                TwText(
                    st.blocked, 0.6875f, 1.25f, dk(Tw.Amber800, Tw.Amber300), maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                        .background(dk(Tw.Amber50, Tw.Amber950.a(0.4f)), banner)
                        .border(cssBorder(), dk(Tw.Amber200, Tw.Amber900), banner)
                        .padding(rem(0.625f)),
                )
            }
        } else {
            TwText("پوشه‌ای متصل نیست؛ برای شروع یک پوشه انتخاب کنید.", 0.75f, 1.25f, Tw.Slate500, maxLines = 1)
        }

        // ---- actions ----------------------------------------------------------------------------------
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(rem(0.5f))) {
            SectionButton(
                if (st.connected) "تغییر پوشه" else "اتصال پوشه",
                dk(Tw.Sky50, Tw.Sky950.a(0.6f)), dk(Tw.Sky600, Tw.Sky400), dk(Tw.Sky200, Tw.Sky800), Modifier.weight(1f), onConnect,
            )
            SectionButton(
                "بکاپ دستی",
                Tw.Sky600, Tw.White, null, Modifier.weight(1f), onBackupNow,
            )
            if (st.connected) {
                SectionButton(
                    "قطع اتصال",
                    dk(Tw.Rose50, Tw.Rose950.a(0.3f)), dk(Tw.Rose600, Tw.Rose300), dk(Tw.Rose200, Tw.Rose900.a(0.5f)), Modifier.weight(1f),
                ) { vm.disconnectBackup(if (cfg.kind == FolderBackupStore.Kind.GOOGLE) "google" else "local") }
            }
        }

        // ---- restore list ------------------------------------------------------------------------------
        if (st.connected) {
            Row(
                Modifier.fillMaxWidth()
                    .clickable(remember { MutableInteractionSource() }, null) { showFiles = !showFiles },
                horizontalArrangement = Arrangement.spacedBy(rem(0.375f)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LucideIcon(if (showFiles) R.drawable.ic_lucide_chevron_up else R.drawable.ic_lucide_chevron_down, 0.875f, dk(Tw.Slate500, Tw.Slate400))
                TwText("فایل‌های بکاپ و بازیابی", 0.6875f, 1f, dk(Tw.Slate500, Tw.Slate400), weight = FW.Semibold, maxLines = 1)
            }
            if (showFiles) {
                if (files.isEmpty()) {
                    TwText("فایلی در این پوشه پیدا نشد.", 0.6875f, 1f, Tw.Slate400, maxLines = 1)
                } else {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(rem(0.375f))) {
                        for (f in files) {
                            Row(
                                Modifier.fillMaxWidth()
                                    .background(dk(Tw.Slate50, Tw.Slate800.a(0.7f)), RoundedCornerShape(rem(0.625f)))
                                    .padding(horizontal = rem(0.625f), vertical = rem(0.375f)),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(rem(0.5f)),
                            ) {
                                LucideIcon(R.drawable.ic_lucide_archive, 0.875f, Tw.Slate400)
                                Column(Modifier.weight(1f)) {
                                    TwText(f.name, 0.6875f, 1f, dk(Tw.Slate800, Tw.Slate100), weight = FW.Semibold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    TwText(
                                        formatSize(f.size) + if (f.lastModified > 0) " • " + faTime(f.lastModified) else "",
                                        0.625f, 0.875f, Tw.Slate400, maxLines = 1,
                                    )
                                }
                                Box(
                                    Modifier.clickable(remember { MutableInteractionSource() }, null) { onRestore(f.name) }
                                        .padding(horizontal = rem(0.5f), vertical = rem(0.25f)),
                                ) {
                                    TwText("بازیابی", 0.6875f, 1f, dk(Tw.Sky600, Tw.Sky400), weight = FW.Bold, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(rem(0.375f))) {
        TwText(label, 0.6875f, 1f, Tw.Slate400, maxLines = 1)
        TwText(value, 0.6875f, 1f, dk(Tw.Slate700, Tw.Slate200), weight = FW.Semibold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SectionButton(text: String, bg: Color, fg: Color, border: Color?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(rem(0.75f))
    Row(
        modifier.cssShadow(TwShadow.xs(), rem(0.75f))
            .background(bg, shape)
            .then(if (border != null) Modifier.border(cssBorder(), border, shape) else Modifier)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = rem(0.5f), vertical = rem(0.5f)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TwText(text, 0.75f, 1f, fg, weight = FW.Bold, maxLines = 1)
    }
}

/** folderBackup.ts formatFileSize (fully Persian, decimal ٫). */
private fun formatSize(bytes: Long): String {
    if (bytes < 0) return "—"
    val fa = { n: Double -> JalaliDate.toPersianDigits(String.format(Locale.US, "%.1f", n)).replace('.', '٫') }
    return when {
        bytes < 1024 -> JalaliDate.toPersianDigits(bytes.toString()) + " بایت"
        bytes < 1024 * 1024 -> fa(bytes / 1024.0) + " کیلوبایت"
        bytes < 1024L * 1024 * 1024 -> fa(bytes / (1024.0 * 1024)) + " مگابایت"
        else -> fa(bytes / (1024.0 * 1024 * 1024)) + " گیگابایت"
    }
}

private fun faTime(ts: Long): String {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = ts
    val j = JalaliDate.toJalali(cal.timeInMillis)
    return JalaliDate.toPersianDigits("${j.y}/${j.m}/${j.d}")
}
