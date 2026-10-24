package com.example.curio.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.curio.domain.AchievementConfig
import com.example.curio.domain.AchievementDef
import com.example.curio.domain.AchievementId
import com.example.curio.domain.AchievementProgress
import com.example.curio.domain.AchievementSource

class FakeAchievementSource : AchievementSource {
    val progress = MutableStateFlow<List<AchievementProgress>>(emptyList())
    val pending = MutableStateFlow<AchievementDef?>(null)
    val unseen = MutableStateFlow(0)
    val notified = mutableListOf<AchievementId>()
    var seenCalls = 0

    override fun observeProgress(): Flow<List<AchievementProgress>> = progress
    override fun observeNextPending(): Flow<AchievementDef?> = pending
    override fun observeUnseenCount(): Flow<Int> = unseen

    override suspend fun markNotified(id: AchievementId) {
        notified += id
        if (pending.value?.id == id) pending.value = null
    }

    override suspend fun markAllSeen() {
        seenCalls++
        unseen.value = 0
    }
}

fun badge(id: AchievementId, current: Int, unlockedAt: Long? = null) =
    AchievementProgress(AchievementConfig.defs.first { it.id == id }, current, unlockedAt)

fun def(id: AchievementId) = AchievementConfig.defs.first { it.id == id }
