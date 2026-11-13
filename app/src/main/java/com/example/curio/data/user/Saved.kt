package com.example.curio.data.user

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_discovery")
data class SavedDiscoveryEntity(
    @PrimaryKey val discoveryId: String,
    val savedAt: Long,
    val note: String? = null,
)

data class SavedNoteRow(val discoveryId: String, val note: String)

@Dao
interface SavedDao {
    @Upsert suspend fun upsert(entity: SavedDiscoveryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: SavedDiscoveryEntity)

    @Query("UPDATE saved_discovery SET note = :note WHERE discoveryId = :id")
    suspend fun updateNote(id: String, note: String?): Int

    @Query("SELECT discoveryId FROM saved_discovery ORDER BY savedAt DESC, discoveryId ASC")
    fun observeIdsNewestFirst(): Flow<List<String>>

    @Query("SELECT discoveryId, note FROM saved_discovery WHERE note IS NOT NULL")
    fun observeNotes(): Flow<List<SavedNoteRow>>

    @Query("DELETE FROM saved_discovery WHERE discoveryId = :id")
    suspend fun delete(id: String)

    @Query("SELECT discoveryId FROM saved_discovery")
    fun observeIds(): Flow<List<String>>

    @Query("SELECT discoveryId FROM saved_discovery")
    suspend fun getIds(): List<String>

    @Query("DELETE FROM saved_discovery")
    suspend fun deleteAll()
}
