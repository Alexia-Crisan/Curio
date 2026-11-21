import copy
import json
import os
import tempfile
import unittest

import check_translation as ct

SOURCE = {"name": "NASA", "url": "https://science.nasa.gov/mars/", "license": "Public domain"}

ENGLISH = {
    "theme": {"id": "space", "name": "Space", "tagline": "Look up.", "available": True, "accentColor": "0xFF6366F1"},
    "subcategories": [{"id": "mars", "name": "Mars", "guidedOrder": 1, "accentColor": "0xFFEA580C"}],
    "discoveries": [
        {
            "id": "space.mars.fact.01",
            "subcategoryId": "mars",
            "type": "FACT",
            "title": "A day on Mars is about 40 minutes longer.",
            "body": "One sol lasts 24 hours, 39 minutes and 35 seconds.",
            "source": SOURCE,
        },
        {
            "id": "space.mars.quiz.01",
            "subcategoryId": "mars",
            "type": "QUIZ",
            "title": "Test yourself",
            "quiz": {"question": "Biggest volcano?", "options": ["Mauna Kea", "Olympus Mons"], "correctIndex": 1, "explanation": "It is 27 kilometers high."},
            "source": SOURCE,
        },
    ],
}

ROMANIAN = {
    "theme": {"id": "space", "name": "Spațiu", "tagline": "Privește în sus.", "available": True, "accentColor": "0xFF6366F1"},
    "subcategories": [{"id": "mars", "name": "Marte", "guidedOrder": 1, "accentColor": "0xFFEA580C"}],
    "discoveries": [
        {
            "id": "space.mars.fact.01",
            "subcategoryId": "mars",
            "type": "FACT",
            "title": "O zi pe Marte durează cu 40 de minute mai mult.",
            "body": "Un sol are 24 de ore, 39 de minute și 35 de secunde.",
            "source": SOURCE,
        },
        {
            "id": "space.mars.quiz.01",
            "subcategoryId": "mars",
            "type": "QUIZ",
            "title": "Pune-te la încercare",
            "quiz": {"question": "Cel mai mare vulcan?", "options": ["Mauna Kea", "Olympus Mons"], "correctIndex": 1, "explanation": "Are 27 de kilometri înălțime."},
            "source": SOURCE,
        },
    ],
}


def problems(english, romanian):
    return ct.check(english, romanian)


def messages(found):
    return [m for _, _, m in found]


class CheckTranslationTest(unittest.TestCase):
    def test_a_faithful_translation_has_no_problems(self):
        self.assertEqual([], problems(ENGLISH, ROMANIAN))

    def test_the_english_file_fails_every_check_that_matters(self):
        found = problems(ENGLISH, ENGLISH)
        self.assertTrue(any("not translated" in m for m in messages(found)))
        self.assertTrue(any("looks English" in m for m in messages(found)))

    def test_a_changed_number_is_reported(self):
        ro = copy.deepcopy(ROMANIAN)
        ro["discoveries"][0]["body"] = "Un sol are 24 de ore, 93 de minute și 35 de secunde."
        self.assertTrue(any("numbers" == w for _, w, _ in problems(ENGLISH, ro)))

    def test_decimal_and_thousand_separators_do_not_matter(self):
        en = copy.deepcopy(ENGLISH)
        ro = copy.deepcopy(ROMANIAN)
        en["discoveries"][0]["body"] = "It is 384,400 km away and 1.3 million wide."
        ro["discoveries"][0]["body"] = "Se află la 384.400 km distanță și are 1,3 milioane lățime."
        self.assertEqual([], problems(en, ro))

    def test_a_cedilla_letter_is_reported(self):
        ro = copy.deepcopy(ROMANIAN)
        ro["discoveries"][0]["title"] = "O zi pe Marte durează 40 de minute mai mult, şi atât."
        self.assertTrue(any("cedilla" in m for m in messages(problems(ENGLISH, ro))))

    def test_a_moved_quiz_answer_is_reported(self):
        ro = copy.deepcopy(ROMANIAN)
        ro["discoveries"][1]["quiz"]["correctIndex"] = 0
        self.assertTrue(any(w == "structure" for _, w, _ in problems(ENGLISH, ro)))

    def test_a_changed_id_is_reported_both_ways(self):
        ro = copy.deepcopy(ROMANIAN)
        ro["discoveries"][0]["id"] = "space.mars.fact.99"
        found = messages(problems(ENGLISH, ro))
        self.assertIn("missing in the Romanian file", found)
        self.assertIn("not in the English file", found)

    def test_a_changed_card_order_is_reported(self):
        ro = copy.deepcopy(ROMANIAN)
        ro["discoveries"].reverse()
        self.assertIn("card order changed", messages(problems(ENGLISH, ro)))

    def test_a_changed_source_is_reported(self):
        ro = copy.deepcopy(ROMANIAN)
        ro["discoveries"][0]["source"] = dict(SOURCE, license="CC BY")
        self.assertTrue(any(w == "structure" for _, w, _ in problems(ENGLISH, ro)))

    def test_a_long_title_and_a_long_body_are_reported(self):
        ro = copy.deepcopy(ROMANIAN)
        ro["discoveries"][0]["title"] = "a" * 201
        ro["discoveries"][0]["body"] = "b" * 1001
        found = messages(problems(None, ro))
        self.assertTrue(any("title has 201" in m for m in found))
        self.assertTrue(any("body has 1001" in m for m in found))

    def test_language_checks_work_without_an_english_file(self):
        ro = copy.deepcopy(ROMANIAN)
        self.assertEqual([], problems(None, ro))
        ro["discoveries"][0]["body"] = "The sol is long."
        self.assertTrue(any("looks English" in m for m in messages(problems(None, ro))))

    def test_repeated_quiz_options_are_reported(self):
        ro = copy.deepcopy(ROMANIAN)
        ro["discoveries"][1]["quiz"]["options"] = ["Olympus Mons", "Olympus Mons"]
        self.assertIn("repeated options", messages(problems(ENGLISH, ro)))

    def test_whitespace_problems_are_reported(self):
        ro = copy.deepcopy(ROMANIAN)
        ro["discoveries"][0]["title"] = " O zi  lungă "
        found = messages(problems(None, ro))
        self.assertIn("leading or trailing space", found)
        self.assertIn("double space", found)

    def test_english_inside_quotation_marks_is_a_title_and_is_left_alone(self):
        ro = copy.deepcopy(ROMANIAN)
        ro["discoveries"][0]["title"] = "\u201eThe Interior of the Palm House\u201d este o lucrare de Carl Blechen."
        ro["discoveries"][0]["body"] = "Lucrarea \u201eA Sunday on the Island\u201d are 40 de minute, 24 de ore, 39 de minute, 35 de secunde."
        self.assertEqual([], problems(None, ro))
        ro["discoveries"][0]["title"] = "\u201eThe Palm House\u201d este made of oil on canvas."
        found = messages(problems(None, ro))
        self.assertTrue(any("looks English" in m and "oil" in m for m in found))

    def test_museum_terms_left_in_english_are_reported(self):
        ro = copy.deepcopy(ROMANIAN)
        ro["discoveries"][0]["body"] = "Tehnica: Oil on canvas. Proveniența: Germany."
        found = [m for m in messages(problems(None, ro)) if "looks English" in m]
        self.assertEqual(1, len(found))
        self.assertIn("canvas", found[0])
        self.assertIn("germany", found[0])

    def test_museum_names_are_proper_names_and_are_left_alone(self):
        ro = copy.deepcopy(ROMANIAN)
        ro["discoveries"][0]["body"] = "Sursa informației: Metropolitan Museum of Art și Art Institute of Chicago."
        self.assertEqual([], problems(None, ro))
        ro["discoveries"][0]["body"] = "Sursa informației: Museum of Modern Art."
        self.assertTrue(any("looks English" in m for m in messages(problems(None, ro))))

    def test_a_topic_name_may_be_an_english_artwork_title_but_not_use_cedilla(self):
        ro = copy.deepcopy(ROMANIAN)
        ro["subcategories"][0]["name"] = "The Interior of the Palm House"
        self.assertEqual([], [p for p in problems(None, ro) if p[1] == "subcategory.name"])
        ro["subcategories"][0]["name"] = "Palm House \u015f"
        self.assertTrue(any(w == "subcategory.name" for _, w, _ in problems(None, ro)))

    def test_a_file_saved_with_a_byte_order_mark_still_loads(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "en.json")
            with open(path, "w", encoding="utf-8-sig") as f:
                json.dump(ENGLISH, f)
            self.assertEqual("space", ct.load(path)["theme"]["id"])

    def test_the_command_line_returns_one_on_problems_and_zero_otherwise(self):
        with tempfile.TemporaryDirectory() as tmp:
            en_path = os.path.join(tmp, "en.json")
            ro_path = os.path.join(tmp, "ro.json")
            with open(en_path, "w", encoding="utf-8") as f:
                json.dump(ENGLISH, f)
            with open(ro_path, "w", encoding="utf-8") as f:
                json.dump(ROMANIAN, f, ensure_ascii=False)
            self.assertEqual(0, ct.main(["--english", en_path, "--romanian", ro_path]))
            self.assertEqual(1, ct.main(["--english", en_path, "--romanian", en_path]))


if __name__ == "__main__":
    unittest.main()
