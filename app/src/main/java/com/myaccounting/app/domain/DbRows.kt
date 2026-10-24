package com.myaccounting.app.domain

/**
 * Per-row JSON for the SQLite store: each row is stored as its backup-format JSON object, so
 * [DatabaseCodec.toJson] stays the single source of truth (single-row db → take the table's only
 * element). Rows are parsed back by feeding all tables through [DatabaseCodec.normalize], which
 * keeps the exact import/repair semantics of the JSON format.
 */
object DbRows {
    const val T_GROUPS = "AccountGroups"
    const val T_ACCOUNTS = "Accounts"
    const val T_BANKS = "Banks"
    const val T_SUBJECTS = "Subjects"
    const val T_PROJECTS = "Projects"
    const val T_PERSONS = "Persons"
    const val T_PERSON_GROUPS = "PersonGroups"
    const val T_FAMILIES = "Families"
    const val T_TRANSACTIONS = "Transactions"
    const val T_DETAILS = "TransactionDetails"
    const val T_INVOICES = "Invoices"
    const val T_DEPTS = "Depts"
    const val T_CHECKS = "Checks"
    const val T_TAGS = "Tags"
    const val T_CONFIGS = "Configs"
    const val T_RATES = "ExchangeRates"

    private fun rowJson(db: AccountingDatabase, table: String): String {
        val parsed = Json.parse(DatabaseCodec.toJson(db))
        val list = (parsed as? Map<*, *>)?.get(table) as? List<*>
        val row = list?.firstOrNull() ?: return "{}"
        return Json.stringify(row)
    }

    fun groupJson(x: AccountGroup) = rowJson(AccountingDatabase(accountGroups = listOf(x)), T_GROUPS)
    fun accountJson(x: Account) = rowJson(AccountingDatabase(accounts = listOf(x)), T_ACCOUNTS)
    fun bankJson(x: Bank) = rowJson(AccountingDatabase(banks = listOf(x)), T_BANKS)
    fun subjectJson(x: Subject) = rowJson(AccountingDatabase(subjects = listOf(x)), T_SUBJECTS)
    fun projectJson(x: Project) = rowJson(AccountingDatabase(projects = listOf(x)), T_PROJECTS)
    fun personJson(x: Person) = rowJson(AccountingDatabase(persons = listOf(x)), T_PERSONS)
    fun personGroupJson(x: PersonGroup) = rowJson(AccountingDatabase(personGroups = listOf(x)), T_PERSON_GROUPS)
    fun familyJson(x: Family) = rowJson(AccountingDatabase(families = listOf(x)), T_FAMILIES)
    fun transactionJson(x: Transaction) = rowJson(AccountingDatabase(transactions = listOf(x)), T_TRANSACTIONS)
    fun detailJson(x: TransactionDetail) = rowJson(AccountingDatabase(transactionDetails = listOf(x)), T_DETAILS)
    fun invoiceJson(x: Invoice) = rowJson(AccountingDatabase(invoices = listOf(x)), T_INVOICES)
    fun deptJson(x: Dept) = rowJson(AccountingDatabase(depts = listOf(x)), T_DEPTS)
    fun checkJson(x: Check) = rowJson(AccountingDatabase(checks = listOf(x)), T_CHECKS)
    fun tagJson(x: Tag) = rowJson(AccountingDatabase(tags = listOf(x)), T_TAGS)
    fun configJson(x: ConfigItem) = rowJson(AccountingDatabase(configs = listOf(x)), T_CONFIGS)
    fun rateJson(x: ExchangeRate) = rowJson(AccountingDatabase(exchangeRates = listOf(x)), T_RATES)

    /** One stored row: uid + optional sort key + the row object (equality diff) + lazy JSON. */
    data class Row(val uid: String, val sort: Long, val obj: Any?, val json: () -> String)

    /** Every table of [db] as rows (same order as [tables]); JSON is only produced on demand. */
    fun snapshot(db: AccountingDatabase): Map<String, List<Row>> = mapOf(
        T_GROUPS to db.accountGroups.map { Row(it.uid, 0, it) { groupJson(it) } },
        T_ACCOUNTS to db.accounts.map { Row(it.uid, 0, it) { accountJson(it) } },
        T_BANKS to db.banks.map { Row(it.uid, 0, it) { bankJson(it) } },
        T_SUBJECTS to db.subjects.map { Row(it.uid, 0, it) { subjectJson(it) } },
        T_PROJECTS to db.projects.map { Row(it.uid, 0, it) { projectJson(it) } },
        T_PERSONS to db.persons.map { Row(it.uid, 0, it) { personJson(it) } },
        T_PERSON_GROUPS to db.personGroups.map { Row(it.uid, 0, it) { personGroupJson(it) } },
        T_FAMILIES to db.families.map { Row(it.uid, 0, it) { familyJson(it) } },
        T_TRANSACTIONS to db.transactions.map { Row(it.uid, it.dateTime, it) { transactionJson(it) } },
        T_DETAILS to db.transactionDetails.map { Row(it.uid, 0, it) { detailJson(it) } },
        T_INVOICES to db.invoices.map { Row(it.uid, 0, it) { invoiceJson(it) } },
        T_DEPTS to db.depts.map { Row(it.uid, 0, it) { deptJson(it) } },
        T_CHECKS to db.checks.map { Row(it.uid, it.createdAt, it) { checkJson(it) } },
        T_TAGS to db.tags.map { Row(it.uid, 0, it) { tagJson(it) } },
        T_CONFIGS to db.configs.map { Row(it.uid, 0, it) { configJson(it) } },
        T_RATES to db.exchangeRates.map { Row(it.date, 0, it) { rateJson(it) } },
    )

    /** Parsed row maps per table → a full [AccountingDatabase] through the normal import pipeline. */
    fun toDatabase(tables: Map<String, List<Any?>>): AccountingDatabase =
        DatabaseCodec.normalize(tables).db
}
