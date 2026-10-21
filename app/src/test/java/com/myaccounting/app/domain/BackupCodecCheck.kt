package com.myaccounting.app.domain

import java.util.Base64

/** Plain-JVM checks of BackupCodec (mirrors BackupCodecTest, runnable without JUnit). Returns failure messages. */
fun backupCodecFailures(): List<String> {
    val json = """{"Accounts":[],"Subjects":[],"Transactions":[],"note":"ماشین"}"""
    val fails = ArrayList<String>()
    fun expect(name: String, bytes: ByteArray) {
        val r = try { BackupCodec.decode(bytes) } catch (e: Exception) { "EXC:${e::class.simpleName}" }
        if (r != json) fails.add("$name -> $r")
    }
    val gz = BackupCodec.gzip(json)
    val b64 = Base64.getEncoder().encodeToString(gz)
    expect("raw gzip", gz)
    expect("plain json", json.toByteArray())
    expect("plain json with BOM", ("\uFEFF" + json).toByteArray())
    expect("base64 of gzip (text)", b64.toByteArray())
    expect("base64 of gzip with newlines", b64.chunked(60).joinToString("\n").toByteArray())
    expect("legacy base64(base64(gzip))", Base64.getEncoder().encodeToString(b64.toByteArray()).toByteArray())
    for ((n, b) in listOf("empty" to ByteArray(0), "garbage" to "hello world".toByteArray(), "bad gzip" to byteArrayOf(0x1f, 0x8b.toByte(), 1, 2, 3))) {
        val r = try { BackupCodec.decode(b); "decoded" } catch (e: BackupCodec.UnrecognizedBackupException) { "ok" } catch (e: Exception) { "other:${e::class.simpleName}" }
        if (r != "ok") fails.add("$n should be rejected but got $r")
    }
    val full = DatabaseCodec.toJson(DatabaseCodec.fromJson(json).db)
    if (DatabaseCodec.fromJson(BackupCodec.decode(BackupCodec.gzip(full))).db.copy(exportDate = "") != DatabaseCodec.fromJson(full).db.copy(exportDate = "")) fails.add("db gzip roundtrip")
    return fails
}

fun main() {
    val f = backupCodecFailures()
    println(if (f.isEmpty()) "BackupCodec checks: all passed" else "FAILED:\n" + f.joinToString("\n"))
}
