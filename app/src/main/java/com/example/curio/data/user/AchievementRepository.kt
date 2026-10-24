package com.example.curio.data.user

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.example.curio.domain.AchievementConfig
import com.example.curio.domain.AchievementDef
import com.example.curio.domain.AchievementEvaluator
import com.example.curio.domain.AchievementId
import com.example.curio.domain.AchievementProgress
import com.example.curio.domain.AchievementSource
import com.example.curio.domain.ContentIndex
import java.time.Clock

class AchievementRepository(
    private val db: CurioDatabase,
    private val activity: UserActivityRepository,
    private val content: ContentIndex,
    private val clock: Clock,
    private val defs: List<AchievementDef> = AchievementConfig.defs.filter { it.mvp },   // should-have badges stay off
) : AchievementSource {
    private val dao = db.achievementDao()

    private fun List<AchievementUnlockEntity>.toUnlockedMap(): Map<AchievementId, Long> =
        mapNotNull { row -> AchievementId.entries.firstOrNull { it.name == row.achievementId }?.let { it to row.unlockedAt } }
            .toMap()

    override fun observeProgress(): Flow<List<AchievementProgress>> =
        combine(activity.observeSnapshot(), dao.observeAll()) { snapshot, rows ->
            AchievementEvaluator.evaluate(snapshot, content, defs, rows.toUnlockedMap()).progress
        }.distinctUntilChanged()

    override fun observeNextPending(): Flow<AchievementDef?> =
        dao.observePending()
            .map { rows -> rows.firstNotNullOfOrNull { r -> defs.firstOrNull { it.id.name == r.achievementId } } }
            .distinctUntilChanged()

    override fun observeUnseenCount(): Flow<Int> = dao.observeUnseenCount()

    override suspend fun markNotified(id: AchievementId) { dao.markNotified(id.name, clock.millis()) }
    override suspend fun markAllSeen() { dao.markAllSeen(clock.millis()) }

    /**
     * Evaluate against the latest committed data and persist every newly earned badge, in one transaction
     */
    suspend fun evaluateNow(): List<AchievementId> = db.withTransaction {
        val snapshot = activity.currentSnapshot()
        val unlocked = dao.getAll().toUnlockedMap()
        val evaluation = AchievementEvaluator.evaluate(snapshot, content, defs, unlocked)
        val now = clock.millis()
        evaluation.newlyEarned.mapIndexedNotNull { i, def ->
            val inserted = dao.insertUnlock(AchievementUnlockEntity(def.id.name, now + i, null, null))
            if (inserted != -1L) def.id else null
        }
    }
}
