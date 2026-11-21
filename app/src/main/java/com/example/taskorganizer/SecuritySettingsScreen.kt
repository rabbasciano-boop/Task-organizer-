package com.example.taskorganizer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
fun SecuritySettingsScreen(
    security: AppSecurity,
    biometricAvailable: Boolean,
    onSecurityChanged: () -> Unit,
    onBack: () -> Unit
) {
    var pinDialogOpen by remember { mutableStateOf(false) }
    var disableDialogOpen by remember { mutableStateOf(false) }
    var pinEnabled by remember { mutableStateOf(security.hasPin()) }
    var biometricEnabled by remember { mutableStateOf(security.isBiometricEnabled()) }
    BackHandler(onBack = onBack)
    PageColumn {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text(stringResource(R.string.back_to_settings), color = Gold) }
        Text(stringResource(R.string.settings_security_title), color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.security_subtitle), color = SoftText)
        Card(modifier = Modifier.fillMaxWidth().clickable { pinDialogOpen = true }, colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.app_lock), color = MainText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(if (pinEnabled) R.string.app_lock_enabled_description else R.string.app_lock_disabled_description), color = SoftText, fontSize = 14.sp)
                }
                Switch(checked = pinEnabled, onCheckedChange = { enabled -> if (enabled) pinDialogOpen = true else disableDialogOpen = true })
            }
        }
        if (pinEnabled && biometricAvailable) {
            Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.biometric_unlock), color = MainText, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.biometric_unlock_description), color = SoftText, fontSize = 13.sp)
                    }
                    Switch(checked = biometricEnabled, onCheckedChange = { biometricEnabled = it; security.setBiometricEnabled(it) })
                }
            }
        }
    }
    if (pinDialogOpen) {
        PinSetupDialog(security = security, changing = pinEnabled, onDismiss = { pinDialogOpen = false }, onSave = {
            pinEnabled = true; pinDialogOpen = false; onSecurityChanged()
        })
    }
    if (disableDialogOpen) {
        AlertDialog(
            onDismissRequest = { disableDialogOpen = false }, containerColor = CardBlue,
            title = { Text(stringResource(R.string.disable_lock_title), color = MainText) },
            text = { Text(stringResource(R.string.disable_lock_description), color = SoftText) },
            confirmButton = { TextButton(onClick = { security.clear(); pinEnabled = false; biometricEnabled = false; disableDialogOpen = false; onSecurityChanged() }) { Text(stringResource(R.string.disable), color = Danger) } },
            dismissButton = { TextButton(onClick = { disableDialogOpen = false }) { Text(stringResource(R.string.cancel), color = SoftText) } }
        )
    }
}

@Composable
fun InformationSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Versione di test"
    }
    BackHandler(onBack = onBack)
    PageColumn {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text(stringResource(R.string.back_to_settings), color = Gold) }
        Text(stringResource(R.string.settings_information_title), color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.information_subtitle), color = SoftText)
        InfoCard(stringResource(R.string.information_version), stringResource(R.string.information_version_value, versionName), stringResource(R.string.information_installed))
        InfoCard(stringResource(R.string.information_compatibility), stringResource(R.string.information_compatibility_description), stringResource(R.string.information_android_version))
        InfoCard(stringResource(R.string.information_support), stringResource(R.string.information_support_description), stringResource(R.string.information_preparing))
    }
}
