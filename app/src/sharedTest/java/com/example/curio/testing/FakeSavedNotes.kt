package com.example.curio.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.curio.domain.NoteRules
import com.example.curio.domain.SavedNotes

class FakeSavedNotes(
    saved: Set<String> = emptySet(),
    initial: Map<String, String> = emptyMap(),
) : SavedNotes {
    val state = MutableStateFlow(initial)
    val savedIds = saved.toMutableSet()
    val calls = mutableListOf<Pair<String, String>>()

    override fun observeNotes(): Flow<Map<String, String>> = state

    override suspend fun saveNote(discoveryId: String, raw: String): Boolean {
        calls += discoveryId to raw
        if (discoveryId !in savedIds) return false
        val note = NoteRules.normalize(raw)
        state.value = if (note == null) state.value - discoveryId else state.value + (discoveryId to note)
        return true
    }
}
