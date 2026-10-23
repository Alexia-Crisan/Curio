package com.example.curio.data.user

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ViewEventEntity::class,
        AchievementUnlockEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class CurioDatabase : RoomDatabase() {
    abstract fun viewEventDao(): ViewEventDao
}
