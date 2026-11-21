package com.example.curio.domain

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentLanguageTest {
    private val contentDir: File = listOf(
        File("src/main/assets/content"),
        File("app/src/main/assets/content"),
    ).first { it.isDirectory }

    private val jsonFiles: List<File> =
        contentDir.listFiles { f -> f.isFile && f.extension == "json" }.orEmpty().sortedBy { it.name }

    private data class Text(val owner: String, val where: String, val value: String)

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun texts(file: File): List<Text> {
        val root = Json.parseToJsonElement(file.readText(Charsets.UTF_8)) as? JsonObject ?: return emptyList()
        val out = mutableListOf<Text>()
        val theme = root["theme"] as? JsonObject
        for (key in listOf("name", "tagline")) {
            theme?.text(key)?.let { out += Text("${file.name} theme", key, it) }
        }
        (root["subcategories"] as? JsonArray).orEmpty().forEach { sub ->
            val o = sub as? JsonObject ?: return@forEach
            o.text("name")?.let { out += Text("${file.name} ${o.text("id")}", "name", it) }
        }
        (root["discoveries"] as? JsonArray).orEmpty().forEach { card ->
            val o = card as? JsonObject ?: return@forEach
            val owner = o.text("id") ?: "?"
            for (key in listOf("title", "body", "mediaAlt")) {
                o.text(key)?.let { out += Text(owner, key, it) }
            }
            val quiz = o["quiz"] as? JsonObject
            quiz?.text("question")?.let { out += Text(owner, "question", it) }
            quiz?.text("explanation")?.let { out += Text(owner, "explanation", it) }
            (quiz?.get("options") as? JsonArray).orEmpty().forEachIndexed { i, option ->
                (option as? JsonPrimitive)?.takeIf { it.isString }?.let { out += Text(owner, "options[$i]", it.content) }
            }
        }
        return out
    }

    private val all: List<Text> by lazy { jsonFiles.flatMap(::texts) }

    @Test
    fun noCedillaOrForeignLettersAreUsed() {
        val bad = all.filter { t -> t.value.any { it in FORBIDDEN } }
        assertTrue(
            "Use comma letters (ș, ț), not cedilla letters:\n" + bad.joinToString("\n") { "${it.owner} | ${it.where} | ${it.value}" },
            bad.isEmpty(),
        )
    }

    @Test
    fun noEnglishFunctionWordsRemain() {
        val bad = all.mapNotNull { t ->
            val hits = WORD.findAll(t.value).map { it.value.lowercase() }.filter { it in ENGLISH }.toSortedSet()
            if (hits.isEmpty()) null else "${t.owner} | ${t.where} | ${hits.joinToString(", ")}"
        }
        assertTrue("Text that looks English (content is Romanian):\n" + bad.joinToString("\n"), bad.isEmpty())
    }

    @Test
    fun theShippedSpaceCardsKeepTheirIds() {
        val space = jsonFiles.firstOrNull { it.name == "space.json" } ?: return
        val ids = (Json.parseToJsonElement(space.readText(Charsets.UTF_8)) as JsonObject)["discoveries"]
            .let { it as JsonArray }
            .mapNotNull { (it as? JsonObject)?.text("id") }
            .toSet()
        val missing = SHIPPED_SPACE_IDS.filter { it !in ids }
        assertTrue("Saved cards and notes point to these ids; they must not disappear:\n" + missing.joinToString("\n"), missing.isEmpty())
    }

    private companion object {
        val FORBIDDEN = setOf('ş', 'ţ', 'Ş', 'Ţ', 'ã', 'Ã')
        val WORD = Regex("[\\p{L}]+")
        val ENGLISH = setOf(
            "the", "and", "of", "is", "was", "were", "with", "from", "that", "which", "this", "its",
            "than", "you", "your", "has", "have", "not", "but", "about", "would", "their", "they",
        )
        val SHIPPED_SPACE_IDS = listOf(
            "space.mars.fact.01", "space.mars.fact.02", "space.mars.quiz.01", "space.mars.fact.03", "space.mars.fact.04", "space.mars.know.01",
            "space.moon.fact.01", "space.moon.fact.02", "space.moon.quiz.01", "space.moon.fact.03", "space.moon.fact.04", "space.moon.know.01",
            "space.sun.fact.01", "space.sun.fact.02", "space.sun.quiz.01", "space.sun.fact.03", "space.sun.fact.04", "space.sun.know.01",
        )
    }
}
