package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourcesBuilderTest {
    private fun d(id: String, name: String, license: String, url: String, credit: String) =
        TestContent.disc(id, "animals", "t1").copy(source = Source(name, url, license, credit))

    @Test
    fun emptyListGivesNoEntries() {
        assertTrue(SourcesBuilder.build(emptyList()).isEmpty())
    }

    @Test
    fun oneSourceWithManyCardsIsOneEntry() {
        val list = listOf(
            d("1", "NASA", "PD", "https://nasa.gov", "A"),
            d("2", "NASA", "PD", "https://nasa.gov", "B"),
            d("3", "NASA", "PD", "", "C"),
        )
        val entries = SourcesBuilder.build(list)
        assertEquals(1, entries.size)
        assertEquals(3, entries[0].cardCount)
        assertEquals("NASA", entries[0].name)
        assertEquals("PD", entries[0].license)
        assertEquals("https://nasa.gov", entries[0].url)
        assertEquals(listOf("A", "B", "C"), entries[0].attributions)
    }

    @Test
    fun sameNameDifferentLicenceAreSeparateEntries() {
        val list = listOf(
            d("1", "NASA", "PD", "https://nasa.gov", "A"),
            d("2", "NASA", "CC0", "https://nasa.gov", "B"),
        )
        val entries = SourcesBuilder.build(list)
        assertEquals(2, entries.size)
    }

    @Test
    fun groupingIgnoresCaseAndSpaces() {
        val list = listOf(
            d("1", "NASA ", "pd", "https://nasa.gov", "A"),
            d("2", "nasa", "PD", "https://nasa.gov", "B"),
        )
        val entries = SourcesBuilder.build(list)
        assertEquals(1, entries.size)
        assertEquals("NASA", entries[0].name)
        assertEquals("pd", entries[0].license)
    }

    @Test
    fun entriesAreSortedByNameThenLicence() {
        val list = listOf(
            d("1", "Wikipedia", "CC BY", "https://wiki.org", ""),
            d("2", "NASA", "PD", "https://nasa.gov", ""),
            d("3", "Unsplash", "CC0", "https://unsplash.com", ""),
        )
        val entries = SourcesBuilder.build(list)
        assertEquals(listOf("NASA", "Unsplash", "Wikipedia"), entries.map { it.name })
    }

    @Test
    fun attributionsAreDistinctAndSorted() {
        val list = listOf(
            d("1", "NASA", "PD", "https://nasa.gov", "B"),
            d("2", "NASA", "PD", "https://nasa.gov", "A"),
            d("3", "NASA", "PD", "https://nasa.gov", "B"),
            d("4", "NASA", "PD", "https://nasa.gov", ""),
        )
        val entries = SourcesBuilder.build(list)
        assertEquals(listOf("A", "B"), entries[0].attributions)
    }

    @Test
    fun blankUrlFallsBackToTheNextCard() {
        val list = listOf(
            d("1", "NASA", "PD", "", "A"),
            d("2", "NASA", "PD", "https://x.org", "B"),
        )
        val entries = SourcesBuilder.build(list)
        assertEquals("https://x.org", entries[0].url)
    }

    @Test
    fun allBlankUrlsKeepTheFirstValue() {
        val list = listOf(
            d("1", "NASA", "PD", "", "A"),
            d("2", "NASA", "PD", "", "B"),
        )
        val entries = SourcesBuilder.build(list)
        assertEquals("", entries[0].url)
    }

    @Test
    fun theFixtureGivesOneEntry() {
        val entries = SourcesBuilder.build(TestContent.discoveries)
        assertEquals(1, entries.size)
        assertEquals(36, entries[0].cardCount)
    }
}
