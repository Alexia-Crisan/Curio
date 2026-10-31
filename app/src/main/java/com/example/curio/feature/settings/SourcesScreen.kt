package com.example.curio.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.curio.R
import com.example.curio.domain.SourceEntry
import com.example.curio.ui.theme.Spacing
import com.example.curio.ui.theme.curioCardBorder
import com.example.curio.ui.theme.curioCardColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(entries: List<SourceEntry>, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sources_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        if (entries.isEmpty()) {
            Text(
                stringResource(R.string.sources_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(padding).padding(Spacing.xl),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                items(entries, key = { it.name + "|" + it.license }) { entry ->
                    Card(colors = curioCardColors(), border = curioCardBorder(false)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Text(entry.name, style = MaterialTheme.typography.titleMedium)
                            Text(entry.license, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(entry.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            Text(stringResource(R.string.sources_cards, entry.cardCount), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            entry.attributions.forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        }
    }
}
