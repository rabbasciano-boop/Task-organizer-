package com.example.taskorganizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

private enum class TimeLogTab(val label: String, val icon: Int) {
    DASHBOARD("Dashboard", R.drawable.ic_time_dashboard),
    INSERT("Inserisci", R.drawable.ic_time_add),
    SUMMARY("Riepilogo", R.drawable.ic_time_summary),
    SETTINGS("Tariffa e turni", R.drawable.ic_nav_settings)
}

/** Independent work-hours area. It deliberately keeps its own bottom navigation, like the supplied TimeLog Pro design. */
@Composable
internal fun TimeLogScreen(
    entries: List<WorkLogEntry>,
    settings: WorkLogSettings,
    activeTimerStartedAt: Long?,
    onSaveEntry: (WorkLogEntry) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onSaveSettings: (WorkLogSettings) -> Unit,
    onStartTimer: () -> Unit,
    onStopTimer: (WorkLogEntry) -> Unit,
    onExportCsv: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var tabName by rememberSaveable { mutableStateOf(TimeLogTab.DASHBOARD.name) }
    val tab = TimeLogTab.valueOf(tabName)
    var editor by remember { mutableStateOf<WorkLogEntry?>(null) }
    var resetEditorToken by rememberSaveable { mutableIntStateOf(0) }
    val snackbars = remember { SnackbarHostState() }
    var deletedEntry by remember { mutableStateOf<WorkLogEntry?>(null) }
    LaunchedEffect(deletedEntry) {
        val deleted = deletedEntry ?: return@LaunchedEffect
        if (snackbars.showSnackbar(message = "Voce eliminata", actionLabel = "Annulla", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) onSaveEntry(deleted)
        deletedEntry = null
    }
    fun openNewEntry() { editor = null; resetEditorToken++; tabName = TimeLogTab.INSERT.name }
    BackHandler {
        if (tab != TimeLogTab.DASHBOARD) tabName = TimeLogTab.DASHBOARD.name else onBack()
    }
    Scaffold(
        containerColor = NightBlue,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TimeLogHeader(settings.hourlyRateCents, activeTimerStartedAt, onBack, { tabName = TimeLogTab.INSERT.name }) },
        bottomBar = {
            NavigationBar(containerColor = CardBlue) {
                TimeLogTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tabName = item.name },
                        icon = { Icon(painterResource(item.icon), contentDescription = null) },
                        label = { Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp) },
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Gold, selectedTextColor = Gold, indicatorColor = CardBlueLight,
                            unselectedIconColor = SoftText, unselectedTextColor = SoftText
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == TimeLogTab.DASHBOARD) FloatingActionButton(
                onClick = ::openNewEntry, containerColor = Gold, contentColor = NightBlue,
                shape = CircleShape, modifier = Modifier.semantics { contentDescription = "Aggiungi orario" }
            ) { Text("+", fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        },
        snackbarHost = { SnackbarHost(snackbars, Modifier.navigationBarsPadding().padding(horizontal = 16.dp)) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).organizerBackground()) {
            when (tab) {
                TimeLogTab.DASHBOARD -> TimeLogDashboard(entries, settings.hourlyRateCents, { editor = it; tabName = TimeLogTab.INSERT.name }) { entry ->
                    onDeleteEntry(entry.id)
                    deletedEntry = entry
                }
                TimeLogTab.INSERT -> TimeLogEntryEditor(
                    existing = editor, resetToken = resetEditorToken, settings = settings,
                    activeTimerStartedAt = activeTimerStartedAt, onStartTimer = onStartTimer,
                    onStopTimer = { startedAt ->
                        val start = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(startedAt), java.time.ZoneId.systemDefault())
                        val now = LocalDateTime.now()
                        val recorded = WorkLogEntry(date = start.toLocalDate().toString(), startTime = start.toLocalTime().format(HourFormat), endTime = now.toLocalTime().format(HourFormat))
                        editor = recorded
                        onStopTimer(recorded)
                    },
                    onSave = { entry -> onSaveEntry(entry); editor = null; tabName = TimeLogTab.DASHBOARD.name },
                    onCancel = { editor = null; tabName = TimeLogTab.DASHBOARD.name }
                )
                TimeLogTab.SUMMARY -> TimeLogSummary(entries, settings.hourlyRateCents, onExportCsv)
                TimeLogTab.SETTINGS -> TimeLogSettingsPage(settings, onSaveSettings)
            }
        }
    }
}

@Composable
private fun TimeLogHeader(hourlyRateCents: Long, activeTimerStartedAt: Long?, onBack: () -> Unit, onOpenTimer: () -> Unit) {
    val elapsed = rememberElapsed(activeTimerStartedAt)
    Surface(color = CardBlue.copy(alpha = .97f), shadowElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp).semantics { contentDescription = "Indietro" }) { Icon(painterResource(R.drawable.ic_arrow_left), contentDescription = null, tint = MainText) }
            Box(Modifier.size(38.dp).background(Gold, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_nav_time_log), contentDescription = null, tint = NightBlue) }
            Column(Modifier.weight(1f)) {
                Text("TimeLog Pro", color = MainText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text("Gestione ore e paga", color = SoftText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            if (activeTimerStartedAt != null) TextButton(onClick = onOpenTimer, colors = ButtonDefaults.textButtonColors(contentColor = Danger)) { Text("● ${formatElapsed(elapsed)}", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp) }
            else Text(formatHourlyRate(hourlyRateCents), color = Gold, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.background(CardBlueLight, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 7.dp))
        }
    }
}

@Composable
private fun TimeLogDashboard(entries: List<WorkLogEntry>, rateCents: Long, onEdit: (WorkLogEntry) -> Unit, onDelete: (WorkLogEntry) -> Unit) {
    var period by rememberSaveable { mutableStateOf("week") }
    val filtered = remember(entries, period) { filterWorkEntries(entries, period).sortedWith(compareByDescending<WorkLogEntry> { it.date }.thenByDescending { it.startTime }) }
    val minutes = filtered.sumOf(::workMinutes)
    val days = filtered.map { it.date }.distinct().size
    LazyColumn(
        Modifier.fillMaxSize().widthIn(max = 600.dp), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { TimeLogPeriodTabs(period, listOf("week" to "Settimana corrente", "month" to "Mese corrente")) { period = it } }
        item { WorkOverviewCard(if (period == "week") "Settimana in corso" else "Mese in corso", days, minutes, rateCents) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Registrazioni recenti", color = MainText, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp); Text("Tocca per modificare", color = SoftText, fontSize = 11.sp) } }
        if (filtered.isEmpty()) item { EmptyState("Nessun orario registrato", "Tocca + per aggiungere la tua prima giornata lavorativa.") }
        else items(filtered, key = { it.id }) { entry -> WorkEntryCard(entry, rateCents, { onEdit(entry) }, { onDelete(entry) }) }
        item { Spacer(Modifier.height(70.dp)) }
    }
}

@Composable
private fun WorkOverviewCard(label: String, days: Int, minutes: Int, rateCents: Long) {
    Column(Modifier.fillMaxWidth().background(CardBlue, RoundedCornerShape(28.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("▣  $label", color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Text("$days ${if (days == 1) "giorno" else "giorni"}", color = MainText, fontSize = 11.sp, modifier = Modifier.background(CardBlueLight, RoundedCornerShape(50)).padding(horizontal = 9.dp, vertical = 3.dp)) }
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) { Text("Ore totali", color = SoftText, fontSize = 12.sp); Text(formatWorkMinutes(minutes), color = MainText, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold) }
            Column(Modifier.weight(1f).padding(start = 16.dp).border(1.dp, Gold.copy(alpha = .25f), RoundedCornerShape(1.dp)).padding(start = 16.dp)) { Text("Guadagno stimato", color = SoftText, fontSize = 12.sp); Text(formatCurrency(workEarningsCents(minutes, rateCents)), color = Success, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) }
        }
    }
}

@Composable
private fun WorkEntryCard(entry: WorkLogEntry, rateCents: Long, onEdit: () -> Unit, onDelete: () -> Unit) {
    var deleteArmed by remember(entry.id) { mutableStateOf(false) }
    val date = remember(entry.date) { runCatching { LocalDate.parse(entry.date) }.getOrNull() }
    Card(Modifier.fillMaxWidth().clickable(onClick = onEdit), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = CardBlue)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.width(4.dp).height(92.dp).background(Success, CircleShape))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text(date?.format(DateTimeFormatter.ofPattern("MMM d", Locale.ITALY))?.replaceFirstChar { it.titlecase(Locale.ITALY) } ?: entry.date, color = MainText, fontWeight = FontWeight.ExtraBold); date?.let { safeDate -> Text(safeDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ITALY).replaceFirstChar { it.titlecase(Locale.ITALY) }, color = Success, fontSize = 11.sp, modifier = Modifier.background(Success.copy(alpha = .13f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 3.dp)) }; Text("${entry.startTime} – ${entry.endTime}${if (isOvernightWorkShift(entry.startTime, entry.endTime)) " (+1 giorno)" else ""}", color = SoftText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Text(entry.notes.ifBlank { "Nessuna nota aggiunta" }, color = if (entry.notes.isBlank()) SoftText else MainText, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 9.dp))
                Row(Modifier.fillMaxWidth().padding(top = 9.dp), horizontalArrangement = Arrangement.End) { if (entry.breakMinutes > 0) Text("Pausa: ${entry.breakMinutes} min", color = SoftText, fontSize = 10.sp, modifier = Modifier.weight(1f)); TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("Modifica", color = Gold, fontSize = 11.sp) }; TextButton(onClick = { if (deleteArmed) onDelete() else deleteArmed = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text(if (deleteArmed) "Conferma?" else "Elimina", color = Danger, fontSize = 11.sp) } }
            }
            Column(horizontalAlignment = Alignment.End) { Text(formatWorkMinutes(workMinutes(entry)), color = MainText, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold); Text(formatCurrency(workEarningsCents(workMinutes(entry), rateCents)), color = Success, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun TimeLogEntryEditor(existing: WorkLogEntry?, resetToken: Int, settings: WorkLogSettings, activeTimerStartedAt: Long?, onStartTimer: () -> Unit, onStopTimer: (Long) -> Unit, onSave: (WorkLogEntry) -> Unit, onCancel: () -> Unit) {
    val seed = remember(existing?.id, resetToken) { existing ?: WorkLogEntry(date = LocalDate.now().toString(), startTime = "09:00", endTime = "18:00", breakMinutes = 60) }
    var dateText by rememberSaveable(seed.id) { mutableStateOf(formatItalianDate(seed.date)) }
    var startText by rememberSaveable(seed.id) { mutableStateOf(seed.startTime) }
    var endText by rememberSaveable(seed.id) { mutableStateOf(seed.endTime) }
    var breakText by rememberSaveable(seed.id) { mutableStateOf(seed.breakMinutes.toString()) }
    var notes by rememberSaveable(seed.id) { mutableStateOf(seed.notes) }
    var error by rememberSaveable(seed.id) { mutableStateOf<String?>(null) }
    val elapsed = rememberElapsed(activeTimerStartedAt)
    val parsedDate = parseItalianDate(dateText)
    val start = normalizeTime(startText)
    val end = normalizeTime(endText)
    val minutes = if (start != null && end != null) workMinutes(start, end, breakText.toIntOrNull() ?: 0) else 0
    LazyColumn(Modifier.fillMaxSize().widthIn(max = 600.dp).imePadding(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(CardBlue, RoundedCornerShape(26.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(35.dp).background(Gold.copy(alpha = .15f), CircleShape), contentAlignment = Alignment.Center) { Text("◷", color = Gold, fontSize = 18.sp) }; Column(Modifier.padding(start = 10.dp).weight(1f)) { Text("Timer dal vivo", color = MainText, fontWeight = FontWeight.ExtraBold); Text("Timbra entrata e uscita in tempo reale", color = SoftText, fontSize = 11.sp) }; Text(if (activeTimerStartedAt == null) "In attesa" else "In corso", color = if (activeTimerStartedAt == null) SoftText else Success, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                Text(formatElapsed(elapsed), color = MainText, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().background(NightBlue.copy(alpha = .25f), RoundedCornerShape(18.dp)).padding(13.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Button(onClick = onStartTimer, enabled = activeTimerStartedAt == null, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Success, contentColor = NightBlue)) { Text("▶  Timbra entrata", fontSize = 12.sp) }; Button(onClick = { activeTimerStartedAt?.let(onStopTimer) }, enabled = activeTimerStartedAt != null, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = MainText)) { Text("■  Timbra uscita", fontSize = 12.sp) } }
            }
        }
        item { TimeLogSectionTitle("Turni preimpostati", "Tocca un turno per compilare gli orari") }
        item { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { settings.presets.forEach { preset -> AssistChip(onClick = { startText = preset.startTime; endText = preset.endTime; breakText = preset.breakMinutes.toString() }, label = { Text("${preset.name} (${preset.startTime}-${preset.endTime})", maxLines = 1) }, colors = AssistChipDefaults.assistChipColors(containerColor = CardBlueLight, labelColor = MainText), border = null) } } }
        item {
            Column(Modifier.fillMaxWidth().background(CardBlue, RoundedCornerShape(26.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(if (existing == null) "Registra giornata" else "Modifica giornata", color = MainText, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp); if (existing != null) TextButton(onClick = onCancel) { Text("Annulla", color = Danger) } }
                OutlinedTextField(value = dateText, onValueChange = { dateText = it; error = null }, label = { Text("Data lavorativa (GG-MM-AAAA)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = editorFieldColors())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedTextField(value = startText, onValueChange = { startText = it; error = null }, label = { Text("Ora inizio") }, placeholder = { Text("09:00") }, modifier = Modifier.weight(1f), singleLine = true, colors = compactEditorFieldColors()); OutlinedTextField(value = endText, onValueChange = { endText = it; error = null }, label = { Text("Ora fine") }, placeholder = { Text("18:00") }, modifier = Modifier.weight(1f), singleLine = true, colors = compactEditorFieldColors()) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(value = breakText, onValueChange = { breakText = it.filter(Char::isDigit); error = null }, label = { Text("Pausa (min)") }, modifier = Modifier.weight(1f), singleLine = true, colors = compactEditorFieldColors()); Column(Modifier.weight(1f).background(Gold.copy(alpha = .12f), RoundedCornerShape(16.dp)).padding(10.dp), horizontalAlignment = Alignment.End) { Text("Totale netto", color = SoftText, fontSize = 10.sp); Text("${formatWorkMinutes(minutes)}${if (start != null && end != null && isOvernightWorkShift(start, end)) " · +1 giorno" else ""}", color = MainText, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold); Text(formatCurrency(workEarningsCents(minutes, settings.hourlyRateCents)), color = MainText, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) } }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Note attività svolta") }, placeholder = { Text("Es. riunione, progetto, consegna…") }, modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp), colors = editorFieldColors())
                error?.let { Text(it, color = Danger, fontSize = 12.sp) }
                Button(onClick = {
                    val safeDate = parsedDate
                    if (safeDate == null || start == null || end == null || minutes <= 0) error = "Controlla data e orari: la durata netta deve essere maggiore di zero."
                    else onSave(seed.copy(date = safeDate.toString(), startTime = start, endTime = end, breakMinutes = (breakText.toIntOrNull() ?: 0).coerceIn(0, 480), notes = notes.trim()))
                }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)) { Text(if (existing == null) "✓  Salva registrazione" else "✓  Aggiorna registrazione", fontWeight = FontWeight.ExtraBold) }
            }
        }
    }
}

@Composable
private fun TimeLogSummary(entries: List<WorkLogEntry>, rateCents: Long, onExportCsv: (String, String) -> Unit) {
    var period by rememberSaveable { mutableStateOf("month") }
    val filtered = remember(entries, period) { filterWorkEntries(entries, period).sortedByDescending { it.date } }
    val minutes = filtered.sumOf(::workMinutes)
    val days = filtered.map { it.date }.distinct().size
    LazyColumn(Modifier.fillMaxSize().widthIn(max = 600.dp), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { TimeLogPeriodTabs(period, listOf("week" to "Settimana", "month" to "Mese", "all" to "Tutto")) { period = it } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { TimeLogStat("◷", "Ore lavorate", formatWorkMinutes(minutes), Modifier.weight(1f)); TimeLogStat("€", "Guadagno totale", formatCurrency(workEarningsCents(minutes, rateCents)), Modifier.weight(1f)) } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { TimeLogStat("▣", "Giorni lavorati", "$days", Modifier.weight(1f)); TimeLogStat("↗", "Media ore / giorno", formatWorkMinutes(if (days == 0) 0 else minutes / days), Modifier.weight(1f)) } }
        item { Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CardBlue)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Esporta report", color = MainText, fontWeight = FontWeight.ExtraBold); Text("Scarica il foglio CSV del periodo", color = SoftText, fontSize = 12.sp) }; Button(onClick = { onExportCsv("TimeLog_Report_${period}_${LocalDate.now()}.csv", workCsv(filtered, rateCents)) }, shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue), enabled = filtered.isNotEmpty()) { Text("⇩  CSV") } } } }
        item { Text("Dettaglio tabellare · ${filtered.size} voci", color = MainText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) }
        if (filtered.isEmpty()) item { EmptyState("Nessun dato per questo periodo", "Aggiungi una registrazione per visualizzare le statistiche.") }
        else items(filtered, key = { it.id }) { entry -> Row(Modifier.fillMaxWidth().background(CardBlue, RoundedCornerShape(16.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(formatItalianDate(entry.date), color = MainText, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text("${entry.startTime} – ${entry.endTime}", color = SoftText, fontSize = 11.sp) }; Text(formatWorkMinutes(workMinutes(entry)), color = MainText, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(formatCurrency(workEarningsCents(workMinutes(entry), rateCents)), color = Success, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(82.dp), textAlign = TextAlign.End) } }
    }
}

@Composable
private fun TimeLogSettingsPage(settings: WorkLogSettings, onSaveSettings: (WorkLogSettings) -> Unit) {
    var rate by rememberSaveable(settings.hourlyRateCents) { mutableStateOf(formatCurrency(settings.hourlyRateCents).removePrefix("€").trim()) }
    var rateError by rememberSaveable { mutableStateOf<String?>(null) }
    var newName by rememberSaveable { mutableStateOf("") }
    var newStart by rememberSaveable { mutableStateOf("09:00") }
    var newEnd by rememberSaveable { mutableStateOf("18:00") }
    var newBreak by rememberSaveable { mutableStateOf("0") }
    LazyColumn(Modifier.fillMaxSize().widthIn(max = 600.dp).imePadding(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { TimeLogSectionTitle("Tariffa oraria", "Usata per calcolare automaticamente i guadagni") }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CardBlue)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = rate, onValueChange = { rate = it; rateError = null }, label = { Text("Tariffa (€ / ora)") }, modifier = Modifier.weight(1f), singleLine = true, isError = rateError != null, colors = editorFieldColors())
                        Button(onClick = {
                            val cents = parseAmountCents(rate)
                            if (cents == null) rateError = "Inserisci una tariffa valida, per esempio 15,00."
                            else onSaveSettings(settings.copy(hourlyRateCents = cents))
                        }, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)) { Text("Salva") }
                    }
                    rateError?.let { Text(it, color = Danger, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
                }
            }
        }
        item { TimeLogSectionTitle("Turni preimpostati", "Crea fasce rapide per l’inserimento con un tocco") }
        items(settings.presets, key = { it.id }) { preset ->
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = CardBlue)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(preset.name, color = MainText, fontWeight = FontWeight.Bold); Text("${preset.startTime} – ${preset.endTime}${if (preset.breakMinutes > 0) " · pausa ${preset.breakMinutes} min" else ""}", color = SoftText, fontSize = 12.sp) }
                    Text("Elimina", color = Danger, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onSaveSettings(settings.copy(presets = settings.presets.filterNot { it.id == preset.id })) }.padding(8.dp))
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CardBlueLight.copy(alpha = .85f))) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Aggiungi nuovo turno rapido", color = MainText, fontWeight = FontWeight.ExtraBold)
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Nome turno") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = compactEditorFieldColors())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(value = newStart, onValueChange = { newStart = it }, label = { Text("Inizio") }, modifier = Modifier.weight(1f), singleLine = true, colors = compactEditorFieldColors()); OutlinedTextField(value = newEnd, onValueChange = { newEnd = it }, label = { Text("Fine") }, modifier = Modifier.weight(1f), singleLine = true, colors = compactEditorFieldColors()) }
                    OutlinedTextField(value = newBreak, onValueChange = { newBreak = it.filter(Char::isDigit) }, label = { Text("Pausa (minuti)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = compactEditorFieldColors())
                    Button(onClick = { val start = normalizeTime(newStart); val end = normalizeTime(newEnd); if (newName.isNotBlank() && start != null && end != null) { onSaveSettings(settings.copy(presets = settings.presets + WorkShiftPreset(name = newName.trim(), startTime = start, endTime = end, breakMinutes = (newBreak.toIntOrNull() ?: 0).coerceIn(0, 480)))); newName = "" } }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)) { Text("+ Aggiungi turno") }
                }
            }
        }
        item { Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CardBlue)) { Column(Modifier.fillMaxWidth().padding(16.dp)) { Text("Dati protetti", color = MainText, fontWeight = FontWeight.ExtraBold); Text("Le registrazioni del Registro ore sono già incluse nel backup cifrato di Task Organizer.", color = SoftText, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp)) } } }
    }
}

@Composable private fun TimeLogPeriodTabs(selected: String, choices: List<Pair<String, String>>, onSelected: (String) -> Unit) = Row(Modifier.fillMaxWidth().background(CardBlue, RoundedCornerShape(18.dp)).padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) { choices.forEach { (key, label) -> Text(label, color = if (selected == key) NightBlue else SoftText, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).background(if (selected == key) Gold else CardBlue, RoundedCornerShape(14.dp)).clickable { onSelected(key) }.padding(vertical = 10.dp)) } }
@Composable private fun TimeLogStat(symbol: String, label: String, value: String, modifier: Modifier) = Card(modifier, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = CardBlue)) { Column(Modifier.padding(15.dp)) { Text(symbol, color = Gold, fontSize = 18.sp); Text(label, color = SoftText, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)); Text(value, color = MainText, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
@Composable private fun TimeLogSectionTitle(title: String, subtitle: String) = Column { Text(title, color = MainText, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp); Text(subtitle, color = SoftText, fontSize = 11.sp) }

private val HourFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
internal fun workMinutes(entry: WorkLogEntry): Int = workMinutes(entry.startTime, entry.endTime, entry.breakMinutes)
internal fun isOvernightWorkShift(start: String, end: String): Boolean = runCatching { LocalTime.parse(end, HourFormat).isBefore(LocalTime.parse(start, HourFormat)) }.getOrDefault(false)
internal fun workMinutes(start: String, end: String, breakMinutes: Int): Int = runCatching { var result = java.time.Duration.between(LocalTime.parse(start, HourFormat), LocalTime.parse(end, HourFormat)).toMinutes(); if (result < 0) result += 24 * 60; (result - breakMinutes.coerceAtLeast(0)).coerceAtLeast(0).toInt() }.getOrDefault(0)
internal fun workEarningsCents(minutes: Int, hourlyRateCents: Long): Long = (minutes.coerceAtLeast(0).toLong() * hourlyRateCents.coerceAtLeast(0) + 30L) / 60L
private fun formatWorkMinutes(minutes: Int) = "${minutes / 60}h ${(minutes % 60).toString().padStart(2, '0')}m"
private fun formatHourlyRate(cents: Long) = "${formatCurrency(cents)}/h"
private fun formatElapsed(milliseconds: Long) = "%02d:%02d:%02d".format(milliseconds / 3_600_000, (milliseconds / 60_000) % 60, (milliseconds / 1_000) % 60)
@Composable private fun rememberElapsed(startedAt: Long?): Long { var now by remember(startedAt) { mutableLongStateOf(System.currentTimeMillis()) }; LaunchedEffect(startedAt) { while (startedAt != null) { now = System.currentTimeMillis(); delay(1_000) } }; return (startedAt?.let { (now - it).coerceAtLeast(0) } ?: 0L) }
internal fun filterWorkEntries(entries: List<WorkLogEntry>, period: String, today: LocalDate = LocalDate.now()): List<WorkLogEntry> = when (period) { "week" -> { val monday = today.minusDays((today.dayOfWeek.value - 1).toLong()); entries.filter { runCatching { val date = LocalDate.parse(it.date); !date.isBefore(monday) && !date.isAfter(monday.plusDays(6)) }.getOrDefault(false) } }; "month" -> entries.filter { runCatching { val date = LocalDate.parse(it.date); date.year == today.year && date.month == today.month }.getOrDefault(false) }; else -> entries.filter { validWorkLogEntry(it.date, it.startTime, it.endTime) } }
private fun csvText(value: String): String { val safe = if (value.trimStart().firstOrNull() in setOf('=', '+', '-', '@')) "'$value" else value; return "\"${safe.replace("\"", "\"\"")}\"" }
internal fun workCsv(entries: List<WorkLogEntry>, rateCents: Long): String = buildString { append("Data;Ora inizio;Ora fine;Pausa (min);Ore nette;Tariffa oraria (€);Guadagno (€);Note\n"); entries.filter { validWorkLogEntry(it.date, it.startTime, it.endTime) }.sortedBy { it.date }.forEach { entry -> val minutes = workMinutes(entry); append("${entry.date};${entry.startTime};${entry.endTime};${entry.breakMinutes};${(minutes / 60.0).toString().replace('.', ',')};${(rateCents / 100.0).toString().replace('.', ',')};${(workEarningsCents(minutes, rateCents) / 100.0).toString().replace('.', ',')};${csvText(entry.notes)}\n") } }
