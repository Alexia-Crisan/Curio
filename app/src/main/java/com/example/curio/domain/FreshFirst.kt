package com.example.curio.domain

import java.time.LocalDate

object FeedConfig {
    const val FRESH_FIRST = true
}

object FreshFirst {
    fun order(cards: List<Discovery>, seenBefore: Set<String>): List<Discovery> {
        if (seenBefore.isEmpty()) return cards
        val (stale, fresh) = cards.partition { it.id in seenBefore }
        return fresh + stale
    }
}

interface FreshnessSource {
    suspend fun seenBefore(date: LocalDate): Set<String>

    companion object {
        val None: FreshnessSource = object : FreshnessSource {
            override suspend fun seenBefore(date: LocalDate): Set<String> = emptySet()
        }
    }
}
