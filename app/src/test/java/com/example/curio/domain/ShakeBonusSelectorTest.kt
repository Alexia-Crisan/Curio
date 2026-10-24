package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ShakeBonusSelectorTest {
    private val date = LocalDate.of(2026, 10, 26)
    private val src = Source("n", "https://example.com", "CC0")

    private fun disc(id: String, theme: String, topic: String, type: ContentType = ContentType.FACT) = Discovery(
        id = id, themeId = theme, subcategoryId = topic, type = type, title = id, source = src,
    )

    private val animalsT1 = (1..6).map { disc("a$it", "animals", "t1") }
    private val animalsT2 = (1..6).map { disc("b$it", "animals", "t2") }
    private val animalsT3 = (1..6).map { disc("c$it", "animals", "t3") }
    private val spaceU1 = (1..6).map { disc("s$it", "space", "u1") }

    private val content = animalsT1 + animalsT2 + animalsT3 + spaceU1

    private fun ev(
        id: String,
        date: LocalDate = this.date,
        source: ViewSource = ViewSource.FEED,
        at: Long = 1_000,
    ) = ViewEvent(
        discoveryId = id,
        date = date,
        firstViewedAt = at,
        dwellMs = 3_000,
        completed = true,
        skipped = false,
        source = source,
        detailDwellMs = 0,
    )

    private fun select(
        events: Collection<ViewEvent> = emptyList(),
        maxPerDay: Int = 3,
        date: LocalDate = this.date,
        list: List<Discovery> = content,
    ) = ShakeBonusSelector.select(date, "animals", "t1", list, events, maxPerDay)

    @Test fun neverReturnsTodaysTopicOrAnotherTheme() {
        var d = date
        repeat(200) {
            val res = select(date = d) as ShakeBonusSelector.Result.Found
            assertEquals("animals", res.discovery.themeId)
            assertTrue(res.discovery.topicId in setOf("t2", "t3"))
            assertFalse(res.isRevisit)
            d = d.plusDays(1)
        }
    }

    @Test fun skipsEverythingAlreadyViewedWhileUnviewedCardsRemain() {
        val events = (1..6).map { ev("b$it", date.minusDays(1)) } + (1..5).map { ev("c$it", date.minusDays(2)) }
        val res = select(events) as ShakeBonusSelector.Result.Found
        assertEquals("c6", res.discovery.id)
        assertFalse(res.isRevisit)
    }

    @Test fun detailAndBonusRowsCountAsViewed() {
        val events = (1..6).map { ev("b$it", date.minusDays(1), source = ViewSource.DETAIL) } +
                (1..5).map { ev("c$it", date.minusDays(3), source = ViewSource.BONUS) }
        val res = select(events) as ShakeBonusSelector.Result.Found
        assertEquals("c6", res.discovery.id)
    }

    @Test fun sameInputsGiveTheSameCard() {
        val first = select()
        val second = select()
        assertEquals(first, second)
    }

    @Test fun resultDoesNotDependOnTheOrderOfTheContentList() {
        val res1 = select(list = content)
        val res2 = select(list = content.shuffled(java.util.Random(7)))
        assertEquals(res1, res2)
    }

    @Test fun differentDaysCanGiveDifferentCards() {
        val idsUsed = (0..29).mapNotNull {
            val res = select(date = date.plusDays(it.toLong())) as? ShakeBonusSelector.Result.Found
            res?.discovery?.id
        }.toSet()
        assertTrue(idsUsed.size > 1)
    }

    @Test fun successiveShakesGiveDifferentCardsUntilTheCap() {
        val events = mutableListOf<ViewEvent>()
        val foundIds = mutableSetOf<String>()

        repeat(3) { i ->
            val res = select(events) as ShakeBonusSelector.Result.Found
            foundIds.add(res.discovery.id)
            events.add(ev(res.discovery.id, date, source = ViewSource.BONUS, at = (i + 1) * 2000L))
        }

        assertEquals(3, foundIds.size)
        val fourth = select(events)
        assertTrue(fourth is ShakeBonusSelector.Result.CapReached)
    }

    @Test fun capIsConfigurable() {
        val events1 = listOf(ev("b1", date, source = ViewSource.BONUS))
        assertTrue(select(events1, maxPerDay = 1) is ShakeBonusSelector.Result.CapReached)

        val events3 = (1..3).map { ev("b$it", date, source = ViewSource.BONUS) }
        assertTrue(select(events3, maxPerDay = Int.MAX_VALUE) is ShakeBonusSelector.Result.Found)
    }

    @Test fun bonusesFromEarlierDaysDoNotCountTowardToday() {
        val events = (1..3).map { ev("b$it", date.minusDays(1), source = ViewSource.BONUS) }
        assertTrue(select(events) is ShakeBonusSelector.Result.Found)
    }

    @Test fun defaultCapIsThree() {
        assertEquals(3, ShakeBonusSelector.MAX_BONUS_PER_DAY)
    }

    @Test fun revisitsTheLeastRecentlyViewedCardWhenNothingUnviewedRemains() {
        val others = animalsT2 + animalsT3
        val events = others.mapIndexed { idx, d -> ev(d.id, date.minusDays(5), at = (idx + 1) * 100L) }
        val res = select(events) as ShakeBonusSelector.Result.Found
        assertEquals("b1", res.discovery.id)
        assertTrue(res.isRevisit)
    }

    @Test fun revisitTiesAreBrokenById() {
        val others = animalsT2 + animalsT3
        val events = others.map { ev(it.id, date.minusDays(5), at = 500L) }
        val res = select(events) as ShakeBonusSelector.Result.Found
        assertEquals("b1", res.discovery.id)
    }

    @Test fun revisitUsesTheLatestViewOfEachCard() {
        val others = animalsT2 + animalsT3
        val events = others.map { ev(it.id, date.minusDays(5), at = 500L) } +
                listOf(ev("b1", date.minusDays(2), at = 9000L))
        val res = select(events) as ShakeBonusSelector.Result.Found
        assertEquals("b2", res.discovery.id)
    }

    @Test fun revisitNeverRepeatsTodaysBonus() {
        val others = animalsT2 + animalsT3
        val events = others.mapIndexed { idx, d -> ev(d.id, date.minusDays(5), at = (idx + 1) * 100L) }.toMutableList()
        val first = (select(events) as ShakeBonusSelector.Result.Found).discovery.id

        events.add(ev(first, date, source = ViewSource.BONUS, at = 5000L))
        val second = (select(events) as ShakeBonusSelector.Result.Found).discovery.id

        assertNotEquals(first, second)
    }

    @Test fun revisitStillNeverOffersTodaysTopic() {
        val allAnimals = animalsT1 + animalsT2 + animalsT3
        val events = allAnimals.map { ev(it.id, date.minusDays(4)) }
        val res = select(events) as ShakeBonusSelector.Result.Found
        assertNotEquals("t1", res.discovery.topicId)
        assertTrue(res.isRevisit)
    }

    @Test fun nothingLeftWhenTheThemeHasOnlyTodaysTopic() {
        val res = select(list = animalsT1)
        assertTrue(res is ShakeBonusSelector.Result.NothingLeft)
    }

    @Test fun nothingLeftWhenEveryOtherCardWasAlreadyOfferedToday() {
        val others = animalsT2 + animalsT3
        val events = others.map { ev(it.id, date, source = ViewSource.BONUS) }
        val res = select(events, maxPerDay = Int.MAX_VALUE)
        assertTrue(res is ShakeBonusSelector.Result.NothingLeft)
    }
}
