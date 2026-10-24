package com.example.curio.data.user

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import com.example.curio.domain.MonthProgress
import com.example.curio.domain.ProgressCalculator
import com.example.curio.domain.TopicMembership
import java.time.YearMonth

class ProgressRepository(
    private val db: CurioDatabase,
    private val membership: TopicMembership,
) {
    /** Recomputed whenever a view event or a day choice changes. */
    fun observeMonth(month: YearMonth): Flow<MonthProgress> =
        combine(db.viewEventDao().observeAll(), db.dayChoiceDao().observeAll()) { events, choices ->
            ProgressCalculator.monthProgress(
                month,
                events.map { it.toDomain() },
                choices.map { it.toDomain() },
                membership,
            )
        }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)
}
