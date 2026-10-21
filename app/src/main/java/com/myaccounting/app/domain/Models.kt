package com.myaccounting.app.domain

/**
 * Domain model — mirrors src/types.ts of the original React app 1:1 so that
 * backups produced by the web/Capacitor app can be imported unchanged.
 * Monetary amounts are Double, exactly like the original (JS number), because the
 * original calculation results must be reproduced bit-for-bit. Amounts are whole
 * toman values (< 2^53), for which Double arithmetic is exact.
 */
data class AccountGroup(val uid: String, val title: String = "", val desc: String? = null)

data class Bank(val uid: String, val title: String = "")

data class Account(
    val uid: String,
    val title: String = "",
    val groupUid: String = "",
    val oldBalance: Double = 0.0,
    val bankUid: String? = null,
    val bankBranchName: String? = null,
    val cardNumber: String? = null,
    val bankAccountNumber: String? = null,
    val isHidden: Int = 0,
    val gpsLocation: String? = null,
    val minimumBalanceAlert: Double = 0.0,
    val showOnNotification: Int = 0,
    val desc: String? = null,
)

/** isCost: 1 = expense, 0 = income */
data class Subject(
    val uid: String,
    val title: String = "",
    val isCost: Int = 1,
    val parentUid: String? = null,
    val isHidden: Int = 0,
    val icon: String? = null,
)

data class Project(val uid: String, val title: String = "", val isActive: Int = 1, val desc: String = "")

data class PersonGroup(val uid: String, val title: String = "", val desc: String? = null)

data class Person(
    val uid: String,
    val fullName: String = "",
    val groupUid: String = "",
    val tel: String = "",
    val mobile: String = "",
    val email: String = "",
    val address: String = "",
    val oldBalance: Double = 0.0,
    val isHidden: Int = 0,
    val desc: String = "",
)

data class Family(val uid: String, val name: String = "", val isHidden: Int = 0, val desc: String? = null)

/** transactionType: 0 = receive from persons, 1 = expense, 2 = income, 3 = transfer, 4 = debt/loan */
data class Transaction(
    val uid: String,
    val subjectUid: String? = null,
    val transactionType: Int = 1,
    val totalValue: Double = 0.0,
    val dateTime: Long = 0L,
    val persianDate: String = "",
    val time: String? = null,
    val eventUid: String? = null,
    val projectUid: String? = null,
    val personUid: String? = null,
    val familyUid: String? = null,
    val invoiceUid: String? = null,
    val deptUid: String? = null,
    val reminderDate: Long? = null,
    val isHidden: Int? = null,
    val desc: String? = null,
    val sourceAccountUid: String? = null,
    val destinationAccountUid: String? = null,
    val checkUid: String? = null,
)

data class TransactionDetail(
    val uid: String,
    val transactionUid: String = "",
    val accountUid: String = "",
    val value: Double = 0.0,
    val subType: Int = 0,
)

data class Invoice(val uid: String, val discount: Double = 0.0, val storeName: String = "", val invoiceNumber: String = "")

data class Dept(
    val uid: String,
    val title: String = "",
    val value: Double = 0.0,
    val paymentStartDate: Long = 0L,
    val periodDays: Double = 30.0,
    val periodValue: Double = 0.0,
    val reminderDaysBeforeDueDate: Int = 0,
    val eventUid: String? = null,
    val projectUid: String? = null,
    val isHidden: Int = 0,
    val desc: String = "",
    val totalInstallments: Int? = null,
    val dueDay: Int? = null,
    val firstDueDate: String? = null,
)

data class CheckDeposit(
    val uid: String,
    val accountUid: String = "",
    val amount: Double = 0.0,
    val dateTime: Long = 0L,
    val transactionUid: String = "",
)

enum class CheckKind(val json: String) { OWN("own"), PERSON("person") }
enum class CheckStatus(val json: String) { PENDING("pending"), PASSED("passed"), BOUNCED("bounced"), CANCELLED("cancelled") }

data class Check(
    val uid: String,
    val kind: CheckKind,
    val amount: Double,
    val dueDate: String,
    val fundByDate: String? = null,
    val accountUid: String? = null,
    val drawerPersonUid: String? = null,
    val payeePersonUid: String? = null,
    val checkNumber: String? = null,
    val subjectUid: String? = null,
    val projectUid: String? = null,
    val desc: String? = null,
    val status: CheckStatus = CheckStatus.PENDING,
    val deposits: List<CheckDeposit> = emptyList(),
    val passedTransactionUid: String? = null,
    val passedAt: Long? = null,
    val createdAt: Long = 0L,
)

data class Tag(val uid: String, val title: String = "")
data class ConfigItem(val uid: String, val key: String = "", val value: String = "")

/** date = Gregorian 'YYYY-MM-DD'; rate = toman per 1 USD */
data class ExchangeRate(val date: String, val rate: Double)

data class AccountingDatabase(
    val version: Int = 1,
    val exportDate: String = "",
    val app: String = "حسابداری",
    val appVersion: String = "1.0.0",
    val accountGroups: List<AccountGroup> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val banks: List<Bank> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val projects: List<Project> = emptyList(),
    val persons: List<Person> = emptyList(),
    val personGroups: List<PersonGroup> = emptyList(),
    val families: List<Family> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val transactionDetails: List<TransactionDetail> = emptyList(),
    val invoices: List<Invoice> = emptyList(),
    val depts: List<Dept> = emptyList(),
    val checks: List<Check> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val configs: List<ConfigItem> = emptyList(),
    val exchangeRates: List<ExchangeRate> = emptyList(),
)

enum class CurrencyUnit(val json: String) { TOMAN("toman"), RIAL("rial"), USD("usd") }
