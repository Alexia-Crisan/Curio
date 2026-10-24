package com.example.curio.data.user

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AchievementDaoTest {
    private lateinit var db: CurioDatabase
    private lateinit var dao: AchievementDao

    private fun row(id: String, at: Long) = AchievementUnlockEntity(id, at, null, null)

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), CurioDatabase::class.java).build()
        dao = db.achievementDao()
    }

    @After fun tearDown() = db.close()

    @Test fun insertingTheSameBadgeTwiceKeepsOneRowAndTheFirstTimestamp() = runBlocking {
        val first = dao.insertUnlock(row("FIRST_DISCOVERY", 100))
        val second = dao.insertUnlock(row("FIRST_DISCOVERY", 999))
        assertNotEquals(-1L, first)
        assertEquals(-1L, second)
        val rows = dao.getAll()
        assertEquals(1, rows.size)
        assertEquals(100L, rows[0].unlockedAt)
    }

    @Test fun markNotifiedWorksOnlyOnce() = runBlocking {
        dao.insertUnlock(row("FIRST_DISCOVERY", 100))
        val firstCall = dao.markNotified("FIRST_DISCOVERY", 200)
        val secondCall = dao.markNotified("FIRST_DISCOVERY", 300)
        assertEquals(1, firstCall)
        assertEquals(0, secondCall)
        assertEquals(200L, dao.getAll().single().notifiedAt)
    }

    @Test fun pendingIsOldestFirstAndShrinksWhenNotified() = runBlocking {
        dao.insertUnlock(row("CURIOUS_MIND", 20))
        dao.insertUnlock(row("FIRST_DISCOVERY", 10))
        val pending = dao.observePending().first()
        assertEquals(listOf("FIRST_DISCOVERY", "CURIOUS_MIND"), pending.map { it.achievementId })

        dao.markNotified("FIRST_DISCOVERY", 100)
        val remaining = dao.observePending().first()
        assertEquals(listOf("CURIOUS_MIND"), remaining.map { it.achievementId })
    }

    @Test fun unseenCountDropsToZeroAfterMarkAllSeen() = runBlocking {
        dao.insertUnlock(row("FIRST_DISCOVERY", 10))
        dao.insertUnlock(row("CURIOUS_MIND", 20))
        assertEquals(2, dao.observeUnseenCount().first())

        val updated = dao.markAllSeen(99)
        assertEquals(2, updated)
        assertEquals(0, dao.observeUnseenCount().first())
    }

    @Test fun markAllSeenDoesNotTouchRowsAlreadySeen() = runBlocking {
        dao.insertUnlock(AchievementUnlockEntity("FIRST_DISCOVERY", 10, null, 5))
        val updated = dao.markAllSeen(99)
        assertEquals(0, updated)
        assertEquals(5L, dao.getAll().single().seenAt)
    }
}
