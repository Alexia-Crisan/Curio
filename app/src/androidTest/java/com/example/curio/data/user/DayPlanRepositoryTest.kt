package com.example.curio.data.user

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.example.curio.domain.DailyMode.CHOOSE
import com.example.curio.domain.DailyMode.GUIDED
import com.example.curio.domain.DailyMode.SURPRISE
import com.example.curio.domain.DayChoice
import com.example.curio.domain.DayPlan
import com.example.curio.domain.TopicSelector
import com.example.curio.domain.ViewSource.BONUS
import com.example.curio.domain.ViewSource.DETAIL
import com.example.curio.domain.ViewSource.FEED
import com.example.curio.domain.ViewTouch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class DayPlanRepositoryTest {
    private lateinit var db: CurioDatabase
    private val topics = (0..6).map { "t$it" }
    private val d1 = LocalDate.of(2026, 10, 26)

    private fun repo(date: LocalDate = d1) = DayPlanRepository(
        db,
        Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
    )

    private suspend fun completeFeedCard(date: LocalDate = d1) {
        db.viewEventDao().record(ViewTouch("discovery_001", date, FEED, 1_000, markCompleted = true))
    }

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), CurioDatabase::class.java).build()
    }

    @After fun tearDown() = db.close()

    @Test fun guidedWritesOneRowAndIsStableAfterRestart() = runBlocking {
        val res = repo().ensureToday("animals", topics, GUIDED) as DayPlan.Ready
        assertEquals(DayChoice(d1, "animals", "t4", GUIDED), res.choice)
        val res2 = repo().ensureToday("animals", topics, GUIDED) as DayPlan.Ready
        assertEquals(res.choice, res2.choice)
        assertEquals("t4", db.dayChoiceDao().find(d1.toString())?.topicId)
    }

    @Test fun surpriseIsWrittenAndStable() = runBlocking {
        val expectedTopic = TopicSelector.surprise(d1, "animals", topics)
        val res = repo().ensureToday("animals", topics, SURPRISE) as DayPlan.Ready
        assertEquals(expectedTopic, res.choice.topicId)
        val res2 = repo().ensureToday("animals", topics, SURPRISE) as DayPlan.Ready
        assertEquals(res.choice, res2.choice)
    }

    @Test fun chooseAsksFirstThenSurvivesRestart() = runBlocking {
        val res = repo().ensureToday("animals", topics, CHOOSE)
        assertTrue(res is DayPlan.NeedsChoice)
        val res2 = repo().chooseTopic("animals", topics, "t2") as DayPlan.Ready
        assertEquals("t2", res2.choice.topicId)

        val res3 = repo().ensureToday("animals", topics, CHOOSE) as DayPlan.Ready
        assertEquals("t2", res3.choice.topicId)
        assertEquals(false, res3.settingsLocked)
    }

    @Test fun changingSettingsBeforeAnyProgressReplacesTodaysRow() = runBlocking {
        repo().ensureToday("animals", topics, GUIDED)
        val res = repo().ensureToday("animals", topics, SURPRISE) as DayPlan.Ready
        assertEquals(SURPRISE, res.choice.mode)
    }

    @Test fun changingSettingsAfterProgressIsLockedUntilTomorrow() = runBlocking {
        repo().ensureToday("animals", topics, GUIDED)
        completeFeedCard(d1)
        val res = repo().ensureToday("animals", topics, SURPRISE) as DayPlan.Ready
        assertEquals("t4", res.choice.topicId)
        assertEquals(GUIDED, res.choice.mode)
        assertTrue(res.settingsLocked)

        val tomorrowRes = repo(d1.plusDays(1)).ensureToday("animals", topics, SURPRISE) as DayPlan.Ready
        assertEquals(SURPRISE, tomorrowRes.choice.mode)
        assertEquals(false, tomorrowRes.settingsLocked)
    }

    @Test fun choosingAnotherTopicAfterProgressIsRefused() = runBlocking {
        repo().chooseTopic("animals", topics, "t2")
        completeFeedCard(d1)
        val res = repo().chooseTopic("animals", topics, "t5") as DayPlan.Ready
        assertEquals("t2", res.choice.topicId)
        assertTrue(res.settingsLocked)
    }

    @Test fun bonusAndDetailEventsDoNotLockTheDay() = runBlocking {
        repo().ensureToday("animals", topics, GUIDED)
        db.viewEventDao().record(ViewTouch("discovery_050", d1, BONUS, 1_000, markCompleted = true))
        db.viewEventDao().record(ViewTouch("discovery_060", d1, DETAIL, 1_000, markCompleted = true))
        val res = repo().ensureToday("animals", topics, SURPRISE) as DayPlan.Ready
        assertEquals(SURPRISE, res.choice.mode)
    }

    @Test fun aPastDayKeepsItsTopicAfterAThemeChange() = runBlocking {
        repo(d1).ensureToday("animals", topics, GUIDED)
        repo(d1.plusDays(1)).ensureToday("space", topics, GUIDED)
        val past = repo(d1.plusDays(1)).peek(d1)
        assertEquals(DayChoice(d1, "animals", "t4", GUIDED), past)
    }

    @Test fun peekNeverCreatesARow() = runBlocking {
        assertNull(repo().peek(d1.minusDays(3)))
        assertNull(db.dayChoiceDao().find(d1.minusDays(3).toString()))
    }

    @Test fun changingThemeInChooseModeAsksAgain() = runBlocking {
        repo().chooseTopic("animals", topics, "t2")
        val res = repo().ensureToday("space", topics, CHOOSE)
        assertTrue(res is DayPlan.NeedsChoice)
    }

    @Test fun aThemeWithoutTopicsIsNoContent() = runBlocking {
        val res = repo().ensureToday("coming-soon", emptyList(), GUIDED)
        assertTrue(res is DayPlan.NoContent)
    }
}
