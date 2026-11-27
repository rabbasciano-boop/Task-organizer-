package com.example.taskorganizer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
fun AccountProfileScreen(
    nickname: String,
    onNicknameChanged: (String) -> Unit,
    signedInAccount: SignedInAccount?,
    accountWorking: Boolean,
    accountMessage: String?,
    onGoogleSignIn: () -> Unit,
    onBack: () -> Unit
) {
    var nicknameDialogOpen by remember { mutableStateOf(false) }
    var nicknameDraft by remember(nickname) { mutableStateOf(nickname) }
    BackHandler(onBack = onBack)
    PageColumn {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text("← Torna ad Account", color = Gold) }
        Text("Profilo e accesso", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Gestisci nickname e accesso facoltativo.", color = SoftText)
        Text("STATO ACCESSO", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        if (signedInAccount == null) {
            InfoCard("Stai usando l'app come ospite", "Puoi organizzare tutte le attività senza creare un account. L'accesso non invia né carica automaticamente le tue attività.", "Accesso facoltativo")
            Button(
                onClick = onGoogleSignIn,
                enabled = !accountWorking,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)
            ) {
                if (accountWorking) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = NightBlue, strokeWidth = 2.dp)
                else Text("Accedi con Google", fontWeight = FontWeight.Bold)
            }
        } else {
            InfoCard("Connessa come ${signedInAccount.displayName}", signedInAccount.email, "Google · Account facoltativo")
        }
        accountMessage?.let { InfoCard("Accesso", it, "Puoi riprovare") }
        Text("PROFILO", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().clickable { nicknameDialogOpen = true }) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Il tuo nickname", color = MainText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(if (nickname.isBlank()) "Tocca qui per sceglierlo" else nickname, color = Gold, fontSize = 15.sp)
            }
        }
    }
    if (nicknameDialogOpen) {
        AlertDialog(
            onDismissRequest = { nicknameDialogOpen = false }, containerColor = CardBlue,
            title = { Text("Come vuoi essere chiamata?", color = MainText) },
            text = { OutlinedTextField(value = nicknameDraft, onValueChange = { nicknameDraft = it }, label = { Text("Nickname") }, singleLine = true, colors = editorFieldColors()) },
            confirmButton = { TextButton(onClick = { onNicknameChanged(nicknameDraft.trim()); nicknameDialogOpen = false }) { Text("Salva", color = Gold) } },
            dismissButton = { TextButton(onClick = { nicknameDialogOpen = false }) { Text("Annulla", color = SoftText) } }
        )
    }
}

@Composable
fun PrivacyInfoScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().organizerBackground()) {
        PageColumn(safeBottom = true, safeTop = true) {
            Text("Privacy e dati", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.settings_privacy_preparing), color = SoftText)
            Text("Informazioni sempre disponibili e comprensibili.", color = SoftText)
            InfoCard("Dati sul telefono", "Attività, note, liste e categorie restano in archivi cifrati distinti per ogni account e per l'ospite. Non vengono inviati agli sviluppatori.", "Locale e cifrato")
            InfoCard("Account facoltativo", "L'accesso Google serve solo a riconoscere il tuo profilo nell'app. Non carica automaticamente le attività, non attiva backup e non dà accesso ai tuoi file.", "A tua scelta")
            InfoCard("Backup sotto il tuo controllo", "Puoi creare un file cifrato con password dalle Impostazioni. L'app non lo invia da sola su Drive, OneDrive o su server propri: scegli tu dove conservarlo.", "Manuale e cifrato")
            InfoCard("Niente tracciamento", "Non vendiamo dati, non mostriamo pubblicità e non analizziamo le tue attività.", "Nessuna pubblicità")
            InfoCard("Il tuo controllo", "Quando uscirai dall'account potrai scegliere se mantenere o cancellare i dati locali. L'eliminazione dell'account sarà una procedura separata e protetta.", "Decidi tu")
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
                Text(stringResource(R.string.settings_back), color = Gold, fontWeight = FontWeight.Bold)
            }
        }
    }
}
