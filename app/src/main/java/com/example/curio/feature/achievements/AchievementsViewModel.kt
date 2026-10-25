package com.example.curio.feature.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.curio.domain.AchievementId
import com.example.curio.domain.AchievementSource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class AchievementsUiState(
    val items: List<AchievementUi> = emptyList(),
    val selected: AchievementUi? = null,
) {
    val unlockedCount: Int get() = items.count { it.isUnlocked }
    val total: Int get() = items.size
}

private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())
private fun defaultFormat(epochMs: Long): String = dateFormat.format(Instant.ofEpochMilli(epochMs))

class AchievementsViewModel(
    private val source: AchievementSource,
    private val formatDate: (Long) -> String = ::defaultFormat,
) : ViewModel() {

    private val selectedId = MutableStateFlow<AchievementId?>(null)

    val state: StateFlow<AchievementsUiState> =
        combine(source.observeProgress(), selectedId) { progress, selected ->
            val items = progress.mapNotNull { it.toUi(formatDate) }
            AchievementsUiState(items, items.firstOrNull { it.id == selected })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AchievementsUiState())

    init {
        viewModelScope.launch {
            source.observeUnseenCount().filter { it > 0 }.collect { source.markAllSeen() }
        }
    }

    fun onBadgeClick(id: AchievementId) { selectedId.value = id }
    fun onDismissSheet() { selectedId.value = null }
}
