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
import java.time.Clock
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class SavedNotesRepositoryTest {
    private lateinit var db: CurioDatabase
    private lateinit var repo: UserActivityRepository
    private val clock = Clock.fixed(DAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), CurioDatabase::class.java).build()
        repo = UserActivityRepository(db, TestContent.index, clock)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun aNoteCanBeAddedToASavedCard() = runBlocking {
        repo.setSaved("a1", true, DETAIL)
        assertTrue(repo.saveNote("a1", "  first thoughts "))
        assertEquals(mapOf("a1" to "first thoughts"), repo.observeNotes().first())
    }

    @Test
    fun aNoteCannotBeAddedToACardThatIsNotSaved() = runBlocking {
        assertFalse(repo.saveNote("a1", "text"))
        assertTrue(repo.observeNotes().first().isEmpty())
        assertTrue(repo.observeSavedIds().first().isEmpty())
    }

    @Test
    fun editingReplacesTheNote() = runBlocking {
        repo.setSaved("a1", true, DETAIL)
        repo.saveNote("a1", "one")
        repo.saveNote("a1", "two")
        assertEquals(mapOf("a1" to "two"), repo.observeNotes().first())
    }

    @Test
    fun blankTextRemovesTheNoteButKeepsTheCardSaved() = runBlocking {
        repo.setSaved("a1", true, DETAIL)
        repo.saveNote("a1", "one")
        assertTrue(repo.saveNote("a1", "   "))
        assertTrue(repo.observeNotes().first().isEmpty())
        assertEquals(setOf("a1"), repo.observeSavedIds().first())
    }

    @Test
    fun overlongTextIsCutToTheLimit() = runBlocking {
        repo.setSaved("a1", true, DETAIL)
        repo.saveNote("a1", "n".repeat(500))
        assertEquals(280, repo.observeNotes().first().getValue("a1").length)
    }

    @Test
    fun savingTheSameCardAgainKeepsItsNoteAndItsPlaceInTheList() = runBlocking {
        db.savedDao().upsert(SavedDiscoveryEntity("a1", 1_000L))
        db.savedDao().upsert(SavedDiscoveryEntity("a2", 2_000L))
        repo.saveNote("a1", "keep me")
        repo.setSaved("a1", true, DETAIL)
        assertEquals(mapOf("a1" to "keep me"), repo.observeNotes().first())
        assertEquals(listOf("a2", "a1"), repo.observeSavedNewestFirst().first())
    }

    @Test
    fun unsavingRemovesTheNoteForGood() = runBlocking {
        repo.setSaved("a1", true, DETAIL)
        repo.saveNote("a1", "gone soon")
        repo.setSaved("a1", false, DETAIL)
        assertTrue(repo.observeNotes().first().isEmpty())
        repo.setSaved("a1", true, DETAIL)
        assertTrue(repo.observeNotes().first().isEmpty())
    }

    @Test
    fun resettingProgressRemovesNotes() = runBlocking {
        repo.setSaved("a1", true, DETAIL)
        repo.saveNote("a1", "note")
        repo.resetAll()
        assertTrue(repo.observeNotes().first().isEmpty())
    }

    @Test
    fun onlyCardsWithANoteAppearInTheNoteMap() = runBlocking {
        repo.setSaved("a1", true, DETAIL)
        repo.setSaved("a2", true, DETAIL)
        repo.saveNote("a2", "only this one")
        assertEquals(mapOf("a2" to "only this one"), repo.observeNotes().first())
    }

    @Test
    fun savedCardsComeNewestFirstWithTiesBrokenById() = runBlocking {
        db.savedDao().upsert(SavedDiscoveryEntity("a1", 100L))
        db.savedDao().upsert(SavedDiscoveryEntity("a2", 300L))
        db.savedDao().upsert(SavedDiscoveryEntity("a3", 200L))
        db.savedDao().upsert(SavedDiscoveryEntity("a4", 300L))
        assertEquals(listOf("a2", "a4", "a3", "a1"), repo.observeSavedNewestFirst().first())
    }
}
