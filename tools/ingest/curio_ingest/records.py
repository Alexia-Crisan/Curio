import re

RECORD_FIELDS = (
    "source", "source_id", "museum", "page_url", "title", "artist", "date",
    "origin", "medium", "dimensions", "credit_line", "classification",
    "image_url", "alt_hint", "license", "retrieved_at",
)

UNKNOWN_ARTISTS = {"", "unknown", "unidentified", "unidentified artist", "anonymous", "unknown artist", "unknown maker"}


def clean(value):
    if value is None:
        return ""
    return re.sub(r"\s+", " ", str(value)).strip()


def usable_artist(name):
    lowered = clean(name).lower()
    if lowered in UNKNOWN_ARTISTS:
        return ""
    if lowered.startswith("unknown") or lowered.startswith("unidentified"):
        return ""
    return clean(name)


def first_text(*values):
    for value in values:
        text = clean(value)
        if text:
            return text
    return ""


def make_record(**fields):
    record = {name: "" for name in RECORD_FIELDS}
    for key, value in fields.items():
        if key not in record:
            raise KeyError(key)
        record[key] = clean(value)
    return record
