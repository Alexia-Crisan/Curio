package com.example.curio.data.user

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import com.example.curio.domain.ActivitySnapshot
import com.example.curio.domain.ActivitySnapshotBuilder
import com.example.curio.domain.ActivitySource
import com.example.curio.domain.ContentIndex
import com.example.curio.domain.ViewEvent
import com.example.curio.domain.ViewSource
import com.example.curio.domain.ViewTouch
import java.time.Clock
import java.time.LocalDate

class UserActivityRepository(
    private val db: CurioDatabase,
    private val content: ContentIndex,
    private val clock: Clock,
) : ActivitySource {
    private val views = db.viewEventDao()
    private val savedDao = db.savedDao()
    private val choices = db.dayChoiceDao()
    private val unlocks = db.achievementDao()

    private fun build(v: List<ViewEventEntity>, s: List<String>, c: List<DayChoiceEntity>): ActivitySnapshot =
        ActivitySnapshotBuilder.build(v.map { it.toDomain() }, s.toSet(), c.map { it.toDomain() }, content)

    override fun observeSnapshot(): Flow<ActivitySnapshot> =
        combine(views.observeAll(), savedDao.observeIds(), choices.observeAll()) { v, s, c -> build(v, s, c) }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    suspend fun currentSnapshot(): ActivitySnapshot = build(views.getAll(), savedDao.getIds(), choices.getAll())

    fun observeSavedIds(): Flow<Set<String>> = savedDao.observeIds().map { it.toSet() }

    suspend fun setSaved(
        discoveryId: String,
        saved: Boolean,
        source: ViewSource,
        date: LocalDate = LocalDate.now(clock),
        recordActivity: Boolean = true,
    ) = db.withTransaction {
        if (saved) {
            savedDao.upsert(SavedDiscoveryEntity(discoveryId, clock.millis()))
            if (recordActivity) {
                views.record(ViewTouch(discoveryId, date, source, clock.millis(), markCompleted = true))
            }
        } else {
            savedDao.delete(discoveryId)
        }
    }

    suspend fun resetAll() = db.withTransaction {
        views.deleteAll()
        choices.deleteAll()
        savedDao.deleteAll()
        unlocks.deleteAll()
    }

    fun observeRecentViews(limit: Int = 50): Flow<List<ViewEvent>> = views.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }
}
