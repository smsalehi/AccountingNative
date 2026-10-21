package com.myaccounting.app.domain

import androidx.compose.ui.graphics.vector.PathParser
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The bank logos are vector drawables converted from the original SVGs. A single unsupported path
 * command makes `painterResource` throw at *composition* time (the bank picker crashed with
 * "Unknown command for: R"), so check every path in the APK's drawables up front.
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
                    try {
                        PathParser().parsePathString(match.groupValues[1])
                    } catch (e: Exception) {
                        failures += "${file.name} path #$index: ${e.message}\n    ${match.groupValues[1].take(160)}"
                    }
                }
            }
        assertTrue("unparseable vector paths:\n" + failures.joinToString("\n"), failures.isEmpty())
    }
}
