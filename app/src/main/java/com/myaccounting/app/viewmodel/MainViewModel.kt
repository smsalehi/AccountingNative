package com.myaccounting.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myaccounting.app.data.AppPrefs
import com.myaccounting.app.data.AppLog
import com.myaccounting.app.data.DatabaseStore
import com.myaccounting.app.data.FolderBackupStore
import com.myaccounting.app.data.ThemeMode
import com.myaccounting.app.domain.Account
import com.myaccounting.app.domain.AccountGroup
import com.myaccounting.app.domain.AccountingDatabase
import com.myaccounting.app.domain.Balance
import com.myaccounting.app.domain.Check
import com.myaccounting.app.domain.CheckDeposit
import com.myaccounting.app.domain.CheckKind
import com.myaccounting.app.domain.CheckStatus
import com.myaccounting.app.domain.Checks
import com.myaccounting.app.domain.Dept
import com.myaccounting.app.domain.JalaliDate
import com.myaccounting.app.domain.Person
import com.myaccounting.app.domain.Project
import com.myaccounting.app.domain.LoanSchedule
import com.myaccounting.app.domain.Notifications
import com.myaccounting.app.domain.Reorder
import com.myaccounting.app.domain.Subject
import com.myaccounting.app.domain.Transaction
import com.myaccounting.app.domain.Transactions
import com.myaccounting.app.domain.BackupCodec
import com.myaccounting.app.domain.Currency
import com.myaccounting.app.domain.CurrencyUnit
import com.myaccounting.app.domain.DatabaseCodec
import com.myaccounting.app.domain.ExchangeRates
import com.myaccounting.app.domain.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Toast variants of the original app (ToastModal.tsx). */
enum class ToastKind { SUCCESS, ERROR, WARNING, INFO }

data class ToastMessage(val text: String, val kind: ToastKind, val id: Long)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    data class UiState(
        val loaded: Boolean = false,
        val unit: CurrencyUnit = CurrencyUnit.TOMAN,
        /** The stored database (always toman). */
        val db: AccountingDatabase = AccountingDatabase(),
        /** What the screens show: [db], or its display-only dollar view. Never persisted. */
        val shownDb: AccountingDatabase = AccountingDatabase(),
        val balances: Map<String, Double> = emptyMap(),
        val rateIndex: ExchangeRates.RateIndex? = null,
        /** The app's toast (port of ToastModal.tsx); [ToastMessage.id] changes on every new message. */
        val toast: ToastMessage? = null,
        /** Unit actually used for display: USD falls back to toman while no rates exist (App.tsx effectiveCurrency). */
        val effectiveUnit: CurrencyUnit = CurrencyUnit.TOMAN,
        /** Number on the bell badge. */
        val urgentNotifications: Int = 0,
        /** Bell popover rows (port of the NotificationDropdown memo). */
        val notifications: List<Notifications.Item> = emptyList(),
        /** Reminder ids dismissed today. */
        val notifDismissed: Set<String> = emptySet(),
        /** Red dot on the loans tab: overdue loans + checks needing attention. */
        val deptsAttention: Int = 0,
        /** Pending own checks per account (shown as «پس از چک‌ها»); empty in the dollar view. */
        val ownChecksByAccount: Map<String, Double> = emptyMap(),
    )

    private val store = DatabaseStore(application)
    private val prefs = AppPrefs(application)
    private val saveLock = Mutex()

    private val dbFlow = MutableStateFlow<AccountingDatabase?>(null)
    private val toastFlow = MutableStateFlow<ToastMessage?>(null)
    private var nextToastId = 0L

    /** Shows a toast; [ToastMessage.id] makes repeated identical texts restart the timer. */
    private fun emit(text: String, kind: ToastKind) {
        toastFlow.value = ToastMessage(text, kind, ++nextToastId)
    }

    val theme: StateFlow<ThemeMode> = prefs.theme.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
    private val unitFlow: StateFlow<CurrencyUnit> = prefs.unit.stateIn(viewModelScope, SharingStarted.Eagerly, CurrencyUnit.TOMAN)

    val fontScale: StateFlow<Int> = prefs.fontScale.stateIn(viewModelScope, SharingStarted.Eagerly, AppPrefs.DEFAULT_FONT_SCALE)
    val collapsedGroups: StateFlow<Set<String>> = prefs.collapsedGroups.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val notifDismissed: StateFlow<Set<String>> = prefs.notifDismissed.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val state: StateFlow<UiState> = combine(dbFlow, unitFlow, toastFlow, notifDismissed) { db, unit, toast, dismissed ->
        if (db == null) {
            UiState(loaded = false, unit = unit, effectiveUnit = unit, toast = toast, notifDismissed = dismissed)
        } else {
            val idx = ExchangeRates.buildRateIndex(db.exchangeRates)
            val effective = if (unit == CurrencyUnit.USD && idx == null) CurrencyUnit.TOMAN else unit
            val shown = if (effective == CurrencyUnit.USD && idx != null) ExchangeRates.buildUsdViewDb(db, idx) else db
            val notifications = Notifications.items(shown, dismissed)
            UiState(
                loaded = true, unit = unit, db = db, shownDb = shown,
                balances = Balance.computeAccountBalances(shown), rateIndex = idx, toast = toast, effectiveUnit = effective,
                urgentNotifications = notifications.size,
                notifications = notifications,
                notifDismissed = dismissed,
                deptsAttention = LoanSchedule.countVisibleOverdueLoans(db) + Checks.countChecksNeedingAttention(db),
                ownChecksByAccount = if (effective == CurrencyUnit.USD) emptyMap() else Checks.pendingOwnChecksByAccount(db),
            )
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    init {
        viewModelScope.launch {
            when (val r = withContext(Dispatchers.IO) { store.load() }) {
                is DatabaseStore.LoadResult.Ok -> dbFlow.value = r.db
                is DatabaseStore.LoadResult.Corrupt -> {
                    // Never overwrite an unreadable file silently: it was copied aside; start empty and tell the user.
                    dbFlow.value = DatabaseStore.emptyDatabase()
                    emit("فایل دیتابیس خوانده نشد و نسخه‌ای از آن کنار گذاشته شد: ${r.error}", ToastKind.ERROR)
                }
            }
        }
    }

    fun consumeMessage() { toastFlow.value = null }

    fun setTheme(mode: ThemeMode) { viewModelScope.launch { prefs.setTheme(mode) } }

    fun setUnit(unit: CurrencyUnit) {
        if (unit == CurrencyUnit.USD && dbFlow.value?.exchangeRates.isNullOrEmpty()) {
            emit("برای نمایش دلاری ابتدا فایل نرخ دلار را وارد کنید.", ToastKind.WARNING)
            return
        }
        viewModelScope.launch { prefs.setUnit(unit) }
    }

    private suspend fun readBytes(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("فایل باز نشد")
    }

    private suspend fun persist(db: AccountingDatabase) {
        saveLock.withLock { withContext(Dispatchers.IO) { store.save(db) } }
        dbFlow.value = db
    }

    fun toggleTheme(currentlyDark: Boolean) { viewModelScope.launch { prefs.setTheme(if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK) } }

    fun setFontScale(percent: Int) { viewModelScope.launch { prefs.setFontScale(percent) } }

    fun setCollapsedGroups(uids: Set<String>) { viewModelScope.launch { prefs.setCollapsedGroups(uids) } }

    /** `dismissNotif` in NotificationDropdown.tsx: hide today, restore tomorrow. */
    fun dismissNotification(id: String) {
        viewModelScope.launch { prefs.setNotifDismissed(notifDismissed.value + id) }
    }

    /** Screens/dialogs that are not ported yet say so instead of silently doing nothing. */
    fun notPorted(what: String) { emit("$what — این بخش هنوز در نسخه نیتیو منتقل نشده است.", ToastKind.WARNING) }

    fun warn(message: String) { emit(message, ToastKind.WARNING) }
    fun info(message: String) { emit(message, ToastKind.INFO) }
    fun success(message: String) { emit(message, ToastKind.SUCCESS) }

    /** The persisted SAF folder for Excel/PDF exports (exportDelivery.ts). */
    suspend fun exportsFolderUri(): String = prefs.exportsFolder.first()
    fun saveExportsFolder(uri: String) { viewModelScope.launch { prefs.setExportsFolder(uri) } }

    /** Check reminder prefs (checkNotifications.ts localStorage key). */
    val checkReminderPrefs: StateFlow<Notifications.CheckReminderPrefs> = combine(
        prefs.checkReminderEnabled,
        prefs.checkReminderHour,
    ) { e, h -> Notifications.CheckReminderPrefs(e, h) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Notifications.CheckReminderPrefs())
    fun saveCheckReminderPrefs(value: Notifications.CheckReminderPrefs) {
        viewModelScope.launch { prefs.setCheckReminderPrefs(value.enabled, value.hour) }
    }

    /** Drag & drop: the group [fromUid] takes the position of [toUid] (reorderArray semantics). */
    fun reorderGroups(fromUid: String, toUid: String) {
        val db = dbFlow.value ?: return
        val from = db.accountGroups.indexOfFirst { it.uid == fromUid }
        val to = db.accountGroups.indexOfFirst { it.uid == toUid }
        if (from == -1 || to == -1 || from == to) return
        viewModelScope.launch { persist(db.copy(accountGroups = Reorder.reorderArray(db.accountGroups, from, to))) }
    }

    fun reorderAccounts(fromUid: String, toUid: String) {
        val db = dbFlow.value ?: return
        val from = db.accounts.indexOfFirst { it.uid == fromUid }
        val to = db.accounts.indexOfFirst { it.uid == toUid }
        if (from == -1 || to == -1 || from == to) return
        viewModelScope.launch { persist(db.copy(accounts = Reorder.reorderArray(db.accounts, from, to))) }
    }

    fun renameGroup(uid: String, title: String) {
        val db = dbFlow.value ?: return
        val t = title.trim()
        if (t.isEmpty()) return
        viewModelScope.launch { persist(db.copy(accountGroups = db.accountGroups.map { if (it.uid == uid) it.copy(title = t) else it })) }
    }

    /** AccountSettingsModal's onAddAccount / onUpdateAccount: one entry point, decided by UID. */
    fun saveAccount(account: Account) {
        val db = dbFlow.value ?: return
        if (unitFlow.value == CurrencyUnit.USD) { warn(Currency.USD_READONLY_MESSAGE); return }
        val accounts = if (db.accounts.any { it.uid == account.uid }) {
            db.accounts.map { if (it.uid == account.uid) account else it }
        } else {
            db.accounts + account
        }
        // A database without groups (fresh install) would hide the very first account; give it a home.
        val groups = if (db.accountGroups.isEmpty()) {
            listOf(AccountGroup("grp_default", "گروه پیش‌فرض"))
        } else {
            db.accountGroups
        }
        viewModelScope.launch { persist(db.copy(accounts = accounts, accountGroups = groups)) }
    }

    /** App.tsx handleSaveTransaction: insert (newest first) or replace, and mirror the details rows. */
    fun saveTransaction(tx: Transaction, isEditing: Boolean) {
        val db = dbFlow.value ?: return
        val transactions = if (isEditing) {
            db.transactions.map { if (it.uid == tx.uid) tx else it }
        } else {
            listOf(tx) + db.transactions
        }
        val details = Transactions.syncDetails(db.transactionDetails, tx)
        viewModelScope.launch {
            persist(db.copy(transactions = transactions, transactionDetails = details))
            emit(if (isEditing) "تراکنش با موفقیت ویرایش شد." else "تراکنش با موفقیت ثبت شد.", ToastKind.SUCCESS)
            AppLog.success(AppLog.Category.TRANSACTION, if (isEditing) "ویرایش تراکنش" else "ثبت تراکنش", tx.desc)
        }
    }

    /** App.tsx handleDeleteTransaction: remove the transaction, its details rows. */
    fun deleteTransaction(uid: String) {
        val db = dbFlow.value ?: return
        viewModelScope.launch {
            persist(
                db.copy(
                    transactions = db.transactions.filter { it.uid != uid },
                    transactionDetails = db.transactionDetails.filter { it.transactionUid != uid },
                ),
            )
            AppLog.warning(AppLog.Category.TRANSACTION, "حذف تراکنش", uid)
        }
    }

    /** NewSubjectModal.tsx onSave: a new subject is appended to the list. */
    fun saveSubject(subject: Subject) {
        val db = dbFlow.value ?: return
        if (unitFlow.value == CurrencyUnit.USD) { warn(Currency.USD_READONLY_MESSAGE); return }
        viewModelScope.launch { persist(db.copy(subjects = db.subjects + subject)) }
    }

    /** Deletes only the account (transactions keep their UIDs, exactly like the original). */
    fun deleteAccount(uid: String) {
        val db = dbFlow.value ?: return
        if (unitFlow.value == CurrencyUnit.USD) { warn(Currency.USD_READONLY_MESSAGE); return }
        viewModelScope.launch { persist(db.copy(accounts = db.accounts.filter { it.uid != uid })) }
    }

    /** Replaces the whole database with the content of a backup file (.json / .json.gz / legacy base64). */
    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val text = BackupCodec.decode(readBytes(uri))
                val parsed = Json.parse(text)
                val v = DatabaseCodec.validate(parsed)
                if (!v.ok) { emit(v.reason ?: "ساختار فایل بکاپ معتبر نیست.", ToastKind.ERROR); return@launch }
                val result = withContext(Dispatchers.Default) { DatabaseCodec.normalize(parsed) }
                persist(result.db)
                emit(
                    "ایمپورت انجام شد — ${v.stats}" + if (result.repaired.isNotEmpty()) " (${result.repaired.size} مورد ترمیم)" else "",
                    ToastKind.SUCCESS,
                )
                AppLog.success(AppLog.Category.IMPORT, "ایمپورت بکاپ انجام شد", "${v.stats}" + if (result.repaired.isNotEmpty()) " — ${result.repaired.size} مورد ترمیم" else "")
            } catch (e: Exception) {
                emit("ایمپورت ناموفق بود: ${e.message ?: e.javaClass.simpleName}", ToastKind.ERROR)
                AppLog.error(AppLog.Category.IMPORT, "ایمپورت بکاپ ناموفق بود", e.message)
            }
        }
    }

    fun exportBackup(uri: Uri) {
        val db = dbFlow.value ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use {
                        it.write(DatabaseCodec.toJson(db).toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("فایل مقصد باز نشد")
                }
                emit("بکاپ ذخیره شد.", ToastKind.SUCCESS)
                AppLog.success(AppLog.Category.BACKUP, "بکاپ ذخیره شد")
            } catch (e: Exception) {
                emit("ذخیره بکاپ ناموفق بود: ${e.message ?: e.javaClass.simpleName}", ToastKind.ERROR)
                AppLog.error(AppLog.Category.BACKUP, "ذخیره بکاپ ناموفق بود", e.message)
            }
        }
    }

    /** Merges a daily-rate file (Dollar_D.txt style). Only NEW days are added (existing days are kept). */
    fun importRates(uri: Uri, fileUnit: ExchangeRates.RateFileUnit) {
        val db = dbFlow.value ?: return
        viewModelScope.launch {
            try {
                val text = String(readBytes(uri), Charsets.UTF_8)
                val parsed = ExchangeRates.parseRateFile(text, fileUnit)
                if (parsed.rows.isEmpty()) { emit("هیچ ردیف معتبری در فایل نرخ پیدا نشد (${parsed.invalid} ردیف نامعتبر).", ToastKind.WARNING); return@launch }
                val merged = ExchangeRates.mergeRates(db.exchangeRates, parsed.rows, overwrite = false)
                persist(db.copy(exchangeRates = merged.merged))
                emit("نرخ دلار: ${merged.added} روز جدید اضافه شد، ${merged.skippedExisting} روز تکراری رد شد، ${parsed.invalid} ردیف نامعتبر.", ToastKind.SUCCESS)
            } catch (e: Exception) {
                emit("وارد کردن نرخ دلار ناموفق بود: ${e.message ?: e.javaClass.simpleName}", ToastKind.ERROR)
            }
        }
    }

    // -----------------------------------------------------------------------
    // Folder backup — folderBackup.ts + autoBackup.ts (google: daily gzip, local: raw on exit)
    // -----------------------------------------------------------------------

    val backupGoogle: StateFlow<AppPrefs.BackupFolderState> =
        prefs.backupFolder("google").stateIn(viewModelScope, SharingStarted.Eagerly, AppPrefs.BackupFolderState())
    val backupLocal: StateFlow<AppPrefs.BackupFolderState> =
        prefs.backupFolder("local").stateIn(viewModelScope, SharingStarted.Eagerly, AppPrefs.BackupFolderState())

    fun saveBackupFolder(kind: String, uri: Uri, folderName: String) {
        viewModelScope.launch {
            prefs.setBackupFolder(kind, uri.toString(), folderName)
            AppLog.success(AppLog.Category.BACKUP, "پوشه بکاپ «$kind» متصل شد", folderName)
        }
    }

    fun disconnectBackup(kind: String) {
        viewModelScope.launch {
            prefs.clearBackupFolder(kind)
            AppLog.info(AppLog.Category.BACKUP, "اتصال پوشه بکاپ «$kind» قطع شد")
        }
    }

    private fun backupCfg(kind: String) = if (kind == "google") FolderBackupStore.GOOGLE else FolderBackupStore.LOCAL

    private fun stampLabel(): String =
        JalaliDate.getFullJalaliDate(System.currentTimeMillis()) + " - " +
            java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))

    /** «بکاپ دستی»: a timestamped gzip file, always written. */
    fun manualFolderBackup(kind: String) {
        val db = dbFlow.value ?: return
        val cfg = backupCfg(kind)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val st = (if (kind == "google") backupGoogle else backupLocal).value
                if (!st.connected) { warn("ابتدا پوشه بکاپ را متصل کنید."); return@launch }
                val (bytes, size) = FolderBackupStore.gzipPayload(db)
                val name = FolderBackupStore.manualFileName(cfg.manualPrefix)
                val ok = FolderBackupStore.writeGzip(getApplication(), Uri.parse(st.uri), name, bytes)
                if (ok) {
                    prefs.stampBackupSync(kind, stampLabel())
                    success("بکاپ دستی ذخیره شد: $name")
                    AppLog.success(AppLog.Category.BACKUP, "بکاپ دستی «${cfg.title}» ذخیره شد", "$name ($size bytes)")
                } else {
                    warn("نوشتن بکاپ دستی ناموفق بود.")
                    AppLog.error(AppLog.Category.BACKUP, "نوشتن بکاپ دستی ناموفق بود", name)
                }
            } catch (e: Exception) {
                warn("بکاپ دستی ناموفق بود: ${e.message ?: e.javaClass.simpleName}")
                AppLog.error(AppLog.Category.BACKUP, "بکاپ دستی ناموفق بود", e.message)
            }
        }
    }

    /** Restores a backup file from the connected folder (same pipeline as importBackup). */
    fun restoreFromFolder(kind: String, fileName: String) {
        val st = (if (kind == "google") backupGoogle else backupLocal).value
        if (!st.connected) { warn("ابتدا پوشه بکاپ را متصل کنید."); return }
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    FolderBackupStore.readBackupText(getApplication(), Uri.parse(st.uri), fileName)
                } ?: throw IllegalStateException("خواندن فایل ناموفق بود")
                val parsed = Json.parse(text)
                val v = DatabaseCodec.validate(parsed)
                if (!v.ok) { emit(v.reason ?: "ساختار فایل بکاپ معتبر نیست.", ToastKind.ERROR); return@launch }
                val result = withContext(Dispatchers.Default) { DatabaseCodec.normalize(parsed) }
                persist(result.db)
                emit("بازیابی انجام شد — ${v.stats}", ToastKind.SUCCESS)
                AppLog.success(AppLog.Category.IMPORT, "بازیابی از «$fileName»", "${v.stats}")
            } catch (e: Exception) {
                emit("بازیابی ناموفق بود: ${e.message ?: e.javaClass.simpleName}", ToastKind.ERROR)
                AppLog.error(AppLog.Category.IMPORT, "بازیابی از پوشه بکاپ ناموفق بود", e.message)
            }
        }
    }

    enum class AutoTrigger { STARTUP, EXIT }

    /**
     * autoBackup.ts runAllAutoBackups: google = one gzip auto-backup per day into the fixed file;
     * local = raw overwrite on every run; both guarded by the 20% rule. Never shows UI, never throws.
     */
    fun runAutoBackups(trigger: AutoTrigger) {
        val db = dbFlow.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (!FolderBackupStore.hasUserData(db)) {
                AppLog.info(AppLog.Category.BACKUP, "بکاپ خودکار رد شد: دیتابیس خالی است.")
                return@launch
            }
            runGoogleDailyAutoBackup(trigger, db)
            runLocalAutoBackup(db)
        }
    }

    private suspend fun runGoogleDailyAutoBackup(trigger: AutoTrigger, db: AccountingDatabase) {
        val cfg = FolderBackupStore.GOOGLE
        try {
            val today = java.time.LocalDate.now().toString()
            val st = backupGoogle.value
            if (!st.connected) return
            if (st.lastAutoDate == today) { AppLog.info(AppLog.Category.BACKUP, "بکاپ خودکار گوگل: امروز قبلاً انجام شد."); return }
            if (FolderBackupStore.isDriveFolderUri(st.uri) && !isOnline()) {
                AppLog.warning(AppLog.Category.BACKUP, "بکاپ خودکار گوگل رد شد: آفلاین.")
                return
            }
            val (bytes, size) = FolderBackupStore.gzipPayload(db)
            val prev = FolderBackupStore.fileSize(getApplication(), Uri.parse(st.uri), cfg.autoFile)
            if (!FolderBackupStore.decideReplace(prev, size.toLong())) {
                val msg = "بکاپ خودکار امروز انجام نشد: حجم بکاپ جدید (${prev} → $size بایت) بیش از ۲۰٪ کمتر است؛ فایل قبلی حفظ شد."
                prefs.setBackupBlocked("google", msg)
                AppLog.warning(AppLog.Category.BACKUP, msg)
                return
            }
            if (FolderBackupStore.writeGzip(getApplication(), Uri.parse(st.uri), cfg.autoFile, bytes)) {
                prefs.setBackupAutoDate("google", today)
                prefs.stampBackupSync("google", stampLabel())
                prefs.setBackupBlocked("google", "")
                AppLog.success(AppLog.Category.BACKUP, "بکاپ خودکار روزانه گوگل ذخیره شد.", "trigger=$trigger")
            } else {
                AppLog.error(AppLog.Category.BACKUP, "نوشتن بکاپ خودکار گوگل ناموفق بود")
            }
        } catch (e: Exception) {
            AppLog.error(AppLog.Category.BACKUP, "خطا در بکاپ خودکار روزانه گوگل", e.message)
        }
    }

    private suspend fun runLocalAutoBackup(db: AccountingDatabase) {
        val cfg = FolderBackupStore.LOCAL
        try {
            val st = backupLocal.value
            if (!st.connected) return
            val json = DatabaseCodec.toJson(db)
            val size = json.toByteArray(Charsets.UTF_8).size.toLong()
            val prev = FolderBackupStore.fileSize(getApplication(), Uri.parse(st.uri), cfg.autoFile)
            if (!FolderBackupStore.decideReplace(prev, size)) {
                val msg = "بکاپ خودکار انجام نشد: حجم بکاپ جدید بیش از ۲۰٪ کمتر است؛ فایل قبلی حفظ شد."
                prefs.setBackupBlocked("local", msg)
                AppLog.warning(AppLog.Category.BACKUP, msg)
                return
            }
            if (FolderBackupStore.writeText(getApplication(), Uri.parse(st.uri), cfg.autoFile, json)) {
                prefs.stampBackupSync("local", stampLabel())
                prefs.setBackupBlocked("local", "")
                AppLog.success(AppLog.Category.BACKUP, "بکاپ خودکار پوشه داخلی ذخیره شد.")
            } else {
                AppLog.error(AppLog.Category.BACKUP, "نوشتن بکاپ خودکار داخلی ناموفق بود")
            }
        } catch (e: Exception) {
            AppLog.error(AppLog.Category.BACKUP, "خطا در بکاپ خودکار پوشه داخلی", e.message)
        }
    }

    private suspend fun isOnline(): Boolean = withContext(Dispatchers.IO) {
        try {
            java.net.InetAddress.getByName("connectivitycheck.gstatic.com").isReachable(2000)
        } catch (_: Exception) {
            false
        }
    }

    // -----------------------------------------------------------------------
    // Persons — App.tsx handleAddPerson / handleEditPerson / delete + reorder
    // -----------------------------------------------------------------------

    fun savePerson(person: Person) {
        val db = dbFlow.value ?: return
        if (unitFlow.value == CurrencyUnit.USD) { warn(Currency.USD_READONLY_MESSAGE); return }
        val persons = if (db.persons.any { it.uid == person.uid }) {
            db.persons.map { if (it.uid == person.uid) person else it }
        } else {
            db.persons + person
        }
        viewModelScope.launch { persist(db.copy(persons = persons)) }
    }

    fun deletePerson(uid: String) {
        val db = dbFlow.value ?: return
        if (unitFlow.value == CurrencyUnit.USD) { warn(Currency.USD_READONLY_MESSAGE); return }
        viewModelScope.launch { persist(db.copy(persons = db.persons.filter { it.uid != uid })) }
    }

    fun reorderPersons(fromUid: String, toUid: String) {
        val db = dbFlow.value ?: return
        val from = db.persons.indexOfFirst { it.uid == fromUid }
        val to = db.persons.indexOfFirst { it.uid == toUid }
        if (from == -1 || to == -1 || from == to) return
        viewModelScope.launch { persist(db.copy(persons = Reorder.reorderArray(db.persons, from, to))) }
    }

    fun reorderPersonGroups(fromUid: String, toUid: String) {
        val db = dbFlow.value ?: return
        val from = db.personGroups.indexOfFirst { it.uid == fromUid }
        val to = db.personGroups.indexOfFirst { it.uid == toUid }
        if (from == -1 || to == -1 || from == to) return
        viewModelScope.launch { persist(db.copy(personGroups = Reorder.reorderArray(db.personGroups, from, to))) }
    }

    // -----------------------------------------------------------------------
    // Depts — App.tsx handleAddDept / handleEditDept / delete + reorder
    // -----------------------------------------------------------------------

    fun saveDept(dept: Dept) {
        val db = dbFlow.value ?: return
        if (unitFlow.value == CurrencyUnit.USD) { warn(Currency.USD_READONLY_MESSAGE); return }
        val depts = if (db.depts.any { it.uid == dept.uid }) {
            db.depts.map { if (it.uid == dept.uid) dept else it }
        } else {
            db.depts + dept
        }
        viewModelScope.launch { persist(db.copy(depts = depts)) }
    }

    fun deleteDept(uid: String) {
        val db = dbFlow.value ?: return
        if (unitFlow.value == CurrencyUnit.USD) { warn(Currency.USD_READONLY_MESSAGE); return }
        viewModelScope.launch { persist(db.copy(depts = db.depts.filter { it.uid != uid })) }
    }

    fun reorderDepts(fromUid: String, toUid: String) {
        val db = dbFlow.value ?: return
        val from = db.depts.indexOfFirst { it.uid == fromUid }
        val to = db.depts.indexOfFirst { it.uid == toUid }
        if (from == -1 || to == -1 || from == to) return
        viewModelScope.launch { persist(db.copy(depts = Reorder.reorderArray(db.depts, from, to))) }
    }

    // -----------------------------------------------------------------------
    // Checks — App.tsx handle*Check (same order of writes as the web app)
    // -----------------------------------------------------------------------

    fun saveCheck(check: Check, isEditing: Boolean) {
        val db = dbFlow.value ?: return
        val checks = if (isEditing) {
            db.checks.map { if (it.uid == check.uid) check else it }
        } else {
            listOf(check) + db.checks
        }
        viewModelScope.launch {
            persist(db.copy(checks = checks))
            emit(if (isEditing) "چک ویرایش شد." else "چک ثبت شد.", ToastKind.SUCCESS)
        }
    }

    /** Removes only the check; transactions already recorded for it stay in the accounts (like the web). */
    fun deleteCheck(uid: String) {
        val db = dbFlow.value ?: return
        viewModelScope.launch { persist(db.copy(checks = db.checks.filter { it.uid != uid })) }
    }

    fun setCheckStatus(uid: String, status: CheckStatus) {
        val db = dbFlow.value ?: return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            persist(
                db.copy(
                    checks = db.checks.map {
                        if (it.uid != uid) it
                        else it.copy(status = status, passedAt = if (status == CheckStatus.PASSED) now else it.passedAt)
                    },
                ),
            )
        }
    }

    fun revertCheckToPending(uid: String) {
        val db = dbFlow.value ?: return
        val check = db.checks.firstOrNull { it.uid == uid } ?: return
        if (check.status == CheckStatus.PENDING) return
        // Undoing an own-check pass also removes the expense transaction it created.
        val txUid = check.passedTransactionUid.takeIf { check.kind == CheckKind.OWN && check.status == CheckStatus.PASSED && !it.isNullOrEmpty() }
        viewModelScope.launch {
            persist(
                db.copy(
                    transactions = if (txUid != null) db.transactions.filter { it.uid != txUid } else db.transactions,
                    transactionDetails = if (txUid != null) db.transactionDetails.filter { it.transactionUid != txUid } else db.transactionDetails,
                    checks = db.checks.map {
                        if (it.uid != uid) it
                        else it.copy(status = CheckStatus.PENDING, passedTransactionUid = null, passedAt = null)
                    },
                ),
            )
        }
    }

    /** Own check passed: writes the expense transaction from the check's account (buildCheckTx in App.tsx). */
    fun passOwnCheck(uid: String) {
        val db = dbFlow.value ?: return
        val check = db.checks.firstOrNull { it.uid == uid } ?: return
        if (check.kind != CheckKind.OWN || check.status != CheckStatus.PENDING) return
        val accountUid = check.accountUid
        if (accountUid.isNullOrEmpty() || db.accounts.none { it.uid == accountUid }) {
            warn("حساب این چک پیدا نشد؛ ابتدا چک را ویرایش و حساب را انتخاب کنید.")
            return
        }
        val now = System.currentTimeMillis()
        val amount = Math.abs(check.amount)
        val tx = Transaction(
            uid = "chk_pass_" + now.toString(36) + java.util.UUID.randomUUID().toString().take(3),
            subjectUid = check.subjectUid,
            transactionType = 1,
            totalValue = amount,
            dateTime = now,
            persianDate = JalaliDate.formatJalaliDate(now),
            projectUid = check.projectUid,
            personUid = check.payeePersonUid,
            sourceAccountUid = accountUid,
            desc = "پاس شدن چک" + (check.checkNumber?.let { " شماره $it" } ?: "") + (check.desc?.let { " — $it" } ?: ""),
            isHidden = 0,
            checkUid = check.uid,
        )
        viewModelScope.launch {
            persist(
                db.copy(
                    transactions = listOf(tx) + db.transactions,
                    transactionDetails = Transactions.syncDetails(db.transactionDetails, tx),
                    checks = db.checks.map {
                        if (it.uid != uid) it
                        else it.copy(status = CheckStatus.PASSED, passedTransactionUid = tx.uid, passedAt = now)
                    },
                ),
            )
            emit("پاس شدن چک ثبت شد و از موجودی حساب کسر گردید.", ToastKind.SUCCESS)
        }
    }

    /** Person check deposit: one expense transaction + a deposit row, capped at the remaining amount. */
    fun addCheckDeposit(checkUid: String, accountUid: String, amount: Double) {
        val db = dbFlow.value ?: return
        val check = db.checks.firstOrNull { it.uid == checkUid } ?: return
        if (check.kind != CheckKind.PERSON) return
        val remaining = Checks.checkRemaining(check, db.transactions)
        val value = Math.min(Math.abs(Math.round(amount).toDouble()), remaining)
        if (!(value > 0)) return
        val now = System.currentTimeMillis()
        val drawerName = db.persons.firstOrNull { it.uid == check.drawerPersonUid }?.fullName ?: ""
        val tx = Transaction(
            uid = "chk_dep_" + now.toString(36) + java.util.UUID.randomUUID().toString().take(3),
            subjectUid = check.subjectUid,
            transactionType = 1,
            totalValue = value,
            dateTime = now,
            persianDate = JalaliDate.formatJalaliDate(now),
            projectUid = check.projectUid,
            personUid = check.drawerPersonUid,
            sourceAccountUid = accountUid,
            desc = "واریز برای چک $drawerName" + (check.checkNumber?.let { " شماره $it" } ?: ""),
            isHidden = 0,
            checkUid = check.uid,
        )
        val deposit = CheckDeposit(
            uid = "dep_" + tx.uid,
            accountUid = accountUid,
            amount = value,
            dateTime = tx.dateTime,
            transactionUid = tx.uid,
        )
        viewModelScope.launch {
            persist(
                db.copy(
                    transactions = listOf(tx) + db.transactions,
                    transactionDetails = Transactions.syncDetails(db.transactionDetails, tx),
                    checks = db.checks.map { if (it.uid != checkUid) it else it.copy(deposits = it.deposits + deposit) },
                ),
            )
            emit(if (value >= remaining) "واریز کامل شد." else "واریز ثبت شد.", ToastKind.SUCCESS)
        }
    }

    // -----------------------------------------------------------------------
    // Settings — subjects (settings → categories) and projects
    // -----------------------------------------------------------------------

    fun updateSubject(subject: Subject) {
        val db = dbFlow.value ?: return
        if (unitFlow.value == CurrencyUnit.USD) { warn(Currency.USD_READONLY_MESSAGE); return }
        val subjects = if (db.subjects.any { it.uid == subject.uid }) {
            db.subjects.map { if (it.uid == subject.uid) subject else it }
        } else db.subjects + subject
        viewModelScope.launch { persist(db.copy(subjects = subjects)) }
    }

    fun deleteSubject(uid: String) {
        val db = dbFlow.value ?: return
        if (unitFlow.value == CurrencyUnit.USD) { warn(Currency.USD_READONLY_MESSAGE); return }
        viewModelScope.launch { persist(db.copy(subjects = db.subjects.filter { it.uid != uid })) }
    }

    fun addProject(project: Project) {
        val db = dbFlow.value ?: return
        if (unitFlow.value == CurrencyUnit.USD) { warn(Currency.USD_READONLY_MESSAGE); return }
        viewModelScope.launch { persist(db.copy(projects = db.projects + project)) }
    }

    fun renameProject(uid: String, title: String) {
        val db = dbFlow.value ?: return
        val t = title.trim()
        if (t.isEmpty()) return
        if (unitFlow.value == CurrencyUnit.USD) { warn(Currency.USD_READONLY_MESSAGE); return }
        viewModelScope.launch { persist(db.copy(projects = db.projects.map { if (it.uid == uid) it.copy(title = t) else it })) }
    }

    fun deleteProject(uid: String) {
        val db = dbFlow.value ?: return
        if (unitFlow.value == CurrencyUnit.USD) { warn(Currency.USD_READONLY_MESSAGE); return }
        viewModelScope.launch { persist(db.copy(projects = db.projects.filter { it.uid != uid })) }
    }

    /** SettingsView "پاک‌سازی کامل داده‌ها": empty database, kept daily USD rates. */
    fun resetData() {
        val db = dbFlow.value ?: return
        viewModelScope.launch {
            persist(DatabaseStore.emptyDatabase().copy(exchangeRates = db.exchangeRates))
            emit("همه داده‌ها پاک شد.", ToastKind.SUCCESS)
        }
    }
}
