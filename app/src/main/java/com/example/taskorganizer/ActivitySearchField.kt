package com.example.taskorganizer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
internal fun ActivitySearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = { onQueryChange(it.take(100)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.activity_search_label)) },
        placeholder = { Text(stringResource(R.string.activity_search_hint)) },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors = compactEditorFieldColors(),
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(painterResource(R.drawable.ic_search_clear), contentDescription = stringResource(R.string.activity_search_clear), tint = MainText)
                }
            }
        }
    )
}
