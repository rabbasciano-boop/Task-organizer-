package com.example.taskorganizer

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

internal object InactivityTiming {
    private const val DAY = 24L * 60 * 60 * 1000
    val stages = listOf(3 * DAY, 14 * DAY)

    fun stageToDeliver(elapsed: Long): Int? = when {
        elapsed >= stages[1] -> 1
        elapsed >= stages[0] -> 0
        else -> null
    }
}

/** Local reminders; never reads activities or contacts a server. */
internal object InactivityNotifications {
    private const val PREFS = "task_organizer_settings"
    private const val LAST_USED = "inactivity_last_used"
    private const val SHOWN_STAGE = "inactivity_shown_stage"
    private const val BASELINE = "last_used"
    private const val STAGE = "stage"
    private const val NOTIFICATION_ID = 99281

    private fun preferences(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun enabled(context: Context): Boolean = preferences(context).getBoolean("privacy_acknowledged", false)

    fun markUsed(context: Context) {
        if (!enabled(context)) return
        preferences(context).edit().putLong(LAST_USED, System.currentTimeMillis()).putInt(SHOWN_STAGE, -1).apply()
        cancel(context)
        reschedule(context)
    }

    fun cancel(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        InactivityTiming.stages.indices.forEach { stage ->
            pendingIntent(context, stage, 0, PendingIntent.FLAG_NO_CREATE)?.let { alarms.cancel(it); it.cancel() }
        }
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    fun reschedule(context: Context) {
        if (!enabled(context)) return
        val prefs = preferences(context)
        val lastUsed = prefs.getLong(LAST_USED, 0L)
        if (lastUsed <= 0L) return
        val now = System.currentTimeMillis()
        val elapsed = now - lastUsed
        val currentStage = InactivityTiming.stageToDeliver(elapsed)
        val shownStage = prefs.getInt(SHOWN_STAGE, -1)
        val alarms = context.getSystemService(AlarmManager::class.java)
        InactivityTiming.stages.forEachIndexed { stage, offset ->
            if (stage <= shownStage || (currentStage != null && stage < currentStage)) return@forEachIndexed
            val triggerAt = maxOf(lastUsed + offset, now + 60_000L)
            val pending = pendingIntent(context, stage, lastUsed, PendingIntent.FLAG_UPDATE_CURRENT) ?: return@forEachIndexed
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun pendingIntent(context: Context, stage: Int, baseline: Long, flags: Int): PendingIntent? {
        val intent = Intent(context, InactivityReceiver::class.java)
            .setAction("com.example.taskorganizer.INACTIVITY.$stage")
            .putExtra(BASELINE, baseline).putExtra(STAGE, stage)
        return PendingIntent.getBroadcast(context, stage, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    fun receive(context: Context, intent: Intent) {
        val prefs = preferences(context)
        val baseline = intent.getLongExtra(BASELINE, 0L)
        val stage = intent.getIntExtra(STAGE, -1)
        if (!enabled(context) || baseline <= 0 || baseline != prefs.getLong(LAST_USED, 0L)) return
        if (stage != InactivityTiming.stageToDeliver(System.currentTimeMillis() - baseline) || stage <= prefs.getInt(SHOWN_STAGE, -1)) return
        if (!TaskReminderScheduler.notificationsAllowed(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        TaskReminderScheduler.ensureNotificationChannels(context)
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(context, TaskReminderScheduler.REMINDER_CHANNEL) else Notification.Builder(context)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) builder.setDefaults(Notification.DEFAULT_SOUND)
        val message = context.getString(if (stage == 0) R.string.inactivity_message_first else R.string.inactivity_message_second)
        val notification = builder.setSmallIcon(R.drawable.ic_stat_task_organizer)
            .setContentTitle(context.getString(R.string.app_name)).setContentText(message)
            .setStyle(Notification.BigTextStyle().bigText(message)).setContentIntent(open).setAutoCancel(true).build()
        if (runCatching { manager.notify(NOTIFICATION_ID, notification) }.isSuccess) {
            prefs.edit().putInt(SHOWN_STAGE, stage).apply()
        }
    }
}

class InactivityReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = InactivityNotifications.receive(context, intent)
}
