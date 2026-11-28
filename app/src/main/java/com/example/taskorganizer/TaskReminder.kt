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
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object TaskReminderScheduler {
    internal const val REMINDER_CHANNEL = "task_reminders"
    internal const val ALARM_CHANNEL = "task_alarm_reminders"
    private const val ACTION_REMINDER = "com.example.taskorganizer.REMINDER"
    private const val EXTRA_TASK_ID = "task_id"
    private const val EXTRA_INDEX = "reminder_index"
    private const val EXTRA_TITLE = "title"
    private const val EXTRA_TIME = "time"
    private const val EXTRA_TASK_START = "task_start"
    private const val EXTRA_ALARM = "alarm"
    private const val EXTRA_PROFILE = "profile_id"
    private const val EXTRA_SNOOZE_COUNT = "snooze_count"
    internal const val SNOOZE_INDEX = 48

    /** Schedules the requested reminders and always keeps the final, most useful alert. */
    fun schedule(context: Context, task: OrganizerTask, profileId: String = LocalProfiles.currentId()) {
        cancel(context, task.id, profileId)
        if (LocalProfiles.currentId() != profileId) return
        if (task.deletedAt > 0L || !task.reminderEnabled || task.completed || task.time == null) return
        val taskMoment = runCatching {
            LocalDateTime.of(LocalDate.parse(task.date), LocalTime.parse(task.time))
        }.getOrNull() ?: return
        val taskStartAt = taskMoment.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val triggerTimes = ReminderTiming.plannedTriggerTimes(taskStartAt, task.reminderMinutesBefore, task.reminderRepeatMinutes)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        triggerTimes.forEachIndexed { index, triggerAt ->
            if (triggerAt > System.currentTimeMillis()) {
                val pendingIntent = reminderIntent(context, task, index, taskStartAt, profileId)
                // A repeating reminder stays battery-friendly; its final scheduled alert is the precise alarm.
                val useExactAlarm = task.alarmEnabled && index == triggerTimes.lastIndex && canUseExactAlarms(alarmManager)
                if (useExactAlarm) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                }
            }
        }
    }

    /** Cancels only PendingIntents that already exist. */
    fun cancel(context: Context, task: OrganizerTask, profileId: String = LocalProfiles.currentId()) = cancel(context, task.id, profileId)

    /** Cancels all reminders identified by one task id. */
    fun cancel(context: Context, taskId: String, profileId: String = LocalProfiles.currentId()) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        repeat(MAX_SCHEDULED_REMINDERS + 1) { index ->
            existingReminderIntent(context, taskId, index, profileId)?.let { pendingIntent ->
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    internal fun scheduleSnooze(context: Context, task: OrganizerTask, profileId: String, count: Int, now: Long) {
        if (!LocalProfiles.mayAccess(profileId, LocalProfiles.currentId(), LocalProfiles.isSwitching())) return
        val time = task.time ?: return
        val startAt = runCatching { LocalDateTime.of(LocalDate.parse(task.date), LocalTime.parse(time)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }.getOrNull() ?: return
        val pending = reminderIntent(context, task, SNOOZE_INDEX, startAt, profileId, count)
        val alarms = context.getSystemService(AlarmManager::class.java)
        val triggerAt = snoozeTriggerAt(now)
        if (canUseExactAlarms(alarms)) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        // Snoozes are intentionally not persisted across a phone reboot.
    }

    fun rescheduleAll(context: Context) {
        val profileId = LocalProfiles.currentId()
        EncryptedTaskStore(context, profileId = profileId).load().tasks.forEach { schedule(context, it, profileId) }
    }

    fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(REMINDER_CHANNEL, "Promemoria", NotificationManager.IMPORTANCE_HIGH)
        )
        val alarmChannel = NotificationChannel(ALARM_CHANNEL, "Sveglie", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Promemoria con suono e vibrazione"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 700)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
            )
        }
        manager.createNotificationChannel(alarmChannel)
        manager.deleteNotificationChannel("optional_inactivity_reminders")
        manager.deleteNotificationChannel("optional_routine_notices")
    }

    fun notificationsAllowed(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return permissionGranted && manager.areNotificationsEnabled()
    }

    fun canUseExactAlarms(context: Context): Boolean = canUseExactAlarms(context.getSystemService(AlarmManager::class.java))

    private fun canUseExactAlarms(alarmManager: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun reminderIntent(context: Context, task: OrganizerTask, index: Int, taskStartAt: Long, profileId: String, snoozeCount: Int = 0): PendingIntent {
        val intent = reminderBroadcastIntent(context, task.id, index, profileId)
            .putExtra(EXTRA_TITLE, task.title)
            .putExtra(EXTRA_TIME, task.time)
            .putExtra(EXTRA_TASK_START, taskStartAt)
            .putExtra(EXTRA_ALARM, task.alarmEnabled)
            .putExtra(EXTRA_SNOOZE_COUNT, snoozeCount)
        // Il request code combina l'ID della task con l'indice per evitare collisioni
        // tra PendingIntent di task diverse con lo stesso indice di promemoria.
        return PendingIntent.getBroadcast(context, reminderRequestCode(task.id, index), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun existingReminderIntent(context: Context, taskId: String, index: Int, profileId: String): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            reminderRequestCode(taskId, index),
            reminderBroadcastIntent(context, taskId, index, profileId),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

    /** Combina l'hash dell'ID task con l'indice per garantire unicità del request code. */
    private fun reminderRequestCode(taskId: String, index: Int): Int =
        (taskId.hashCode() * 31 + index) and Int.MAX_VALUE

    private fun reminderBroadcastIntent(context: Context, taskId: String, index: Int, profileId: String): Intent =
        Intent(context, TaskReminderReceiver::class.java)
            .setAction(ACTION_REMINDER)
            .setData(android.net.Uri.parse("taskorganizer://reminder/$profileId/$taskId/$index"))
            .putExtra(EXTRA_TASK_ID, taskId)
            .putExtra(EXTRA_INDEX, index)
            .putExtra(EXTRA_PROFILE, profileId)

    internal fun taskId(intent: Intent) = intent.getStringExtra(EXTRA_TASK_ID).orEmpty()
    internal fun reminderIndex(intent: Intent) = intent.getIntExtra(EXTRA_INDEX, 0)
    internal fun title(intent: Intent) = intent.getStringExtra(EXTRA_TITLE).orEmpty()
    internal fun time(intent: Intent) = intent.getStringExtra(EXTRA_TIME).orEmpty()
    internal fun taskStartAt(intent: Intent) = intent.getLongExtra(EXTRA_TASK_START, 0L)
    internal fun isAlarm(intent: Intent) = intent.getBooleanExtra(EXTRA_ALARM, false)
    internal fun profileId(intent: Intent) = intent.getStringExtra(EXTRA_PROFILE).orEmpty()
    internal fun snoozeCount(intent: Intent) = intent.getIntExtra(EXTRA_SNOOZE_COUNT, 0)
    internal fun isForCurrentProfile(intent: Intent): Boolean =
        LocalProfiles.mayAccess(intent.getStringExtra(EXTRA_PROFILE), LocalProfiles.currentId(), LocalProfiles.isSwitching())

    internal fun remainingLabel(startAt: Long, now: Long = System.currentTimeMillis()): String =
        ReminderTiming.remainingLabel(startAt, now)

    private const val MAX_SCHEDULED_REMINDERS = 48
}

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (TaskReminderScheduler.reminderIndex(intent) == TaskReminderScheduler.SNOOZE_INDEX) {
            val pendingResult = goAsync()
            Thread {
                try {
                    if (!TaskReminderScheduler.isForCurrentProfile(intent)) return@Thread
                    val store = EncryptedTaskStore(context.applicationContext, profileId = TaskReminderScheduler.profileId(intent))
                    val current = if (store.hasArchive()) store.load().tasks.firstOrNull { it.id == TaskReminderScheduler.taskId(intent) } else null
                    if (current != null && !current.completed && current.deletedAt == 0L && current.reminderEnabled && current.time != null) show(context, intent)
                } catch (error: Exception) {
                    android.util.Log.e("TaskReminder", "Could not verify postponed reminder", error)
                } finally {
                    pendingResult.finish()
                }
            }.start()
            return
        }
        show(context, intent)
    }

    private fun show(context: Context, intent: Intent) {
        if (!TaskReminderScheduler.isForCurrentProfile(intent)) return
        if (!TaskReminderScheduler.notificationsAllowed(context)) return
        TaskReminderScheduler.ensureNotificationChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val isAlarm = TaskReminderScheduler.isAlarm(intent)
        val channelId = if (isAlarm) TaskReminderScheduler.ALARM_CHANNEL else TaskReminderScheduler.REMINDER_CHANNEL
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val publicVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(context, channelId) else Notification.Builder(context)
        publicVersion
            .setSmallIcon(R.drawable.ic_stat_task_organizer)
            .setContentTitle("Task Organizer")
            .setContentText("Hai un promemoria")

        val message = ReminderTiming.notificationText(TaskReminderScheduler.taskStartAt(intent), System.currentTimeMillis(), TaskReminderScheduler.title(intent))
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(context, channelId) else Notification.Builder(context)
        val notificationDefaults = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> 0
            isAlarm -> Notification.DEFAULT_VIBRATE or Notification.DEFAULT_SOUND
            else -> Notification.DEFAULT_SOUND
        }
        if (isAlarm && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder
                .setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
                )
        }
        builder
            .setSmallIcon(R.drawable.ic_stat_task_organizer)
            .setContentTitle(if (isAlarm) "Promemoria importante" else "Promemoria attività")
            .setContentText("$message · ${TaskReminderScheduler.time(intent)}")
            .setStyle(Notification.BigTextStyle().bigText("$message · ${TaskReminderScheduler.time(intent)}"))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion.build())
            .setCategory(if (isAlarm) Notification.CATEGORY_ALARM else Notification.CATEGORY_REMINDER)
            .setDefaults(notificationDefaults)
        ReminderNotificationActions.add(context, builder, intent)
        val notification = builder.build()
        manager.notify(TaskReminderScheduler.taskId(intent), TaskReminderScheduler.reminderIndex(intent), notification)
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Verifica che l'azione ricevuta sia attesa: protegge da intent spoofing
        val action = intent.action ?: return
        val expected = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.TIME_SET",
            Intent.ACTION_TIMEZONE_CHANGED,
            // ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED è API 31+; usiamo la stringa letterale
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"
        )
        if (action !in expected) return
        val pendingResult = goAsync()
        Thread {
            try {
                LocalProfiles.clearUnscopedNotificationsOnce(context.applicationContext)
                TaskReminderScheduler.rescheduleAll(context.applicationContext)
            } catch (_: Exception) {
                // The encrypted archive remains untouched; recovery is available when the app opens.
            } finally {
                runCatching { InactivityNotifications.reschedule(context.applicationContext) }
                runCatching { RoutineNotifications.rescheduleAll(context.applicationContext) }
                pendingResult.finish()
            }
        }.start()
    }
}
