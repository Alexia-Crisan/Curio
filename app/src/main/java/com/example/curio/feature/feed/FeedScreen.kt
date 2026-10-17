package com.example.curio.feature.feed

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.curio.domain.Discovery

@Composable
fun FeedScreen(viewModel: FeedViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedIds.collectAsStateWithLifecycle()

    when (val s = state) {
        FeedUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        is FeedUiState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            Button(onClick = viewModel::load) { Text("Retry: ${s.message}") }
        }
        is FeedUiState.Ready -> FeedPager(
            cards = s.cards,
            savedIds = savedIds,
            onSave = viewModel::onSaveClicked,
            onQuizAnswered = viewModel::onQuizAnswered,
        )
    }
}

@Composable
private fun FeedPager(
    cards: List<Discovery>,
    savedIds: Set<String>,
    onSave: (String) -> Unit,
    onQuizAnswered: (String) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { cards.size })
    Box(Modifier.fillMaxSize()) {
        VerticalPager(state = pagerState, key = { cards[it].id }) { page ->
            val d = cards[page]
            DiscoveryCard(
                d = d,
                saved = d.id in savedIds,
                onSave = { onSave(d.id) },
                onQuizAnswered = { onQuizAnswered(d.id) },
            )
        }
        SegmentedProgress(
            count = cards.size,
            current = pagerState.currentPage,
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp),
        )
    }
}

@Composable
private fun SegmentedProgress(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(count) { i ->
            val fill by animateFloatAsState(if (i <= current) 1f else 0f, label = "segment")
            Box(
                Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fill)
                        .background(Color.White)
                )
            }
        }
    }
}
