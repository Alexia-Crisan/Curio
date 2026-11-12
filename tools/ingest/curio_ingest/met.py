from urllib.parse import urlencode

from .records import clean, first_text, make_record, usable_artist

BASE = "https://collectionapi.metmuseum.org/public/collection"
SEARCH = BASE + "/v1.1/search"
MUSEUM = "Metropolitan Museum of Art"
MIN_INTERVAL = 0.25


def search_url(query, offset=0, limit=100, highlights=True, department_id=None):
    params = [("q", query), ("hasImages", "true"), ("offset", int(offset)), ("limit", int(limit))]
    if highlights:
        params.append(("isHighlight", "true"))
    if department_id is not None:
        params.append(("departmentId", int(department_id)))
    return SEARCH + "?" + urlencode(params)


def object_url(object_id):
    return BASE + "/v1/objects/" + str(int(object_id))


def to_record(obj):
    if not isinstance(obj, dict) or not obj.get("isPublicDomain"):
        return None
    image = clean(obj.get("primaryImage"))
    title = clean(obj.get("title"))
    object_id = obj.get("objectID")
    page = clean(obj.get("objectURL"))
    if not image or not title or object_id is None or not page.startswith("https://"):
        return None
    return make_record(
        source="met",
        source_id=str(object_id),
        museum=MUSEUM,
        page_url=page,
        title=title,
        artist=usable_artist(obj.get("artistDisplayName")),
        date=obj.get("objectDate"),
        origin=first_text(obj.get("country"), obj.get("culture"), obj.get("region")),
        medium=obj.get("medium"),
        dimensions=obj.get("dimensions"),
        credit_line=obj.get("creditLine"),
        classification=first_text(obj.get("classification"), obj.get("objectName")),
        image_url=image,
        alt_hint="",
        license="CC0 1.0 (Met Open Access); public-domain work",
    )


def fetch_record(client, object_id):
    return to_record(client.get_json(object_url(object_id)))


def discover(client, query, limit=40, highlights=True, department_id=None):
    payload = client.get_json(search_url(query, 0, min(max(limit, 1), 500), highlights, department_id))
    ids = payload.get("objectIDs") or []
    records = []
    skipped = 0
    for object_id in ids:
        if len(records) >= limit:
            break
        record = fetch_record(client, object_id)
        if record is None:
            skipped += 1
        else:
            records.append(record)
    return records, skipped
