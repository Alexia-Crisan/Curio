# Acceptance Traceability Matrix

| ID | Criterion (short) | Automated (class.test) | Manual check | Search hint |
|---|---|---|---|---|
| AC-A1 | 25th distinct card unlocks Curious Mind once | `DemoPresetsTest.theNextCardUnlocksCuriousMind` | Preset `almost_there`, complete one card | `CURIOUS_MIND`, `theNextCardUnlocksCuriousMind` |
| AC-A2 | Repeated views never inflate the count | `AchievementEvaluatorTest.sameCardReviewedIsCountedOnce` | none | `distinct`, `sameCard`, `repeat` in `AchievementEvaluatorTest` |
| AC-A3 | Unsaving keeps an unlocked badge | `AchievementEvaluatorTest.anUnlockedBadgeStaysUnlocked` | Save 10, unsave 3 | `anUnlockedBadgeStaysUnlocked` |
| AC-A4 | Bonus cards count for Hidden Discoveries; empty pool shows a message | `ShakeBonusSelectorTest.nothingLeftWritesNoRow` | Shake x3, shake with exhausted pool | `HIDDEN_DISCOVERIES`, `ShakeBonusSelectorTest` |
| AC-A5 | Full Day needs the six cards of the chosen topic | `ProgressCalculatorTest.fourRegularPlusTwoBonusCardsDoNotCompleteTheDay` | Choose mode run | `fourRegularPlusTwoBonus`, `ProgressCalculatorTest` |
| AC-A6 | Restart keeps badges and queue | `FullJourneyTest.restartKeepsEverything` | Force-stop after an unlock | `restartKeepsEverything`, `aRestartDoesNotEarn` |
| AC-A7 | Works fully offline | none (no network code) | Airplane-mode run | none |
| AC-A8 | Reset clears everything atomically | `FullJourneyTest.resetReturnsToAFreshApp` | Reset, then check every screen | `resetReturnsToAFreshApp`, `resetEmptiesEveryUserTable` |
| AC-A9 | Two unlocks queue and show one at a time | `AchievementsViewModelTest.bannersAreQueuedOneAtATimeInOrder` | Animator scale off | `bannersAreQueuedOneAtATimeInOrder` |
| AC-A10 | Accessibility of badges | none | TalkBack walk, Accessibility Scanner | Developer B |
| AC-T1 | Empty state: all LOCKED, stage SEED | `TreePerformanceTest.anEmptySnapshotLocksEverything` | Fresh install | `anEmptySnapshotLocksEverything` |
| AC-T2 | State and stage boundaries (42 cards: 1 / 6 / 14 / 28 / 42) | `TreeExplainerTest.fortyTwoOneDone` | none | `stageFor`, `fortyTwo` |
| AC-T3 | Same input gives the same tree | `TreePerformanceTest.treeBuildIsDeterministic` | none | `treeBuildIsDeterministic` |
| AC-T4 | Six leaves per topic, one landmark | `TreePerformanceTest.everyTopicHasSixLeavesAndALandmark` | none | `everyTopicHasSixLeavesAndALandmark`, `DatasetValidationTest` |
| AC-T5 | Completed leaf opens Detail, locked leaf leaks nothing | `TreePerformanceTest.lockedLeavesLeakNoTitles` | Tap through | `lockedLeavesLeakNoTitles`, `tappingAHiddenLeaf` |
| AC-T6 | A bonus lights a leaf in another topic without changing the day | `FullJourneyTest.aBonusCardLightsAnotherTopicWithoutChangingTheDay` | Shake, complete, open tree | `aBonusCardLightsAnotherTopic` |
| AC-T7 | Force-stop and reopen: identical tree | `FullJourneyTest.restartKeepsEverything` | Force-stop | `restartKeepsEverything` |
| AC-T8 | Build time under 5 ms | `TreePerformanceTest.treeBuildTakesUnderFiveMilliseconds` | Layout Inspector for frames | `treeBuildTakesUnderFiveMilliseconds` |
| AC-T9 | Small screen and 200 % font | none | Emulators | Developer B |
| AC-T10 | Why-text matches data; next visit equals `TopicSelector` | `TreeTeaserTest.surpriseUsesTheSelector` | none | `surpriseUsesTheSelector`, `explainTopic` |
| AC-R1 | Golden selector values unchanged | `TopicSelectorGoldenTest.guidedFiveTopics` | none | `TopicSelectorGoldenTest` |
| AC-R2 | `day_choice` survives restart; past day keeps its topic | `FullJourneyTest.aPastDayKeepsItsTopicAfterAThemeChange` | none | `aPastDayKeepsItsTopicAfterAThemeChange`, `DayPlanRepositoryTest` |
| AC-R3 | Bonus excludes today's topic and viewed cards; deterministic | `BonusRepositoryTest.revealWritesAnUncompletedBonusRowForToday` | none | `ShakeBonusSelectorTest` |
| AC-R4 | Month lengths and year boundary | `ProgressCalculatorTest.monthLengths` | none | `monthLengths`, `newYearsEve` |
| AC-R5 | Schema export matches committed JSON | `RoomSchemaExportTest.theExportedSchemaExistsAndIsVersionOne` | none | `RoomSchemaExportTest` |
| AC-R6 | Original acceptance criteria still pass on a phone | none | Run the original list on a real phone | none |
| AC-R7 | On a revisit, unseen cards come first, seen cards last, dataset order inside each group | `FreshFirstTest.seenCardsMoveToTheEnd` | Complete a topic, return to it on another day, check the order | `seenCardsMoveToTheEnd`, `aRevisitShowsUnseenCardsFirst` |
| AC-R8 | The order is stable for the whole day and across restarts; completing a card today never moves it | `FreshFirstFlowTest.theOrderIsStableWhileTheDayProgresses` | Complete a card mid-feed, leave and return | `theOrderIsStableWhileTheDayProgresses`, `theOrderIsStableAfterARestart` |
| AC-R9 | Selection and Full Day are unchanged by the ordering | `FeedAssemblerFreshTest.theSetOfRegularCardsNeverChanges` | none | `theSetOfRegularCardsNeverChanges`, `fullDayStillCompletesInTheNewOrder` |

## Gaps
- AC-A10: Manual accessibility checks reserved for Developer B's final UI pass.
- AC-T9: Manual visual/responsive checks reserved for Developer B's layout styling.
