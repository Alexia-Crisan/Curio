package com.example.curio.domain

object TestContent {
    fun disc(
        id: String,
        themeId: String,
        topicId: String,
        type: ContentType = ContentType.FACT,
    ) = Discovery(
        id = id,
        themeId = themeId,
        subcategoryId = topicId,
        type = type,
        title = id,
        source = Source("Test", "http://test.com", "CC0"),
    )

    fun topic(id: String, themeId: String = "animals") = Topic(id, themeId, "Topic $id")

    internal fun theme(id: String, vararg topicIds: String) = Theme(
        id = id,
        title = "Theme $id",
        topics = topicIds.map { topic(it, id) },
        available = id != "oceans",
    )

    val index: ContentIndex by lazy {
        val animalsTopics = listOf("t1", "t2", "t3", "t4")
        val spaceTopics = listOf("u1", "u2")

        val animalsTheme = Theme("animals", "Animals", animalsTopics.map { topic(it, "animals") })
        val spaceTheme = Theme("space", "Space", spaceTopics.map { topic(it, "space") })
        val oceansTheme = Theme("oceans", "Oceans", emptyList(), available = false)

        val discoveries = mutableListOf<Discovery>()

        val prefixes = mapOf("t1" to "a", "t2" to "b", "t3" to "c", "t4" to "d")
        prefixes.forEach { (tId, prefix) ->
            for (i in 1..5) {
                discoveries.add(disc("${prefix}$i", "animals", tId, ContentType.FACT))
            }
            discoveries.add(disc("${prefix}6", "animals", tId, ContentType.KNOWLEDGE))
        }

        val spacePrefixes = mapOf("u1" to "s", "u2" to "p")
        spacePrefixes.forEach { (tId, prefix) ->
            for (i in 1..5) {
                discoveries.add(disc("${prefix}$i", "space", tId, ContentType.FACT))
            }
            discoveries.add(disc("${prefix}6", "space", tId, ContentType.KNOWLEDGE))
        }

        ContentIndex(listOf(animalsTheme, spaceTheme, oceansTheme), discoveries)
    }
}
