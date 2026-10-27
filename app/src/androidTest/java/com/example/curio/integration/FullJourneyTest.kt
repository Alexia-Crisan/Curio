package com.example.curio.integration

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.example.curio.data.user.AchievementRepository
import com.example.curio.data.user.BonusRepository
import com.example.curio.data.user.CurioDatabase
import com.example.curio.data.user.DayPlanRepository
import com.example.curio.data.user.ProgressRepository
import com.example.curio.data.user.UserActivityRepository
import com.example.curio.domain.AchievementId
import com.example.curio.domain.DailyMode
import com.example.curio.domain.DayPlan
import com.example.curio.domain.DwellTracker
import com.example.curio.domain.NodeState
import com.example.curio.domain.ShakeBonusSelector
import com.example.curio.domain.TeaserBuilder
import com.example.curio.domain.TestContent
import com.example.curio.domain.TreeBuilder
import com.example.curio.domain.TreeGrowth
import com.example.curio.domain.ViewSource
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class FullJourneyTest {
    private lateinit var db: CurioDatabase
    private lateinit var clock: Clock
    private val index = TestContent.index
    private val animalsTopics = listOf("t1", "t2", "t3", "t4")
    private val date = LocalDate.of(2026, 10, 26)
    private val zone = ZoneId.of("Europe/Bucharest")

    private lateinit var dayPlans: DayPlanRepository
    private lateinit var bonus: BonusRepository
    private lateinit var progress: ProgressRepository
    private lateinit var userActivity: UserActivityRepository
    private lateinit var achievements: AchievementRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), CurioDatabase::class.java).build()
        clock = Clock.fixed(Instant.parse("2026-10-26T09:00:00Z"), zone)
        createRepos()
    }

    private fun createRepos() {
        dayPlans = DayPlanRepository(db, clock)
        bonus = BonusRepository(db, index.discoveries, clock)
        progress = ProgressRepository(db, index.membership)
        userActivity = UserActivityRepository(db, index, clock)
        achievements = AchievementRepository(db, userActivity, index, clock)
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

    private suspend fun watch(ids: List<String>, dwellMs: Long, source: ViewSource = ViewSource.FEED, startMs: Long = 10_000L) {
        val tracker = DwellTracker(date)
        var t = startMs
        for (id in ids) {
            tracker.show(id, source, t).forEach { db.viewEventDao().record(it) }
            t += dwellMs
            tracker.leave(t).forEach { db.viewEventDao().record(it) }
            t += 100L
        }
    }

    private suspend fun todaysTopic(): String {
        val plan = dayPlans.ensureToday("animals", animalsTopics, DailyMode.GUIDED) as DayPlan.Ready
        return plan.choice.topicId
    }

    @Test
    fun aFullDayEndsToEnd() = runBlocking {
        val topic = todaysTopic()
        val cards = index.discoveriesOf(topic).map { it.id }
        watch(cards, 3_000L)

        val monthProg = progress.observeMonth(YearMonth.of(2026, 10)).first()
        assertEquals(1, monthProg.daysCompleted)

        val newBadges = achievements.evaluateNow()
        assertTrue(newBadges.contains(AchievementId.FIRST_DISCOVERY))
        assertTrue(newBadges.contains(AchievementId.FULL_DAY))

        val snapshot = userActivity.currentSnapshot()
        val trees = TreeBuilder.buildAll(index, snapshot)
        val animalsTree = trees.first { it.theme.id == "animals" }
        val branch = animalsTree.branches.first { it.topic.id == topic }
        assertEquals(NodeState.COMPLETE, branch.state)
    }

    @Test
    fun sixQuickSwipesEarnNothing() = runBlocking {
        val topic = todaysTopic()
        val cards = index.discoveriesOf(topic).map { it.id }
        watch(cards, 1_000L)

        val monthProg = progress.observeMonth(YearMonth.of(2026, 10)).first()
        assertEquals(0, monthProg.daysCompleted)

        val newBadges = achievements.evaluateNow()
        assertTrue(newBadges.isEmpty())

        val events = db.viewEventDao().getAll()
        assertTrue(events.all { it.skipped })
    }

    @Test
    fun restartKeepsEverything() = runBlocking {
        val topic = todaysTopic()
        val cards = index.discoveriesOf(topic).map { it.id }
        watch(cards, 3_000L)
        achievements.evaluateNow()

        val snapBefore = userActivity.currentSnapshot()
        val treesBefore = TreeBuilder.buildAll(index, snapBefore)

        createRepos()

        val monthProg = progress.observeMonth(YearMonth.of(2026, 10)).first()
        assertEquals(1, monthProg.daysCompleted)

        val snapAfter = userActivity.currentSnapshot()
        val treesAfter = TreeBuilder.buildAll(index, snapAfter)
        assertEquals(treesBefore, treesAfter)

        val newBadges = achievements.evaluateNow()
        assertTrue(newBadges.isEmpty())
        assertEquals(2, count("achievement_unlock"))
    }

    @Test
    fun aBonusCardLightsAnotherTopicWithoutChangingTheDay() = runBlocking {
        val topic = todaysTopic()
        val cards = index.discoveriesOf(topic).map { it.id }
        watch(cards.take(4), 3_000L)

        val bonusRes = bonus.reveal("animals", topic) as ShakeBonusSelector.Result.Found
        watch(listOf(bonusRes.discovery.id), 3_000L, source = ViewSource.BONUS)

        val monthProg = progress.observeMonth(YearMonth.of(2026, 10)).first()
        assertEquals(0, monthProg.daysCompleted)

        val snapshot = userActivity.currentSnapshot()
        val trees = TreeBuilder.buildAll(index, snapshot)
        val bonusTree = trees.first { it.theme.id == bonusRes.discovery.themeId }
        val branch = bonusTree.branches.first { it.topic.id == bonusRes.discovery.topicId }
        assertTrue(branch.state == NodeState.SEEDED || branch.state == NodeState.GROWING || branch.state == NodeState.COMPLETE)

        achievements.evaluateNow()
        val hiddenProgress = achievements.observeProgress().first().first { it.def.id == AchievementId.HIDDEN_DISCOVERIES }
        assertEquals(1, hiddenProgress.current)
    }

    @Test
    fun theBonusCardSurvivesARestart() = runBlocking {
        val topic = todaysTopic()
        val bonusRes = bonus.reveal("animals", topic) as ShakeBonusSelector.Result.Found

        createRepos()

        val restored = bonus.todaysBonusCards()
        assertEquals(1, restored.size)
        assertEquals(bonusRes.discovery.id, restored[0].id)
    }

    @Test
    fun savingFromDetailNeverMakesAnActiveDay() = runBlocking {
        userActivity.setSaved("c1", true, ViewSource.DETAIL, recordActivity = false)
        val snapshot = userActivity.currentSnapshot()
        assertTrue(snapshot.completedFeedDays.isEmpty())
        assertEquals(setOf("c1"), snapshot.savedIds)
    }

    @Test
    fun aPastDayKeepsItsTopicAfterAThemeChange() = runBlocking {
        val topicX = todaysTopic()
        val nextClock = Clock.fixed(Instant.parse("2026-10-27T09:00:00Z"), zone)
        val nextDayPlans = DayPlanRepository(db, nextClock)
        nextDayPlans.ensureToday("space", listOf("u1", "u2"), DailyMode.GUIDED)

        val pastChoice = dayPlans.peek(date)
        assertEquals(topicX, pastChoice?.topicId)
    }

    @Test
    fun theTeaserAfterAFullDay() = runBlocking {
        val topic = todaysTopic()
        val cards = index.discoveriesOf(topic).map { it.id }
        watch(cards, 3_000L)

        val snapshot = userActivity.currentSnapshot()
        val trees = TreeBuilder.buildAll(index, snapshot)
        val teaser = TeaserBuilder.forFeed(trees, "animals", DailyMode.GUIDED, date, zone)

        assertNotNull(teaser)
        assertEquals(6, teaser!!.leavesToday)
        assertNotNull(teaser.next)
    }

    @Test
    fun growthIsVisibleAfterReturning() = runBlocking {
        val topic = todaysTopic()
        val cards = index.discoveriesOf(topic).map { it.id }
        val startMs = clock.millis()
        watch(cards, 3_000L, startMs = startMs)

        val snapshot = userActivity.currentSnapshot()
        val animalsTree = TreeBuilder.build(index.themes[0], index, snapshot)

        val summaryOld = TreeGrowth.since(animalsTree, sinceMs = startMs - 1)
        assertEquals(6, summaryOld.newLeafCount)

        val summaryNew = TreeGrowth.since(animalsTree, sinceMs = startMs + 30_000)
        assertEquals(0, summaryNew.newLeafCount)
    }

    @Test
    fun resetReturnsToAFreshApp() = runBlocking {
        val topic = todaysTopic()
        watch(index.discoveriesOf(topic).map { it.id }, 3_000L)
        bonus.reveal("animals", topic)
        achievements.evaluateNow()

        userActivity.resetAll()

        assertEquals(0, count("view_event"))
        assertEquals(0, count("day_choice"))
        assertEquals(0, count("saved_discovery"))
        assertEquals(0, count("achievement_unlock"))

        val snapshot = userActivity.currentSnapshot()
        val trees = TreeBuilder.buildAll(index, snapshot)
        assertTrue(trees.flatMap { it.branches }.all { it.state == NodeState.LOCKED })

        val progresses = achievements.observeProgress().first()
        assertTrue(progresses.all { it.current == 0 && !it.isUnlocked })

        val newBadges = achievements.evaluateNow()
        assertTrue(newBadges.isEmpty())
    }

    @Test
    fun resettingTwiceIsHarmless() = runBlocking {
        userActivity.resetAll()
        userActivity.resetAll()
        assertEquals(0, count("view_event"))
    }
}
