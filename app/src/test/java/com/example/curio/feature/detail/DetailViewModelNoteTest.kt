package com.example.curio.feature.detail

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import com.example.curio.domain.SavedNotes
import com.example.curio.domain.TestContent
import com.example.curio.domain.TouchQueue
import com.example.curio.domain.ViewTouch
import com.example.curio.testing.FakeDetailSaves
import com.example.curio.testing.FakeSavedNotes
import com.example.curio.testing.MainDispatcherRule
import com.example.curio.testing.MutableClock
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelNoteTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val clock = MutableClock(Instant.parse("2026-10-26T09:00:00Z").toEpochMilli())
    private val recorded = mutableListOf<ViewTouch>()
    private val queue = TouchQueue(CoroutineScope(UnconfinedTestDispatcher())) { touch -> recorded += touch }
    private val saves = FakeDetailSaves(setOf("a6"))

    private fun vmWithoutNotes() = DetailViewModel("a6", TestContent.index, saves, queue, clock)

    private fun vm(notes: SavedNotes) = DetailViewModel("a6", TestContent.index, saves, queue, clock, notes)

    @Test
    fun theNoteIsEmptyWhenNoSourceIsGiven() = runTest {
        assertNull(vmWithoutNotes().note.first())
    }

    @Test
    fun theNoteShowsWhatTheSourceHoldsForThisCard() = runTest {
        val notes = FakeSavedNotes(setOf("a6"), mapOf("a6" to "remember", "a7" to "other"))
        assertEquals("remember", vm(notes).note.first { it != null })
    }

    @Test
    fun notesOfOtherCardsAreIgnored() = runTest {
        val notes = FakeSavedNotes(setOf("a6"), mapOf("a7" to "other"))
        val model = vm(notes)
        val job = launch { model.note.collect { } }
        assertNull(model.note.value)
        job.cancel()
    }

    @Test
    fun savingForwardsTheCardIdAndTheRawText() = runTest {
        val notes = FakeSavedNotes(setOf("a6"))
        vm(notes).saveNote("  hello ")
        assertEquals(listOf("a6" to "  hello "), notes.calls)
    }

    @Test
    fun savedTextAppearsAndBlankTextRemovesIt() = runTest {
        val notes = FakeSavedNotes(setOf("a6"))
        val model = vm(notes)
        val job = launch { model.note.collect { } }
        testScheduler.runCurrent()
        model.saveNote("  Remember this ")
        testScheduler.runCurrent()
        assertEquals("Remember this", model.note.value)
        model.saveNote("   ")
        testScheduler.runCurrent()
        assertNull(model.note.value)
        job.cancel()
    }
}
