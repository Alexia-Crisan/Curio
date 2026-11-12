# Curio ingestion tool

Turns open data from museum and science APIs into **candidate topics** that a human reviews, then promotes into the app's content files. It runs on a developer's computer. The app never calls these APIs.

```
fetch   ->   records/        normalised facts, one JSON per artwork (gitignored)
build   ->   candidates/     one topic of 6 cards per record, status "candidate"
images  ->   images/         the original image of each candidate (gitignored), a human shrinks it
review  ->   a human edits the candidate, sets status "approved"
promote ->   app/src/main/assets/content/<theme>.json
```

Nothing in this tool writes into `app/src/main/assets/content` except `promote --write`.

## Setup

Python 3.9 or newer, standard library only. No `pip install`.

```
export CURIO_INGEST_CONTACT="your.email@example.com"
```

The address goes into the `User-Agent` of every request (the Art Institute of Chicago asks for it in `AIC-User-Agent` too). The tool refuses to fetch without it.

## Commands (run from `tools/ingest`)

```
python3 -m curio_ingest fetch --source artic --query "impressionism" --limit 20
python3 -m curio_ingest fetch --source met --query "sunflowers" --limit 20
python3 -m curio_ingest build --template templates/art.json --out candidates/art
python3 -m curio_ingest images --candidates candidates/art
python3 -m curio_ingest promote --theme art --candidates candidates/art
python3 -m curio_ingest promote --theme art --candidates candidates/art --write
python3 -m unittest discover -s tests -t .
```

- `fetch` keeps only works that the museum marks as public domain, that have an image and an exact page address. Met searches ask for highlights unless you pass `--all-works`. Responses are cached in `.cache/`, so repeating a command costs no request. Requests are spaced (Art Institute at least 1 s, Met 0.25 s) and retried with back-off on 429 and 5xx.
- `build` needs enough different records in `records/` to write the quiz (four different artists, or four different dates). It never overwrites a candidate that already exists, so a human's edits are safe; use `--overwrite` to rebuild.
- `images` downloads each candidate's image and warns when it is larger than 300 KB.
- `promote` is a dry run until you add `--write`. It only touches candidates with `"status": "approved"`.

## What a human does between `build` and `promote`

For every candidate you intend to keep (checklist from `CURIO_CONTENT_PLAN.md`, section 8):

1. Open the museum page in `provenance.pageUrl` and confirm each statement on the cards is on that page.
2. Read the six cards: title and body agree, the quiz answer is right and the wrong options are wrong, the wording is clear to a 15-year-old. Fix the text in the candidate file if it is not.
3. Shrink the image under 300 KB (any image tool), save it as `app/src/main/assets/content/media/<media path of the IMAGE card>`, for example `media/art/met-436535.jpg`.
4. Write `mediaAlt` for the IMAGE card yourself (`review.altHint` shows the museum's own description when it has one).
5. Empty `review.warnings` once the warnings are dealt with, put your name in `review.reviewer`, and set `"status": "approved"`.
6. Run `promote` (dry run), read the list, then `promote --write`, then `./gradlew :app:testDebugUnitTest --tests "*ContentAssetsTest"`.
7. Add the image's licence line to `app/src/main/assets/content/SOURCES.md`.
8. The theme stays `"available": false` until a human sets it to `true`.

## Files

| Path | Who owns it |
|---|---|
| `curio_ingest/` | code |
| `templates/art.json` | a human: the wording of the cards. Placeholders are `{title}`, `{artist}`, `{date}`, `{origin}`, `{medium}`, `{dimensions}`, `{credit_line}`, `{classification}`, `{museum}`, `{theme}`, `{topic_id}`. A variant is used only when all fields in its `requires` are present |
| `themes/art.json` | a human: theme name, tagline, colour |
| `curio_ingest/licenses.py` | a human: the licences the tool accepts (`CC0 1.0`, `Public domain`) |
| `candidates/<theme>/` | reviewed and committed |
| `records/`, `images/`, `.cache/` | local, gitignored |

## Why the text is templated

The cards state facts that the museum's own record says (title, maker, date, materials, size, place, credit line). The wording comes from the template, which a human wrote, never from a model. A card that needs a field the record does not have uses the next variant of the template, or the record is skipped with the reason printed.

## Limits

- Tested only against recorded-shape fixtures in `tests/fixtures`, not against the live APIs. After the first real fetch, open one file in `records/` and check that `artist`, `date`, `medium` and `dimensions` are filled; an empty field means the API field name differs from the one the code reads.
- The Art Institute's image address uses the `iiif_url` of the API response with a 843-pixel width, the size its documentation recommends.
- Public-domain status is the museum's statement. Open the page before trusting it for a card.
