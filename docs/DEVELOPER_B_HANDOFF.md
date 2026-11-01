# Developer B Handoff

Developer A guarantees all state persistence, domain rules, ViewModels, and data models. Developer B owns UI components, widgets, sensors, and screen layouts.

| # | Item | Where | What B must do | What A guarantees | Real signature |
|---|---|---|---|---|---|
| 1 | `BadgeCard` | `ui/components/BadgeCard.kt` | Final visuals for locked, in-progress and unlocked states | `AchievementUi`, `AchievementVisuals` | `fun BadgeCard(badge: AchievementUi, onClick: () -> Unit, modifier: Modifier)` |
| 2 | `UnlockBanner` | `ui/components/UnlockBanner.kt` | Final visuals, slide-in, haptic call | Queued unlocks | `fun UnlockBanner(title: String, iconRes: Int, onClick: () -> Unit, modifier: Modifier)` |
| 3 | `TreeCard` | `ui/components/TreeCard.kt` | Progress card visuals and open tree action | `TreeUiState`, `TreeViewModel` | `fun TreeCard(state: TreeUiState, onThemeSelected: (String) -> Unit, onTopicClick: (String) -> Unit, onLeafClick: (LeafNode) -> Unit, onOpenTree: () -> Unit, modifier: Modifier)` |
| 4 | `TreeScreen` | `feature/tree/TreeScreen.kt` | Tree screen branch rows and node sheets | `TreeScreenUi`, `NodeSheet` | `fun TreeScreen(state: TreeScreenUi, onBack: () -> Unit, onThemeSelected: (String) -> Unit, onTopicTap: (String) -> Unit, onLeafTap: (String, LeafNode) -> Unit, onSheetDismiss: () -> Unit, onOpenDetail: (String) -> Unit)` |
| 5 | Growth animation | `TreeScreen`, `TreeCard` | Animate new growth leaves | `TreeGrowth` | n/a |
| 6 | `TeaserLine` | `ui/components/TeaserLine.kt` | Place teaser at feed end | `EndCardTeaser` | `fun TeaserLine(teaser: EndCardTeaser, topicTitleOf: (String) -> String, modifier: Modifier)` |
| 7 | Feed placeholders | `feature/feed/FeedScreen.kt` | Visuals for loading/empty/missing states | `FeedUiState` | `fun FeedScreen(viewModel: FeedViewModel, shakes: Flow<Unit>, shakeEnabled: Boolean, onShakeDetected: () -> Unit)` |
| 8 | Shake sensor | `hardware/ShakeSensor.kt` | Detect shake and call `FeedViewModel.onShake()` | Bonus selection | `class ShakeSensor(context: Context)` |
| 9 | Detail dwell | `feature/detail/DetailViewModel.kt` | Dwell tracking and time logging | `TouchQueue` | `class DetailViewModel(...)` |
| 10 | Detail opened from feed | `feature/feed/FeedScreen.kt` | Call `FeedViewModel.onDetailOpened` | View recording | n/a |
| 11 | Detail opened from library | `feature/library/LibraryScreen.kt` | Navigate to detail | `Route.Detail` | `fun LibraryScreen(...)` |
| 12 | Calendar cells | `feature/progress/ProgressScreen.kt` | Call `onDayClick(date)` | `PastDayPolicy` | `fun ProgressScreen(...)` |
| 13 | Settings extras | `feature/settings/SettingsScreen.kt` | Settings controls and toggles | `SettingsStore` | `fun SettingsScreen(...)` |
| 14 | Onboarding visuals | `feature/onboarding/OnboardingScreen.kt` | Theme and mode picker visuals | `OnboardingViewModel` | `fun OnboardingScreen(...)` |
| 15 | Compose UI tests | `androidTest` | UI tests for achievements and tree | Fakes | n/a |
| 16 | Accessibility | all screens | TalkBack and contrast compliance | Strings and descriptions | n/a |

## Acceptance for B
All core data pipelines, ViewModels, and domain logic are fully tested and operational. UI layout refinement and sensor event routing are ready for integration.
