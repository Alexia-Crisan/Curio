package com.example.curio.feature.achievements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.curio.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgesEntryCard(summary: BadgeSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(
        if (summary.hasNew) R.string.badges_entry_cd_new else R.string.badges_entry_cd,
        summary.unlocked, summary.total,
    )
    Card(onClick = onClick, modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description }) {
        Row(Modifier.padding(16.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.badges_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.badges_unlocked_count, summary.unlocked, summary.total), style = MaterialTheme.typography.bodyMedium)
            }
            if (summary.hasNew) Badge()
        }
    }
}
