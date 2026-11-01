package com.example.curio.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface DetailSaves {
    fun observeSaved(): Flow<Set<String>>
    suspend fun setSaved(discoveryId: String, saved: Boolean, date: LocalDate)
}
