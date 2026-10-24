package com.myaccounting.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.myaccounting.app.domain.AccountingDatabase
import com.myaccounting.app.domain.DatabaseCodec
import com.myaccounting.app.domain.DbRows
import com.myaccounting.app.domain.Json
import java.io.File
import java.util.zip.GZIPOutputStream

/**
 * The native app's own database: SQLite, one table per entity. Each row stores its
 * backup-format JSON (DbRows keeps [DatabaseCodec.toJson] the single source of truth), so saving is
 * incremental — only the changed rows are written inside one transaction, instead of re-serialising
 * the whole document like the web's OPFS file.
 *
 * Migration: a legacy `smart_accounting_db_v1.json` file (the web-format store of earlier builds) is
 * imported once and renamed aside. Backups: [exportGzip] gzips the SQLite file itself; the JSON
 * import/export in Settings stays for web interop.
 */
class DatabaseStore(private val context: Context) {

    sealed class LoadResult {
        data class Ok(val db: AccountingDatabase) : LoadResult()
        /** The database could not be read; [backupCopy] (when present) is a file kept aside. */
        data class Corrupt(val backupCopy: File?, val error: String) : LoadResult()
    }

    private val sqliteFile: File get() = File(context.filesDir, FILE_NAME)
    private val legacyFile: File get() = File(context.filesDir, LEGACY_NAME)

    private val helper = object : SQLiteOpenHelper(context, FILE_NAME, null, 1) {
        override fun onCreate(db: SQLiteDatabase) { createSchema(db) }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }

    private val db: SQLiteDatabase by lazy {
        helper.writableDatabase.apply { rawQuery("PRAGMA synchronous=NORMAL", null).use { it.moveToFirst() } }
    }

    fun load(): LoadResult {
        return try {
            // Migration from the legacy single-JSON store (older native builds).
            if (!sqliteFile.exists() || sqliteFile.length() == 0L) {
                if (legacyFile.exists()) {
                    val legacy = DatabaseCodec.fromJson(legacyFile.readText(Charsets.UTF_8)).db
                    fullWrite(legacy)
                    legacyFile.renameTo(File(context.filesDir, "$LEGACY_NAME.migrated"))
                    return LoadResult.Ok(legacy)
                }
                return LoadResult.Ok(emptyDatabase())
            }
            LoadResult.Ok(readAll())
        } catch (e: Exception) {
            val copy = try {
                if (sqliteFile.exists()) {
                    val c = File(context.filesDir, "$FILE_NAME.corrupt-${System.currentTimeMillis()}")
                    sqliteFile.copyTo(c, overwrite = true)
                    c
                } else null
            } catch (_: Exception) { null }
            LoadResult.Corrupt(copy, e.message ?: e.javaClass.simpleName)
        }
    }

    /** Diff-based incremental write: only changed rows are serialized and written, in one transaction. */
    fun save(old: AccountingDatabase?, new: AccountingDatabase) {
        if (old == null) {
            fullWrite(new)
            return
        }
        val newSnap = DbRows.snapshot(new)
        val oldSnap = DbRows.snapshot(old)
        db.beginTransaction()
        try {
            for ((table, rows) in newSnap) {
                val oldRows = oldSnap[table].orEmpty().associateBy { it.uid }
                for (r in rows) {
                    val o = oldRows[r.uid]
                    if (o == null || o.obj != r.obj || o.sort != r.sort) {
                        upsert(table, DbRows.Row(r.uid, r.sort, null, r.json))
                    }
                }
                if (oldRows.isNotEmpty()) {
                    val newUids = rows.mapTo(HashSet()) { it.uid }
                    for (o in oldRows.values) {
                        if (o.uid !in newUids) db.delete("t_" + table, "uid=?", arrayOf(o.uid))
                    }
                }
            }
            writeMeta(new)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** The gzipped SQLite file: the native backup format. */
    fun exportGzip(): ByteArray {
        db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
        val raw = sqliteFile.readBytes()
        val bos = java.io.ByteArrayOutputStream(raw.size / 3 + 1024)
        GZIPOutputStream(bos).use { it.write(raw) }
        return bos.toByteArray()
    }

    /** Restores a gzipped SQLite backup and returns the loaded database (already normalized). */
    fun importGzip(bytes: ByteArray): AccountingDatabase {
        val tmp = File(context.cacheDir, "restore-${System.currentTimeMillis()}.sqlite")
        try {
            java.util.zip.GZIPInputStream(bytes.inputStream()).use { input -> tmp.outputStream().use { input.copyTo(it) } }
            val restored = SQLiteDatabase.openDatabase(tmp.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val root = LinkedHashMap<String, List<Any?>>()
            try {
                for (table in TABLES) {
                    val rows = ArrayList<Any?>()
                    restored.query("t_" + table, arrayOf("json"), null, null, null, null, null)?.use { c ->
                        while (c.moveToNext()) rows.add(Json.parse(c.getString(0)))
                    }
                    root[table] = rows
                }
            } finally {
                restored.close()
            }
            return DbRows.toDatabase(root)
        } finally {
            tmp.delete()
        }
    }

    // --- internals ---------------------------------------------------------------------------------

    private fun readAll(): AccountingDatabase {
        val root = LinkedHashMap<String, List<Any?>>()
        for (table in TABLES) {
            val rows = ArrayList<Any?>()
            db.query("t_" + table, arrayOf("json"), null, null, null, null, null)?.use { c ->
                while (c.moveToNext()) rows.add(Json.parse(c.getString(0)))
            }
            root[table] = rows
        }
        return DbRows.toDatabase(root)
    }

    private fun fullWrite(new: AccountingDatabase) {
        val snap = DbRows.snapshot(new)
        db.beginTransaction()
        try {
            for (table in TABLES) {
                db.delete("t_" + table, null, null)
                for (r in snap[table].orEmpty()) upsert(table, r)
            }
            writeMeta(new)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun upsert(table: String, r: DbRows.Row) {
        db.insertWithOnConflict(
            "t_" + table, null,
            android.content.ContentValues().apply {
                put("uid", r.uid)
                put("sort", r.sort)
                put("json", r.json)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    private fun writeMeta(new: AccountingDatabase) {
        for ((k, v) in mapOf("version" to new.version.toString(), "exportDate" to new.exportDate, "app" to new.app, "appVersion" to new.appVersion)) {
            db.insertWithOnConflict(
                "meta", null,
                android.content.ContentValues().apply { put("k", k); put("v", v) },
                SQLiteDatabase.CONFLICT_REPLACE,
            )
        }
    }

    companion object {
        const val FILE_NAME = "smart_accounting_db.sqlite"
        const val LEGACY_NAME = "smart_accounting_db_v1.json"

        val TABLES = listOf(
            DbRows.T_GROUPS, DbRows.T_ACCOUNTS, DbRows.T_BANKS, DbRows.T_SUBJECTS, DbRows.T_PROJECTS,
            DbRows.T_PERSONS, DbRows.T_PERSON_GROUPS, DbRows.T_FAMILIES, DbRows.T_TRANSACTIONS,
            DbRows.T_DETAILS, DbRows.T_INVOICES, DbRows.T_DEPTS, DbRows.T_CHECKS, DbRows.T_TAGS,
            DbRows.T_CONFIGS, DbRows.T_RATES,
        )

        fun emptyDatabase() = AccountingDatabase(exportDate = java.time.Instant.now().toString())

        private fun createSchema(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS meta (k TEXT PRIMARY KEY, v TEXT)")
            for (table in TABLES) {
                db.execSQL("CREATE TABLE IF NOT EXISTS t_$table (uid TEXT PRIMARY KEY, sort INTEGER NOT NULL DEFAULT 0, json TEXT NOT NULL)")
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS i_tx_sort ON t_${DbRows.T_TRANSACTIONS}(sort)")
        }
    }
}
