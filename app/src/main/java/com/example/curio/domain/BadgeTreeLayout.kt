package com.example.curio.domain

data class TreePoint(val x: Float, val y: Float)

data class TreeLimb(
    val from: TreePoint,
    val control: TreePoint,
    val to: TreePoint,
    val width: Float,
    val appearAt: Int,
)

enum class FoliageShade { DEEP, MID, LIGHT }

data class TreeFoliage(
    val center: TreePoint,
    val radius: Float,
    val shade: FoliageShade,
    val appearAt: Int,
    val untilLevel: Int = Int.MAX_VALUE,
)

data class TreeSlot(
    val index: Int,
    val center: TreePoint,
    val radius: Float,
    val badge: AchievementId?,
) {
    val isFilled: Boolean get() = badge != null
}

data class TreeGround(val center: TreePoint, val radiusX: Float, val radiusY: Float)

data class BadgeTreeLayout(
    val level: Int,
    val trunk: List<TreeLimb>,
    val foliage: List<TreeFoliage>,
    val slots: List<TreeSlot>,
    val ground: TreeGround,
) {
    fun isVisible(appearAt: Int, untilLevel: Int = Int.MAX_VALUE): Boolean = appearAt <= level && level < untilLevel

    companion object {
        const val WIDTH = 100f
        const val HEIGHT = 120f
        const val MAX_LEVEL = 7
        const val SLOT_RADIUS = 8.5f

        fun levelOf(progress: List<AchievementProgress>): Int =
            progress.count { it.def.mvp && it.isUnlocked }.coerceAtMost(MAX_LEVEL)

        fun unlockOrder(progress: List<AchievementProgress>): List<AchievementId> {
            val position = AchievementConfig.defs.withIndex().associate { it.value.id to it.index }
            return progress
                .filter { it.def.mvp && it.isUnlocked }
                .sortedWith(compareBy<AchievementProgress>({ it.unlockedAt }, { position[it.def.id] ?: Int.MAX_VALUE }))
                .map { it.def.id }
                .distinct()
                .take(MAX_LEVEL)
        }

        fun of(progress: List<AchievementProgress>): BadgeTreeLayout = ofOrder(unlockOrder(progress))

        fun ofOrder(unlocked: List<AchievementId>): BadgeTreeLayout {
            val order = unlocked.distinct().take(MAX_LEVEL)
            return BadgeTreeLayout(
                level = order.size,
                trunk = TRUNK,
                foliage = FOLIAGE,
                slots = TIPS.mapIndexed { index, tip -> TreeSlot(index, tip, SLOT_RADIUS, order.getOrNull(index)) },
                ground = GROUND,
            )
        }

        fun growth(animatedLevel: Float, appearAt: Int): Float = (animatedLevel - appearAt + 1f).coerceIn(0f, 1f)

        fun scaleOf(foliage: TreeFoliage, animatedLevel: Float): Float {
            val appear = growth(animatedLevel, foliage.appearAt)
            if (foliage.untilLevel == Int.MAX_VALUE) return appear
            return appear * (1f - growth(animatedLevel, foliage.untilLevel))
        }

        fun thickness(animatedLevel: Float): Float = 0.55f + 0.45f * (animatedLevel / MAX_LEVEL).coerceIn(0f, 1f)

        private val GROUND = TreeGround(TreePoint(50f, 110f), 40f, 7f)

        private val TRUNK_POINTS = listOf(TreePoint(50f, 108f), TreePoint(50f, 96f), TreePoint(50f, 60f))
        private val TRUNK_WIDTHS = listOf(14f, 12.5f)

        private val TRUNK: List<TreeLimb> = TRUNK_WIDTHS.indices.map { i ->
            val a = TRUNK_POINTS[i]
            val b = TRUNK_POINTS[i + 1]
            TreeLimb(a, TreePoint((a.x + b.x) / 2f, (a.y + b.y) / 2f), b, TRUNK_WIDTHS[i], i)
        }

        private val TIPS = listOf(
            TreePoint(50f, 54f), TreePoint(34f, 72f), TreePoint(66f, 72f), TreePoint(24f, 54f),
            TreePoint(76f, 54f), TreePoint(38f, 36f), TreePoint(62f, 36f),
        )

        private val FOLIAGE: List<TreeFoliage> = buildList {
            add(TreeFoliage(TreePoint(44f, 98f), 5f, FoliageShade.MID, 0, 1))
            add(TreeFoliage(TreePoint(56f, 98f), 5f, FoliageShade.MID, 0, 1))
            add(TreeFoliage(TreePoint(50f, 93f), 6f, FoliageShade.LIGHT, 0, 1))
            TIPS.forEachIndexed { j, tip ->
                add(TreeFoliage(TreePoint(tip.x + 2f, tip.y + 3f), 17f, FoliageShade.DEEP, j + 1))
                add(TreeFoliage(tip, 16f, FoliageShade.MID, j + 1))
                add(TreeFoliage(TreePoint(tip.x - 6f, tip.y - 7f), 5f, FoliageShade.LIGHT, j + 1))
            }
            add(TreeFoliage(TreePoint(50f, 70f), 14f, FoliageShade.DEEP, 3))
            add(TreeFoliage(TreePoint(50f, 38f), 14f, FoliageShade.DEEP, 7))
            add(TreeFoliage(TreePoint(50f, 24f), 15f, FoliageShade.DEEP, 7))
            add(TreeFoliage(TreePoint(50f, 22f), 14f, FoliageShade.MID, 7))
            add(TreeFoliage(TreePoint(45f, 16f), 5f, FoliageShade.LIGHT, 7))
        }
    }
}
