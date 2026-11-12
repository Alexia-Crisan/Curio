package com.example.curio.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentLoaderTest {

    private fun source(url: String = "https://science.nasa.gov/mars/") =
        """{"name":"NASA","url":"$url","license":"Public domain"}"""

    private fun fact(id: String, sub: String, extra: String = "", src: String = source()) =
        """{"id":"$id","subcategoryId":"$sub","type":"FACT","title":"Title $id","body":"Body $id","source":$src$extra}"""

    private fun knowledge(id: String, sub: String) =
        """{"id":"$id","subcategoryId":"$sub","type":"KNOWLEDGE","title":"Short $id","body":"Body $id","source":${source()}}"""

    private fun topicCards(prefix: String, sub: String, count: Int = 6, withKnowledge: Boolean = true): List<String> =
        (1..count).map { n ->
            if (withKnowledge && n == count.coerceAtMost(6)) knowledge("$prefix.$n", sub) else fact("$prefix.$n", sub)
        }

    private fun padded(prefix: String, sub: String, vararg extras: String): List<String> {
        val fillers = 6 - extras.size
        return extras.toList() + (1..fillers).map { n ->
            if (n == fillers) knowledge("$prefix.k", sub) else fact("$prefix.f$n", sub)
        }
    }

    private fun subs(vararg defs: String) = defs.joinToString(",", "[", "]")

    private fun sub(id: String, order: Int? = null, accent: String? = null): String {
        val o = if (order != null) ""","guidedOrder":$order""" else ""
        val a = if (accent != null) ""","accentColor":"$accent"""" else ""
        return """{"id":"$id","name":"Name $id"$o$a}"""
    }

    private fun theme(
        id: String,
        subcategories: String,
        discoveries: List<String>,
        available: String = "",
        accent: String = "",
    ): String =
        """{"theme":{"id":"$id","name":"Theme $id"$available$accent},"subcategories":$subcategories,"discoveries":[${discoveries.joinToString(",")}]}"""

    private fun load(vararg files: Pair<String, String>) =
        ContentLoader.load(files.map { ContentFile(it.first, it.second) })

    private fun validTheme(id: String = "space") =
        theme(id, subs(sub("a", 1), sub("b", 2)), topicCards("$id.a", "a") + topicCards("$id.b", "b"))

    @Test
    fun aCompleteThemeLoadsWithoutIssues() {
        val r = load("space.json" to validTheme())
        assertTrue(r.issues.toString(), r.issues.isEmpty())
        val t = r.index.themes.single()
        assertTrue(t.available)
        assertEquals(listOf("a", "b"), t.topics.map { it.id })
        assertEquals(12, r.index.discoveries.size)
        assertEquals(6, r.index.discoveriesOf("a").size)
    }

    @Test
    fun topicsFollowGuidedOrderThenFileOrder() {
        val text = theme(
            "space",
            subs(sub("c", 3), sub("a"), sub("b", 1), sub("d", 1)),
            topicCards("s.c", "c") + topicCards("s.a", "a") + topicCards("s.b", "b") + topicCards("s.d", "d"),
        )
        val r = load("space.json" to text)
        assertEquals(listOf("b", "d", "c", "a"), r.index.themes.single().topics.map { it.id })
    }

    @Test
    fun discoveriesKeepTheOrderOfTheFile() {
        val cards = listOf(
            fact("x.3", "a"), fact("x.1", "a"), fact("x.5", "a"),
            fact("x.2", "a"), fact("x.4", "a"), knowledge("x.6", "a"),
        )
        val r = load("space.json" to theme("space", subs(sub("a")), cards))
        assertEquals(listOf("x.3", "x.1", "x.5", "x.2", "x.4", "x.6"), r.index.discoveriesOf("a").map { it.id })
    }

    @Test
    fun aTopicWithFiveDiscoveriesIsExcluded() {
        val text = theme("space", subs(sub("a"), sub("b")), topicCards("s.a", "a") + topicCards("s.b", "b", count = 5, withKnowledge = false))
        val r = load("space.json" to text)
        assertEquals(listOf("a"), r.index.themes.single().topics.map { it.id })
        assertNull(r.index.discovery("s.b.1"))
        assertTrue(r.issues.any { it.fatal && it.where.contains("b") && it.message.contains("exactly 6") })
    }

    @Test
    fun aTopicWithSevenDiscoveriesIsExcluded() {
        val text = theme("space", subs(sub("a")), topicCards("s.a", "a") + fact("s.a.7", "a"))
        val r = load("space.json" to text)
        assertTrue(r.index.themes.single().topics.isEmpty())
        assertFalse(r.index.themes.single().available)
    }

    @Test
    fun aTopicWithoutAKnowledgeCardIsExcluded() {
        val text = theme("space", subs(sub("a")), topicCards("s.a", "a", withKnowledge = false))
        val r = load("space.json" to text)
        assertTrue(r.index.themes.single().topics.isEmpty())
        assertTrue(r.issues.any { it.message.contains("KNOWLEDGE") })
    }

    @Test
    fun aThemeDeclaredUnavailableStaysUnavailableAndIsMarkedDraft() {
        val text = theme("space", subs(sub("a")), topicCards("s.a", "a"), available = ""","available":false""")
        val r = load("space.json" to text)
        val t = r.index.themes.single()
        assertFalse(t.available)
        assertEquals(1, t.topics.size)
        assertEquals(setOf("space"), r.draftThemeIds)
    }

    @Test
    fun aThemeWithoutContentCanBeListedAsComingSoon() {
        val text = theme("oceans", "[]", emptyList(), available = ""","available":false""")
        val r = load("oceans.json" to text)
        val t = r.index.themes.single()
        assertFalse(t.available)
        assertTrue(t.topics.isEmpty())
        assertTrue(r.issues.isEmpty())
    }

    @Test
    fun aThemeDeclaredAvailableWithNoCompleteTopicBecomesUnavailableWithAWarning() {
        val r = load("space.json" to theme("space", subs(sub("a")), topicCards("s.a", "a", count = 3, withKnowledge = false)))
        assertFalse(r.index.themes.single().available)
        assertTrue(r.issues.any { !it.fatal && it.where == "theme" })
    }

    @Test
    fun aBrokenFileIsReportedAndTheOthersStillLoad() {
        val r = load("a.json" to "{ not json", "b.json" to validTheme("animals"))
        assertEquals(listOf("animals"), r.index.themes.map { it.id })
        assertTrue(r.issues.single().let { it.file == "a.json" && it.fatal })
    }

    @Test
    fun aTopLevelArrayIsRejected() {
        val r = load("a.json" to "[]")
        assertTrue(r.index.themes.isEmpty())
        assertEquals(1, r.issues.size)
    }

    @Test
    fun aMissingThemeObjectIsRejected() {
        val r = load("a.json" to """{"subcategories":[],"discoveries":[]}""")
        assertTrue(r.index.themes.isEmpty())
        assertTrue(r.issues.single().fatal)
    }

    @Test
    fun aDuplicateThemeIdIsSkipped() {
        val r = load("a.json" to validTheme("space"), "b.json" to validTheme("space"))
        assertEquals(1, r.index.themes.size)
        assertEquals(12, r.index.discoveries.size)
        assertTrue(r.issues.any { it.file == "b.json" && it.message.contains("already defined") })
    }

    @Test
    fun aDuplicateDiscoveryIdAcrossFilesDropsTheLaterOne() {
        val first = validTheme("space")
        val second = theme("animals", subs(sub("z")), topicCards("space.a", "z"))
        val r = load("a.json" to first, "b.json" to second)
        assertTrue(r.issues.any { it.file == "b.json" && it.message.contains("duplicate discovery id") })
        assertTrue(r.index.themes.first { it.id == "animals" }.topics.isEmpty())
    }

    @Test
    fun aDiscoveryWithAnUnknownSubcategoryIsDropped() {
        val cards = topicCards("s.a", "a") + fact("s.x", "nope")
        val r = load("space.json" to theme("space", subs(sub("a")), cards))
        assertNull(r.index.discovery("s.x"))
        assertEquals(1, r.index.themes.single().topics.size)
        assertTrue(r.issues.any { it.message.contains("unknown subcategoryId") })
    }

    @Test
    fun anUnknownTypeIsDropped() {
        val bad = """{"id":"s.bad","subcategoryId":"a","type":"POEM","title":"t","source":${source()}}"""
        val r = load("space.json" to theme("space", subs(sub("a")), padded("s", "a", bad)))
        assertNull(r.index.discovery("s.bad"))
        assertTrue(r.issues.any { it.message.contains("unknown or missing type") })
    }

    @Test
    fun aSourceWithoutHttpsIsDropped() {
        val bad = fact("s.http", "a", src = source("http://example.com"))
        val r = load("space.json" to theme("space", subs(sub("a")), padded("s", "a", bad)))
        assertNull(r.index.discovery("s.http"))
        assertTrue(r.issues.any { it.fatal && it.message.contains("https") })
    }

    @Test
    fun aSourceWithoutLicenseIsDropped() {
        val noLicense = """{"name":"NASA","url":"https://nasa.gov/x","license":" "}"""
        val r = load("space.json" to theme("space", subs(sub("a")), padded("s", "a", fact("s.nl", "a", src = noLicense))))
        assertNull(r.index.discovery("s.nl"))
        assertTrue(r.issues.any { it.fatal && it.message.contains("license") })
    }

    @Test
    fun aSourceWithoutANameIsDropped() {
        val noName = """{"name":"","url":"https://nasa.gov/x","license":"Public domain"}"""
        val r = load("space.json" to theme("space", subs(sub("a")), padded("s", "a", fact("s.nn", "a", src = noName))))
        assertNull(r.index.discovery("s.nn"))
    }

    @Test
    fun aSourceMissingEntirelyIsDropped() {
        val noSource = """{"id":"s.ns","subcategoryId":"a","type":"FACT","title":"T"}"""
        val r = load("space.json" to theme("space", subs(sub("a")), padded("s", "a", noSource)))
        assertNull(r.index.discovery("s.ns"))
    }

    @Test
    fun aQuizIsMappedAndValidated() {
        val quiz = """{"id":"s.q","subcategoryId":"a","type":"QUIZ","title":"Test","quiz":{"question":"Q?","options":["A","B","C"],"correctIndex":1,"explanation":"E"},"source":${source()}}"""
        val r = load("space.json" to theme("space", subs(sub("a")), padded("s", "a", quiz)))
        val q = r.index.discovery("s.q")!!.quiz!!
        assertEquals(1, q.correctIndex)
        assertEquals(listOf("A", "B", "C"), q.options)
    }

    @Test
    fun aQuizWithABadCorrectIndexIsDropped() {
        val quiz = """{"id":"s.q","subcategoryId":"a","type":"QUIZ","title":"Test","quiz":{"question":"Q?","options":["A","B"],"correctIndex":2,"explanation":"E"},"source":${source()}}"""
        val r = load("space.json" to theme("space", subs(sub("a")), padded("s", "a", quiz)))
        assertNull(r.index.discovery("s.q"))
        assertTrue(r.issues.any { it.message.contains("QUIZ needs") })
    }

    @Test
    fun aQuizWithRepeatedOptionsIsDropped() {
        val quiz = """{"id":"s.q","subcategoryId":"a","type":"QUIZ","title":"Test","quiz":{"question":"Q?","options":["A","A"],"correctIndex":0,"explanation":"E"},"source":${source()}}"""
        val r = load("space.json" to theme("space", subs(sub("a")), padded("s", "a", quiz)))
        assertNull(r.index.discovery("s.q"))
    }

    @Test
    fun aQuizWithoutAQuizObjectIsDropped() {
        val quiz = """{"id":"s.q","subcategoryId":"a","type":"QUIZ","title":"Test","source":${source()}}"""
        val r = load("space.json" to theme("space", subs(sub("a")), padded("s", "a", quiz)))
        assertNull(r.index.discovery("s.q"))
    }

    @Test
    fun anArticleAndAKnowledgeCardNeedABody() {
        val article = """{"id":"s.ar","subcategoryId":"a","type":"ARTICLE","title":"T","source":${source()}}"""
        val know = """{"id":"s.kn","subcategoryId":"a","type":"KNOWLEDGE","title":"T","source":${source()}}"""
        val r = load("space.json" to theme("space", subs(sub("a")), padded("s", "a", article, know)))
        assertNull(r.index.discovery("s.ar"))
        assertNull(r.index.discovery("s.kn"))
        assertEquals(2, r.issues.count { it.fatal && it.message.contains("needs a body") })
    }

    @Test
    fun imageAndVideoPathsBecomeAssetUrls() {
        val image = """{"id":"s.im","subcategoryId":"a","type":"IMAGE","title":"T","media":"mars/dust.jpg","mediaAlt":"A dusty plain","source":${source()}}"""
        val video = """{"id":"s.vi","subcategoryId":"a","type":"VIDEO","title":"T","media":"mars/rover.mp4","mediaAlt":"A rover driving","source":${source()}}"""
        val r = load("space.json" to theme("space", subs(sub("a")), padded("s", "a", image, video)))
        val im = r.index.discovery("s.im")!!
        val vi = r.index.discovery("s.vi")!!
        assertEquals("file:///android_asset/content/media/mars/dust.jpg", im.mediaUrl)
        assertEquals("asset:///content/media/mars/rover.mp4", vi.mediaUrl)
        assertEquals("A dusty plain", im.mediaAlt)
        assertTrue(r.issues.none { it.where.contains("s.im") || it.where.contains("s.vi") })
    }

    @Test
    fun remoteAndEscapingMediaPathsAreRejectedAndARelativeOneIsKept() {
        fun image(id: String, media: String) =
            """{"id":"$id","subcategoryId":"a","type":"IMAGE","title":"T","media":"$media","mediaAlt":"x","source":${source()}}"""
        val bad = padded("s", "a", image("s.r1", "https://example.com/a.jpg"), image("s.r2", "../secret.jpg"), image("s.r3", "/abs/a.jpg"))
        val rejected = load("space.json" to theme("space", subs(sub("a")), bad))
        assertEquals(3, rejected.issues.count { it.fatal && it.message.contains("relative path") })
        val ok = load("space.json" to theme("space", subs(sub("a")), padded("s", "a", image("s.ok", "ok.jpg"))))
        assertNotNull(ok.index.discovery("s.ok"))
        assertTrue(ok.issues.isEmpty())
    }

    @Test
    fun aMissingAltTextIsAWarningAndTheCardIsKept() {
        val image = """{"id":"s.im","subcategoryId":"a","type":"IMAGE","title":"T","media":"a.jpg","source":${source()}}"""
        val r = load("space.json" to theme("space", subs(sub("a")), padded("s", "a", image)))
        assertNotNull(r.index.discovery("s.im"))
        assertNull(r.index.discovery("s.im")!!.mediaAlt)
        assertTrue(r.issues.any { !it.fatal && it.message.contains("mediaAlt") })
    }

    @Test
    fun anImageWithoutMediaIsDropped() {
        val image = """{"id":"s.im","subcategoryId":"a","type":"IMAGE","title":"T","source":${source()}}"""
        val r = load("space.json" to theme("space", subs(sub("a")), padded("s", "a", image)))
        assertNull(r.index.discovery("s.im"))
        assertTrue(r.issues.any { it.fatal && it.message.contains("needs a media path") })
    }

    @Test
    fun accentFollowsSubcategoryThenThemeThenDefault() {
        val text = theme(
            "space",
            subs(sub("a", accent = "0xFFEA580C"), sub("b")),
            padded("s.a", "a", fact("s.a", "a")) + padded("s.b", "b", fact("s.b", "b")),
            accent = ""","accentColor":"#6366F1"""",
        )
        val r = load("space.json" to text)
        assertEquals(0xFFEA580CL, r.index.discovery("s.a")!!.accentColor)
        assertEquals(0xFF6366F1L, r.index.discovery("s.b")!!.accentColor)
        val noTheme = load("x.json" to theme("x", subs(sub("a")), padded("x", "a", fact("x.a", "a"))))
        assertEquals(ContentLoader.DEFAULT_ACCENT, noTheme.index.discovery("x.a")!!.accentColor)
    }

    @Test
    fun hexColorFormatsAreParsed() {
        fun accentOf(raw: String): Long? {
            val t = theme("x", subs(sub("a", accent = raw)), padded("x", "a", fact("x.a", "a")))
            return load("x.json" to t).index.discovery("x.a")?.accentColor
        }
        assertEquals(0xFF112233L, accentOf("#112233"))
        assertEquals(0x80112233L, accentOf("#80112233"))
        assertEquals(0xFF112233L, accentOf("0xFF112233"))
        assertEquals(0xFF112233L, accentOf("FF112233"))
    }

    @Test
    fun anUnreadableColorIsAWarningAndFallsBack() {
        val t = theme("x", subs(sub("a", accent = "blue")), padded("x", "a", fact("x.a", "a")))
        val r = load("x.json" to t)
        assertEquals(ContentLoader.DEFAULT_ACCENT, r.index.discovery("x.a")!!.accentColor)
        assertTrue(r.issues.any { !it.fatal && it.message.contains("unreadable color") })
    }

    @Test
    fun filesAreReadInNameOrderRegardlessOfInputOrder() {
        val r = load("b.json" to validTheme("animals"), "a.json" to validTheme("space"))
        assertEquals(listOf("space", "animals"), r.index.themes.map { it.id })
    }

    @Test
    fun wrongJsonTypesDoNotCrash() {
        val weird = """{"theme":{"id":"x","name":5,"available":"yes"},"subcategories":[1,{"id":["a"]}],"discoveries":[null,"s",{"id":7}]}"""
        val r = load("x.json" to weird)
        assertEquals(1, r.index.themes.size)
        assertTrue(r.issues.isNotEmpty())
        assertTrue(r.index.discoveries.isEmpty())
    }

    @Test
    fun theThemeTitleComesFromTheNameField() {
        val r = load("space.json" to validTheme())
        assertEquals("Theme space", r.index.themes.single().title)
        assertEquals("Name a", r.index.themes.single().topics.first().title)
    }

    @Test
    fun theShapeOfTheShippedSampleFileIsUnderstood() {
        val cards = ((1..5).map { fact("space.mars.fact.0$it", "mars") } + knowledge("space.mars.know.01", "mars")).joinToString(",")
        val sample = """
            {
              "theme": {"id":"space","name":"Space","tagline":"Look up.","accentColor":"0xFF6366F1"},
              "subcategories":[{"id":"mars","name":"Mars","guidedOrder":1,"accentColor":"0xFFEA580C"}],
              "discoveries":[$cards]
            }
        """.trimIndent()
        val r = load("space.json" to sample)
        assertTrue(r.issues.toString(), r.issues.isEmpty())
        assertEquals(0xFFEA580CL, r.index.discovery("space.mars.fact.01")!!.accentColor)
        assertTrue(r.index.themes.single().available)
    }
}
