package com.example.taskorganizer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OpeningScreen() {
    val progress by animateFloatAsState(
        targetValue = 0.92f,
        animationSpec = tween(durationMillis = 1_100),
        label = "openingProgress"
    )
    Box(
        modifier = Modifier.fillMaxSize().organizerBackground(),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.fillMaxSize().background(openingHaloBrush()))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.splash_mark_transparent),
                contentDescription = "Task Organizer",
                modifier = Modifier.size(210.dp),
                contentScale = ContentScale.Fit
            )
            Text("Task Organizer", color = MainText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Sto preparando tutto", color = SoftText, fontSize = 13.sp)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(0.46f).height(5.dp),
                color = Gold,
                trackColor = CardBlueLight
            )
        }
    }
}

@Composable
fun PrivacyConsentScreen(onContinueAsGuest: () -> Unit) {
    var confirmed by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize().organizerBackground()) {
        PageColumn(safeBottom = true, safeTop = true) {
            Text("Prima di iniziare", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("La tua privacy viene prima dell'organizzazione.", color = SoftText)
            InfoCard(
                "I tuoi dati restano qui",
                "Attività, note, liste, categorie, preferenze e PIN restano nel telefono in un archivio cifrato. Non vengono inviati agli sviluppatori.",
                "Solo sul telefono"
            )
            InfoCard(
                "Account facoltativo",
                "Potrai accedere con Google o Microsoft soltanto se vorrai usare backup, calendario o servizi collegati. L'accesso non caricherà automaticamente le tue attività.",
                "Potrai decidere in seguito"
            )
            InfoCard(
                "Niente pubblicità o tracciamento",
                "Non vendiamo dati, non mostriamo pubblicità e non analizziamo le tue attività. Le notifiche verranno chieste solo quando deciderai di attivarle.",
                "Trasparenza"
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = confirmed, onCheckedChange = { confirmed = it })
                Text("Ho letto come vengono trattati i miei dati.", color = MainText, fontSize = 14.sp, modifier = Modifier.padding(start = 6.dp))
            }
            Button(
                onClick = onContinueAsGuest,
                enabled = confirmed,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)
            ) { Text("Continua come ospite", fontWeight = FontWeight.Bold) }
            Text("Potrai aggiungere un account in qualsiasi momento dalla scheda Account.", color = SoftText, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun ArchiveRecoveryScreen(onRetry: () -> Unit, onStartFresh: (onFinished: (Boolean) -> Unit) -> Unit) {
    var showConfirmation by remember { mutableStateOf(false) }
    var recoveryError by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize().organizerBackground()) {
        PageColumn(safeBottom = true, safeTop = true) {
            Text("Archivio da recuperare", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Task Organizer non riesce a leggere l'archivio locale. Per proteggere le tue attività non abbiamo modificato né cancellato nulla.", color = SoftText)
            InfoCard(
                "I dati restano al sicuro",
                "Potrebbe trattarsi di un file danneggiato o di una chiave di sicurezza non più disponibile sul telefono. Puoi riprovare oppure iniziare con un archivio nuovo.",
                "Nessuna cancellazione automatica"
            )
            Button(
                onClick = { recoveryError = false; onRetry() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)
            ) { Text("Riprova", fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = { showConfirmation = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Inizia con un nuovo archivio", color = MainText) }
            if (recoveryError) {
                Text("Non è stato possibile preparare una copia di sicurezza. I tuoi dati non sono stati toccati.", color = Danger, fontSize = 13.sp)
            }
        }
    }
    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("Iniziare da zero?", color = MainText) },
            text = { Text("Prima conserveremo una copia dell'archivio non leggibile sul telefono. Solo dopo verrà creato un nuovo archivio vuoto.", color = SoftText) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmation = false
                    onStartFresh { succeeded -> recoveryError = !succeeded }
                }) { Text("Conserva copia e continua", color = Gold) }
            },
            dismissButton = { TextButton(onClick = { showConfirmation = false }) { Text("Annulla", color = SoftText) } }
        )
    }
}
