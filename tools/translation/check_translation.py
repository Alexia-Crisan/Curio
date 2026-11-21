import argparse
import json
import re
import sys

TEXT_FIELDS = ("title", "body", "mediaAlt")
NAME_FIELDS = ("name", "tagline")
STOPWORDS = frozenset(
    (
        "the and of is was were with from that which this its than you your has have not but about would their they "
        "oil canvas panel paper ink watercolor watercolour wood stone marble gold silver glass porcelain stoneware glazed "
        "linen silk cotton copper ivory bronze lithograph graphite charcoal pencil "
        "overall framed sheet height width depth diameter "
        "germany france italy netherlands england spain japan greece turkey russia switzerland belgium norway sweden "
        "denmark ireland scotland poland portugal united britain"
    ).split()
)
PROPER_NAMES = ("Art Institute of Chicago", "Metropolitan Museum of Art")
FORBIDDEN_LETTERS = frozenset("şţŞŢãÃ")
NUMBER = re.compile(r"\d+(?:[.,]\d+)*")
QUOTED = re.compile(r"[\u201e\u201c\u00ab][^\u201e\u201c\u201d\u00bb]*[\u201d\u00bb]")
WORD = re.compile(r"[^\W\d_]+")
MAX_TITLE = 200
MAX_BODY = 1000


def numbers(text):
    return [re.sub(r"[.,]", "", m) for m in NUMBER.findall(text)]


def card_texts(card):
    out = []
    for key in TEXT_FIELDS:
        if isinstance(card.get(key), str):
            out.append((key, card[key]))
    quiz = card.get("quiz")
    if isinstance(quiz, dict):
        if isinstance(quiz.get("question"), str):
            out.append(("quiz.question", quiz["question"]))
        for i, option in enumerate(quiz.get("options", [])):
            out.append((f"quiz.options[{i}]", option))
        if isinstance(quiz.get("explanation"), str):
            out.append(("quiz.explanation", quiz["explanation"]))
    return out


def theme_texts(data):
    out = []
    theme = data.get("theme", {})
    for key in NAME_FIELDS:
        if isinstance(theme.get(key), str):
            out.append(("theme", f"theme.{key}", theme[key]))
    for sub in data.get("subcategories", []):
        if isinstance(sub.get("name"), str):
            out.append((sub.get("id", "?"), "subcategory.name", sub["name"]))
    for card in data.get("discoveries", []):
        for where, text in card_texts(card):
            out.append((card.get("id", "?"), where, text))
    return out


def language_problems(data):
    problems = []
    for owner, where, text in theme_texts(data):
        bad = sorted({c for c in text if c in FORBIDDEN_LETTERS})
        if bad:
            problems.append((owner, where, "cedilla or foreign letters " + "".join(bad) + ", use comma letters ș ț"))
        if where != "subcategory.name":
            outside = text
            for name in PROPER_NAMES:
                outside = outside.replace(name, " ")
            outside = QUOTED.sub(" ", outside)
            words = sorted({w.lower() for w in WORD.findall(outside)} & STOPWORDS)
            if words:
                problems.append((owner, where, "looks English, found: " + ", ".join(words)))
        if text != text.strip():
            problems.append((owner, where, "leading or trailing space"))
        if "  " in text:
            problems.append((owner, where, "double space"))
        if not text.strip():
            problems.append((owner, where, "empty text"))
        if where == "title" and len(text) > MAX_TITLE:
            problems.append((owner, where, f"title has {len(text)} characters, maximum {MAX_TITLE}"))
        if where == "body" and len(text) > MAX_BODY:
            problems.append((owner, where, f"body has {len(text)} characters, maximum {MAX_BODY}"))
    return problems


def skeleton(card):
    out = {k: v for k, v in card.items() if k not in TEXT_FIELDS and k != "quiz"}
    quiz = card.get("quiz")
    if isinstance(quiz, dict):
        out["quiz"] = {
            "options": len(quiz.get("options", [])),
            "correctIndex": quiz.get("correctIndex"),
            "other": sorted(k for k in quiz if k not in ("question", "options", "correctIndex", "explanation")),
        }
    out["_text_fields"] = sorted(k for k in card if k in TEXT_FIELDS)
    return out


def parity_problems(english, romanian):
    problems = []
    en_theme = english.get("theme", {})
    ro_theme = romanian.get("theme", {})
    for key in sorted(set(en_theme) | set(ro_theme)):
        if key in NAME_FIELDS:
            if key not in ro_theme:
                problems.append(("theme", f"theme.{key}", "missing"))
            elif ro_theme[key] == en_theme.get(key):
                problems.append(("theme", f"theme.{key}", "not translated"))
        elif en_theme.get(key) != ro_theme.get(key):
            problems.append(("theme", f"theme.{key}", f"changed from {en_theme.get(key)!r} to {ro_theme.get(key)!r}"))
    en_subs = english.get("subcategories", [])
    ro_subs = romanian.get("subcategories", [])
    if [s.get("id") for s in en_subs] != [s.get("id") for s in ro_subs]:
        problems.append(("subcategories", "ids", "ids or their order changed"))
    else:
        for en_sub, ro_sub in zip(en_subs, ro_subs):
            if en_sub.get("name") == ro_sub.get("name"):
                problems.append((en_sub["id"], "subcategory.name", "not translated"))
            en_rest = {k: v for k, v in en_sub.items() if k != "name"}
            ro_rest = {k: v for k, v in ro_sub.items() if k != "name"}
            if en_rest != ro_rest:
                problems.append((en_sub["id"], "subcategory", "fields other than name changed"))
    en_cards = english.get("discoveries", [])
    ro_cards = romanian.get("discoveries", [])
    en_ids = [c.get("id") for c in en_cards]
    ro_ids = [c.get("id") for c in ro_cards]
    for missing in [i for i in en_ids if i not in ro_ids]:
        problems.append((missing, "id", "missing in the Romanian file"))
    for extra in [i for i in ro_ids if i not in en_ids]:
        problems.append((extra, "id", "not in the English file"))
    if sorted(en_ids) == sorted(ro_ids) and en_ids != ro_ids:
        problems.append(("discoveries", "order", "card order changed"))
    by_id = {c.get("id"): c for c in ro_cards}
    for en_card in en_cards:
        ro_card = by_id.get(en_card.get("id"))
        if ro_card is None:
            continue
        owner = en_card["id"]
        if skeleton(en_card) != skeleton(ro_card):
            problems.append((owner, "structure", "type, subcategory, source, quiz answer position or other non-text fields changed"))
        en_texts = dict(card_texts(en_card))
        ro_texts = dict(card_texts(ro_card))
        for where, en_text in en_texts.items():
            if where.startswith("quiz.options"):
                continue
            if ro_texts.get(where) == en_text:
                problems.append((owner, where, "not translated"))
        en_numbers = sorted(n for _, t in en_texts.items() for n in numbers(t))
        ro_numbers = sorted(n for _, t in ro_texts.items() for n in numbers(t))
        if en_numbers != ro_numbers:
            problems.append((owner, "numbers", f"English {en_numbers} but Romanian {ro_numbers}"))
        quiz = ro_card.get("quiz")
        if isinstance(quiz, dict):
            options = quiz.get("options", [])
            if len(set(options)) != len(options):
                problems.append((owner, "quiz.options", "repeated options"))
    return problems


def check(english, romanian):
    problems = language_problems(romanian)
    if english is not None:
        problems += parity_problems(english, romanian)
    return problems


def load(path):
    with open(path, encoding="utf-8-sig") as handle:
        return json.load(handle)


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument("--romanian", required=True)
    parser.add_argument("--english")
    args = parser.parse_args(argv)
    english = load(args.english) if args.english else None
    problems = check(english, load(args.romanian))
    for owner, where, message in problems:
        print(f"ERROR | {owner} | {where} | {message}")
    cards = len(load(args.romanian).get("discoveries", []))
    print(f"{cards} cards checked, {len(problems)} problems")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
