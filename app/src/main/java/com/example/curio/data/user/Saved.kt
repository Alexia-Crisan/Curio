package com.example.curio.data.user

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_discovery")
data class SavedDiscoveryEntity(@PrimaryKey val discoveryId: String, val savedAt: Long)

@Dao
interface SavedDao {
    @Upsert suspend fun upsert(entity: SavedDiscoveryEntity)

    @Query("DELETE FROM saved_discovery WHERE discoveryId = :id")
    suspend fun delete(id: String)

    @Query("SELECT discoveryId FROM saved_discovery")
    fun observeIds(): Flow<List<String>>

    @Query("SELECT discoveryId FROM saved_discovery")
    suspend fun getIds(): List<String>

    @Query("DELETE FROM saved_discovery")
    suspend fun deleteAll()
}
