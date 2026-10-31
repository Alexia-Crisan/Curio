package com.example.curio.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.curio.R
import com.example.curio.domain.LeafNode
import com.example.curio.domain.TopicNode
import com.example.curio.feature.progress.TreeUiState
import com.example.curio.feature.tree.TreeVisuals
import com.example.curio.ui.theme.CurioTheme
import com.example.curio.ui.theme.Spacing
import com.example.curio.ui.theme.curioCardBorder
import com.example.curio.ui.theme.curioCardColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeCard(
    state: TreeUiState,
    onThemeSelected: (String) -> Unit,
    onTopicClick: (String) -> Unit,
    onLeafClick: (LeafNode) -> Unit,
    onOpenTree: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = curioCardColors(),
        border = curioCardBorder(false),
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Text(stringResource(R.string.tree_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            val tree = state.currentTree
            when {
                tree == null -> Text(stringResource(R.string.tree_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> {
                    if (state.trees.size > 1) {
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()).padding(vertical = Spacing.sm),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            state.trees.forEach { t ->
                                FilterChip(
                                    selected = t.theme.id == tree.theme.id,
                                    onClick = { onThemeSelected(t.theme.id) },
                                    label = { Text(t.theme.title) },
                                )
                            }
                        }
                    }
                    Text(stringResource(TreeVisuals.stage(tree.stage)), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.tree_progress, tree.completed, tree.total), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(Spacing.sm))
                    tree.branches.forEach { branch ->
                        TopicRow(branch, branch.topic.id in state.expandedTopicIds, onTopicClick, onLeafClick)
                    }
                }
            }
            Spacer(Modifier.height(Spacing.sm))
            TextButton(onClick = onOpenTree, modifier = Modifier.heightIn(min = Spacing.xxxl)) {
                Text(stringResource(R.string.tree_open), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun TopicRow(branch: TopicNode, expanded: Boolean, onTopicClick: (String) -> Unit, onLeafClick: (LeafNode) -> Unit) {
    val stateLabel = stringResource(TreeVisuals.state(branch.state))
    val description = stringResource(R.string.tree_cd_topic, branch.topic.title, stateLabel, branch.completed, branch.total)
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = Spacing.xxxl)
                .semantics(mergeDescendants = true) { contentDescription = description }
                .clickable { onTopicClick(branch.topic.id) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(branch.topic.title, style = MaterialTheme.typography.titleSmall)
                Text(stateLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(stringResource(R.string.tree_topic_progress, branch.completed, branch.total), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        if (expanded) branch.leaves.forEach { LeafRow(it, onLeafClick) }
    }
}

@Composable
private fun LeafRow(leaf: LeafNode, onLeafClick: (LeafNode) -> Unit) {
    val title = leaf.title
    val description = if (leaf.completed && title != null) stringResource(R.string.tree_cd_leaf_done, title)
    else stringResource(R.string.tree_cd_leaf_locked)
    Row(
        Modifier.fillMaxWidth().heightIn(min = Spacing.xxxl).padding(start = Spacing.lg)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .clickable(enabled = leaf.completed) { onLeafClick(leaf) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (leaf.completed) "●" else "○",
            color = if (leaf.completed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(Spacing.md))
        Column {
            Text(
                title ?: stringResource(R.string.tree_leaf_hidden),
                style = MaterialTheme.typography.bodyMedium,
                color = if (leaf.completed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (leaf.isLandmark) Text(stringResource(R.string.tree_leaf_landmark), style = MaterialTheme.typography.labelSmall, color = CurioTheme.colors.reward)
            if (leaf.viaBonus) Text(stringResource(R.string.tree_leaf_bonus), style = MaterialTheme.typography.labelSmall, color = CurioTheme.colors.reward)
        }
    }
}
