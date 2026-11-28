package com.example.taskorganizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalDateTime

internal enum class QuickTaskSaveResult { SAVED, SAVED_WITHOUT_REMINDER, FAILED }

@Composable
internal fun QuickTaskField(text: String, draft: QuickTaskDraft?, now: LocalDateTime, saving: Boolean, onTextChange: (String) -> Unit, onSubmit: () -> Unit) {
    val enabled = text.isNotBlank() && !saving
    Card(colors = CardDefaults.cardColors(containerColor = CardBlue.copy(alpha = .92f)), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = text, onValueChange = onTextChange, enabled = !saving,
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(stringResource(R.string.quick_add_label)) },
                placeholder = { Text(stringResource(R.string.quick_add_hint)) },
                shape = RoundedCornerShape(18.dp), colors = compactEditorFieldColors(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (enabled) onSubmit() }),
                trailingIcon = {
                    IconButton(onClick = onSubmit, enabled = enabled) {
                        Icon(painterResource(R.drawable.ic_quick_add), contentDescription = stringResource(R.string.quick_add_action), tint = if (enabled) Gold else SoftText)
                    }
                }
            )
            if (draft != null) {
                val dateLabel = when (draft.date) {
                    LocalDate.now() -> stringResource(R.string.quick_date_today)
                    LocalDate.now().plusDays(1) -> stringResource(R.string.quick_date_tomorrow)
                    else -> formatItalianDate(draft.date.toString())
                }
                val timeLabel = draft.time?.toString() ?: stringResource(R.string.quick_no_time)
                Text(stringResource(R.string.quick_preview_title, draft.title), color = MainText, fontSize = 13.sp)
                Text(stringResource(R.string.quick_preview, dateLabel, timeLabel), color = SoftText, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                if (draft.ambiguousHour && draft.time != null) {
                    val laterTime = draft.time.plusHours(12).toString()
                    Text(stringResource(R.string.quick_time_ambiguous, timeLabel, laterTime), color = SoftText, fontSize = 12.sp)
                }
                if (draft.isPast(now)) Text(stringResource(R.string.quick_time_past), color = Danger, fontSize = 13.sp)
            }
        }
    }
}
