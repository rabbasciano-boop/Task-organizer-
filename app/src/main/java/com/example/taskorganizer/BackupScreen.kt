package com.example.taskorganizer

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
@Composable
internal fun BackupScreen(
    store: EncryptedTaskStore,
    onBack: () -> Unit,
    pendingImportUri: Uri?,
    onChooseImport: () -> Unit,
    onImportUriHandled: () -> Unit,
    pendingExportUri: Uri?,
    onCreateExport: (String, CharArray) -> Unit,
    takeExportPassword: () -> CharArray?,
    onExportUriHandled: () -> Unit,
    onImported: (TaskArchive, (Result<ArchiveRestoreOutcome>) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportPassword by remember { mutableStateOf("") }
    var exportConfirmation by remember { mutableStateOf("") }
    var importedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var importPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }
    BackHandler(enabled = working) { }

    LaunchedEffect(pendingImportUri) {
        val uri = pendingImportUri ?: return@LaunchedEffect
        working = true
        val result = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use(::readBackupBytes)
                    ?: error("Non è stato possibile leggere il file scelto.")
            }
        }
        working = false
        result.onSuccess {
            importedBytes = it
            importPassword = ""
            message = "File selezionato. Inserisci la password del backup per controllarlo e ripristinarlo."
        }.onFailure {
            importedBytes = null
            message = "Il file non può essere letto: scegli un backup di Task Organizer integro."
        }
        // Clear the hand-off only after the read finishes. Clearing it before this point
        // changes the LaunchedEffect key and cancels the read, leaving the loader visible.
        onImportUriHandled()
    }
    LaunchedEffect(pendingExportUri) {
        val uri = pendingExportUri ?: return@LaunchedEffect
        val password = takeExportPassword()
        if (password == null) {
            val deleted = withContext(Dispatchers.IO) {
                runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }.getOrDefault(false)
            }
            message = if (deleted) {
                "Backup non creato: la richiesta è stata interrotta e il file vuoto è stato rimosso."
            } else {
                "Backup non creato: la richiesta è stata interrotta. Se vedi un file vuoto nella cartella scelta, puoi eliminarlo."
            }
            onExportUriHandled()
            return@LaunchedEffect
        }
        working = true
        val result = try { withContext(Dispatchers.IO) {
            runCatching {
                try {
                    val encrypted = store.exportPasswordProtectedBackup(password)
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(encrypted)
                        output.flush()
                    } ?: error("Non è stato possibile creare il file scelto.")
                    val savedBytes = context.contentResolver.openInputStream(uri)?.use { input ->
                        val buffer = ByteArray(8 * 1024)
                        var total = 0L
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            total += read
                        }
                        total
                    } ?: 0L
                    check(savedBytes > 0L) { "Il file di backup risulta vuoto." }
                } finally {
                    password.fill('\u0000')
                }
            }
        } } finally { password.fill('\u0000') }
        working = false
        result.onSuccess {
            exportPassword = ""
            exportConfirmation = ""
            message = "Backup creato. Conservalo in un luogo sicuro: senza la password non potrà essere recuperato."
        }.onFailure {
            message = "Non è stato possibile creare il backup. Controlla lo spazio disponibile e riprova."
        }
        onExportUriHandled()
    }

    Box(modifier = Modifier.fillMaxSize().organizerBackground()) {
        PageColumn(safeBottom = true, safeTop = true) {
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text("← Torna alle opzioni", color = Gold) }
            Text("Backup cifrato", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.backup_profile_contents), color = SoftText)
            InfoCard("La password è solo tua", "Non viene salvata nell'app e non può essere recuperata. Puoi conservare il file dove preferisci, anche su Drive o OneDrive.", "Protetto")

            Text("CREA UNA COPIA", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = exportPassword,
                onValueChange = { exportPassword = it },
                label = { Text("Password del backup") },
                placeholder = { Text("Almeno 8 caratteri") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = editorFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = if (exportPassword.length < 8) {
                    "Scegli almeno 8 caratteri (${exportPassword.length}/8)."
                } else {
                    "Password valida."
                },
                color = if (exportPassword.length < 8) SoftText else Success,
                fontSize = 12.sp
            )
            OutlinedTextField(
                value = exportConfirmation,
                onValueChange = { exportConfirmation = it },
                label = { Text("Ripeti la password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = editorFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = when {
                    exportConfirmation.isEmpty() -> "Ripeti la password per confermarla."
                    exportConfirmation != exportPassword -> "Le password non coincidono."
                    else -> "Le password coincidono."
                },
                color = when {
                    exportConfirmation.isEmpty() -> SoftText
                    exportConfirmation != exportPassword -> Danger
                    else -> Success
                },
                fontSize = 12.sp
            )
            Text("Per una protezione migliore scegli una frase lunga e unica, non una password breve.", color = SoftText, fontSize = 12.sp)
            Button(
                onClick = {
                    when {
                        exportPassword.length < 8 -> message = "Scegli una password di almeno 8 caratteri."
                        exportPassword != exportConfirmation -> message = "Le due password non coincidono."
                        else -> {
                            onCreateExport("task-organizer-backup-${LocalDate.now()}.tob", exportPassword.toCharArray())
                        }
                    }
                },
                enabled = !working && exportPassword.length >= 8 && exportPassword == exportConfirmation,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)
            ) { Text("Crea backup", fontWeight = FontWeight.Bold) }

            HorizontalDivider(color = CardBlueLight)
            Text("RIPRISTINA UNA COPIA", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Scegli un file .tob. Se hai il PIN attivo, al rientro dall'archivio Android dovrai sbloccare l'app.", color = SoftText, fontSize = 13.sp)
            OutlinedButton(
                onClick = onChooseImport,
                enabled = !working,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Scegli il backup da ripristinare", color = Gold, fontWeight = FontWeight.Bold) }
            if (importedBytes != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBlueLight),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Backup selezionato", color = MainText, fontWeight = FontWeight.Bold)
                        Text("Inserisci la password scelta al momento della creazione. Le attività, liste e categorie attuali verranno sostituite.", color = SoftText, fontSize = 13.sp)
                    }
                }
                OutlinedTextField(
                    value = importPassword,
                    onValueChange = { importPassword = it },
                    label = { Text("Password del backup") },
                    placeholder = { Text("La password scelta durante la creazione") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = editorFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (importPassword.length < 8) {
                        "Inserisci almeno 8 caratteri (${importPassword.length}/8). Non è il PIN dell'app."
                    } else {
                        "Password pronta. Il ripristino sostituirà i dati attuali."
                    },
                    color = if (importPassword.length < 8) SoftText else Success,
                    fontSize = 12.sp
                )
                Button(
                    onClick = {
                        val data = importedBytes
                        if (importPassword.length < 8 || data == null) {
                            message = "Inserisci la password usata per creare il backup."
                        } else {
                            working = true
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    runCatching {
                                        val passwordChars = importPassword.toCharArray()
                                        try {
                                            store.decodePasswordProtectedBackup(data, passwordChars)
                                        } finally {
                                            passwordChars.fill('\u0000')
                                        }
                                    }
                                }
                                result.onSuccess { archive ->
                                    importPassword = ""
                                    importedBytes = null
                                    onImported(archive) { applied ->
                                        working = false
                                        message = if (applied.isSuccess) {
                                            if (applied.getOrThrow().safetyCopyCreated) {
                                                "Backup ripristinato. Una copia cifrata dei dati precedenti è stata conservata in sicurezza sul telefono."
                                            } else {
                                                "Backup ripristinato. Promemoria e widget sono stati aggiornati."
                                            }
                                        } else {
                                            "Il backup è valido, ma non è stato possibile applicarlo. I dati attuali non sono stati modificati."
                                        }
                                    }
                                }.onFailure {
                                    working = false
                                    message = "Password errata oppure backup non valido. I dati attuali non sono stati modificati."
                                }
                            }
                        }
                    },
                    enabled = !working && importPassword.length >= 8,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = MainText)
                ) { Text("Ripristina e sostituisci i dati", fontWeight = FontWeight.Bold) }
            }
            if (working) CircularProgressIndicator(color = Gold, modifier = Modifier.align(Alignment.CenterHorizontally))
            message?.let { Text(it, color = SoftText, fontSize = 13.sp) }
        }
    }
}

private fun readBackupBytes(input: java.io.InputStream): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    var total = 0
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        total += read
        require(total <= MAX_BACKUP_FILE_BYTES) { "Il file è troppo grande." }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

private const val MAX_BACKUP_FILE_BYTES = 10 * 1024 * 1024

