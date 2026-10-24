package com.example.curio.data.user

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ViewEventEntity::class,
        AchievementUnlockEntity::class,
        DayChoiceEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class CurioDatabase : RoomDatabase() {
    abstract fun viewEventDao(): ViewEventDao
    abstract fun dayChoiceDao(): DayChoiceDao

    companion object {
        fun create(context: Context): CurioDatabase =
            Room.databaseBuilder(context.applicationContext, CurioDatabase::class.java, "curio.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
