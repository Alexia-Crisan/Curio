package com.example.curio.feature.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.curio.R
import com.example.curio.domain.NoteRules
import com.example.curio.ui.theme.Spacing

@Composable
fun NoteSection(
    note: String?,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }

    Column(modifier.fillMaxWidth()) {
        Text(stringResource(R.string.note_title), style = MaterialTheme.typography.titleMedium)
        if (note != null) {
            Spacer(Modifier.height(Spacing.sm))
            Text(note, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("saved_note"))
        }
        TextButton(
            onClick = {
                draft = note.orEmpty()
                editing = true
            },
        ) {
            Text(stringResource(if (note == null) R.string.note_add else R.string.note_edit))
        }
    }

    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(stringResource(R.string.note_title)) },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= NoteRules.MAX_LENGTH) draft = it },
                    modifier = Modifier.fillMaxWidth().testTag("note_field"),
                    placeholder = { Text(stringResource(R.string.note_hint)) },
                    supportingText = { Text("${draft.length}/${NoteRules.MAX_LENGTH}") },
                    minLines = 3,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSave(draft)
                        editing = false
                    },
                ) {
                    Text(stringResource(R.string.note_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editing = false }) {
                    Text(stringResource(R.string.note_cancel))
                }
            },
        )
    }
}
