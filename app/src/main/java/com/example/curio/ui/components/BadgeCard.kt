package com.example.curio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.curio.R
import com.example.curio.feature.achievements.AchievementUi
import com.example.curio.ui.theme.CurioTheme
import com.example.curio.ui.theme.Spacing
import com.example.curio.ui.theme.curioCardBorder
import com.example.curio.ui.theme.curioCardColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgeCard(badge: AchievementUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val title = stringResource(badge.titleRes)
    val description = if (badge.isUnlocked) stringResource(R.string.badge_cd_unlocked, title, badge.unlockedOn.orEmpty())
    else stringResource(R.string.badge_cd_locked, title, badge.current, badge.target)
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description },
        colors = curioCardColors(),
        border = curioCardBorder(badge.isUnlocked),
    ) {
        Column(Modifier.padding(Spacing.lg).heightIn(min = Spacing.xxxl), horizontalAlignment = Alignment.CenterHorizontally) {
            if (badge.isUnlocked) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(CurioTheme.colors.rewardContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(badge.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = CurioTheme.colors.onRewardContainer,
                    )
                }
            } else {
                Icon(
                    painterResource(badge.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            Text(title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Spacing.sm))
            if (badge.isUnlocked) {
                Text(
                    stringResource(R.string.badge_unlocked_on, badge.unlockedOn.orEmpty()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            } else {
                LinearProgressIndicator(
                    progress = { badge.fraction },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    "${stringResource(R.string.badge_locked)} · ${stringResource(R.string.badge_progress, badge.current, badge.target)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
