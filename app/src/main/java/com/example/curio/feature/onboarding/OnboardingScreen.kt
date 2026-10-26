package com.example.curio.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.curio.R
import com.example.curio.domain.DailyMode

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel, onFinished: () -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { onFinished() }
    }

    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.onboarding_subtitle), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.onboarding_pick_theme), style = MaterialTheme.typography.titleMedium)

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ui.themes, key = { it.id }) { theme ->
                val selected = theme.id == ui.selectedThemeId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .selectable(
                            selected = selected,
                            enabled = theme.available,
                            role = Role.RadioButton,
                            onClick = { viewModel.selectTheme(theme.id) },
                        ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(selected = selected, onClick = null, enabled = theme.available)
                        Column {
                            Text(theme.title, style = MaterialTheme.typography.titleMedium)
                            if (!theme.available) {
                                Text(stringResource(R.string.theme_coming_soon), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        Text(stringResource(R.string.onboarding_pick_mode), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DailyMode.values().forEach { mode ->
                FilterChip(
                    selected = ui.mode == mode,
                    onClick = { viewModel.selectMode(mode) },
                    label = { Text(stringResource(modeLabel(mode))) },
                )
            }
        }
        Text(stringResource(modeDescription(ui.mode)), style = MaterialTheme.typography.bodyMedium)

        Button(
            onClick = viewModel::start,
            enabled = ui.canStart,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.onboarding_start))
        }
    }
}

fun modeLabel(mode: DailyMode): Int = when (mode) {
    DailyMode.GUIDED -> R.string.mode_guided
    DailyMode.CHOOSE -> R.string.mode_choose
    DailyMode.SURPRISE -> R.string.mode_surprise
}

fun modeDescription(mode: DailyMode): Int = when (mode) {
    DailyMode.GUIDED -> R.string.mode_guided_desc
    DailyMode.CHOOSE -> R.string.mode_choose_desc
    DailyMode.SURPRISE -> R.string.mode_surprise_desc
}
