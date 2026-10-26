package com.example.curio.domain

import java.time.LocalDate
import kotlin.random.Random

class DailyTopicResolver {

    fun resolveTopic(
        date: LocalDate,
        mode: DailyMode,
        topics: List<String>,
        chosenTopic: String? = null,
    ): String {
        if (topics.isEmpty()) return ""

        return when (mode) {
            DailyMode.GUIDED -> {
                val index = (date.dayOfMonth - 1) % topics.size
                topics[index]
            }
            DailyMode.CHOOSE -> {
                chosenTopic ?: topics.first()
            }
            DailyMode.SURPRISE -> {
                val seed = date.toEpochDay()
                val randomIndex = Random(seed).nextInt(topics.size)
                topics[randomIndex]
            }
        }
    }
}
