package com.example.curio.feature.achievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.curio.R
import com.example.curio.ui.components.BadgeCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(viewModel: AchievementsViewModel, onBack: () -> Unit, onGoToFeed: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val oneColumn = LocalDensity.current.fontScale >= 1.5f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.badges_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = if (oneColumn) GridCells.Fixed(1) else GridCells.Adaptive(150.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.badges_unlocked_count, state.unlockedCount, state.total), style = MaterialTheme.typography.titleMedium)
            }
            items(state.items, key = { it.id.name }) { badge ->
                BadgeCard(badge, onClick = { viewModel.onBadgeClick(badge.id) })
            }
        }
    }

    state.selected?.let { badge ->
        ModalBottomSheet(onDismissRequest = viewModel::onDismissSheet) {
            BadgeSheet(badge, onGoToFeed = { viewModel.onDismissSheet(); onGoToFeed() })
        }
    }
}

@Composable
private fun BadgeSheet(badge: AchievementUi, onGoToFeed: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painterResource(badge.iconRes), contentDescription = null, modifier = Modifier.size(56.dp),
            tint = if (badge.isUnlocked) colors.primary else colors.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Text(stringResource(badge.titleRes), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.badge_how_to_earn), style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
        Text(badge.description(), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        if (badge.isUnlocked) {
            Text(stringResource(R.string.badge_unlocked_on, badge.unlockedOn.orEmpty()), style = MaterialTheme.typography.bodyMedium)
        } else {
            LinearProgressIndicator(progress = { badge.fraction }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.badge_progress, badge.current, badge.target), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onGoToFeed) { Text(stringResource(R.string.badge_go_to_feed)) }
        }
    }
}
