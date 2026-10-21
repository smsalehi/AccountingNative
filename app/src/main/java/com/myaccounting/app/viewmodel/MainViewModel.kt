package com.myaccounting.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myaccounting.app.data.AppPrefs
import com.myaccounting.app.data.DatabaseStore
import com.myaccounting.app.data.ThemeMode
import com.myaccounting.app.domain.AccountingDatabase
import com.myaccounting.app.domain.Balance
import com.myaccounting.app.domain.Checks
import com.myaccounting.app.domain.LoanSchedule
import com.myaccounting.app.domain.Notifications
import com.myaccounting.app.domain.Reorder
import com.myaccounting.app.domain.BackupCodec
import com.myaccounting.app.domain.CurrencyUnit
import com.myaccounting.app.domain.DatabaseCodec
import com.myaccounting.app.domain.ExchangeRates
import com.myaccounting.app.domain.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

    val state: StateFlow<UiState> = combine(dbFlow, unitFlow, toastFlow) { db, unit, toast ->
        if (db == null) {
            UiState(loaded = false, unit = unit, effectiveUnit = unit, toast = toast)
        } else {
            val idx = ExchangeRates.buildRateIndex(db.exchangeRates)
            val effective = if (unit == CurrencyUnit.USD && idx == null) CurrencyUnit.TOMAN else unit
            val shown = if (effective == CurrencyUnit.USD && idx != null) ExchangeRates.buildUsdViewDb(db, idx) else db
            UiState(
                loaded = true, unit = unit, db = db, shownDb = shown,
                balances = Balance.computeAccountBalances(shown), rateIndex = idx, toast = toast, effectiveUnit = effective,
                urgentNotifications = Notifications.urgentCount(db),
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

    /** Screens/dialogs that are not ported yet say so instead of silently doing nothing. */
    fun notPorted(what: String) { emit("$what — این بخش هنوز در نسخه نیتیو منتقل نشده است.", ToastKind.WARNING) }

    fun warn(message: String) { emit(message, ToastKind.WARNING) }

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
            } catch (e: Exception) {
                emit("ایمپورت ناموفق بود: ${e.message ?: e.javaClass.simpleName}", ToastKind.ERROR)
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
            } catch (e: Exception) {
                emit("ذخیره بکاپ ناموفق بود: ${e.message ?: e.javaClass.simpleName}", ToastKind.ERROR)
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
}
