package com.example.taskorganizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val SubtaskListSaver = listSaver<List<Subtask>, String>(
    save = { list -> list.flatMap { listOf(it.id, it.title, it.done.toString()) } },
    restore = { values -> values.chunked(3).filter { it.size == 3 }.map { Subtask(it[0], it[1], it[2].toBoolean()) } }
)

internal fun subtaskCount(subtasks: List<Subtask>): String? =
    if (subtasks.isEmpty()) null else "${subtasks.count { it.done }}/${subtasks.size}"

@Composable
internal fun SubtaskEditor(subtasks: List<Subtask>, draft: String, onDraftChange: (String) -> Unit, onSubtasksChange: (List<Subtask>) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBlue), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.subtasks_title), color = MainText, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.subtasks_description), color = SoftText, fontSize = 12.sp)
            OutlinedTextField(
                value = draft, onValueChange = { onDraftChange(it.take(MAX_SUBTASK_TITLE)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(stringResource(R.string.subtasks_input)) },
                enabled = subtasks.size < MAX_SUBTASKS,
                shape = RoundedCornerShape(18.dp), colors = compactEditorFieldColors()
            )
            Button(
                onClick = { if (draft.isNotBlank() && subtasks.size < MAX_SUBTASKS) { onSubtasksChange(subtasks + Subtask(title = draft.trim())); onDraftChange("") } },
                enabled = draft.isNotBlank() && subtasks.size < MAX_SUBTASKS,
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text(stringResource(R.string.subtasks_add)) }
            if (subtasks.size >= MAX_SUBTASKS) Text(stringResource(R.string.subtasks_limit), color = SoftText, fontSize = 12.sp)
            subtasks.forEach { subtask ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.weight(1f).heightIn(min = 48.dp).toggleable(
                            value = subtask.done, role = Role.Checkbox,
                            onValueChange = { checked -> onSubtasksChange(subtasks.map { if (it.id == subtask.id) it.copy(done = checked) else it }) }
                        ), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = subtask.done, onCheckedChange = null)
                        Text(subtask.title, color = MainText, modifier = Modifier.weight(1f))
                    }
                    IconButton(onClick = { onSubtasksChange(subtasks.filterNot { it.id == subtask.id }) }) {
                        Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.subtasks_delete, subtask.title), tint = MainText)
                    }
                }
            }
        }
    }
}
