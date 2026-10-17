package com.example.curio.domain

import java.time.Instant
import java.time.LocalDate

interface Clock {
    fun today(): LocalDate
    fun now(): Instant
}
