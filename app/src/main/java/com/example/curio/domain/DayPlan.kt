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
    /** [settingsLocked] = the user changed mode/theme today but progress already exists, so the change applies tomorrow. */
    data class Ready(val choice: DayChoice, val settingsLocked: Boolean = false) : DayPlan
    data class NeedsChoice(val date: LocalDate) : DayPlan     // Choose mode and nothing picked yet today
    data object NoContent : DayPlan                           // the theme has no topics ("coming soon")
}
