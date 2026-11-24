package com.example.taskorganizer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskorganizer.ui.theme.OrganizerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun SettingsHubScreen(signedInAccount: SignedInAccount?, onOpen: (String) -> Unit) {
    PageColumn {
        Text(stringResource(R.string.settings_title), color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.settings_subtitle), color = SoftText)
        Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(38.dp).background(Success.copy(alpha = 0.18f), RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                    Text(signedInAccount?.email?.firstOrNull()?.uppercase() ?: "U", color = Success, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(signedInAccount?.displayName ?: "Utente", color = MainText, fontWeight = FontWeight.Bold)
                    Text(stringResource(if (signedInAccount == null) R.string.settings_guest else R.string.settings_google_connected), color = SoftText, fontSize = 12.sp)
                }
            }
        }
        Text(stringResource(R.string.settings_preferences), color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        SettingsNavigationCard("👤", stringResource(R.string.settings_account_title), stringResource(R.string.settings_account_subtitle)) { onOpen("account") }
        SettingsNavigationCard("🎨", stringResource(R.string.settings_appearance_title), stringResource(R.string.settings_appearance_subtitle)) { onOpen("appearance") }
        SettingsNavigationCard("⌂", stringResource(R.string.settings_home_title), stringResource(R.string.settings_home_subtitle)) { onOpen("home") }
        SettingsNavigationCard("🔒", stringResource(R.string.settings_security_title), stringResource(R.string.settings_security_subtitle)) { onOpen("security") }
        SettingsNavigationCard("ⓘ", stringResource(R.string.settings_information_title), stringResource(R.string.settings_information_subtitle)) { onOpen("information") }
    }
}

@Composable
private fun SettingsNavigationCard(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().sunsetGlow(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
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
    onOpenPrivacy: () -> Unit
) {
    BackHandler(onBack = onBack)
    PageColumn {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text(stringResource(R.string.back_to_settings), color = Gold) }
        Text(stringResource(R.string.settings_account_title), color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.account_subtitle), color = SoftText)
        signedInAccount?.let { account ->
            InfoCard(stringResource(R.string.account_signed_in, account.displayName), account.email, stringResource(R.string.account_google_connected))
        } ?: InfoCard(stringResource(R.string.settings_guest), stringResource(R.string.account_guest_description), stringResource(R.string.account_optional_access))
        Text(stringResource(R.string.account_management), color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        SettingsNavigationCard("👤", stringResource(R.string.account_profile_title), stringResource(R.string.account_profile_subtitle), onOpenProfile)
        SettingsNavigationCard("◈", stringResource(R.string.account_backup_title), stringResource(R.string.account_backup_subtitle), onOpenBackup)
        SettingsNavigationCard("▣", stringResource(R.string.account_privacy_title), stringResource(R.string.account_privacy_subtitle), onOpenPrivacy)
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
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text(stringResource(R.string.back_to_settings), color = Gold) }
        Text(stringResource(R.string.settings_appearance_title), color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.appearance_subtitle), color = SoftText)
        Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(stringResource(R.string.follow_phone_theme), color = MainText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.follow_phone_theme_description), color = SoftText, fontSize = 13.sp)
                }
                Switch(checked = followSystemTheme, onCheckedChange = onFollowSystemThemeChanged)
            }
        }
        Text(stringResource(R.string.free_themes), color = SoftText, fontSize = 13.sp)
        ThemeChoiceCard(OrganizerTheme.ORIGINAL_DARK, stringResource(R.string.theme_original_dark), stringResource(R.string.theme_original_dark_description), selectedTheme, onThemeChanged)
        ThemeChoiceCard(OrganizerTheme.CARTA_DORATA, stringResource(R.string.theme_golden_paper), stringResource(R.string.theme_golden_paper_description), selectedTheme, onThemeChanged)
        Text(stringResource(R.string.additional_themes), color = SoftText, fontSize = 13.sp)
        ThemeChoiceCard(OrganizerTheme.AURORA_MIDNIGHT, stringResource(R.string.theme_aurora_midnight), stringResource(R.string.theme_aurora_midnight_description), selectedTheme, onThemeChanged)
        ThemeChoiceCard(OrganizerTheme.AURORA_NORDIC, stringResource(R.string.theme_aurora_nordic), stringResource(R.string.theme_aurora_nordic_description), selectedTheme, onThemeChanged)
        ThemeChoiceCard(OrganizerTheme.SUNSET_GLOW, stringResource(R.string.theme_sunset_glow), stringResource(R.string.theme_sunset_glow_description), selectedTheme, onThemeChanged)
        ThemeChoiceCard(OrganizerTheme.SUNSET_GLOW_DARK, stringResource(R.string.theme_sunset_glow_dark), stringResource(R.string.theme_sunset_glow_dark_description), selectedTheme, onThemeChanged)
        ThemeChoiceCard(OrganizerTheme.AGENDA_REALE, stringResource(R.string.theme_agenda_reale), stringResource(R.string.theme_agenda_reale_description), selectedTheme, onThemeChanged)
    }
}

@Composable
internal fun HomeSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hideWidgetTitles by remember { mutableStateOf(TaskWidgetUpdater.areTaskTitlesHidden(context)) }
    BackHandler(onBack = onBack)
    PageColumn {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text(stringResource(R.string.back_to_settings), color = Gold) }
        Text(stringResource(R.string.settings_home_title), color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.home_settings_subtitle), color = SoftText)
        InfoCard(stringResource(R.string.widget_home_title), stringResource(R.string.widget_home_description), stringResource(R.string.free))
        Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(stringResource(R.string.hide_widget_titles), color = MainText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.hide_widget_titles_description), color = SoftText, fontSize = 13.sp)
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
        OrganizerTheme.SUNSET_GLOW -> Color(0xFFFFF6EE)
        OrganizerTheme.SUNSET_GLOW_DARK -> Color(0xFF211530)
        OrganizerTheme.AGENDA_REALE -> Color(0xFF230B2E)
    }
    val previewText = if (theme.isDark) Color(0xFFF8FAFC) else Color(0xFF14243B)
    val previewAccent = when (theme) {
        OrganizerTheme.ORIGINAL_DARK -> Color(0xFFD7B46A)
        OrganizerTheme.CARTA_DORATA -> Color(0xFF805C1D)
        OrganizerTheme.AURORA_MIDNIGHT -> Color(0xFF38BDF8)
        OrganizerTheme.AURORA_NORDIC -> Color(0xFF08766D)
        OrganizerTheme.SUNSET_GLOW -> Color(0xFFEA6A47)
        OrganizerTheme.SUNSET_GLOW_DARK -> Color(0xFFFF7B47)
        OrganizerTheme.AGENDA_REALE -> Color(0xFFFF69B4)
    }
    Card(
        modifier = Modifier.fillMaxWidth().sunsetGlow(RoundedCornerShape(20.dp), strong = selectedTheme == theme).clickable { onThemeChanged(theme) },
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

