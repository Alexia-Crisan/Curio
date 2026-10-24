package com.example.curio.data.user

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.example.curio.domain.ContentType
import com.example.curio.domain.Discovery
import com.example.curio.domain.ShakeBonusSelector
import com.example.curio.domain.Source
import com.example.curio.domain.ViewSource.BONUS
import com.example.curio.domain.ViewTouch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class StepClock(private var nowInstant: Instant) : Clock() {
    override fun getZone(): ZoneOffset = ZoneOffset.UTC
    override fun withZone(zone: java.time.ZoneId?): Clock = this
    override fun instant(): Instant = nowInstant
    fun advance(ms: Long) {
        nowInstant = nowInstant.plusMillis(ms)
    }
}

@RunWith(AndroidJUnit4::class)
class BonusRepositoryTest {
    private lateinit var db: CurioDatabase
    private val date = LocalDate.of(2026, 10, 26)
    private val src = Source("n", "https://example.com", "CC0")

    private fun disc(id: String, theme: String, topic: String) = Discovery(
        id = id, themeId = theme, subcategoryId = topic, type = ContentType.FACT, title = id, source = src,
    )

    private val animalsT1 = (1..6).map { disc("a$it", "animals", "t1") }
    private val animalsT2 = (1..6).map { disc("b$it", "animals", "t2") }
    private val animalsT3 = (1..6).map { disc("c$it", "animals", "t3") }
    private val content = animalsT1 + animalsT2 + animalsT3

    private lateinit var clock: StepClock

    private fun repo(list: List<Discovery> = content, maxPerDay: Int = 3) =
        BonusRepository(db, list, clock, maxPerDay)

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), CurioDatabase::class.java).build()
        clock = StepClock(date.atStartOfDay(ZoneOffset.UTC).toInstant())
    }

    @After fun tearDown() = db.close()

    @Test fun revealWritesAnUncompletedBonusRowForToday() = runBlocking {
        val res = repo().reveal("animals", "t1") as ShakeBonusSelector.Result.Found
        val rows = db.viewEventDao().getAll()
        assertEquals(1, rows.size)
        assertEquals(res.discovery.id, rows[0].discoveryId)
        assertEquals(date.toString(), rows[0].date)
        assertEquals("BONUS", rows[0].source)
        assertFalse(rows[0].completed)
        assertFalse(rows[0].skipped)
    }

    @Test fun twoRevealsAreDistinctAndRestoredInOrder() = runBlocking {
        val r = repo()
        val first = (r.reveal("animals", "t1") as ShakeBonusSelector.Result.Found).discovery.id
        clock.advance(2000)
        val second = (r.reveal("animals", "t1") as ShakeBonusSelector.Result.Found).discovery.id

        assertFalse(first == second)
        val restored = r.todaysBonusCards().map { it.id }
        assertEquals(listOf(first, second), restored)
    }

    @Test fun afterARestartTheSequenceContinuesAndTheCapStillHolds() = runBlocking {
        val r1 = repo()
        r1.reveal("animals", "t1")
        clock.advance(2000)
        r1.reveal("animals", "t1")

        val r2 = repo()
        assertEquals(2, r2.todaysBonusCards().size)

        clock.advance(2000)
        val third = r2.reveal("animals", "t1")
        assertTrue(third is ShakeBonusSelector.Result.Found)

        clock.advance(2000)
        val fourth = r2.reveal("animals", "t1")
        assertTrue(fourth is ShakeBonusSelector.Result.CapReached)
    }

    @Test fun reachingTheCapWritesNoRow() = runBlocking {
        val r = repo()
        repeat(3) {
            r.reveal("animals", "t1")
            clock.advance(2000)
        }
        val cap = r.reveal("animals", "t1")
        assertTrue(cap is ShakeBonusSelector.Result.CapReached)
        assertEquals(3, db.viewEventDao().getAll().size)
    }

    @Test fun nothingLeftWritesNoRow() = runBlocking {
        val r = repo(list = animalsT1)
        val res = r.reveal("animals", "t1")
        assertTrue(res is ShakeBonusSelector.Result.NothingLeft)
        assertTrue(db.viewEventDao().getAll().isEmpty())
    }

    @Test fun twoRevealsAtTheSameTimeNeverReturnTheSameCard() = runBlocking {
        val r = repo()
        val d1 = async { r.reveal("animals", "t1") }
        val d2 = async { r.reveal("animals", "t1") }
        val res1 = d1.await() as ShakeBonusSelector.Result.Found
        val res2 = d2.await() as ShakeBonusSelector.Result.Found

        assertFalse(res1.discovery.id == res2.discovery.id)
        assertEquals(2, db.viewEventDao().getAll().size)
    }

    @Test fun aRevealedCardBecomesCompletedAfterThreeSecondsAndStaysBonus() = runBlocking {
        val res = repo().reveal("animals", "t1") as ShakeBonusSelector.Result.Found
        db.viewEventDao().record(ViewTouch(res.discovery.id, date, BONUS, 5_000, dwellDeltaMs = 3_000))

        val rows = db.viewEventDao().getAll()
        assertEquals(1, rows.size)
        assertTrue(rows[0].completed)
        assertEquals("BONUS", rows[0].source)
    }
}
