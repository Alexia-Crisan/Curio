package com.example.curio.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.example.curio.data.user.CurioDatabase
import com.example.curio.data.user.DayPlanRepository
import com.example.curio.data.user.FreshnessRepository
import com.example.curio.data.user.ProgressRepository
import com.example.curio.data.user.UserActivityRepository
import com.example.curio.domain.DailyMode
import com.example.curio.domain.DayPlan
import com.example.curio.domain.DwellTracker
import com.example.curio.domain.FeedAssembler
import com.example.curio.domain.TestContent
import com.example.curio.domain.ViewSource
import com.example.curio.domain.ViewTouch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class FreshFirstFlowTest {
    private lateinit var db: CurioDatabase
    private lateinit var clock: Clock
    private val index = TestContent.index
    private val animalsTopics = listOf("t1", "t2", "t3", "t4")
    private val date = LocalDate.of(2026, 10, 26)
    private val zone = ZoneId.of("Europe/Bucharest")

    private lateinit var dayPlans: DayPlanRepository
    private lateinit var progress: ProgressRepository
    private lateinit var userActivity: UserActivityRepository
    private lateinit var freshness: FreshnessRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), CurioDatabase::class.java).build()
        clock = Clock.fixed(Instant.parse("2026-10-26T09:00:00Z"), zone)
        createRepos()
    }

    private fun createRepos() {
        dayPlans = DayPlanRepository(db, clock)
        progress = ProgressRepository(db, index.membership)
        userActivity = UserActivityRepository(db, index, clock)
        freshness = FreshnessRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun doneOnDate(id: String, d: LocalDate) {
        db.viewEventDao().record(ViewTouch(id, d, ViewSource.FEED, 1000L, markCompleted = true))
    }

    private suspend fun watch(ids: List<String>, dwellMs: Long, startMs: Long = 10_000L) {
        val tracker = DwellTracker(date)
        var t = startMs
        for (id in ids) {
            tracker.show(id, ViewSource.FEED, t).forEach { db.viewEventDao().record(it) }
            t += dwellMs
            tracker.leave(t).forEach { db.viewEventDao().record(it) }
            t += 100L
        }
    }

    @Test
    fun aFirstVisitKeepsDatasetOrder() = runBlocking {
        val items = FeedAssembler.assemble("t1", index.discoveries, completedBefore = freshness.seenBefore(date))
        assertEquals(listOf("a1", "a2", "a3", "a4", "a5", "a6"), items.map { it.discovery.id })
    }

    @Test
    fun aRevisitShowsUnseenCardsFirst() = runBlocking {
        val prevDate = date.minusDays(7)
        doneOnDate("a1", prevDate)
        doneOnDate("a2", prevDate)

        val items = FeedAssembler.assemble("t1", index.discoveries, completedBefore = freshness.seenBefore(date))
        assertEquals(listOf("a3", "a4", "a5", "a6", "a1", "a2"), items.map { it.discovery.id })
    }

    @Test
    fun theOrderIsStableWhileTheDayProgresses() = runBlocking {
        val prevDate = date.minusDays(7)
        doneOnDate("a1", prevDate)
        doneOnDate("a2", prevDate)

        val before = FeedAssembler.assemble("t1", index.discoveries, completedBefore = freshness.seenBefore(date))
        watch(listOf("a3"), 3_000L)

        val after = FeedAssembler.assemble("t1", index.discoveries, completedBefore = freshness.seenBefore(date))
        assertEquals(before.map { it.discovery.id }, after.map { it.discovery.id })
    }

    @Test
    fun theOrderIsStableAfterARestart() = runBlocking {
        val prevDate = date.minusDays(7)
        doneOnDate("a1", prevDate)
        doneOnDate("a2", prevDate)

        val before = FeedAssembler.assemble("t1", index.discoveries, completedBefore = freshness.seenBefore(date))
        createRepos()
        val after = FeedAssembler.assemble("t1", index.discoveries, completedBefore = freshness.seenBefore(date))
        assertEquals(before.map { it.discovery.id }, after.map { it.discovery.id })
    }

    @Test
    fun fullDayStillCompletesInTheNewOrder() = runBlocking {
        val prevDate = date.minusDays(7)
        doneOnDate("a1", prevDate)
        doneOnDate("a2", prevDate)

        val plan = dayPlans.ensureToday("animals", animalsTopics, DailyMode.GUIDED) as DayPlan.Ready
        val items = FeedAssembler.assemble(plan.choice.topicId, index.discoveries, completedBefore = freshness.seenBefore(date))
        watch(items.map { it.discovery.id }, 3_000L)

        val monthProg = progress.observeMonth(YearMonth.of(2026, 10)).first()
        assertEquals(1, monthProg.daysCompleted)
    }

    @Test
    fun aPastDayKeepsItsOwnOrder() = runBlocking {
        doneOnDate("a1", date.minusDays(3))
        doneOnDate("a2", date.minusDays(3))
        doneOnDate("a3", date.minusDays(1))

        val pastOrder = FeedAssembler.assemble("t1", index.discoveries, completedBefore = freshness.seenBefore(date.minusDays(1)))
        assertEquals(listOf("a3", "a4", "a5", "a6", "a1", "a2"), pastOrder.map { it.discovery.id })

        val todayOrder = FeedAssembler.assemble("t1", index.discoveries, completedBefore = freshness.seenBefore(date))
        assertEquals(listOf("a4", "a5", "a6", "a1", "a2", "a3"), todayOrder.map { it.discovery.id })
    }

    @Test
    fun resetBringsDatasetOrderBack() = runBlocking {
        val prevDate = date.minusDays(7)
        doneOnDate("a1", prevDate)
        doneOnDate("a2", prevDate)

        userActivity.resetAll()

        val items = FeedAssembler.assemble("t1", index.discoveries, completedBefore = freshness.seenBefore(date))
        assertEquals(listOf("a1", "a2", "a3", "a4", "a5", "a6"), items.map { it.discovery.id })
    }
}
