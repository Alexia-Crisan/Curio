from urllib.parse import urlencode

from .records import clean, first_text, make_record, usable_artist

API = "https://api.artic.edu/api/v1"
DEFAULT_IIIF = "https://www.artic.edu/iiif/2"
MUSEUM = "Art Institute of Chicago"
MIN_INTERVAL = 1.0
IMAGE_SIZE = "843,"


def search_url(query, page=1, limit=50):
    params = [
        ("q", query),
        ("query[term][is_public_domain]", "true"),
        ("limit", int(limit)),
        ("page", int(page)),
    ]
    return API + "/artworks/search?" + urlencode(params)


def artwork_url(artwork_id):
    return API + "/artworks/" + str(int(artwork_id))


def iiif_base(payload):
    config = payload.get("config") if isinstance(payload, dict) else None
    base = clean(config.get("iiif_url")) if isinstance(config, dict) else ""
    return base or DEFAULT_IIIF


def first_line(value):
    for line in str(value or "").splitlines():
        if clean(line):
            return clean(line)
    return ""


def to_record(data, iiif_url=DEFAULT_IIIF):
    if not isinstance(data, dict) or not data.get("is_public_domain"):
        return None
    image_id = clean(data.get("image_id"))
    title = clean(data.get("title"))
    artwork_id = data.get("id")
    if not image_id or not title or artwork_id is None:
        return None
    classes = data.get("classification_titles")
    classification = first_text(*(classes if isinstance(classes, list) else []), data.get("classification_title"))
    thumbnail = data.get("thumbnail")
    alt = clean(thumbnail.get("alt_text")) if isinstance(thumbnail, dict) else ""
    return make_record(
        source="artic",
        source_id=str(artwork_id),
        museum=MUSEUM,
        page_url="https://www.artic.edu/artworks/" + str(artwork_id),
        title=title,
        artist=usable_artist(first_text(data.get("artist_title"), first_line(data.get("artist_display")))),
        date=data.get("date_display"),
        origin=data.get("place_of_origin"),
        medium=data.get("medium_display"),
        dimensions=data.get("dimensions"),
        credit_line=data.get("credit_line"),
        classification=classification,
        image_url=iiif_url.rstrip("/") + "/" + image_id + "/full/" + IMAGE_SIZE + "/0/default.jpg",
        alt_hint=alt,
        license="CC0 1.0 (Art Institute of Chicago data); public-domain artwork",
    )


def fetch_record(client, artwork_id):
    payload = client.get_json(artwork_url(artwork_id))
    data = payload.get("data") if isinstance(payload, dict) else None
    return to_record(data, iiif_base(payload))


def discover(client, query, limit=40, highlights=True, department_id=None):
    payload = client.get_json(search_url(query, 1, min(max(limit, 1), 100)))
    rows = payload.get("data") or []
    records = []
    skipped = 0
    for row in rows:
        if len(records) >= limit:
            break
        artwork_id = row.get("id") if isinstance(row, dict) else None
        if artwork_id is None:
            skipped += 1
            continue
        record = fetch_record(client, artwork_id)
        if record is None:
            skipped += 1
        else:
            records.append(record)
    return records, skipped
