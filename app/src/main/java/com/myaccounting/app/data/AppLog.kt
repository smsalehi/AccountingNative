package com.myaccounting.app.data

import com.myaccounting.app.domain.JalaliDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Port of utils/logger.ts: in-memory event log (the web kept it in localStorage; native keeps it
 * for the current session). Newest first, capped like the original.
 */
object AppLog {
    enum class Level { INFO, SUCCESS, WARNING, ERROR }
    enum class Category(val title: String) {
        APP("برنامه"), DATABASE("دیتابیس"), IMPORT("ایمپورت"), EXPORT("خروجی"),
        TRANSACTION("تراکنش"), ACCOUNT("حساب"), BACKUP("پشتیبان"), SETTINGS("تنظیمات"), ERROR("خطای سیستمی"),
    }

    data class Entry(
        val id: Long,
        val timestamp: Long,
        /** «۱۴۰۳/۶/۱۵ ۰۹:۳۰» — the web's jalali column. */
        val jalali: String,
        val level: Level,
        val category: Category,
        val message: String,
        val details: String? = null,
    )

    private const val MAX = 500
    private val nextId = java.util.concurrent.atomic.AtomicLong(1)
    private val logsFlow = MutableStateFlow<List<Entry>>(emptyList())
    val logs: StateFlow<List<Entry>> = logsFlow

    fun add(level: Level, category: Category, message: String, details: String? = null) {
        val e = Entry(nextId.getAndIncrement(), System.currentTimeMillis(), nowLabel(), level, category, message, details?.takeIf { it.isNotBlank() })
        logsFlow.value = (listOf(e) + logsFlow.value).take(MAX)
    }
    fun info(category: Category, message: String, details: String? = null) = add(Level.INFO, category, message, details)
    fun success(category: Category, message: String, details: String? = null) = add(Level.SUCCESS, category, message, details)
    fun warning(category: Category, message: String, details: String? = null) = add(Level.WARNING, category, message, details)
    fun error(category: Category, message: String, details: String? = null) = add(Level.ERROR, category, message, details)
    fun clear() { logsFlow.value = emptyList() }

    private fun nowLabel(): String {
        val cal = java.util.Calendar.getInstance()
        val j = JalaliDate.toJalali(cal.timeInMillis)
        val hh = "%02d".format(cal.get(java.util.Calendar.HOUR_OF_DAY))
        val mm = "%02d".format(cal.get(java.util.Calendar.MINUTE))
        return JalaliDate.toPersianDigits("${j.y}/${j.m}/${j.d} $hh:$mm")
    }

    /** exportLogsToFile('json') body. */
    fun toJson(): String {
        val arr = logsFlow.value.joinToString(",\n") { e ->
            """{"id":${e.id},"timestamp":${e.timestamp},"jalali":${Json.stringify(e.jalali)},""" +
                """"level":"${e.level.name.lowercase()}","category":"${e.category.name.lowercase()}",""" +
                """"message":${Json.stringify(e.message)},"details":${e.details?.let { Json.stringify(it) } ?: "null"}}"""
        }
        return "[\n$arr\n]\n"
    }

    /** exportLogsToFile('txt') body. */
    fun toTxt(): String = logsFlow.value.joinToString("\n") { e ->
        "[${e.jalali}] ${e.level.name.uppercase()}/${e.category.name.lowercase()} — ${e.message}" +
            (e.details?.let { "\n    $it" } ?: "")
    } + "\n"
}
