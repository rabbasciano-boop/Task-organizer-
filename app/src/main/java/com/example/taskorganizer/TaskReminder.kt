package com.example.taskorganizer

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object TaskReminderScheduler {
    private const val ACTION_REMINDER = "com.example.taskorganizer.REMINDER"
    private const val EXTRA_TITLE = "title"
    private const val EXTRA_TIME = "time"
    private const val EXTRA_ALARM = "alarm"

    fun schedule(context: Context, task: OrganizerTask) {
        cancel(context, task)
        if (!task.reminderEnabled || task.completed || task.time == null) return
        val taskMoment = runCatching { LocalDateTime.of(LocalDate.parse(task.date), LocalTime.parse(task.time)) }.getOrNull() ?: return
        val startAt = taskMoment.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        var triggerAt = taskMoment.minusMinutes(task.reminderMinutesBefore.toLong()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        var index = 0
        while (triggerAt <= startAt && index < MAX_SCHEDULED_REMINDERS) {
            if (triggerAt > System.currentTimeMillis()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, reminderIntent(context, task, index))
            }
            if (task.reminderRepeatMinutes <= 0) break
            triggerAt += task.reminderRepeatMinutes * 60_000L
            index++
        }
    }

    fun cancel(context: Context, task: OrganizerTask) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        repeat(MAX_SCHEDULED_REMINDERS) { index -> alarmManager.cancel(reminderIntent(context, task, index)) }
    }

    fun rescheduleAll(context: Context) {
        EncryptedTaskStore(context).load().tasks.forEach { schedule(context, it) }
    }

    private fun reminderIntent(context: Context, task: OrganizerTask, index: Int): PendingIntent {
        val intent = Intent(context, TaskReminderReceiver::class.java)
            .setAction(ACTION_REMINDER)
            .putExtra(EXTRA_TITLE, task.title)
            .putExtra(EXTRA_TIME, task.time)
            .putExtra(EXTRA_ALARM, task.alarmEnabled)
        return PendingIntent.getBroadcast(context, task.id.hashCode() + index, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    internal fun title(intent: Intent) = intent.getStringExtra(EXTRA_TITLE).orEmpty()
    internal fun time(intent: Intent) = intent.getStringExtra(EXTRA_TIME).orEmpty()
    internal fun isAlarm(intent: Intent) = intent.getBooleanExtra(EXTRA_ALARM, false)

    private const val MAX_SCHEDULED_REMINDERS = 48
}

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val isAlarm = TaskReminderScheduler.isAlarm(intent)
        val channelId = if (isAlarm) "task_alarm_reminders" else "task_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel("task_reminders", "Promemoria attività", NotificationManager.IMPORTANCE_HIGH))
            val alarmChannel = NotificationChannel("task_alarm_reminders", "Sveglie attività", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Promemoria con suono e vibrazione"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 700)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
                )
            }
            manager.createNotificationChannel(alarmChannel)
        }
        val openApp = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(context, channelId) else Notification.Builder(context)
        manager.notify(intent.dataString?.hashCode() ?: TaskReminderScheduler.title(intent).hashCode(), builder
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(if (isAlarm) "Promemoria importante tra 15 minuti" else "Tra 15 minuti")
            .setContentText("${TaskReminderScheduler.title(intent)} alle ${TaskReminderScheduler.time(intent)}")
            .setContentIntent(openApp).setAutoCancel(true)
            .setCategory(if (isAlarm) Notification.CATEGORY_ALARM else Notification.CATEGORY_REMINDER)
            .setDefaults(if (isAlarm) Notification.DEFAULT_ALL else 0)
            .build())
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TaskReminderScheduler.rescheduleAll(context)
    }
}
