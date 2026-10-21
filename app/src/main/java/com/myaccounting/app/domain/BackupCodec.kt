package com.myaccounting.app.domain

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Backup file container formats used by the original app (src/utils/folderBackup.ts):
 *  - `*.json.gz`  : raw gzip bytes of the JSON (current format)
 *  - `*.json`     : plain UTF-8 JSON (local auto-backup file)
 *  - legacy       : a file whose TEXT content is base64 of the JSON, or base64 of base64-of-gzip
 * decode() accepts all of them (port of decodeNativeBackupContent + raw gzip sniffing).
 */
object BackupCodec {
    class UnrecognizedBackupException : Exception("فرمت فایل بکاپ شناسایی نشد")

    private fun isGzip(b: ByteArray) = b.size >= 2 && b[0] == 0x1f.toByte() && b[1] == 0x8b.toByte()

    private fun gunzip(bytes: ByteArray): String =
        GZIPInputStream(ByteArrayInputStream(bytes)).use { String(it.readBytes(), Charsets.UTF_8) }

    fun gzip(text: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(text.toByteArray(Charsets.UTF_8)) }
        return bos.toByteArray()
    }

    private fun b64(text: String): ByteArray = Base64.getMimeDecoder().decode(text.trim())

    private fun looksLikeJson(s: String?) = s != null && s.trim().removePrefix("\uFEFF").startsWith("{")

    fun decode(bytes: ByteArray): String {
        if (bytes.isEmpty()) throw UnrecognizedBackupException()
        if (isGzip(bytes)) {
            try {
                val t = gunzip(bytes)
                if (looksLikeJson(t)) return t
            } catch (_: Exception) { /* corrupt gzip → treated as unrecognized below */ }
        }
        val trimmed = String(bytes, Charsets.UTF_8).trim().removePrefix("\uFEFF")
        if (trimmed.isEmpty()) throw UnrecognizedBackupException()
        if (trimmed.startsWith("{")) return trimmed
        // base64(gzip(json)) as text
        try {
            val t = gunzip(b64(trimmed))
            if (looksLikeJson(t)) return t
        } catch (_: Exception) { /* legacy fallbacks */ }
        try {
            val once = String(b64(trimmed), Charsets.UTF_8)
            if (looksLikeJson(once)) return once
            val twice = gunzip(b64(once.trim()))
            if (looksLikeJson(twice)) return twice
        } catch (_: Exception) { /* fall through */ }
        throw UnrecognizedBackupException()
    }
}
