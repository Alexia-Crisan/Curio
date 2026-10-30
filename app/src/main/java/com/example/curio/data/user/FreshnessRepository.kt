package com.example.curio.data.user

import com.example.curio.domain.FreshnessSource
import java.time.LocalDate

class FreshnessRepository(private val db: CurioDatabase) : FreshnessSource {
    override suspend fun seenBefore(date: LocalDate): Set<String> =
        db.viewEventDao().completedIdsBefore(date.toString()).toSet()
}
