package com.example.curio.domain

import kotlinx.coroutines.flow.Flow

enum class InteractionType { OPENED, COMPLETED, SAVED, UNSAVED, SHARED, SKIPPED }

interface InteractionRepository {
    suspend fun record(discoveryId: String, type: InteractionType, dwellMs: Long? = null)
    fun observeSavedIds(): Flow<Set<String>>
}
