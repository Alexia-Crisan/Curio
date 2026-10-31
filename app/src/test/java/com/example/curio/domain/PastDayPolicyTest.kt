package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PastDayPolicyTest {
    private val T = LocalDate.of(2026, 10, 26)

    @Test
    fun todayGoesToTheLiveFeedWithAChoice() {
        assertEquals(PastDayTarget.Live, PastDayPolicy.targetFor(T, T, true))
    }

    @Test
    fun todayGoesToTheLiveFeedWithoutAChoice() {
        assertEquals(PastDayTarget.Live, PastDayPolicy.targetFor(T, T, false))
    }

    @Test
    fun yesterdayWithAChoiceOpensThatDay() {
        val date = T.minusDays(1)
        assertEquals(PastDayTarget.Past(date), PastDayPolicy.targetFor(date, T, true))
    }

    @Test
    fun yesterdayWithoutAChoiceDoesNothing() {
        val date = T.minusDays(1)
        assertEquals(PastDayTarget.None, PastDayPolicy.targetFor(date, T, false))
    }

    @Test
    fun tomorrowDoesNothingEvenWithAChoice() {
        val date = T.plusDays(1)
        assertEquals(PastDayTarget.None, PastDayPolicy.targetFor(date, T, true))
    }

    @Test
    fun aFarFutureDayDoesNothing() {
        val date = T.plusDays(365)
        assertEquals(PastDayTarget.None, PastDayPolicy.targetFor(date, T, true))
    }

    @Test
    fun aFarPastDayWithAChoiceOpens() {
        val date = T.minusDays(400)
        assertEquals(PastDayTarget.Past(date), PastDayPolicy.targetFor(date, T, true))
    }

    @Test
    fun theMonthBoundaryIsHandled() {
        val date = LocalDate.of(2026, 10, 31)
        val today = LocalDate.of(2026, 11, 1)
        assertEquals(PastDayTarget.Past(date), PastDayPolicy.targetFor(date, today, true))
    }

    @Test
    fun theYearBoundaryIsHandled() {
        val date = LocalDate.of(2026, 12, 31)
        val today = LocalDate.of(2027, 1, 1)
        assertEquals(PastDayTarget.Past(date), PastDayPolicy.targetFor(date, today, true))
    }

    @Test
    fun theLeapDayIsHandled() {
        val date = LocalDate.of(2028, 2, 29)
        val today = LocalDate.of(2028, 3, 1)
        assertEquals(PastDayTarget.Past(date), PastDayPolicy.targetFor(date, today, true))
    }

    @Test
    fun thePastTargetCarriesTheExactDate() {
        val date = LocalDate.of(2026, 5, 15)
        val target = PastDayPolicy.targetFor(date, T, true) as PastDayTarget.Past
        assertEquals(date, target.date)
    }
}
