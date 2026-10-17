package com.example.curio.data.fake

import com.example.curio.domain.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class FixedClock(private val date: LocalDate) : Clock {
    override fun today(): LocalDate = date
    override fun now(): Instant = date.atStartOfDay().toInstant(ZoneOffset.UTC)
}
