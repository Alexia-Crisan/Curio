package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgeTreeLayoutTest {

    private val mvp = AchievementConfig.defs.filter { it.mvp }

    private fun progress(def: AchievementDef, at: Long?) = AchievementProgress(def, if (at != null) def.target else 0, at)

    private fun unlockedFirst(n: Int): List<AchievementProgress> =
        AchievementConfig.defs.mapIndexed { i, d -> progress(d, if (d.mvp && i < n) 100L + i else null) }

    @Test fun sevenMvpBadgesExist() = assertEquals(7, mvp.size)

    @Test fun levelCountsOnlyUnlockedMvpBadges() {
        for (n in 0..7) assertEquals(n, BadgeTreeLayout.levelOf(unlockedFirst(n)))
    }

    @Test fun nonMvpBadgesNeverRaiseTheLevel() {
        val all = AchievementConfig.defs.map { progress(it, 500L) }
        assertEquals(7, BadgeTreeLayout.levelOf(all))
        assertEquals(7, BadgeTreeLayout.of(all).slots.count { it.isFilled })
        assertTrue(BadgeTreeLayout.unlockOrder(all).all { id -> mvp.any { it.id == id } })
    }

    @Test fun nothingUnlockedGivesLevelZeroAndNoFilledSlots() {
        val layout = BadgeTreeLayout.of(AchievementConfig.defs.map { progress(it, null) })
        assertEquals(0, layout.level)
        assertTrue(layout.slots.none { it.isFilled })
        assertEquals(7, layout.slots.size)
    }

    @Test fun emptyProgressIsLevelZero() {
        assertEquals(0, BadgeTreeLayout.of(emptyList()).level)
    }

    @Test fun badgesFillSlotsInUnlockOrderNotDefinitionOrder() {
        val a = mvp[5]
        val b = mvp[0]
        val c = mvp[3]
        val list = AchievementConfig.defs.map { d ->
            progress(d, when (d.id) { a.id -> 10L; b.id -> 20L; c.id -> 30L; else -> null })
        }
        val layout = BadgeTreeLayout.of(list)
        assertEquals(listOf(a.id, b.id, c.id), layout.slots.take(3).map { it.badge })
        assertTrue(layout.slots.drop(3).all { it.badge == null })
    }

    @Test fun sameTimestampFallsBackToDefinitionOrder() {
        val list = AchievementConfig.defs.map { progress(it, if (it.mvp) 7L else null) }
        assertEquals(mvp.map { it.id }, BadgeTreeLayout.unlockOrder(list))
    }

    @Test fun inputOrderDoesNotChangeTheLayout() {
        val list = unlockedFirst(4)
        assertEquals(BadgeTreeLayout.of(list), BadgeTreeLayout.of(list.reversed()))
        assertEquals(BadgeTreeLayout.of(list), BadgeTreeLayout.of(list))
    }

    @Test fun duplicatesAndOverflowAreIgnored() {
        val ids = mvp.map { it.id }
        val layout = BadgeTreeLayout.ofOrder(ids + ids + AchievementId.THEME_EXPLORER)
        assertEquals(7, layout.level)
        assertEquals(ids, layout.slots.map { it.badge })
        val dup = BadgeTreeLayout.ofOrder(listOf(ids[0], ids[0], ids[1]))
        assertEquals(2, dup.level)
    }

    @Test fun trunkIsContinuousAndStartsOnTheGround() {
        val t = BadgeTreeLayout.ofOrder(emptyList()).trunk
        assertEquals(t.first().from.y, 108f, 0f)
        for (i in 0 until t.size - 1) assertEquals(t[i].to, t[i + 1].from)
        assertEquals(listOf(0, 1), t.map { it.appearAt })
        assertEquals(50f, t.last().to.x, 0f)
        assertTrue(t[0].width > t[1].width)
    }

    @Test fun everythingStaysInsideTheDrawingBox() {
        val layout = BadgeTreeLayout.ofOrder(emptyList())
        val w = BadgeTreeLayout.WIDTH
        val h = BadgeTreeLayout.HEIGHT
        layout.foliage.forEach { f ->
            assertTrue(f.center.x - f.radius >= 0f && f.center.x + f.radius <= w)
            assertTrue(f.center.y - f.radius >= 0f && f.center.y + f.radius <= h)
        }
        layout.slots.forEach { s ->
            assertTrue(s.center.x - s.radius >= 0f && s.center.x + s.radius <= w)
            assertTrue(s.center.y - s.radius >= 0f && s.center.y + s.radius <= h)
        }
        val g = layout.ground
        assertTrue(g.center.x - g.radiusX >= 0f && g.center.x + g.radiusX <= w && g.center.y + g.radiusY <= h)
    }

    @Test fun trunkReachesTheCanopyBottomAtLevelOne() {
        val layout = BadgeTreeLayout.ofOrder(emptyList())
        val top = layout.trunk.last().to.y
        val firstBlob = layout.foliage.filter { it.appearAt == 1 && it.shade == FoliageShade.MID }.minOf { it.center.y + it.radius }
        assertTrue(top < firstBlob)
    }

    @Test fun slotsFormAClimbingCanopy() {
        val s = BadgeTreeLayout.ofOrder(emptyList()).slots
        assertEquals(50f, s[0].center.x, 0f)
        assertTrue(s[1].center.y > s[0].center.y && s[2].center.y > s[0].center.y)
        assertTrue(s[5].center.y < s[0].center.y && s[6].center.y < s[0].center.y)
        assertEquals(100f, s[1].center.x + s[2].center.x, 0f)
        assertEquals(100f, s[3].center.x + s[4].center.x, 0f)
        assertEquals(100f, s[5].center.x + s[6].center.x, 0f)
    }

    @Test fun slotsDoNotOverlapEachOther() {
        val s = BadgeTreeLayout.ofOrder(emptyList()).slots
        for (i in s.indices) for (j in i + 1 until s.size) {
            val dx = s[i].center.x - s[j].center.x
            val dy = s[i].center.y - s[j].center.y
            assertTrue("$i,$j", Math.sqrt((dx * dx + dy * dy).toDouble()) >= s[i].radius + s[j].radius)
        }
    }

    @Test fun slotsAreBigEnoughToTapOnASmallPhone() {
        val usableWidthDp = 280f
        val diameterDp = 2f * BadgeTreeLayout.SLOT_RADIUS * usableWidthDp / BadgeTreeLayout.WIDTH
        assertTrue(diameterDp >= 44f)
    }

    @Test fun visibleFoliageNeverShrinksAsLevelsRiseExceptTheSprout() {
        val sizes = (0..7).map { lvl ->
            val l = BadgeTreeLayout.ofOrder(mvp.take(lvl).map { it.id })
            l.foliage.count { l.isVisible(it.appearAt, it.untilLevel) }
        }
        assertEquals(3, sizes[0])
        for (i in 2..7) assertTrue("level $i", sizes[i] > sizes[i - 1])
    }

    @Test fun theSproutExistsOnlyAtLevelZero() {
        val sprout = BadgeTreeLayout.ofOrder(emptyList()).foliage.filter { it.untilLevel != Int.MAX_VALUE }
        assertEquals(3, sprout.size)
        val level0 = BadgeTreeLayout.ofOrder(emptyList())
        val level1 = BadgeTreeLayout.ofOrder(mvp.take(1).map { it.id })
        assertTrue(sprout.all { level0.isVisible(it.appearAt, it.untilLevel) })
        assertTrue(sprout.none { level1.isVisible(it.appearAt, it.untilLevel) })
    }

    @Test fun everySlotHasFoliageBehindIt() {
        val layout = BadgeTreeLayout.ofOrder(emptyList())
        layout.slots.forEach { slot ->
            assertTrue("slot ${slot.index}", layout.foliage.any { it.center == slot.center && it.appearAt == slot.index + 1 })
        }
    }

    @Test fun growthFractionRampsFromOneLevelBeforeTheElementAppears() {
        assertEquals(0f, BadgeTreeLayout.growth(2f, 3), 0f)
        assertEquals(0.5f, BadgeTreeLayout.growth(2.5f, 3), 0.0001f)
        assertEquals(1f, BadgeTreeLayout.growth(3f, 3), 0f)
        assertEquals(1f, BadgeTreeLayout.growth(7f, 3), 0f)
        assertEquals(1f, BadgeTreeLayout.growth(0f, 0), 0f)
    }

    @Test fun thicknessGrowsWithLevelAndIsClamped() {
        assertEquals(0.55f, BadgeTreeLayout.thickness(0f), 0.0001f)
        assertEquals(1f, BadgeTreeLayout.thickness(7f), 0.0001f)
        assertEquals(1f, BadgeTreeLayout.thickness(12f), 0.0001f)
        assertEquals(0.55f, BadgeTreeLayout.thickness(-3f), 0.0001f)
        for (i in 0 until 7) assertTrue(BadgeTreeLayout.thickness(i.toFloat()) < BadgeTreeLayout.thickness(i + 1f))
    }

    @Test fun filledFlagFollowsTheBadge() {
        val l = BadgeTreeLayout.ofOrder(listOf(AchievementId.FULL_DAY))
        assertTrue(l.slots[0].isFilled)
        assertFalse(l.slots[1].isFilled)
        assertNull(l.slots[1].badge)
    }

    @Test fun foliageScaleGrowsInAndTheSproutShrinksOut() {
        val layout = BadgeTreeLayout.ofOrder(emptyList())
        val sprout = layout.foliage.first { it.untilLevel == 1 }
        assertEquals(1f, BadgeTreeLayout.scaleOf(sprout, 0f), 0f)
        assertEquals(0.5f, BadgeTreeLayout.scaleOf(sprout, 0.5f), 0.0001f)
        assertEquals(0f, BadgeTreeLayout.scaleOf(sprout, 1f), 0f)
        assertEquals(0f, BadgeTreeLayout.scaleOf(sprout, 5f), 0f)
        val leaf = layout.foliage.first { it.appearAt == 3 }
        assertEquals(0f, BadgeTreeLayout.scaleOf(leaf, 2f), 0f)
        assertEquals(0.25f, BadgeTreeLayout.scaleOf(leaf, 2.25f), 0.0001f)
        assertEquals(1f, BadgeTreeLayout.scaleOf(leaf, 3f), 0f)
        assertEquals(1f, BadgeTreeLayout.scaleOf(leaf, 7f), 0f)
    }

    @Test fun everyElementIsFullyGrownAtMaxLevel() {
        val layout = BadgeTreeLayout.ofOrder(mvp.map { it.id })
        layout.foliage.filter { it.untilLevel == Int.MAX_VALUE }.forEach {
            assertEquals(1f, BadgeTreeLayout.scaleOf(it, 7f), 0f)
        }
        layout.trunk.forEach { assertEquals(1f, BadgeTreeLayout.growth(7f, it.appearAt), 0f) }
    }

    @Test fun onlyNonMvpBadgesGiveLevelZero() {
        val list = AchievementConfig.defs.map { progress(it, if (it.mvp) null else 9L) }
        assertEquals(0, BadgeTreeLayout.levelOf(list))
        assertTrue(BadgeTreeLayout.unlockOrder(list).isEmpty())
        assertEquals(0, BadgeTreeLayout.of(list).level)
    }

    @Test fun theCrownAppearsOnlyAtLevelSeven() {
        fun crown(level: Int): Int {
            val l = BadgeTreeLayout.ofOrder(mvp.take(level).map { it.id })
            return l.foliage.count { l.isVisible(it.appearAt, it.untilLevel) && it.center.y < 26f }
        }
        assertEquals(0, crown(6))
        assertTrue(crown(7) > 0)
    }
}
