package com.example.taskorganizer

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

internal enum class RoutineNotice(val key: String, val day: DayOfWeek?, val hour: Int, val title: Int, val body: Int) {
    LIST("list", null, 8, R.string.routine_list_title, R.string.routine_list_body),
    WORK("work", DayOfWeek.WEDNESDAY, 19, R.string.routine_work_title, R.string.routine_work_body),
    WEEKLY("weekly", DayOfWeek.SUNDAY, 18, R.string.routine_weekly_title, R.string.routine_weekly_body)
}

internal object RoutineTiming {
    fun next(notice: RoutineNotice, now: ZonedDateTime): ZonedDateTime {
        var date = now.toLocalDate()
        if (notice.day != null) date = date.with(TemporalAdjusters.nextOrSame(notice.day))
        var result = ZonedDateTime.of(date, LocalTime.of(notice.hour, 0), now.zone)
        if (!result.isAfter(now)) {
            date = if (notice.day == null) date.plusDays(1) else date.plusWeeks(1)
            result = ZonedDateTime.of(date, LocalTime.of(notice.hour, 0), now.zone)
        }
        return result
    }
}

/** Local notices, scheduled without reading the user's tasks. */
internal object RoutineNotifications {
    private const val PREFS = "task_organizer_settings"
    private const val EXTRA_KIND = "kind"
    private const val EXTRA_WHEN = "scheduled_at"
    private const val NOTIFICATION_BASE = 99310
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun enabled(context: Context) = prefs(context).getBoolean("privacy_acknowledged", false)

    private fun pending(context: Context, notice: RoutineNotice, scheduledAt: Long, flags: Int): PendingIntent? {
        val intent = Intent(context, RoutineNotificationReceiver::class.java)
            .setAction("com.example.taskorganizer.ROUTINE.${notice.key}")
            .putExtra(EXTRA_KIND, notice.key).putExtra(EXTRA_WHEN, scheduledAt)
        return PendingIntent.getBroadcast(context, notice.ordinal, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun cancel(context: Context, notice: RoutineNotice) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        pending(context, notice, 0L, PendingIntent.FLAG_NO_CREATE)?.let { alarms.cancel(it); it.cancel() }
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_BASE + notice.ordinal)
    }

    fun cancelAll(context: Context) = RoutineNotice.entries.forEach { cancel(context, it) }
    fun rescheduleAll(context: Context) = RoutineNotice.entries.forEach { schedule(context, it) }

    private fun schedule(context: Context, notice: RoutineNotice) {
        if (!enabled(context)) return
        val at = RoutineTiming.next(notice, ZonedDateTime.now()).toInstant().toEpochMilli()
        val alarm = pending(context, notice, at, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarm)
    }

    fun receive(context: Context, intent: Intent) {
        val notice = RoutineNotice.entries.firstOrNull { it.key == intent.getStringExtra(EXTRA_KIND) } ?: return
        if (!enabled(context)) return
        try {
            val scheduled = intent.getLongExtra(EXTRA_WHEN, 0L)
            val zone = ZoneId.systemDefault()
            val current = ZonedDateTime.now(zone)
            val due = if (scheduled > 0) java.time.Instant.ofEpochMilli(scheduled).atZone(zone) else return
            // Do not deliver an old notice the next day after a long delay or time-zone change.
            if (due.toLocalDate() != current.toLocalDate() || current.toInstant().toEpochMilli() < scheduled) return
            val deliveryDateKey = "routine_notice_last_${notice.key}"
            val today = current.toLocalDate().toString()
            if (prefs(context).getString(deliveryDateKey, null) == today) return
            if (!TaskReminderScheduler.notificationsAllowed(context)) return
            val manager = context.getSystemService(NotificationManager::class.java)
            TaskReminderScheduler.ensureNotificationChannels(context)
            val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val text = context.getString(notice.body)
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(context, TaskReminderScheduler.SUGGESTIONS_CHANNEL) else Notification.Builder(context)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) builder.setDefaults(Notification.DEFAULT_SOUND)
            val notification = builder.setSmallIcon(R.drawable.ic_stat_task_organizer)
                .setContentTitle(context.getString(notice.title)).setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text)).setContentIntent(open)
                .setAutoCancel(true).build()
            if (runCatching { manager.notify(NOTIFICATION_BASE + notice.ordinal, notification) }.isSuccess) {
                prefs(context).edit().putString(deliveryDateKey, today).apply()
            }
        } finally {
            schedule(context, notice)
        }
    }
}

class RoutineNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = RoutineNotifications.receive(context, intent)
}
