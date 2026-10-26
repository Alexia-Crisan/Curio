package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TreeSelectionTest {
    private val trees = TreeBuilder.buildAll(TestContent.index, snapshotOf())

    @Test
    fun manualChoiceWinsWhenDisplayed() {
        assertEquals("space", TreeSelection.resolve(trees, "space", "animals"))
    }

    @Test
    fun anUndisplayedManualChoiceFallsBackToCurrent() {
        assertEquals("space", TreeSelection.resolve(trees, "oceans", "space"))
    }

    @Test
    fun anUndisplayedCurrentFallsBackToTheFirstTree() {
        assertEquals("animals", TreeSelection.resolve(trees, null, "oceans"))
    }

    @Test
    fun nothingChosenGivesTheFirstTree() {
        assertEquals("animals", TreeSelection.resolve(trees, null, null))
    }

    @Test
    fun noTreesGiveNull() {
        assertNull(TreeSelection.resolve(emptyList(), "animals", "animals"))
    }

    @Test
    fun unknownIdsAreIgnored() {
        assertEquals("animals", TreeSelection.resolve(trees, "nope", "nope2"))
    }
}
