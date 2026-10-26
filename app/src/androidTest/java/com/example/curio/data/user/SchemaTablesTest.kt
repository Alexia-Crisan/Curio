package com.example.curio.data.user

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SchemaTablesTest {
    private lateinit var db: CurioDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), CurioDatabase::class.java).build()
        db.openHelper.writableDatabase
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun verifyTablesExist() {
        val tables = mutableSetOf<String>()
        db.openHelper.readableDatabase.query("SELECT name FROM sqlite_master WHERE type='table'").use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0)
                if (!name.startsWith("android_") && !name.startsWith("sqlite_") && name != "room_master_table") {
                    tables.add(name)
                }
            }
        }
        val expected = setOf("view_event", "day_choice", "saved_discovery", "achievement_unlock")
        assertEquals(expected, tables)
    }
}
