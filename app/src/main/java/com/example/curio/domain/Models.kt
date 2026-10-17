package com.example.curio.domain

enum class ContentType { VIDEO, IMAGE, FACT, ARTICLE, QUIZ, SOURCE }

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
)
