package com.myaccounting.app.domain

import kotlin.random.Random

/**
 * Transaction helpers of the original app (App.tsx `syncDetailsForTx`, NewTransactionModal's duplicate
 * check and UID format). The rows of TransactionDetails mirror every transaction: one leg for the source
 * account and — for a transfer — a second leg for the destination account.
 */
object Transactions {
    /** `tx_` + Date.now().toString(36) + 5 random base-36 characters (NewTransactionModal.tsx). */
    fun newUid(now: Long = System.currentTimeMillis()): String {
        val alphabet = "0123456789abcdefghijklmnopqrstuvwxyz"
        val suffix = (1..5).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
        return "tx_" + now.toString(36) + suffix
    }

    /** `subj_` + Date.now().toString(36) + 5 random base-36 characters (NewSubjectModal.tsx). */
    fun newSubjectUid(now: Long = System.currentTimeMillis()): String = "subj_" + uidSuffix(now)

    private fun uidSuffix(now: Long): String {
        val alphabet = "0123456789abcdefghijklmnopqrstuvwxyz"
        return now.toString(36) + (1..5).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
    }

    /** App.tsx: the details table of [tx] replaced by its (one or two) mirrored rows, prepended. */
    fun syncDetails(prevDetails: List<TransactionDetail>, tx: Transaction): List<TransactionDetail> {
        val without = prevDetails.filter { it.transactionUid != tx.uid }
        val rows = ArrayList<TransactionDetail>(3)
        tx.sourceAccountUid?.let {
            rows += TransactionDetail(uid = "dt_${tx.uid}_src", transactionUid = tx.uid, accountUid = it, value = tx.totalValue, subType = 0)
        }
        if (tx.transactionType == 3 && !tx.destinationAccountUid.isNullOrEmpty()) {
            rows += TransactionDetail(uid = "dt_${tx.uid}_dst", transactionUid = tx.uid, accountUid = tx.destinationAccountUid, value = tx.totalValue, subType = 1)
        }
        return rows + without
    }

    /**
     * NewTransactionModal.tsx `checkDuplicate`: a six-field match on rounded toman amounts —
     * value, subject, source account, Persian date, person and loan.
     */
    fun isDuplicate(
        db: AccountingDatabase,
        editingUid: String?,
        totalTomans: Double,
        subjectUid: String?,
        sourceAccountUid: String,
        persianDate: String,
        personUid: String?,
        deptUid: String?,
    ): Boolean {
        val newValue = Math.round(totalTomans).toDouble()
        return db.transactions.any { t ->
            if (t.uid == editingUid) return@any false
            val tValue = Math.round(t.totalValue).toDouble()
            tValue == newValue &&
                (t.subjectUid ?: "") == (subjectUid ?: "") &&
                (t.sourceAccountUid ?: "") == (sourceAccountUid ?: "") &&
                (t.persianDate ?: "") == (persianDate ?: "") &&
                (t.personUid ?: "") == (personUid ?: "") &&
                (t.deptUid ?: "") == (deptUid ?: "")
        }
    }

    /** `desc || (type === 1 ? 'هزینه' : type === 2 ? 'درآمد' : 'انتقال وجه')` */
    fun defaultDescription(type: Int): String = when (type) {
        1 -> "هزینه"
        2 -> "درآمد"
        else -> "انتقال وجه"
    }

    /** NewTransactionModal: a negative source balance asks for a confirmation first (App.tsx). */
    fun wouldGoNegative(balances: Map<String, Double>, sourceAccountUid: String, totalValue: Double): Boolean {
        val current = balances[sourceAccountUid] ?: 0.0
        return current - Math.abs(totalValue) < 0
    }
}
