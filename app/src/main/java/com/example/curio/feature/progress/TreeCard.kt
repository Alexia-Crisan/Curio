package com.example.curio.feature.progress

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.curio.domain.LeafNode
import com.example.curio.domain.NodeState
import com.example.curio.domain.TopicNode

@Composable
fun TreeCard(
    uiState: TreeUiState,
    onSelectTheme: (String) -> Unit,
    onToggleTopicExpanded: (String) -> Unit,
    onLeafClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentTree = uiState.currentTree ?: return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "🌳 Knowledge Tree",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = "Stage: ${currentTree.stage.name}",
                        fontSize = 13.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = "${currentTree.completed} / ${currentTree.total}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { currentTree.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF10B981),
                trackColor = Color.White.copy(alpha = 0.1f),
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.trees.forEach { tree ->
                    val selected = tree.theme.id == uiState.selectedThemeId
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Color(0xFF6366F1) else Color.White.copy(alpha = 0.1f))
                            .clickable { onSelectTheme(tree.theme.id) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = tree.theme.title,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            currentTree.branches.forEach { topicNode ->
                val isExpanded = topicNode.topic.id in uiState.expandedTopicIds
                TopicBranchRow(
                    node = topicNode,
                    isExpanded = isExpanded,
                    onToggleExpand = { onToggleTopicExpanded(topicNode.topic.id) },
                    onLeafClick = onLeafClick,
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun TopicBranchRow(
    node: TopicNode,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onLeafClick: (String) -> Unit,
) {
    val stateColor = when (node.state) {
        NodeState.LOCKED -> Color.Gray
        NodeState.SEEDED -> Color(0xFF38BDF8)
        NodeState.GROWING -> Color(0xFFF59E0B)
        NodeState.COMPLETE -> Color(0xFF10B981)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { onToggleExpand() }
            .padding(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = node.topic.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${node.state.name} (${node.completed}/${node.total})",
                    color = stateColor,
                    fontSize = 12.sp,
                )
            }
            Text(
                text = if (isExpanded) "▲" else "▼",
                color = Color.Gray,
                fontSize = 12.sp,
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                node.leaves.forEach { leaf ->
                    LeafRow(leaf = leaf, onLeafClick = onLeafClick)
                }
            }
        }
    }
}

@Composable
private fun LeafRow(leaf: LeafNode, onLeafClick: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (leaf.completed) Color.White.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(enabled = leaf.completed) { onLeafClick(leaf.discoveryId) }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = when {
                leaf.isLandmark -> "🌸"
                leaf.viaBonus -> "✨"
                leaf.completed -> "🍃"
                else -> "🔒"
            },
            fontSize = 14.sp,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = leaf.title ?: "?",
            color = if (leaf.completed) Color.White else Color.Gray,
            fontSize = 13.sp,
            fontWeight = if (leaf.completed) FontWeight.Medium else FontWeight.Normal,
        )
    }
}
