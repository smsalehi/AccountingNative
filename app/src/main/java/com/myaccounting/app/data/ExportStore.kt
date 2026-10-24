package com.myaccounting.app.data

import android.content.ContentResolver
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

    private val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
    )

    fun mimeOf(fileName: String): String =
        if (fileName.endsWith(".html", ignoreCase = true)) "text/html" else "text/csv"

    fun write(context: Context, folder: Uri, fileName: String, bytes: ByteArray): Boolean {
        return try {
            val resolver = context.contentResolver
            val docUri = findChild(resolver, folder, fileName)
                ?: DocumentsContract.createDocument(resolver, folder, mimeOf(fileName), fileName)
            if (docUri == null) return false
            val pfd = resolver.openFileDescriptor(docUri, "w") ?: return false
            pfd.use {
                FileOutputStream(it.fileDescriptor).use { out -> out.write(bytes) }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun findChild(resolver: ContentResolver, folder: Uri, fileName: String): Uri? {
        return try {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(folder, DocumentsContract.getDocumentId(folder))
            val children = resolver.query(childrenUri, projection, null, null, null)
            var found: Uri? = null
            if (children != null) {
                children.use { c ->
                    while (c.moveToNext()) {
                        if (c.getString(1) == fileName) {
                            found = DocumentsContract.buildDocumentUriUsingTree(folder, c.getString(0))
                            break
                        }
                    }
                }
            }
            found
        } catch (_: Exception) {
            null
        }
    }
}
