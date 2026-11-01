package com.example.curio.data.user

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.example.curio.domain.DailyMode
import com.example.curio.domain.DayChoice
import com.example.curio.domain.DayPlan
import com.example.curio.domain.TestContent
import com.example.curio.domain.TopicSelector
import com.example.curio.domain.ViewSource
import com.example.curio.domain.ViewTouch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class SurpriseAvoidanceTest {
    private lateinit var db: CurioDatabase
    private val D = LocalDate.of(2026, 10, 26)
    private val zone = ZoneId.of("Europe/Bucharest")
    private val index = TestContent.index
    private val ids = listOf("t1", "t2", "t3", "t4")

    private fun clockOn(date: LocalDate): Clock =
        Clock.fixed(date.atTime(12, 0).atZone(zone).toInstant(), zone)

    private fun repo(date: LocalDate) = DayPlanRepository(db, clockOn(date))

    private suspend fun yesterday(topic: String, theme: String = "animals") {
        db.dayChoiceDao().upsert(DayChoice(D.minusDays(1), theme, topic, DailyMode.SURPRISE).toEntity())
    }

    private fun topicOf(plan: DayPlan): String =
        (plan as DayPlan.Ready).choice.topicId

    private val rawD = TopicSelector.surprise(D, "animals", ids)!!

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), CurioDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun withoutYesterdayTheRawPickIsUsed() = runBlocking {
        assertEquals(rawD, topicOf(repo(D).ensureToday("animals", ids, DailyMode.SURPRISE)))
    }

    @Test
    fun yesterdayEqualToTheRawPickForcesAnotherTopic() = runBlocking {
        yesterday(rawD)
        val res = topicOf(repo(D).ensureToday("animals", ids, DailyMode.SURPRISE))
        assertNotEquals(rawD, res)
        assertEquals(TopicSelector.surprise(D, "animals", ids, rawD), res)
    }

    @Test
    fun yesterdayDifferentFromTheRawPickChangesNothing() = runBlocking {
        val diff = ids.first { it != rawD }
        yesterday(diff)
        assertEquals(rawD, topicOf(repo(D).ensureToday("animals", ids, DailyMode.SURPRISE)))
    }

    @Test
    fun todaysChoiceIsStableAllDay() = runBlocking {
        yesterday(rawD)
        val first = topicOf(repo(D).ensureToday("animals", ids, DailyMode.SURPRISE))
        val second = topicOf(repo(D).ensureToday("animals", ids, DailyMode.SURPRISE))
        val restarted = topicOf(DayPlanRepository(db, clockOn(D)).ensureToday("animals", ids, DailyMode.SURPRISE))
        assertEquals(first, second)
        assertEquals(first, restarted)
    }

    @Test
    fun aModeChangeBeforeProgressStillAvoidsYesterday() = runBlocking {
        repo(D).ensureToday("animals", ids, DailyMode.GUIDED)
        yesterday(rawD)
        val r2 = topicOf(repo(D).ensureToday("animals", ids, DailyMode.SURPRISE))
        assertNotEquals(rawD, r2)
    }

    @Test
    fun guidedIsNeverAffected() = runBlocking {
        val g = TopicSelector.guided(D, ids)!!
        yesterday(g)
        assertEquals(g, topicOf(repo(D).ensureToday("animals", ids, DailyMode.GUIDED)))
    }

    @Test
    fun yesterdayFromAnotherThemeIsIgnored() = runBlocking {
        yesterday("u1", theme = "space")
        assertEquals(rawD, topicOf(repo(D).ensureToday("animals", ids, DailyMode.SURPRISE)))
    }

    @Test
    fun aLockedDayKeepsItsTopic() = runBlocking {
        val first = topicOf(repo(D).ensureToday("animals", ids, DailyMode.SURPRISE))
        db.viewEventDao().record(ViewTouch("a1", D, ViewSource.FEED, 1000L, markCompleted = true))
        yesterday(first)
        val second = topicOf(repo(D).ensureToday("animals", ids, DailyMode.SURPRISE))
        assertEquals(first, second)
    }

    @Test
    fun chooseModeIsUnaffected() = runBlocking {
        val plan = repo(D).ensureToday("animals", ids, DailyMode.CHOOSE)
        assertTrue(plan is DayPlan.NeedsChoice)
    }

    @Test
    fun aTwoWeekRunNeverRepeatsATopic() = runBlocking {
        var curr = D
        var prevTopic: String? = null
        for (i in 0..13) {
            val plan = repo(curr).ensureToday("animals", ids, DailyMode.SURPRISE)
            val topic = topicOf(plan)
            if (prevTopic != null) {
                assertNotEquals(prevTopic, topic)
            }
            prevTopic = topic
            curr = curr.plusDays(1)
        }
    }

    @Test
    fun aSkippedDayRestoresTheRawPick() = runBlocking {
        val d2 = D.plusDays(2)
        val rawD2 = TopicSelector.surprise(d2, "animals", ids)!!
        assertEquals(rawD2, topicOf(repo(d2).ensureToday("animals", ids, DailyMode.SURPRISE)))
    }
}
