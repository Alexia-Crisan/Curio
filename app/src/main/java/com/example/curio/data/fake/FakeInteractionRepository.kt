package com.example.curio.data.fake

import com.example.curio.domain.InteractionRepository
import com.example.curio.domain.InteractionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeInteractionRepository : InteractionRepository {
    private val saved = MutableStateFlow<Set<String>>(emptySet())

    override suspend fun record(discoveryId: String, type: InteractionType, dwellMs: Long?) {
        when (type) {
            InteractionType.SAVED -> saved.update { it + discoveryId }
            InteractionType.UNSAVED -> saved.update { it - discoveryId }
            else -> Unit
        }
    }

    override fun observeSavedIds(): Flow<Set<String>> = saved
}
