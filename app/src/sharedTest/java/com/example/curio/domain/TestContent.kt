package com.example.curio.domain

import java.time.LocalDate

internal val DAY: LocalDate = LocalDate.of(2026, 10, 26)

internal fun ev(
    id: String, date: LocalDate = DAY, completed: Boolean = true,
    source: ViewSource = ViewSource.FEED, at: Long = 1_000, detail: Long = 0,
) = ViewEvent(id, date, at, if (completed) 3_000 else 1_000, completed, false, source, detail)

internal fun idsOf(prefix: String, range: IntRange = 1..6): List<String> = range.map { "$prefix$it" }

internal fun snapshotOf(
    completed: Set<String> = emptySet(),
    bonus: Set<String> = emptySet(),
    firstAt: Map<String, Long> = emptyMap(),
) = ActivitySnapshot(
    completedIds = completed, completedFeedDays = emptySet(), savedIds = emptySet(), deepReadIds = emptySet(),
    bonusCompletedIds = bonus, completedDays = emptySet(), firstCompletedAt = firstAt,
)

internal object TestContent {
    private val src = Source("n", "https://example.com", "CC0")

    fun disc(id: String, theme: String, topic: String, type: ContentType = ContentType.FACT) = Discovery(
        id = id, themeId = theme, subcategoryId = topic, type = type, title = id, source = src,
    )

    private fun topicCards(prefix: String, theme: String, topic: String) = (1..6).map {
        disc("$prefix$it", theme, topic, if (it == 6) ContentType.KNOWLEDGE else ContentType.FACT)
    }

    val discoveries: List<Discovery> =
        topicCards("a", "animals", "t1") + topicCards("b", "animals", "t2") +
        topicCards("c", "animals", "t3") + topicCards("d", "animals", "t4") +
        topicCards("s", "space", "u1") + topicCards("p", "space", "u2")

    fun theme(id: String, vararg topics: String, available: Boolean = true) = Theme(
        id = id, title = id,
        topics = topics.map { Topic(it, id, it) }, available = available,
    )

    val themes = listOf(
        theme("animals", "t1", "t2", "t3", "t4"),
        theme("space", "u1", "u2"),
        theme("oceans", available = false),
    )

    val index = ContentIndex(themes, discoveries)
}
