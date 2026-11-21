package com.example.taskorganizer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class KanbanColumn(val id: String, val label: String, val icon: String)

private val KanbanColumns = listOf(
    KanbanColumn("todo", "Da fare", "📥"),
    KanbanColumn("shopping", "Spesa", "🛒"),
    KanbanColumn("doing", "In corso", "⚡"),
    KanbanColumn("done", "Fatto", "✓")
)
private val KanbanStageIds = KanbanColumns.mapTo(mutableSetOf()) { it.id }

private fun kanbanStageOf(item: OrganizerListItem): String = when {
    item.completed -> "done"
    item.kanbanStage in KanbanStageIds -> item.kanbanStage
    item.listType == "shopping" -> "shopping"
    else -> "todo"
}

@Composable
fun KanbanListsScreen(
    items: List<OrganizerListItem>,
    onAdd: (OrganizerListItem) -> Unit,
    onChange: (OrganizerListItem) -> Unit,
    onDelete: (OrganizerListItem) -> Unit
) {
    var activeColumn by rememberSaveable { mutableStateOf("todo") }
    var title by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }
    var deleteCandidate by remember { mutableStateOf<OrganizerListItem?>(null) }
    val active = KanbanColumns.first { it.id == activeColumn }
    val visibleItems = items.filter { kanbanStageOf(it) == activeColumn }.sortedByDescending { it.createdAt }

    PageColumn {
        Text("Liste e spesa", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KanbanColumns.forEach { column ->
                val selected = column.id == activeColumn
                val count = items.count { kanbanStageOf(it) == column.id }
                TextButton(
                    onClick = { activeColumn = column.id; error = "" },
                    modifier = Modifier.weight(1f).background(if (selected) Gold else CardBlue, RoundedCornerShape(14.dp)).padding(vertical = 4.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(column.icon, fontSize = 14.sp)
                        Text(column.label, color = if (selected) NightBlue else SoftText, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(count.toString(), color = if (selected) NightBlue else MainText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Aggiungi in ${active.icon} ${active.label}", color = MainText, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text(if (activeColumn == "shopping") "Cosa devi comprare?" else "Scrivi una nuova voce...") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = compactEditorFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (activeColumn == "shopping") {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        placeholder = { Text("Spesa prevista · facoltativa") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(18.dp),
                        colors = compactEditorFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (error.isNotBlank()) Text(error, color = Danger, fontSize = 12.sp)
                Button(
                    onClick = {
                        val cents = if (activeColumn == "shopping") parseAmountCents(amount) else null
                        error = when {
                            title.isBlank() -> "Scrivi prima una voce."
                            activeColumn == "shopping" && amount.isNotBlank() && cents == null -> "Inserisci un importo valido, ad esempio 3,50."
                            else -> ""
                        }
                        if (error.isBlank()) {
                            onAdd(
                                OrganizerListItem(
                                    title = title.trim(),
                                    listType = if (activeColumn == "shopping") "shopping" else "todo",
                                    amountCents = cents,
                                    completed = activeColumn == "done",
                                    kanbanStage = activeColumn
                                )
                            )
                            title = ""
                            amount = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = NightBlue)
                ) { Text("+ Card", fontWeight = FontWeight.Bold) }
            }
        }

        if (visibleItems.isEmpty()) {
            EmptyState("Nessuna card in questa corsia", "Aggiungi una voce oppure sposta qui una card esistente.")
        } else {
            visibleItems.forEach { item ->
                key(item.id) {
                    Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(
                                        if (item.listType == "shopping") "🛒 Spesa" else "📌 Lista",
                                        color = if (item.listType == "shopping") Gold else SoftText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(item.title, color = if (item.completed) SoftText else MainText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    item.amountCents?.let { Text(formatCurrency(it), color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                }
                                TextButton(onClick = { deleteCandidate = item }) { Text("×", color = Danger, fontSize = 20.sp) }
                            }
                            HorizontalDivider(color = CardBlueLight)
                            Text("Sposta in:", color = SoftText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                KanbanColumns.filter { it.id != activeColumn }.forEach { target ->
                                    TextButton(
                                        onClick = {
                                            onChange(
                                                item.copy(
                                                    kanbanStage = target.id,
                                                    listType = when (target.id) {
                                                        "shopping" -> "shopping"
                                                        "todo" -> "todo"
                                                        else -> item.listType
                                                    },
                                                    completed = target.id == "done"
                                                )
                                            )
                                        },
                                        modifier = Modifier.weight(1f).background(CardBlueLight, RoundedCornerShape(10.dp))
                                    ) { Text("${target.icon} ${target.label}", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    deleteCandidate?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            containerColor = CardBlue,
            title = { Text("Eliminare elemento?", color = MainText) },
            text = { Text("Vuoi eliminare “${item.title}”? L'operazione non può essere annullata.", color = SoftText) },
            confirmButton = { TextButton(onClick = { onDelete(item); deleteCandidate = null }) { Text("Elimina", color = Danger) } },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Annulla", color = SoftText) } }
        )
    }
}
