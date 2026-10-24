package com.example.curio.data.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    /** Returns -1 when the badge is already unlocked (IGNORE): that is the duplicate protection. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlock(entity: AchievementUnlockEntity): Long

    @Query("SELECT * FROM achievement_unlock") suspend fun getAll(): List<AchievementUnlockEntity>
    @Query("SELECT * FROM achievement_unlock") fun observeAll(): Flow<List<AchievementUnlockEntity>>

    /** The banner queue: unlocked but not yet announced, oldest first. */
    @Query("SELECT * FROM achievement_unlock WHERE notifiedAt IS NULL ORDER BY unlockedAt, achievementId")
    fun observePending(): Flow<List<AchievementUnlockEntity>>

    @Query("UPDATE achievement_unlock SET notifiedAt = :at WHERE achievementId = :id AND notifiedAt IS NULL")
    suspend fun markNotified(id: String, at: Long): Int

    @Query("UPDATE achievement_unlock SET seenAt = :at WHERE seenAt IS NULL")
    suspend fun markAllSeen(at: Long): Int

    @Query("SELECT COUNT(*) FROM achievement_unlock WHERE seenAt IS NULL")
    fun observeUnseenCount(): Flow<Int>

    @Query("DELETE FROM achievement_unlock") suspend fun deleteAll()
}
