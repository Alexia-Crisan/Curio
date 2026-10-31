package com.example.curio.feature.progress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.curio.R
import com.example.curio.domain.DayStatus
import com.example.curio.domain.MonthProgress
import com.example.curio.feature.achievements.BadgeSummary
import com.example.curio.feature.achievements.BadgesEntryCard
import com.example.curio.ui.components.TreeCard
import com.example.curio.ui.theme.Spacing
import com.example.curio.ui.theme.curioCardBorder
import com.example.curio.ui.theme.curioCardColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ProgressScreen(
    treeUiState: TreeUiState = TreeUiState(),
    badgeSummary: BadgeSummary = BadgeSummary(0, 7, false),
    onOpenAchievements: () -> Unit = {},
    onSelectTheme: (String) -> Unit = {},
    onToggleTopicExpanded: (String) -> Unit = {},
    onLeafClick: (String) -> Unit = {},
    onOpenTree: () -> Unit = {},
    month: MonthProgress? = null,
    savedCount: Int = 0,
    today: LocalDate = LocalDate.now(),
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onDayClick: (LocalDate) -> Unit = {},
) {
    val completedDays = month?.daysCompleted ?: 0
    val totalDays = month?.daysInMonth ?: 30
    val daysExplored = month?.days?.size ?: 0
    val progressFraction = if (totalDays > 0) (completedDays.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(progressFraction, label = "progress")

    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.xl))
        Text(
            text = stringResource(R.string.progress_days_completed),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.align(Alignment.Start),
        )

        Spacer(Modifier.height(Spacing.xxl))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 18.dp.toPx()
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$completedDays / $totalDays",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.progress_days_completed),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(Spacing.xxl))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = curioCardColors(),
                border = curioCardBorder(false),
            ) {
                Column(Modifier.padding(Spacing.lg)) {
                    Text(stringResource(R.string.progress_days_explored), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(Spacing.xs))
                    Text(daysExplored.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = curioCardColors(),
                border = curioCardBorder(false),
            ) {
                Column(Modifier.padding(Spacing.lg)) {
                    Text(stringResource(R.string.progress_saved), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(Spacing.xs))
                    Text(savedCount.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(Modifier.height(Spacing.xxl))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPreviousMonth) {
                Text("‹", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = month?.month?.format(DateTimeFormatter.ofPattern("MMMM yyyy")) ?: "",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(onClick = onNextMonth) {
                Text("›", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(Modifier.height(Spacing.sm))

        if (month != null) {
            MonthCalendar(
                month = month,
                today = today,
                onDayClick = onDayClick,
            )
            Spacer(Modifier.height(Spacing.xxl))
        }

        BadgesEntryCard(
            summary = badgeSummary,
            onClick = onOpenAchievements,
        )

        Spacer(Modifier.height(Spacing.xxl))

        TreeCard(
            state = treeUiState,
            onThemeSelected = onSelectTheme,
            onTopicClick = onToggleTopicExpanded,
            onLeafClick = { onLeafClick(it.discoveryId) },
            onOpenTree = onOpenTree,
        )
    }
}

@Composable
fun MonthCalendar(
    month: MonthProgress,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit,
) {
    val first = month.month.atDay(1)
    val offset = first.dayOfWeek.value - 1
    val cells: List<LocalDate?> = List(offset) { null } +
        (1..month.daysInMonth).map { month.month.atDay(it) }
    Column(Modifier.fillMaxWidth()) {
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                    ) {
                        date?.let { d ->
                            DayCell(
                                date = d,
                                status = month.statusOf(d),
                                isToday = d == today,
                                enabled = d <= today,
                                onClick = { onDayClick(d) },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    status: DayStatus,
    isToday: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val c = MaterialTheme.colorScheme
    val state = stringResource(
        when (status) {
            DayStatus.COMPLETE -> R.string.day_complete
            DayStatus.IN_PROGRESS -> R.string.day_in_progress
            DayStatus.NOT_STARTED -> R.string.day_not_started
        }
    )
    val description = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)) + ", " + state
    Box(
        modifier
            .clip(CircleShape)
            .then(
                when (status) {
                    DayStatus.COMPLETE -> Modifier.background(c.primary)
                    DayStatus.IN_PROGRESS -> Modifier.border(2.dp, c.primary, CircleShape)
                    DayStatus.NOT_STARTED -> Modifier
                }
            )
            .then(if (isToday) Modifier.border(2.dp, c.onSurface, CircleShape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${date.dayOfMonth}",
            color = if (status == DayStatus.COMPLETE) c.onPrimary else c.onSurface,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
