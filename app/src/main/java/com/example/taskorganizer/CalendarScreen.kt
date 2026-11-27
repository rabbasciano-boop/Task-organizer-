package com.example.taskorganizer

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskorganizer.ui.theme.OrganizerPalette
import com.example.taskorganizer.ui.theme.OrganizerTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
@Composable
fun CalendarScreen(
    tasks: List<OrganizerTask>,
    onAdd: () -> Unit,
    onToggle: (String) -> Unit,
    onEdit: (OrganizerTask) -> Unit
) {
    if (OrganizerPalette.currentTheme == OrganizerTheme.CARTA_DORATA) {
        CartaDorataCalendar(tasks, onAdd, onToggle, onEdit)
        return
    }
    var displayedMonthValue by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var selectedDateValue by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val displayedMonth = YearMonth.parse(displayedMonthValue)
    val selectedDate = LocalDate.parse(selectedDateValue)
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALY) }
    val firstDayOffset = displayedMonth.atDay(1).dayOfWeek.value - 1
    val daysInMonth = displayedMonth.lengthOfMonth()
    val gridCells = (0 until 42).toList()
    val selectedTasks by remember(selectedDateValue) {
        derivedStateOf {
            tasks.filter { it.date == selectedDateValue }.sortedBy { it.time ?: "99:99" }
        }
    }
    val previousMonthDescription = stringResource(R.string.previous_month)
    val nextMonthDescription = stringResource(R.string.next_month)

    LazyPageColumn {
        item { Text("Calendario", color = MainText, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    onClick = { displayedMonthValue = displayedMonth.minusMonths(1).toString() },
                    modifier = Modifier.size(48.dp).semantics { contentDescription = previousMonthDescription }
                ) { Icon(painterResource(R.drawable.ic_arrow_left), contentDescription = null, tint = Gold) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(displayedMonth.format(monthFormatter).replaceFirstChar { it.titlecase(Locale.ITALY) }, color = Gold, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { displayedMonthValue = YearMonth.now().toString(); selectedDateValue = LocalDate.now().toString() }) { Text("Oggi", color = SoftText, fontSize = 12.sp) }
                }
                TextButton(
                    onClick = { displayedMonthValue = displayedMonth.plusMonths(1).toString() },
                    modifier = Modifier.size(48.dp).semantics { contentDescription = nextMonthDescription }
                ) { Icon(painterResource(R.drawable.ic_arrow_right), contentDescription = null, tint = Gold) }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("L", "M", "M", "G", "V", "S", "D").forEach { day ->
                            Text(day, modifier = Modifier.weight(1f), color = Gold, fontSize = 12.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        }
                    }
                    gridCells.chunked(7).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            week.forEach { cell ->
                                val dayNumber = cell - firstDayOffset + 1
                                val date = if (dayNumber in 1..daysInMonth) displayedMonth.atDay(dayNumber) else null
                                val hasTasks = date != null && tasks.any { it.date == date.toString() }
                                val isSelected = date == selectedDate
                                CalendarDayCell(date, hasTasks, isSelected) { clickedDate -> selectedDateValue = clickedDate.toString() }
                            }
                        }
                    }
                }
            }
        }
        item { Text(formatItalianDate(selectedDate.toString()), color = MainText, fontSize = 19.sp, fontWeight = FontWeight.Bold) }
        if (selectedTasks.isEmpty()) {
            item { EmptyState("Nessun impegno", "Non hai attività previste per questo giorno.") }
        } else {
            items(selectedTasks, key = { it.id }) { task ->
                CompactTaskCard(task, onToggle = { onToggle(task.id) }, onEdit = { onEdit(task) })
            }
        }
        item {
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Text("+ Nuova attività", color = Gold, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RowScope.CalendarDayCell(date: LocalDate?, hasTasks: Boolean, isSelected: Boolean, onSelected: (LocalDate) -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier.weight(1f).height(48.dp).then(
            if (isSelected && OrganizerPalette.agendaReale) Modifier.sunsetPrimarySurface(shape)
            else Modifier.background(
                color = when {
                    isSelected -> Gold
                    date == LocalDate.now() -> CardBlueLight
                    else -> Color.Transparent
                },
                shape = shape
            )
        ).then(if (date != null) Modifier.clickable { onSelected(date) } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(date.dayOfMonth.toString(), color = if (isSelected) NightBlue else MainText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (hasTasks) Text("•", color = if (isSelected) NightBlue else Gold, fontSize = 16.sp, lineHeight = 12.sp)
            }
        }
    }
}
