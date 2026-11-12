import argparse
import datetime
import json
import sys
from pathlib import Path

from . import artic, met
from .build import build_all, load_template, write_candidates
from .http import Client, FetchError, user_agent
from .promote import MAX_IMAGE_BYTES, promote

TOOL_ROOT = Path(__file__).resolve().parent.parent
REPO_ROOT = TOOL_ROOT.parent.parent
ASSETS = REPO_ROOT / "app" / "src" / "main" / "assets" / "content"
SOURCES = {"met": met, "artic": artic}


def make_client(source, args):
    agent = user_agent()
    headers = {"AIC-User-Agent": agent} if source == "artic" else {}
    return Client(Path(args.cache), agent, SOURCES[source].MIN_INTERVAL, headers)


def read_records(directory):
    records = []
    for path in sorted(Path(directory).rglob("*.json")):
        records.append(json.loads(path.read_text(encoding="utf-8")))
    return records


def command_fetch(args):
    module = SOURCES[args.source]
    client = make_client(args.source, args)
    records, skipped = module.discover(client, args.query, args.limit, not args.all_works, args.department)
    today = datetime.datetime.now(datetime.timezone.utc).date().isoformat()
    out = Path(args.records) / args.source
    out.mkdir(parents=True, exist_ok=True)
    for record in records:
        record["retrieved_at"] = today
        (out / (record["source_id"] + ".json")).write_text(json.dumps(record, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("fetched " + str(len(records)) + " records into " + str(out) + ", skipped " + str(skipped) + " (not public domain, no image or no usable fields), requests made " + str(client.requests_made))
    return 0


def command_build(args):
    template = load_template(args.template)
    records = read_records(args.records)
    candidates, skipped = build_all(template, records)
    written, kept = write_candidates(candidates, args.out, args.overwrite)
    print("records " + str(len(records)) + ", candidates built " + str(len(candidates)) + ", written " + str(len(written)) + ", kept existing " + str(len(kept)) + ", skipped " + str(len(skipped)))
    for topic_id, reason in skipped:
        print("  skipped " + topic_id + ": " + reason)
    withwarn = [c["topic"]["id"] for c in candidates if c["review"]["warnings"]]
    if withwarn:
        print("  with warnings: " + ", ".join(withwarn))
    return 0


def command_images(args):
    out = Path(args.images)
    clients = {}
    count = 0
    for path in sorted(Path(args.candidates).glob("*.json")):
        candidate = json.loads(path.read_text(encoding="utf-8"))
        provenance = candidate["provenance"]
        if not any(entry.get("media") for entry in candidate["discoveries"]):
            continue
        source = provenance["sourceApi"]
        if source not in clients:
            clients[source] = make_client(source, args)
        target = out / candidate["theme"] / (candidate["topic"]["id"] + ".jpg")
        if target.exists():
            continue
        data = clients[source].get_bytes(provenance["imageUrl"])
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
        count += 1
        note = "  TOO LARGE: shrink under 300 KB before copying" if len(data) > MAX_IMAGE_BYTES else ""
        print(str(target) + " " + str(len(data) // 1024) + " KB" + note)
    print("downloaded " + str(count) + " images")
    return 0


def command_promote(args):
    target = Path(args.target) if args.target else ASSETS / (args.theme + ".json")
    meta = Path(args.theme_meta) if args.theme_meta else TOOL_ROOT / "themes" / (args.theme + ".json")
    media_root = Path(args.media_root) if args.media_root else ASSETS / "media"
    report, data = promote(args.candidates, target, meta, media_root, args.write, args.allow_warnings)
    print("not approved (ignored): " + str(len(report["not_approved"])))
    print("already in the theme file: " + str(len(report["already"])))
    for name, problems in report["refused"]:
        print("REFUSED " + name)
        for problem in problems:
            print("  - " + problem)
    print("promoted: " + (", ".join(report["promoted"]) or "none"))
    if report["promoted"]:
        print(("written to " if args.write else "dry run, nothing written; would write to ") + str(target))
        if not args.write:
            print("run again with --write after reading the list above")
    return 1 if report["refused"] else 0


def parser():
    root = argparse.ArgumentParser(prog="curio_ingest")
    root.add_argument("--cache", default=str(TOOL_ROOT / ".cache"))
    sub = root.add_subparsers(dest="command", required=True)

    fetch = sub.add_parser("fetch")
    fetch.add_argument("--source", choices=sorted(SOURCES), required=True)
    fetch.add_argument("--query", required=True)
    fetch.add_argument("--limit", type=int, default=20)
    fetch.add_argument("--all-works", action="store_true")
    fetch.add_argument("--department", type=int, default=None)
    fetch.add_argument("--records", default=str(TOOL_ROOT / "records"))
    fetch.set_defaults(run=command_fetch)

    build = sub.add_parser("build")
    build.add_argument("--template", required=True)
    build.add_argument("--records", default=str(TOOL_ROOT / "records"))
    build.add_argument("--out", required=True)
    build.add_argument("--overwrite", action="store_true")
    build.set_defaults(run=command_build)

    images = sub.add_parser("images")
    images.add_argument("--candidates", required=True)
    images.add_argument("--images", default=str(TOOL_ROOT / "images"))
    images.set_defaults(run=command_images)

    prom = sub.add_parser("promote")
    prom.add_argument("--theme", required=True)
    prom.add_argument("--candidates", required=True)
    prom.add_argument("--target", default=None)
    prom.add_argument("--theme-meta", default=None)
    prom.add_argument("--media-root", default=None)
    prom.add_argument("--allow-warnings", action="store_true")
    prom.add_argument("--write", action="store_true")
    prom.set_defaults(run=command_promote)
    return root


def main(argv=None):
    args = parser().parse_args(argv)
    try:
        return args.run(args)
    except (FetchError, ValueError, OSError) as error:
        print("ERROR: " + str(error), file=sys.stderr)
        return 2
