import unittest

from curio_ingest.records import clean, first_text, make_record, usable_artist


class RecordsTest(unittest.TestCase):
    def test_clean_collapses_whitespace_and_none(self):
        self.assertEqual("a b c", clean("  a \n b\t c "))
        self.assertEqual("", clean(None))

    def test_unknown_artists_are_dropped(self):
        for name in ["", "Unknown", "unidentified artist", "Anonymous", "Unknown maker"]:
            self.assertEqual("", usable_artist(name))

    def test_unknown_with_a_qualifier_is_dropped(self):
        self.assertEqual("", usable_artist("Unknown Artist (British)"))
        self.assertEqual("", usable_artist("Unidentified Chinese artist"))

    def test_a_real_name_is_kept(self):
        self.assertEqual("Vincent van Gogh", usable_artist(" Vincent  van Gogh "))

    def test_first_text_skips_empty_values(self):
        self.assertEqual("b", first_text("", None, " ", "b", "c"))
        self.assertEqual("", first_text("", None))

    def test_make_record_rejects_unknown_fields(self):
        with self.assertRaises(KeyError):
            make_record(colour="red")

    def test_make_record_has_every_field(self):
        record = make_record(title="x")
        self.assertEqual("x", record["title"])
        self.assertEqual("", record["retrieved_at"])


if __name__ == "__main__":
    unittest.main()
