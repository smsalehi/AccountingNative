package com.myaccounting.app.domain

import androidx.compose.ui.graphics.vector.PathParser
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The bank logos are vector drawables converted from the original SVGs. A single unsupported path
 * command makes `painterResource` throw at *composition* time (the bank picker crashed with
 * "Unknown command for: R"), so check every path in the APK's drawables up front.
 *
 * The "R" came from aapt2 replacing an oversized path with the literal `STRING_TOO_LARGE`: its
 * string pool cannot hold more than 32767 bytes, so an over-long pathData is also a failure here
 * (bank_melli / bank_tejarat / bank_sepah are shipped as PNGs for that reason).
 */
class VectorPathTest {
    @Test
    fun everyPathDataParses() {
        val dir = File("src/main/res/drawable")
        assertTrue("drawable directory missing: ${dir.absolutePath}", dir.isDirectory)
        val pathData = Regex("pathData=\"([^\"]*)\"")
        val failures = mutableListOf<String>()
        dir.listFiles { f -> f.isFile && f.name.endsWith(".xml") }!!
            .sortedBy { it.name }
            .forEach { file ->
                val text = file.readText()
                pathData.findAll(text).forEachIndexed { index, match ->
                    val data = match.groupValues[1]
                    if (data.length > 32_000) {
                        failures += "${file.name} path #$index: ${data.length} chars (aapt2 keeps at most 32767; " +
                            "it would become STRING_TOO_LARGE) — ship it as a PNG instead"
                    }
                    try {
                        PathParser().parsePathString(data)
                    } catch (e: Exception) {
                        failures += "${file.name} path #$index: ${e.message}\n    ${data.take(160)}"
                    }
                }
            }
        assertTrue("unparseable vector paths:\n" + failures.joinToString("\n"), failures.isEmpty())
    }
}
