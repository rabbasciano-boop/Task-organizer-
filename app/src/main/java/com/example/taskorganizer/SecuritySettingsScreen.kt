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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
@Composable
fun SecuritySettingsScreen(
    security: AppSecurity,
    biometricAvailable: Boolean,
    onRequestBiometric: ((Boolean, String?) -> Unit) -> Unit,
    onSecurityChanged: () -> Unit,
    onBack: () -> Unit
) {
    var pinDialogOpen by remember { mutableStateOf(false) }
    var disableDialogOpen by remember { mutableStateOf(false) }
    var disableAuthorizationOpen by remember { mutableStateOf(false) }
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
            confirmButton = { TextButton(onClick = { disableDialogOpen = false; disableAuthorizationOpen = true }) { Text(stringResource(R.string.disable), color = Danger) } },
            dismissButton = { TextButton(onClick = { disableDialogOpen = false }) { Text(stringResource(R.string.cancel), color = SoftText) } }
        )
    }
    if (disableAuthorizationOpen) {
        DisableLockAuthorizationDialog(
            security = security,
            biometricAvailable = biometricAvailable && security.isBiometricEnabled(),
            onRequestBiometric = onRequestBiometric,
            onAuthorized = {
                security.clear()
                pinEnabled = false
                biometricEnabled = false
                disableAuthorizationOpen = false
                onSecurityChanged()
            },
            onDismiss = { disableAuthorizationOpen = false }
        )
    }
}

@Composable
private fun DisableLockAuthorizationDialog(
    security: AppSecurity,
    biometricAvailable: Boolean,
    onRequestBiometric: ((Boolean, String?) -> Unit) -> Unit,
    onAuthorized: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }

    fun checkPin() {
        working = true
        scope.launch {
            when (val result = withContext(Dispatchers.Default) { security.verifyPinWithRateLimit(pin) }) {
                PinVerification.Verified -> onAuthorized()
                is PinVerification.Incorrect -> {
                    error = if (result.delayMillis > 0L) {
                        "Troppi tentativi. Attendi ${formatSecurityLockDelay(result.delayMillis)} prima di riprovare."
                    } else {
                        "PIN non corretto. Riprova."
                    }
                }
                is PinVerification.Locked -> {
                    error = "Troppi tentativi. Attendi ${formatSecurityLockDelay((result.untilMillis - System.currentTimeMillis()).coerceAtLeast(0L))} prima di riprovare."
                }
            }
            working = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        containerColor = CardBlue,
        title = { Text("Conferma la tua identità", color = MainText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Per disattivare il blocco app, inserisci il PIN oppure usa l'impronta configurata.", color = SoftText, fontSize = 13.sp)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(12); error = null },
                    label = { Text("PIN") },
                    placeholder = { Text("Almeno 6 cifre") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = editorFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = Danger, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = ::checkPin, enabled = !working && pin.isNotBlank()) {
                Text(if (working) "Verifica…" else "Conferma PIN", color = Gold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (biometricAvailable) {
                    TextButton(onClick = {
                        working = true
                        onRequestBiometric { success, message ->
                            working = false
                            if (success) onAuthorized() else error = message ?: "Impronta non riconosciuta."
                        }
                    }, enabled = !working) { Text("Usa impronta", color = Gold) }
                }
                TextButton(onClick = onDismiss, enabled = !working) { Text(stringResource(R.string.cancel), color = SoftText) }
            }
        }
    )
}

private fun formatSecurityLockDelay(milliseconds: Long): String {
    val seconds = ((milliseconds + 999) / 1_000).coerceAtLeast(1)
    return if (seconds < 60) "$seconds secondi" else "${(seconds + 59) / 60} minuti"
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
    }
}
