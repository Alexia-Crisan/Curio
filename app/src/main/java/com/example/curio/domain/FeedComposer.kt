package com.example.curio.domain

object FeedComposer {
    fun feedFor(topicId: String, discoveries: List<Discovery>): List<Discovery> =
        discoveries.filter { it.topicId == topicId }
}
