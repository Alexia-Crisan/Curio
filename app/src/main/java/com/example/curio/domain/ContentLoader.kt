package com.example.curio.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

data class ContentFile(val name: String, val text: String)

data class ContentIssue(
    val themeId: String?,
    val file: String,
    val where: String,
    val message: String,
    val fatal: Boolean,
) {
    override fun toString(): String = "${if (fatal) "ERROR" else "WARNING"} | $file | $where | $message"
}

data class ContentLoadResult(
    val index: ContentIndex,
    val issues: List<ContentIssue>,
    val draftThemeIds: Set<String>,
)

object ContentLoader {
    const val IMAGE_ROOT = "file:///android_asset/content/media/"
    const val VIDEO_ROOT = "asset:///content/media/"
    const val DEFAULT_ACCENT = 0xFF3B82F6L

    private val ID = Regex("[A-Za-z0-9._-]+")
    private val HEX = Regex("(0x|0X)?[0-9A-Fa-f]{8}")
    private val HASH = Regex("#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})")

    private class SubDef(val id: String, val title: String, val order: Int, val accent: Long?, val position: Int)

    fun load(files: List<ContentFile>): ContentLoadResult {
        val issues = mutableListOf<ContentIssue>()
        val themes = mutableListOf<Theme>()
        val discoveries = mutableListOf<Discovery>()
        val drafts = mutableSetOf<String>()
        val seenThemes = mutableSetOf<String>()
        val seenDiscoveries = mutableSetOf<String>()
        for (file in files.sortedBy { it.name }) {
            val root = parseRoot(file, issues) ?: continue
            val themeObject = root.obj("theme")
            if (themeObject == null) {
                issues += ContentIssue(null, file.name, "theme", "missing theme object", true)
                continue
            }
            val themeId = themeObject.str("id")?.trim().orEmpty()
            if (themeId.isEmpty() || !ID.matches(themeId)) {
                issues += ContentIssue(null, file.name, "theme.id", "missing or invalid theme id", true)
                continue
            }
            if (!seenThemes.add(themeId)) {
                issues += ContentIssue(themeId, file.name, "theme.id", "theme id already defined in another file", true)
                continue
            }
            val built = buildTheme(file.name, themeId, themeObject, root, seenDiscoveries, issues)
            if (themeObject.bool("available") == false) drafts += themeId
            themes += built.first
            discoveries += built.second
        }
        return ContentLoadResult(ContentIndex(themes, discoveries), issues, drafts)
    }

    private fun parseRoot(file: ContentFile, issues: MutableList<ContentIssue>): JsonObject? {
        val parsed = try {
            Json.parseToJsonElement(file.text)
        } catch (e: Exception) {
            issues += ContentIssue(null, file.name, "file", "invalid JSON: ${e.message.orEmpty().take(120)}", true)
            return null
        }
        if (parsed !is JsonObject) {
            issues += ContentIssue(null, file.name, "file", "top level must be an object", true)
            return null
        }
        return parsed
    }

    private fun buildTheme(
        fileName: String,
        themeId: String,
        themeObject: JsonObject,
        root: JsonObject,
        seenDiscoveries: MutableSet<String>,
        issues: MutableList<ContentIssue>,
    ): Pair<Theme, List<Discovery>> {
        fun issue(where: String, message: String, fatal: Boolean) {
            issues += ContentIssue(themeId, fileName, where, message, fatal)
        }

        var title = themeObject.str("name")?.trim().orEmpty()
        if (title.isEmpty()) {
            issue("theme.name", "missing theme name", false)
            title = themeId
        }
        val declaredAvailable = themeObject.bool("available") ?: true
        val themeAccent = accent(themeObject.str("accentColor"), "theme.accentColor", ::issue)

        val subs = LinkedHashMap<String, SubDef>()
        root.arr("subcategories")?.forEachIndexed { i, element ->
            val where = "subcategories[$i]"
            val o = element as? JsonObject
            if (o == null) {
                issue(where, "must be an object", true)
                return@forEachIndexed
            }
            val id = o.str("id")?.trim().orEmpty()
            if (id.isEmpty() || !ID.matches(id)) {
                issue(where, "missing or invalid id", true)
                return@forEachIndexed
            }
            if (id in subs) {
                issue(where, "duplicate subcategory id $id", true)
                return@forEachIndexed
            }
            val name = o.str("name")?.trim().orEmpty().ifEmpty {
                issue(where, "missing name for $id", false)
                id
            }
            subs[id] = SubDef(id, name, o.int("guidedOrder") ?: Int.MAX_VALUE, accent(o.str("accentColor"), "$where.accentColor", ::issue), i)
        }

        val kept = mutableListOf<Discovery>()
        root.arr("discoveries")?.forEachIndexed { i, element ->
            val o = element as? JsonObject
            val label = o?.str("id")?.let { "discoveries[$i] ($it)" } ?: "discoveries[$i]"
            if (o == null) {
                issue(label, "must be an object", true)
                return@forEachIndexed
            }
            val d = buildDiscovery(themeId, themeAccent, subs, o, label, seenDiscoveries, ::issue) ?: return@forEachIndexed
            seenDiscoveries += d.id
            kept += d
        }

        val byTopic = kept.groupBy { it.subcategoryId }
        val topics = mutableListOf<Topic>()
        val finalDiscoveries = mutableListOf<Discovery>()
        for (sub in subs.values.sortedWith(compareBy({ it.order }, { it.position }))) {
            val items = byTopic[sub.id].orEmpty()
            if (items.size != DAILY_FEED_SIZE) {
                issue("subcategory ${sub.id}", "needs exactly $DAILY_FEED_SIZE valid discoveries, has ${items.size}", true)
                continue
            }
            if (items.none { it.type == ContentType.KNOWLEDGE }) {
                issue("subcategory ${sub.id}", "needs at least one KNOWLEDGE discovery", true)
                continue
            }
            topics += Topic(sub.id, themeId, sub.title)
            finalDiscoveries += items
        }
        if (declaredAvailable && topics.isEmpty()) {
            issue("theme", "declared available but has no complete topic", false)
        }
        return Theme(themeId, title, topics, declaredAvailable && topics.isNotEmpty()) to finalDiscoveries
    }

    private fun buildDiscovery(
        themeId: String,
        themeAccent: Long?,
        subs: Map<String, SubDef>,
        o: JsonObject,
        label: String,
        seen: Set<String>,
        issue: (String, String, Boolean) -> Unit,
    ): Discovery? {
        val id = o.str("id")?.trim().orEmpty()
        if (id.isEmpty() || !ID.matches(id)) {
            issue(label, "missing or invalid id", true)
            return null
        }
        if (id in seen) {
            issue(label, "duplicate discovery id", true)
            return null
        }
        val subId = o.str("subcategoryId")?.trim().orEmpty()
        val sub = subs[subId]
        if (sub == null) {
            issue(label, "unknown subcategoryId '$subId'", true)
            return null
        }
        val type = o.str("type")?.let { raw -> ContentType.entries.firstOrNull { it.name == raw } }
        if (type == null) {
            issue(label, "unknown or missing type", true)
            return null
        }
        val title = o.str("title")?.trim().orEmpty()
        if (title.isEmpty()) {
            issue(label, "missing title", true)
            return null
        }
        val body = o.str("body")?.trim()?.takeIf { it.isNotEmpty() }
        if ((type == ContentType.KNOWLEDGE || type == ContentType.ARTICLE) && body == null) {
            issue(label, "$type needs a body", true)
            return null
        }

        val sourceObject = o.obj("source")
        val sourceName = sourceObject?.str("name")?.trim().orEmpty()
        val sourceUrl = sourceObject?.str("url")?.trim().orEmpty()
        val sourceLicense = sourceObject?.str("license")?.trim().orEmpty()
        if (sourceName.isEmpty() || sourceLicense.isEmpty() || !sourceUrl.startsWith("https://") || sourceUrl.length <= 8) {
            issue(label, "source needs name, license and an https url", true)
            return null
        }
        val source = Source(sourceName, sourceUrl, sourceLicense, sourceObject?.str("attribution")?.trim().orEmpty())

        var quiz: Quiz? = null
        if (type == ContentType.QUIZ) {
            quiz = buildQuiz(o.obj("quiz"))
            if (quiz == null) {
                issue(label, "QUIZ needs question, at least 2 options, a valid correctIndex and an explanation", true)
                return null
            }
        }

        var mediaUrl: String? = null
        var mediaAlt: String? = null
        if (type == ContentType.IMAGE || type == ContentType.VIDEO) {
            val media = o.str("media")?.trim().orEmpty()
            if (media.isEmpty()) {
                issue(label, "$type needs a media path", true)
                return null
            }
            if (!validMediaPath(media)) {
                issue(label, "media must be a relative path inside content/media (no remote urls, no '..')", true)
                return null
            }
            mediaUrl = (if (type == ContentType.IMAGE) IMAGE_ROOT else VIDEO_ROOT) + media
            mediaAlt = o.str("mediaAlt")?.trim()?.takeIf { it.isNotEmpty() }
            if (mediaAlt == null) issue(label, "missing mediaAlt (alt text)", false)
        }

        val accentValue = sub.accent ?: themeAccent ?: DEFAULT_ACCENT
        return Discovery(
            id = id,
            themeId = themeId,
            subcategoryId = subId,
            type = type,
            title = title,
            body = body,
            mediaUrl = mediaUrl,
            quiz = quiz,
            source = source,
            accentColor = accentValue,
            mediaAlt = mediaAlt,
        )
    }

    private fun buildQuiz(o: JsonObject?): Quiz? {
        if (o == null) return null
        val question = o.str("question")?.trim().orEmpty()
        val explanation = o.str("explanation")?.trim().orEmpty()
        val options = o.arr("options")?.map { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content?.trim().orEmpty() }
        val correct = o.int("correctIndex")
        if (question.isEmpty() || explanation.isEmpty() || options == null || options.size < 2) return null
        if (options.any { it.isEmpty() } || options.toSet().size != options.size) return null
        if (correct == null || correct !in options.indices) return null
        return Quiz(question, options, correct, explanation)
    }

    private fun validMediaPath(path: String): Boolean =
        !path.startsWith("/") && "://" !in path && ".." !in path && "\\" !in path

    private fun accent(raw: String?, where: String, issue: (String, String, Boolean) -> Unit): Long? {
        if (raw == null) return null
        val text = raw.trim()
        HASH.matchEntire(text)?.let { m ->
            val digits = m.groupValues[1]
            val value = java.lang.Long.parseLong(digits, 16)
            return if (digits.length == 6) 0xFF000000L or value else value
        }
        if (HEX.matches(text)) {
            return java.lang.Long.parseLong(text.removePrefix("0x").removePrefix("0X"), 16)
        }
        issue(where, "unreadable color '$raw', expected 0xAARRGGBB or #RRGGBB", false)
        return null
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull

    private fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.arr(key: String): JsonArray? = this[key] as? JsonArray
}
