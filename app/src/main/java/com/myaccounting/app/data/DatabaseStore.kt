package com.myaccounting.app.data

import android.content.Context
import com.myaccounting.app.domain.AccountingDatabase
import com.myaccounting.app.domain.DatabaseCodec
import java.io.File
import java.io.FileOutputStream

/**
 * Persists the whole database as one JSON file in app-private storage — the same document model the original app kept in
 * the browser's OPFS (format = backup format). Writes are atomic (temp file + fsync + rename), so a crash mid-save can never
 * leave a half-written database.
 */
class DatabaseStore(private val context: Context) {
    private val file: File get() = File(context.filesDir, FILE_NAME)

    sealed class LoadResult {
        data class Ok(val db: AccountingDatabase) : LoadResult()
        /** The file exists but could not be parsed. It was copied to [backupCopy] and left untouched. */
        data class Corrupt(val backupCopy: File?, val error: String) : LoadResult()
    }

    fun load(): LoadResult {
        val f = file
        if (!f.exists()) return LoadResult.Ok(emptyDatabase())
        return try {
            LoadResult.Ok(DatabaseCodec.fromJson(f.readText(Charsets.UTF_8)).db)
        } catch (e: Exception) {
            val copy = File(context.filesDir, "$FILE_NAME.corrupt-${System.currentTimeMillis()}")
            val saved = try { f.copyTo(copy, overwrite = true) } catch (_: Exception) { null }
            LoadResult.Corrupt(saved, e.message ?: e.javaClass.simpleName)
        }
    }

    fun save(db: AccountingDatabase) {
        val json = DatabaseCodec.toJson(db)
        val tmp = File(context.filesDir, "$FILE_NAME.tmp")
        FileOutputStream(tmp).use { out ->
            out.write(json.toByteArray(Charsets.UTF_8))
            out.fd.sync()
        }
        if (!tmp.renameTo(file)) {
            // renameTo can fail on exotic file systems: fall back to copy, never delete the old file first
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }

    companion object {
        const val FILE_NAME = "smart_accounting_db_v1.json"
        fun emptyDatabase() = AccountingDatabase(exportDate = java.time.Instant.now().toString())
    }
}
