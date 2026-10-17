package com.example.curio.domain

import java.time.LocalDate

interface DiscoveryRepository {
    suspend fun getDailyFeed(date: LocalDate): List<Discovery>
}
