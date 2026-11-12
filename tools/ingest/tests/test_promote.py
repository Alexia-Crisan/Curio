import copy
import json
import tempfile
import unittest
from pathlib import Path

from curio_ingest.build import build_all, load_template, write_candidates
from curio_ingest.promote import check_candidate, promote
from tests.support import TEMPLATE, batch

META = Path(__file__).resolve().parent.parent / "themes" / "art.json"


def approved(candidate, media_root):
    out = copy.deepcopy(candidate)
    out["status"] = "approved"
    out["review"]["reviewer"] = "Dev B"
    out["discoveries"][0]["mediaAlt"] = "A painting"
    path = Path(media_root) / out["discoveries"][0]["media"]
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b"x" * 1000)
    return out


class PromoteTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.media = self.tmp / "media"
        self.target = self.tmp / "content" / "art.json"
        self.candidates_dir = self.tmp / "candidates"
        template = load_template(TEMPLATE)
        self.candidates, _ = build_all(template, batch(6))
        write_candidates(self.candidates, self.candidates_dir)

    def approve(self, *names):
        for name in names:
            path = self.candidates_dir / (name + ".json")
            data = json.loads(path.read_text(encoding="utf-8"))
            path.write_text(json.dumps(approved(data, self.media), ensure_ascii=False), encoding="utf-8")

    def run_promote(self, **kwargs):
        return promote(self.candidates_dir, self.target, META, self.media, **kwargs)

    def test_nothing_is_promoted_while_everything_is_a_candidate(self):
        report, data = self.run_promote(write=True)
        self.assertEqual([], report["promoted"])
        self.assertEqual(6, len(report["not_approved"]))
        self.assertIsNone(data)
        self.assertFalse(self.target.exists())

    def test_a_dry_run_writes_nothing(self):
        self.approve("met-1")
        report, data = self.run_promote(write=False)
        self.assertEqual(["met-1"], report["promoted"])
        self.assertFalse(self.target.exists())
        self.assertEqual(1, len(data["subcategories"]))

    def test_write_creates_the_theme_file_from_the_theme_meta(self):
        self.approve("met-1")
        self.run_promote(write=True)
        data = json.loads(self.target.read_text(encoding="utf-8"))
        self.assertEqual("art", data["theme"]["id"])
        self.assertFalse(data["theme"]["available"])
        self.assertEqual([{"id": "met-1", "name": "Work 1", "guidedOrder": 1}], data["subcategories"])
        self.assertEqual(6, len(data["discoveries"]))

    def test_app_discoveries_carry_the_subcategory_and_no_review_data(self):
        self.approve("met-1")
        self.run_promote(write=True)
        data = json.loads(self.target.read_text(encoding="utf-8"))
        for entry in data["discoveries"]:
            self.assertEqual("met-1", entry["subcategoryId"])
            self.assertNotIn("status", entry)
            self.assertNotIn("review", entry)
        image = data["discoveries"][0]
        self.assertEqual("A painting", image["mediaAlt"])
        self.assertEqual("art/met-1.jpg", image["media"])
        self.assertNotIn("quiz", image)

    def test_guided_order_continues_after_existing_topics(self):
        self.approve("met-1", "met-2")
        self.run_promote(write=True)
        data = json.loads(self.target.read_text(encoding="utf-8"))
        self.assertEqual([1, 2], [sub["guidedOrder"] for sub in data["subcategories"]])
        self.approve("met-3")
        self.run_promote(write=True)
        data = json.loads(self.target.read_text(encoding="utf-8"))
        self.assertEqual([1, 2, 3], [sub["guidedOrder"] for sub in data["subcategories"]])

    def test_running_twice_does_not_duplicate(self):
        self.approve("met-1")
        self.run_promote(write=True)
        report, _ = self.run_promote(write=True)
        self.assertEqual(["met-1"], report["already"])
        data = json.loads(self.target.read_text(encoding="utf-8"))
        self.assertEqual(6, len(data["discoveries"]))

    def test_existing_content_in_the_target_is_kept(self):
        self.target.parent.mkdir(parents=True)
        existing = {"theme": {"id": "art", "name": "Art", "available": True}, "subcategories": [{"id": "old", "name": "Old", "guidedOrder": 7}], "discoveries": [{"id": "art.old.fact.01"}]}
        self.target.write_text(json.dumps(existing), encoding="utf-8")
        self.approve("met-1")
        self.run_promote(write=True)
        data = json.loads(self.target.read_text(encoding="utf-8"))
        self.assertTrue(data["theme"]["available"])
        self.assertEqual(["old", "met-1"], [sub["id"] for sub in data["subcategories"]])
        self.assertEqual(8, data["subcategories"][1]["guidedOrder"])
        self.assertEqual("art.old.fact.01", data["discoveries"][0]["id"])

    def test_a_target_of_another_theme_is_an_error(self):
        self.target.parent.mkdir(parents=True)
        self.target.write_text(json.dumps({"theme": {"id": "space"}}), encoding="utf-8")
        self.approve("met-1")
        with self.assertRaises(ValueError):
            self.run_promote(write=True)

    def test_a_candidate_without_a_reviewer_is_refused(self):
        self.approve("met-1")
        path = self.candidates_dir / "met-1.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        data["review"]["reviewer"] = " "
        path.write_text(json.dumps(data), encoding="utf-8")
        report, _ = self.run_promote(write=True)
        self.assertEqual([], report["promoted"])
        self.assertIn("reviewer", report["refused"][0][1][0])
        self.assertFalse(self.target.exists())

    def test_a_candidate_without_alt_text_is_refused(self):
        self.approve("met-1")
        path = self.candidates_dir / "met-1.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        data["discoveries"][0]["mediaAlt"] = ""
        path.write_text(json.dumps(data), encoding="utf-8")
        report, _ = self.run_promote(write=True)
        self.assertTrue(any("mediaAlt" in problem for problem in report["refused"][0][1]))

    def test_a_missing_image_file_is_refused(self):
        self.approve("met-1")
        (self.media / "art" / "met-1.jpg").unlink()
        report, _ = self.run_promote(write=True)
        self.assertTrue(any("not found" in problem for problem in report["refused"][0][1]))

    def test_an_image_over_300_kb_is_refused(self):
        self.approve("met-1")
        (self.media / "art" / "met-1.jpg").write_bytes(b"x" * (300 * 1024 + 1))
        report, _ = self.run_promote(write=True)
        self.assertTrue(any("300 KB" in problem for problem in report["refused"][0][1]))

    def test_an_image_of_exactly_300_kb_is_accepted(self):
        self.approve("met-1")
        (self.media / "art" / "met-1.jpg").write_bytes(b"x" * (300 * 1024))
        report, _ = self.run_promote(write=True)
        self.assertEqual(["met-1"], report["promoted"])

    def test_warnings_block_promotion_unless_allowed(self):
        self.approve("met-1")
        path = self.candidates_dir / "met-1.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        data["review"]["warnings"] = ["body longer than 220 characters"]
        path.write_text(json.dumps(data), encoding="utf-8")
        blocked, _ = self.run_promote()
        self.assertEqual([], blocked["promoted"])
        allowed, _ = self.run_promote(allow_warnings=True)
        self.assertEqual(["met-1"], allowed["promoted"])

    def test_a_refused_candidate_does_not_stop_the_others(self):
        self.approve("met-1", "met-2")
        (self.media / "art" / "met-1.jpg").unlink()
        report, _ = self.run_promote(write=True)
        self.assertEqual(["met-2"], report["promoted"])
        self.assertEqual("met-1.json", report["refused"][0][0])

    def test_no_temporary_file_is_left_behind(self):
        self.approve("met-1")
        self.run_promote(write=True)
        self.assertEqual(["art.json"], [path.name for path in self.target.parent.iterdir()])


class CheckTest(unittest.TestCase):
    def setUp(self):
        self.media = Path(tempfile.mkdtemp())
        template = load_template(TEMPLATE)
        candidates, _ = build_all(template, batch(6))
        self.candidate = approved(candidates[0], self.media)

    def problems(self, mutate):
        candidate = copy.deepcopy(self.candidate)
        mutate(candidate)
        return check_candidate(candidate, self.media)

    def test_a_clean_candidate_has_no_problems(self):
        self.assertEqual([], check_candidate(self.candidate, self.media))

    def test_five_cards_are_refused(self):
        self.assertTrue(self.problems(lambda c: c["discoveries"].pop(1)))

    def test_no_knowledge_card_is_refused(self):
        def mutate(c):
            c["discoveries"][5]["type"] = "FACT"
        self.assertTrue(any("KNOWLEDGE" in p for p in self.problems(mutate)))

    def test_an_http_source_is_refused(self):
        def mutate(c):
            c["discoveries"][1]["source"]["url"] = "http://x.test"
        self.assertTrue(any("https" in p for p in self.problems(mutate)))

    def test_a_licence_outside_the_allow_list_is_refused(self):
        def mutate(c):
            c["discoveries"][1]["source"]["license"] = "CC BY-NC"
        self.assertTrue(any("allow-list" in p for p in self.problems(mutate)))

    def test_a_quiz_with_a_bad_index_is_refused(self):
        def mutate(c):
            c["discoveries"][2]["quiz"]["correctIndex"] = 4
        self.assertTrue(any("correctIndex" in p for p in self.problems(mutate)))

    def test_a_quiz_with_repeated_options_is_refused(self):
        def mutate(c):
            c["discoveries"][2]["quiz"]["options"][1] = c["discoveries"][2]["quiz"]["options"][0]
        self.assertTrue(any("options" in p for p in self.problems(mutate)))

    def test_a_duplicate_id_is_refused(self):
        def mutate(c):
            c["discoveries"][1]["id"] = c["discoveries"][0]["id"]
        self.assertTrue(any("duplicate" in p for p in self.problems(mutate)))

    def test_a_bad_id_is_refused(self):
        def mutate(c):
            c["discoveries"][1]["id"] = "bad id"
        self.assertTrue(any("invalid id" in p for p in self.problems(mutate)))

    def test_a_media_path_that_climbs_out_is_refused(self):
        def mutate(c):
            c["discoveries"][0]["media"] = "../secret.jpg"
        self.assertTrue(any("relative path" in p for p in self.problems(mutate)))

    def test_an_absolute_media_path_is_refused(self):
        def mutate(c):
            c["discoveries"][0]["media"] = "/etc/passwd"
        self.assertTrue(any("relative path" in p for p in self.problems(mutate)))

    def test_a_remote_media_url_is_refused(self):
        def mutate(c):
            c["discoveries"][0]["media"] = "https://x.test/a.jpg"
        self.assertTrue(any("relative path" in p for p in self.problems(mutate)))

    def test_a_knowledge_card_without_a_body_is_refused(self):
        def mutate(c):
            c["discoveries"][5]["body"] = ""
        self.assertTrue(any("body" in p for p in self.problems(mutate)))

    def test_an_empty_title_is_refused(self):
        def mutate(c):
            c["discoveries"][1]["title"] = " "
        self.assertTrue(any("title" in p for p in self.problems(mutate)))


if __name__ == "__main__":
    unittest.main()
