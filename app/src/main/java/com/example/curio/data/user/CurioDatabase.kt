package com.example.curio.data.user

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ViewEventEntity::class,
        DayChoiceEntity::class,
        SavedDiscoveryEntity::class,
        AchievementUnlockEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class CurioDatabase : RoomDatabase() {
    abstract fun viewEventDao(): ViewEventDao
    abstract fun dayChoiceDao(): DayChoiceDao
    abstract fun savedDao(): SavedDao
    abstract fun achievementDao(): AchievementDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE saved_discovery ADD COLUMN note TEXT")
            }
        }

        fun create(context: Context): CurioDatabase =
            Room.databaseBuilder(context.applicationContext, CurioDatabase::class.java, "curio.db")
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
    }
}
