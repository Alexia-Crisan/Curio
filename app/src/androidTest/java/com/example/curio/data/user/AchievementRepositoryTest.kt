package com.example.curio.data.user

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.example.curio.domain.AchievementId
import com.example.curio.domain.DAY
import com.example.curio.domain.TestContent
import com.example.curio.domain.ViewSource.DETAIL
import com.example.curio.domain.ViewSource.FEED
import com.example.curio.domain.ev
import com.example.curio.domain.idsOf
import java.time.Clock
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class AchievementRepositoryTest {
    private lateinit var db: CurioDatabase
    private val clock = Clock.fixed(DAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
    private val all30 = (idsOf("a") + idsOf("b") + idsOf("c") + idsOf("d") + idsOf("s")).toSet()

    private fun activity() = UserActivityRepository(db, TestContent.index, clock)
    private fun repo() = AchievementRepository(db, activity(), TestContent.index, clock)

    private suspend fun complete(ids: Collection<String>, date: java.time.LocalDate = DAY, source: com.example.curio.domain.ViewSource = FEED) {
        ids.forEach { db.viewEventDao().upsert(ev(it, date, source = source).toEntity()) }
    }

    private suspend fun save(ids: Collection<String>) {
        ids.forEach { activity().setSaved(it, true, DETAIL, recordActivity = false) }
    }

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), CurioDatabase::class.java).build()
    }

    @After fun tearDown() = db.close()

    @Test fun nothingIsEarnedOnAFreshDatabase() = runBlocking {
        assertTrue(repo().evaluateNow().isEmpty())
        assertTrue(db.achievementDao().getAll().isEmpty())
    }

    @Test fun theFirstCompletedCardEarnsFirstDiscoveryExactlyOnce() = runBlocking {
        complete(listOf("a1"))
        val earned = repo().evaluateNow()
        assertEquals(listOf(AchievementId.FIRST_DISCOVERY), earned)

        val secondCall = repo().evaluateNow()
        assertTrue(secondCall.isEmpty())
        assertEquals(1, db.achievementDao().getAll().size)
    }

    @Test fun aRestartDoesNotEarnTheSameBadgeAgain() = runBlocking {
        complete(listOf("a1"))
        repo().evaluateNow()

        val newRepo = repo()
        assertTrue(newRepo.evaluateNow().isEmpty())
        assertEquals(1, db.achievementDao().getAll().size)
    }

    @Test fun severalBadgesInOnePassFollowDefinitionOrder() = runBlocking {
        complete(all30.take(25))
        val earned = repo().evaluateNow()
        assertEquals(
            listOf(AchievementId.FIRST_DISCOVERY, AchievementId.CURIOUS_MIND, AchievementId.TOPIC_EXPLORER),
            earned
        )
    }

    @Test fun twoConcurrentEvaluationsEarnTheBadgeOnce() = runBlocking {
        complete(listOf("a1"))
        val r = repo()
        val e1 = async(Dispatchers.Default) { r.evaluateNow() }
        val e2 = async(Dispatchers.Default) { r.evaluateNow() }
        val combined = e1.await() + e2.await()

        assertEquals(listOf(AchievementId.FIRST_DISCOVERY), combined)
        assertEquals(1, db.achievementDao().getAll().size)
    }

    @Test fun shouldHaveBadgesAreNotEvaluatedYet() = runBlocking {
        (0..4).forEach { i -> complete(listOf("a1"), date = DAY.plusDays(i.toLong())) }
        val earned = repo().evaluateNow()
        assertEquals(listOf(AchievementId.FIRST_DISCOVERY), earned)
    }

    @Test fun anUnlockedBadgeStaysUnlockedWhenTheSavesAreRemoved() = runBlocking {
        save(all30.take(10))
        assertEquals(listOf(AchievementId.KNOWLEDGE_COLLECTOR), repo().evaluateNow())

        all30.take(6).forEach { activity().setSaved(it, false, DETAIL) }

        val prog = repo().observeProgress().first().first { it.def.id == AchievementId.KNOWLEDGE_COLLECTOR }
        assertTrue(prog.isUnlocked)
        assertEquals(10, prog.current)
        assertTrue(repo().evaluateNow().isEmpty())
    }

    @Test fun lockedBadgesShowLiveProgress() = runBlocking {
        save(all30.take(4))
        val prog = repo().observeProgress().first().first { it.def.id == AchievementId.KNOWLEDGE_COLLECTOR }
        assertFalse(prog.isUnlocked)
        assertEquals(4, prog.current)
    }

    @Test fun bannersAreQueuedOneAtATimeInOrder() = runBlocking {
        complete(all30.take(25))
        val r = repo()
        r.evaluateNow()

        assertEquals(AchievementId.FIRST_DISCOVERY, r.observeNextPending().first()?.id)
        r.markNotified(AchievementId.FIRST_DISCOVERY)

        assertEquals(AchievementId.CURIOUS_MIND, r.observeNextPending().first()?.id)
        r.markNotified(AchievementId.CURIOUS_MIND)

        assertEquals(AchievementId.TOPIC_EXPLORER, r.observeNextPending().first()?.id)
        r.markNotified(AchievementId.TOPIC_EXPLORER)

        assertNull(r.observeNextPending().first())
    }

    @Test fun openingTheScreenClearsTheUnseenCount() = runBlocking {
        complete(listOf("a1"))
        val r = repo()
        r.evaluateNow()
        assertEquals(1, r.observeUnseenCount().first())

        r.markAllSeen()
        assertEquals(0, r.observeUnseenCount().first())
    }

    @Test fun resetThenEvaluatingEarnsNothingAndShowsEmptyProgress() = runBlocking {
        save(all30.take(10))
        val r = repo()
        r.evaluateNow()

        activity().resetAll()

        assertTrue(r.evaluateNow().isEmpty())
        assertTrue(db.achievementDao().getAll().isEmpty())

        val prog = r.observeProgress().first()
        assertTrue(prog.all { it.current == 0 && !it.isUnlocked })
    }

    @Test fun theLiveTrackerEarnsABadgeAfterADatabaseChange() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default + Job())
        try {
            AchievementTracker(scope, activity(), repo()).start()
            complete(listOf("a1"))

            val rows = db.achievementDao().observeAll().first { it.isNotEmpty() }
            assertEquals(1, rows.size)
            assertEquals("FIRST_DISCOVERY", rows[0].achievementId)
        } finally {
            scope.cancel()
        }
    }
}
