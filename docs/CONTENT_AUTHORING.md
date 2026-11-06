# Curio Content Authoring Guide

This guide defines the structure, rules, and tooling for creating educational content in Curio.

---

## 1. Content File Location & Naming

Bundled content files are stored in `app/src/main/assets/content/`. Each theme has a dedicated JSON file named after its `theme_id`:
`app/src/main/assets/content/<theme_id>.json` (for example, `space.json`).

---

## 2. JSON Structure

Each content file must contain three top-level fields:

```json
{
  "theme": {
    "id": "space",
    "name": "Space",
    "tagline": "Look up.",
    "accentColor": "0xFF6366F1"
  },
  "subcategories": [
    {
      "id": "mars",
      "name": "Mars",
      "guidedOrder": 1,
      "accentColor": "0xFFEA580C"
    }
  ],
  "discoveries": [
    {
      "id": "space.mars.fact.01",
      "subcategoryId": "mars",
      "type": "FACT",
      "title": "A day on Mars is just 37 minutes longer than on Earth.",
      "body": "One Martian day, called a sol, lasts about 24 hours and 39 minutes.",
      "source": {
        "name": "NASA",
        "url": "https://science.nasa.gov/mars/",
        "license": "Public domain"
      }
    }
  ]
}
```

---

## 3. Field Definitions

### Theme (`theme`)
- `id` (String, required): Unique identifier for the theme (e.g., `"space"`, `"animals"`).
- `name` (String, required): Display title in Romanian.
- `tagline` (String, required): Short subtitle for theme cards.
- `accentColor` (String, required): Hex color string starting with `0xFF` (e.g., `"0xFF6366F1"`).

### Topics / Subcategories (`subcategories`)
- `id` (String, required): Topic identifier (e.g., `"mars"`, `"ocean"`).
- `name` (String, required): Display title in Romanian.
- `guidedOrder` (Int, required): Sequence order for Guided mode.
- `accentColor` (String, required): Topic accent color string.

### Discoveries (`discoveries`)
- `id` (String, required): Format `<theme_id>.<topic_id>.<type_lowercase>.<num>`.
- `subcategoryId` (String, required): Matches a topic `id`.
- `type` (String, required): `FACT`, `KNOWLEDGE`, `QUIZ`, `ARTICLE`, `VIDEO`, `IMAGE`.
- `title` (String, required): Headline/title in Romanian.
- `body` (String, optional): Explanation text (required for `KNOWLEDGE` cards).
- `mediaUrl` (String, optional): Image or video asset URL/path.
- `quiz` (Object, required for `QUIZ` type):
  - `question`: Romanian string.
  - `options`: List of 4 option strings.
  - `correctIndex`: 0-based integer.
  - `explanation`: Detailed explanation string.
- `source` (Object, required):
  - `name`: Publisher/source name (e.g., `"NASA"`).
  - `url`: Direct source link.
  - `license`: Licence type (e.g., `"CC BY-SA 4.0"`, `"Public domain"`).
  - `attribution`: Optional attribution string.

---

## 4. Content Rules & Requirements

1. **6 Discoveries per Topic**: Each topic must have exactly 6 discoveries.
2. **At least 1 `KNOWLEDGE` ("Pe scurt") card per Topic**: Required for the "Cititor atent" badge rule.
3. **Valid Sources**: Every discovery must have a source with `name`, `url`, and `license`.
4. **Diacritics & Romanian Text**: All user-visible text must be in Romanian with proper comma-below diacritics (`ș`, `ț`, `ă`, `â`, `î`).
5. **No Speculative Licences**: Always verify public domain or Creative Commons compliance for images/text.

---

## 5. Tooling & Validation

To check content completeness, run:

```bash
python tools/content_report.py
```

The script checks:
- Topic completion (6/6 discoveries).
- Presence of required `KNOWLEDGE` cards.
- Validity of source references and URLs.
