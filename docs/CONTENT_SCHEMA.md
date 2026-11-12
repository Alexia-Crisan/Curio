# Curio content schema (version 2)

One JSON file per theme in `app/src/main/assets/content/`. The file name is the theme id plus `.json` (`space.json` defines theme `space`). Every file is read at start-up by `ContentLoader`. Anything that breaks a rule below is reported in Logcat (tag `CurioContent`) and by the unit test `ContentAssetsTest`.

## Shape

```json
{
  "theme": {
    "id": "space",
    "name": "Space",
    "tagline": "Look up.",
    "available": true,
    "accentColor": "0xFF6366F1"
  },
  "subcategories": [
    { "id": "mars", "name": "Mars", "guidedOrder": 1, "accentColor": "0xFFEA580C" }
  ],
  "discoveries": [
    {
      "id": "space.mars.fact.01",
      "subcategoryId": "mars",
      "type": "FACT",
      "title": "A day on Mars is just 37 minutes longer than on Earth.",
      "body": "One Martian day, called a sol, lasts about 24 hours and 39 minutes.",
      "source": { "name": "NASA", "url": "https://science.nasa.gov/mars/", "license": "Public domain" }
    }
  ]
}
```

## Fields

| Where | Field | Required | Rule |
|---|---|---|---|
| theme | `id` | yes | letters, digits, `.`, `_`, `-`; same as the file name without `.json` |
| theme | `name` | yes | shown to the user |
| theme | `tagline` | no | read but not shown yet |
| theme | `available` | no, default `true` | `false` marks a draft or a "coming soon" theme: its problems do not fail the tests and the app shows it disabled |
| theme | `accentColor` | no | `0xAARRGGBB`, `#RRGGBB` or `#AARRGGBB` |
| subcategory (a topic) | `id`, `name` | yes | the id is what discoveries point to |
| subcategory | `guidedOrder` | no | topics are ordered by this number, then by position in the file; Guided mode walks the topics in that order |
| subcategory | `accentColor` | no | colour of its cards; falls back to the theme colour, then to blue |
| discovery | `id` | yes | unique across all files; letters, digits, `.`, `_`, `-` |
| discovery | `subcategoryId` | yes | must exist in `subcategories` |
| discovery | `type` | yes | `FACT`, `ARTICLE`, `QUIZ`, `IMAGE`, `VIDEO`, `SOURCE` or `KNOWLEDGE` (capitals) |
| discovery | `title` | yes | not empty |
| discovery | `body` | `ARTICLE` and `KNOWLEDGE`: yes; others: optional | plain text |
| discovery | `quiz` | `QUIZ`: yes | `question`, `options` (at least 2, all different, none empty), `correctIndex` (0-based, inside the options), `explanation` |
| discovery | `media` | `IMAGE` and `VIDEO`: yes | a relative path inside `app/src/main/assets/content/media/`, for example `mars/dust.jpg`; no `https://`, no `/` at the start, no `..` |
| discovery | `mediaAlt` | `IMAGE` and `VIDEO`: should | alt text for TalkBack; a missing one is a warning |
| discovery | `source` | yes | `name`, `license` and an `https://` `url`; optional `attribution` |

## Rules of a topic

A topic is playable only with **exactly 6 valid discoveries** and **at least one `KNOWLEDGE` card** (the "Pe scurt" card that becomes a leaf of the tree). A topic that breaks this is left out of the app together with its cards, and the loader reports why. The order of the six cards in the feed is the order in the file.

A theme is available when it does not declare `"available": false` and has at least one playable topic.

## Offline first

Images and videos are packaged in the app. Remote media addresses are rejected. Keep files small (images under 300 KB, videos short and compressed), and keep the licence and author of every media file in `assets/content/SOURCES.md`.

## Checking your work

```
./gradlew :app:testDebugUnitTest --tests "*ContentAssetsTest"
```
The test prints every problem as `ERROR | file | where | message` or `WARNING | ...`. When the first theme is complete, set `"available": true` in its file and remove the `@Ignore` line from `atLeastOneThemeIsAvailable` in `ContentAssetsTest`.

## Release and debug behaviour

A debug build with no playable theme falls back to built-in test fixtures so development is not blocked. A release build never does: it shows only what is in `assets/content`.
