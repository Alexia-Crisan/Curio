package com.example.curio.data.user

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.curio.domain.ContentIndex
import com.example.curio.domain.DailyMode
import com.example.curio.domain.DayPlan
import com.example.curio.domain.DwellTracker
import com.example.curio.domain.ShakeBonusSelector
import com.example.curio.domain.TestContent
import com.example.curio.domain.TouchSink
import com.example.curio.domain.ViewSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class FeedBackendFlowTest {
    private lateinit var db: CurioDatabase
    private lateinit var clock: Clock
    private lateinit var dayPlans: DayPlanRepository
    private lateinit var bonus: BonusRepository
    private lateinit var progress: ProgressRepository
    private lateinit var index: ContentIndex
    private lateinit var sink: TouchSink
    private val animalsTopics = listOf("t1", "t2", "t3", "t4")
    private val date = LocalDate.of(2026, 10, 26)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), CurioDatabase::class.java).build()
        clock = Clock.fixed(Instant.parse("2026-10-26T09:00:00Z"), ZoneId.of("Europe/Bucharest"))
        dayPlans = DayPlanRepository(db, clock)
        bonus = BonusRepository(db, TestContent.discoveries, clock)
        index = TestContent.index
        progress = ProgressRepository(db, index.membership)
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

    private suspend fun viewCards(dwellMs: Long): String {
        val plan = dayPlans.ensureToday("animals", animalsTopics, DailyMode.GUIDED) as DayPlan.Ready
        val topic = plan.choice.topicId
        val discoveries = index.discoveriesOf(topic)
        val tracker = DwellTracker(date)
        var t = 10_000L
        for (d in discoveries) {
            val shows = tracker.show(d.id, ViewSource.FEED, t)
            shows.forEach { sink.record(it) }
            t += dwellMs
            val leaves = tracker.leave(t)
            leaves.forEach { sink.record(it) }
            t += 100
        }
        return topic
    }

    @Test
    fun sixCardsViewedForThreeSecondsCompleteTheDay() = runBlocking {
        viewCards(3_000L)
        val monthProgress = progress.observeMonth(YearMonth.of(2026, 10)).first()
        assertEquals(1, monthProgress.daysCompleted)
    }

    @Test
    fun sixQuickSwipesDoNotCompleteTheDay() = runBlocking {
        viewCards(1_000L)
        val monthProgress = progress.observeMonth(YearMonth.of(2026, 10)).first()
        assertEquals(0, monthProgress.daysCompleted)
        val dayProg = monthProgress.days[date]
        assertNotNull(dayProg)
        assertEquals(0, dayProg!!.completed)
        val events = db.viewEventDao().getAll()
        assertTrue(events.all { it.skipped })
    }

    @Test
    fun fiveCompleteCardsAreInProgress() = runBlocking {
        val plan = dayPlans.ensureToday("animals", animalsTopics, DailyMode.GUIDED) as DayPlan.Ready
        val topic = plan.choice.topicId
        val discoveries = index.discoveriesOf(topic)
        val tracker = DwellTracker(date)
        var t = 10_000L
        for ((i, d) in discoveries.withIndex()) {
            val dwell = if (i < 5) 3_000L else 1_000L
            tracker.show(d.id, ViewSource.FEED, t).forEach { sink.record(it) }
            t += dwell
            tracker.leave(t).forEach { sink.record(it) }
            t += 100
        }
        val monthProgress = progress.observeMonth(YearMonth.of(2026, 10)).first()
        assertEquals(0, monthProgress.daysCompleted)
        val dayProg = monthProgress.days[date]
        assertNotNull(dayProg)
        assertEquals(5, dayProg!!.completed)
    }

    @Test
    fun aPastDayPeekCreatesNothing() = runBlocking {
        val pastDate = date.minusDays(1)
        val res = dayPlans.peek(pastDate)
        assertNull(res)
        assertEquals(0, count("day_choice"))
    }

    @Test
    fun aPastDayPeekReturnsTheStoredChoice() = runBlocking {
        dayPlans.ensureToday("animals", animalsTopics, DailyMode.GUIDED)
        val nextClock = Clock.fixed(Instant.parse("2026-10-27T09:00:00Z"), ZoneId.of("Europe/Bucharest"))
        val nextDayPlans = DayPlanRepository(db, nextClock)
        val choice = nextDayPlans.peek(date)
        assertNotNull(choice)
        assertEquals(1, count("day_choice"))
    }

    @Test
    fun bonusCardsSurviveARestart() = runBlocking {
        dayPlans.ensureToday("animals", animalsTopics, DailyMode.GUIDED)
        val r1 = BonusRepository(db, TestContent.discoveries, clock)
        val res = r1.reveal("animals", "t1")
        assertTrue(res is ShakeBonusSelector.Result.Found)
        val r2 = BonusRepository(db, TestContent.discoveries, clock)
        assertEquals(1, r2.todaysBonusCards().size)
    }

    @Test
    fun aBonusCardNeverCompletesTheDay() = runBlocking {
        val plan = dayPlans.ensureToday("animals", animalsTopics, DailyMode.GUIDED) as DayPlan.Ready
        val topic = plan.choice.topicId
        val discoveries = index.discoveriesOf(topic)
        val tracker = DwellTracker(date)
        var t = 10_000L
        for (i in 0 until 4) {
            val d = discoveries[i]
            tracker.show(d.id, ViewSource.FEED, t).forEach { sink.record(it) }
            t += 3_000L
            tracker.leave(t).forEach { sink.record(it) }
            t += 100
        }
        repeat(2) {
            val bonusRes = bonus.reveal("animals", topic) as ShakeBonusSelector.Result.Found
            tracker.show(bonusRes.discovery.id, ViewSource.BONUS, t).forEach { sink.record(it) }
            t += 3_000L
            tracker.leave(t).forEach { sink.record(it) }
            t += 100
        }
        val monthProgress = progress.observeMonth(YearMonth.of(2026, 10)).first()
        assertEquals(0, monthProgress.daysCompleted)
    }

    @Test
    fun theFirstShowCreatesARowBeforeAnyDwell() = runBlocking {
        val tracker = DwellTracker(date)
        val shows = tracker.show("a1", ViewSource.FEED, 10_000L)
        shows.forEach { sink.record(it) }
        assertEquals(1, count("view_event"))
        val rows = db.viewEventDao().getAll()
        assertEquals(1, rows.size)
        assertFalse(rows[0].completed)
    }
}
