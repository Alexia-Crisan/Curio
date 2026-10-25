package com.example.curio.domain

import java.time.LocalDate

enum class DailyMode { GUIDED, CHOOSE, SURPRISE }

data class DayChoice(
    val date: LocalDate,
    val themeId: String,
    val topicId: String,
    val mode: DailyMode,
)

sealed interface DayPlan {
    data class Ready(val choice: DayChoice, val settingsLocked: Boolean = false) : DayPlan
    data class NeedsChoice(val date: LocalDate) : DayPlan
    data object NoContent : DayPlan
}
