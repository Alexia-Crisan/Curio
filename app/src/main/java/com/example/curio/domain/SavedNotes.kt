package com.example.curio.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

object NoteRules {
    const val MAX_LENGTH = 280

    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.length <= MAX_LENGTH) return trimmed
        var end = MAX_LENGTH
        if (Character.isHighSurrogate(trimmed[end - 1])) end -= 1
        return trimmed.substring(0, end).trimEnd().ifEmpty { null }
    }

    fun remaining(raw: String): Int = MAX_LENGTH - raw.length
}

interface SavedNotes {
    fun observeNotes(): Flow<Map<String, String>>
    suspend fun saveNote(discoveryId: String, raw: String): Boolean
}

object NoSavedNotes : SavedNotes {
    private val none = MutableStateFlow<Map<String, String>>(emptyMap())

    override fun observeNotes(): Flow<Map<String, String>> = none

    override suspend fun saveNote(discoveryId: String, raw: String): Boolean = false
}
