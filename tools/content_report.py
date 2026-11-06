import glob
import json
import os
import sys

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.abspath(os.path.join(script_dir, ".."))
    content_dir = os.path.join(project_root, "app", "src", "main", "assets", "content")

    if len(sys.argv) > 1:
        json_files = [os.path.abspath(sys.argv[1])]
    else:
        json_files = sorted(glob.glob(os.path.join(content_dir, "*.json")))

    total_themes = 0
    total_topics = 0
    total_discoveries = 0
    total_errors = 0
    total_warnings = 0

    incomplete_topics_by_theme = {}
    all_discovery_ids = set()

    for file_path in json_files:
        if not os.path.exists(file_path):
            continue

        try:
            with open(file_path, "r", encoding="utf-8") as f:
                data = json.load(f)
        except Exception:
            total_errors += 1
            continue

        theme = data.get("theme", {})
        subcategories = data.get("subcategories", [])
        discoveries = data.get("discoveries", [])

        theme_id = theme.get("id", "")
        theme_name = theme.get("name", theme_id)
        is_available = theme.get("available", True)

        total_themes += 1
        print(f"Theme: {theme_name} (id: {theme_id}) [{os.path.basename(file_path)}]")
        print("-" * 60)

        topic_ids = {s.get("id") for s in subcategories if s.get("id")}
        incomplete_count = 0

        for sub in subcategories:
            sub_id = sub.get("id")
            sub_name = sub.get("name", sub_id)
            total_topics += 1

            topic_discoveries = [d for d in discoveries if d.get("subcategoryId") == sub_id]
            count = len(topic_discoveries)
            knowledge_count = sum(1 for d in topic_discoveries if d.get("type") == "KNOWLEDGE")

            status_notes = []
            if count < 6:
                status_notes.append(f"needs {6 - count} more")
            elif count > 6:
                status_notes.append("too many")

            if knowledge_count == 0:
                status_notes.append("no KNOWLEDGE")

            if not status_notes:
                status_str = "OK"
            else:
                status_str = ", ".join(status_notes)
                if is_available:
                    incomplete_count += 1
                    total_errors += 1

            print(f"  Topic: {sub_name} (id: {sub_id}) -> {count} cards, {knowledge_count} KNOWLEDGE [{status_str}]")

        incomplete_topics_by_theme[theme_id] = incomplete_count

        file_errors = []
        file_warnings = []

        for d in discoveries:
            total_discoveries += 1
            d_id = d.get("id", "")

            if not d_id:
                file_errors.append("Discovery missing ID")
                total_errors += 1
            else:
                if d_id in all_discovery_ids:
                    file_errors.append(f"{d_id}: duplicate discovery ID")
                    total_errors += 1
                all_discovery_ids.add(d_id)

            sub_id = d.get("subcategoryId")
            if sub_id not in topic_ids:
                file_errors.append(f"{d_id}: subcategoryId '{sub_id}' is not a topic of theme '{theme_id}'")
                total_errors += 1

            title = d.get("title", "")
            if len(title) > 200:
                file_errors.append(f"{d_id}: title length {len(title)} exceeds limit 200")
                total_errors += 1

            body = d.get("body", "")
            if body and len(body) > 1000:
                file_errors.append(f"{d_id}: body length {len(body)} exceeds limit 1000")
                total_errors += 1

            source = d.get("source", {})
            s_name = source.get("name")
            s_url = source.get("url")
            s_license = source.get("license")

            if not s_name or not s_license or not s_url:
                file_errors.append(f"{d_id}: source missing required name, url, or license")
                total_errors += 1
            elif not s_url.startswith("https://"):
                file_errors.append(f"{d_id}: source URL '{s_url}' does not start with https://")
                total_errors += 1

            media_url = d.get("mediaUrl")
            if media_url:
                alt_text = d.get("altText")
                if not alt_text:
                    file_errors.append(f"{d_id}: mediaUrl provided without altText")
                    total_errors += 1

                if not media_url.startswith("http://") and not media_url.startswith("https://"):
                    media_path = os.path.join(project_root, "app", "src", "main", "assets", media_url)
                    if not os.path.exists(media_path):
                        file_errors.append(f"{d_id}: media file missing on disk at {media_url}")
                        total_errors += 1
                    else:
                        size_kb = os.path.getsize(media_path) / 1024.0
                        if size_kb > 400.0:
                            file_warnings.append(f"{d_id}: media file size {size_kb:.1f} KB exceeds 400 KB limit")
                            total_warnings += 1

        if file_errors:
            print("\n  Errors:")
            for err in file_errors:
                print(f"    - {err}")

        if file_warnings:
            print("\n  Warnings:")
            for warn in file_warnings:
                print(f"    - {warn}")

        print("")

    print("=" * 60)
    print("SUMMARY")
    print("=" * 60)
    print(f"themes {total_themes}, topics {total_topics}, discoveries {total_discoveries}, errors {total_errors}, warnings {total_warnings}")
    for t_id, inc in incomplete_topics_by_theme.items():
        if inc > 0:
            print(f"  Theme '{t_id}': {inc} topics incomplete")
    print("=" * 60)

    if total_errors > 0:
        sys.exit(1)
    else:
        sys.exit(0)

if __name__ == "__main__":
    main()
