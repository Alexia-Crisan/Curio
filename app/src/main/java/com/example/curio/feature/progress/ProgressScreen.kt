package com.example.curio.feature.progress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.curio.feature.achievements.BadgeSummary
import com.example.curio.feature.achievements.BadgesEntryCard

@Composable
fun ProgressScreen(
    treeUiState: TreeUiState = TreeUiState(),
    badgeSummary: BadgeSummary = BadgeSummary(0, 7, false),
    onOpenAchievements: () -> Unit = {},
    onSelectTheme: (String) -> Unit = {},
    onToggleTopicExpanded: (String) -> Unit = {},
    onLeafClick: (String) -> Unit = {},
    onOpenTree: () -> Unit = {},
    completedDays: Int = 12,
    totalDays: Int = 30,
    currentStreak: Int = 5,
) {
    val progressFraction = (completedDays.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(progressFraction, label = "progress")

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Your Discovery Progress",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start),
        )

        Spacer(Modifier.height(32.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 18.dp.toPx()
                drawArc(
                    color = Color.White.copy(alpha = 0.15f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                drawArc(
                    color = Color(0xFF6366F1),
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$completedDays / $totalDays",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = "Days Completed",
                    fontSize = 13.sp,
                    color = Color.Gray,
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("🔥 Streak", fontSize = 15.sp, color = Color.Gray)
                    Spacer(Modifier.height(6.dp))
                    Text("$currentStreak Days", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("⭐ Total Saved", fontSize = 15.sp, color = Color.Gray)
                    Spacer(Modifier.height(6.dp))
                    Text("8 Items", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        BadgesEntryCard(
            summary = badgeSummary,
            onClick = onOpenAchievements,
        )

        Spacer(Modifier.height(28.dp))

        TreeCard(
            uiState = treeUiState,
            onSelectTheme = onSelectTheme,
            onToggleTopicExpanded = onToggleTopicExpanded,
            onLeafClick = onLeafClick,
            onOpenTree = onOpenTree,
        )
    }
}
