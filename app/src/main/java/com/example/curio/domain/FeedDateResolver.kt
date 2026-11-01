package com.example.curio.domain

import java.time.LocalDate

sealed interface FeedDateTarget {
    data object Live : FeedDateTarget
    data class Past(val date: LocalDate) : FeedDateTarget
}

object FeedDateResolver {
    fun resolve(dateArg: String?, today: LocalDate): FeedDateTarget {
        val parsed = dateArg?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: return FeedDateTarget.Live
        return if (parsed.isBefore(today)) FeedDateTarget.Past(parsed) else FeedDateTarget.Live
    }
}
