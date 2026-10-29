package com.example.curio.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.curio.data.user.UserActivityRepository
import com.example.curio.domain.ContentIndex
import com.example.curio.domain.Discovery
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HistoryDay(val date: LocalDate, val items: List<Discovery>)

class LibraryViewModel(
    activity: UserActivityRepository,
    content: ContentIndex,
) : ViewModel() {
    val saved: StateFlow<List<Discovery>> = activity.observeSavedIds()
        .map { ids -> ids.mapNotNull(content::discovery) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val history: StateFlow<List<HistoryDay>> =
        activity.observeRecentViews(50)
            .map { events ->
                events.groupBy { it.date }.map { (date, evs) ->
                    HistoryDay(date, evs.mapNotNull { content.discovery(it.discoveryId) })
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )
}
