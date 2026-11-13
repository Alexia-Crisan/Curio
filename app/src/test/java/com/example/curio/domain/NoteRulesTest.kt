package com.example.curio.domain

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteRulesTest {

    @Test fun theLimitIsTwoHundredAndEighty() = assertEquals(280, NoteRules.MAX_LENGTH)

    @Test fun blankTextMeansNoNote() {
        assertNull(NoteRules.normalize(""))
        assertNull(NoteRules.normalize("   "))
        assertNull(NoteRules.normalize("\n\t \n"))
    }

    @Test fun surroundingSpacesAreTrimmedAndInnerTextIsKept() {
        assertEquals("Mars is red", NoteRules.normalize("  Mars is red \n"))
        assertEquals("line one\nline two", NoteRules.normalize("line one\nline two"))
        assertEquals("a  b", NoteRules.normalize(" a  b "))
    }

    @Test fun textAtTheLimitIsKeptWhole() {
        val text = "x".repeat(280)
        assertEquals(text, NoteRules.normalize(text))
    }

    @Test fun longerTextIsCutToTheLimit() {
        val result = NoteRules.normalize("y".repeat(500))
        assertEquals("y".repeat(280), result)
    }

    @Test fun trimHappensBeforeTheLengthCheck() {
        val text = "  " + "z".repeat(280) + "  "
        assertEquals("z".repeat(280), NoteRules.normalize(text))
    }

    @Test fun aCutNeverSplitsAnEmoji() {
        val text = "a".repeat(279) + "😀" + "b"
        val result = NoteRules.normalize(text)!!
        assertEquals("a".repeat(279), result)
        assertFalse(Character.isHighSurrogate(result.last()))
    }

    @Test fun anEmojiThatFitsIsKept() {
        val text = "a".repeat(278) + "😀" + "b"
        val result = NoteRules.normalize(text)!!
        assertEquals(280, result.length)
        assertTrue(result.endsWith("😀"))
    }

    @Test fun normalizingTwiceChangesNothing() {
        listOf("  hi ", "q".repeat(400), "a".repeat(279) + "😀", "\n x \n").forEach {
            val once = NoteRules.normalize(it)
            assertEquals(once, once?.let(NoteRules::normalize))
        }
    }

    @Test fun aCutDoesNotLeaveTrailingSpaces() {
        assertEquals("a".repeat(279), NoteRules.normalize("a".repeat(279) + " tail"))
    }

    @Test fun remainingCountsDown() {
        assertEquals(280, NoteRules.remaining(""))
        assertEquals(275, NoteRules.remaining("hello"))
        assertEquals(0, NoteRules.remaining("k".repeat(280)))
    }

    @Test fun theEmptySourceHasNoNotesAndRefusesToSave() = runBlocking {
        assertTrue(NoSavedNotes.observeNotes().first().isEmpty())
        assertFalse(NoSavedNotes.saveNote("a1", "text"))
        assertTrue(NoSavedNotes.observeNotes().first().isEmpty())
    }
}
