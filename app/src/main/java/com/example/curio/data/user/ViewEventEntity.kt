package com.example.curio.data.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "view_event", primaryKeys = ["discoveryId", "date"])
data class ViewEventEntity(
    val discoveryId: String,
    val date: String,                 // ISO local date of the FEED this event belongs to
    val firstViewedAt: Long,
    val dwellMs: Long,
    val completed: Boolean,
    val skipped: Boolean,
    @ColumnInfo(defaultValue = "FEED") val source: String = "FEED",
    @ColumnInfo(defaultValue = "0") val detailDwellMs: Long = 0,
)

/** Created now so the schema is final; the DAO and tracker come in a later step. */
@Entity(tableName = "achievement_unlock")
data class AchievementUnlockEntity(
    @PrimaryKey val achievementId: String,
    val unlockedAt: Long,
    val notifiedAt: Long?,
    val seenAt: Long?,
)
