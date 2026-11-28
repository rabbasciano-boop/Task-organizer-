package com.example.taskorganizer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
@Composable
fun ActivitiesScreen(
    tasks: List<OrganizerTask>,
    deletedTasks: List<OrganizerTask>,
    onAdd: () -> Unit,
    onToggle: (String) -> Unit,
    onEdit: (OrganizerTask) -> Unit,
    onDelete: (String) -> Unit,
    onRestore: (String) -> Unit,
    onPermanentlyDelete: (String) -> Unit,
    onRestoreMany: (Set<String>) -> Unit,
    onPermanentlyDeleteMany: (Set<String>) -> Unit
) {
    var selectedMonth by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("") }
    var selectedStatus by rememberSaveable { mutableStateOf("all") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filterOpen by rememberSaveable { mutableStateOf<String?>(null) }
    var taskToDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val taskToDelete = taskToDeleteId?.let { id -> tasks.firstOrNull { it.id == id } }
    var trashOpen by rememberSaveable { mutableStateOf(false) }
    val monthOptions = listOf("" to "Tutti i mesi") + tasks.map { it.date.take(7) }.distinct().sorted().map { it to formatMonthKey(it) }
    val categoryOptions = listOf("" to "Tutte le categorie") + tasks.map { it.category to it.categoryIcon }
        .distinctBy { it.first }
        .sortedBy { it.first }
        .map { (category, icon) -> category to categoryDisplay(category, icon) }
    val statusOptions = listOf("all" to "Tutte", "todo" to "Da fare", "done" to "Completate")
    val sortedTasks = tasks.filter { task ->
                (selectedMonth.isBlank() || task.date.startsWith(selectedMonth)) &&
                    (selectedCategory.isBlank() || task.category == selectedCategory) &&
                    taskMatchesQuery(task, searchQuery) &&
                    when (selectedStatus) {
                        "todo" -> !task.completed
                        "done" -> task.completed
                        else -> true
                    }
            }.sortedWith(compareBy<OrganizerTask> { it.completed }.thenBy { it.date }.thenBy { it.time ?: "99:99" })
    val completed = tasks.count { it.completed }

    LazyPageColumn {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Attività", color = MainText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Tutto quello che devi fare, in un unico posto.", color = SoftText)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(24.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    Stat("${tasks.size}", "totali")
                    Stat("$completed", "completate")
                    Stat("${tasks.size - completed}", "da fare")
                }
            }
        }
        item { ActivitySearchField(searchQuery) { searchQuery = it } }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Filtra attività", color = MainText, fontWeight = FontWeight.Bold)
                    Text("Tocca una voce per scegliere", color = SoftText, fontSize = 12.sp)
                    FilterChoice("Mese", monthOptions.firstOrNull { it.first == selectedMonth }?.second ?: "Tutti i mesi") { filterOpen = "month" }
                    FilterChoice("Categoria", categoryOptions.firstOrNull { it.first == selectedCategory }?.second ?: "Tutte le categorie") { filterOpen = "category" }
                    FilterChoice("Stato", statusOptions.firstOrNull { it.first == selectedStatus }?.second ?: "Tutte") { filterOpen = "status" }
                }
            }
        }
        item { Text("${sortedTasks.size} attività mostrate", color = SoftText, fontSize = 13.sp) }
        if (sortedTasks.isEmpty()) {
            item {
                if (searchQuery.isNotBlank()) {
                    EmptyState(stringResource(R.string.activity_search_no_results, searchQuery.trim()), stringResource(R.string.activity_search_no_results_hint))
                } else {
                    EmptyState("Nessuna attività trovata", "Prova a cambiare i filtri oppure aggiungi una nuova attività.")
                }
            }
        } else {
            items(sortedTasks, key = { it.id }) { task ->
                TaskCard(task, onToggle = { onToggle(task.id) }, onEdit = { onEdit(task) }, onDelete = { taskToDeleteId = task.id })
            }
        }
        item {
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Text("+ Nuova attività", color = Gold, fontWeight = FontWeight.Bold)
            }
        }
        if (deletedTasks.isNotEmpty()) {
            item {
                OutlinedButton(onClick = { trashOpen = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text("Cestino (${deletedTasks.size})", color = Gold, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
    filterOpen?.let { filter ->
        val title = when (filter) {
            "month" -> "Filtra per mese"
            "category" -> "Filtra per categoria"
            else -> "Filtra per stato"
        }
        val options = when (filter) {
            "month" -> monthOptions
            "category" -> categoryOptions
            else -> statusOptions
        }
        AlertDialog(
            onDismissRequest = { filterOpen = null },
            containerColor = CardBlue,
            title = { Text(title, color = MainText) },
            text = {
                Column {
                    options.forEach { (value, label) ->
                        Text(label, color = MainText, modifier = Modifier.fillMaxWidth().clickable {
                            when (filter) {
                                "month" -> selectedMonth = value
                                "category" -> selectedCategory = value
                                else -> selectedStatus = value
                            }
                            filterOpen = null
                        }.padding(vertical = 12.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { filterOpen = null }) { Text("Chiudi", color = Gold) } }
        )
    }
    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDeleteId = null },
            containerColor = CardBlue,
            title = { Text("Eliminare attività?", color = MainText) },
            text = { Text("Vuoi spostare “${task.title}” nel Cestino? Potrai ripristinarla in seguito.", color = SoftText) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(task.id)
                    taskToDeleteId = null
                }) { Text("Elimina", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { taskToDeleteId = null }) { Text("Annulla", color = SoftText) } }
        )
    }
    if (trashOpen) {
        TrashDialog(
            tasks = deletedTasks.sortedByDescending { it.createdAt },
            onDismiss = { trashOpen = false },
            onRestore = onRestore,
            onPermanentlyDelete = onPermanentlyDelete,
            onRestoreMany = onRestoreMany,
            onPermanentlyDeleteMany = onPermanentlyDeleteMany
        )
    }
}

@Composable
private fun TrashDialog(
    tasks: List<OrganizerTask>,
    onDismiss: () -> Unit,
    onRestore: (String) -> Unit,
    onPermanentlyDelete: (String) -> Unit,
    onRestoreMany: (Set<String>) -> Unit,
    onPermanentlyDeleteMany: (Set<String>) -> Unit
) {
    var permanentDeleteTask by remember { mutableStateOf<OrganizerTask?>(null) }
    var deleteSelectionConfirmation by remember { mutableStateOf<Set<String>?>(null) }
    var selectedTaskIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val currentIds = tasks.mapTo(mutableSetOf()) { it.id }
    val effectiveSelection = selectedTaskIds.intersect(currentIds)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBlue,
        title = { Text("Cestino", color = MainText) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Le attività nel Cestino possono essere ripristinate per 30 giorni.", color = SoftText, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { selectedTaskIds = if (effectiveSelection.size == currentIds.size) emptySet() else currentIds },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(if (effectiveSelection.size == currentIds.size && currentIds.isNotEmpty()) "Deseleziona" else "Seleziona tutto", color = Gold, fontSize = 12.sp) }
                    if (effectiveSelection.isEmpty()) {
                        OutlinedButton(onClick = { deleteSelectionConfirmation = currentIds }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Text("Elimina tutto", color = Danger, fontSize = 12.sp)
                        }
                    }
                }
                if (effectiveSelection.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onRestoreMany(effectiveSelection); selectedTaskIds = emptySet() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Text("Ripristina selezionate (${effectiveSelection.size})", color = Success, fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = { deleteSelectionConfirmation = effectiveSelection }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Text("Elimina selezionate (${effectiveSelection.size})", color = Danger, fontSize = 12.sp)
                        }
                    }
                }
                tasks.forEach { task ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (task.id in selectedTaskIds) Gold.copy(alpha = 0.18f) else CardBlueLight),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.clickable { selectedTaskIds = if (task.id in selectedTaskIds) selectedTaskIds - task.id else selectedTaskIds + task.id }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Checkbox(checked = task.id in selectedTaskIds, onCheckedChange = { checked -> selectedTaskIds = if (checked) selectedTaskIds + task.id else selectedTaskIds - task.id })
                                Text(task.title, color = MainText, fontWeight = FontWeight.Bold)
                            }
                            Text("${formatItalianDate(task.date)}${formatTimeRange(task)?.let { " · $it" } ?: ""}", color = SoftText, fontSize = 12.sp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                TextButton(onClick = { onRestore(task.id) }) { Text("Ripristina", color = Success) }
                                TextButton(onClick = { permanentDeleteTask = task }) { Text("Elimina", color = Danger) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi", color = Gold) } }
    )
    permanentDeleteTask?.let { task ->
        AlertDialog(
            onDismissRequest = { permanentDeleteTask = null },
            containerColor = CardBlue,
            title = { Text("Eliminare definitivamente?", color = MainText) },
            text = { Text("“${task.title}” non potrà più essere recuperata dal Cestino.", color = SoftText) },
            confirmButton = { TextButton(onClick = { onPermanentlyDelete(task.id); permanentDeleteTask = null }) { Text("Elimina definitivamente", color = Danger) } },
            dismissButton = { TextButton(onClick = { permanentDeleteTask = null }) { Text("Annulla", color = SoftText) } }
        )
    }
    deleteSelectionConfirmation?.let { ids ->
        AlertDialog(
            onDismissRequest = { deleteSelectionConfirmation = null },
            containerColor = CardBlue,
            title = { Text(if (ids.size == tasks.size) "Eliminare tutto il Cestino?" else "Eliminare le attività selezionate?", color = MainText) },
            text = { Text("Le ${ids.size} attività selezionate non potranno più essere recuperate.", color = SoftText) },
            confirmButton = {
                TextButton(onClick = {
                    onPermanentlyDeleteMany(ids)
                    selectedTaskIds = emptySet()
                    deleteSelectionConfirmation = null
                    if (ids.size == tasks.size) onDismiss()
                }) { Text("Elimina definitivamente", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { deleteSelectionConfirmation = null }) { Text("Annulla", color = SoftText) } }
        )
    }
}
