package com.example.curio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ArchitectureRulesTest {

    private val sources: List<File> =
        File("src/main/java").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun named(names: List<String>): List<File> = names.map { name ->
        val matches = sources.filter { it.name == name }
        assertEquals("expected exactly one file named $name", 1, matches.size)
        matches.first()
    }

    private fun violations(files: List<File>, regex: Regex, rawLines: Boolean = false): List<String> =
        files.flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                val trimmed = line.trim()
                val candidate = when {
                    rawLines -> line
                    trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*") -> ""
                    else -> line.substringBefore("//")
                }
                if (regex.containsMatchIn(candidate)) "${file.name}:${index + 1}: $trimmed" else null
            }
        }

    private fun assertNone(found: List<String>) = assertTrue(found.joinToString("\n"), found.isEmpty())

    private val pureDomainFiles = listOf(
        "ViewEvent.kt", "ViewEventRecorder.kt", "DayPlan.kt", "TopicSelector.kt", "FeedComposer.kt",
        "ProgressCalculator.kt", "ShakeBonusSelector.kt", "ContentIndex.kt", "TopicProgress.kt",
        "ActivitySnapshot.kt", "Achievement.kt", "AchievementEvaluator.kt", "Sources.kt",
        "TreeModels.kt", "TreeBuilder.kt", "TreeExplainer.kt", "TreeSelection.kt", "TreeGrowth.kt",
        "TreeVisitStore.kt", "TouchQueue.kt", "DwellTracker.kt", "FeedAssembler.kt", "Settings.kt",
        "SourcesBuilder.kt", "FreshFirst.kt", "FeedDateResolver.kt", "BadgeTreeLayout.kt",
    )

    private val clockInjectedDataFiles = listOf(
        "DayPlanRepository.kt", "ProgressRepository.kt", "BonusRepository.kt", "UserActivityRepository.kt",
        "AchievementRepository.kt", "AchievementTracker.kt", "FreshnessRepository.kt",
        "PreferencesTreeVisitStore.kt", "PreferencesSettingsStore.kt",
    )

    private val commentFreeFiles = listOf(
        "Settings.kt", "SourcesBuilder.kt", "TreeSelection.kt", "TreeExplainer.kt", "TreeGrowth.kt",
        "TreeVisitStore.kt", "FreshFirst.kt", "PreferencesSettingsStore.kt",
        "OnboardingViewModel.kt", "SettingsViewModel.kt", "TreeScreenViewModel.kt", "TreeScreen.kt",
        "TeaserLine.kt", "FreshnessRepository.kt", "PreferencesTreeVisitStore.kt",
        "OnboardingScreen.kt", "SettingsScreen.kt", "SourcesScreen.kt",
    )

    private val systemTime =
        Regex("""LocalDate\.now\(\s*\)|LocalDateTime\.now\(\s*\)|Instant\.now\(\s*\)|System\.currentTimeMillis|Clock\.system""")

    private val unseededRandomness =
        Regex("""kotlin\.random|Math\.random|\.shuffled\(\s*\)|\.random\(\s*\)|Random\.Default|[^a-zA-Z.]Random\(\s*\)""")

    @Test fun everyNamedFileExistsExactlyOnce() {
        named(pureDomainFiles)
        named(clockInjectedDataFiles)
        named(commentFreeFiles)
    }

    @Test fun pureDomainFilesDoNotImportAndroid() {
        assertNone(violations(named(pureDomainFiles), Regex("""^\s*import\s+androidx?\.""")))
    }

    @Test fun pureDomainFilesNeverReadTheSystemClock() {
        assertNone(violations(named(pureDomainFiles), systemTime))
    }

    @Test fun pureDomainFilesUseNoUnseededRandomness() {
        assertNone(violations(named(pureDomainFiles), unseededRandomness))
    }

    @Test fun clockInjectedDataFilesNeverReadTheSystemClock() {
        assertNone(violations(named(clockInjectedDataFiles), systemTime))
    }

    @Test fun noDestructiveMigrationAnywhere() {
        val buildFiles = listOf(File("build.gradle.kts"), File("build.gradle")).filter { it.isFile }
        val testSources = sources.filter { it.name != "CurioDatabase.kt" }
        assertNone(violations(testSources + buildFiles, Regex("""fallbackToDestructiveMigration""")))
    }

    @Test fun productionCodeNeverReferencesTheDemoTools() {
        assertNone(violations(sources, Regex("""DemoPreset|DemoDataSeeder|DebugDemoActivity""")))
    }

    @Test fun theCommentFreeFilesStayCommentFree() {
        assertNone(violations(named(commentFreeFiles), Regex("""^\s*(//|/\*|\*)"""), rawLines = true))
    }

    @Test fun theDatabaseStaysAtVersionOneWithExportedSchema() {
        val database = sources.first { it.readText().contains("RoomDatabase()") && it.readText().contains("@Database") }
        val text = database.readText()
        assertTrue("version must stay 1", Regex("""version\s*=\s*1\b""").containsMatchIn(text))
        assertTrue("exportSchema must stay true", Regex("""exportSchema\s*=\s*true""").containsMatchIn(text))
    }
}
