package com.example.taskorganizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.taskorganizer.ui.theme.CartaHeadingFont
import com.example.taskorganizer.ui.theme.OrganizerPalette
import com.example.taskorganizer.ui.theme.OrganizerTheme

@Composable
internal fun TodayScreen(
    tasks: List<OrganizerTask>,
    nickname: String,
    signedInAccount: SignedInAccount?,
    onAdd: () -> Unit,
    onQuickAdd: (OrganizerTask, (QuickTaskSaveResult) -> Unit) -> Unit,
    backupLastAt: Long? = null,
    backupSnoozedUntil: Long? = null,
    onOpenBackup: () -> Unit = {},
    onSnoozeBackup: () -> Unit = {},
    onToggleTask: (String) -> Unit,
    onCompleteTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onRestoreTask: (String) -> Unit,
    onEdit: (OrganizerTask) -> Unit,
    onOpenActivities: () -> Unit,
    onOpenLists: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenTimeLog: () -> Unit,
    onManageAccount: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    var deletedId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(deletedId) {
        val taskId = deletedId ?: return@LaunchedEffect
        if (snackbar.showSnackbar(message = "Attività spostata nel Cestino", actionLabel = "Annulla", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) onRestoreTask(taskId)
        deletedId = null
    }
    var selectedDateValue by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var currentDate by remember { mutableStateOf(LocalDate.now()) }
    var backupNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            backupNow = System.currentTimeMillis()
            LocalDate.now().takeIf { it != currentDate }?.let { today -> currentDate = today; selectedDateValue = today.toString() }
        }
    }
    val selectedDate = LocalDate.parse(selectedDateValue)
    val weekDays = (0L..6L).map { selectedDate.with(DayOfWeek.MONDAY).plusDays(it) }
    var filter by rememberSaveable { mutableStateOf("all") }
    val selectedTasks = tasks.filter { it.date == selectedDateValue }.sortedBy { it.time ?: "99:99" }
    val visibleTasks = selectedTasks.filter {
        when (filter) {
            "scheduled" -> !it.completed
            "alarm" -> it.alarmEnabled
            "completed" -> it.completed
            else -> true
        }
    }
    val completedCount = selectedTasks.count { it.completed }
    val isToday = selectedDate == currentDate
    val nextTask = selectedTasks.firstOrNull { task ->
        !task.completed && !task.time.isNullOrBlank() && (!isToday || task.time >= LocalTime.now().withSecond(0).withNano(0).toString())
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALY) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val context = LocalContext.current
    var quickText by rememberSaveable { mutableStateOf("") }
    var quickId by rememberSaveable { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    var quickSaving by remember { mutableStateOf(false) }
    var confirmPastQuickTask by rememberSaveable { mutableStateOf(false) }
    var confirmQuickReminderAtStart by rememberSaveable { mutableStateOf(false) }
    var quickNow by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(quickText) {
        quickNow = LocalDateTime.now()
        while (quickText.isNotBlank()) { delay(15_000); quickNow = LocalDateTime.now() }
    }
    val quickDraft = remember(quickText, quickNow) { quickText.takeIf { it.isNotBlank() }?.let { parseQuickTask(it, quickNow) } }
    fun saveQuickTask(reminderMinutesBefore: Int = 15) {
        if (quickText.isBlank() || quickSaving) return
        val textAtSubmit = quickText
        val idAtSubmit = quickId
        val parsed = parseQuickTask(textAtSubmit, LocalDateTime.now())
        val task = OrganizerTask(id = idAtSubmit, title = parsed.title, date = parsed.date.toString(), time = parsed.time?.toString(), reminderEnabled = parsed.time != null, reminderMinutesBefore = reminderMinutesBefore)
        quickSaving = true
        onQuickAdd(task) { result ->
            quickSaving = false
            if (result != QuickTaskSaveResult.FAILED) {
                if (quickId == idAtSubmit && quickText == textAtSubmit) {
                    quickText = ""
                    quickId = java.util.UUID.randomUUID().toString()
                }
                scope.launch {
                    val message = context.getString(if (result == QuickTaskSaveResult.SAVED) R.string.quick_saved else R.string.quick_saved_no_reminder)
                    if (snackbar.showSnackbar(message, actionLabel = context.getString(R.string.quick_edit), duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) onEdit(task)
                }
            }
        }
    }
    fun submitQuickTask() {
        if (quickText.isBlank() || quickSaving) return
        val now = LocalDateTime.now()
        val draft = parseQuickTask(quickText, now)
        val startAt = draft.time?.let { LocalDateTime.of(draft.date, it).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }
        when {
            draft.isPast(now) -> confirmPastQuickTask = true
            startAt != null && ReminderTiming.wouldMissAllFutureReminders(startAt, 15, 0, System.currentTimeMillis()) -> confirmQuickReminderAtStart = true
            else -> saveQuickTask()
        }
    }
    if (confirmPastQuickTask) {
        AlertDialog(
            onDismissRequest = { confirmPastQuickTask = false },
            containerColor = CardBlue,
            title = { Text(stringResource(R.string.quick_past_confirm_title), color = MainText) },
            text = { Text(stringResource(R.string.quick_past_confirm_message), color = SoftText) },
            confirmButton = { TextButton(onClick = { confirmPastQuickTask = false; saveQuickTask() }) { Text(stringResource(R.string.quick_past_confirm_save), color = Gold) } },
            dismissButton = { TextButton(onClick = { confirmPastQuickTask = false }) { Text(stringResource(R.string.quick_past_confirm_edit), color = SoftText) } }
        )
    }
    if (confirmQuickReminderAtStart) {
        AlertDialog(
            onDismissRequest = { confirmQuickReminderAtStart = false }, containerColor = CardBlue,
            title = { Text(stringResource(R.string.reminder_advance_passed_title), color = MainText) },
            text = { Text(stringResource(R.string.reminder_advance_passed_message), color = SoftText) },
            confirmButton = { TextButton(onClick = { confirmQuickReminderAtStart = false; saveQuickTask(0) }) { Text(stringResource(R.string.reminder_notify_at_start), color = Gold) } },
            dismissButton = { TextButton(onClick = { confirmQuickReminderAtStart = false }) { Text(stringResource(R.string.reminder_return_to_text), color = SoftText) } }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            HomeAccountDrawer(
                nickname, signedInAccount,
                onOpenActivities = { scope.launch { drawerState.close() }; onOpenActivities() },
                onOpenLists = { scope.launch { drawerState.close() }; onOpenLists() },
                onOpenCalendar = { scope.launch { drawerState.close() }; onOpenCalendar() },
                onOpenTimeLog = { scope.launch { drawerState.close() }; onOpenTimeLog() },
                onOpenSettings = { scope.launch { drawerState.close() }; onManageAccount() },
                onOpenPrivacy = { scope.launch { drawerState.close() }; onOpenPrivacy() },
                onClose = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 6.dp, bottom = if (keyboardVisible) 0.dp else 84.dp)) {
                HomeHeader(nickname, signedInAccount, selectedDate, isToday, { scope.launch { drawerState.open() } }, onOpenCalendar, dateFormatter)
                HomeWeekStrip(weekDays, selectedDate, currentDate, tasks) { selectedDateValue = it.toString() }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f).widthIn(max = 600.dp).imePadding().testTag("home_activity_list"),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item { QuickTaskField(quickText, quickDraft, quickNow, quickSaving, { value -> if (quickText != value) { quickText = value; quickId = java.util.UUID.randomUUID().toString() } }, ::submitQuickTask) }
                    if (shouldSuggestBackup(tasks.size, backupLastAt, backupSnoozedUntil, backupNow)) item { BackupReminderCard(onOpenBackup, onSnoozeBackup) }
                    item { DailySummaryCard(isToday, completedCount, selectedTasks.size, nextTask?.time) }
                    if (com.example.taskorganizer.ui.theme.OrganizerPalette.currentTheme == com.example.taskorganizer.ui.theme.OrganizerTheme.CARTA_DORATA) item { GoldDivider() }
                    item { HomeFilters(filter, selectedTasks, completedCount) { filter = it } }
                    item {
                        Column {
                            Text(if (selectedDate == currentDate) "Attività di oggi" else dateFormatter.format(selectedDate).replaceFirstChar { it.titlecase(Locale.ITALY) }, color = MainText, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = if (OrganizerPalette.currentTheme == OrganizerTheme.CARTA_DORATA) CartaHeadingFont else null)
                            Text("Scorri una card per completarla o eliminarla", color = SoftText, fontSize = 11.sp)
                        }
                    }
                    if (visibleTasks.isEmpty()) item { EmptyState("Nessuna attività in questa data", if (filter == "completed") "Non hai ancora completato attività." else "Puoi pianificare un nuovo impegno quando vuoi.") }
                    else items(visibleTasks, key = { it.id }) { task ->
                        HomeAgendaTaskCard(task, { onToggleTask(task.id) }, { onCompleteTask(task.id) }, { onDeleteTask(task.id); deletedId = task.id }, { onEdit(task) })
                    }
                }
            }
            val addShape = RoundedCornerShape(18.dp)
            if (!keyboardVisible) Box(
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 18.dp).height(52.dp)
                    .then(if (com.example.taskorganizer.ui.theme.OrganizerPalette.gradientTaskCards) Modifier.sunsetGlow(addShape, strong = true).sunsetPrimarySurface(addShape) else Modifier.background(Gold, addShape).sunsetGlow(addShape))
                    .clip(addShape).clickable(onClick = onAdd).semantics { contentDescription = "Crea una nuova attività"; role = Role.Button }
                    .padding(horizontal = 18.dp), contentAlignment = Alignment.Center
            ) { Row(verticalAlignment = Alignment.CenterVertically) { Text("+", color = if (com.example.taskorganizer.ui.theme.OrganizerPalette.sunsetGlow) Color.White else NightBlue, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("Nuova attività", Modifier.padding(start = 7.dp), color = if (com.example.taskorganizer.ui.theme.OrganizerPalette.sunsetGlow) Color.White else NightBlue, fontWeight = FontWeight.ExtraBold) } }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(start = 18.dp, end = 18.dp, bottom = 84.dp))
        }
    }
}

@Composable
private fun HomeHeader(nickname: String, account: SignedInAccount?, selectedDate: LocalDate, isToday: Boolean, onOpenDrawer: () -> Unit, onOpenCalendar: () -> Unit, formatter: DateTimeFormatter) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AccountAvatar(account?.photoUrl, account?.email?.firstOrNull()?.uppercase() ?: nickname.firstOrNull()?.uppercase() ?: "U", onOpenDrawer)
            Box(Modifier.size(15.dp).background(Gold, CircleShape).border(1.dp, CardBlue, CircleShape), contentAlignment = Alignment.Center) {
                Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_nav_menu), contentDescription = null, tint = NightBlue, modifier = Modifier.size(10.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(buildString { append(formatter.format(selectedDate).replaceFirstChar { it.titlecase(Locale.ITALY) }); if (isToday) append(" • Oggi") }, color = Gold, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Text(greetingFor(nickname), modifier = Modifier.heightIn(max = 120.dp).verticalScroll(rememberScrollState()), color = MainText, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = if (OrganizerPalette.currentTheme == OrganizerTheme.CARTA_DORATA) CartaHeadingFont else null)
        }
        val calendarShape = RoundedCornerShape(16.dp)
        Box(Modifier.size(43.dp).then(if (com.example.taskorganizer.ui.theme.OrganizerPalette.sunsetGlow) Modifier.sunsetGlow(calendarShape) else Modifier).clip(calendarShape).background(CardBlue.copy(alpha = .92f)).border(1.dp, Gold.copy(alpha = .24f), calendarShape).clickable(onClick = onOpenCalendar).semantics { contentDescription = "Apri calendario"; role = Role.Button }, contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_nav_calendar), null, tint = Gold)
        }
    }
}

@Composable
private fun HomeWeekStrip(weekDays: List<LocalDate>, selectedDate: LocalDate, currentDate: LocalDate, tasks: List<OrganizerTask>, onSelect: (LocalDate) -> Unit) {
    var dragDistance by remember { mutableStateOf(0f) }
    val threshold = with(LocalDensity.current) { 80.dp.toPx() }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).pointerInput(selectedDate) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { _, amount -> dragDistance += amount },
                onDragEnd = { when { dragDistance <= -threshold -> onSelect(selectedDate.plusWeeks(1)); dragDistance >= threshold -> onSelect(selectedDate.minusWeeks(1)) }; dragDistance = 0f },
                onDragCancel = { dragDistance = 0f }
            )
        }, horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        weekDays.forEach { date ->
            val selected = date == selectedDate
            val pendingCount = tasks.count { it.date == date.toString() && !it.completed }
            val completedCount = tasks.count { it.date == date.toString() && it.completed }
            val hasPending = pendingCount > 0
            val hasCompleted = completedCount > 0
            val dayShape = RoundedCornerShape(17.dp)
            Column(
                Modifier.weight(1f)
                    .then(if (com.example.taskorganizer.ui.theme.OrganizerPalette.sunsetGlow) Modifier.sunsetGlow(dayShape, strong = selected) else Modifier)
                    .then(if (selected && com.example.taskorganizer.ui.theme.OrganizerPalette.gradientTaskCards) Modifier.sunsetPrimarySurface(dayShape) else Modifier.background(if (selected) Gold else CardBlue.copy(alpha = .88f), dayShape))
                    .then(
                        if (com.example.taskorganizer.ui.theme.OrganizerPalette.sunsetGlow) Modifier.border(1.25.dp, sunsetPrimaryBrush(), dayShape)
                        else if (date == currentDate && !selected) Modifier.border(1.dp, Gold, dayShape)
                        else Modifier
                    )
                    .semantics {
                        contentDescription = "${date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ITALY)} ${date.dayOfMonth}: $pendingCount da fare, $completedCount completate"
                        role = Role.Button
                    }
                    .clickable { onSelect(date) }.padding(vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ITALY).removeSuffix(".").replaceFirstChar { it.titlecase(Locale.ITALY) }, color = if (selected && com.example.taskorganizer.ui.theme.OrganizerPalette.sunsetGlow) Color.White else if (selected) NightBlue else SoftText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(date.dayOfMonth.toString(), color = if (selected && com.example.taskorganizer.ui.theme.OrganizerPalette.sunsetGlow) Color.White else if (selected) NightBlue else MainText, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (hasPending) Box(Modifier.size(4.dp).background(if (selected && com.example.taskorganizer.ui.theme.OrganizerPalette.sunsetGlow) Color.White else if (selected) NightBlue else Gold, CircleShape))
                    if (hasCompleted) Box(Modifier.size(4.dp).background(if (selected) NightBlue else Success, CircleShape))
                }
            }
        }
    }
}

@Composable
private fun DailySummaryCard(isToday: Boolean, completed: Int, total: Int, nextTime: String?) {
    val progress = if (total == 0) 0f else completed.toFloat() / total
    val summaryShape = RoundedCornerShape(24.dp)
    Column(
        Modifier.fillMaxWidth().then(if (com.example.taskorganizer.ui.theme.OrganizerPalette.sunsetGlow) Modifier.sunsetGlow(summaryShape) else Modifier).background(CardBlue.copy(alpha = .92f), summaryShape)
            .then(if (com.example.taskorganizer.ui.theme.OrganizerPalette.sunsetGlow) Modifier.border(1.5.dp, sunsetPrimaryBrush(), summaryShape) else Modifier)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (isToday) "Oggi: $completed/$total completate" else "Completate: $completed/$total", color = MainText, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            Text(nextTime?.let { "Prossima: $it" } ?: "Nessuna in programma", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        if (com.example.taskorganizer.ui.theme.OrganizerPalette.currentTheme == com.example.taskorganizer.ui.theme.OrganizerTheme.CARTA_DORATA) {
            Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(Color(0xFFE6D8BD)).semantics { progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(progress, 0f..1f) }) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(7.dp).background(goldFoilBrush()))
            }
        } else {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = Gold, trackColor = CardBlueLight)
        }
    }
}

@Composable
private fun HomeFilters(filter: String, tasks: List<OrganizerTask>, completed: Int, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("all" to "Tutte (${tasks.size})", "scheduled" to "Da fare (${tasks.count { !it.completed }})", "alarm" to "Sveglie (${tasks.count { it.alarmEnabled }})", "completed" to "Completate ($completed)").forEach { (key, label) ->
            val selected = filter == key
            val shape = RoundedCornerShape(50)
            Text(label, color = if (selected && com.example.taskorganizer.ui.theme.OrganizerPalette.sunsetGlow) Color.White else if (selected) NightBlue else MainText, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = (if (selected) Modifier.sunsetPrimarySurface(shape).sunsetGlow(shape, strong = true) else Modifier.background(CardBlue.copy(alpha = .88f), shape).sunsetGlow(shape)).clickable { onChange(key) }.padding(horizontal = 14.dp, vertical = 9.dp))
        }
    }
}

@Composable
private fun HomeAccountDrawer(
    nickname: String,
    account: SignedInAccount?,
    onOpenActivities: () -> Unit,
    onOpenLists: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenTimeLog: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onClose: () -> Unit
) {
    var closeDragDistance by remember { mutableStateOf(0f) }
    val closeThreshold = with(LocalDensity.current) { 80.dp.toPx() }
    ModalDrawerSheet(
        drawerContainerColor = CardBlue,
        modifier = Modifier
            .widthIn(max = 304.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, amount -> closeDragDistance += amount },
                    onDragEnd = {
                        if (closeDragDistance <= -closeThreshold) onClose()
                        closeDragDistance = 0f
                    },
                    onDragCancel = { closeDragDistance = 0f }
                )
            }
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AccountAvatar(account?.photoUrl, account?.email?.firstOrNull()?.uppercase() ?: nickname.firstOrNull()?.uppercase() ?: "U", {})
                Column(Modifier.weight(1f)) {
                    Text(account?.displayName?.ifBlank { nickname } ?: nickname, color = MainText, fontWeight = FontWeight.ExtraBold)
                    Text(if (account == null) "Modalità ospite" else "Account collegato", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onClose) { Text("Chiudi", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
            HorizontalDivider(color = CardBlueLight, modifier = Modifier.padding(vertical = 10.dp))
            Text("Menu", color = SoftText, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 12.dp, bottom = 3.dp))
            HomeDrawerItem(R.drawable.ic_nav_activities, "Attività", onOpenActivities)
            HomeDrawerItem(R.drawable.ic_nav_lists, "Liste e spesa", onOpenLists)
            HomeDrawerItem(R.drawable.ic_nav_calendar, "Calendario", onOpenCalendar)
            HomeDrawerItem(R.drawable.ic_nav_time_log, "Registro ore", onOpenTimeLog)
            HomeDrawerItem(R.drawable.ic_nav_settings, "Impostazioni", onOpenSettings)
            HomeDrawerItem(R.drawable.ic_nav_privacy, "Privacy e dati", onOpenPrivacy)
        }
    }
}

@Composable
private fun HomeDrawerItem(icon: Int, label: String, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label, fontWeight = FontWeight.SemiBold) }, selected = false,
        icon = { Icon(androidx.compose.ui.res.painterResource(icon), contentDescription = null, tint = Gold) }, onClick = onClick, shape = RoundedCornerShape(15.dp),
        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = CardBlue, unselectedTextColor = MainText, unselectedIconColor = Gold)
    )
}
