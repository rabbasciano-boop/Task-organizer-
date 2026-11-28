package com.example.taskorganizer

import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
@Composable
internal fun TaskEditorScreen(
    task: OrganizerTask?,
    customCategories: List<String>,
    customCategoryIcons: Map<String, String>,
    onAddCustomCategory: (String, String) -> Unit,
    onDeleteCustomCategory: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: (OrganizerTask) -> Unit,
    notificationPermissionAllowed: Boolean,
    onRequestNotificationPermission: () -> Unit
) {
    val context = LocalContext.current
    var title by rememberSaveable(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var date by rememberSaveable(task?.id) { mutableStateOf(task?.date?.let(::formatItalianDate) ?: LocalDate.now().format(ItalianDateFormatter)) }
    var time by rememberSaveable(task?.id) { mutableStateOf(task?.time.orEmpty()) }
    var endTime by rememberSaveable(task?.id) { mutableStateOf(task?.endTime.orEmpty()) }
    var category by rememberSaveable(task?.id) { mutableStateOf(task?.category ?: "Personale") }
    var categoryIcon by rememberSaveable(task?.id) { mutableStateOf(task?.categoryIcon ?: customCategoryIcons[task?.category]) }
    var priority by rememberSaveable(task?.id) { mutableStateOf(task?.priority ?: "Media") }
    var note by rememberSaveable(task?.id) { mutableStateOf(task?.note.orEmpty()) }
    var examStatus by rememberSaveable(task?.id) { mutableStateOf(task?.examStatus ?: "Da prenotare") }
    var reminderEnabled by rememberSaveable(task?.id) { mutableStateOf(task?.reminderEnabled ?: true) }
    var reminderMinutesBefore by rememberSaveable(task?.id) { mutableStateOf(task?.reminderMinutesBefore ?: 15) }
    var reminderRepeatMinutes by rememberSaveable(task?.id) { mutableStateOf(task?.reminderRepeatMinutes ?: 0) }
    var alarmEnabled by rememberSaveable(task?.id) { mutableStateOf(task?.alarmEnabled ?: false) }
    var cardColor by rememberSaveable(task?.id) { mutableStateOf(task?.cardColor ?: DefaultCardColor) }
    var recurrence by rememberSaveable(task?.id) { mutableStateOf(task?.recurrence ?: "none") }
    var subtasks by rememberSaveable(task?.id, stateSaver = SubtaskListSaver) { mutableStateOf(task?.subtasks.orEmpty()) }
    var subtaskDraft by rememberSaveable(task?.id) { mutableStateOf("") }
    var error by rememberSaveable(task?.id) { mutableStateOf("") }
    var discardDialogOpen by rememberSaveable(task?.id) { mutableStateOf(false) }
    var reminderAdvanceDialogOpen by rememberSaveable(task?.id) { mutableStateOf(false) }
    var datePickerOpen by rememberSaveable(task?.id) { mutableStateOf(false) }
    val initialDate = remember(task?.id) { task?.date?.let(::formatItalianDate) ?: LocalDate.now().format(ItalianDateFormatter) }
    val hasUnsavedChanges = title != task?.title.orEmpty() ||
        date != initialDate ||
        time != task?.time.orEmpty() ||
        endTime != task?.endTime.orEmpty() ||
        category != (task?.category ?: "Personale") ||
        categoryIcon != (task?.categoryIcon ?: customCategoryIcons[task?.category]) ||
        priority != (task?.priority ?: "Media") ||
        note != task?.note.orEmpty() ||
        examStatus != (task?.examStatus ?: "Da prenotare") ||
        reminderEnabled != (task?.reminderEnabled ?: true) ||
        reminderMinutesBefore != (task?.reminderMinutesBefore ?: 15) ||
        reminderRepeatMinutes != (task?.reminderRepeatMinutes ?: 0) ||
        alarmEnabled != (task?.alarmEnabled ?: false) ||
        cardColor != (task?.cardColor ?: DefaultCardColor) ||
        recurrence != (task?.recurrence ?: "none") ||
        subtasks != task?.subtasks.orEmpty() || subtaskDraft.isNotBlank()

    fun requestClose() {
        if (hasUnsavedChanges) discardDialogOpen = true else onCancel()
    }

    fun attemptSave(reminderOverride: Int? = null) {
        val parsedDate = parseItalianDate(date)
        val normalizedTime = normalizeTime(time)
        val normalizedEndTime = normalizeTime(endTime)
        val validTime = time.isBlank() || normalizedTime != null
        val validEndTime = endTime.isBlank() || normalizedEndTime != null
        error = when {
            title.isBlank() -> "Inserisci un titolo."
            subtaskDraft.isNotBlank() -> context.getString(R.string.subtasks_pending)
            parsedDate == null -> "La data deve essere nel formato GG-MM-AAAA."
            !validTime -> "L'orario deve essere nel formato 00:00."
            !validEndTime -> "L'orario di fine deve essere nel formato 00:00."
            normalizedTime == null && normalizedEndTime != null -> "Inserisci prima l'orario di inizio."
            normalizedTime != null && normalizedEndTime != null && normalizedEndTime <= normalizedTime -> "L'orario di fine deve essere successivo all'inizio."
            else -> ""
        }
        if (error.isBlank()) {
            val selectedAdvance = reminderOverride ?: reminderMinutesBefore
            if (reminderEnabled && normalizedTime != null) {
                val startAt = LocalDateTime.of(parsedDate, LocalTime.parse(normalizedTime)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                if (ReminderTiming.wouldMissAllFutureReminders(startAt, selectedAdvance, reminderRepeatMinutes, System.currentTimeMillis())) {
                    reminderAdvanceDialogOpen = true
                    return
                }
            }
            val recurrenceScheduleChanged = task != null &&
                (task.date != parsedDate.toString() || task.recurrence != recurrence)
            onSave(
                OrganizerTask(
                    id = task?.id ?: java.util.UUID.randomUUID().toString(), title = title.trim(), date = parsedDate.toString(),
                    time = normalizedTime, endTime = normalizedEndTime, category = category, categoryIcon = categoryIcon, priority = priority, note = note.trim(),
                    completed = task?.completed ?: false, reminderEnabled = reminderEnabled && normalizedTime != null, reminderMinutesBefore = selectedAdvance, reminderRepeatMinutes = reminderRepeatMinutes, alarmEnabled = alarmEnabled && reminderEnabled && normalizedTime != null, cardColor = cardColor, examStatus = if (category == "Esami") examStatus else null,
                    createdAt = task?.createdAt ?: System.currentTimeMillis(), deletedAt = task?.deletedAt ?: 0L, recurrence = recurrence,
                    seriesId = if (recurrence == "none") null else task?.seriesId,
                    recurrenceAnchorDate = if (recurrence == "none" || recurrenceScheduleChanged) null else task?.recurrenceAnchorDate,
                    subtasks = subtasks
                )
            )
        }
    }
    BackHandler(onBack = ::requestClose)

    if (reminderAdvanceDialogOpen) {
        AlertDialog(
            onDismissRequest = { reminderAdvanceDialogOpen = false }, containerColor = CardBlue,
            title = { Text(stringResource(R.string.reminder_advance_passed_title), color = MainText) },
            text = { Text(stringResource(R.string.reminder_advance_passed_message), color = SoftText) },
            confirmButton = { TextButton(onClick = { reminderAdvanceDialogOpen = false; reminderMinutesBefore = 0; attemptSave(0) }) { Text(stringResource(R.string.reminder_notify_at_start), color = Gold) } },
            dismissButton = { TextButton(onClick = { reminderAdvanceDialogOpen = false }) { Text(stringResource(R.string.reminder_change_advance), color = SoftText) } }
        )
    }

    Box(modifier = Modifier.fillMaxSize().organizerBackground()) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = ::requestClose) { Text("×", color = MainText, fontSize = 26.sp) }
            Text(if (task == null) "Nuova attività" else "Modifica attività", color = MainText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = { attemptSave() }) { Text("Salva", color = Gold, fontWeight = FontWeight.Bold) }
        }
        if (error.isNotBlank()) {
            Text(error, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), color = Danger, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ANTEPRIMA CARD LIVE", color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.size(5.dp))
            TaskEditorPreview(
                title = title,
                note = note,
                date = date,
                time = time,
                endTime = endTime,
                category = category,
                categoryIcon = categoryIcon,
                priority = priority,
                reminderEnabled = reminderEnabled,
                reminderMinutesBefore = reminderMinutesBefore,
                alarmEnabled = alarmEnabled,
                cardColor = cardColor,
                subtasks = subtasks
            )
        }
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        Card(modifier = Modifier.editorSunsetGlow(), colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(22.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("TITOLO ATTIVITÀ", color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = title, onValueChange = { title = it }, placeholder = { Text("Es. Ripasso capitolo 3") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(18.dp), colors = compactEditorFieldColors())
                Text("NOTE · FACOLTATIVE", color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = note, onValueChange = { note = it }, placeholder = { Text("Aggiungi dettagli o cose da ricordare") }, modifier = Modifier.fillMaxWidth(), minLines = 2, shape = RoundedCornerShape(18.dp), colors = compactEditorFieldColors())
                TaskColorPicker(selectedColor = cardColor, onSelected = { cardColor = it })
            }
        }
        SubtaskEditor(subtasks, subtaskDraft, { subtaskDraft = it }, { subtasks = it })
        Card(modifier = Modifier.editorSunsetGlow(), colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(22.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("DATA E FASCIA ORARIA", color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactEditorChip("Oggi", date == LocalDate.now().format(ItalianDateFormatter), { date = LocalDate.now().format(ItalianDateFormatter) }, Modifier.weight(1f))
                    CompactEditorChip("Domani", date == LocalDate.now().plusDays(1).format(ItalianDateFormatter), { date = LocalDate.now().plusDays(1).format(ItalianDateFormatter) }, Modifier.weight(1f))
                    CompactEditorChip("📅 $date", false, { datePickerOpen = true }, Modifier.weight(1f))
                }
        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Data") },
            placeholder = { Text("GG-MM-AAAA oppure GG/MM/AAAA") },
            trailingIcon = {
                IconButton(onClick = { datePickerOpen = true }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_nav_calendar),
                        contentDescription = "Apri calendario",
                        tint = Gold
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = compactEditorFieldColors()
        )
        if (datePickerOpen) {
            val initialDateMillis = remember(date) { parseItalianDate(date)?.toPickerMillis() }
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
            DatePickerDialog(
                onDismissRequest = { datePickerOpen = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { selectedMillis ->
                            date = selectedMillis.toPickerDate().format(ItalianDateFormatter)
                        }
                        datePickerOpen = false
                    }) { Text("Conferma", color = Gold) }
                },
                dismissButton = { TextButton(onClick = { datePickerOpen = false }) { Text("Annulla", color = SoftText) } },
                colors = androidx.compose.material3.DatePickerDefaults.colors(containerColor = CardBlue)
            ) {
                DatePicker(state = datePickerState)
            }
        }
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeField(label = "Ora inizio", value = time, onValueChange = { time = it })
                    TimeField(label = "Ora fine", value = endTime, optional = true, onValueChange = { endTime = it })
                }
            }
        }
        Card(modifier = Modifier.editorSunsetGlow(), colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(22.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Promemoria e sveglia", color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Promemoria", color = MainText, fontWeight = FontWeight.SemiBold)
                Text("Scegli quando ricevere l'avviso", color = SoftText, fontSize = 13.sp)
            }
            Switch(checked = reminderEnabled && time.isNotBlank(), onCheckedChange = { reminderEnabled = it }, enabled = time.isNotBlank())
        }
        if (reminderEnabled && time.isNotBlank()) {
            if (!notificationPermissionAllowed) {
                Text("Le notifiche sono disattivate: puoi comunque salvare l'attività, ma Android non potrà avvisarti.", color = Danger, fontSize = 13.sp)
                OutlinedButton(
                    onClick = onRequestNotificationPermission,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Attiva notifiche", color = Gold, fontWeight = FontWeight.SemiBold) }
            }
            val advanceOptions = listOf(0 to "All'orario", 5 to "5 min", 10 to "10 min", 15 to "15 min", 30 to "30 min", 60 to "1 ora", 120 to "2 ore", 1440 to "1 giorno")
            Text("Avvisami", color = MainText, fontWeight = FontWeight.SemiBold)
            EditorChoiceChips(
                options = advanceOptions.map { (minutes, label) -> minutes.toString() to label },
                selectedKey = reminderMinutesBefore.toString(),
                onSelected = { reminderMinutesBefore = it.toIntOrNull() ?: 15 }
            )
            val repeatOptions = listOf(0 to "Non ripetere", 5 to "Ogni 5 min", 10 to "Ogni 10 min", 15 to "Ogni 15 min", 30 to "Ogni 30 min", 60 to "Ogni ora")
            Text("Ripeti avviso", color = MainText, fontWeight = FontWeight.SemiBold)
            EditorChoiceChips(
                options = repeatOptions.map { (minutes, label) -> minutes.toString() to label },
                selectedKey = reminderRepeatMinutes.toString(),
                onSelected = { reminderRepeatMinutes = it.toIntOrNull() ?: 0 }
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Modalità sveglia", color = MainText, fontWeight = FontWeight.SemiBold)
                Text("Suono e vibrazione per gli impegni importanti", color = SoftText, fontSize = 13.sp)
            }
            Switch(
                checked = alarmEnabled && reminderEnabled && time.isNotBlank(),
                onCheckedChange = { alarmEnabled = it },
                enabled = reminderEnabled && time.isNotBlank()
            )
        }
        if (reminderEnabled && time.isNotBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !TaskReminderScheduler.canUseExactAlarms(context)) {
            Text(stringResource(R.string.reminder_exact_permission_message), color = SoftText, fontSize = 13.sp)
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        android.content.Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            .setData(android.net.Uri.parse("package:${context.packageName}"))
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Consenti sveglie precise", color = Gold, fontWeight = FontWeight.SemiBold) }
        }
            }
        }
        Card(modifier = Modifier.editorSunsetGlow(), colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(22.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Organizzazione", color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        CategoryField(
            value = category,
            icon = categoryIcon,
            customCategories = customCategories,
            customCategoryIcons = customCategoryIcons,
            onSelected = {
                category = it
                categoryIcon = customCategoryIcons[it]
            },
            onAddCustomCategory = { newCategory, icon ->
                onAddCustomCategory(newCategory, icon)
                category = newCategory
                categoryIcon = icon
            },
            onDeleteCustomCategory = onDeleteCustomCategory
        )
        Text("Priorità", color = MainText, fontWeight = FontWeight.SemiBold)
        EditorChoiceChips(
            options = listOf("Bassa" to "🟢 Bassa", "Media" to "🟡 Media", "Alta" to "🔴 Alta"),
            selectedKey = priority,
            onSelected = { priority = it }
        )
        val recurrenceOptions = linkedMapOf("none" to "Non ripetere", "daily" to "Ogni giorno", "weekly" to "Ogni settimana", "monthly" to "Ogni mese")
        Text("Ripetizione", color = MainText, fontWeight = FontWeight.SemiBold)
        EditorChoiceChips(options = recurrenceOptions.toList(), selectedKey = recurrence, onSelected = { recurrence = it })
        if (category == "Esami") {
            OptionField("Stato dell'esame", examStatus, listOf("Da prenotare", "Prenotato", "Fatto")) { examStatus = it }
        }
            }
        }
        if (error.isNotBlank()) Text(error, color = Danger, fontSize = 13.sp)
        Button(
            onClick = { attemptSave() },
            modifier = Modifier.fillMaxWidth().height(54.dp).editorSunsetGlow(RoundedCornerShape(16.dp), strong = true), shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)
        ) { Text("Salva attività", fontWeight = FontWeight.Bold) }
        TextButton(onClick = ::requestClose, modifier = Modifier.fillMaxWidth()) { Text("Annulla", color = SoftText) }
        }
    }
    }
    if (discardDialogOpen) {
        AlertDialog(
            onDismissRequest = { discardDialogOpen = false },
            containerColor = CardBlue,
            title = { Text("Scartare le modifiche?", color = MainText) },
            text = { Text("Le modifiche non salvate a questa attività andranno perse.", color = SoftText) },
            confirmButton = {
                TextButton(onClick = {
                    discardDialogOpen = false
                    onCancel()
                }) { Text("Scarta", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { discardDialogOpen = false }) { Text("Continua a modificare", color = Gold) } }
        )
    }
}

@Composable
private fun Modifier.editorSunsetGlow(shape: RoundedCornerShape = RoundedCornerShape(22.dp), strong: Boolean = false): Modifier =
    if (com.example.taskorganizer.ui.theme.OrganizerPalette.sunsetGlow) sunsetGlow(shape, strong) else this

@Composable
private fun TaskEditorPreview(
    title: String,
    note: String,
    date: String,
    time: String,
    endTime: String,
    category: String,
    categoryIcon: String?,
    priority: String,
    reminderEnabled: Boolean,
    reminderMinutesBefore: Int,
    alarmEnabled: Boolean,
    cardColor: String,
    subtasks: List<Subtask>
) {
    val useSunsetGradient = com.example.taskorganizer.ui.theme.OrganizerPalette.gradientTaskCards
    val previewShape = RoundedCornerShape(22.dp)
    val previewBackground = if (useSunsetGradient) Color.Transparent else taskCardBackground(cardColor).copy(alpha = 0.76f)
    val previewText = if (useSunsetGradient) {
        if (com.example.taskorganizer.ui.theme.OrganizerPalette.agendaReale) Color(0xFF240833) else Color.White
    } else if (cardColor == DefaultCardColor) MainText else Color(0xFFF7F9FC)
    val previewMuted = if (useSunsetGradient) {
        if (com.example.taskorganizer.ui.theme.OrganizerPalette.agendaReale) Color(0xFF240833).copy(alpha = 0.78f) else Color.White
    } else if (cardColor == DefaultCardColor) SoftText else Color(0xFFD8E1EC)
    val reminderText = when (reminderMinutesBefore) {
        0 -> "All'orario"
        60 -> "1 ora prima"
        120 -> "2 ore prima"
        1440 -> "1 giorno prima"
        else -> "$reminderMinutesBefore min prima"
    }
    Card(modifier = if (com.example.taskorganizer.ui.theme.OrganizerPalette.sunsetGlow) Modifier.sunsetGlow(previewShape, strong = true).sunsetTaskSurface(cardColor, previewShape) else Modifier.then(if (useSunsetGradient) Modifier.sunsetTaskSurface(cardColor, previewShape) else Modifier).sunsetGlow(previewShape, strong = useSunsetGradient), colors = CardDefaults.cardColors(containerColor = previewBackground), shape = previewShape) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(listOfNotNull(categoryDisplay(category, categoryIcon), subtaskCount(subtasks)).joinToString(" · "), color = previewText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (alarmEnabled) Text("⏰ Sveglia", color = previewText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(priority, color = previewText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(title.ifBlank { "Titolo nuova attività" }, color = previewText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (note.isNotBlank()) Text("📝 $note", color = previewMuted, fontSize = 12.sp, maxLines = 1)
            HorizontalDivider(color = previewMuted.copy(alpha = 0.25f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("📅 $date${if (time.isNotBlank()) " · $time${if (endTime.isNotBlank()) "–$endTime" else ""}" else ""}", color = previewMuted, fontSize = 12.sp)
                if (reminderEnabled && time.isNotBlank()) Text("🔔 $reminderText", color = previewMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CompactEditorChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.height(40.dp).background(if (selected) Gold else CardBlueLight, RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) NightBlue else MainText, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun EditorChoiceChips(
    options: List<Pair<String, String>>,
    selectedKey: String,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(3).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (key, label) ->
                    val selected = key == selectedKey
                    Box(
                        modifier = Modifier.weight(1f).height(40.dp).background(if (selected) Gold else CardBlueLight, RoundedCornerShape(12.dp)).clickable { onSelected(key) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (selected) NightBlue else MainText, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(label: String, value: String, optional: Boolean = false, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var pickerOpen by remember { mutableStateOf(false) }
    val normalized = normalizeTime(value)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(if (optional) "00:00 — facoltativo" else "00:00 — formato 24 ore") },
        trailingIcon = { TextButton(onClick = { pickerOpen = true }) { Text("Scegli", color = Gold) } },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = compactEditorFieldColors()
    )
    if (pickerOpen) {
        val currentTime = LocalTime.now().withSecond(0).withNano(0)
        val initialHour = normalized?.substringBefore(":")?.toIntOrNull() ?: currentTime.hour
        val initialMinute = normalized?.substringAfter(":")?.toIntOrNull() ?: currentTime.minute
        key(value, pickerOpen) {
            val pickerState = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
            AlertDialog(
            onDismissRequest = { pickerOpen = false },
            containerColor = CardBlue,
            title = { Text("Scegli l'orario", color = MainText) },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(onClick = {
                    onValueChange(String.format(Locale.ITALY, "%02d:%02d", pickerState.hour, pickerState.minute))
                    pickerOpen = false
                }) { Text("Conferma", color = Gold) }
            },
            dismissButton = { TextButton(onClick = { pickerOpen = false }) { Text("Annulla", color = SoftText) } }
            )
        }
    }
}

@Composable
private fun TaskColorPicker(selectedColor: String, onSelected: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Colore della card", color = MainText, fontWeight = FontWeight.SemiBold)
        Text(TaskCardColorOptions.firstOrNull { it.first == selectedColor }?.second ?: "Predefinito", color = SoftText, fontSize = 12.sp)
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        TaskCardColorOptions.forEach { (color, label) ->
            Card(
                modifier = Modifier.size(34.dp).border(if (selectedColor == color) 2.dp else 0.dp, Gold, RoundedCornerShape(50)).clickable { onSelected(color) },
                colors = CardDefaults.cardColors(containerColor = taskCardBackground(color)),
                shape = RoundedCornerShape(50)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (selectedColor == color) "✓" else "", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CustomCategoryIconPicker(selectedIcon: String, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CustomCategoryIconOptions.chunked(6).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { icon ->
                    Card(
                        modifier = Modifier.size(38.dp).clickable { onSelected(icon) },
                        colors = CardDefaults.cardColors(containerColor = if (icon == selectedIcon) Gold else CardBlueLight),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(icon, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryField(
    value: String,
    icon: String?,
    customCategories: List<String>,
    customCategoryIcons: Map<String, String>,
    onSelected: (String) -> Unit,
    onAddCustomCategory: (String, String) -> Unit,
    onDeleteCustomCategory: (String) -> Unit
) {
    var customDialogOpen by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customIcon by remember { mutableStateOf(CustomCategoryIconOptions.first()) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    val standardCategories = listOf("Studio", "Personale", "Lavoro", "Esami")
    val categories = standardCategories + customCategories.filter { it !in standardCategories }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Categoria", color = MainText, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = { customDialogOpen = true }) { Text("+ Nuova categoria", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        categories.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { category ->
                    val selected = category == value
                    val isCustomCategory = category in customCategories
                    Box(
                        modifier = Modifier.weight(1f).height(40.dp).background(if (selected) Gold else CardBlueLight, RoundedCornerShape(12.dp)).combinedClickable(
                            onClick = { onSelected(category) },
                            onLongClick = if (isCustomCategory) { { categoryToDelete = category } } else null
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(categoryDisplay(category, customCategoryIcons[category]), color = if (selected) NightBlue else MainText, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
                repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    if (customDialogOpen) {
        AlertDialog(
            onDismissRequest = { customDialogOpen = false },
            containerColor = CardBlue,
            title = { Text("Nuova categoria", color = MainText) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Nome della categoria") },
                        placeholder = { Text("Es. Salute") },
                        singleLine = true,
                        colors = editorFieldColors()
                    )
                    Text("Scegli un'icona", color = SoftText, fontSize = 13.sp)
                    CustomCategoryIconPicker(selectedIcon = customIcon, onSelected = { customIcon = it })
                    OutlinedTextField(
                        value = customIcon,
                        onValueChange = { customIcon = it },
                        label = { Text("Oppure usa una tua emoji") },
                        placeholder = { Text("Es. 🎧") },
                        singleLine = true,
                        colors = editorFieldColors()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val category = customName.trim()
                    if (category.isNotBlank()) {
                        onAddCustomCategory(category, customIcon.ifBlank { CustomCategoryIconOptions.first() })
                        customName = ""
                        customIcon = CustomCategoryIconOptions.first()
                        customDialogOpen = false
                    }
                }) { Text("Aggiungi", color = Gold) }
            },
            dismissButton = { TextButton(onClick = { customDialogOpen = false }) { Text("Annulla", color = SoftText) } }
        )
    }
    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            containerColor = CardBlue,
            title = { Text("Eliminare $category?", color = MainText) },
            text = { Text("La categoria sparirà dall'elenco, ma le attività già associate non verranno cancellate.", color = SoftText) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCustomCategory(category)
                    categoryToDelete = null
                }) { Text("Elimina", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { categoryToDelete = null }) { Text("Annulla", color = SoftText) } }
        )
    }
}

