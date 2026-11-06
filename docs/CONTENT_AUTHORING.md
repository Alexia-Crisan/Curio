# Curio Content Authoring Guide

This document is a practical guide for content authors creating and editing educational themes, topics, and discoveries for Curio.

---

## 1. What Content Is and Where It Lives

Content in Curio is stored offline inside bundled JSON files under `app/src/main/assets/content/`.
Each theme has its own JSON file named after its `theme_id`:
`app/src/main/assets/content/<theme_id>.json` (for example, `space.json`).

Media files (images/graphics) reside in `app/src/main/assets/media/`.

---

## 2. The Schema, Field by Field

### Theme Object (`theme`)
| Field | Type | Required? | Rule / Description | Used By |
|---|---|---|---|---|
| `id` | String | Required | Lowercase ID (e.g. `"space"`, `"animals"`). | `ContentIndex`, routes |
| `name` | String | Required | Romanian title shown on theme cards. | Theme picker, `ThemeScreen` |
| `tagline` | String | Required | Short subtitle/tagline. | Theme picker |
| `accentColor` | String | Required | Hex color string (`"0xFF6366F1"`). | Theme cards, accent bar |
| `available` | Boolean | Optional | Defaults to `true`. If `false`, theme is hidden/coming soon. | Theme picker |

### Subcategory / Topic Object (`subcategories`)
| Field | Type | Required? | Rule / Description | Used By |
|---|---|---|---|---|
| `id` | String | Required | Lowercase topic ID (e.g. `"mars"`). | `TopicProgress`, `DayChoice` |
| `name` | String | Required | Romanian topic name. | Feed, Progress calendar |
| `guidedOrder` | Int | Required | 1-based order index for Guided mode. | `TopicSelector.guided` |
| `accentColor` | String | Required | Hex color string. | Topic card |

### Discovery Object (`discoveries`)
| Field | Type | Required? | Rule / Description | Used By |
|---|---|---|---|---|
| `id` | String | Required | Format: `<theme>.<topic>.<type>.<num>`. | `ViewEvent`, `SavedDiscovery` |
| `subcategoryId` | String | Required | Must match a topic ID in `subcategories`. | `ContentIndex.discoveriesOf` |
| `type` | Enum String | Required | `FACT`, `KNOWLEDGE`, `QUIZ`, `ARTICLE`, `VIDEO`, `IMAGE`. | Card layout, badge rules |
| `title` | String | Required | Headline in Romanian (max 200 chars). | Feed card, Detail screen |
| `body` | String | Optional | Detail text in Romanian (max 1000 chars; required for `KNOWLEDGE`). | Detail screen |
| `mediaUrl` | String | Optional | Asset path (e.g. `media/mars_sunset.jpg`) or URL. | Feed card |
| `altText` | String | Optional | Accessibility text in Romanian (required if `mediaUrl` set). | TalkBack |
| `quiz` | Object | Optional | Required for `QUIZ` type cards. | Quiz feed card |
| `source` | Object | Required | Source attribution object. | Detail screen, Sources screen |

#### `DiscoveryType` in the Feed
- `FACT`: Quick, single-card trivia fact.
- `KNOWLEDGE`: Deep-dive explanation card ("Pe scurt"). Serves as the topic's landmark in the knowledge tree, and reading it for 8s in Detail contributes to the "Cititor atent" badge.
- `QUIZ`: Interactive question with 4 options and an explanation.

### Source Object (`source`)
| Field | Type | Required? | Rule / Description |
|---|---|---|---|
| `name` | String | Required | Source or publisher name (e.g., `"NASA"`). |
| `url` | String | Required | Direct URL starting with `https://`. |
| `license` | String | Required | Licence type (e.g., `"Public domain"`, `"CC BY-SA 4.0"`). |
| `attribution` | String | Optional | Custom attribution string if required by licence. |

---

## 3. Rules Enforced by `DatasetValidationTest`

The unit test `DatasetValidationTest` enforces the following rules on the dataset:
- Every topic with 6 cards MUST contain at least 1 `KNOWLEDGE` card (landmark for the knowledge tree).
- Every discovery ID must be unique across all files.
- Titles must not exceed 200 characters; bodies must not exceed 1000 characters.
- Source URLs must start with `https://`.
- Every discovery with a `mediaUrl` must provide a non-empty `altText`.

*Note:* `availableThemesAreComplete` is disabled during active content authoring and enabled at the final content freeze. `tools/content_report.py` checks these rules instantly without Gradle.

---

## 4. How to Add Content (Step-by-Step)

### Template Skeleton (Do not copy as real content)
```json
{
  "id": "...",
  "subcategoryId": "...",
  "type": "FACT",
  "title": "...",
  "body": "...",
  "source": {
    "name": "...",
    "url": "https://...",
    "license": "..."
  }
}
```

### Steps to Add a New Discovery:
1. Open `app/src/main/assets/content/<theme_id>.json`.
2. Locate the `discoveries` list.
3. Add a new discovery entry following the schema.
4. Ensure the topic has exactly 6 discoveries, including 1 `KNOWLEDGE` card.
5. Run `python tools/content_report.py` to verify formatting.

---

## 5. Text Writing Rules

- **Language**: All visible text MUST be in Romanian with proper diacritics (`ș`, `ț`, `ă`, `â`, `î`, never `ş` or `ţ`).
- **Clarity**: One clear idea per card. The title must stand on its own.
- **Accuracy**: No medical, legal, or financial claims. Every fact must be verifiable via the source link.
- **Tone**: Neutral, informative, and engaging.

---

## 6. Sources and Licences Checklist

- Prefer public-domain material (NASA, ESA, government agencies) or Creative Commons (`CC BY-SA 4.0`, `CC BY 4.0`).
- Record exact source name, licence, and `https://` URL.
- Never use uncredited images from search engines or social media.

---

## 7. Images

- **Format & Size**: Recommended `.jpg` or `.webp`, maximum 1080 px wide, file size under 400 KB.
- **Location**: Store in `app/src/main/assets/media/`.
- **Accessibility**: Provide descriptive `altText` in Romanian describing what the image shows.

---

## 8. How to Check Your Work

1. Run the Python report script:
   ```bash
   python tools/content_report.py
   ```
2. Run unit tests:
   ```bash
   ./gradlew :app:testDebugUnitTest --tests "*DatasetValidationTest"
   ```
3. Inspect the updated content in the Android app feed.

---

## 9. Git Workflow for Content

- Create a topic branch: `git checkout -b a/content-<theme>`
- Commit only JSON changes and media files.
- Review changes before merging to `main`.
