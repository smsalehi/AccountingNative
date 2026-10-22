package com.myaccounting.app.ui.transactions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.myaccounting.app.domain.TransactionFilters.DatePreset

/**
 * Filter state of TransactionsView.tsx, hoisted out of the composable so it survives tab
 * switches — the web keeps every view mounted and hidden, so its filters never reset either.
 */
class TxUi {
    var search by mutableStateOf("")
    /** all | expense | income | transfer */
    var type by mutableStateOf("all")
    var accountUids by mutableStateOf<List<String>>(emptyList())
    var projectUids by mutableStateOf<List<String>>(emptyList())
    var subjectUids by mutableStateOf<List<String>>(emptyList())
    var personUids by mutableStateOf<List<String>>(emptyList())
    var deptUid by mutableStateOf("all")
    var preset by mutableStateOf(DatePreset.ALL)
    var customStart by mutableStateOf("")
    var customEnd by mutableStateOf("")
    var projectsCollapsed by mutableStateOf(true)
    var expandedTx by mutableStateOf<String?>(null)
    var accountDialog by mutableStateOf(false)
    var projectMenu by mutableStateOf(false)
    var subjectMenu by mutableStateOf(false)
    var personMenu by mutableStateOf(false)
    var subjectSearch by mutableStateOf("")
    var subjectExpanded by mutableStateOf(false)
    var subjectIncomeExpanded by mutableStateOf(false)

    fun toggle(kind: Kind, uid: String) {
        val current = when (kind) {
            Kind.ACCOUNT -> accountUids
            Kind.PROJECT -> projectUids
            Kind.SUBJECT -> subjectUids
            Kind.PERSON -> personUids
        }
        val next = if (uid in current) current - uid else current + uid
        when (kind) {
            Kind.ACCOUNT -> accountUids = next
            Kind.PROJECT -> projectUids = next
            Kind.SUBJECT -> subjectUids = next
            Kind.PERSON -> personUids = next
        }
    }

    /** «پاک کردن» / the reset button of the filter card. */
    fun reset() {
        search = ""
        type = "all"
        accountUids = emptyList()
        projectUids = emptyList()
        subjectUids = emptyList()
        personUids = emptyList()
        deptUid = "all"
        preset = DatePreset.ALL
        customStart = ""
        customEnd = ""
    }

    /** The web's `useEffect(..., [initialAccountFilter])`. */
    fun applyAccountFilter(uid: String?) {
        accountUids = if (uid != null) listOf(uid) else emptyList()
    }

    /** The web's `useEffect(..., [initialDeptFilter])`. */
    fun applyDeptFilter(uid: String?) {
        deptUid = uid ?: "all"
    }

    /** The web's `useEffect(..., [initialProjectFilter])`. */
    fun applyProjectFilter(uid: String?) {
        projectUids = if (uid != null) listOf(uid) else emptyList()
    }

    /**
     * The web's `useEffect(..., [filterRequestId])`: entering the tab from another view clears the
     * internal filters (search, type, subjects, persons, date) and re-applies the incoming ones.
     */
    fun resetForRequest(account: String?, dept: String?, project: String?) {
        search = ""
        type = "all"
        subjectUids = emptyList()
        personUids = emptyList()
        preset = DatePreset.ALL
        customStart = ""
        customEnd = ""
        accountUids = if (account != null) listOf(account) else emptyList()
        projectUids = if (project != null) listOf(project) else emptyList()
        deptUid = dept ?: "all"
    }

    enum class Kind { ACCOUNT, PROJECT, SUBJECT, PERSON }
}
