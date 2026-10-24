package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ProgressCalculatorTest {
    private val oct26 = LocalDate.of(2026, 10, 26)
    private val octMonth = YearMonth.of(2026, 10)

    private val aCards = (1..6).map { "a$it" }.toSet()
    private val bCards = (1..6).map { "b$it" }.toSet()
    private val membership = TopicMembership(mapOf("t1" to aCards, "t2" to bCards))

    private fun ev(
        id: String,
        date: LocalDate = oct26,
        completed: Boolean = true,
        source: ViewSource = ViewSource.FEED,
    ) = ViewEvent(
        discoveryId = id,
        date = date,
        firstViewedAt = 1_000,
        dwellMs = 3_000,
        completed = completed,
        skipped = false,
        source = source,
        detailDwellMs = 0,
    )

    private fun choice(date: LocalDate = oct26, topicId: String = "t1") = DayChoice(date, "animals", topicId, DailyMode.GUIDED)

    private fun six(prefix: String = "a", date: LocalDate = oct26) = (1..6).map { ev("$prefix$it", date = date) }

    private fun oct(
        events: List<ViewEvent>,
        choices: List<DayChoice> = listOf(choice()),
    ) = ProgressCalculator.monthProgress(octMonth, events, choices, membership)

    @Test fun emptyMonthHasNothingCompleted() {
        val p = oct(emptyList(), emptyList())
        assertEquals(0, p.daysCompleted)
        assertEquals(31, p.daysInMonth)
    }

    @Test fun sixCompletedCardsOfTheChosenTopicCompleteTheDay() {
        val p = oct(six())
        assertEquals(1, p.daysCompleted)
        assertEquals(DayStatus.COMPLETE, p.statusOf(oct26))
    }

    @Test fun fiveOfSixIsInProgress() {
        val p = oct(six().dropLast(1))
        assertEquals(0, p.daysCompleted)
        assertEquals(DayStatus.IN_PROGRESS, p.statusOf(oct26))
        assertEquals(5, p.days[oct26]?.completed)
        assertEquals(6, p.days[oct26]?.total)
    }

    @Test fun viewedButNotCompletedCardsDoNotCount() {
        val events = (1..5).map { ev("a$it") } + ev("a6", completed = false)
        val p = oct(events)
        assertEquals(DayStatus.IN_PROGRESS, p.statusOf(oct26))
    }

    @Test fun fourRegularPlusTwoBonusIsNotACompleteDay() {
        val events = (1..4).map { ev("a$it") } + ev("b1", source = ViewSource.BONUS) + ev("b2", source = ViewSource.BONUS)
        val p = oct(events)
        assertEquals(0, p.daysCompleted)
        assertEquals(4, p.days[oct26]?.completed)
    }

    @Test fun fourRegularPlusTwoCardsFromAnotherTopicIsNotComplete() {
        val events = (1..4).map { ev("a$it") } + ev("b1", source = ViewSource.DETAIL) + ev("b2", source = ViewSource.DETAIL)
        val p = oct(events)
        assertEquals(0, p.daysCompleted)
    }

    @Test fun aBonusRowNeverCountsEvenForATopicCard() {
        val events = (1..5).map { ev("a$it") } + ev("a6", source = ViewSource.BONUS)
        val p = oct(events)
        assertEquals(DayStatus.IN_PROGRESS, p.statusOf(oct26))
    }

    @Test fun openingDetailOfATopicCardCountsAsCompleted() {
        val events = (1..5).map { ev("a$it") } + ev("a6", source = ViewSource.DETAIL)
        val p = oct(events)
        assertEquals(DayStatus.COMPLETE, p.statusOf(oct26))
    }

    @Test fun threeFromEachOfTwoTopicsNeverCombine() {
        val events = (1..3).map { ev("a$it") } + (1..3).map { ev("b$it") }
        val p = oct(events, listOf(choice(topicId = "t2")))
        assertEquals(0, p.daysCompleted)
        assertEquals(3, p.days[oct26]?.completed)
    }

    @Test fun eventsOnAnotherDateDoNotCount() {
        val p = oct(six(date = oct26.minusDays(1)))
        assertEquals(DayStatus.NOT_STARTED, p.statusOf(oct26))
    }

    @Test fun aDayWithoutAChoiceRowCannotBeComplete() {
        val p = oct(six(), emptyList())
        assertEquals(0, p.daysCompleted)
        assertEquals(DayStatus.NOT_STARTED, p.statusOf(oct26))
    }

    @Test fun aTopicMissingFromTheContentIsNeverComplete() {
        val p = oct(six(), listOf(choice(topicId = "removed-topic")))
        assertEquals(0, p.daysCompleted)
        assertEquals(0, p.days[oct26]?.total)
    }

    @Test fun severalCompleteDaysAreCounted() {
        val days = listOf(oct26, oct26.plusDays(1), oct26.plusDays(2))
        val choices = days.map { choice(date = it) }
        val events = days.flatMap { six(date = it) }
        val p = oct(events, choices)
        assertEquals(3, p.daysCompleted)
    }

    @Test fun monthLengths() {
        assertEquals(28, ProgressCalculator.monthProgress(YearMonth.of(2026, 2), emptyList(), emptyList(), membership).daysInMonth)
        assertEquals(29, ProgressCalculator.monthProgress(YearMonth.of(2028, 2), emptyList(), emptyList(), membership).daysInMonth)
        assertEquals(30, ProgressCalculator.monthProgress(YearMonth.of(2026, 4), emptyList(), emptyList(), membership).daysInMonth)
        assertEquals(31, ProgressCalculator.monthProgress(YearMonth.of(2026, 10), emptyList(), emptyList(), membership).daysInMonth)
    }

    @Test fun lastDayOfOctoberCountsForOctoberNotNovember() {
        val lastOct = LocalDate.of(2026, 10, 31)
        val octResult = ProgressCalculator.monthProgress(YearMonth.of(2026, 10), six(date = lastOct), listOf(choice(date = lastOct)), membership)
        val novResult = ProgressCalculator.monthProgress(YearMonth.of(2026, 11), six(date = lastOct), listOf(choice(date = lastOct)), membership)
        assertEquals(1, octResult.daysCompleted)
        assertEquals(0, novResult.daysCompleted)
    }

    @Test fun newYearsEveCountsForDecemberNotJanuary() {
        val nye = LocalDate.of(2026, 12, 31)
        val decResult = ProgressCalculator.monthProgress(YearMonth.of(2026, 12), six(date = nye), listOf(choice(date = nye)), membership)
        val janResult = ProgressCalculator.monthProgress(YearMonth.of(2027, 1), six(date = nye), listOf(choice(date = nye)), membership)
        assertEquals(1, decResult.daysCompleted)
        assertEquals(0, janResult.daysCompleted)
    }

    @Test fun leapDayIsCountedInFebruary2028() {
        val leapDay = LocalDate.of(2028, 2, 29)
        val result = ProgressCalculator.monthProgress(YearMonth.of(2028, 2), six(date = leapDay), listOf(choice(date = leapDay)), membership)
        assertEquals(1, result.daysCompleted)
        assertEquals(29, result.daysInMonth)
    }
}
