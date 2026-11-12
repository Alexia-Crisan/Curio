import hashlib
import json
import re
from pathlib import Path

from .licenses import is_allowed
from .records import RECORD_FIELDS, clean

PLACEHOLDER = re.compile(r"\{([a-z_]+)\}")
CONTEXT_FIELDS = ("theme", "topic_id")
BODY_LIMIT = 220
TITLE_LIMIT = 140
NAME_LIMIT = 80
CARD_TYPES = ("FACT", "ARTICLE", "QUIZ", "IMAGE", "VIDEO", "SOURCE", "KNOWLEDGE")


class TemplateError(Exception):
    pass


def digest(text):
    return int(hashlib.sha256(text.encode("utf-8")).hexdigest(), 16)


def placeholders(text):
    return PLACEHOLDER.findall(text or "")


def render(text, context):
    return PLACEHOLDER.sub(lambda match: context[match.group(1)], text or "")


def variant_texts(variant):
    texts = [variant.get("title", ""), variant.get("body", ""), variant.get("media", "")]
    quiz = variant.get("quiz")
    if quiz:
        texts.extend([quiz.get("question", ""), quiz.get("explanation", "")])
    return texts


def load_template(path):
    template = json.loads(Path(path).read_text(encoding="utf-8"))
    validate_template(template)
    return template


def validate_template(template):
    for key in ("theme", "cards"):
        if key not in template:
            raise TemplateError("Template misses " + key)
    ids = set()
    for card in template["cards"]:
        card_id = card.get("id", "")
        if not re.fullmatch(r"[a-z0-9._-]+", card_id):
            raise TemplateError("Bad card id: " + repr(card_id))
        if card_id in ids:
            raise TemplateError("Duplicate card id: " + card_id)
        ids.add(card_id)
        if card.get("type") not in CARD_TYPES:
            raise TemplateError("Bad type in " + card_id)
        if not card.get("variants"):
            raise TemplateError("No variants in " + card_id)
        for variant in card["variants"]:
            requires = variant.get("requires", [])
            for name in requires:
                if name not in RECORD_FIELDS:
                    raise TemplateError("Unknown field " + name + " in " + card_id)
            for text in variant_texts(variant):
                for name in placeholders(text):
                    if name not in CONTEXT_FIELDS and name not in requires:
                        raise TemplateError("Placeholder " + name + " not in requires of " + card_id)
            if card["type"] == "QUIZ":
                quiz = variant.get("quiz")
                if not quiz or quiz.get("answerField") not in RECORD_FIELDS or "question" not in quiz or "explanation" not in quiz:
                    raise TemplateError("Bad quiz in " + card_id)
                if quiz.get("answerField") not in requires:
                    raise TemplateError("Quiz answerField must be required in " + card_id)
                if int(quiz.get("options", 4)) < 2:
                    raise TemplateError("Quiz needs at least 2 options in " + card_id)
            if card["type"] in ("ARTICLE", "KNOWLEDGE") and not variant.get("body"):
                raise TemplateError("Body required in " + card_id)
            if card["type"] in ("IMAGE", "VIDEO") and not variant.get("media"):
                raise TemplateError("Media required in " + card_id)
    if not any(card["type"] == "KNOWLEDGE" for card in template["cards"]):
        raise TemplateError("Template needs a KNOWLEDGE card")
    if len(template["cards"]) != 6:
        raise TemplateError("Template needs exactly 6 cards")


def topic_id_of(record):
    return (record["source"] + "-" + record["source_id"]).lower()


def make_quiz(variant, record, pool):
    quiz = variant["quiz"]
    field = quiz["answerField"]
    count = int(quiz.get("options", 4))
    answer = clean(record[field])
    seen = {answer.lower()}
    candidates = []
    for other in pool:
        value = clean(other.get(field, ""))
        if value and value.lower() not in seen:
            seen.add(value.lower())
            candidates.append(value)
    if len(candidates) < count - 1:
        return None
    seed = record["source"] + ":" + record["source_id"]
    candidates.sort(key=lambda value: digest(seed + "|" + value))
    wrong = candidates[: count - 1]
    position = digest(seed + "|position") % count
    options = list(wrong)
    options.insert(position, answer)
    context = context_of(record)
    return {
        "question": render(quiz["question"], context),
        "options": options,
        "correctIndex": position,
        "explanation": render(quiz["explanation"], context),
    }


def context_of(record):
    context = dict(record)
    context["theme"] = ""
    context["topic_id"] = ""
    return context


def attribution_of(record):
    parts = ["“" + record["title"] + "”"]
    if record["artist"]:
        parts.append(record["artist"])
    parts.append(record["museum"])
    return ", ".join(parts)


def source_of(record):
    return {
        "name": record["museum"] + ", collection online",
        "url": record["page_url"],
        "license": record["license"],
        "attribution": attribution_of(record),
    }


def build_card(template, card, record, pool, theme, topic_id):
    for variant in card["variants"]:
        if not all(record.get(name) for name in variant.get("requires", [])):
            continue
        context = context_of(record)
        context["theme"] = theme
        context["topic_id"] = topic_id
        entry = {
            "id": theme + "." + topic_id + "." + card["id"],
            "type": card["type"],
            "title": render(variant.get("title", ""), context),
            "source": source_of(record),
        }
        if variant.get("body"):
            entry["body"] = render(variant["body"], context)
        if card["type"] == "QUIZ":
            quiz = make_quiz(variant, record, pool)
            if quiz is None:
                continue
            entry["quiz"] = quiz
        if variant.get("media"):
            entry["media"] = render(variant["media"], context)
            entry["mediaAlt"] = ""
        return entry
    return None


def warnings_of(topic_name, discoveries):
    found = []
    if len(topic_name) > NAME_LIMIT:
        found.append("topic name longer than " + str(NAME_LIMIT) + " characters")
    for entry in discoveries:
        if len(entry["title"]) > TITLE_LIMIT:
            found.append(entry["id"] + ": title longer than " + str(TITLE_LIMIT) + " characters")
        if len(entry.get("body", "")) > BODY_LIMIT:
            found.append(entry["id"] + ": body longer than " + str(BODY_LIMIT) + " characters")
    return found


def build_topic(template, record, pool, retrieved_default=""):
    theme = template["theme"]
    topic_id = topic_id_of(record)
    if not is_allowed(record.get("license", "")):
        return None, "licence not on the allow-list: " + repr(record.get("license", ""))
    if not record.get("page_url", "").startswith("https://"):
        return None, "page url is not https"
    discoveries = []
    for card in template["cards"]:
        entry = build_card(template, card, record, pool, theme, topic_id)
        if entry is None:
            return None, "no usable variant for card " + card["id"]
        discoveries.append(entry)
    name = record["title"]
    candidate = {
        "status": "candidate",
        "theme": theme,
        "topic": {"id": topic_id, "name": name},
        "discoveries": discoveries,
        "provenance": {
            "sourceApi": record["source"],
            "sourceId": record["source_id"],
            "pageUrl": record["page_url"],
            "license": record["license"],
            "creditLine": record["credit_line"],
            "imageUrl": record["image_url"],
            "retrievedAt": record.get("retrieved_at") or retrieved_default,
        },
        "review": {
            "reviewer": "",
            "altHint": record["alt_hint"],
            "warnings": warnings_of(name, discoveries),
        },
    }
    return candidate, ""


def build_all(template, records):
    ordered = sorted(records, key=lambda record: (record["source"], record["source_id"]))
    candidates = []
    skipped = []
    for record in ordered:
        candidate, reason = build_topic(template, record, ordered)
        if candidate is None:
            skipped.append((topic_id_of(record), reason))
        else:
            candidates.append(candidate)
    return candidates, skipped


def write_candidates(candidates, out_dir, overwrite=False):
    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)
    written = []
    kept = []
    for candidate in candidates:
        path = out / (candidate["topic"]["id"] + ".json")
        if path.exists() and not overwrite:
            kept.append(path.name)
            continue
        path.write_text(json.dumps(candidate, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        written.append(path.name)
    return written, kept
