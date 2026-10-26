package com.example.curio.feature.feed

import com.example.curio.domain.FeedItem
import com.example.curio.domain.Topic
import java.time.LocalDate

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data object NoTheme : FeedUiState
    data object NoContent : FeedUiState
    data class NeedsChoice(val topics: List<Topic>) : FeedUiState
    data class PastDayMissing(val date: LocalDate) : FeedUiState
    data class Ready(
        val date: LocalDate,
        val themeId: String,
        val topicId: String,
        val items: List<FeedItem>,
        val readOnly: Boolean,
        val settingsLocked: Boolean,
    ) : FeedUiState
}

sealed interface FeedEvent {
    data class ScrollTo(val index: Int) : FeedEvent
    data object BonusCapReached : FeedEvent
    data object BonusNothingLeft : FeedEvent
}
