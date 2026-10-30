package com.example.curio.data.user

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class FreshnessRepositoryTest {
    private lateinit var db: CurioDatabase
    private lateinit var repo: FreshnessRepository
    private val D = LocalDate.of(2026, 10, 26)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), CurioDatabase::class.java).build()
        repo = FreshnessRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun done(id: String, date: LocalDate, source: ViewSource = ViewSource.FEED) {
        db.viewEventDao().record(ViewTouch(id, date, source, 1000L, markCompleted = true))
    }

    private suspend fun seen(id: String, date: LocalDate) {
        db.viewEventDao().record(ViewTouch(id, date, ViewSource.FEED, 1000L, dwellDeltaMs = 500L, markCompleted = false))
    }

    @Test
    fun emptyOnAFreshDatabase() = runBlocking {
        assertTrue(repo.seenBefore(D).isEmpty())
    }

    @Test
    fun onlyCompletedRowsCount() = runBlocking {
        seen("a1", D.minusDays(1))
        done("a2", D.minusDays(1))
        assertEquals(setOf("a2"), repo.seenBefore(D))
    }

    @Test
    fun onlyEarlierDatesCount() = runBlocking {
        done("a1", D.minusDays(1))
        done("a2", D)
        done("a3", D.plusDays(1))
        assertEquals(setOf("a1"), repo.seenBefore(D))
    }

    @Test
    fun theFeedDateItselfIsExcluded() = runBlocking {
        done("a1", D)
        assertTrue(repo.seenBefore(D).isEmpty())
    }

    @Test
    fun anySourceCounts() = runBlocking {
        done("a1", D.minusDays(2), ViewSource.FEED)
        done("a2", D.minusDays(2), ViewSource.BONUS)
        done("a3", D.minusDays(2), ViewSource.DETAIL)
        assertEquals(setOf("a1", "a2", "a3"), repo.seenBefore(D))
    }

    @Test
    fun aCardSeenOnTwoEarlierDaysAppearsOnce() = runBlocking {
        done("a1", D.minusDays(2))
        done("a1", D.minusDays(1))
        assertEquals(setOf("a1"), repo.seenBefore(D))
    }

    @Test
    fun monthAndYearBoundariesAreCorrect() = runBlocking {
        done("a1", LocalDate.of(2026, 9, 30))
        done("a2", LocalDate.of(2025, 12, 31))
        done("a3", LocalDate.of(2026, 10, 1))
        assertEquals(setOf("a1", "a2"), repo.seenBefore(LocalDate.of(2026, 10, 1)))
    }

    @Test
    fun leapDayIsHandled() = runBlocking {
        done("a1", LocalDate.of(2028, 2, 29))
        assertEquals(setOf("a1"), repo.seenBefore(LocalDate.of(2028, 3, 1)))
        assertTrue(repo.seenBefore(LocalDate.of(2028, 2, 29)).isEmpty())
    }

    @Test
    fun completingTodayDoesNotChangeTodaysAnswer() = runBlocking {
        done("a1", D.minusDays(1))
        val before = repo.seenBefore(D)
        done("a2", D)
        assertEquals(before, repo.seenBefore(D))
    }

    @Test
    fun aPastDayUsesItsOwnHistory() = runBlocking {
        done("a1", D.minusDays(3))
        done("a2", D.minusDays(1))
        assertEquals(setOf("a1"), repo.seenBefore(D.minusDays(2)))
        assertEquals(setOf("a1", "a2"), repo.seenBefore(D))
    }
}
