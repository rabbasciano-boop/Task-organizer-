package com.example.taskorganizer

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.clickable
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
@Composable
fun CompactTaskCard(task: OrganizerTask, onToggle: () -> Unit, onEdit: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    val useSunsetGradient = OrganizerPalette.sunsetGlow
    Card(modifier = Modifier.then(if (useSunsetGradient) Modifier.sunsetPrimarySurface(shape) else Modifier).sunsetGlow(shape), colors = CardDefaults.cardColors(containerColor = if (useSunsetGradient) Color.Transparent else CardBlueLight), shape = shape) {
        Row(modifier = Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clickable(onClick = onToggle).semantics {
                    contentDescription = if (task.completed) "Segna ${task.title} come da fare" else "Segna ${task.title} come completata"
                    role = Role.Button
                },
                contentAlignment = Alignment.Center
            ) {
                Text(if (task.completed) "✓" else "○", color = taskCompletionColor(task), fontSize = 24.sp)
            }
            Spacer(Modifier.size(4.dp))
            Column(modifier = Modifier.weight(1f).clickable(onClick = onEdit)) {
                Text(task.title, color = if (task.completed) SoftText else MainText, fontWeight = FontWeight.Bold)
                Text(listOfNotNull(formatTimeRange(task), categoryDisplay(task.category, task.categoryIcon)).joinToString(" · "), color = SoftText, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun TaskCard(task: OrganizerTask, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val titleColor = taskCardTextColor(task)
    val mutedColor = taskCardMutedColor(task)
    val shape = RoundedCornerShape(20.dp)
    val useSunsetGradient = task.cardColor == DefaultCardColor && OrganizerPalette.sunsetGlow
    Card(modifier = Modifier.then(if (useSunsetGradient) Modifier.sunsetPrimarySurface(shape) else Modifier).sunsetGlow(shape, strong = task.cardColor == DefaultCardColor), colors = CardDefaults.cardColors(containerColor = if (useSunsetGradient) Color.Transparent else taskCardBackground(task.cardColor)), shape = shape) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clickable(onClick = onToggle).semantics {
                        contentDescription = if (task.completed) "Segna ${task.title} come da fare" else "Segna ${task.title} come completata"
                        role = Role.Button
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (task.completed) "✓" else "○", color = taskCompletionColor(task), fontSize = 25.sp)
                }
                Spacer(Modifier.size(2.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.title, color = if (task.completed) mutedColor else titleColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("${formatItalianDate(task.date)}${formatTimeRange(task)?.let { " · $it" } ?: ""}", color = mutedColor, fontSize = 13.sp)
                }
            }
            Text("${categoryDisplay(task.category, task.categoryIcon)} · priorità ${task.priority.lowercase()}", color = Gold, fontSize = 13.sp)
            task.examStatus?.let { Text("Esame: $it", color = Gold, fontSize = 13.sp) }
            if (task.note.isNotBlank()) Text(task.note, color = mutedColor, fontSize = 14.sp)
            HorizontalDivider(color = if (task.cardColor == DefaultCardColor) CardBlueLight else mutedColor.copy(alpha = 0.25f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onToggle) { Text(if (task.completed) "Riapri" else "Completa", color = Success) }
                TextButton(onClick = onEdit) { Text("Modifica", color = Gold) }
                TextButton(onClick = onDelete) { Text("Elimina", color = Danger) }
            }
        }
    }
}
