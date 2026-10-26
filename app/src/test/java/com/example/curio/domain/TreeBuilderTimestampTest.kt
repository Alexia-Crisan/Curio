package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TreeBuilderTimestampTest {
    @Test
    fun aDoneLeafCarriesItsCompletionTime() {
        val tree = TreeBuilder.build(TestContent.index.themes[0], TestContent.index, snapshotOf(completed = setOf("a1"), firstAt = mapOf("a1" to 500L)))
        val leaf = tree.branches.flatMap { it.leaves }.first { it.discoveryId == "a1" }
        assertEquals(500L, leaf.completedAtMs)
    }

    @Test
    fun aHiddenLeafHasNoTime() {
        val tree = TreeBuilder.build(TestContent.index.themes[0], TestContent.index, snapshotOf(completed = setOf("a1"), firstAt = mapOf("a1" to 500L, "a2" to 600L)))
        val leaf = tree.branches.flatMap { it.leaves }.first { it.discoveryId == "a2" }
        assertNull(leaf.completedAtMs)
    }

    @Test
    fun aDoneLeafWithoutATimestampHasNullTime() {
        val tree = TreeBuilder.build(TestContent.index.themes[0], TestContent.index, snapshotOf(completed = setOf("a1"), firstAt = emptyMap()))
        val leaf = tree.branches.flatMap { it.leaves }.first { it.discoveryId == "a1" }
        assertNull(leaf.completedAtMs)
    }
}
