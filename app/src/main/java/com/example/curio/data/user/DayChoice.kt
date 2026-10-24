package com.example.curio.data.user

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.example.curio.domain.DailyMode
import com.example.curio.domain.DayChoice
import java.time.LocalDate

@Entity(tableName = "day_choice")
data class DayChoiceEntity(
    @PrimaryKey val date: String,
    val themeId: String,
    val topicId: String,
    val mode: String,
)

fun DayChoiceEntity.toDomain() = DayChoice(LocalDate.parse(date), themeId, topicId, DailyMode.valueOf(mode))
fun DayChoice.toEntity() = DayChoiceEntity(date.toString(), themeId, topicId, mode.name)

@Dao
interface DayChoiceDao {
    @Query("SELECT * FROM day_choice WHERE date = :date")
    suspend fun find(date: String): DayChoiceEntity?

    @Upsert
    suspend fun upsert(entity: DayChoiceEntity)

    @Query("DELETE FROM day_choice WHERE date = :date")
    suspend fun delete(date: String)

    @Query("SELECT * FROM day_choice")
    fun observeAll(): Flow<List<DayChoiceEntity>>

    @Query("SELECT * FROM day_choice")
    suspend fun getAll(): List<DayChoiceEntity>

    @Query("DELETE FROM day_choice")
    suspend fun deleteAll()
}
