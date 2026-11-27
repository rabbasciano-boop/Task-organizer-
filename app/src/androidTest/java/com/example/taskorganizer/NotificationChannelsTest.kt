package com.example.taskorganizer

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Test

class NotificationChannelsTest {
    @Test fun registersOptionalChannelsBeforeFirstNotification() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val context = ApplicationProvider.getApplicationContext<Context>()
        TaskReminderScheduler.ensureNotificationChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        assertNotNull(manager.getNotificationChannel("optional_inactivity_reminders"))
        assertNotNull(manager.getNotificationChannel("optional_routine_notices"))
    }
}
