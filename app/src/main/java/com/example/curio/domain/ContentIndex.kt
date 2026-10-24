package com.example.curio.domain

/** Immutable lookup over the loaded dataset. Build it ONCE when the content loads. */
class ContentIndex(val themes: List<Theme>, val discoveries: List<Discovery>) {
    val topics: List<Topic> = themes.flatMap { it.topics }
    val membership: TopicMembership = TopicMembership.from(discoveries)

    private val discoveryById = discoveries.associateBy { it.id }
    private val discoveriesByTopic = discoveries.groupBy { it.topicId }
    private val themeByTopic = themes.flatMap { th -> th.topics.map { it.id to th.id } }.toMap()

    fun discovery(id: String): Discovery? = discoveryById[id]
    fun contains(id: String): Boolean = id in discoveryById
    fun discoveriesOf(topicId: String): List<Discovery> = discoveriesByTopic[topicId].orEmpty()   // dataset order
    fun topicsOf(themeId: String): List<Topic> = themes.firstOrNull { it.id == themeId }?.topics.orEmpty()
    fun themeIdOfTopic(topicId: String): String? = themeByTopic[topicId]
}
