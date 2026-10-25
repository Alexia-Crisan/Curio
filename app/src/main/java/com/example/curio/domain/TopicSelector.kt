package com.example.curio.domain

import java.time.LocalDate
import java.util.Random

object TopicSelector {

    fun guided(date: LocalDate, topicIds: List<String>): String? =
        if (topicIds.isEmpty()) null else topicIds[(date.dayOfMonth - 1) % topicIds.size]

    fun surprise(date: LocalDate, themeId: String, topicIds: List<String>): String? {
        if (topicIds.isEmpty()) return null
        val seed = mix(date.toEpochDay() * 31L + themeId.hashCode())
        return topicIds[Random(seed).nextInt(topicIds.size)]
    }

    fun choose(topicIds: List<String>, storedChoice: String?): String? =
        storedChoice?.takeIf { it in topicIds }

    fun select(
        mode: DailyMode, date: LocalDate, themeId: String,
        topicIds: List<String>, storedChoice: String? = null,
    ): String? = when (mode) {
        DailyMode.GUIDED -> guided(date, topicIds)
        DailyMode.SURPRISE -> surprise(date, themeId, topicIds)
        DailyMode.CHOOSE -> choose(topicIds, storedChoice)
    }

    private fun mix(value: Long): Long {
        var z = value + -7046029254386353131L
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        return z xor (z ushr 31)
    }
}
