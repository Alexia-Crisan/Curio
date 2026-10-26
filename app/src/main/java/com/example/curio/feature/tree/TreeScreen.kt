package com.example.curio.feature.tree

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.curio.R
import com.example.curio.domain.LeafNode
import com.example.curio.domain.NextStep
import com.example.curio.domain.TopicNode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeScreen(
    state: TreeScreenUi,
    onBack: () -> Unit,
    onThemeSelected: (String) -> Unit,
    onTopicTap: (String) -> Unit,
    onLeafTap: (String, LeafNode) -> Unit,
    onSheetDismiss: () -> Unit,
    onOpenDetail: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tree_title)) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.tree_back)) } },
            )
        },
    ) { padding ->
        val tree = state.selected
        when {
            state.isLoading -> Unit
            tree == null -> Text(stringResource(R.string.tree_empty), modifier = Modifier.padding(padding).padding(24.dp))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.trees.size > 1) {
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.trees.forEach { t ->
                                    FilterChip(
                                        selected = t.theme.id == tree.theme.id,
                                        onClick = { onThemeSelected(t.theme.id) },
                                        label = { Text(t.theme.title) },
                                    )
                                }
                            }
                        }
                        Text(stringResource(TreeVisuals.stage(tree.stage)), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.tree_progress, tree.completed, tree.total))
                        state.themeExplanation?.let { e ->
                            val next = e.nextStage
                            val left = e.cardsToNextStage
                            if (next != null && left != null) {
                                Text(stringResource(R.string.tree_theme_next_stage, left, stringResource(TreeVisuals.stage(next))))
                            } else if (e.total > 0) {
                                Text(stringResource(R.string.tree_theme_top))
                            }
                        }
                        state.growth?.let { g ->
                            if (g.newLeafCount > 0) {
                                Text(pluralStringResource(R.plurals.tree_growth_new, g.newLeafCount, g.newLeafCount))
                                if (g.stageChanged) Text(stringResource(R.string.tree_growth_stage, stringResource(TreeVisuals.stage(g.stageAfter))))
                            }
                        }
                    }
                }
                items(tree.branches, key = { it.topic.id }) { branch ->
                    BranchRow(branch, onTopicTap, onLeafTap)
                }
            }
        }

        state.sheet?.let { sheet ->
            ModalBottomSheet(onDismissRequest = onSheetDismiss) {
                NodeSheetContent(sheet, onOpenDetail, onSheetDismiss)
            }
        }
    }
}

@Composable
private fun BranchRow(branch: TopicNode, onTopicTap: (String) -> Unit, onLeafTap: (String, LeafNode) -> Unit) {
    val stateText = stringResource(TreeVisuals.state(branch.state))
    val description = stringResource(R.string.tree_cd_topic, branch.topic.title, stateText, branch.completed, branch.total)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button) { onTopicTap(branch.topic.id) }
                    .semantics { contentDescription = description },
            ) {
                Text(branch.topic.title, style = MaterialTheme.typography.titleMedium)
                Text(stateText, style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.tree_topic_progress, branch.completed, branch.total), style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                branch.leaves.forEach { leaf ->
                    val leafDescription = if (leaf.completed) {
                        stringResource(R.string.tree_cd_leaf_done, leaf.title.orEmpty())
                    } else {
                        stringResource(R.string.tree_cd_leaf_locked)
                    }
                    AssistChip(
                        onClick = { onLeafTap(branch.topic.id, leaf) },
                        label = { Text(if (leaf.completed) leaf.title.orEmpty() else stringResource(R.string.tree_leaf_hidden)) },
                        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = leafDescription },
                    )
                }
            }
        }
    }
}

@Composable
fun NodeSheetContent(sheet: NodeSheet, onOpenDetail: (String) -> Unit, onClose: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (sheet) {
            is NodeSheet.TopicSheet -> {
                val e = sheet.explanation
                Text(sheet.node.topic.title, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(TreeVisuals.state(e.state)))
                Text(stringResource(R.string.tree_topic_progress, e.completed, e.total))
                Text(
                    when (val n = e.next) {
                        is NextStep.StartTopic -> stringResource(R.string.tree_next_start, n.cardsToGrow)
                        is NextStep.ReachGrowing -> stringResource(R.string.tree_next_reach_growing, n.cardsLeft)
                        is NextStep.FinishTopic -> stringResource(R.string.tree_next_finish, n.cardsLeft)
                        NextStep.Done -> stringResource(R.string.tree_next_done)
                    },
                )
                if (e.hasLandmark) {
                    Text(stringResource(if (e.landmarkFound) R.string.tree_sheet_landmark_found else R.string.tree_sheet_landmark_missing))
                }
                if (e.bonusLeaves > 0) Text(stringResource(R.string.tree_sheet_bonus_count, e.bonusLeaves))
            }
            is NodeSheet.LeafSheet -> {
                val e = sheet.explanation
                Text(
                    if (e.completed) e.title.orEmpty() else stringResource(R.string.tree_leaf_hidden),
                    style = MaterialTheme.typography.titleLarge,
                )
                if (e.isLandmark) Text(stringResource(R.string.tree_leaf_landmark))
                if (e.viaBonus) Text(stringResource(R.string.tree_leaf_bonus))
                if (!e.completed) Text(stringResource(R.string.tree_sheet_locked_hint))
                if (e.canOpenDetail) {
                    Button(
                        onClick = { onOpenDetail(e.discoveryId) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.tree_sheet_open_detail))
                    }
                }
            }
        }
        TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.tree_sheet_close))
        }
    }
}
