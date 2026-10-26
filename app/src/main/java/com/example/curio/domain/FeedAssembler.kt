package com.example.curio.domain

enum class FeedItemKind { REGULAR, BONUS }

data class FeedItem(val discovery: Discovery, val kind: FeedItemKind, val isRevisit: Boolean = false) {
    val source: ViewSource get() = if (kind == FeedItemKind.BONUS) ViewSource.BONUS else ViewSource.FEED
}

object FeedAssembler {
    fun assemble(
        topicId: String,
        discoveries: List<Discovery>,
        bonus: List<Discovery> = emptyList(),
        revisitIds: Set<String> = emptySet(),
    ): List<FeedItem> {
        val regular = FeedComposer.feedFor(topicId, discoveries)
        val regularIds = regular.map { it.id }.toSet()
        val extra = bonus.distinctBy { it.id }.filter { it.id !in regularIds }
        return regular.map { FeedItem(it, FeedItemKind.REGULAR) } +
            extra.map { FeedItem(it, FeedItemKind.BONUS, isRevisit = it.id in revisitIds) }
    }
}
