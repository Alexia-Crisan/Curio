package com.example.curio.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.curio.R
import com.example.curio.domain.Discovery
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun LibraryScreen(
    savedCards: List<Discovery>,
    history: List<HistoryDay> = emptyList(),
    onCardClick: (String) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Library",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(20.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            val tabs = listOf("Saved (${savedCards.size})", "History")
            tabs.forEachIndexed { i, tab ->
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == i) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { selectedTab = i }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = tab,
                        color = if (selectedTab == i) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        when {
            (selectedTab == 0 && savedCards.isEmpty()) -> EmptyState(stringResource(R.string.library_saved_empty))
            selectedTab == 0 -> SavedList(savedCards, onCardClick)
            history.isEmpty() -> EmptyState(stringResource(R.string.library_history_empty))
            else -> HistoryList(history, onCardClick)
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SavedList(savedCards: List<Discovery>, onCardClick: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(savedCards, key = { it.id }) { card ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCardClick(card.id) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(card.accentColor)),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = card.type.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = card.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryList(days: List<HistoryDay>, onCardClick: (String) -> Unit) {
    val today = remember { LocalDate.now() }
    LazyColumn {
        days.forEach { day ->
            item(key = "h-${day.date}") {
                Text(
                    text = dayLabel(day.date, today),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 4.dp)
                        .semantics { heading() },
                )
            }
            items(day.items, key = { "${day.date}-${it.id}" }) { d ->
                ListItem(
                    headlineContent = { Text(d.title) },
                    supportingContent = { Text(d.source.name) },
                    modifier = Modifier.clickable { onCardClick(d.id) },
                )
            }
        }
    }
}

@Composable
private fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.history_today)
    today.minusDays(1) -> stringResource(R.string.history_yesterday)
    else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
}
