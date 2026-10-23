package com.example.curio.domain

import org.junit.Assert.assertTrue
import org.junit.Test

class DatasetValidationTest {
    private val c = TestContent.index

    @Test
    fun finishedTopicsHaveAKnowledgeCard() {
        val bad = c.themes.flatMap { theme ->
            theme.topics.filter { t ->
                val items = c.discoveriesOf(t.id)
                items.size == DAILY_FEED_SIZE && items.none { it.type == ContentType.KNOWLEDGE }
            }.map { it.id }
        }
        assertTrue("Topics with six cards but no KNOWLEDGE card (no tree landmark): $bad", bad.isEmpty())
    }
}
