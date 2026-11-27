package com.example.taskorganizer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
internal fun NotificationSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var enabled by remember { mutableStateOf(InactivityNotifications.enabled(context)) }
    val routineStates = remember { mutableStateMapOf<RoutineNotice, Boolean>().apply { RoutineNotice.entries.forEach { put(it, RoutineNotifications.enabled(context, it)) } } }
    var allowed by remember { mutableStateOf(TaskReminderScheduler.notificationsAllowed(context)) }
    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) allowed = TaskReminderScheduler.notificationsAllowed(context)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    BackHandler(onBack = onBack)
    PageColumn {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text(stringResource(R.string.back_to_settings), color = Gold) }
        Text(stringResource(R.string.inactivity_settings_title), color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.inactivity_toggle_title), color = MainText, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.inactivity_toggle_description), color = SoftText)
                    }
                    Switch(checked = enabled, onCheckedChange = { enabled = it; InactivityNotifications.setEnabled(context, it) })
                }
            }
        }
        RoutineNotice.entries.forEach { notice ->
            val description = when (notice) {
                RoutineNotice.LIST -> R.string.routine_list_description
                RoutineNotice.WORK -> R.string.routine_work_description
                RoutineNotice.WEEKLY -> R.string.routine_weekly_description
            }
            Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(notice.title), color = MainText, fontWeight = FontWeight.Bold)
                        Text(stringResource(description), color = SoftText)
                    }
                    Switch(checked = routineStates[notice] == true, onCheckedChange = {
                        routineStates[notice] = it
                        RoutineNotifications.setEnabled(context, notice, it)
                    })
                }
            }
        }
        if ((enabled || routineStates.values.any { it }) && !allowed) {
            Text(stringResource(R.string.inactivity_permission_notice), color = SoftText)
            TextButton(onClick = {
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                } else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                context.startActivity(intent)
            }) { Text(stringResource(R.string.inactivity_open_android_settings), color = Gold) }
        }
    }
}
