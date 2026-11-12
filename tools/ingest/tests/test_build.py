import copy
import json
import re
import tempfile
import unittest
from pathlib import Path

from curio_ingest import build
from curio_ingest.build import TemplateError, build_all, build_topic, load_template, write_candidates
from tests.support import TEMPLATE, batch, record


class TemplateTest(unittest.TestCase):
    def setUp(self):
        self.template = load_template(TEMPLATE)

    def test_the_shipped_template_is_valid_and_has_six_cards(self):
        self.assertEqual(6, len(self.template["cards"]))
        self.assertEqual(["IMAGE", "FACT", "QUIZ", "FACT", "FACT", "KNOWLEDGE"], [card["type"] for card in self.template["cards"]])

    def test_an_unknown_placeholder_is_rejected(self):
        bad = copy.deepcopy(self.template)
        bad["cards"][1]["variants"][0]["title"] = "{nonsense}"
        with self.assertRaises(TemplateError):
            build.validate_template(bad)

    def test_a_placeholder_missing_from_requires_is_rejected(self):
        bad = copy.deepcopy(self.template)
        bad["cards"][1]["variants"][0]["requires"] = ["title"]
        with self.assertRaises(TemplateError):
            build.validate_template(bad)

    def test_an_unknown_required_field_is_rejected(self):
        bad = copy.deepcopy(self.template)
        bad["cards"][1]["variants"][0]["requires"].append("colour")
        with self.assertRaises(TemplateError):
            build.validate_template(bad)

    def test_five_cards_are_rejected(self):
        bad = copy.deepcopy(self.template)
        bad["cards"].pop(1)
        with self.assertRaises(TemplateError):
            build.validate_template(bad)

    def test_a_template_without_knowledge_is_rejected(self):
        bad = copy.deepcopy(self.template)
        bad["cards"][5]["type"] = "FACT"
        with self.assertRaises(TemplateError):
            build.validate_template(bad)

    def test_duplicate_card_ids_are_rejected(self):
        bad = copy.deepcopy(self.template)
        bad["cards"][3]["id"] = bad["cards"][1]["id"]
        with self.assertRaises(TemplateError):
            build.validate_template(bad)

    def test_a_knowledge_variant_without_a_body_is_rejected(self):
        bad = copy.deepcopy(self.template)
        del bad["cards"][5]["variants"][0]["body"]
        with self.assertRaises(TemplateError):
            build.validate_template(bad)

    def test_an_image_variant_without_media_is_rejected(self):
        bad = copy.deepcopy(self.template)
        del bad["cards"][0]["variants"][0]["media"]
        with self.assertRaises(TemplateError):
            build.validate_template(bad)

    def test_a_quiz_whose_answer_field_is_not_required_is_rejected(self):
        bad = copy.deepcopy(self.template)
        bad["cards"][2]["variants"][0]["requires"].remove("artist")
        with self.assertRaises(TemplateError):
            build.validate_template(bad)


class BuildTest(unittest.TestCase):
    def setUp(self):
        self.template = load_template(TEMPLATE)
        self.records = batch(6)
        self.candidate, self.reason = build_topic(self.template, self.records[0], self.records)

    def test_a_full_record_builds_a_candidate(self):
        self.assertEqual("", self.reason)
        self.assertEqual("candidate", self.candidate["status"])
        self.assertEqual("met-1", self.candidate["topic"]["id"])
        self.assertEqual("Work 1", self.candidate["topic"]["name"])

    def test_the_candidate_has_six_cards_in_template_order(self):
        ids = [entry["id"] for entry in self.candidate["discoveries"]]
        self.assertEqual(
            ["art.met-1.image.01", "art.met-1.fact.01", "art.met-1.quiz.01", "art.met-1.fact.02", "art.met-1.fact.03", "art.met-1.know.01"],
            ids,
        )

    def test_card_text_is_filled_from_the_record(self):
        cards = {entry["id"].split(".")[-2] + "." + entry["id"].split(".")[-1]: entry for entry in self.candidate["discoveries"]}
        self.assertEqual("“Work 1” is a work by Artist 1.", cards["image.01"]["title"])
        self.assertIn("1801", cards["image.01"]["body"])
        self.assertEqual("The medium of “Work 1” is listed as: Oil on canvas.", cards["fact.01"]["title"])
        self.assertEqual("“Work 1” measures 1 × 10 cm.", cards["fact.02"]["title"])
        self.assertEqual("“Work 1” comes from Place 1.", cards["fact.03"]["title"])
        self.assertIn("Metropolitan Museum of Art", cards["know.01"]["body"])

    def test_no_placeholder_is_left_in_any_text(self):
        text = json.dumps(self.candidate, ensure_ascii=False)
        self.assertIsNone(re.search(r"\{[a-z_]+\}", text))

    def test_every_card_carries_a_source_with_the_exact_page(self):
        for entry in self.candidate["discoveries"]:
            self.assertEqual("https://www.metmuseum.org/art/collection/search/1", entry["source"]["url"])
            self.assertTrue(entry["source"]["license"].startswith("CC0 1.0"))
            self.assertIn("Artist 1", entry["source"]["attribution"])

    def test_the_image_card_has_a_media_path_and_an_empty_alt(self):
        image = self.candidate["discoveries"][0]
        self.assertEqual("art/met-1.jpg", image["media"])
        self.assertEqual("", image["mediaAlt"])

    def test_provenance_records_where_the_data_came_from(self):
        provenance = self.candidate["provenance"]
        self.assertEqual("met", provenance["sourceApi"])
        self.assertEqual("1", provenance["sourceId"])
        self.assertEqual("2026-11-10", provenance["retrievedAt"])
        self.assertEqual("https://images.metmuseum.org/1.jpg", provenance["imageUrl"])

    def test_the_review_block_starts_empty(self):
        self.assertEqual("", self.candidate["review"]["reviewer"])
        self.assertEqual([], self.candidate["review"]["warnings"])

    def test_the_quiz_has_four_distinct_options_and_the_right_answer(self):
        quiz = self.candidate["discoveries"][2]["quiz"]
        self.assertEqual(4, len(quiz["options"]))
        self.assertEqual(4, len(set(quiz["options"])))
        self.assertEqual("Artist 1", quiz["options"][quiz["correctIndex"]])
        self.assertEqual("Who made “Work 1”?", quiz["question"])

    def test_the_quiz_never_uses_the_answer_as_a_distractor(self):
        for index, rec in enumerate(self.records):
            candidate, _ = build_topic(self.template, rec, self.records)
            quiz = candidate["discoveries"][2]["quiz"]
            self.assertEqual(1, quiz["options"].count(rec["artist"]))

    def test_the_quiz_is_deterministic(self):
        again, _ = build_topic(self.template, self.records[0], self.records)
        self.assertEqual(self.candidate["discoveries"][2]["quiz"], again["discoveries"][2]["quiz"])

    def test_the_correct_position_varies_across_topics(self):
        positions = set()
        pool = batch(12)
        for rec in pool:
            candidate, _ = build_topic(self.template, rec, pool)
            positions.add(candidate["discoveries"][2]["quiz"]["correctIndex"])
        self.assertGreater(len(positions), 1)

    def test_a_pool_with_one_artist_falls_back_to_the_date_quiz(self):
        pool = batch(4, artist="Same Artist")
        candidate, reason = build_topic(self.template, pool[0], pool)
        self.assertIsNotNone(candidate, reason)
        self.assertEqual("How does the Metropolitan Museum of Art date “Work 1”?", candidate["discoveries"][2]["quiz"]["question"])

    def test_a_single_record_cannot_build_a_quiz(self):
        candidate, reason = build_topic(self.template, self.records[0], [self.records[0]])
        self.assertIsNone(candidate)
        self.assertIn("quiz.01", reason)

    def test_a_licence_outside_the_allow_list_is_refused(self):
        rec = record(1, license="CC BY-NC 4.0")
        candidate, reason = build_topic(self.template, rec, self.records)
        self.assertIsNone(candidate)
        self.assertIn("allow-list", reason)

    def test_an_empty_licence_is_refused(self):
        candidate, reason = build_topic(self.template, record(1, license=""), self.records)
        self.assertIsNone(candidate)

    def test_an_http_page_is_refused(self):
        candidate, reason = build_topic(self.template, record(1, page_url="http://x.test/1"), self.records)
        self.assertIsNone(candidate)
        self.assertIn("https", reason)

    def test_a_missing_medium_uses_the_classification_variant(self):
        rec = record(1, medium="")
        candidate, reason = build_topic(self.template, rec, self.records)
        self.assertIsNotNone(candidate, reason)
        self.assertEqual("The Metropolitan Museum of Art classifies “Work 1” as: Paintings.", candidate["discoveries"][1]["title"])

    def test_a_missing_origin_uses_the_credit_line_variant(self):
        candidate, _ = build_topic(self.template, record(1, origin=""), self.records)
        self.assertEqual("The credit line of “Work 1” reads: Gift 1.", candidate["discoveries"][4]["title"])

    def test_a_record_with_nothing_for_a_card_is_skipped_with_the_card_named(self):
        rec = record(1, origin="", credit_line="")
        candidate, reason = build_topic(self.template, rec, self.records)
        self.assertIsNone(candidate)
        self.assertIn("fact.03", reason)

    def test_a_missing_artist_uses_the_museum_variant_of_the_image_card(self):
        candidate, _ = build_topic(self.template, record(1, artist=""), self.records)
        self.assertEqual("“Work 1” belongs to the collection of the Metropolitan Museum of Art.", candidate["discoveries"][0]["title"])
        self.assertNotIn("body", candidate["discoveries"][0])

    def test_a_long_title_and_body_raise_warnings(self):
        candidate, _ = build_topic(self.template, record(1, title="T" * 100, dimensions="D" * 300), self.records)
        joined = " ".join(candidate["review"]["warnings"])
        self.assertIn("topic name longer", joined)
        self.assertIn("fact.02", joined)

    def test_values_that_look_like_placeholders_are_not_expanded(self):
        candidate, _ = build_topic(self.template, record(1, title="{artist}"), self.records)
        self.assertEqual("“{artist}” is a work by Artist 1.", candidate["discoveries"][0]["title"])


class BuildAllTest(unittest.TestCase):
    def setUp(self):
        self.template = load_template(TEMPLATE)

    def test_build_all_returns_candidates_and_skips(self):
        records = batch(6) + [record(99, license="Proprietary")]
        candidates, skipped = build_all(self.template, records)
        self.assertEqual(6, len(candidates))
        self.assertEqual([("met-99", skipped[0][1])], skipped)

    def test_the_order_does_not_depend_on_the_input_order(self):
        records = batch(6)
        first, _ = build_all(self.template, records)
        second, _ = build_all(self.template, list(reversed(records)))
        self.assertEqual(first, second)

    def test_two_sources_do_not_collide(self):
        records = batch(5) + [record(1, source="artic", museum="Art Institute of Chicago", page_url="https://www.artic.edu/artworks/1")]
        candidates, _ = build_all(self.template, records)
        ids = [candidate["topic"]["id"] for candidate in candidates]
        self.assertEqual(len(ids), len(set(ids)))
        self.assertIn("artic-1", ids)

    def test_write_candidates_creates_one_file_per_topic(self):
        candidates, _ = build_all(self.template, batch(6))
        out = tempfile.mkdtemp()
        written, kept = write_candidates(candidates, out)
        self.assertEqual(6, len(written))
        self.assertEqual([], kept)
        self.assertEqual("met-1", json.loads((Path(out) / "met-1.json").read_text(encoding="utf-8"))["topic"]["id"])

    def test_existing_candidates_are_never_overwritten(self):
        candidates, _ = build_all(self.template, batch(6))
        out = tempfile.mkdtemp()
        write_candidates(candidates, out)
        path = Path(out) / "met-1.json"
        edited = json.loads(path.read_text(encoding="utf-8"))
        edited["review"]["reviewer"] = "Dev B"
        path.write_text(json.dumps(edited), encoding="utf-8")
        written, kept = write_candidates(candidates, out)
        self.assertEqual([], written)
        self.assertEqual(6, len(kept))
        self.assertEqual("Dev B", json.loads(path.read_text(encoding="utf-8"))["review"]["reviewer"])

    def test_overwrite_replaces_existing_candidates(self):
        candidates, _ = build_all(self.template, batch(6))
        out = tempfile.mkdtemp()
        write_candidates(candidates, out)
        written, kept = write_candidates(candidates, out, overwrite=True)
        self.assertEqual(6, len(written))


if __name__ == "__main__":
    unittest.main()
