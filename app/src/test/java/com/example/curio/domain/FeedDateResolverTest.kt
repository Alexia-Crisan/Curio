package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FeedDateResolverTest {
    private val today = LocalDate.of(2026, 10, 26)

    private fun resolve(arg: String?, now: LocalDate = today) = FeedDateResolver.resolve(arg, now)

    @Test fun aMissingDateIsTheLiveFeed() {
        assertEquals(FeedDateTarget.Live, resolve(null))
    }

    @Test fun todayIsTheLiveFeed() {
        assertEquals(FeedDateTarget.Live, resolve("2026-10-26"))
    }

    @Test fun yesterdayIsAPastDay() {
        assertEquals(FeedDateTarget.Past(LocalDate.of(2026, 10, 25)), resolve("2026-10-25"))
    }

    @Test fun tomorrowFallsBackToTheLiveFeed() {
        assertEquals(FeedDateTarget.Live, resolve("2026-10-27"))
    }

    @Test fun aFarFutureDayFallsBackToTheLiveFeed() {
        assertEquals(FeedDateTarget.Live, resolve("2027-10-26"))
    }

    @Test fun aFarPastDayIsAPastDay() {
        assertEquals(FeedDateTarget.Past(LocalDate.of(2025, 9, 21)), resolve("2025-09-21"))
    }

    @Test fun anEmptyStringFallsBackToTheLiveFeed() {
        assertEquals(FeedDateTarget.Live, resolve(""))
    }

    @Test fun textThatIsNotADateFallsBackToTheLiveFeed() {
        assertEquals(FeedDateTarget.Live, resolve("yesterday"))
    }

    @Test fun aWrongFormatFallsBackToTheLiveFeed() {
        assertEquals(FeedDateTarget.Live, resolve("25.10.2026"))
    }

    @Test fun aDateThatDoesNotExistFallsBackToTheLiveFeed() {
        assertEquals(FeedDateTarget.Live, resolve("2026-02-30"))
    }

    @Test fun theMonthBoundaryIsHandled() {
        assertEquals(
            FeedDateTarget.Past(LocalDate.of(2026, 10, 31)),
            resolve("2026-10-31", LocalDate.of(2026, 11, 1)),
        )
    }

    @Test fun theYearBoundaryIsHandled() {
        assertEquals(
            FeedDateTarget.Past(LocalDate.of(2026, 12, 31)),
            resolve("2026-12-31", LocalDate.of(2027, 1, 1)),
        )
    }

    @Test fun theLeapDayIsHandled() {
        assertEquals(
            FeedDateTarget.Past(LocalDate.of(2028, 2, 29)),
            resolve("2028-02-29", LocalDate.of(2028, 3, 1)),
        )
    }

    @Test fun thePastTargetCarriesTheExactDate() {
        val target = resolve("2026-10-20") as FeedDateTarget.Past
        assertEquals(LocalDate.of(2026, 10, 20), target.date)
    }
}
