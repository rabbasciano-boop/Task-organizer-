package com.example.taskorganizer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun AccountSwitchScreen(working: Boolean, message: String?, onGoogle: () -> Unit, onGuest: () -> Unit) {
    BackHandler { }
    Box(Modifier.fillMaxSize().organizerBackground()) {
        PageColumn(safeBottom = true, safeTop = true) {
            Text(stringResource(R.string.switch_account_title), color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.switch_account_description), color = SoftText)
            Button(onClick = onGoogle, enabled = !working, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)) {
                Text(stringResource(R.string.switch_account_google), fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onGuest, enabled = !working, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) {
                Text(stringResource(R.string.switch_account_guest), color = MainText)
            }
            if (message != null) Text(message, color = SoftText)
        }
    }
}
