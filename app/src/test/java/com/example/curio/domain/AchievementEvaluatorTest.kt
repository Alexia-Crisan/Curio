package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AchievementEvaluatorTest {
    private val c = TestContent.index
    private val all30 = (idsOf("a") + idsOf("b") + idsOf("c") + idsOf("d") + idsOf("s")).toSet()

    private fun snap(
        events: List<ViewEvent> = emptyList(),
        saved: Set<String> = emptySet(),
        choices: List<DayChoice> = emptyList(),
    ) = ActivitySnapshotBuilder.build(events, saved, choices, c)

    private fun eval(s: ActivitySnapshot, unlocked: Map<AchievementId, Long> = emptyMap()) =
        AchievementEvaluator.evaluate(s, c, AchievementConfig.defs, unlocked)

    private fun Evaluation.badge(id: AchievementId) = progress.first { it.def.id == id }
    private fun Evaluation.earned(id: AchievementId) = newlyEarned.any { it.id == id }

    private fun done(ids: Collection<String>, date: LocalDate = DAY, source: ViewSource = ViewSource.FEED) =
        ids.map { ev(it, date, source = source) }

    @Test fun allNineBadgesExistWithUniqueIds() {
        assertEquals(9, AchievementConfig.defs.size)
        assertEquals(9, AchievementConfig.defs.map { it.id }.toSet().size)
    }

    @Test fun sevenBadgesAreMvp() {
        assertEquals(7, AchievementConfig.defs.count { it.mvp })
        assertFalse(AchievementConfig.defs.first { it.id == AchievementId.CONSISTENT_CURIOSITY }.mvp)
        assertFalse(AchievementConfig.defs.first { it.id == AchievementId.THEME_EXPLORER }.mvp)
    }

    @Test fun nothingIsEarnedOnAFreshInstall() {
        val e = eval(snap())
        assertTrue(e.newlyEarned.isEmpty())
        assertTrue(e.progress.all { it.current == 0 && !it.isUnlocked })
    }

    @Test fun firstDiscoveryUnlocksOnTheFirstCompletedCard() {
        val e = eval(snap(done(listOf("a1"))))
        assertTrue(e.earned(AchievementId.FIRST_DISCOVERY))
    }

    @Test fun aCardThatWasOnlyViewedDoesNotCount() {
        val e = eval(snap(listOf(ev("a1", completed = false))))
        assertFalse(e.earned(AchievementId.FIRST_DISCOVERY))
    }

    @Test fun fullDayNeedsAllSixOfTheChosenTopic() {
        val choice = DayChoice(DAY, "animals", "t1", DailyMode.GUIDED)
        val full = eval(snap(done(idsOf("a")), choices = listOf(choice)))
        assertTrue(full.earned(AchievementId.FULL_DAY))

        val partial = eval(snap(done(idsOf("a", 1..5)), choices = listOf(choice)))
        assertFalse(partial.earned(AchievementId.FULL_DAY))
    }

    @Test fun bonusCardsDoNotMakeAFullDay() {
        val choice = DayChoice(DAY, "animals", "t1", DailyMode.GUIDED)
        val events = done(idsOf("a", 1..4)) + done(listOf("b1", "b2"), source = ViewSource.BONUS)
        val e = eval(snap(events, choices = listOf(choice)))
        assertFalse(e.earned(AchievementId.FULL_DAY))
    }

    @Test fun curiousMindUnlocksAtTwentyFiveDistinctCards() {
        val notEnough = eval(snap(done(all30.take(24))))
        assertFalse(notEnough.earned(AchievementId.CURIOUS_MIND))
        assertEquals(24, notEnough.badge(AchievementId.CURIOUS_MIND).current)

        val enough = eval(snap(done(all30.take(25))))
        assertTrue(enough.earned(AchievementId.CURIOUS_MIND))
    }

    @Test fun repeatedViewsNeverInflateTheCount() {
        val events = (1..10).flatMap { done(listOf("a1", "a2", "a3"), date = DAY.plusDays(it.toLong())) }
        val e = eval(snap(events))
        assertEquals(3, e.badge(AchievementId.CURIOUS_MIND).current)
    }

    @Test fun displayedProgressIsCappedAtTheTarget() {
        val e = eval(snap(done(all30)))
        assertEquals(25, e.badge(AchievementId.CURIOUS_MIND).current)
    }

    @Test fun threeExploredTopicsInOneThemeUnlockTopicExplorer() {
        val events = done(idsOf("a", 1..3)) + done(idsOf("b", 1..3)) + done(idsOf("c", 1..3))
        val e = eval(snap(events))
        assertTrue(e.earned(AchievementId.TOPIC_EXPLORER))
    }

    @Test fun twoExploredTopicsShowProgressTwo() {
        val events = done(idsOf("a", 1..3)) + done(idsOf("b", 1..3))
        val e = eval(snap(events))
        assertFalse(e.earned(AchievementId.TOPIC_EXPLORER))
        assertEquals(2, e.badge(AchievementId.TOPIC_EXPLORER).current)
    }

    @Test fun topicsInDifferentThemesDoNotCombine() {
        val events = done(idsOf("a", 1..3)) + done(idsOf("b", 1..3)) + done(idsOf("s", 1..3)) + done(idsOf("p", 1..3))
        val e = eval(snap(events))
        assertFalse(e.earned(AchievementId.TOPIC_EXPLORER))
        assertEquals(2, e.badge(AchievementId.TOPIC_EXPLORER).current)
    }

    @Test fun twoCardsDoNotMakeATopicExplored() {
        val events = done(listOf("a1", "a2", "b1", "b2", "c1", "c2"))
        val e = eval(snap(events))
        assertEquals(0, e.badge(AchievementId.TOPIC_EXPLORER).current)
    }

    @Test fun tenSavedUnlocksKnowledgeCollector() {
        val enough = eval(snap(saved = all30.take(10).toSet()))
        assertTrue(enough.earned(AchievementId.KNOWLEDGE_COLLECTOR))

        val notEnough = eval(snap(saved = all30.take(9).toSet()))
        assertFalse(notEnough.earned(AchievementId.KNOWLEDGE_COLLECTOR))
        assertEquals(9, notEnough.badge(AchievementId.KNOWLEDGE_COLLECTOR).current)
    }

    @Test fun unsavingLowersProgressOnlyWhileTheBadgeIsLocked() {
        val e = eval(snap(saved = all30.take(7).toSet()))
        assertEquals(7, e.badge(AchievementId.KNOWLEDGE_COLLECTOR).current)
    }

    @Test fun savedCardsThatLeftTheContentDoNotCount() {
        val e = eval(snap(saved = (all30.take(9) + "ghost").toSet()))
        assertEquals(9, e.badge(AchievementId.KNOWLEDGE_COLLECTOR).current)
    }

    @Test fun fiveDeepReadsUnlockDeepLearner() {
        val deepCards = listOf("a6", "b6", "c6", "d6", "s6")
        val enoughEvents = deepCards.map { ev(it, detail = 8000) }
        assertTrue(eval(snap(enoughEvents)).earned(AchievementId.DEEP_LEARNER))

        val notEnoughEvents = deepCards.take(4).map { ev(it, detail = 8000) }
        assertFalse(eval(snap(notEnoughEvents)).earned(AchievementId.DEEP_LEARNER))
    }

    @Test fun deepReadsNeedKnowledgeCards() {
        val nonKnowledgeEvents = idsOf("a", 1..5).map { ev(it, detail = 20000) }
        val e = eval(snap(nonKnowledgeEvents))
        assertEquals(0, e.badge(AchievementId.DEEP_LEARNER).current)
    }

    @Test fun threeFreshBonusCompletionsUnlockHiddenDiscoveries() {
        val events = done(listOf("b1", "c1", "d1"), source = ViewSource.BONUS)
        assertTrue(eval(snap(events)).earned(AchievementId.HIDDEN_DISCOVERIES))
    }

    @Test fun revisitedBonusCardsDoNotCount() {
        val events = done(listOf("b1", "c1", "d1"), date = DAY.minusDays(5)) +
                done(listOf("b1", "c1", "d1"), date = DAY, source = ViewSource.BONUS)
        val e = eval(snap(events))
        assertEquals(0, e.badge(AchievementId.HIDDEN_DISCOVERIES).current)
    }

    @Test fun bonusCardsThatWereOnlyRevealedDoNotCount() {
        val events = listOf("b1", "c1", "d1").map { ev(it, source = ViewSource.BONUS, completed = false) }
        val e = eval(snap(events))
        assertEquals(0, e.badge(AchievementId.HIDDEN_DISCOVERIES).current)
    }

    @Test fun fiveDifferentFeedDaysUnlockItEvenWhenNotConsecutive() {
        val days = listOf(DAY, DAY.plusDays(2), DAY.plusDays(5), DAY.plusDays(9), DAY.plusDays(20))
        val events = days.flatMap { done(listOf("a1"), date = it) }
        val e = eval(snap(events))
        assertTrue(e.earned(AchievementId.CONSISTENT_CURIOSITY))
        assertEquals(5, e.badge(AchievementId.CONSISTENT_CURIOSITY).current)
    }

    @Test fun fourDaysIsNotEnough() {
        val days = listOf(DAY, DAY.plusDays(2), DAY.plusDays(5), DAY.plusDays(9))
        val events = days.flatMap { done(listOf("a1"), date = it) }
        val e = eval(snap(events))
        assertFalse(e.earned(AchievementId.CONSISTENT_CURIOSITY))
    }

    @Test fun detailOnlyDaysDoNotCount() {
        val days = listOf(DAY, DAY.plusDays(2), DAY.plusDays(5), DAY.plusDays(9), DAY.plusDays(20))
        val events = days.flatMap { done(listOf("a1"), date = it, source = ViewSource.DETAIL) }
        val e = eval(snap(events))
        assertEquals(0, e.badge(AchievementId.CONSISTENT_CURIOSITY).current)
    }

    @Test fun twoThemesWithThreeDiscoveriesEachUnlockThemeExplorer() {
        val events = done(idsOf("a", 1..3)) + done(idsOf("s", 1..3))
        assertTrue(eval(snap(events)).earned(AchievementId.THEME_EXPLORER))
    }

    @Test fun oneThemeIsNotEnough() {
        val events = done(idsOf("a")) + done(idsOf("b"))
        val e = eval(snap(events))
        assertEquals(1, e.badge(AchievementId.THEME_EXPLORER).current)
    }

    @Test fun anUnlockedBadgeStaysUnlockedWhenTheDataNoLongerSupportsIt() {
        val unlocked = mapOf(AchievementId.KNOWLEDGE_COLLECTOR to 123L)
        val e = eval(snap(), unlocked = unlocked)
        val badge = e.badge(AchievementId.KNOWLEDGE_COLLECTOR)
        assertTrue(badge.isUnlocked)
        assertEquals(10, badge.current)
        assertEquals(123L, badge.unlockedAt)
        assertFalse(e.earned(AchievementId.KNOWLEDGE_COLLECTOR))
    }

    @Test fun alreadyUnlockedBadgesAreNeverEarnedAgain() {
        val unlocked = mapOf(AchievementId.FIRST_DISCOVERY to 1L)
        val e = eval(snap(done(listOf("a1"))), unlocked = unlocked)
        assertTrue(e.newlyEarned.isEmpty())
    }

    @Test fun evaluatingTwiceGivesNoSecondCelebration() {
        val firstEval = eval(snap(done(listOf("a1"))))
        val earnedMap = firstEval.newlyEarned.associate { it.id to 1000L }
        val secondEval = eval(snap(done(listOf("a1"))), unlocked = earnedMap)
        assertTrue(secondEval.newlyEarned.isEmpty())
    }

    @Test fun severalBadgesCanBeEarnedAtOnceInDefinitionOrder() {
        val e = eval(snap(done(all30.take(25))))
        val expectedEarned = listOf(
            AchievementId.FIRST_DISCOVERY,
            AchievementId.CURIOUS_MIND,
            AchievementId.TOPIC_EXPLORER,
        )
        assertEquals(expectedEarned, e.newlyEarned.map { it.id })
    }

    @Test fun progressFractionIsCurrentOverTarget() {
        val e = eval(snap(saved = all30.take(7).toSet()))
        val fraction = e.badge(AchievementId.KNOWLEDGE_COLLECTOR).fraction
        assertEquals(0.7f, fraction, 0.001f)
    }
}
