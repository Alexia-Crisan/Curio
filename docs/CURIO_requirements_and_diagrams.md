# Curio: Requirements and Architecture Diagrams

This document outlines the complete project specifications, feature set, user paths, wireframes, tech stack, and structural/behavioral/functional diagrams for **Curio**, an offline-first educational Android application.

---

# 1. Project Specifications

## 1.1 Project Description
Curio is an offline-first Android application that delivers a daily curated educational feed in a modern vertical pager format (one discovery card at a time). Users select a theme (e.g., Space, Animals) and a daily delivery mode (Guided, Surprise, or Choose). Each day, the app presents a structured sequence of discoveries for that day's topic. A discovery card is marked complete after 3 seconds on screen, when opened in Detail, or when bookmarked. A day is completed when all 6 discoveries of its topic are completed in the feed.

Curio rewards consistent learning through a month progress calendar, unlockable badges (Achievements), and an interactive Knowledge Tree that grows through six stages (Seed, Sprout, Sapling, Tree, Mature, Bloom) based on accumulated discoveries. Shaking the device provides bonus discovery cards (capped at 3 per day). All content is bundled locally, ensuring full offline capability from first launch.

**Success Criteria:**
- First-launch onboarding completes and renders the initial feed card in under 1 minute.
- Full functionality operates seamlessly in airplane mode without internet permissions.
- Card completion, day completion, and achievement evaluation are 100% deterministic and unit-tested.
- Vertical feed scrolling maintains a stable frame rate without jank on mid-range devices.

**Timeline & Budget:** Milestone 1 (M1) completed; Milestone 2 (M2) targeted for mid-December; final submission January 2027. Student project with zero monetary budget and two engineering developers.

## 1.2 Features (Screens and Capabilities)
1. **Onboarding**: Theme selector and daily mode picker (Guided, Surprise, Choose).
2. **Feed (Home)**: Vertical discovery pager, detail view, bookmarking, shake bonus cards, end card with tree growth teaser, and achievement unlock banners.
3. **Card Detail**: Full article text, "Pe scurt" knowledge summary, publisher source, and licence attribution.
4. **Calendar (Progress)**: Month grid tracking completed days, with tap-to-open read-only past-day feeds (`FeedDay`).
5. **Achievements**: Grid of unlocked and locked badges with detailed progress and unlock timestamps.
6. **Knowledge Tree**: Theme tree progress, topic branch states (Locked, Seeded, Growing, Complete), and landmark discovery exploration.
7. **Settings**: Theme and daily mode switching, sources and licences review, and progress reset.

```mermaid
mindmap
  root((Curio))
    Onboarding
      Choose theme
      Choose daily mode
    Daily feed
      Topic of the day
      Swipe cards
      Card detail
      Save card
      Shake bonus card
      End card with tree teaser
    Calendar
      Month progress
      Past day read-only
    Achievements
      Badge list
      Unlock banner
    Knowledge tree
      Theme tree and stage
      Topic branches
      Leaves
    Settings
      Theme and mode
      Sources
      Reset progress
```

## 1.3 User Path and User Stories
Only one primary user role exists (**Learner**), alongside the Android system as a secondary actor (clock, accelerometer sensor).

```mermaid
journey
  title Learner Path Through Curio
  section First Launch
    Pick a theme: 4: Learner
    Pick a daily mode: 4: Learner
  section Daily Learning
    Open feed with topic of the day: 5: Learner
    Swipe through cards: 5: Learner
    Open a card detail: 4: Learner
    Shake for a bonus card: 5: Learner
  section Progress
    See the day completed on the calendar: 5: Learner
    Unlock a badge: 5: Learner
    Watch the knowledge tree grow: 5: Learner
  section Maintenance
    Check sources: 3: Learner
    Reset progress: 2: Learner
```

**User Stories:**
- **US1**: As a learner, I can pick a theme and daily mode at first launch so that the feed matches my interests.
- **US2**: As a learner, I can swipe through today's cards so that I learn one small thing at a time.
- **US3**: As a learner, I can open a card's detail so that I can read more.
- **US4**: As a learner, I can save a card so that I can find it again.
- **US5**: As a learner, I can shake my phone to get a bonus card so that I can keep learning after today's topic.
- **US6**: As a learner, I can see my month on a calendar so that I know which days I completed.
- **US7**: As a learner, I can reopen a past day in read-only mode so that I can review it.
- **US8**: As a learner, I can unlock badges so that I feel rewarded for regular learning.
- **US9**: As a learner, I can see a knowledge tree so that I see how much I have learned in a theme.
- **US10**: As a learner, I can see the sources and licences of the content so that I trust it.
- **US11**: As a learner, I can reset my progress so that I can start again.
- **US12**: As a learner, I can use the whole app offline so that I can learn anywhere.

**Requirements Summary:**
- **Functional**: FR1 (Daily topic selection by mode with Surprise yesterday avoidance); FR2 (Card completion after 3s, Detail opened, or saved); FR3 (Day completion by topic feed cards); FR4 (Achievement evaluation from snapshot); FR5 (Knowledge tree branch states and growth stages); FR6 (Shake bonus cards capped at 3 per day).
- **Non-Functional**: NFR1 (Offline-first local bundled content); NFR2 (Deterministic topic selection); NFR3 (Pure Kotlin domain logic testable without Android); NFR4 (Exported Room database schema).

## 1.4 Mock-ups / Wireframes (PlantUML Salt)

```plantuml
@startsalt
{
  {+
    <b>Onboarding
    ----
    Choose a theme
    (X) Space
    () Animals
    () History
    ----
    Daily mode
    (X) Guided
    () Surprise
    () Choose
    ----
    [ Start learning ]
  } | {+
    <b>Feed
    ----
    [ Image of the topic ]
    ----
    <b>Card title
    Short text of the discovery...
    ----
    [Detail] | [Save]
    ----
    Card 3 of 6
  } | {+
    <b>Calendar - October 2026
    ----
    {#
      Mo | Tu | We | Th | Fr | Sa | Su
      . | . | . | 1 | 2 | 3 | 4
      5 | 6 | 7 | 8 | 9 | 10 | 11
    }
    ----
    Days done: 6 / 28
    [ Open past day ]
  }
}
@endsalt
```

## 1.5 Tech Stack
- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose, Material 3 (Lagoon design system)
- **Navigation**: Navigation Compose (typed routes with kotlinx.serialization)
- **Local Database**: Room (SQLite) with exported schema (`version = 1`)
- **Settings Store**: DataStore Preferences
- **Concurrency**: Kotlin Coroutines & Flows
- **Dependency Injection**: Manual `AppContainer`
- **Time & Platform**: `java.time.Clock` (injected), Android Sensor API (Accelerometer for shake detection)
- **Testing**: JUnit 4, Kotlin Coroutines Test, AndroidX Test (Room migration / instrumented tests)

---

# 2. Project Requirements: Diagrams

## 2.1 Behavioral Diagrams

### 2.1.1 Use-Case Diagram (PlantUML)
```plantuml
@startuml
left to right direction
skinparam packageStyle rectangle
actor "Learner" as U
actor "Android OS\n(shake sensor, clock)" as OS <<system>>

rectangle "Curio (offline-first Android app)" {
  usecase "Complete onboarding\n(theme + daily mode)" as UC1
  usecase "Browse daily feed" as UC2
  usecase "Open card detail" as UC3
  usecase "Save card" as UC4
  usecase "Shake for bonus card" as UC5
  usecase "View month progress" as UC6
  usecase "Open past day (read-only)" as UC7
  usecase "View achievements" as UC8
  usecase "View knowledge tree" as UC9
  usecase "Change settings" as UC10
  usecase "View sources" as UC11
  usecase "Reset progress" as UC12
  usecase "Choose topic of the day" as UC13
  usecase "Record view event" as UC14
  usecase "Evaluate achievements" as UC15
  usecase "Show unlock banner" as UC16
  usecase "Show tree growth teaser" as UC17
}

U --> UC1
U --> UC2
U --> UC3
U --> UC4
U --> UC5
U --> UC6
U --> UC7
U --> UC8
U --> UC9
U --> UC10
U --> UC11
UC5 <-- OS
UC2 ..> UC14 : <<include>>
UC3 ..> UC14 : <<include>>
UC5 ..> UC14 : <<include>>
UC14 ..> UC15 : <<include>>
UC13 .up.> UC2 : <<extend>>\n[mode = CHOOSE]
UC16 .up.> UC15 : <<extend>>\n[badge earned]
UC17 .up.> UC2 : <<extend>>\n[end of feed]
UC7 ..> UC6 : <<include>>
UC12 .up.> UC10 : <<extend>>\n[learner confirms]
@enduml
```

### 2.1.2 Collaboration Diagram (PlantUML)
```plantuml
@startuml
skinparam componentStyle uml1
actor Learner
rectangle ":FeedScreen" as FS
rectangle ":FeedViewModel" as VM
rectangle ":DwellTracker" as DT
rectangle ":TouchQueue" as TQ
rectangle ":ViewEventRecorder" as REC
rectangle ":ViewEventDao" as DAO
rectangle ":AchievementEvaluator" as AE
rectangle ":AchievementRepository" as AR

Learner -right-> FS : "1: swipe to next card"
FS -right-> VM : "2: onCardShown(id)"
VM -down-> DT : "3: show(id, FEED, now)"
DT --> VM : "3.1: touches"
VM -right-> TQ : "4: submit(touches)"
TQ -down-> REC : "5: merge(existing, touch)"
REC --> TQ : "5.1: ViewEvent"
TQ -right-> DAO : "6: upsert(event)"
TQ -down-> AE : "7: evaluate(snapshot)"
AE --> AR : "8: save new unlocks"
AR ..> VM : "8.1: banner queue (Flow)"
VM ..> FS : "9: FeedUiState (Flow)"
@enduml
```

### 2.1.3 Sequence Diagrams (PlantUML)
**Browsing the Feed:**
```plantuml
@startuml
actor Learner
participant FeedScreen
participant FeedViewModel
participant DayPlanRepository
participant FeedAssembler
participant DwellTracker
participant TouchQueue
database "Room DB" as DB
participant AchievementEvaluator

Learner -> FeedScreen : open app
FeedScreen -> FeedViewModel : init(today)
FeedViewModel -> DayPlanRepository : ensureToday(mode)
DayPlanRepository -> DB : read day_choice & yesterday choice
DB --> DayPlanRepository : stored choice / avoid topic
DayPlanRepository -> DayPlanRepository : TopicSelector.select(mode, date, theme, avoid)
DayPlanRepository -> DB : write day_choice
DayPlanRepository --> FeedViewModel : topicId
FeedViewModel -> FeedAssembler : assemble(topicId, completedBefore)
FeedAssembler --> FeedViewModel : ordered FeedItems
FeedViewModel --> FeedScreen : FeedUiState(items)

loop for each card shown
  Learner -> FeedScreen : swipe
  FeedScreen -> FeedViewModel : onCardShown(id)
  FeedViewModel -> DwellTracker : show(id, FEED, now)
  DwellTracker --> FeedViewModel : touches
  FeedViewModel -> TouchQueue : submit(touches)
  TouchQueue -> DB : upsert view_event (merged)
end
@enduml
```

### 2.1.4 Activity Diagram (PlantUML)
```plantuml
@startuml
start
:Open app;
:Read today's date from injected clock;
if (Onboarding done?) then (no)
  :Choose theme and daily mode;
  :Save settings;
else (yes)
endif
if (Day choice already stored?) then (no)
  switch (Daily mode)
  case (GUIDED)
    :Topic = guided rotation by date;
  case (SURPRISE)
    :Topic = seeded random;
    if (Equals yesterday's topic?) then (yes)
      :Redraw avoiding yesterday's topic;
    else (no)
    endif
  case (CHOOSE)
    :Learner picks a topic;
  endswitch
  :Store day choice;
else (yes)
endif
:Build feed (unseen cards first);
repeat
  :Show card;
  fork
    :Dwell timer ticks every 3 s;
  fork again
    :Learner may open detail or save;
  end fork
  if (Settled >= 3 s or detail or saved?) then (yes)
    :Mark card completed;
    :Evaluate achievements;
  else (no)
    :Mark card skipped;
  endif
repeat while (More cards?) is (yes)
->no;
if (All cards completed?) then (yes)
  :Day counts as completed;
  :Grow tree leaf and stage;
else (no)
endif
:Show end card with tree teaser;
stop
@enduml
```

## 2.2 Structural Diagrams

### 2.2.1 Class Diagram (PlantUML)
```plantuml
@startuml
skinparam classAttributeIconSize 0
enum DiscoveryType {
  FACT
  KNOWLEDGE
  QUIZ
  VIDEO
  ARTICLE
  IMAGE
  SOURCE
}
enum ViewSource {
  FEED
  BONUS
  DETAIL
}
enum DailyMode {
  GUIDED
  SURPRISE
  CHOOSE
}
enum NodeState {
  LOCKED
  SEEDED
  GROWING
  COMPLETE
}
enum GrowthStage {
  SEED
  SPROUT
  SAPLING
  TREE
  MATURE
  BLOOM
}

class Theme {
  +id: String
  +title: String
  +available: Boolean
}
class Topic {
  +id: String
  +themeId: String
  +title: String
}
class Discovery {
  +id: String
  +themeId: String
  +subcategoryId: String
  +type: DiscoveryType
  +title: String
  +body: String?
}
class Source {
  +name: String
  +url: String
  +license: String
}
class ViewEvent {
  +discoveryId: String
  +date: LocalDate
  +dwellMs: Long
  +completed: Boolean
  +source: ViewSource
  +detailDwellMs: Long
}
class DayChoice {
  +date: LocalDate
  +themeId: String
  +topicId: String
  +mode: DailyMode
}
class SavedDiscovery {
  +discoveryId: String
  +savedAt: Long
}
class AchievementUnlock {
  +achievementId: String
  +unlockedAt: Long
  +notifiedAt: Long
}
class ActivitySnapshot {
  +completedIds: Set<String>
  +completedFeedDays: Set<LocalDate>
  +savedIds: Set<String>
}

class TopicSelector <<object>> {
  +guided(date, topicIds): String
  +surprise(date, themeId, topicIds, avoid): String
}
class ProgressCalculator <<object>> {
  +dayProgress(choice, events, membership): DayProgress
}
class AchievementEvaluator <<object>> {
  +evaluate(snapshot, index): AchievementEvaluation
}
class TreeBuilder <<object>> {
  +build(theme, index, snapshot): ThemeTree
  +stageFor(done, total): GrowthStage
}
class FeedViewModel {
  +state: StateFlow<FeedUiState>
  +onPageSettled(index)
  +onShake()
}

Theme "1" *-- "many" Topic
Topic "1" o-- "many" Discovery
Discovery *-- Source
Discovery --> DiscoveryType
ViewEvent "many" --> "1" Discovery
ViewEvent --> ViewSource
DayChoice --> DailyMode
DayChoice "many" --> "1" Topic
SavedDiscovery "many" --> "1" Discovery
ActivitySnapshot ..> ViewEvent : derived from
AchievementEvaluator ..> ActivitySnapshot
AchievementEvaluator ..> AchievementUnlock : produces
TreeBuilder ..> ThemeTree : creates
TreeBuilder ..> ActivitySnapshot
ProgressCalculator ..> ViewEvent
ProgressCalculator ..> DayChoice
FeedViewModel ..> TopicSelector
FeedViewModel ..> TreeBuilder
FeedViewModel ..> AchievementEvaluator
@enduml
```

### 2.2.2 Component Diagram (PlantUML)
```plantuml
@startuml
skinparam componentStyle uml2
package "UI Layer (Jetpack Compose, Material 3)" {
  [OnboardingScreen] as ONB
  [FeedScreen] as FEED
  [DetailScreen] as DETAIL
  [ProgressScreen] as CAL
  [AchievementsScreen] as ACH
  [SettingsScreen] as SET
  [Navigation (CurioNavHost)] as NAV
}
package "Presentation Layer" {
  [FeedViewModel] as FVM
  [DetailViewModel] as DVM
  [ProgressViewModel] as PVM
  [AchievementsViewModel] as AVM
  [SettingsViewModel] as SVM
}
package "Domain Layer (Pure Kotlin)" {
  [TopicSelector] as TS
  [ProgressCalculator] as PC
  [AchievementEvaluator] as AE
  [TreeBuilder / TreeGrowth] as TB
  [ShakeBonusSelector] as SB
  [TouchQueue / DwellTracker] as DW
}
package "Data Layer" {
  [Repositories] as REPO
  database "Room (SQLite)" as ROOM
  database "Preferences DataStore" as DS
  [Bundled Content JSON] as CONTENT
}
[AppContainer] as DI
[Accelerometer Sensor] as SENS

NAV --> ONB
NAV --> FEED
NAV --> DETAIL
NAV --> CAL
NAV --> ACH
NAV --> SET
FEED --> FVM
DETAIL --> DVM
CAL --> PVM
ACH --> AVM
SET --> SVM
SENS --> FVM
FVM --> TS
FVM --> DW
FVM --> SB
AVM --> AE
PVM --> TB
FVM --> REPO
DVM --> REPO
PVM --> REPO
AVM --> REPO
SVM --> REPO
REPO --> PC
REPO --> ROOM
REPO --> DS
REPO --> CONTENT
DI ..> REPO : provides
DI ..> FVM : provides
@enduml
```

---

# 3. Pseudocode of Main Rules

**Card Completion (F0):**
```kotlin
fun isCardCompleted(event: ViewEvent): Boolean =
    event.completed || event.dwellMs >= 3000L || event.detailDwellMs > 0L
```

**Day Completion (F1):**
```kotlin
fun dayProgress(choice: DayChoice, events: Collection<ViewEvent>, membership: TopicMembership): DayProgress {
    val topicItems = membership.discoveriesOf(choice.topicId)
    val done = events.asSequence()
        .filter { it.date == choice.date }
        .filter { it.completed && it.source != ViewSource.BONUS }
        .filter { it.discoveryId in topicItems }
        .map { it.discoveryId }
        .toSet().size
    return DayProgress(choice.date, choice.topicId, done, topicItems.size)
}
```

**Surprise Mode Avoidance (Step 19 / P8):**
```kotlin
fun surprise(date: LocalDate, themeId: String, topicIds: List<String>, avoidTopicId: String? = null): String? {
    if (topicIds.isEmpty()) return null
    val seed = mix(date.toEpochDay() * 31L + themeId.hashCode())
    val first = topicIds[Random(seed).nextInt(topicIds.size)]
    if (avoidTopicId == null || first != avoidTopicId || topicIds.size == 1) return first
    val others = topicIds.filter { it != avoidTopicId }
    return others[Random(mix(seed xor REDRAW_SALT)).nextInt(others.size)]
}
```

---

# 4. Handover Checklist
- [x] Project specifications and user journey defined.
- [x] PlantUML and Mermaid architectural diagrams updated.
- [x] Core domain rules and pseudocode documented.
- [x] All 495 unit tests passing successfully.
