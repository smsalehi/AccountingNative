package com.myaccounting.app.domain

import java.time.Instant

/**
 * Backup (JSON) reader/writer — the file format is the original app's `AccountingDatabase` JSON, so existing backups
 * import unchanged. normalize()/sanitize logic is a port of normalizeDatabase/sanitizeDatabase in src/utils/storage.ts.
 * Known difference: unknown extra JSON fields on rows are not preserved (see README "Known limitations").
 */
object DatabaseCodec {
    class ImportResult(val db: AccountingDatabase, val repaired: List<String>)
    class Validation(val ok: Boolean, val reason: String? = null, val stats: String? = null, val missingTables: List<String> = emptyList())

    private val TABLE_KEYS = listOf(
        "AccountGroups", "Accounts", "Banks", "Subjects", "Projects", "Persons", "PersonGroups", "Families",
        "Transactions", "TransactionDetails", "Invoices", "Depts", "Tags", "Configs",
    )

    @Suppress("UNCHECKED_CAST")
    private fun asRecord(v: Any?): Map<String, Any?>? = v as? Map<String, Any?>

    fun validate(parsed: Any?): Validation {
        val root = asRecord(parsed) ?: return Validation(false, "ساختار فایل JSON معتبر نیست (ریشه شیء نیست).")
        val missing = listOf("Accounts", "Subjects", "Transactions").filter { root[it] !is List<*> }
        if (missing.isNotEmpty()) return Validation(false, "جداول ضروری یافت نشد: ${missing.joinToString("، ")}")
        val stats = "حساب‌ها: ${(root["Accounts"] as List<*>).size}، سرفصل‌ها: ${(root["Subjects"] as List<*>).size}، تراکنش‌ها: ${(root["Transactions"] as List<*>).size}"
        return Validation(true, stats = stats, missingTables = TABLE_KEYS.filter { root[it] !is List<*> })
    }

    fun fromJson(text: String): ImportResult = normalize(Json.parse(text))

    // ---- field helpers ----------------------------------------------------------------
    private fun Map<String, Any?>.s(k: String): String? = this[k] as? String
    private fun Map<String, Any?>.sOr(k: String, d: String = ""): String = when (val v = this[k]) { null -> d; is String -> v; is Double -> Json.numberText(v); else -> v.toString() }
    private fun Map<String, Any?>.n(k: String): Double = jsNumber(this[k]).let { if (it.isFiniteNum()) it else 0.0 }
    private fun Map<String, Any?>.nRaw(k: String): Double = jsNumber(this[k])
    private fun Map<String, Any?>.i(k: String, d: Int = 0): Int = jsNumber(this[k]).let { if (it.isFiniteNum()) it.toInt() else d }
    private fun Map<String, Any?>.iOrNull(k: String): Int? = if (this[k] == null) null else jsNumber(this[k]).let { if (it.isFiniteNum()) it.toInt() else null }
    private fun Map<String, Any?>.lOrNull(k: String): Long? = if (this[k] == null) null else jsNumber(this[k]).let { if (it.isFiniteNum()) it.toLong() else null }

    fun normalize(raw: Any?): ImportResult {
        val repaired = ArrayList<String>()
        val src: Map<String, Any?> = asRecord(raw) ?: emptyMap()

        // rows of a table: non-array → [] (repaired); non-object rows are dropped (noted)
        fun rows(key: String): List<Map<String, Any?>> {
            val v = src[key]
            if (v !is List<*>) { repaired.add(key); return emptyList() }
            val out = ArrayList<Map<String, Any?>>(v.size)
            var dropped = 0
            for (r in v) { val m = asRecord(r); if (m == null) dropped++ else out.add(m) }
            if (dropped > 0) repaired.add("$key: $dropped ردیف خراب (null) حذف شد")
            return out
        }

        var groups = rows("AccountGroups").map { AccountGroup(it.sOr("UID"), it.sOr("Title"), it.s("Desc")) }
        var accounts = rows("Accounts")
        val banks = rows("Banks").map { Bank(it.sOr("UID"), it.sOr("Title")) }
        val subjects = rows("Subjects").map { Subject(it.sOr("UID"), it.sOr("Title"), it.i("IsCost", 1), it.s("ParentUID"), it.i("IsHidden"), it.s("Icon")) }
        val projects = rows("Projects").map { Project(it.sOr("UID"), it.sOr("Title"), it.i("IsActive", 1), it.sOr("Desc")) }
        val persons = rows("Persons").map {
            Person(it.sOr("UID"), it.sOr("FullName"), it.sOr("GroupUID"), it.sOr("Tel"), it.sOr("Mobile"), it.sOr("Email"), it.sOr("Address"), it.n("OldBalance"), it.i("IsHidden"), it.sOr("Desc"))
        }
        val personGroups = rows("PersonGroups").map { PersonGroup(it.sOr("UID"), it.sOr("Title"), it.s("Desc")) }
        val families = rows("Families").map { Family(it.sOr("UID"), it.sOr("Name"), it.i("IsHidden"), it.s("Desc")) }
        val txRows = rows("Transactions")
        val detailRows = rows("TransactionDetails")
        val invoices = rows("Invoices").map { Invoice(it.sOr("UID"), it.n("Discount"), it.sOr("StoreName"), it.sOr("InvoiceNumber")) }
        val depts = rows("Depts").map {
            Dept(
                it.sOr("UID"), it.sOr("Title"), it.n("Value"), it.n("PaymentStartDate").toLong(), it.n("PeriodDays"), it.n("PeriodValue"),
                it.i("ReminderDaysBeforeDueDate"), it.s("EventUID"), it.s("ProjectUID"), it.i("IsHidden"), it.sOr("Desc"),
                it.iOrNull("TotalInstallments"), it.iOrNull("DueDay"), it.s("FirstDueDate"),
            )
        }
        val tags = rows("Tags").map { Tag(it.sOr("UID"), it.sOr("Title")) }
        val configs = rows("Configs").map { ConfigItem(it.sOr("UID"), it.sOr("Key"), it.sOr("Value")) }

        val checks = (src["Checks"] as? List<*> ?: emptyList<Any?>()).mapNotNull { asRecord(it) }
            .filter { it["UID"] is String && jsNumber(it["Amount"]).isFiniteNum() }
            .map { c ->
                Check(
                    uid = c.sOr("UID"),
                    kind = if (c.s("Kind") == "person") CheckKind.PERSON else CheckKind.OWN,
                    amount = c.n("Amount"),
                    dueDate = c.sOr("DueDate"),
                    fundByDate = c.s("FundByDate"),
                    accountUid = c.s("AccountUID"),
                    drawerPersonUid = c.s("DrawerPersonUID"),
                    payeePersonUid = c.s("PayeePersonUID"),
                    checkNumber = c.s("CheckNumber"),
                    subjectUid = c.s("SubjectUID"),
                    projectUid = c.s("ProjectUID"),
                    desc = c.s("Desc"),
                    status = CheckStatus.values().firstOrNull { it.json == c.s("Status") } ?: CheckStatus.CANCELLED,
                    deposits = (c["Deposits"] as? List<*> ?: emptyList<Any?>()).mapNotNull { asRecord(it) }.map { d ->
                        CheckDeposit(d.sOr("UID"), d.sOr("AccountUID"), d.n("Amount"), d.n("DateTime").toLong(), d.sOr("TransactionUID"))
                    },
                    passedTransactionUid = c.s("PassedTransactionUID"),
                    passedAt = c.lOrNull("PassedAt"),
                    createdAt = c.n("CreatedAt").toLong(),
                )
            }
        val rates = (src["ExchangeRates"] as? List<*> ?: emptyList<Any?>()).mapNotNull { asRecord(it) }.mapNotNull { r ->
            val date = r["Date"] as? String ?: return@mapNotNull null
            val rate = jsNumber(r["Rate"])
            if (ExchangeRates.dateKeyToDay(date) == null || !rate.isFiniteNum() || rate <= 0) null else ExchangeRate(date, rate)
        }

        // default group for orphan accounts
        if (groups.isEmpty() && accounts.isNotEmpty()) {
            val fallback = "grp_imported_default"
            groups = listOf(AccountGroup(fallback, "سایر حساب‌ها", "ساخته‌شده خودکار هنگام ایمپورت (گروه‌بندی فایل مبدأ ناقص بود)"))
            var reassigned = 0
            accounts = accounts.map { a ->
                if (a["GroupUID"] != fallback) { reassigned++; a + ("GroupUID" to fallback) } else a
            }
            repaired.add("AccountGroups: گروه پیش‌فرض ساخته شد ($reassigned حساب یتیم منتقل شد)")
        }
        if (banks.isEmpty() && accounts.any { !(it["BankUID"] as? String).isNullOrEmpty() }) {
            repaired.add("Banks: فهرست بانک‌ها خالی است؛ نام بانک نمایش داده نمی‌شود")
        }

        // sanitize accounts
        var coercedAcc = 0
        val now = System.currentTimeMillis()
        val accountList = accounts.mapIndexed { idx, a ->
            var coerced = false
            var uid = a["UID"] as? String ?: ""
            if (uid.isEmpty()) { uid = "acc_sanitized_${now}_$idx"; coerced = true }
            val ob = a["OldBalance"]
            if (!(ob is Double && ob.isFiniteNum())) coerced = true
            if (coerced) coercedAcc++
            Account(
                uid, a.sOr("Title"), a.sOr("GroupUID"), a.n("OldBalance"), a.s("BankUID"), a.s("BankBranchName"), a.s("CardNumber"),
                a.s("BankAccountNumber"), a.i("IsHidden"), a.s("GpsLocation"), a.n("MinimumBalanceAlert"), a.i("ShowOnNotification"), a.s("Desc"),
            )
        }
        if (coercedAcc > 0) repaired.add("Accounts: $coercedAcc حساب با فیلد ناسازگار اصلاح شد")

        // sanitize transactions
        var coercedTx = 0
        var txList = txRows.mapIndexed { idx, t ->
            var coerced = false
            var uid = t["UID"] as? String ?: ""
            if (uid.isEmpty()) { uid = "tx_sanitized_${now}_$idx"; coerced = true }
            val rawType = t["TransactionType"]
            val tt = jsNumber(rawType)
            val validTt = if (tt == 0.0 || tt == 1.0 || tt == 2.0 || tt == 3.0 || tt == 4.0) tt.toInt() else 1
            if (!(rawType is Double && rawType == validTt.toDouble())) coerced = true
            val rawTv = t["TotalValue"]
            val tv = jsNumber(rawTv)
            val validTv = if (tv.isFiniteNum()) tv else 0.0
            if (!(rawTv is Double && rawTv == validTv)) coerced = true
            val rawPd = t["PersianDate"]
            if (rawPd !is String) coerced = true
            val rawDt = t["DateTime"]
            val dt = jsNumber(rawDt)
            val validDt = if (dt.isFiniteNum()) dt.toLong() else 0L
            if (!(rawDt is Double && rawDt == validDt.toDouble())) coerced = true
            if (coerced) coercedTx++
            Transaction(
                uid = uid, subjectUid = t.s("SubjectUID"), transactionType = validTt, totalValue = validTv, dateTime = validDt,
                persianDate = if (rawPd == null) "" else if (rawPd is String) rawPd else t.sOr("PersianDate"),
                time = t.s("Time"), eventUid = t.s("EventUID"), projectUid = t.s("ProjectUID"), personUid = t.s("PersonUID"),
                familyUid = t.s("FamilyUID"), invoiceUid = t.s("InvoiceUID"), deptUid = t.s("DeptUID"),
                reminderDate = t.lOrNull("ReminderDate"), isHidden = t.iOrNull("IsHidden"), desc = t.s("Desc"),
                sourceAccountUid = t.s("SourceAccountUID"), destinationAccountUid = t.s("DestinationAccountUID"), checkUid = t.s("CheckUID"),
            )
        }
        if (coercedTx > 0) repaired.add("Transactions: $coercedTx تراکنش با تایپ فیلد ناسازگار اصلاح شد")

        val details = detailRows.map {
            TransactionDetail(it.sOr("UID"), it.sOr("TransactionUID"), it.sOr("AccountUID"), it.nRaw("Value"), it.i("SubType"))
        }

        // rebuild transaction→account link and zero amounts from TransactionDetails
        val detailsByTx = HashMap<String, MutableList<TransactionDetail>>()
        for (d in details) if (d.transactionUid.isNotEmpty()) detailsByTx.getOrPut(d.transactionUid) { ArrayList() }.add(d)
        var linked = 0
        var amountFixed = 0
        txList = txList.map { t ->
            val ds = detailsByTx[t.uid]
            val needLink = t.sourceAccountUid.isNullOrEmpty()
            val needAmount = (!t.totalValue.isFiniteNum() || t.totalValue == 0.0) && !ds.isNullOrEmpty()
            if (!needLink && !needAmount) return@map t
            if (ds.isNullOrEmpty()) return@map t
            var fixed = t
            if (needLink) {
                val firstAcc = ds.firstOrNull { it.accountUid.isNotEmpty() }?.accountUid ?: return@map t
                fixed = fixed.copy(sourceAccountUid = firstAcc)
                if (fixed.transactionType == 3 && fixed.destinationAccountUid.isNullOrEmpty()) {
                    val second = ds.firstOrNull { it.accountUid.isNotEmpty() && it.accountUid != firstAcc }?.accountUid
                    if (second != null) fixed = fixed.copy(destinationAccountUid = second)
                }
                linked++
            }
            if (needAmount) {
                val legValues = ds.map { it.value }.filter { it.isFiniteNum() && it != 0.0 }.map { Math.abs(it) }
                if (legValues.isNotEmpty()) {
                    val total = if (fixed.transactionType == 3) legValues.max() else legValues.sum()
                    fixed = fixed.copy(totalValue = total)
                    amountFixed++
                }
            }
            fixed
        }
        if (linked > 0) repaired.add("Transactions: لینک حساب $linked تراکنش از TransactionDetails بازسازی شد")
        if (amountFixed > 0) repaired.add("Transactions: مبلغ $amountFixed تراکنش (از جمله انتقال وجه‌ها) از TransactionDetails بازسازی شد")

        val db = AccountingDatabase(
            version = (src["version"] as? Double)?.toInt() ?: 1,
            exportDate = src["exportDate"] as? String ?: Instant.now().toString(),
            app = src["app"] as? String ?: "حسابداری",
            appVersion = src["appVersion"] as? String ?: "1.0.0",
            accountGroups = groups, accounts = accountList, banks = banks, subjects = subjects, projects = projects,
            persons = persons, personGroups = personGroups, families = families, transactions = txList,
            transactionDetails = details, invoices = invoices, depts = depts, checks = checks, tags = tags,
            configs = configs, exchangeRates = rates,
        )
        return ImportResult(db, repaired)
    }

    // ---- writer -------------------------------------------------------------------------

    private fun obj(vararg pairs: Pair<String, Any?>, keepNull: Set<String> = emptySet()): Map<String, Any?> {
        val m = LinkedHashMap<String, Any?>()
        for ((k, v) in pairs) if (v != null || k in keepNull) m[k] = v
        return m
    }

    fun toJson(db: AccountingDatabase): String {
        val root = LinkedHashMap<String, Any?>()
        root["version"] = db.version
        root["exportDate"] = db.exportDate
        root["app"] = db.app
        root["appVersion"] = db.appVersion
        root["AccountGroups"] = db.accountGroups.map { obj("UID" to it.uid, "Title" to it.title, "Desc" to it.desc, keepNull = setOf("Desc")) }
        root["Accounts"] = db.accounts.map {
            obj(
                "UID" to it.uid, "Title" to it.title, "GroupUID" to it.groupUid, "OldBalance" to it.oldBalance, "BankUID" to it.bankUid,
                "BankBranchName" to it.bankBranchName, "CardNumber" to it.cardNumber, "BankAccountNumber" to it.bankAccountNumber,
                "IsHidden" to it.isHidden, "GpsLocation" to it.gpsLocation, "MinimumBalanceAlert" to it.minimumBalanceAlert,
                "ShowOnNotification" to it.showOnNotification, "Desc" to it.desc,
                keepNull = setOf("BankUID", "BankBranchName", "CardNumber", "BankAccountNumber", "GpsLocation", "Desc"),
            )
        }
        root["Banks"] = db.banks.map { obj("UID" to it.uid, "Title" to it.title) }
        root["Subjects"] = db.subjects.map {
            obj("UID" to it.uid, "Title" to it.title, "IsCost" to it.isCost, "ParentUID" to it.parentUid, "IsHidden" to it.isHidden, "Icon" to it.icon, keepNull = setOf("ParentUID"))
        }
        root["Projects"] = db.projects.map { obj("UID" to it.uid, "Title" to it.title, "IsActive" to it.isActive, "Desc" to it.desc) }
        root["Persons"] = db.persons.map {
            obj(
                "UID" to it.uid, "FullName" to it.fullName, "GroupUID" to it.groupUid, "Tel" to it.tel, "Mobile" to it.mobile, "Email" to it.email,
                "Address" to it.address, "OldBalance" to it.oldBalance, "IsHidden" to it.isHidden, "Desc" to it.desc,
            )
        }
        root["PersonGroups"] = db.personGroups.map { obj("UID" to it.uid, "Title" to it.title, "Desc" to it.desc, keepNull = setOf("Desc")) }
        root["Families"] = db.families.map { obj("UID" to it.uid, "Name" to it.name, "IsHidden" to it.isHidden, "Desc" to it.desc, keepNull = setOf("Desc")) }
        root["Transactions"] = db.transactions.map {
            obj(
                "UID" to it.uid, "SubjectUID" to it.subjectUid, "TransactionType" to it.transactionType, "TotalValue" to it.totalValue,
                "DateTime" to it.dateTime, "PersianDate" to it.persianDate, "Time" to it.time, "EventUID" to it.eventUid,
                "ProjectUID" to it.projectUid, "PersonUID" to it.personUid, "FamilyUID" to it.familyUid, "InvoiceUID" to it.invoiceUid,
                "DeptUID" to it.deptUid, "ReminderDate" to it.reminderDate, "IsHidden" to it.isHidden, "Desc" to it.desc,
                "SourceAccountUID" to it.sourceAccountUid, "DestinationAccountUID" to it.destinationAccountUid, "CheckUID" to it.checkUid,
                keepNull = setOf("SubjectUID"),
            )
        }
        root["TransactionDetails"] = db.transactionDetails.map {
            obj("UID" to it.uid, "TransactionUID" to it.transactionUid, "AccountUID" to it.accountUid, "Value" to it.value, "SubType" to it.subType)
        }
        root["Invoices"] = db.invoices.map { obj("UID" to it.uid, "Discount" to it.discount, "StoreName" to it.storeName, "InvoiceNumber" to it.invoiceNumber) }
        root["Depts"] = db.depts.map {
            obj(
                "UID" to it.uid, "Title" to it.title, "Value" to it.value, "PaymentStartDate" to it.paymentStartDate, "PeriodDays" to it.periodDays,
                "PeriodValue" to it.periodValue, "ReminderDaysBeforeDueDate" to it.reminderDaysBeforeDueDate, "EventUID" to it.eventUid,
                "ProjectUID" to it.projectUid, "IsHidden" to it.isHidden, "Desc" to it.desc, "TotalInstallments" to it.totalInstallments,
                "DueDay" to it.dueDay, "FirstDueDate" to it.firstDueDate, keepNull = setOf("EventUID", "ProjectUID"),
            )
        }
        root["Checks"] = db.checks.map { c ->
            obj(
                "UID" to c.uid, "Kind" to c.kind.json, "Amount" to c.amount, "DueDate" to c.dueDate, "FundByDate" to c.fundByDate,
                "AccountUID" to c.accountUid, "DrawerPersonUID" to c.drawerPersonUid, "PayeePersonUID" to c.payeePersonUid,
                "CheckNumber" to c.checkNumber, "SubjectUID" to c.subjectUid, "ProjectUID" to c.projectUid, "Desc" to c.desc,
                "Status" to c.status.json,
                "Deposits" to c.deposits.map { d -> obj("UID" to d.uid, "AccountUID" to d.accountUid, "Amount" to d.amount, "DateTime" to d.dateTime, "TransactionUID" to d.transactionUid) },
                "PassedTransactionUID" to c.passedTransactionUid, "PassedAt" to c.passedAt, "CreatedAt" to c.createdAt,
            )
        }
        root["Tags"] = db.tags.map { obj("UID" to it.uid, "Title" to it.title) }
        root["Configs"] = db.configs.map { obj("UID" to it.uid, "Key" to it.key, "Value" to it.value) }
        root["ExchangeRates"] = db.exchangeRates.map { obj("Date" to it.date, "Rate" to it.rate) }
        return Json.stringify(root)
    }
}
