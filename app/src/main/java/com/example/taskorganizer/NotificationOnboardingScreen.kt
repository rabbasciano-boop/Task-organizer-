package com.example.taskorganizer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal fun notificationSetupNeedsPermission(pending: Boolean, allowed: Boolean): Boolean = pending && !allowed

@Composable
internal fun NotificationOnboardingScreen(onEnable: () -> Unit) {
    Box(Modifier.fillMaxSize().organizerBackground(), contentAlignment = Alignment.Center) {
        Button(
            onClick = onEnable,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)
        ) { Text(stringResource(R.string.notification_setup_button), fontWeight = FontWeight.Bold) }
    }
}
