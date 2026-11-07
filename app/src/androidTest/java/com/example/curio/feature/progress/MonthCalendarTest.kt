package com.example.curio.feature.progress

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.curio.R
import com.example.curio.domain.DayProgress
import com.example.curio.domain.MonthProgress
import com.example.curio.ui.theme.CurioTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@RunWith(AndroidJUnit4::class)
class MonthCalendarTest {
    @get:Rule val rule = createComposeRule()
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    private val today = LocalDate.of(2026, 11, 20)
    private val complete = LocalDate.of(2026, 11, 3)
    private val started = LocalDate.of(2026, 11, 4)
    private val month = MonthProgress(
        YearMonth.of(2026, 11),
        mapOf(
            complete to DayProgress(complete, "t1", completed = 6, total = 6),
            started to DayProgress(started, "t1", completed = 2, total = 6),
        ),
    )

    private fun label(d: LocalDate, res: Int) =
        d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)) + ", " + ctx.getString(res)

    @Test fun completeAndStartedDaysAreDescribed() {
        rule.setContent { CurioTheme { MonthCalendar(month, today, onDayClick = {}) } }
        rule.onNodeWithContentDescription(label(complete, R.string.day_complete)).assertExists()
        rule.onNodeWithContentDescription(label(started, R.string.day_in_progress)).assertExists()
    }

    @Test fun futureDaysAreNotClickable() {
        var clicked: LocalDate? = null
        rule.setContent { CurioTheme { MonthCalendar(month, today) { clicked = it } } }
        rule.onNodeWithContentDescription(label(LocalDate.of(2026, 11, 25), R.string.day_not_started)).assertIsNotEnabled()
        rule.onNodeWithContentDescription(label(complete, R.string.day_complete)).performClick()
        assertEquals(complete, clicked)
    }
}
