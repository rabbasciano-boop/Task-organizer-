package com.example.taskorganizer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text("← Torna alle impostazioni", color = Gold) }
        Text("Sicurezza app", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Proteggi l'accesso alle tue attività.", color = SoftText)
        Card(modifier = Modifier.fillMaxWidth().clickable { pinDialogOpen = true }, colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Blocco app", color = MainText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(if (pinEnabled) "Il PIN protegge l'accesso quando riapri l'app. Tocca la scheda per cambiarlo." else "Attiva un PIN personale per proteggere le tue attività.", color = SoftText, fontSize = 14.sp)
                }
                Switch(checked = pinEnabled, onCheckedChange = { enabled -> if (enabled) pinDialogOpen = true else disableDialogOpen = true })
            }
        }
        if (pinEnabled && biometricAvailable) {
            Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sblocco biometrico", color = MainText, fontWeight = FontWeight.Bold)
                        Text("Usa impronta o riconoscimento configurato sul telefono.", color = SoftText, fontSize = 13.sp)
                    }
                    Switch(checked = biometricEnabled, onCheckedChange = { biometricEnabled = it; security.setBiometricEnabled(it) })
                }
            }
        }
    }
    if (pinDialogOpen) {
        PinSetupDialog(security = security, changing = pinEnabled, onDismiss = { pinDialogOpen = false }, onSave = {
            security.setPin(it); pinEnabled = true; pinDialogOpen = false; onSecurityChanged()
        })
    }
    if (disableDialogOpen) {
        AlertDialog(
            onDismissRequest = { disableDialogOpen = false }, containerColor = CardBlue,
            title = { Text("Disattivare il blocco?", color = MainText) },
            text = { Text("L'app tornerà ad aprirsi senza PIN.", color = SoftText) },
            confirmButton = { TextButton(onClick = { security.clear(); pinEnabled = false; biometricEnabled = false; disableDialogOpen = false; onSecurityChanged() }) { Text("Disattiva", color = Danger) } },
            dismissButton = { TextButton(onClick = { disableDialogOpen = false }) { Text("Annulla", color = SoftText) } }
        )
    }
}

@Composable
fun InformationSettingsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    PageColumn {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text("← Torna alle impostazioni", color = Gold) }
        Text("Informazioni", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Dettagli utili su Task Organizer.", color = SoftText)
        InfoCard("Compatibilità", "L'app funziona da Android 7.0 in poi. L'impronta richiede Android 9 o successivo e un telefono compatibile.", "Android 7.0+")
    }
}
