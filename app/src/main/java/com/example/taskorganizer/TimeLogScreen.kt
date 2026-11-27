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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

private enum class TimeLogTab(val label: String, val icon: Int) {
    DASHBOARD("Dashboard", R.drawable.ic_time_dashboard),
    INSERT("Inserisci", R.drawable.ic_time_add),
    SUMMARY("Riepilogo", R.drawable.ic_time_summary),
    SETTINGS("Tariffa e turni", R.drawable.ic_time_rate)
}

/** Independent work-hours area. It deliberately keeps its own bottom navigation, like the supplied TimeLog Pro design. */
@Composable
internal fun TimeLogScreen(
    entries: List<WorkLogEntry>,
    settings: WorkLogSettings,
    activeTimerStartedAt: Long?,
    onSaveEntry: (WorkLogEntry, (Boolean) -> Unit) -> Unit,
    onDeleteEntry: (String, (Boolean) -> Unit) -> Unit,
    onSaveSettings: (WorkLogSettings) -> Unit,
    onStartTimer: () -> Unit,
    onStopTimer: (WorkLogEntry, (Boolean) -> Unit) -> Unit,
    onExportCsv: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var tabName by rememberSaveable { mutableStateOf(TimeLogTab.DASHBOARD.name) }
    val tab = TimeLogTab.valueOf(tabName)
    var editor by remember { mutableStateOf<WorkLogEntry?>(null) }
    var resetEditorToken by rememberSaveable { mutableIntStateOf(0) }
    var timerSaving by remember { mutableStateOf(false) }
    var timerSaved by remember { mutableStateOf(false) }
    var longTimerStartedAt by remember { mutableStateOf<Long?>(null) }
    var longTimerEnd by remember { mutableStateOf("") }
    var longTimerError by remember { mutableStateOf<String?>(null) }
    val pendingDeletions = remember { mutableStateListOf<WorkLogEntry>() }
    val pendingRestores = remember { mutableStateListOf<String>() }
    pendingDeletions.forEach { deleted ->
        key(deleted.id) {
            val restoring = deleted.id in pendingRestores
            LaunchedEffect(deleted.id, restoring) {
                if (restoring) return@LaunchedEffect
                delay(6_000)
                pendingDeletions.removeAll { it.id == deleted.id }
            }
        }
    }
    fun openNewEntry() { editor = null; timerSaved = false; resetEditorToken++; tabName = TimeLogTab.INSERT.name }
    fun saveStoppedTimer(recorded: WorkLogEntry) {
        timerSaving = true
        onStopTimer(recorded) { saved ->
            if (saved) editor = recorded
            timerSaving = false
            timerSaved = saved
        }
    }
    BackHandler {
        if (tab != TimeLogTab.DASHBOARD) tabName = TimeLogTab.DASHBOARD.name else onBack()
    }
    Scaffold(
        containerColor = NightBlue,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TimeLogHeader(
                hourlyRateCents = settings.hourlyRateCents,
                activeTimerStartedAt = activeTimerStartedAt,
                onBack = onBack,
                onOpenTimer = { tabName = TimeLogTab.INSERT.name },
                onSaveRate = { cents -> onSaveSettings(settings.copy(hourlyRateCents = cents)) }
            )
        },
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
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).organizerBackground()) {
            when (tab) {
                TimeLogTab.DASHBOARD -> TimeLogDashboard(entries.filterNot { candidate -> pendingDeletions.any { it.id == candidate.id } }, settings.hourlyRateCents, { editor = it; timerSaved = false; resetEditorToken++; tabName = TimeLogTab.INSERT.name }) { entry ->
                    if (pendingDeletions.none { it.id == entry.id }) {
                        pendingDeletions += entry
                        onDeleteEntry(entry.id) { saved ->
                            if (!saved) pendingDeletions.removeAll { it.id == entry.id }
                        }
                    }
                }
                TimeLogTab.INSERT -> TimeLogEntryEditor(
                    existing = editor, resetToken = resetEditorToken, settings = settings,
                    activeTimerStartedAt = activeTimerStartedAt, timerSaving = timerSaving, timerSaved = timerSaved,
                    onStartTimer = { timerSaved = false; onStartTimer() },
                    onStopTimer = { startedAt ->
                        val start = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(startedAt), java.time.ZoneId.systemDefault())
                        val now = LocalDateTime.now()
                        if (workTimerExceedsOneDay(startedAt, System.currentTimeMillis())) {
                            longTimerStartedAt = startedAt
                            longTimerEnd = now.toLocalTime().format(HourFormat)
                            longTimerError = null
                        } else {
                            saveStoppedTimer(WorkLogEntry(date = start.toLocalDate().toString(), startTime = start.toLocalTime().format(HourFormat), endTime = now.toLocalTime().format(HourFormat)))
                        }
                    },
                    onSave = { entry -> onSaveEntry(entry) { saved -> if (saved) { editor = null; tabName = TimeLogTab.DASHBOARD.name } } },
                    onCancel = { editor = null; tabName = TimeLogTab.DASHBOARD.name }
                )
                TimeLogTab.SUMMARY -> TimeLogSummary(entries.filterNot { candidate -> pendingDeletions.any { it.id == candidate.id } }, settings.hourlyRateCents, onExportCsv)
                TimeLogTab.SETTINGS -> TimeLogSettingsPage(settings, onSaveSettings)
            }
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = if (tab == TimeLogTab.DASHBOARD) 88.dp else 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pendingDeletions.forEach { deleted ->
                    key(deleted.id) {
                        TimeLogUndoCard(onUndo = {
                            if (deleted.id !in pendingRestores) {
                                pendingRestores += deleted.id
                                onSaveEntry(deleted) { saved ->
                                    if (saved) pendingDeletions.removeAll { it.id == deleted.id }
                                    pendingRestores.remove(deleted.id)
                                }
                            }
                        })
                    }
                }
            }
        }
    }
    longTimerStartedAt?.let { startedAt ->
        val start = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(startedAt), java.time.ZoneId.systemDefault())
        val startTime = start.toLocalTime().format(HourFormat)
        AlertDialog(
            onDismissRequest = { longTimerStartedAt = null },
            containerColor = CardBlue,
            title = { Text("Timer oltre 24 ore", color = MainText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Il registro salva turni inferiori a 24 ore. Indica l'orario di fine del primo turno: il tempo restante dovrà essere aggiunto separatamente. Finché non confermi, il timer continua.", color = SoftText)
                    TimeLogTimeField(value = longTimerEnd, label = "Ora di fine primo turno", onValueChange = { longTimerEnd = it; longTimerError = null }, modifier = Modifier.fillMaxWidth())
                    longTimerError?.let { Text(it, color = Danger) }
                }
            },
            dismissButton = { TextButton(onClick = { longTimerStartedAt = null }) { Text("Continua timer", color = SoftText) } },
            confirmButton = { TextButton(onClick = {
                val end = normalizeTime(longTimerEnd)
                if (end == null || end == startTime) longTimerError = "Scegli un orario di fine diverso dall'inizio."
                else {
                    longTimerStartedAt = null
                    saveStoppedTimer(WorkLogEntry(date = start.toLocalDate().toString(), startTime = startTime, endTime = end))
                }
            }) { Text("Salva primo turno", color = Gold) } }
        )
    }
}

@Composable
private fun TimeLogHeader(hourlyRateCents: Long, activeTimerStartedAt: Long?, onBack: () -> Unit, onOpenTimer: () -> Unit, onSaveRate: (Long) -> Unit) {
    val elapsed = rememberElapsed(activeTimerStartedAt)
    var editRate by remember { mutableStateOf(false) }
    Surface(color = CardBlue.copy(alpha = .97f), shadowElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp).semantics { contentDescription = "Indietro" }) { Icon(painterResource(R.drawable.ic_arrow_left), contentDescription = null, tint = MainText) }
            Box(Modifier.size(38.dp).background(Gold, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_nav_time_log), contentDescription = null, tint = NightBlue) }
            Column(Modifier.weight(1f)) {
                Text("Registro ore", color = MainText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text("Gestione ore e paga", color = SoftText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            if (activeTimerStartedAt != null) TextButton(onClick = onOpenTimer, colors = ButtonDefaults.textButtonColors(contentColor = Danger)) { Text("In corso ${formatElapsed(elapsed)}", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp) }
            else Text(
                formatHourlyRate(hourlyRateCents), color = Gold, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.background(CardBlueLight, RoundedCornerShape(50)).clickable { editRate = true }.semantics { contentDescription = "Modifica tariffa oraria" }.padding(horizontal = 10.dp, vertical = 7.dp)
            )
        }
    }
    if (editRate) TimeLogRateDialog(hourlyRateCents, onDismiss = { editRate = false }) { cents ->
        onSaveRate(cents)
        editRate = false
    }
}

@Composable
private fun TimeLogDashboard(entries: List<WorkLogEntry>, rateCents: Long, onEdit: (WorkLogEntry) -> Unit, onDelete: (WorkLogEntry) -> Unit) {
    var period by rememberSaveable { mutableStateOf("week") }
    var selectedDateText by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedDate = selectedDateText?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val filtered = remember(entries, period, selectedDateText) {
        (if (selectedDate == null) filterWorkEntries(entries, period) else entries.filter { it.date == selectedDate.toString() })
            .sortedWith(compareByDescending<WorkLogEntry> { it.date }.thenByDescending { it.startTime })
    }
    val minutes = filtered.sumOf(::workMinutes)
    val days = filtered.map { it.date }.distinct().size
    LazyColumn(
        Modifier.fillMaxSize().widthIn(max = 600.dp), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { TimeLogPeriodTabs(period, listOf("week" to "Settimana corrente", "month" to "Mese corrente")) { period = it } }
        item {
            TimeLogCalendar(entries = entries, selectedDate = selectedDate) { date ->
                selectedDateText = if (date == selectedDate) null else date.toString()
            }
        }
        item { WorkOverviewCard(selectedDate?.format(DateTimeFormatter.ofPattern("d MMMM", Locale.ITALY))?.replaceFirstChar { it.titlecase(Locale.ITALY) } ?: if (period == "week") "Settimana in corso" else "Mese in corso", days, minutes, rateCents) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Registrazioni recenti", color = MainText, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp); Text("Tocca per modificare", color = SoftText, fontSize = 11.sp) } }
        if (filtered.isEmpty()) item { EmptyState("Nessun orario registrato", "Tocca + per aggiungere la tua prima giornata lavorativa.") }
        else items(filtered, key = { it.id }) { entry -> WorkEntryCard(entry, rateCents, { onEdit(entry) }, { onDelete(entry) }) }
        item { Spacer(Modifier.height(70.dp)) }
    }
}

@Composable
private fun WorkOverviewCard(label: String, days: Int, minutes: Int, rateCents: Long) {
    Column(Modifier.fillMaxWidth().background(CardBlue, RoundedCornerShape(28.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold); Text("$days ${if (days == 1) "giorno" else "giorni"}", color = MainText, fontSize = 11.sp, modifier = Modifier.background(CardBlueLight, RoundedCornerShape(50)).padding(horizontal = 9.dp, vertical = 3.dp)) }
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
private fun TimeLogEntryEditor(existing: WorkLogEntry?, resetToken: Int, settings: WorkLogSettings, activeTimerStartedAt: Long?, timerSaving: Boolean, timerSaved: Boolean, onStartTimer: () -> Unit, onStopTimer: (Long) -> Unit, onSave: (WorkLogEntry) -> Unit, onCancel: () -> Unit) {
    val seed = remember(existing?.id, resetToken) { existing ?: WorkLogEntry(date = LocalDate.now().toString(), startTime = "09:00", endTime = "18:00", breakMinutes = 60) }
    var dateText by rememberSaveable(resetToken) { mutableStateOf(formatItalianDate(seed.date)) }
    var startText by rememberSaveable(resetToken) { mutableStateOf(seed.startTime) }
    var endText by rememberSaveable(resetToken) { mutableStateOf(seed.endTime) }
    var breakText by rememberSaveable(resetToken) { mutableStateOf(seed.breakMinutes.toString()) }
    var notes by rememberSaveable(resetToken) { mutableStateOf(seed.notes) }
    var error by rememberSaveable(resetToken) { mutableStateOf<String?>(null) }
    LaunchedEffect(existing?.id, timerSaved) {
        if (timerSaved && existing != null) {
            dateText = formatItalianDate(existing.date)
            startText = existing.startTime
            endText = existing.endTime
        }
    }
    val elapsed = rememberElapsed(activeTimerStartedAt)
    val parsedDate = parseItalianDate(dateText)
    val start = normalizeTime(startText)
    val end = normalizeTime(endText)
    val minutes = if (start != null && end != null) workMinutes(start, end, breakText.toIntOrNull() ?: 0) else 0
    LazyColumn(Modifier.fillMaxSize().widthIn(max = 600.dp), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(CardBlue, RoundedCornerShape(26.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(35.dp).background(Gold.copy(alpha = .15f), CircleShape), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_nav_time_log), contentDescription = null, tint = Gold, modifier = Modifier.size(18.dp)) }; Column(Modifier.padding(start = 10.dp).weight(1f)) { Text("Timer dal vivo", color = MainText, fontWeight = FontWeight.ExtraBold); Text("Timbra entrata e uscita in tempo reale", color = SoftText, fontSize = 11.sp) }; Text(if (activeTimerStartedAt == null) "In attesa" else "In corso", color = if (activeTimerStartedAt == null) SoftText else Success, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                Text(formatElapsed(elapsed), color = MainText, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().background(NightBlue.copy(alpha = .25f), RoundedCornerShape(18.dp)).padding(13.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Button(onClick = onStartTimer, enabled = activeTimerStartedAt == null && !timerSaving, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Success, contentColor = NightBlue)) { Text("Timbra entrata", fontSize = 12.sp) }; Button(onClick = { activeTimerStartedAt?.let(onStopTimer) }, enabled = activeTimerStartedAt != null && !timerSaving, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = MainText)) { Text(if (timerSaving) "Salvataggio…" else "Timbra uscita", fontSize = 12.sp) } }
                if (timerSaved) Text("Registrazione salvata. Puoi aggiungere note e dettagli qui sotto.", color = Success, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        item { TimeLogSectionTitle("Turni preimpostati", "Tocca un turno per compilare gli orari") }
        item { Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { settings.presets.forEach { preset -> AssistChip(onClick = { startText = preset.startTime; endText = preset.endTime; breakText = preset.breakMinutes.toString() }, label = { Text("${preset.name} (${preset.startTime}-${preset.endTime})", maxLines = 1) }, colors = AssistChipDefaults.assistChipColors(containerColor = CardBlueLight, labelColor = MainText), border = null) } } }
        item {
            Column(Modifier.fillMaxWidth().background(CardBlue, RoundedCornerShape(26.dp)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(if (existing == null) "Registra giornata" else "Modifica giornata", color = MainText, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp); if (existing != null) TextButton(onClick = onCancel) { Text("Chiudi", color = SoftText) } }
                if (existing != null) Text("Voce già salvata: puoi aggiungere o modificare i dettagli.", color = SoftText, fontSize = 11.sp)
                OutlinedTextField(value = dateText, onValueChange = { dateText = it; error = null }, label = { Text("Data lavorativa (GG-MM-AAAA)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = editorFieldColors())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TimeLogTimeField(value = startText, label = "Ora inizio", onValueChange = { startText = it; error = null }, modifier = Modifier.weight(1f))
                    TimeLogTimeField(value = endText, label = "Ora fine", onValueChange = { endText = it; error = null }, modifier = Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(value = breakText, onValueChange = { breakText = it.filter(Char::isDigit); error = null }, label = { Text("Pausa (min)") }, modifier = Modifier.weight(1f), singleLine = true, colors = compactEditorFieldColors()); Column(Modifier.weight(1f).background(Gold.copy(alpha = .12f), RoundedCornerShape(16.dp)).padding(10.dp), horizontalAlignment = Alignment.End) { Text("Totale netto", color = SoftText, fontSize = 10.sp); Text("${formatWorkMinutes(minutes)}${if (start != null && end != null && isOvernightWorkShift(start, end)) " · +1 giorno" else ""}", color = MainText, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold); Text(formatCurrency(workEarningsCents(minutes, settings.hourlyRateCents)), color = MainText, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) } }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Note attività svolta") }, placeholder = { Text("Es. riunione, progetto, consegna…") }, modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp), colors = editorFieldColors())
                error?.let { Text(it, color = Danger, fontSize = 12.sp) }
                Button(onClick = {
                    val safeDate = parsedDate
                    if (safeDate == null || start == null || end == null || minutes <= 0) error = "Controlla data e orari: la durata netta deve essere maggiore di zero."
                    else onSave(seed.copy(date = safeDate.toString(), startTime = start, endTime = end, breakMinutes = (breakText.toIntOrNull() ?: 0).coerceIn(0, 480), notes = notes.trim()))
                }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)) { Text(if (existing == null) "Salva registrazione" else "Aggiorna registrazione", fontWeight = FontWeight.ExtraBold) }
            }
        }
    }
}

@Composable
private fun TimeLogRateDialog(hourlyRateCents: Long, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    var value by remember(hourlyRateCents) { mutableStateOf(formatCurrency(hourlyRateCents).substringBefore('€').trim()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBlue,
        title = { Text("Tariffa oraria", color = MainText, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Inserisci solo il valore in euro, ad esempio 15,00.", color = SoftText, fontSize = 12.sp)
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it; error = null },
                    label = { Text("€ / ora") },
                    singleLine = true,
                    isError = error != null,
                    colors = editorFieldColors()
                )
                error?.let { Text(it, color = Danger, fontSize = 12.sp) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla", color = SoftText) } },
        confirmButton = {
            TextButton(onClick = {
                val cents = parseAmountCents(value)
                if (cents == null) error = "Inserisci una tariffa valida, per esempio 15,00."
                else onSave(cents)
            }) { Text("Salva", color = Gold, fontWeight = FontWeight.ExtraBold) }
        }
    )
}

@Composable
private fun TimeLogCalendar(entries: List<WorkLogEntry>, selectedDate: LocalDate?, onSelected: (LocalDate) -> Unit) {
    var shownMonthText by rememberSaveable { mutableStateOf(YearMonth.from(selectedDate ?: LocalDate.now()).toString()) }
    val shownMonth = runCatching { YearMonth.parse(shownMonthText) }.getOrDefault(YearMonth.now())
    val entriesByDate = remember(entries, shownMonth) {
        entries.mapNotNull { entry -> runCatching { LocalDate.parse(entry.date) }.getOrNull() }
            .filter { YearMonth.from(it) == shownMonth }
            .groupingBy { it }
            .eachCount()
    }
    val firstOffset = shownMonth.atDay(1).dayOfWeek.value - 1
    val totalSlots = ((firstOffset + shownMonth.lengthOfMonth() + 6) / 7) * 7
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CardBlue)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Calendario", color = MainText, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                IconButton(onClick = { shownMonthText = shownMonth.minusMonths(1).toString() }) { Icon(painterResource(R.drawable.ic_arrow_left), contentDescription = "Mese precedente", tint = Gold) }
                Text(
                    shownMonth.month.getDisplayName(TextStyle.FULL, Locale.ITALY).replaceFirstChar { it.titlecase(Locale.ITALY) } + " ${shownMonth.year}",
                    color = MainText, fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { shownMonthText = shownMonth.plusMonths(1).toString() }) { Icon(painterResource(R.drawable.ic_arrow_right), contentDescription = "Mese successivo", tint = Gold) }
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("L", "M", "M", "G", "V", "S", "D").forEach { day ->
                    Text(day, color = SoftText, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            repeat(totalSlots / 7) { row ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { column ->
                        val day = row * 7 + column - firstOffset + 1
                        if (day !in 1..shownMonth.lengthOfMonth()) Spacer(Modifier.weight(1f).height(38.dp))
                        else {
                            val date = shownMonth.atDay(day)
                            val count = entriesByDate[date] ?: 0
                            val isSelected = date == selectedDate
                            Column(
                                modifier = Modifier.weight(1f).height(38.dp).padding(horizontal = 2.dp)
                                    .background(if (isSelected) Gold else if (count > 0) CardBlueLight else CardBlue, RoundedCornerShape(10.dp))
                                    .clickable { onSelected(date) }.semantics {
                                        contentDescription = date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ITALY)) + ", $count ${if (count == 1) "registrazione" else "registrazioni"}" + if (isSelected) ", selezionato" else ""
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(day.toString(), color = if (isSelected) NightBlue else MainText, fontWeight = if (isSelected || count > 0) FontWeight.ExtraBold else FontWeight.Normal, fontSize = 11.sp)
                                if (count > 0) Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { repeat(count.coerceAtMost(3)) { Box(Modifier.size(3.dp).background(if (isSelected) NightBlue else Gold, CircleShape)) } }
                            }
                        }
                    }
                }
            }
            Text(if (selectedDate == null) "Tocca un giorno per vedere le registrazioni." else "Tocca di nuovo il giorno selezionato per tornare al periodo.", color = SoftText, fontSize = 10.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeLogTimeField(value: String, label: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var pickerOpen by remember { mutableStateOf(false) }
    val normalized = normalizeTime(value)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text("00:00") },
        trailingIcon = { TextButton(onClick = { pickerOpen = true }) { Text("Scegli", color = Gold, fontSize = 10.sp) } },
        modifier = modifier,
        singleLine = true,
        colors = compactEditorFieldColors()
    )
    if (pickerOpen) {
        val now = LocalTime.now()
        val initialHour = normalized?.substringBefore(':')?.toIntOrNull() ?: now.hour
        val initialMinute = normalized?.substringAfter(':')?.toIntOrNull() ?: now.minute
        val pickerState = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickerOpen = false },
            containerColor = CardBlue,
            title = { Text("Scegli l'orario", color = MainText) },
            text = { TimePicker(state = pickerState) },
            dismissButton = { TextButton(onClick = { pickerOpen = false }) { Text("Annulla", color = SoftText) } },
            confirmButton = { TextButton(onClick = { onValueChange(String.format(Locale.ITALY, "%02d:%02d", pickerState.hour, pickerState.minute)); pickerOpen = false }) { Text("Conferma", color = Gold) } }
        )
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
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { TimeLogStat(R.drawable.ic_nav_time_log, "Ore lavorate", formatWorkMinutes(minutes), Modifier.weight(1f)); TimeLogStat(R.drawable.ic_time_rate, "Guadagno totale", formatCurrency(workEarningsCents(minutes, rateCents)), Modifier.weight(1f)) } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { TimeLogStat(R.drawable.ic_nav_calendar, "Giorni lavorati", "$days", Modifier.weight(1f)); TimeLogStat(R.drawable.ic_time_summary, "Media ore / giorno", formatWorkMinutes(if (days == 0) 0 else minutes / days), Modifier.weight(1f)) } }
        item { Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CardBlue)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Esporta report", color = MainText, fontWeight = FontWeight.ExtraBold); Text("Scarica il foglio CSV del periodo", color = SoftText, fontSize = 12.sp) }; Button(onClick = { onExportCsv("TimeLog_Report_${period}_${LocalDate.now()}.csv", workCsv(filtered, rateCents)) }, shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue), enabled = filtered.isNotEmpty()) { Text("CSV") } } } }
        item { Text("Dettaglio tabellare · ${filtered.size} voci", color = MainText, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp) }
        if (filtered.isEmpty()) item { EmptyState("Nessun dato per questo periodo", "Aggiungi una registrazione per visualizzare le statistiche.") }
        else items(filtered, key = { it.id }) { entry -> Row(Modifier.fillMaxWidth().background(CardBlue, RoundedCornerShape(16.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(formatItalianDate(entry.date), color = MainText, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text("${entry.startTime} – ${entry.endTime}", color = SoftText, fontSize = 11.sp) }; Text(formatWorkMinutes(workMinutes(entry)), color = MainText, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(formatCurrency(workEarningsCents(workMinutes(entry), rateCents)), color = Success, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(82.dp), textAlign = TextAlign.End) } }
    }
}

@Composable
private fun TimeLogSettingsPage(settings: WorkLogSettings, onSaveSettings: (WorkLogSettings) -> Unit) {
    var rate by rememberSaveable(settings.hourlyRateCents) { mutableStateOf(formatCurrency(settings.hourlyRateCents).substringBefore('€').trim()) }
    var rateError by rememberSaveable { mutableStateOf<String?>(null) }
    var newName by rememberSaveable { mutableStateOf("") }
    var newStart by rememberSaveable { mutableStateOf("09:00") }
    var newEnd by rememberSaveable { mutableStateOf("18:00") }
    var newBreak by rememberSaveable { mutableStateOf("0") }
    var presetPendingDelete by remember { mutableStateOf<WorkShiftPreset?>(null) }
    LazyColumn(Modifier.fillMaxSize().widthIn(max = 600.dp), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                    IconButton(onClick = { presetPendingDelete = preset }) { Icon(painterResource(R.drawable.ic_delete), contentDescription = "Elimina turno ${preset.name}", tint = Danger) }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CardBlueLight.copy(alpha = .85f))) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Aggiungi nuovo turno rapido", color = MainText, fontWeight = FontWeight.ExtraBold)
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Nome turno") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = compactEditorFieldColors())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimeLogTimeField(value = newStart, label = "Inizio", onValueChange = { newStart = it }, modifier = Modifier.weight(1f))
                        TimeLogTimeField(value = newEnd, label = "Fine", onValueChange = { newEnd = it }, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = newBreak, onValueChange = { newBreak = it.filter(Char::isDigit) }, label = { Text("Pausa (minuti)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = compactEditorFieldColors())
                    Button(onClick = { val start = normalizeTime(newStart); val end = normalizeTime(newEnd); if (newName.isNotBlank() && start != null && end != null) { onSaveSettings(settings.copy(presets = settings.presets + WorkShiftPreset(name = newName.trim(), startTime = start, endTime = end, breakMinutes = (newBreak.toIntOrNull() ?: 0).coerceIn(0, 480)))); newName = "" } }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)) { Text("+ Aggiungi turno") }
                }
            }
        }
        item { Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = CardBlue)) { Column(Modifier.fillMaxWidth().padding(16.dp)) { Text("Dati protetti", color = MainText, fontWeight = FontWeight.ExtraBold); Text("Le registrazioni del Registro ore sono già incluse nel backup cifrato di Task Organizer.", color = SoftText, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp)) } } }
    }
    presetPendingDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { presetPendingDelete = null },
            containerColor = CardBlue,
            title = { Text("Eliminare ${preset.name}?", color = MainText) },
            text = { Text("Il turno rapido non sarà più disponibile nell’inserimento delle ore.", color = SoftText) },
            dismissButton = { TextButton(onClick = { presetPendingDelete = null }) { Text("Annulla", color = SoftText) } },
            confirmButton = { TextButton(onClick = { onSaveSettings(settings.copy(presets = settings.presets.filterNot { it.id == preset.id })); presetPendingDelete = null }) { Text("Elimina", color = Danger) } }
        )
    }
}

@Composable private fun TimeLogPeriodTabs(selected: String, choices: List<Pair<String, String>>, onSelected: (String) -> Unit) = Row(Modifier.fillMaxWidth().background(CardBlue, RoundedCornerShape(18.dp)).padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) { choices.forEach { (key, label) -> Text(label, color = if (selected == key) NightBlue else SoftText, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).background(if (selected == key) Gold else CardBlue, RoundedCornerShape(14.dp)).clickable { onSelected(key) }.padding(vertical = 10.dp)) } }
@Composable private fun TimeLogStat(icon: Int, label: String, value: String, modifier: Modifier) = Card(modifier, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = CardBlue)) { Column(Modifier.padding(15.dp)) { Icon(painterResource(icon), contentDescription = null, tint = Gold, modifier = Modifier.size(20.dp)); Text(label, color = SoftText, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)); Text(value, color = MainText, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
@Composable private fun TimeLogSectionTitle(title: String, subtitle: String) = Column { Text(title, color = MainText, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp); Text(subtitle, color = SoftText, fontSize = 11.sp) }
@Composable private fun TimeLogUndoCard(onUndo: () -> Unit) = Card(colors = CardDefaults.cardColors(containerColor = CardBlueLight), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) { Text("Voce eliminata", color = MainText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); TextButton(onClick = onUndo) { Text("Annulla", color = Gold, fontWeight = FontWeight.ExtraBold) } } }

private val HourFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
internal fun workTimerExceedsOneDay(startedAt: Long, now: Long): Boolean = now >= startedAt && now - startedAt >= 24L * 60 * 60 * 1_000
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
