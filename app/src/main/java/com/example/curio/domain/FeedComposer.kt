package com.example.curio.domain

object FeedComposer {
    /**
     * The day's regular feed is the topic's discoveries in DATASET ORDER (authors control the pacing).
     * DatasetValidationTest guarantees exactly DAILY_FEED_SIZE per topic for available themes.
     */
    fun feedFor(topicId: String, discoveries: List<Discovery>): List<Discovery> =
        discoveries.filter { it.topicId == topicId }
}
