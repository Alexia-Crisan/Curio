package com.example.curio.data.user

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.example.curio.domain.ViewSource
import com.example.curio.domain.ViewTouch
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ViewEventDaoTest {
    private lateinit var db: CurioDatabase
    private lateinit var dao: ViewEventDao
    private val day = LocalDate.of(2026, 10, 26)

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), CurioDatabase::class.java).build()
        dao = db.viewEventDao()
    }
    @After fun tearDown() = db.close()

    @Test fun twoTouchesBecomeOneRow() = runBlocking {
        dao.record(ViewTouch("discovery_001", day, ViewSource.FEED, nowMs = 1_000, dwellDeltaMs = 1_000))
        dao.record(ViewTouch("discovery_001", day, ViewSource.FEED, nowMs = 5_000, dwellDeltaMs = 2_500))
        val rows = dao.observeAll().first()
        assertEquals(1, rows.size)
        assertEquals(3_500, rows[0].dwellMs)
        assertEquals(1_000, rows[0].firstViewedAt)
        assertTrue(rows[0].completed)
    }

    @Test fun sameDiscoveryOnTwoDatesIsTwoRows() = runBlocking {
        dao.record(ViewTouch("discovery_001", day, ViewSource.FEED, 1_000, markCompleted = true))
        dao.record(ViewTouch("discovery_001", day.plusDays(1), ViewSource.FEED, 2_000, markCompleted = true))
        assertEquals(2, dao.observeAll().first().size)
    }

    @Test fun detailAfterFeedKeepsFeedSource() = runBlocking {
        dao.record(ViewTouch("discovery_001", day, ViewSource.FEED, 1_000, dwellDeltaMs = 500))
        dao.record(ViewTouch("discovery_001", day, ViewSource.DETAIL, 2_000, detailDwellDeltaMs = 8_000, markCompleted = true))
        val row = dao.observeAll().first().single()
        assertEquals("FEED", row.source)
        assertEquals(8_000, row.detailDwellMs)
        assertTrue(row.completed)
    }

    @Test fun bonusRowSurvivesAsBonus() = runBlocking {
        dao.record(ViewTouch("discovery_050", day, ViewSource.BONUS, 1_000))
        assertEquals("BONUS", dao.observeAll().first().single().source)
    }
}
