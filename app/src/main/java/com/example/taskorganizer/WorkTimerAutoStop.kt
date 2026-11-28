package com.example.taskorganizer

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

internal const val WORK_TIMER_LIMIT_MILLIS = 8L * 60 * 60 * 1000
private const val TIMER_ACTION = "com.example.taskorganizer.WORK_TIMER_AUTO_STOP"
private const val EXTRA_PROFILE = "timer_profile"
private const val EXTRA_START = "timer_start"
private const val TIMER_NOTIFICATION_ID = 99350

internal fun workTimerRemaining(timer: WorkTimerAutoStop, nowWall: Long, nowElapsed: Long, bootCount: Int?): Long {
    val startedElapsed = timer.startedElapsedRealtime
    val remaining = if (startedElapsed != null && timer.bootCount != null && timer.bootCount == bootCount && nowElapsed >= startedElapsed) {
        WORK_TIMER_LIMIT_MILLIS - (nowElapsed - startedElapsed)
    } else timer.deadlineAt - nowWall
    return remaining.coerceIn(0L, WORK_TIMER_LIMIT_MILLIS)
}

/** Split at clock-offset transitions so an eight-hour timer keeps eight actual hours. */
internal fun autoStoppedWorkEntries(startAt: Long, timer: WorkTimerAutoStop, zone: ZoneId): List<WorkLogEntry> {
    var cursor = Instant.ofEpochMilli(startAt).truncatedTo(ChronoUnit.MINUTES)
    val finish = cursor.plusMillis(WORK_TIMER_LIMIT_MILLIS)
    var remainingBreak = timer.breakMinutes.coerceIn(0, 480)
    val entries = mutableListOf<WorkLogEntry>()
    while (cursor.isBefore(finish)) {
        val transition = zone.rules.nextTransition(cursor.minusNanos(1))?.takeIf { it.instant.isAfter(cursor) && it.instant.isBefore(finish) }
        val end = transition?.instant ?: finish
        val minutes = Duration.between(cursor, end).toMinutes().toInt()
        val partBreak = minOf(remainingBreak, minutes)
        val localStart = java.time.LocalDateTime.ofInstant(cursor, zone)
        val localEnd = transition?.dateTimeBefore ?: java.time.LocalDateTime.ofInstant(end, zone)
        val part = timerWorkEntries(localStart, localEnd, partBreak, timer.notes)
        check(part.isNotEmpty()) { "Impossibile registrare il timer automaticamente" }
        val firstIndex = entries.size
        entries += part.mapIndexed { index, entry -> entry.copy(id = autoStoppedWorkEntryId(startAt, firstIndex + index), autoStopped = true) }
        remainingBreak -= partBreak
        cursor = end
    }
    return entries
}

internal fun autoStoppedWorkEntryId(startAt: Long, index: Int): String =
    UUID.nameUUIDFromBytes("work-timer:$startAt:$index".toByteArray(Charsets.UTF_8)).toString()

internal fun stopWorkTimerIfDue(archive: TaskArchive, expectedStart: Long, wallNow: Long, elapsedNow: Long, bootCount: Int?, zone: ZoneId): TaskArchive? {
    val timer = archive.activeWorkTimerAutoStop ?: return null
    if (archive.activeWorkTimerStartedAt != expectedStart || workTimerRemaining(timer, wallNow, elapsedNow, bootCount) > 0L) return null
    val newEntries = autoStoppedWorkEntries(expectedStart, timer, zone)
    val existingIds = archive.workLogEntries.mapTo(mutableSetOf()) { it.id }
    return archive.copy(workLogEntries = archive.workLogEntries + newEntries.filterNot { it.id in existingIds }, activeWorkTimerStartedAt = null, activeWorkTimerAutoStop = null)
}

/** A draft queued just before the deadline may finish writing after the automatic stop. */
internal fun applyWorkTimerDraft(archive: TaskArchive, expectedStart: Long, breakMinutes: Int, notes: String): TaskArchive? {
    val safeBreak = breakMinutes.coerceIn(0, 480)
    val active = archive.activeWorkTimerAutoStop
    if (archive.activeWorkTimerStartedAt == expectedStart && active != null) {
        return archive.copy(activeWorkTimerAutoStop = active.copy(breakMinutes = safeBreak, notes = notes))
    }
    val parts = (0..3).mapNotNull { index -> archive.workLogEntries.firstOrNull { it.id == autoStoppedWorkEntryId(expectedStart, index) && it.autoStopped } }
    if (parts.isEmpty() || parts.first().id != autoStoppedWorkEntryId(expectedStart, 0)) return null
    var remainingBreak = safeBreak
    val updated = parts.associate { entry ->
        val allocation = minOf(remainingBreak, workMinutes(entry.startTime, entry.endTime, 0))
        remainingBreak -= allocation
        entry.id to entry.copy(breakMinutes = allocation, notes = notes.trim())
    }
    return archive.copy(workLogEntries = archive.workLogEntries.map { updated[it.id] ?: it })
}

internal object WorkTimerAutoStopScheduler {
    fun currentBootCount(context: Context): Int? = runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT) }.getOrNull()?.takeIf { it >= 0 }

    private fun pending(context: Context, profile: String, startedAt: Long, flags: Int): PendingIntent? {
        val intent = Intent(context, WorkTimerAutoStopReceiver::class.java).setAction(TIMER_ACTION)
            .setData(Uri.Builder().scheme("taskorganizer").authority("work-timer").appendPath(profile).appendPath(startedAt.toString()).build())
            .putExtra(EXTRA_PROFILE, profile).putExtra(EXTRA_START, startedAt)
        return PendingIntent.getBroadcast(context, 0, intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    fun cancel(context: Context, profile: String, startedAt: Long) {
        pending(context, profile, startedAt, PendingIntent.FLAG_NO_CREATE)?.let {
            context.getSystemService(AlarmManager::class.java).cancel(it); it.cancel()
        }
    }

    fun arm(context: Context, profile: String, startedAt: Long, timer: WorkTimerAutoStop) {
        check(LocalProfiles.mayAccess(profile, LocalProfiles.currentId(), LocalProfiles.isSwitching()))
        val remaining = workTimerRemaining(timer, System.currentTimeMillis(), SystemClock.elapsedRealtime(), currentBootCount(context))
        val pi = pending(context, profile, startedAt, PendingIntent.FLAG_UPDATE_CURRENT) ?: error("Timer non programmabile")
        val alarms = context.getSystemService(AlarmManager::class.java)
        val trigger = SystemClock.elapsedRealtime() + remaining.coerceAtLeast(1_000L)
        if (TaskReminderScheduler.canUseExactAlarms(context)) alarms.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pi)
        else alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pi)
    }

    /** Call on a worker after start, opening the profile, reboot, or time-zone changes. */
    fun synchronize(context: Context, profile: String) {
        if (!LocalProfiles.mayAccess(profile, LocalProfiles.currentId(), LocalProfiles.isSwitching())) return
        val archive = EncryptedTaskStore(context, profileId = profile).load()
        val startedAt = archive.activeWorkTimerStartedAt ?: return
        val timer = archive.activeWorkTimerAutoStop ?: return // Existing pre-update timers remain manual.
        val remaining = workTimerRemaining(timer, System.currentTimeMillis(), SystemClock.elapsedRealtime(), currentBootCount(context))
        if (remaining == 0L) { finish(context, profile, startedAt); return }
        arm(context, profile, startedAt, timer)
    }

    fun finish(context: Context, profile: String, startedAt: Long) {
        if (!LocalProfiles.mayAccess(profile, LocalProfiles.currentId(), LocalProfiles.isSwitching())) return
        val store = EncryptedTaskStore(context, profileId = profile)
        val updated = store.updateIf { archive ->
            if (!LocalProfiles.mayAccess(profile, LocalProfiles.currentId(), LocalProfiles.isSwitching())) null
            else stopWorkTimerIfDue(archive, startedAt, System.currentTimeMillis(), SystemClock.elapsedRealtime(), currentBootCount(context), ZoneId.systemDefault())
        }
        if (updated == null) { synchronize(context, profile); return }
        runCatching { cancel(context, profile, startedAt) }.onFailure { Log.e("WorkTimerAutoStop", "Could not cancel timer alarm", it) }
        runCatching { context.sendBroadcast(Intent(ACTION_ARCHIVE_CHANGED).setPackage(context.packageName).putExtra(EXTRA_CHANGED_PROFILE, profile)) }
            .onFailure { Log.e("WorkTimerAutoStop", "Could not refresh display", it) }
        if (!LocalProfiles.mayAccess(profile, LocalProfiles.currentId(), LocalProfiles.isSwitching()) || !TaskReminderScheduler.notificationsAllowed(context)) return
        TaskReminderScheduler.ensureNotificationChannels(context)
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(context, TaskReminderScheduler.REMINDER_CHANNEL) else Notification.Builder(context)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) builder.setDefaults(Notification.DEFAULT_SOUND)
        val notification = builder.setSmallIcon(R.drawable.ic_stat_task_organizer)
            .setContentTitle(context.getString(R.string.work_timer_auto_notification_title))
            .setContentText(context.getString(R.string.work_timer_auto_notification_body))
            .setContentIntent(open).setAutoCancel(true).build()
        context.getSystemService(NotificationManager::class.java).notify(profile, TIMER_NOTIFICATION_ID, notification)
    }
}

class WorkTimerAutoStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TIMER_ACTION) return
        val profile = intent.getStringExtra(EXTRA_PROFILE).orEmpty()
        val startedAt = intent.getLongExtra(EXTRA_START, 0L)
        if (startedAt <= 0L) return
        val result = goAsync()
        Thread {
            try { WorkTimerAutoStopScheduler.finish(context.applicationContext, profile, startedAt) }
            catch (error: Exception) { Log.e("WorkTimerAutoStop", "Could not save timer", error) }
            finally { result.finish() }
        }.start()
    }
}
