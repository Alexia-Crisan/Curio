package com.example.curio.data.user

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
import com.example.curio.domain.DailyMode.GUIDED
import com.example.curio.domain.DayChoice
import com.example.curio.domain.DayStatus
import com.example.curio.domain.TopicMembership
import com.example.curio.domain.ViewSource.BONUS
import com.example.curio.domain.ViewSource.FEED
import com.example.curio.domain.ViewTouch
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
class ProgressRepositoryTest {
    private lateinit var db: CurioDatabase
    private val day = LocalDate.of(2026, 10, 26)
    private val octMonth = YearMonth.of(2026, 10)

    private val aCards = (1..6).map { "a$it" }.toSet()
    private val bCards = (1..6).map { "b$it" }.toSet()
    private val membership = TopicMembership(mapOf("t1" to aCards, "t2" to bCards))

    private suspend fun choose(topic: String = "t1") {
        db.dayChoiceDao().upsert(DayChoice(day, "animals", topic, GUIDED).toEntity())
    }

    private suspend fun complete(id: String, source: com.example.curio.domain.ViewSource = FEED) {
        db.viewEventDao().record(ViewTouch(id, day, source, 1_000, markCompleted = true))
    }

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), CurioDatabase::class.java).build()
    }

    @After fun tearDown() = db.close()

    @Test fun sixCompletedCardsMakeADay() = runBlocking {
        choose("t1")
        (1..6).forEach { complete("a$it") }
        val repo = ProgressRepository(db, membership)
        val p = repo.observeMonth(octMonth).first()
        assertEquals(1, p.daysCompleted)
        assertEquals(DayStatus.COMPLETE, p.statusOf(day))
    }

    @Test fun bonusCardsDoNotCompleteTheDay() = runBlocking {
        choose("t1")
        (1..4).forEach { complete("a$it") }
        complete("b1", BONUS)
        complete("b2", BONUS)
        val repo = ProgressRepository(db, membership)
        val p = repo.observeMonth(octMonth).first()
        assertEquals(0, p.daysCompleted)
        assertEquals(4, p.days[day]?.completed)
    }

    @Test fun otherMonthsAreNotAffected() = runBlocking {
        choose("t1")
        (1..6).forEach { complete("a$it") }
        val repo = ProgressRepository(db, membership)
        val p = repo.observeMonth(YearMonth.of(2026, 11)).first()
        assertEquals(0, p.daysCompleted)
    }
}
