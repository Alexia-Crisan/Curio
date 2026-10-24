package com.example.curio.data.user

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.example.curio.domain.DAY
import com.example.curio.domain.TestContent
import com.example.curio.domain.ViewSource.DETAIL
import com.example.curio.domain.ViewSource.FEED
import java.time.Clock
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class UserActivityRepositoryTest {
    private lateinit var db: CurioDatabase
    private lateinit var repo: UserActivityRepository
    private val clock = Clock.fixed(DAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), CurioDatabase::class.java).build()
        repo = UserActivityRepository(db, TestContent.index, clock)
    }

    @After fun tearDown() = db.close()

    @Test fun savingFromTheFeedCompletesTheCardAsFeed() = runBlocking {
        repo.setSaved("a1", true, FEED)
        val rows = db.viewEventDao().getAll()
        assertEquals(1, rows.size)
        assertEquals("FEED", rows[0].source)
        assertTrue(rows[0].completed)

        val snap = repo.currentSnapshot()
        assertTrue("a1" in snap.savedIds)
        assertTrue("a1" in snap.completedIds)
    }

    @Test fun savingFromDetailRecordsADetailRowThatIsNotAnActiveDay() = runBlocking {
        repo.setSaved("a1", true, DETAIL)
        val rows = db.viewEventDao().getAll()
        assertEquals("DETAIL", rows[0].source)
        assertTrue(repo.currentSnapshot().completedFeedDays.isEmpty())
    }

    @Test fun savingWithoutRecordingActivityWritesNoViewRow() = runBlocking {
        repo.setSaved("a1", true, FEED, recordActivity = false)
        assertTrue(db.viewEventDao().getAll().isEmpty())
        assertEquals(setOf("a1"), repo.observeSavedIds().first())
    }

    @Test fun unsavingKeepsTheCompletion() = runBlocking {
        repo.setSaved("a1", true, FEED)
        repo.setSaved("a1", false, FEED)
        assertTrue(repo.observeSavedIds().first().isEmpty())
        assertTrue("a1" in repo.currentSnapshot().completedIds)
    }

    @Test fun savingTwiceIsOneRow() = runBlocking {
        repo.setSaved("a1", true, FEED)
        repo.setSaved("a1", true, FEED)
        assertEquals(1, repo.observeSavedIds().first().size)
        assertEquals(1, db.viewEventDao().getAll().size)
    }

    @Test fun aSaveFromDetailNeverDowngradesAFeedRow() = runBlocking {
        db.viewEventDao().record(com.example.curio.domain.ViewTouch("a1", DAY, FEED, 1000, dwellDeltaMs = 500))
        repo.setSaved("a1", true, DETAIL)
        val row = db.viewEventDao().getAll().single()
        assertEquals("FEED", row.source)
    }

    @Test fun theSnapshotFlowEmitsWhenDataChanges() = runBlocking {
        repo.setSaved("b1", true, FEED)
        val snap = repo.observeSnapshot().first { "b1" in it.savedIds }
        assertTrue("b1" in snap.completedIds)
    }

    @Test fun resetClearsEverythingInOneGo() = runBlocking {
        repo.setSaved("a1", true, FEED)
        db.dayChoiceDao().upsert(DayChoiceEntity(DAY.toString(), "animals", "t1", "GUIDED"))
        db.achievementDao().insertUnlock(AchievementUnlockEntity("FIRST_DISCOVERY", 100, null, null))

        repo.resetAll()

        assertTrue(db.viewEventDao().getAll().isEmpty())
        assertTrue(db.dayChoiceDao().getAll().isEmpty())
        assertTrue(db.savedDao().getIds().isEmpty())
        assertTrue(db.achievementDao().getAll().isEmpty())
    }
}
