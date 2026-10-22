package com.myaccounting.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The transaction helpers of the original app: UID formats, the mirrored TransactionDetails rows
 * (App.tsx `syncDetailsForTx`), the six-field duplicate check and the negative-balance guard.
 */
class TransactionsTest {

    @Test
    fun newUidUsesTheWebPrefixAndSuffixLength() {
        val uid = Transactions.newUid(1_700_000_000_000L)
        assertTrue(uid, uid.startsWith("tx_"))
        // Date.now().toString(36) + 5 random base-36 characters
        assertEquals(3 + "1700000000000".toLong().toString(36).length + 5, uid.length)
        assertTrue(uid.substring(3), uid.substring(3).all { it in "0123456789abcdefghijklmnopqrstuvwxyz" })

        val subj = Transactions.newSubjectUid(1_700_000_000_000L)
        assertTrue(subj, subj.startsWith("subj_"))
        assertEquals(5 + "1700000000000".toLong().toString(36).length + 5, subj.length)

        assertNotEquals(Transactions.newUid(), Transactions.newUid())
    }

    private fun tx(
        uid: String = "tx_1",
        type: Int = 1,
        value: Double = 100.0,
        source: String? = "acc_a",
        dest: String? = null,
    ) = Transaction(
        uid = uid, transactionType = type, totalValue = value,
        sourceAccountUid = source, destinationAccountUid = dest,
    )

    @Test
    fun syncDetailsMirrorsOneLegPerAccountAndReplacesOldRows() {
        val t = tx()
        var details = Transactions.syncDetails(emptyList(), t)
        assertEquals(1, details.size)
        assertEquals("dt_tx_1_src", details[0].uid)
        assertEquals("tx_1", details[0].transactionUid)
        assertEquals("acc_a", details[0].accountUid)
        assertEquals(100.0, details[0].value, 0.0)
        assertEquals(0, details[0].subType)

        // A transfer gets the destination leg as well.
        val transfer = tx(uid = "tx_2", type = 3, source = "acc_a", dest = "acc_b")
        details = Transactions.syncDetails(details, transfer)
        assertEquals(3, details.size)
        assertEquals(listOf("dt_tx_2_src", "dt_tx_2_dst", "dt_tx_1_src"), details.map { it.uid })
        assertEquals(1, details[1].subType)

        // Re-saving a transaction replaces its own rows, and the new rows are prepended.
        val edited = transfer.copy(totalValue = 250.0)
        details = Transactions.syncDetails(details, edited)
        assertEquals(3, details.size)
        assertEquals(listOf("dt_tx_2_src", "dt_tx_2_dst", "dt_tx_1_src"), details.map { it.uid })
        assertEquals(250.0, details[0].value, 0.0)

        // A transfer without a destination keeps a single leg (the web checks DestinationAccountUID).
        val halfTransfer = tx(uid = "tx_3", type = 3, source = "acc_a", dest = null)
        assertEquals(1, Transactions.syncDetails(emptyList(), halfTransfer).size)
        // No source account at all: no rows.
        assertTrue(Transactions.syncDetails(emptyList(), tx(uid = "tx_4", source = null)).isEmpty())
    }

    @Test
    fun isDuplicateMatchesAllSixFieldsOnRoundedTomans() {
        val existing = Transaction(
            uid = "tx_1", transactionType = 1, totalValue = 250_000.0, persianDate = "1404/01/02",
            subjectUid = "subj_1", sourceAccountUid = "acc_a", personUid = null, deptUid = null,
        )
        val db = AccountingDatabase(transactions = listOf(existing))
        fun dup(
            value: Double = 250_000.0, subject: String? = "subj_1", source: String = "acc_a",
            date: String = "1404/01/02", person: String? = null, dept: String? = null, editing: String? = null,
        ) = Transactions.isDuplicate(db, editing, value, subject, source, date, person, dept)

        assertTrue(dup())
        assertTrue("rounding", dup(value = 250_000.4))
        assertFalse("value", dup(value = 250_001.0))
        assertFalse("subject", dup(subject = "subj_2"))
        assertFalse("source", dup(source = "acc_b"))
        assertFalse("date", dup(date = "1404/01/03"))
        assertFalse("person", dup(person = "prs_1"))
        assertFalse("dept", dup(dept = "dept_1"))
        // Editing the very same transaction must not collide with itself.
        assertFalse("self", dup(editing = "tx_1"))
        // null and '' are the same field value …
        val noSubject = db.copy(transactions = listOf(existing.copy(subjectUid = null, personUid = null, deptUid = null)))
        assertTrue(Transactions.isDuplicate(noSubject, null, 250_000.0, "", "acc_a", "1404/01/02", "", ""))
        // … but a missing subject is not the same as a different one.
        assertFalse("null vs subj_1", dup(subject = null))
    }

    @Test
    fun defaultsAndNegativeBalanceGuard() {
        assertEquals("هزینه", Transactions.defaultDescription(1))
        assertEquals("درآمد", Transactions.defaultDescription(2))
        assertEquals("انتقال وجه", Transactions.defaultDescription(3))
        assertEquals("انتقال وجه", Transactions.defaultDescription(0))

        val balances = mapOf("acc_a" to 100_000.0, "acc_b" to 0.0)
        assertFalse(Transactions.wouldGoNegative(balances, "acc_a", 100_000.0))
        assertTrue(Transactions.wouldGoNegative(balances, "acc_a", 100_001.0))
        assertTrue("unknown account starts at zero", Transactions.wouldGoNegative(balances, "acc_x", 1.0))
        assertFalse("the sign of the stored value is ignored", Transactions.wouldGoNegative(balances, "acc_a", -100_000.0))
    }
}
