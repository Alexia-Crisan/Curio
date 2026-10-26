package com.example.curio.feature.feed

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.curio.R
import com.example.curio.domain.FeedItem

@Composable
fun FeedScreen(viewModel: FeedViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedIds.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onScreenPaused() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onScreenResumed() }
    DisposableEffect(Unit) { onDispose { viewModel.onScreenPaused() } }

    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            FeedUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            FeedUiState.NoTheme -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(stringResource(R.string.feed_no_theme), color = Color.White)
            }
            FeedUiState.NoContent -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(stringResource(R.string.feed_no_content), color = Color.White)
            }
            is FeedUiState.NeedsChoice -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.feed_choose_topic), color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    LazyColumn {
                        items(s.topics) { topic ->
                            Text(
                                text = topic.title,
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onTopicChosen(topic.id) }
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }
            is FeedUiState.PastDayMissing -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(stringResource(R.string.feed_past_day_missing), color = Color.White)
            }
            is FeedUiState.Ready -> {
                key(s.date, s.topicId) {
                    val pagerState = rememberPagerState(pageCount = { s.items.size })
                    LaunchedEffect(pagerState.settledPage, s.items.size) {
                        viewModel.onPageSettled(pagerState.settledPage)
                    }
                    LaunchedEffect(Unit) {
                        viewModel.events.collect { e ->
                            when (e) {
                                is FeedEvent.ScrollTo -> pagerState.animateScrollToPage(e.index)
                                FeedEvent.BonusCapReached -> snackbarHostState.showSnackbar(context.getString(R.string.feed_bonus_cap))
                                FeedEvent.BonusNothingLeft -> snackbarHostState.showSnackbar(context.getString(R.string.feed_bonus_nothing))
                            }
                        }
                    }
                    FeedPager(
                        items = s.items,
                        savedIds = savedIds,
                        onSave = { id, saved -> viewModel.onSave(id, saved) },
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun FeedPager(
    items: List<FeedItem>,
    savedIds: Set<String>,
    onSave: (String, Boolean) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { items.size })
    Box(Modifier.fillMaxSize()) {
        VerticalPager(state = pagerState, key = { items[it].discovery.id }) { page ->
            val item = items[page]
            val d = item.discovery
            val isSaved = d.id in savedIds
            Box(Modifier.fillMaxSize()) {
                DiscoveryCard(
                    d = d,
                    saved = isSaved,
                    onSave = { onSave(d.id, !isSaved) },
                    onQuizAnswered = {},
                )
                if (item.isRevisit) {
                    Text(
                        text = stringResource(R.string.feed_bonus_revisit),
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(top = 20.dp, end = 16.dp)
                            .align(Alignment.TopEnd)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF6366F1))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
        SegmentedProgress(
            count = items.size,
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
