package com.example.taskorganizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay
@Composable
internal fun TodayScreen(
    tasks: List<OrganizerTask>,
    nickname: String,
    signedInAccount: SignedInAccount?,
    onAdd: () -> Unit,
    onAddLog: (String, String, Int, LocalDate) -> Unit,
    onToggleTask: (String) -> Unit,
    onCompleteTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onRestoreTask: (String) -> Unit,
    onEdit: (OrganizerTask) -> Unit,
    onManageAccount: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    val deleteSnackbar = remember { SnackbarHostState() }
    var deletedTaskId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(deletedTaskId) {
        val taskId = deletedTaskId ?: return@LaunchedEffect
        if (
            deleteSnackbar.showSnackbar(
                message = "Attività spostata nel Cestino",
                actionLabel = "Annulla",
                duration = SnackbarDuration.Short
            ) == SnackbarResult.ActionPerformed
        ) {
            onRestoreTask(taskId)
        }
        deletedTaskId = null
    }
    var selectedDateValue by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var currentDate by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            val today = LocalDate.now()
            if (today != currentDate) {
                currentDate = today
                selectedDateValue = today.toString()
            }
        }
    }
    val selectedDate = LocalDate.parse(selectedDateValue)
    val weekStart = selectedDate.with(DayOfWeek.MONDAY)
    val weekDays = (0L..6L).map { weekStart.plusDays(it) }
    var agendaFilter by rememberSaveable { mutableStateOf("all") }
    var activeTimerTitle by rememberSaveable { mutableStateOf("Attività personale") }
    var activeTimerCategory by rememberSaveable { mutableStateOf("Personale") }
    var timerRunning by rememberSaveable { mutableStateOf(false) }
    var timerStartedAtMillis by rememberSaveable { mutableStateOf(0L) }
    var timerAccumulatedMillis by rememberSaveable { mutableStateOf(0L) }
    var timerNowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val timerElapsedMillis = timerAccumulatedMillis + if (timerRunning && timerStartedAtMillis > 0L) {
        (timerNowMillis - timerStartedAtMillis).coerceAtLeast(0L)
    } else 0L
    val timerSeconds = (timerElapsedMillis / 1_000L).toInt()
    val canSaveLiveLog = timerSeconds >= 60 && !selectedDate.isAfter(currentDate)
    LaunchedEffect(timerRunning, timerStartedAtMillis) {
        while (timerRunning) {
            timerNowMillis = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val selectedTasks = tasks.filter { it.date == selectedDateValue }.sortedBy { it.time ?: "99:99" }
    val visibleTasks = selectedTasks.filter { task ->
        when (agendaFilter) {
            "scheduled" -> !task.completed
            "logged" -> task.completed
            else -> true
        }
    }
    val loggedToday = selectedTasks.count { it.completed }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALY) }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALY) }
    var profileMenuOpen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
    PageColumn {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Task Organizer", color = Gold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            signedInAccount?.let { account ->
                Box(contentAlignment = Alignment.TopEnd) {
                    AccountAvatar(
                        photoUrl = account.photoUrl,
                        initial = account.email.firstOrNull()?.uppercase() ?: "U",
                        onClick = { profileMenuOpen = true }
                    )
                    DropdownMenu(
                        expanded = profileMenuOpen,
                        onDismissRequest = { profileMenuOpen = false },
                        containerColor = CardBlue,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                            Text(account.displayName, color = MainText, fontWeight = FontWeight.Bold)
                            Text(account.email, color = SoftText, fontSize = 12.sp)
                        }
                        HorizontalDivider(color = CardBlueLight)
                        DropdownMenuItem(
                            text = { Text("Gestisci account", color = MainText) },
                            onClick = { profileMenuOpen = false; onManageAccount() }
                        )
                        DropdownMenuItem(
                            text = { Text("Privacy e dati", color = MainText) },
                            onClick = { profileMenuOpen = false; onOpenPrivacy() }
                        )
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(monthFormatter.format(selectedDate).replaceFirstChar { it.titlecase(Locale.ITALY) }, color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { selectedDateValue = currentDate.toString() }) { Text("Oggi", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
            Text(greetingFor(nickname), color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Il tuo calendario e registro attività", color = SoftText, fontSize = 13.sp)
        }

        var weekDragDistance by remember { mutableStateOf(0f) }
        val weekSwipeThreshold = with(LocalDensity.current) { 80.dp.toPx() }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Scorri per cambiare settimana · tocca una data per vedere gli impegni", color = SoftText, fontSize = 12.sp)
            Row(
                modifier = Modifier.fillMaxWidth().pointerInput(selectedDateValue) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            weekDragDistance += dragAmount
                        },
                        onDragEnd = {
                            when {
                                weekDragDistance <= -weekSwipeThreshold -> selectedDateValue = selectedDate.plusWeeks(1).toString()
                                weekDragDistance >= weekSwipeThreshold -> selectedDateValue = selectedDate.minusWeeks(1).toString()
                            }
                            weekDragDistance = 0f
                        },
                        onDragCancel = { weekDragDistance = 0f }
                    )
                },
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                weekDays.forEach { date ->
                    val isSelected = date == selectedDate
                    val isToday = date == currentDate
                    val hasScheduled = tasks.any { it.date == date.toString() && !it.completed }
                    val hasLogged = tasks.any { it.date == date.toString() && it.completed }
                    Column(
                        modifier = Modifier.weight(1f).background(if (isSelected) Gold else CardBlue, RoundedCornerShape(18.dp))
                            .then(if (isToday && !isSelected) Modifier.border(1.dp, Gold, RoundedCornerShape(18.dp)) else Modifier)
                            .clickable { selectedDateValue = date.toString() }.padding(vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ITALY).removeSuffix(".").replaceFirstChar { it.titlecase(Locale.ITALY) }, color = if (isSelected) NightBlue else SoftText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(date.dayOfMonth.toString(), color = if (isSelected) NightBlue else MainText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (hasScheduled) Box(Modifier.size(5.dp).background(if (isSelected) NightBlue else Gold, RoundedCornerShape(50)))
                            if (hasLogged) Box(Modifier.size(5.dp).background(if (isSelected) NightBlue else Success, RoundedCornerShape(50)))
                        }
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = CardBlueLight), shape = RoundedCornerShape(22.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Registra attività live", color = MainText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(activeTimerTitle, color = SoftText, fontSize = 12.sp)
                    }
                    Text(formatLiveTimer(timerSeconds), color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        Triple("💼 Lavoro", "Lavoro", "Lavoro"),
                        Triple("👤 Personale", "Personale", "Personale"),
                        Triple("📚 Studio", "Studio", "Studio")
                    ).forEach { (label, title, category) ->
                        TextButton(onClick = { activeTimerTitle = title; activeTimerCategory = category }) { Text(label, color = SoftText, fontSize = 12.sp) }
                    }
                }
                if (selectedDate.isAfter(currentDate)) {
                    Text("Puoi registrare un'attività solo per oggi o per una data passata.", color = Danger, fontSize = 12.sp)
                } else if (timerSeconds < 60) {
                    Text("Registra almeno un minuto di attività.", color = SoftText, fontSize = 12.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        if (timerRunning) {
                            timerAccumulatedMillis += (System.currentTimeMillis() - timerStartedAtMillis).coerceAtLeast(0L)
                            timerStartedAtMillis = 0L
                            timerRunning = false
                        } else {
                            timerStartedAtMillis = System.currentTimeMillis()
                            timerNowMillis = timerStartedAtMillis
                            timerRunning = true
                        }
                    }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Text(if (timerRunning) "Pausa" else "Avvia", color = Gold, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            onAddLog(activeTimerTitle, activeTimerCategory, timerSeconds / 60, selectedDate)
                            timerRunning = false
                            timerStartedAtMillis = 0L
                            timerAccumulatedMillis = 0L
                            timerNowMillis = System.currentTimeMillis()
                        },
                        enabled = canSaveLiveLog,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)
                    ) { Text("Salva", fontWeight = FontWeight.Bold) }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("all" to "Tutto (${selectedTasks.size})", "scheduled" to "📅 Programmate", "logged" to "✓ Completate").forEach { (key, label) ->
                val active = agendaFilter == key
                TextButton(
                    onClick = { agendaFilter = key },
                    modifier = Modifier.weight(1f).background(if (active) CardBlueLight else CardBlue, RoundedCornerShape(16.dp))
                ) { Text(label, color = if (active) Gold else SoftText, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
            }
        }

        Text(dateFormatter.format(selectedDate).replaceFirstChar { it.titlecase(Locale.ITALY) }, color = MainText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        if (visibleTasks.isEmpty()) {
            EmptyState("Nessuna attività in questa data", if (agendaFilter == "logged") "Non hai ancora registrato attività." else "Puoi pianificare un nuovo impegno quando vuoi.")
        } else {
            visibleTasks.forEach { task ->
                key(task.id) {
                    HomeAgendaTaskCard(
                        task = task,
                        onToggle = { onToggleTask(task.id) },
                        onComplete = { onCompleteTask(task.id) },
                        onDelete = { onDeleteTask(task.id); deletedTaskId = task.id },
                        onEdit = { onEdit(task) }
                    )
                }
            }
        }
        Button(
            onClick = onAdd, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)
        ) { Text("+  Registra / Pianifica", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
        SnackbarHost(hostState = deleteSnackbar, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }
}

private fun formatLiveTimer(seconds: Int): String {
    val hours = seconds / 3_600
    val minutes = (seconds % 3_600) / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d:%02d".format(Locale.ROOT, hours, minutes, remainingSeconds)
}

