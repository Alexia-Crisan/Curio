package com.example.curio.data

import com.example.curio.domain.Clock
import java.time.Instant
import java.time.LocalDate

class RealClock : Clock {
    override fun today(): LocalDate = LocalDate.now()
    override fun now(): Instant = Instant.now()
}
