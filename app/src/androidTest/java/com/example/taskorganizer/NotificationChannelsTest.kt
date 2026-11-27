package com.example.taskorganizer

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationChannelsTest {
    @Test fun registersOnlyTheReminderAndAlarmChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val context = ApplicationProvider.getApplicationContext<Context>()
        TaskReminderScheduler.ensureNotificationChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        assertNotNull(manager.getNotificationChannel(TaskReminderScheduler.REMINDER_CHANNEL))
        assertNotNull(manager.getNotificationChannel(TaskReminderScheduler.ALARM_CHANNEL))
        assertNull(manager.getNotificationChannel("optional_inactivity_reminders"))
        assertNull(manager.getNotificationChannel("optional_routine_notices"))
        val existingSound = manager.getNotificationChannel(TaskReminderScheduler.ALARM_CHANNEL).sound
        val existingImportance = manager.getNotificationChannel(TaskReminderScheduler.ALARM_CHANNEL).importance
        TaskReminderScheduler.ensureNotificationChannels(context)
        assertEquals(existingSound, manager.getNotificationChannel(TaskReminderScheduler.ALARM_CHANNEL).sound)
        assertEquals(existingImportance, manager.getNotificationChannel(TaskReminderScheduler.ALARM_CHANNEL).importance)
    }
}
