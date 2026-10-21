package com.myaccounting.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parity with the ORIGINAL TypeScript utilities. src/test/resources/gold_ts.txt was produced by running the original
 * src/utils TypeScript files (bundled with esbuild, TZ=Asia/Tehran) through tools/golden/gold.ts. [goldenLines] runs the same
 * scenarios through the Kotlin port. Regenerate with tools/golden/README.md.
 */
class GoldenParityTest {
    @Test
    fun kotlinPortMatchesOriginalTypeScript() {
        val expected = javaClass.getResourceAsStream("/gold_ts.txt")!!.bufferedReader(Charsets.UTF_8).readText()
            .lines().filter { it.contains('=') }
        val actual = goldenLines().filter { !it.startsWith("roundtrip_equal=") }
        assertEquals(expected.map { it.substringBefore('=') }, actual.map { it.substringBefore('=') })
        for (i in expected.indices) assertEquals("scenario ${expected[i].substringBefore('=')}", expected[i], actual[i])
    }

    @Test
    fun backupRoundTripIsLossless() {
        assertTrue(goldenLines().contains("roundtrip_equal=[true]"))
    }
}
