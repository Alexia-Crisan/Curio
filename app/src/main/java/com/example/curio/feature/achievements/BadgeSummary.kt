package com.example.curio.feature.achievements

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import com.example.curio.domain.AchievementSource

data class BadgeSummary(val unlocked: Int, val total: Int, val hasNew: Boolean)

fun AchievementSource.observeSummary(): Flow<BadgeSummary> =
    combine(observeProgress(), observeUnseenCount()) { progress, unseen ->
        val shown = progress.filter { AchievementVisuals.of(it.def.id) != null }
        BadgeSummary(shown.count { it.isUnlocked }, shown.size, hasNew = unseen > 0)
    }.distinctUntilChanged()
