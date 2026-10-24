package com.myaccounting.app.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.FileOutputStream

/**
 * The native half of exportDelivery.ts: write export files (CSV / report HTML) into the
 * user's chosen SAF folder. An existing file with the same name is overwritten in place
 * (the web's File System Access writable behaves the same way).
 */
object ExportStore {

    fun mimeOf(fileName: String): String =
        if (fileName.endsWith(".html", ignoreCase = true)) "text/html" else "text/csv"

    fun write(context: Context, folder: Uri, fileName: String, bytes: ByteArray): Boolean = try {
        val resolver = context.contentResolver
        val docUri = findChild(resolver, folder, fileName)
            ?: DocumentsContract.createDocument(resolver, folder, mimeOf(fileName), fileName)
            ?: return false
        resolver.openFileDescriptor(docUri, "w")?.use { pfd ->
            FileOutputStream(pfd.fileDescriptor).use { it.write(bytes) }
        } ?: return false
        true
    } catch (_: Exception) {
        false
    }

    private fun findChild(resolver: android.content.ContentResolver, folder: Uri, fileName: String): Uri? = try {
        resolver.queryChildDocuments(
            folder,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            ),
            null,
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                if (c.getString(1) == fileName) {
                    return DocumentsContract.buildDocumentUriUsingTree(folder, c.getString(0))
                }
            }
            null
        }
    } catch (_: Exception) {
        null
    }
}
