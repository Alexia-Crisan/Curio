package com.example.curio.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.curio.domain.DetailSaves
import java.time.LocalDate

class FakeDetailSaves(initial: Set<String> = emptySet()) : DetailSaves {
    val state = MutableStateFlow(initial)
    val calls = mutableListOf<Triple<String, Boolean, LocalDate>>()

    override fun observeSaved(): Flow<Set<String>> = state

    override suspend fun setSaved(discoveryId: String, saved: Boolean, date: LocalDate) {
        calls += Triple(discoveryId, saved, date)
        state.value = if (saved) state.value + discoveryId else state.value - discoveryId
    }
}
