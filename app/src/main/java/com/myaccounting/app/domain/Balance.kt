package com.myaccounting.app.domain

/** Number(x) of JavaScript for the value shapes that can occur in a backup. */
internal fun jsNumber(v: Any?): Double = when (v) {
    null -> 0.0
    is Double -> v
    is Int -> v.toDouble()
    is Long -> v.toDouble()
    is Boolean -> if (v) 1.0 else 0.0
    is String -> { val t = v.trim(); if (t.isEmpty()) 0.0 else t.toDoubleOrNull() ?: Double.NaN }
    else -> Double.NaN
}

internal fun Double.isFiniteNum(): Boolean = !this.isNaN() && !this.isInfinite()

/**
 * Port of src/utils/balance.ts.
 * balance = OldBalance + movement of all transactions. Types: 0/2 = +, 1 = -, 3 = per-leg SubType (1 = +, 0 = -).
 * Hidden transactions/accounts count. A transaction with valid legs is computed from the legs only; otherwise
 * from the header TotalValue (abs) using Source/DestinationAccountUID.
 */
object Balance {
    private class Leg(val accountUid: String, val value: Double, val subType: Int)

    fun computeAccountBalances(db: AccountingDatabase): Map<String, Double> {
        val balances = LinkedHashMap<String, Double>()
        for (acc in db.accounts) balances[acc.uid] = if (acc.oldBalance.isFiniteNum()) acc.oldBalance else 0.0

        val detailsByTx = HashMap<String, MutableList<Leg>>()
        for (d in db.transactionDetails) {
            if (d.transactionUid.isEmpty()) continue
            if (d.accountUid.isEmpty() || !balances.containsKey(d.accountUid)) continue
            if (!d.value.isFiniteNum() || d.value == 0.0) continue
            detailsByTx.getOrPut(d.transactionUid) { ArrayList() }.add(Leg(d.accountUid, Math.abs(d.value), d.subType))
        }

        fun apply(uid: String, delta: Double) { balances[uid] = (balances[uid] ?: 0.0) + delta }

        for (t in db.transactions) {
            val type = t.transactionType
            if (type !in 0..4) continue
            val legs = if (t.uid.isNotEmpty()) detailsByTx[t.uid] else null
            if (legs != null && legs.isNotEmpty()) {
                when (type) {
                    1 -> for (l in legs) apply(l.accountUid, -l.value)
                    0, 2 -> for (l in legs) apply(l.accountUid, l.value)
                    else -> for (l in legs) apply(l.accountUid, (if (l.subType == 1) 1 else -1) * l.value)
                }
                continue
            }
            val rawTotal = t.totalValue
            if (!rawTotal.isFiniteNum() || rawTotal == 0.0) continue
            val v = Math.abs(rawTotal)
            val src = t.sourceAccountUid
            val dst = t.destinationAccountUid
            if (!src.isNullOrEmpty() && balances.containsKey(src)) {
                when (type) {
                    1 -> apply(src, -v)
                    0, 2 -> apply(src, v)
                    3 -> apply(src, -v)
                }
            }
            if (type == 3 && !dst.isNullOrEmpty() && balances.containsKey(dst)) apply(dst, v)
        }
        return balances
    }

    fun getAccountBalance(db: AccountingDatabase, accountUid: String, cache: Map<String, Double>? = null): Double =
        (cache ?: computeAccountBalances(db))[accountUid] ?: 0.0
}
