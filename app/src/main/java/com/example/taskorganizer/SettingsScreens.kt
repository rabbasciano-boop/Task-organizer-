package com.example.taskorganizer

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskorganizer.ui.theme.TaskOrganizerTheme
import com.example.taskorganizer.ui.theme.OrganizerPalette
import com.example.taskorganizer.ui.theme.OrganizerTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.text.NumberFormat
import java.net.URL
import java.io.ByteArrayOutputStream
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Composable
internal fun SettingsHubScreen(signedInAccount: SignedInAccount?, onOpen: (String) -> Unit) {
    PageColumn {
        Text("Impostazioni", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Personalizza Task Organizer in un unico posto.", color = SoftText)
        Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(38.dp).background(Success.copy(alpha = 0.18f), RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                    Text(signedInAccount?.email?.firstOrNull()?.uppercase() ?: "U", color = Success, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(signedInAccount?.displayName ?: "Utente", color = MainText, fontWeight = FontWeight.Bold)
                    Text(if (signedInAccount == null) "Stai usando l'app come ospite" else "Account Google collegato", color = SoftText, fontSize = 12.sp)
                }
            }
        }
        Text("PREFERENZE", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        SettingsNavigationCard("👤", "Account", "Accesso, backup, privacy e dati personali") { onOpen("account") }
        SettingsNavigationCard("🎨", "Aspetto", "Temi, colori e modalità chiara o scura") { onOpen("appearance") }
        SettingsNavigationCard("⌂", "Schermata Home", "Widget e contenuti mostrati in apertura") { onOpen("home") }
        SettingsNavigationCard("✓", "Attività e promemoria", "Categorie, notifiche e sveglie") { onOpen("activities") }
        SettingsNavigationCard("🔒", "Sicurezza app", "PIN e sblocco biometrico") { onOpen("security") }
        SettingsNavigationCard("ⓘ", "Informazioni", "Compatibilità e dettagli dell'app") { onOpen("information") }
    }
}

@Composable
private fun SettingsNavigationCard(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBlue),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(38.dp).background(CardBlueLight, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Text(icon, fontSize = 18.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = MainText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = SoftText, fontSize = 12.sp)
            }
            Text("›", color = Gold, fontSize = 25.sp)
        }
    }
}

@Composable
internal fun AccountHubScreen(
    signedInAccount: SignedInAccount?,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenCalendar: () -> Unit
) {
    BackHandler(onBack = onBack)
    PageColumn {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text("← Torna alle impostazioni", color = Gold) }
        Text("Account", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Tutto ciò che riguarda il tuo profilo e i tuoi dati.", color = SoftText)
        signedInAccount?.let { account ->
            InfoCard("Connessa come ${account.displayName}", account.email, "Google collegato")
        } ?: InfoCard("Stai usando l'app come ospite", "Puoi creare e organizzare attività senza un account.", "Accesso facoltativo")
        Text("GESTIONE ACCOUNT", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        SettingsNavigationCard("👤", "Profilo e accesso", "Nickname, accesso Google ed uscita dall'account", onOpenProfile)
        SettingsNavigationCard("◈", "Backup e sincronizzazione", "Crea o ripristina una copia cifrata", onOpenBackup)
        SettingsNavigationCard("▣", "Privacy e dati", "Consensi, dati locali e cancellazione account", onOpenPrivacy)
        SettingsNavigationCard("▦", "Calendario personale", "Google Calendar e Outlook", onOpenCalendar)
    }
}

@Composable
internal fun AccountCalendarSettingsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    PageColumn {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text("← Torna ad Account", color = Gold) }
        Text("Calendario personale", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Qui potrai scegliere se collegare il tuo calendario Google o Outlook.", color = SoftText)
        InfoCard("Collegamento opzionale", "Gli impegni personali resteranno separati dalle attività di Task Organizer. Il collegamento verrà richiesto solo quando deciderai di attivarlo.", "Prossimamente")
    }
}

@Composable
internal fun AppearanceSettingsScreen(
    selectedTheme: OrganizerTheme,
    followSystemTheme: Boolean,
    onThemeChanged: (OrganizerTheme) -> Unit,
    onFollowSystemThemeChanged: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    PageColumn {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text("← Torna alle impostazioni", color = Gold) }
        Text("Aspetto", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Tema e colori dell'app.", color = SoftText)
        Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Segui il tema del telefono", color = MainText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Tema scuro quando il telefono è scuro e Carta Dorata quando è chiaro.", color = SoftText, fontSize = 13.sp)
                }
                Switch(checked = followSystemTheme, onCheckedChange = onFollowSystemThemeChanged)
            }
        }
        Text("Temi gratuiti", color = SoftText, fontSize = 13.sp)
        ThemeChoiceCard(OrganizerTheme.ORIGINAL_DARK, "🌙 Originale Dark", "Blu notte classico con accenti oro", selectedTheme, onThemeChanged)
        ThemeChoiceCard(OrganizerTheme.CARTA_DORATA, "📜 Carta Dorata", "Chiaro caldo, morbido e leggibile", selectedTheme, onThemeChanged)
        Text("Temi aggiuntivi", color = SoftText, fontSize = 13.sp)
        ThemeChoiceCard(OrganizerTheme.AURORA_MIDNIGHT, "✨ Aurora Midnight", "Vetro scuro con bagliori ciano e viola", selectedTheme, onThemeChanged)
        ThemeChoiceCard(OrganizerTheme.AURORA_NORDIC, "❄️ Aurora Nordic", "Vetro chiaro iridescente, menta e cielo", selectedTheme, onThemeChanged)
    }
}

@Composable
internal fun HomeSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hideWidgetTitles by remember { mutableStateOf(TaskWidgetUpdater.areTaskTitlesHidden(context)) }
    BackHandler(onBack = onBack)
    PageColumn {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text("← Torna alle impostazioni", color = Gold) }
        Text("Schermata Home", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Widget e contenuti mostrati all'apertura.", color = SoftText)
        InfoCard("Widget per la schermata Home", "Tieni premuto uno spazio libero nella schermata Home, tocca Widget e cerca Task Organizer. Puoi scegliere Progresso, Riepilogo o Agenda.", "Gratuito")
        Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Nascondi titoli nei widget", color = MainText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Mostra “Attività pianificata” invece del titolo. Categoria e orario restano visibili.", color = SoftText, fontSize = 13.sp)
                }
                Switch(checked = hideWidgetTitles, onCheckedChange = { hidden ->
                    hideWidgetTitles = hidden
                    TaskWidgetUpdater.setTaskTitlesHidden(context, hidden)
                    scope.launch(Dispatchers.IO) { TaskWidgetUpdater.updateAll(context) }
                })
            }
        }
    }
}

@Composable
internal fun ActivitySettingsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    PageColumn {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text("← Torna alle impostazioni", color = Gold) }
        Text("Attività e promemoria", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Le preferenze pratiche per organizzare ogni impegno.", color = SoftText)
        InfoCard("Categorie e icone", "Puoi aggiungere e personalizzare le categorie direttamente quando crei o modifichi un'attività.", "Disponibile")
        InfoCard("Colori delle attività", "Quando crei un'attività puoi scegliere il colore della sua card per riconoscerla più facilmente.", "Disponibile")
        InfoCard("Promemoria e sveglie", "Li imposti in ogni attività: scegli anticipo, ripetizione, notifica o sveglia.", "Disponibile")
    }
}


@Composable
private fun ThemeChoiceCard(
    theme: OrganizerTheme,
    title: String,
    subtitle: String,
    selectedTheme: OrganizerTheme,
    onThemeChanged: (OrganizerTheme) -> Unit
) {
    val previewBackground = when (theme) {
        OrganizerTheme.ORIGINAL_DARK -> Color(0xFF152036)
        OrganizerTheme.CARTA_DORATA -> Color(0xFFFFFDF8)
        OrganizerTheme.AURORA_MIDNIGHT -> Color(0xFF171E3A)
        OrganizerTheme.AURORA_NORDIC -> Color(0xFFF0F9F8)
    }
    val previewText = if (theme.isDark) Color(0xFFF8FAFC) else Color(0xFF14243B)
    val previewAccent = when (theme) {
        OrganizerTheme.ORIGINAL_DARK -> Color(0xFFD7B46A)
        OrganizerTheme.CARTA_DORATA -> Color(0xFF805C1D)
        OrganizerTheme.AURORA_MIDNIGHT -> Color(0xFF38BDF8)
        OrganizerTheme.AURORA_NORDIC -> Color(0xFF08766D)
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onThemeChanged(theme) },
        colors = CardDefaults.cardColors(containerColor = previewBackground),
        shape = RoundedCornerShape(20.dp),
        border = if (selectedTheme == theme) BorderStroke(2.dp, previewAccent) else null
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = previewText, fontWeight = FontWeight.Bold)
                Text(subtitle, color = previewText.copy(alpha = 0.72f), fontSize = 13.sp)
            }
            Switch(
                checked = selectedTheme == theme,
                onCheckedChange = { onThemeChanged(theme) }
            )
        }
    }
}

@Composable
internal fun PinSetupDialog(
    security: AppSecurity,
    changing: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBlue,
        title = { Text(if (changing) "Cambia il PIN" else "Crea il tuo PIN", color = MainText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Usa almeno 6 cifre. Il PIN non verrà mai salvato in chiaro.", color = SoftText, fontSize = 13.sp)
                if (changing) {
                    PinField("PIN attuale", currentPin) { currentPin = it }
                }
                PinField("Nuovo PIN", newPin) { newPin = it }
                PinField("Conferma nuovo PIN", confirmPin) { confirmPin = it }
                if (error.isNotBlank()) Text(error, color = Danger, fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!newPin.matches(Regex("\\d{6,}"))) {
                    error = "Il PIN deve contenere almeno 6 cifre."
                } else if (newPin != confirmPin) {
                    error = "I due PIN non coincidono."
                } else {
                    working = true
                    scope.launch {
                        val verification = if (changing) withContext(Dispatchers.Default) { security.verifyPinWithRateLimit(currentPin) } else PinVerification.Verified
                        when (verification) {
                            PinVerification.Verified -> {
                                withContext(Dispatchers.Default) { security.setPin(newPin) }
                                working = false
                                onSave()
                            }
                            is PinVerification.Locked -> {
                                working = false
                                error = "Troppi tentativi. Attendi ${formatLockDelay((verification.untilMillis - System.currentTimeMillis()).coerceAtLeast(0L))} prima di riprovare."
                            }
                            is PinVerification.Incorrect -> {
                                working = false
                                error = if (verification.delayMillis > 0) "Troppi tentativi. Attendi ${formatLockDelay(verification.delayMillis)} prima di riprovare." else "Il PIN attuale non è corretto."
                            }
                        }
                    }
                }
            }, enabled = !working) { Text(if (working) "Verifica…" else "Salva", color = Gold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = SoftText) } }
    )
}

@Composable
private fun PinField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { entered -> onChange(entered.filter(Char::isDigit).take(12)) },
        label = { Text(label) },
        placeholder = { Text(if (label == "Nuovo PIN") "Almeno 6 cifre" else "••••••") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        colors = editorFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
internal fun AppLockScreen(
    security: AppSecurity,
    biometricAvailable: Boolean,
    onUnlocked: () -> Unit,
    onRequestBiometric: ((Boolean, String?) -> Unit) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxSize().organizerBackground().clickable(onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBlue),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("✦", color = Gold, fontSize = 34.sp)
                Text("Task Organizer", color = MainText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Inserisci il PIN per accedere alle tue attività.", color = SoftText, textAlign = TextAlign.Center)
                PinField("PIN", pin) { pin = it }
                if (error.isNotBlank()) Text(error, color = Danger, fontSize = 13.sp, textAlign = TextAlign.Center)
                Button(
                    onClick = {
                        working = true
                        scope.launch {
                        when (val result = withContext(Dispatchers.Default) { security.verifyPinWithRateLimit(pin) }) {
                            PinVerification.Verified -> {
                                pin = ""
                                error = ""
                                onUnlocked()
                            }
                            is PinVerification.Incorrect -> {
                                error = if (result.delayMillis > 0) {
                                    "Troppi tentativi. Attendi ${formatLockDelay(result.delayMillis)} prima di riprovare."
                                } else {
                                    "PIN non corretto. Riprova."
                                }
                            }
                            is PinVerification.Locked -> {
                                error = "Troppi tentativi. Attendi ${formatLockDelay((result.untilMillis - System.currentTimeMillis()).coerceAtLeast(0L))} prima di riprovare."
                            }
                        }
                        working = false
                        }
                    },
                    enabled = !working,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)
                ) { Text(if (working) "Verifica…" else "Sblocca", fontWeight = FontWeight.Bold) }
                if (biometricAvailable && security.isBiometricEnabled()) {
                    OutlinedButton(onClick = {
                        onRequestBiometric { success, message ->
                            if (success) onUnlocked() else error = message.orEmpty()
                        }
                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Text("Usa impronta", color = Gold, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun formatLockDelay(milliseconds: Long): String {
    val seconds = ((milliseconds + 999) / 1000).coerceAtLeast(1)
    return if (seconds < 60) "$seconds secondi" else "${(seconds + 59) / 60} minuti"
}

