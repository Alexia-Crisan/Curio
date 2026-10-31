package com.example.curio.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
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
import com.example.curio.ui.theme.Spacing
import com.example.curio.ui.theme.curioCardBorder
import com.example.curio.ui.theme.curioCardColors
import com.example.curio.ui.theme.curioSelectedCardColors

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel, onFinished: () -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { onFinished() }
    }

    Box(Modifier.fillMaxSize().systemBarsPadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.xl)
                .widthIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.onboarding_subtitle), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.onboarding_pick_theme), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
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
                        colors = if (selected) curioSelectedCardColors() else curioCardColors(),
                        border = curioCardBorder(selected),
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.lg),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        ) {
                            RadioButton(selected = selected, onClick = null, enabled = theme.available)
                            Column {
                                Text(theme.title, style = MaterialTheme.typography.titleMedium)
                                if (!theme.available) {
                                    Text(stringResource(R.string.theme_coming_soon), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            Text(stringResource(R.string.onboarding_pick_mode), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                DailyMode.values().forEach { mode ->
                    FilterChip(
                        selected = ui.mode == mode,
                        onClick = { viewModel.selectMode(mode) },
                        label = { Text(stringResource(modeLabel(mode))) },
                    )
                }
            }
            Text(stringResource(modeDescription(ui.mode)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Button(
                onClick = viewModel::start,
                enabled = ui.canStart,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.onboarding_start), style = MaterialTheme.typography.labelLarge)
            }
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
