package com.example.curio.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DayCompletionWatcherTest {
    @Test
    fun firesOnceWhenTheDayBecomesComplete() {
        val w = DayCompletionWatcher()
        assertFalse(w.update(false))
        assertTrue(w.update(true))
        assertFalse(w.update(true))
    }

    @Test
    fun anAlreadyCompleteDayDoesNotFireOnOpen() {
        val w = DayCompletionWatcher()
        assertFalse(w.update(true))
        assertFalse(w.update(true))
    }

    @Test
    fun aResetDayCanFireAgain() {
        val w = DayCompletionWatcher()
        w.update(false); w.update(true)
        assertFalse(w.update(false))
        assertTrue(w.update(true))
    }
}
