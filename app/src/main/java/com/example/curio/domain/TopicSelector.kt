package com.example.curio.domain

import java.time.LocalDate
import java.util.Random

object TopicSelector {

    /** Guided: topics[(dayOfMonth - 1) % size] over the theme's ordered topic list. */
    fun guided(date: LocalDate, topicIds: List<String>): String? =
        if (topicIds.isEmpty()) null else topicIds[(date.dayOfMonth - 1) % topicIds.size]

    /**
     * Surprise: random-looking but a pure function of (date, theme), so stable all day and after restart.
     */
    fun surprise(date: LocalDate, themeId: String, topicIds: List<String>): String? {
        if (topicIds.isEmpty()) return null
        val seed = mix(date.toEpochDay() * 31L + themeId.hashCode())
        return topicIds[Random(seed).nextInt(topicIds.size)]
    }

    /** Choose: the stored pick, only if it belongs to this theme. No pick means null, meaning "ask the user". */
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
        var z = value + -7046029254386353131L          // 0x9E3779B97F4A7C15
        z = (z xor (z ushr 30)) * -4658895280553007687L // 0xBF58476D1CE4E5B9
        z = (z xor (z ushr 27)) * -7723592293110705685L // 0x94D049BB133111EB
        return z xor (z ushr 31)
    }
}
