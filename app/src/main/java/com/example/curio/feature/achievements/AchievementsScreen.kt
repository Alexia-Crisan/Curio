package com.example.curio.feature.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.curio.R
import com.example.curio.ui.components.BadgeCard
import com.example.curio.ui.theme.CurioTheme
import com.example.curio.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(viewModel: AchievementsViewModel, onBack: () -> Unit, onGoToFeed: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val oneColumn = LocalDensity.current.fontScale >= 1.5f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.badges_title), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = if (oneColumn) GridCells.Fixed(1) else GridCells.Adaptive(150.dp),
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(R.string.badges_unlocked_count, state.unlockedCount, state.total),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
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
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.xl).padding(bottom = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (badge.isUnlocked) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(CurioTheme.colors.rewardContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(badge.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = CurioTheme.colors.onRewardContainer,
                )
            }
        } else {
            Icon(
                painterResource(badge.iconRes),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Spacing.md))
        Text(stringResource(badge.titleRes), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.md))
        Text(stringResource(R.string.badge_how_to_earn), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(badge.description(), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.lg))
        if (badge.isUnlocked) {
            Text(stringResource(R.string.badge_unlocked_on, badge.unlockedOn.orEmpty()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LinearProgressIndicator(
                progress = { badge.fraction },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(stringResource(R.string.badge_progress, badge.current, badge.target), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Spacing.lg))
            Button(onClick = onGoToFeed, shape = CircleShape) { Text(stringResource(R.string.badge_go_to_feed), style = MaterialTheme.typography.labelLarge) }
        }
    }
}
