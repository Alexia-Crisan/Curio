package com.example.curio.domain

import java.time.LocalDate
import java.util.Random

object TopicConfig {
    const val SURPRISE_AVOIDS_YESTERDAY = true
    const val PLACEHOLDER_TOPIC_ID = "mars"
}

object TopicSelector {

    fun guided(date: LocalDate, topicIds: List<String>): String? =
        if (topicIds.isEmpty()) null else topicIds[Math.floorMod(date.toEpochDay(), topicIds.size.toLong()).toInt()]

    fun surprise(date: LocalDate, themeId: String, topicIds: List<String>, avoidTopicId: String? = null): String? {
        if (topicIds.isEmpty()) return null
        val seed = mix(date.toEpochDay() * 31L + themeId.hashCode())
        val first = topicIds[Random(seed).nextInt(topicIds.size)]
        if (avoidTopicId == null || first != avoidTopicId || topicIds.size == 1) return first
        val others = topicIds.filter { it != avoidTopicId }
        return others[Random(mix(seed xor REDRAW_SALT)).nextInt(others.size)]
    }

    fun choose(topicIds: List<String>, storedChoice: String?): String? =
        storedChoice?.takeIf { it in topicIds }

    fun select(
        mode: DailyMode, date: LocalDate, themeId: String,
        topicIds: List<String>, storedChoice: String? = null,
        avoidTopicId: String? = null,
        placeholderTopicId: String = TopicConfig.PLACEHOLDER_TOPIC_ID,
    ): String? {
        if (mode != DailyMode.CHOOSE && placeholderTopicId.isNotEmpty() && placeholderTopicId in topicIds) {
            return placeholderTopicId
        }
        return when (mode) {
            DailyMode.GUIDED -> guided(date, topicIds)
            DailyMode.SURPRISE -> surprise(date, themeId, topicIds, avoidTopicId)
            DailyMode.CHOOSE -> choose(topicIds, storedChoice)
        }
    }

    private const val REDRAW_SALT = 0x2545F4914F6CDD1DL

    private fun mix(value: Long): Long {
        var z = value + -7046029254386353131L
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        return z xor (z ushr 31)
    }
}
