package com.example.taskorganizer

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskorganizer.ui.theme.OrganizerPalette
import com.example.taskorganizer.ui.theme.OrganizerTheme
@Composable
fun CompactTaskCard(task: OrganizerTask, onToggle: () -> Unit, onEdit: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val useSunsetGradient = OrganizerPalette.gradientTaskCards
    Card(modifier = if (OrganizerPalette.sunsetGlow) Modifier.sunsetGlow(shape).sunsetTaskSurface(task.cardColor, shape) else Modifier.then(if (useSunsetGradient) Modifier.sunsetTaskSurface(task.cardColor, shape) else Modifier).sunsetGlow(shape), colors = CardDefaults.cardColors(containerColor = if (useSunsetGradient) Color.Transparent else CardBlueLight), shape = shape) {
        Row(modifier = Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clickable(onClick = onToggle).semantics {
                    contentDescription = if (task.completed) "Segna ${task.title} come da fare" else "Segna ${task.title} come completata"
                    role = Role.Button
                },
                contentAlignment = Alignment.Center
            ) {
                TaskCompletionMark(task)
            }
            Spacer(Modifier.size(4.dp))
            Column(modifier = Modifier.weight(1f).clickable(onClick = onEdit)) {
                Text(task.title, color = if (useSunsetGradient) taskCardTextColor(task) else if (task.completed) SoftText else MainText, fontWeight = FontWeight.Bold)
                Text(listOfNotNull(formatTimeRange(task), categoryDisplay(task.category, task.categoryIcon), subtaskCount(task.subtasks)).joinToString(" · "), color = if (useSunsetGradient) taskCardMutedColor(task) else SoftText, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun TaskCard(task: OrganizerTask, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val titleColor = taskCardTextColor(task)
    val mutedColor = taskCardMutedColor(task)
    val shape = RoundedCornerShape(20.dp)
    val useSunsetGradient = OrganizerPalette.gradientTaskCards
    val cardActionColor = when {
        OrganizerPalette.agendaReale -> Color(0xFF240833)
        OrganizerPalette.sunsetGlow -> Color.White
        else -> null
    }
    Card(modifier = if (OrganizerPalette.sunsetGlow) Modifier.sunsetGlow(shape, strong = true).sunsetTaskSurface(task.cardColor, shape) else Modifier.then(if (useSunsetGradient) Modifier.sunsetTaskSurface(task.cardColor, shape) else Modifier).sunsetGlow(shape, strong = useSunsetGradient), colors = CardDefaults.cardColors(containerColor = if (useSunsetGradient) Color.Transparent else taskCardBackground(task.cardColor)), shape = shape) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clickable(onClick = onToggle).semantics {
                        contentDescription = if (task.completed) "Segna ${task.title} come da fare" else "Segna ${task.title} come completata"
                        role = Role.Button
                    },
                    contentAlignment = Alignment.Center
                ) {
                    TaskCompletionMark(task)
                }
                Spacer(Modifier.size(2.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.title, color = if (task.completed) mutedColor else titleColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("${formatItalianDate(task.date)}${formatTimeRange(task)?.let { " · $it" } ?: ""}", color = mutedColor, fontSize = 13.sp)
                }
            }
            Text(listOfNotNull(categoryDisplay(task.category, task.categoryIcon), subtaskCount(task.subtasks), "priorità ${task.priority.lowercase()}").joinToString(" · "), color = cardActionColor ?: Gold, fontSize = 13.sp)
            task.examStatus?.let { Text("Esame: $it", color = cardActionColor ?: Gold, fontSize = 13.sp) }
            if (task.note.isNotBlank()) Text(task.note, color = mutedColor, fontSize = 14.sp)
            HorizontalDivider(color = if (task.cardColor == DefaultCardColor) CardBlueLight else mutedColor.copy(alpha = 0.25f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onToggle) { Text(if (task.completed) "Riapri" else "Completa", color = cardActionColor ?: Success) }
                TextButton(onClick = onEdit) { Text("Modifica", color = cardActionColor ?: Gold) }
                TextButton(onClick = onDelete) { Text("Elimina", color = cardActionColor ?: Danger) }
            }
        }
    }
}

@Composable
private fun TaskCompletionMark(task: OrganizerTask) {
    if (task.completed && OrganizerPalette.currentTheme == OrganizerTheme.CARTA_DORATA) {
        Box(Modifier.size(28.dp).background(goldFoilBrush(), CircleShape).border(1.5.dp, Color(0xFF8F6A22), CircleShape), contentAlignment = Alignment.Center) {
            Text("✓", color = NightBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        Text(if (task.completed) "✓" else "○", color = taskCompletionColor(task), fontSize = 24.sp)
    }
}
