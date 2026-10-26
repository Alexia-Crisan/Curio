package com.example.curio.data.user

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.curio.domain.DailyMode
import com.example.curio.domain.DayPlan
import com.example.curio.domain.DwellTracker
import com.example.curio.domain.TestContent
import com.example.curio.domain.TouchSink
import com.example.curio.domain.ViewSource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class ResetProgressTest {
    private lateinit var db: CurioDatabase
    private lateinit var clock: Clock
    private lateinit var dayPlans: DayPlanRepository
    private lateinit var userActivity: UserActivityRepository
    private lateinit var sink: TouchSink
    private val animalsTopics = listOf("t1", "t2", "t3", "t4")
    private val date = LocalDate.of(2026, 10, 26)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), CurioDatabase::class.java).build()
        clock = Clock.fixed(Instant.parse("2026-10-26T09:00:00Z"), ZoneId.of("Europe/Bucharest"))
        dayPlans = DayPlanRepository(db, clock)
        userActivity = UserActivityRepository(db, TestContent.index, clock)
        sink = TouchSink { db.viewEventDao().record(it) }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun count(table: String): Int {
        return db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    @Test
    fun resetEmptiesEveryUserTable() = runBlocking {
        val plan = dayPlans.ensureToday("animals", animalsTopics, DailyMode.GUIDED) as DayPlan.Ready
        val discoveries = TestContent.index.discoveriesOf(plan.choice.topicId)
        val tracker = DwellTracker(date)
        var t = 10_000L
        for (d in discoveries) {
            tracker.show(d.id, ViewSource.FEED, t).forEach { sink.record(it) }
            t += 3_000L
            tracker.leave(t).forEach { sink.record(it) }
        }
        userActivity.setSaved("a1", true, ViewSource.FEED)

        userActivity.resetAll()

        assertEquals(0, count("view_event"))
        assertEquals(0, count("day_choice"))
        assertEquals(0, count("saved_discovery"))
        assertEquals(0, count("achievement_unlock"))
    }

    @Test
    fun resetLeavesTheSnapshotEmpty() = runBlocking {
        userActivity.setSaved("a1", true, ViewSource.FEED)
        userActivity.resetAll()
        val snapshot = userActivity.currentSnapshot()
        assertTrue(snapshot.completedIds.isEmpty())
        assertTrue(snapshot.savedIds.isEmpty())
    }

    @Test
    fun theFeedPlanCanBeCreatedAgainAfterAReset() = runBlocking {
        dayPlans.ensureToday("animals", animalsTopics, DailyMode.GUIDED)
        userActivity.resetAll()
        val plan = dayPlans.ensureToday("animals", animalsTopics, DailyMode.GUIDED)
        assertTrue(plan is DayPlan.Ready)
        assertEquals(1, count("day_choice"))
    }

    @Test
    fun resetIsAllOrNothingOnTheWriteSide() = runBlocking {
        userActivity.resetAll()
        userActivity.resetAll()
        assertEquals(0, count("view_event"))
    }
}
