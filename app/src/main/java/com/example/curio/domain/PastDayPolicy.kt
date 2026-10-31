package com.example.curio.domain

import java.time.LocalDate

sealed interface PastDayTarget {
    data object Live : PastDayTarget
    data class Past(val date: LocalDate) : PastDayTarget
    data object None : PastDayTarget
}

object PastDayPolicy {
    fun targetFor(date: LocalDate, today: LocalDate, hasChoice: Boolean): PastDayTarget = when {
        date.isAfter(today) -> PastDayTarget.None
        date == today -> PastDayTarget.Live
        hasChoice -> PastDayTarget.Past(date)
        else -> PastDayTarget.None
    }
}
