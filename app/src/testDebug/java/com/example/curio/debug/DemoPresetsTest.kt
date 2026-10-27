package com.example.curio.debug

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import com.example.curio.debug.DemoPreset
import com.example.curio.debug.DemoPresets
import com.example.curio.debug.DemoPlan
import com.example.curio.domain.AchievementEvaluator
import com.example.curio.domain.AchievementId
import com.example.curio.domain.ActivitySnapshotBuilder
import com.example.curio.domain.ContentIndex
import com.example.curio.domain.DAY
import com.example.curio.domain.ProgressCalculator
import com.example.curio.domain.TestContent
import com.example.curio.domain.TreeBuilder
import com.example.curio.domain.ViewEvent
import com.example.curio.domain.ViewEventRecorder
import com.example.curio.domain.ViewSource
import com.example.curio.domain.ViewTouch
import com.example.curio.domain.ev

class DemoPresetsTest {
    private val D = DAY
    private val NOW = 1_000_000_000L

    private fun events(plan: DemoPlan): List<ViewEvent> =
        plan.touches.groupBy { it.discoveryId to it.date }.map { (_, ts) -> ts.fold<ViewTouch, ViewEvent?>(null) { acc, t -> ViewEventRecorder.merge(acc, t) }!! }

    private fun snapshot(plan: DemoPlan, extra: List<ViewEvent> = emptyList()) =
        ActivitySnapshotBuilder.build(events(plan) + extra, emptySet(), plan.choices, TestContent.index)

    private val almost by lazy { DemoPresets.build(DemoPreset.ALMOST_THERE, TestContent.index, D, NOW, "t1")!! }
    private val two by lazy { DemoPresets.build(DemoPreset.TWO_THEMES, TestContent.index, D, NOW, "t1")!! }

    @Test
    fun freshIsEmpty() {
        val plan = DemoPresets.build(DemoPreset.FRESH, TestContent.index, D, NOW, "t1")!!
        assertTrue(plan.touches.isEmpty())
        assertTrue(plan.choices.isEmpty())
    }

    @Test
    fun fromKeyMapsEveryPreset() {
        assertEquals(DemoPreset.FRESH, DemoPreset.fromKey("fresh"))
        assertEquals(DemoPreset.ALMOST_THERE, DemoPreset.fromKey("almost_there"))
        assertEquals(DemoPreset.TWO_THEMES, DemoPreset.fromKey("two_themes"))
        assertNull(DemoPreset.fromKey("nope"))
        assertNull(DemoPreset.fromKey(null))
    }

    @Test
    fun almostThereHasTwentyFourDistinctCompletedCards() {
        assertEquals(24, snapshot(almost).completedIds.size)
    }

    @Test
    fun almostThereNeverTouchesTodaysTopic() {
        val t1Ids = setOf("a1", "a2", "a3", "a4", "a5", "a6")
        assertTrue(almost.touches.none { it.discoveryId in t1Ids })
    }

    @Test
    fun almostThereHasExactlyTwoBonusCards() {
        assertEquals(2, snapshot(almost).bonusCompletedIds.size)
    }

    @Test
    fun almostThereHasThreeCompleteDays() {
        val mp = ProgressCalculator.monthProgress(YearMonth.of(2026, 10), events(almost), almost.choices, TestContent.index.membership)
        assertEquals(3, mp.daysCompleted)
    }

    @Test
    fun theFourthDayIsInProgress() {
        val fourthDate = D.minusDays(4)
        val mp = ProgressCalculator.monthProgress(YearMonth.of(2026, 10), events(almost), almost.choices, TestContent.index.membership)
        val dayProg = mp.days[fourthDate]
        assertNotNull(dayProg)
        assertEquals(4, dayProg!!.completed)
        assertFalse(dayProg.isComplete)
    }

    @Test
    fun theNextCardUnlocksCuriousMind() {
        val eval1 = AchievementEvaluator.evaluate(snapshot(almost), TestContent.index)
        assertFalse(eval1.newlyEarned.map { it.id }.contains(AchievementId.CURIOUS_MIND))

        val eval2 = AchievementEvaluator.evaluate(snapshot(almost, extra = listOf(ev("a1"))), TestContent.index)
        assertTrue(eval2.newlyEarned.map { it.id }.contains(AchievementId.CURIOUS_MIND))
    }

    @Test
    fun hiddenDiscoveriesStandsAtTwoOfThree() {
        val eval = AchievementEvaluator.evaluate(snapshot(almost), TestContent.index)
        val p = eval.progress.first { it.def.id == AchievementId.HIDDEN_DISCOVERIES }
        assertEquals(2, p.current)
        assertEquals(3, p.def.target)
    }

    @Test
    fun almostThereNeedsFiveOtherTopics() {
        val animalsIndex = ContentIndex(listOf(TestContent.index.themes[0]), TestContent.index.discoveries.filter { it.themeId == "animals" })
        assertNull(DemoPresets.build(DemoPreset.ALMOST_THERE, animalsIndex, D, NOW, "t1"))
    }

    @Test
    fun twoThemesCompletesOneTopicPerTheme() {
        val trees = TreeBuilder.buildAll(TestContent.index, snapshot(two))
        val animalsTree = trees.first { it.theme.id == "animals" }
        val spaceTree = trees.first { it.theme.id == "space" }
        assertTrue(animalsTree.branches.any { it.state == com.example.curio.domain.NodeState.COMPLETE })
        assertTrue(animalsTree.branches.any { it.state == com.example.curio.domain.NodeState.GROWING })
        assertTrue(spaceTree.branches.any { it.state == com.example.curio.domain.NodeState.COMPLETE })
        assertTrue(spaceTree.branches.any { it.state == com.example.curio.domain.NodeState.GROWING })
    }

    @Test
    fun twoThemesHasEighteenCompletedCards() {
        assertEquals(18, snapshot(two).completedIds.size)
    }

    @Test
    fun twoThemesLightsTheThemeExplorerBadge() {
        val eval = AchievementEvaluator.evaluate(snapshot(two), TestContent.index)
        val p = eval.progress.first { it.def.id == AchievementId.TOPIC_EXPLORER }
        assertEquals(2, p.current)
    }

    @Test
    fun twoThemesNeedsTwoAvailableThemes() {
        val singleThemeIndex = ContentIndex(listOf(TestContent.index.themes[0]), TestContent.index.discoveries.filter { it.themeId == "animals" })
        assertNull(DemoPresets.build(DemoPreset.TWO_THEMES, singleThemeIndex, D, NOW, "t1"))
    }

    @Test
    fun everyTouchIsCompletedFeedOrBonus() {
        val allTouches = almost.touches + two.touches
        assertTrue(allTouches.all { it.markCompleted })
        assertTrue(allTouches.all { it.source == ViewSource.FEED || it.source == ViewSource.BONUS })
        assertTrue(allTouches.all { it.dwellDeltaMs == ViewEventRecorder.COMPLETE_AT_MS })
    }

    @Test
    fun everyDateIsInThePast() {
        val allTouches = almost.touches + two.touches
        val allChoices = almost.choices + two.choices
        assertTrue(allTouches.all { it.date < D })
        assertTrue(allChoices.all { it.date < D })
    }

    @Test
    fun choiceDatesAreDistinct() {
        assertEquals(almost.choices.map { it.date }.distinct().size, almost.choices.size)
        assertEquals(two.choices.map { it.date }.distinct().size, two.choices.size)
    }

    @Test
    fun presetsAreDeterministic() {
        DemoPreset.values().forEach { preset ->
            val p1 = DemoPresets.build(preset, TestContent.index, D, NOW, "t1")
            val p2 = DemoPresets.build(preset, TestContent.index, D, NOW, "t1")
            assertEquals(p1, p2)
        }
    }
}
