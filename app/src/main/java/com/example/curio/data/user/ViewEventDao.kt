package com.example.curio.data.user

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.example.curio.domain.ViewEventRecorder
import com.example.curio.domain.ViewTouch

@Dao
abstract class ViewEventDao {
    @Query("SELECT * FROM view_event WHERE discoveryId = :id AND date = :date")
    abstract suspend fun find(id: String, date: String): ViewEventEntity?

    @Upsert
    abstract suspend fun upsert(entity: ViewEventEntity)

    @Query("SELECT * FROM view_event")
    abstract fun observeAll(): Flow<List<ViewEventEntity>>

    @Query("SELECT * FROM view_event")
    abstract suspend fun getAll(): List<ViewEventEntity>

    @Query("SELECT * FROM view_event WHERE date = :date AND source = 'BONUS' ORDER BY firstViewedAt, discoveryId")
    abstract suspend fun bonusRows(date: String): List<ViewEventEntity>

    @Query("SELECT COUNT(*) FROM view_event WHERE date = :date AND completed = 1 AND source = 'FEED'")
    abstract suspend fun completedFeedCount(date: String): Int

    @Query("DELETE FROM view_event")
    abstract suspend fun deleteAll()

    @Transaction
    open suspend fun record(touch: ViewTouch) {
        val existing = find(touch.discoveryId, touch.date.toString())?.toDomain()
        upsert(ViewEventRecorder.merge(existing, touch).toEntity())
    }
}
