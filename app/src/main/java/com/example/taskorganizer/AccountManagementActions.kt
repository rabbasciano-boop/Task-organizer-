package com.example.taskorganizer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun AccountManagementActions(working: Boolean, onSignOut: (Boolean) -> Unit, onDeleteAccount: (Boolean) -> Unit) {
    var dialog by remember { mutableStateOf<String?>(null) }
    var deleteLocal by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { deleteLocal = false; dialog = "signout" }, enabled = !working,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(16.dp)
    ) { Text(stringResource(R.string.account_sign_out), color = Gold, fontWeight = FontWeight.Bold) }
    TextButton(
        onClick = { deleteLocal = false; dialog = "delete" }, enabled = !working,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
    ) { Text(stringResource(R.string.account_delete), color = Danger, fontWeight = FontWeight.SemiBold) }

    dialog?.let { action ->
        val deleting = action == "delete"
        AlertDialog(
            onDismissRequest = { dialog = null }, containerColor = CardBlue,
            title = { Text(stringResource(if (deleting) R.string.account_delete_title else R.string.account_sign_out_title), color = MainText) },
            text = {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    Text(stringResource(if (deleting) R.string.account_delete_description else R.string.account_sign_out_description), color = SoftText)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = deleteLocal, role = Role.Checkbox, onValueChange = { deleteLocal = it }).padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = deleteLocal, onCheckedChange = null)
                        Text(stringResource(R.string.account_delete_local_option), color = MainText, modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    if (deleting) onDeleteAccount(deleteLocal) else onSignOut(deleteLocal)
                }) { Text(stringResource(if (deleting) R.string.account_delete_confirm else R.string.account_sign_out_confirm), color = if (deleting) Danger else Gold) }
            },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.cancel), color = SoftText) } }
        )
    }
}
