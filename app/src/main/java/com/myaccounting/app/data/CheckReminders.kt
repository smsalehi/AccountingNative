package com.myaccounting.app.data

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.myaccounting.app.R
import com.myaccounting.app.domain.Notifications
import kotlinx.coroutines.flow.first

/**
 * Native half of checkNotifications.ts: check reminders as exact AlarmManager alarms that post
 * local notifications. Rescheduling cancels every pending reminder first (same ids), so edited /
 * passed / deleted checks never leave a stale alarm behind.
 */
object CheckReminders {
    const val CHANNEL_ID = "checks"
    const val EXTRA_ID = "notif_id"
    const val EXTRA_TITLE = "notif_title"
    const val EXTRA_BODY = "notif_body"

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "یادآوری چک‌ها", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "یادآوری سررسید چک‌ها و مهلت واریز"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
    }

    /** Whether the system lets us set exact alarms (API 31+; USE_EXACT_ALARM covers most installs). */
    fun canScheduleExact(context: Context): Boolean? {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return null
        return try {
            am.canScheduleExactAlarms()
        } catch (_: Exception) {
            null
        }
    }

    fun openExactAlarmSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun notificationsGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun pendingIntent(context: Context, id: Int, title: String, body: String): PendingIntent {
        val intent = Intent(context, CheckReminderReceiver::class.java)
            .putExtra(EXTRA_ID, id)
            .putExtra(EXTRA_TITLE, title)
            .putExtra(EXTRA_BODY, body)
        return PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** لغو همه یادآوری‌های قبلی و زمان‌بندی برنامه جدید. Returns the scheduled count. */
    fun reschedule(
        context: Context,
        planned: List<Notifications.PlannedReminder>,
    ): Int {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return 0
        // لغو همه یادآوری‌های قبلی: شناسه‌ها از کلید چک ساخته می‌شوند، پس همان‌ها را با intent خالی لغو می‌کنیم
        val cancelIntent = Intent(context, CheckReminderReceiver::class.java)
        for (p in planned) {
            am.cancel(PendingIntent.getBroadcast(context, p.id, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        }
        if (planned.isEmpty()) return 0
        if (!notificationsGranted(context)) return 0

        ensureChannel(context)
        val exact = canScheduleExact(context) != false
        var scheduled = 0
        for (p in planned) {
            val pi = pendingIntent(context, p.id, p.title, p.body)
            try {
                if (exact) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, p.atMs, pi)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, p.atMs, pi)
                }
                scheduled++
            } catch (_: SecurityException) {
                try {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, p.atMs, pi)
                    scheduled++
                } catch (_: Exception) {
                }
            }
        }
        return scheduled
    }
}

/** Posts the reminder notification when the alarm fires. */
class CheckReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(CheckReminders.EXTRA_ID, 0)
        val title = intent.getStringExtra(CheckReminders.EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(CheckReminders.EXTRA_BODY) ?: ""
        if (!CheckReminders.notificationsGranted(context)) return
        CheckReminders.ensureChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification: Notification = NotificationCompat.Builder(context, CheckReminders.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_lucide_calendar_check)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build()
        nm.notify(id, notification)
    }
}

/** Re-arms the reminders after a reboot (alarms do not survive one). */
class CheckReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val result = DatabaseStore(context).load()
        if (result !is DatabaseStore.LoadResult.Ok) return
        val prefs = AppPrefs(context)
        val (enabled, hour) = kotlinx.coroutines.runBlocking {
            val e = prefs.checkReminderEnabled.first()
            val h = prefs.checkReminderHour.first()
            e to h
        }
        val planned = Notifications.planCheckNotifications(
            result.db.checks,
            result.db.transactions,
            result.db.persons,
            Notifications.CheckReminderPrefs(enabled, hour),
            com.myaccounting.app.domain.CurrencyUnit.TOMAN,
        )
        CheckReminders.reschedule(context, planned)
    }
}
