package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentIndexTest {
    private val c = TestContent.index

    @Test fun findsADiscoveryById() {
        assertEquals("t1", c.discovery("a1")?.topicId)
    }

    @Test fun unknownIdIsNull() {
        assertNull(c.discovery("nope"))
    }

    @Test fun discoveriesOfATopicKeepDatasetOrder() {
        assertEquals(idsOf("a"), c.discoveriesOf("t1").map { it.id })
    }

    @Test fun unknownTopicHasNoDiscoveries() {
        assertTrue(c.discoveriesOf("zzz").isEmpty())
    }

    @Test fun topicsOfATheme() {
        assertEquals(listOf("u1", "u2"), c.topicsOf("space").map { it.id })
    }

    @Test fun themeOfATopic() {
        assertEquals("space", c.themeIdOfTopic("u2"))
    }

    @Test fun membershipMatchesTheContent() {
        assertEquals(idsOf("b").toSet(), c.membership.discoveriesOf("t2"))
    }

    @Test fun containsOnlyKnownIds() {
        assertTrue(c.contains("p6"))
        assertFalse(c.contains("x1"))
    }
}
