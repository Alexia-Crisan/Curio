#!/usr/bin/env python3
import json
import glob
import os
import sys

def analyze_content():
    content_dir = os.path.join("app", "src", "main", "assets", "content")
    json_files = sorted(glob.glob(os.path.join(content_dir, "*.json")))

    if not json_files:
        print(f"No content JSON files found in {content_dir}")
        return

    print("=" * 70)
    print("CURIO CONTENT STATUS REPORT")
    print("=" * 70)

    total_themes = 0
    total_topics = 0
    total_discoveries = 0
    complete_topics = 0

    for file_path in json_files:
        try:
            with open(file_path, "r", encoding="utf-8") as f:
                data = json.load(f)
        except Exception as e:
            print(f"Error reading {file_path}: {e}")
            continue

        theme = data.get("theme", {})
        subcategories = data.get("subcategories", [])
        discoveries = data.get("discoveries", [])

        theme_id = theme.get("id", "unknown")
        theme_name = theme.get("name", "Unknown")

        total_themes += 1
        print(f"\nTheme: {theme_name} (id: {theme_id}) [{os.path.basename(file_path)}]")
        print("-" * 50)

        for sub in subcategories:
            sub_id = sub.get("id")
            sub_name = sub.get("name")
            total_topics += 1

            topic_discoveries = [d for d in discoveries if d.get("subcategoryId") == sub_id]
            count = len(topic_discoveries)
            total_discoveries += count

            knowledge_count = sum(1 for d in topic_discoveries if d.get("type") == "KNOWLEDGE")
            quiz_count = sum(1 for d in topic_discoveries if d.get("type") == "QUIZ")
            fact_count = sum(1 for d in topic_discoveries if d.get("type") == "FACT")

            has_sources = all(
                d.get("source", {}).get("name") and d.get("source", {}).get("url")
                for d in topic_discoveries
            )

            is_complete = count >= 6 and knowledge_count >= 1 and has_sources
            if is_complete:
                complete_topics += 1
                status = "COMPLETE [6/6]"
            else:
                status = f"INCOMPLETE [{count}/6]"

            print(f"  Topic: {sub_name} (id: {sub_id}) -> {status}")
            print(f"    Types: {fact_count} FACT, {knowledge_count} KNOWLEDGE, {quiz_count} QUIZ / OTHER")
            print(f"    Sources valid: {'YES' if has_sources else 'NO'}")

            for d in topic_discoveries:
                title = d.get("title", "")
                d_type = d.get("type", "UNKNOWN")
                print(f"      - [{d_type}] {title[:60]}{'...' if len(title) > 60 else ''}")

    print("\n" if json_files else "")
    print("=" * 70)
    print("SUMMARY")
    print("=" * 70)
    print(f"Total Themes:       {total_themes}")
    print(f"Total Topics:       {total_topics}")
    print(f"Complete Topics:    {complete_topics} / {total_topics}")
    print(f"Total Discoveries:  {total_discoveries}")
    print("=" * 70)

if __name__ == "__main__":
    analyze_content()
