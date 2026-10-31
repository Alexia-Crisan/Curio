package com.example.curio.feature.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.curio.data.user.ProgressRepository
import com.example.curio.data.user.UserActivityRepository
import com.example.curio.domain.MonthProgress
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(
    progress: ProgressRepository,
    activity: UserActivityRepository,
    private val clock: java.time.Clock,
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now(clock))
    val monthProgress: StateFlow<MonthProgress?> = month
        .flatMapLatest { progress.observeMonth(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val savedCount: StateFlow<Int> = activity.observeSavedIds()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    fun today(): LocalDate = LocalDate.now(clock)
    fun previousMonth() = month.update { it.minusMonths(1) }
    fun nextMonth() = month.update { if (it < YearMonth.now(clock)) it.plusMonths(1) else it }
}
