package com.example.curio.domain

enum class ContentType { VIDEO, IMAGE, FACT, ARTICLE, QUIZ, SOURCE, KNOWLEDGE }

typealias DiscoveryType = ContentType

data class Source(
    val name: String,
    val url: String,
    val license: String,
)

data class Quiz(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
)

data class Discovery(
    val id: String,
    val themeId: String,
    val subcategoryId: String,
    val type: ContentType,
    val title: String,
    val body: String? = null,
    val mediaUrl: String? = null,
    val quiz: Quiz? = null,
    val source: Source,
    val accentColor: Long = 0xFF3B82F6,
) {
    val topicId: String get() = subcategoryId
}

data class Topic(
    val id: String,
    val themeId: String,
    val title: String,
)

data class Theme(
    val id: String,
    val title: String,
    val topics: List<Topic>,
    val available: Boolean = true,
)
