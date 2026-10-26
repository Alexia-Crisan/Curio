package com.example.curio.domain

data class SourceEntry(
    val name: String,
    val license: String,
    val url: String,
    val attributions: List<String>,
    val cardCount: Int,
)

object SourcesBuilder {
    fun build(discoveries: List<Discovery>): List<SourceEntry> =
        discoveries
            .groupBy { it.source.name.trim().lowercase() to it.source.license.trim().lowercase() }
            .values
            .map { group ->
                val first = group.first().source
                SourceEntry(
                    name = first.name.trim(),
                    license = first.license.trim(),
                    url = group.map { it.source.url }.firstOrNull { it.isNotBlank() } ?: first.url,
                    attributions = group.map { it.source.attribution.trim() }.filter { it.isNotEmpty() }.distinct().sorted(),
                    cardCount = group.size,
                )
            }
            .sortedWith(compareBy({ it.name.lowercase() }, { it.license.lowercase() }))
}
