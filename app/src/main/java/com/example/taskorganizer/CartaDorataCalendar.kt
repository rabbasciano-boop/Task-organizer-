package com.example.taskorganizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.taskorganizer.ui.theme.CartaHeadingFont
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

/** Calendar layout based on the Carta Dorata concept, using real task data. */
@Composable
internal fun CartaDorataCalendar(tasks: List<OrganizerTask>, onAdd: () -> Unit, onToggle: (String) -> Unit, onEdit: (OrganizerTask) -> Unit) {
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var selectedText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var todayDate by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            todayDate = LocalDate.now()
            delay(30_000L)
        }
    }
    val month = YearMonth.parse(monthText)
    val selected = LocalDate.parse(selectedText)
    val selectedTasks = tasks.filter { it.date == selectedText }.sortedBy { it.time ?: "99:99" }
    val datesWithTasks = tasks.mapTo(mutableSetOf()) { it.date }
    val firstOffset = month.atDay(1).dayOfWeek.value - 1
    val weeks = (firstOffset + month.lengthOfMonth() + 6) / 7
    val previous = stringResource(R.string.previous_month)
    val next = stringResource(R.string.next_month)
    val heading = remember(month) { month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALY)).replaceFirstChar { it.titlecase(Locale.ITALY) } }
    val selectedHeading = remember(selected) { selected.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALY)).replaceFirstChar { it.titlecase(Locale.ITALY) } }

    LazyPageColumn {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("CALENDARIO", color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(heading, modifier = Modifier.weight(1f), color = Gold, fontFamily = CartaHeadingFont, fontSize = 36.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { monthText = month.minusMonths(1).toString() }, modifier = Modifier.semantics { contentDescription = previous }) { Icon(painterResource(R.drawable.ic_arrow_left), null, tint = Gold) }
                    IconButton(onClick = { monthText = month.plusMonths(1).toString() }, modifier = Modifier.semantics { contentDescription = next }) { Icon(painterResource(R.drawable.ic_arrow_right), null, tint = Gold) }
                }
                TextButton(onClick = { monthText = YearMonth.now().toString(); selectedText = LocalDate.now().toString() }) { Text("Oggi", color = Gold) }
                GoldDivider()
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    listOf("L", "M", "M", "G", "V", "S", "D").forEach { Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                }
                repeat(weeks) { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(7) { day ->
                            val n = week * 7 + day - firstOffset + 1
                            val date = if (n in 1..month.lengthOfMonth()) month.atDay(n) else null
                            if (date == null) Spacer(Modifier.weight(1f).height(46.dp)) else {
                                val selectedDay = date == selected
                                val today = date == todayDate
                                val dayShape = RoundedCornerShape(10.dp)
                                val description = date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ITALY)) + if (date.toString() in datesWithTasks) ", con attività" else ", nessuna attività"
                                Column(
                                    Modifier.weight(1f).heightIn(min = 46.dp)
                                        .background(if (selectedDay) Color(0xFF805C1D) else if (today) Color(0xFFF0E4D0) else Color.Transparent, dayShape)
                                        .then(if (today && !selectedDay) Modifier.border(1.dp, Color(0xFFB88B35), dayShape) else Modifier)
                                        .clickable { selectedText = date.toString() }.semantics { contentDescription = description },
                                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                                ) {
                                    Text(n.toString(), color = if (selectedDay) Color(0xFFFFFDF8) else MainText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                    if (date.toString() in datesWithTasks) Box(Modifier.size(4.dp).background(if (selectedDay) Color(0xFFFFFDF8) else Gold, CircleShape))
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardBlue), border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .28f))) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedHeading, modifier = Modifier.weight(1f), fontFamily = CartaHeadingFont, fontSize = 25.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold, color = MainText)
                        Text("${selectedTasks.size} attività", fontSize = 12.sp, color = SoftText)
                    }
                    HorizontalDivider(color = Gold.copy(alpha = .32f))
                    if (selectedTasks.isEmpty()) Text("Nessuna attività in programma. Scegli un altro giorno o aggiungine una.", color = SoftText, fontSize = 14.sp)
                    selectedTasks.forEach { task ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(task.time ?: "—", color = Gold, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(task.title, modifier = Modifier.weight(1f).clickable { onEdit(task) }, color = if (task.completed) SoftText else MainText, fontSize = 15.sp)
                            IconButton(onClick = { onToggle(task.id) }, modifier = Modifier.semantics { contentDescription = if (task.completed) "Riapri ${task.title}" else "Completa ${task.title}" }) {
                                Text(if (task.completed) "✓" else "○", color = if (task.completed) Gold else SoftText, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }
        }
        item { OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) { Text("+ Nuova attività", color = Gold, fontWeight = FontWeight.Bold) } }
    }
}
