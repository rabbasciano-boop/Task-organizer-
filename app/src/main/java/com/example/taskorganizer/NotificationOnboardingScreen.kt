package com.example.taskorganizer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal fun notificationSetupNeedsPermission(pending: Boolean, allowed: Boolean): Boolean = pending && !allowed

@Composable
internal fun NotificationOnboardingScreen(onEnable: () -> Unit) {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().organizerBackground()) {
        PageColumn(safeBottom = true, safeTop = true) {
            Text(stringResource(R.string.notification_setup_title), color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.notification_setup_description), color = SoftText)
            InfoCard(
                stringResource(R.string.notification_setup_reminders_title),
                stringResource(R.string.notification_setup_reminders_description),
                stringResource(R.string.notification_setup_reminders_note)
            )
            InfoCard(
                stringResource(R.string.notification_setup_control_title),
                stringResource(R.string.notification_setup_control_description),
                stringResource(R.string.notification_setup_control_note)
            )
            Button(
                onClick = onEnable,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(top = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)
            ) { Text(stringResource(R.string.notification_setup_button), fontWeight = FontWeight.Bold) }
            Text(stringResource(R.string.notification_setup_denied), color = SoftText)
        }
    }
}
