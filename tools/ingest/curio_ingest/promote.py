import json
import os
import re
from pathlib import Path

from .licenses import is_allowed

ID_PATTERN = re.compile(r"[A-Za-z0-9._-]+")
MAX_IMAGE_BYTES = 300 * 1024
TYPES_WITH_MEDIA = ("IMAGE", "VIDEO")


def check_candidate(candidate, media_root, allow_warnings=False):
    problems = []
    discoveries = candidate.get("discoveries") or []
    if len(discoveries) != 6:
        problems.append("needs exactly 6 discoveries, has " + str(len(discoveries)))
    if not any(entry.get("type") == "KNOWLEDGE" for entry in discoveries):
        problems.append("needs a KNOWLEDGE card")
    review = candidate.get("review") or {}
    if not (review.get("reviewer") or "").strip():
        problems.append("review.reviewer is empty")
    if review.get("warnings") and not allow_warnings:
        problems.append("review.warnings is not empty: " + "; ".join(review["warnings"]))
    topic = candidate.get("topic") or {}
    if not ID_PATTERN.fullmatch(topic.get("id", "")) or not (topic.get("name") or "").strip():
        problems.append("topic id or name is invalid")
    seen = set()
    for entry in discoveries:
        problems.extend(check_discovery(entry, media_root, seen))
    return problems


def check_discovery(entry, media_root, seen):
    label = entry.get("id", "?")
    problems = []
    if not ID_PATTERN.fullmatch(entry.get("id", "")):
        problems.append(label + ": invalid id")
    if entry.get("id") in seen:
        problems.append(label + ": duplicate id")
    seen.add(entry.get("id"))
    if not (entry.get("title") or "").strip():
        problems.append(label + ": empty title")
    kind = entry.get("type")
    if kind in ("ARTICLE", "KNOWLEDGE") and not (entry.get("body") or "").strip():
        problems.append(label + ": body required")
    source = entry.get("source") or {}
    if not (source.get("url") or "").startswith("https://"):
        problems.append(label + ": source url must be https")
    if not (source.get("name") or "").strip():
        problems.append(label + ": source name is empty")
    if not is_allowed(source.get("license", "")):
        problems.append(label + ": licence not on the allow-list")
    if kind == "QUIZ":
        problems.extend(check_quiz(label, entry.get("quiz") or {}))
    if kind in TYPES_WITH_MEDIA:
        problems.extend(check_media(label, entry, media_root))
    return problems


def check_quiz(label, quiz):
    options = quiz.get("options") or []
    problems = []
    if len(options) < 2 or len(set(options)) != len(options) or any(not str(option).strip() for option in options):
        problems.append(label + ": quiz options must be at least 2, different, not empty")
    index = quiz.get("correctIndex")
    if not isinstance(index, int) or isinstance(index, bool) or not 0 <= index < len(options):
        problems.append(label + ": correctIndex is outside the options")
    if not (quiz.get("question") or "").strip() or not (quiz.get("explanation") or "").strip():
        problems.append(label + ": quiz question and explanation are required")
    return problems


def check_media(label, entry, media_root):
    media = entry.get("media") or ""
    problems = []
    if not media or media.startswith("/") or ".." in media.split("/") or "://" in media:
        problems.append(label + ": media must be a relative path inside the media folder")
        return problems
    if not (entry.get("mediaAlt") or "").strip():
        problems.append(label + ": mediaAlt is empty (a human writes it)")
    path = Path(media_root) / media
    if not path.is_file():
        problems.append(label + ": media file not found at " + str(path))
    elif path.stat().st_size > MAX_IMAGE_BYTES:
        problems.append(label + ": media file is larger than 300 KB")
    return problems


def app_discovery(entry, subcategory_id):
    out = {"id": entry["id"], "subcategoryId": subcategory_id, "type": entry["type"], "title": entry["title"]}
    if entry.get("body"):
        out["body"] = entry["body"]
    if entry.get("quiz"):
        out["quiz"] = entry["quiz"]
    if entry.get("media"):
        out["media"] = entry["media"]
        out["mediaAlt"] = entry["mediaAlt"]
    out["source"] = entry["source"]
    return out


def load_target(target, theme_meta_path, theme_id):
    path = Path(target)
    if path.is_file():
        data = json.loads(path.read_text(encoding="utf-8"))
    else:
        meta = json.loads(Path(theme_meta_path).read_text(encoding="utf-8"))
        data = {"theme": meta["theme"], "subcategories": [], "discoveries": []}
    if data["theme"]["id"] != theme_id:
        raise ValueError("Target theme id " + data["theme"]["id"] + " does not match " + theme_id)
    data.setdefault("subcategories", [])
    data.setdefault("discoveries", [])
    return data


def merge(data, candidate):
    topic = candidate["topic"]
    existing = {sub["id"] for sub in data["subcategories"]}
    if topic["id"] in existing:
        return False
    order = max([sub.get("guidedOrder", 0) for sub in data["subcategories"]] or [0]) + 1
    data["subcategories"].append({"id": topic["id"], "name": topic["name"], "guidedOrder": order})
    for entry in candidate["discoveries"]:
        data["discoveries"].append(app_discovery(entry, topic["id"]))
    return True


def promote(candidates_dir, target, theme_meta_path, media_root, write=False, allow_warnings=False):
    files = sorted(Path(candidates_dir).glob("*.json"))
    approved = []
    report = {"promoted": [], "already": [], "refused": [], "not_approved": []}
    for path in files:
        candidate = json.loads(path.read_text(encoding="utf-8"))
        if candidate.get("status") != "approved":
            report["not_approved"].append(path.name)
            continue
        approved.append((path, candidate))
    if not approved:
        return report, None
    theme_id = approved[0][1]["theme"]
    data = load_target(target, theme_meta_path, theme_id)
    for path, candidate in approved:
        if candidate["theme"] != theme_id:
            report["refused"].append((path.name, ["theme differs from the first candidate"]))
            continue
        problems = check_candidate(candidate, media_root, allow_warnings)
        if problems:
            report["refused"].append((path.name, problems))
            continue
        if merge(data, candidate):
            report["promoted"].append(candidate["topic"]["id"])
        else:
            report["already"].append(candidate["topic"]["id"])
    if write and report["promoted"]:
        write_target(target, data)
    return report, data


def write_target(target, data):
    path = Path(target)
    path.parent.mkdir(parents=True, exist_ok=True)
    temp = path.with_name(path.name + ".tmp")
    temp.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    os.replace(temp, path)
