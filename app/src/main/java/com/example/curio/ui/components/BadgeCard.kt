package com.example.curio.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.curio.R
import com.example.curio.feature.achievements.AchievementUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgeCard(badge: AchievementUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val title = stringResource(badge.titleRes)
    val description = if (badge.isUnlocked) stringResource(R.string.badge_cd_unlocked, title, badge.unlockedOn.orEmpty())
    else stringResource(R.string.badge_cd_locked, title, badge.current, badge.target)
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description },
        colors = CardDefaults.cardColors(
            containerColor = if (badge.isUnlocked) colors.primaryContainer else colors.surfaceVariant,
            contentColor = if (badge.isUnlocked) colors.onPrimaryContainer else colors.onSurfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp).heightIn(min = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(badge.iconRes), contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            if (badge.isUnlocked) {
                Text(stringResource(R.string.badge_unlocked_on, badge.unlockedOn.orEmpty()), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            } else {
                LinearProgressIndicator(progress = { badge.fraction }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Text("${stringResource(R.string.badge_locked)} · ${stringResource(R.string.badge_progress, badge.current, badge.target)}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
