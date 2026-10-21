package com.myaccounting.app.domain

import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {
    @Test
    fun allBackupContainerFormatsDecode() {
        val failures = backupCodecFailures()
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}
