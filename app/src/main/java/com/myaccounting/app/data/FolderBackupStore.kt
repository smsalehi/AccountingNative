package com.myaccounting.app.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.myaccounting.app.domain.AccountingDatabase
import com.myaccounting.app.domain.BackupCodec
import com.myaccounting.app.domain.DatabaseCodec
import java.io.FileNotFoundException

/**
 * Native half of folderBackup.ts + autoBackup.ts: two persisted SAF backup folders —
 * 'google' (Drive folder, one gzip auto-backup per day, fixed file name) and 'local'
 * (internal-storage folder, raw auto-backup on every app close, fixed file name).
 * Manual «بکاپ دستی» files are timestamped gzip archives.
 */
object FolderBackupStore {

    enum class Kind { GOOGLE, LOCAL }

    data class Cfg(
        val kind: Kind,
        val title: String,
        val subtitle: String,
        val autoFile: String,
        val manualPrefix: String,
        val prefsPrefix: String,
    )

    val GOOGLE = Cfg(Kind.GOOGLE, "بکاپ‌گیری Google Drive", "پوشه Google Drive — بکاپ خودکار روزانه با نام ثابت",
        "google-backup-auto-accounting.gz", "google", "bk_google")
    val LOCAL = Cfg(Kind.LOCAL, "بکاپ حافظه داخلی", "پوشه حافظه داخلی — بکاپ خودکار هنگام بستن برنامه با نام ثابت",
        "local-backup-auto-accounting.json", "local", "bk_local")

    /** A new automatic backup replaces the previous one only when it is NOT more than 20% smaller. */
    const val AUTO_GUARD_RATIO = 0.8
    private const val DRIVE_AUTHORITY = "com.google.android.apps.docs.storage"

    data class BackupFileEntry(val name: String, val size: Long, val lastModified: Long, val uri: Uri?)

    fun isDriveFolderUri(uri: String): Boolean =
        Regex("^content://([^/]+)").find(uri.trim())?.groupValues?.get(1) == DRIVE_AUTHORITY

    /** folderBackup.ts manualFileName: `prefix_yyyy-MM-dd_HH-mm-ss.json.gz` (Gregorian, local time). */
    fun manualFileName(prefix: String, at: java.util.Calendar = java.util.Calendar.getInstance()): String {
        fun p(n: Int) = n.toString().padStart(2, '0')
        val date = "${at.get(java.util.Calendar.YEAR)}-${p(at.get(java.util.Calendar.MONTH) + 1)}-${p(at.get(java.util.Calendar.DAY_OF_MONTH))}"
        val time = "${p(at.get(java.util.Calendar.HOUR_OF_DAY))}-${p(at.get(java.util.Calendar.MINUTE))}-${p(at.get(java.util.Calendar.SECOND))}"
        return "${prefix}_${date}_$time.json.gz"
    }

    /** folderBackup.ts decideReplace: replace when no previous file or not >20% smaller. */
    fun decideReplace(prevSize: Long?, newSize: Long): Boolean =
        prevSize == null || prevSize <= 0 || newSize >= prevSize * AUTO_GUARD_RATIO

    /** autoBackup.ts hasUserData: rates/configs alone are not «data». */
    fun hasUserData(db: AccountingDatabase): Boolean =
        db.accountGroups.isNotEmpty() || db.accounts.isNotEmpty() || db.banks.isNotEmpty() ||
            db.subjects.isNotEmpty() || db.projects.isNotEmpty() || db.persons.isNotEmpty() ||
            db.personGroups.isNotEmpty() || db.families.isNotEmpty() || db.transactions.isNotEmpty() ||
            db.transactionDetails.isNotEmpty() || db.invoices.isNotEmpty() || db.depts.isNotEmpty() ||
            db.checks.isNotEmpty() || db.tags.isNotEmpty() || db.configs.isNotEmpty()

    /** Serializes + gzips; returns the payload and its raw gzip size (the guard compares these). */
    fun gzipPayload(db: AccountingDatabase): Pair<ByteArray, Int> {
        val bytes = BackupCodec.gzip(DatabaseCodec.toJson(db))
        return bytes to bytes.size
    }

    fun writeGzip(context: Context, folder: Uri, fileName: String, gzipBytes: ByteArray): Boolean {
        return writeToDoc(context, folder, fileName, "application/gzip") { out -> out.write(gzipBytes) }
    }

    fun writeText(context: Context, folder: Uri, fileName: String, text: String): Boolean {
        val bytes = text.toByteArray(Charsets.UTF_8)
        return writeToDoc(context, folder, fileName, "application/json") { out -> out.write(bytes) }
    }

    private fun writeToDoc(context: Context, folder: Uri, fileName: String, mime: String, write: (java.io.OutputStream) -> Unit): Boolean {
        return try {
            val resolver = context.contentResolver
            val docUri = findChild(context, folder, fileName)
                ?: DocumentsContract.createDocument(resolver, folder, mime, fileName)
                ?: return false
            resolver.openFileDescriptor(docUri, "w")?.use { pfd ->
                java.io.FileOutputStream(pfd.fileDescriptor).use(write)
            } ?: return false
            true
        } catch (_: Exception) {
            false
        }
    }

    fun fileSize(context: Context, folder: Uri, fileName: String): Long? = try {
        val docUri = findChild(context, folder, fileName) ?: return null
        context.contentResolver.openFileDescriptor(docUri, "r")?.use { it.statSize }
    } catch (_: Exception) {
        null
    }

    fun listBackupFiles(context: Context, folder: Uri): List<BackupFileEntry> {
        return try {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folder, DocumentsContract.getDocumentId(folder))
            val out = ArrayList<BackupFileEntry>()
            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_SIZE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                ),
                null, null, null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(1) ?: continue
                    if (!name.endsWith(".gz") && name != LOCAL.autoFile) continue
                    val size = if (c.isNull(2)) -1L else c.getLong(2)
                    val modified = if (c.isNull(3)) 0L else c.getLong(3)
                    out.add(BackupFileEntry(name, size, modified, DocumentsContract.buildDocumentUriUsingTree(folder, c.getString(0))))
                }
            }
            out.sortedByDescending { it.lastModified }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Reads any backup file (.gz gzip / plain json) and returns the JSON text. */
    fun readBackupText(context: Context, folder: Uri, fileName: String): String? = try {
        val docUri = findChild(context, folder, fileName) ?: return null
        val bytes = context.contentResolver.openInputStream(docUri)?.use { it.readBytes() } ?: return null
        BackupCodec.decode(bytes)
    } catch (_: Exception) {
        null
    }

    fun deleteFile(context: Context, folder: Uri, fileName: String): Boolean = try {
        val docUri = findChild(context, folder, fileName) ?: return false
        DocumentsContract.deleteDocument(context.contentResolver, docUri)
    } catch (_: Exception) {
        false
    }

    private fun findChild(context: Context, folder: Uri, fileName: String): Uri? = try {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folder, DocumentsContract.getDocumentId(folder))
        context.contentResolver.query(
            childrenUri,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                if (c.getString(1) == fileName) return DocumentsContract.buildDocumentUriUsingTree(folder, c.getString(0))
            }
            null
        }
    } catch (_: FileNotFoundException) {
        null
    } catch (_: Exception) {
        null
    }
}
