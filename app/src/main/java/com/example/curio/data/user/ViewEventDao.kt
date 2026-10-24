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
    abstract fun observeAll(): Flow<List<ViewEventEntity>>   // a few hundred rows at most

    /** Only regular feed cards lock the day. Bonus and Detail-only rows do not. */
    @Query("SELECT COUNT(*) FROM view_event WHERE date = :date AND completed = 1 AND source = 'FEED'")
    abstract suspend fun completedFeedCount(date: String): Int

    /** The only way the app writes view events. Read-merge-write is one transaction. */
    @Transaction
    open suspend fun record(touch: ViewTouch) {
        val existing = find(touch.discoveryId, touch.date.toString())?.toDomain()
        upsert(ViewEventRecorder.merge(existing, touch).toEntity())
    }
}
