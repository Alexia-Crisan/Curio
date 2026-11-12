import tempfile
import unittest

from curio_ingest import artic, met
from tests.support import fixture, make_client


def met_routes():
    return [
        ("/v1.1/search", fixture("met_search.json")),
        ("/objects/436535", fixture("met_436535.json")),
        ("/objects/10001", fixture("met_10001.json")),
        ("/objects/10002", fixture("met_10002.json")),
        ("/objects/10003", fixture("met_10003.json")),
    ]


def artic_routes():
    return [
        ("/artworks/search", fixture("artic_search.json")),
        ("/artworks/27992", fixture("artic_27992.json")),
        ("/artworks/20001", fixture("artic_20001.json")),
        ("/artworks/20002", fixture("artic_20002.json")),
    ]


class MetTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp()

    def test_the_search_url_asks_for_images_and_highlights(self):
        url = met.search_url("van gogh", 0, 50)
        self.assertIn("/v1.1/search?", url)
        self.assertIn("hasImages=true", url)
        self.assertIn("isHighlight=true", url)
        self.assertIn("q=van+gogh", url)
        self.assertIn("limit=50", url)

    def test_highlights_can_be_switched_off(self):
        self.assertNotIn("isHighlight", met.search_url("x", highlights=False))

    def test_the_department_is_optional(self):
        self.assertIn("departmentId=11", met.search_url("x", department_id=11))
        self.assertNotIn("departmentId", met.search_url("x"))

    def test_a_public_domain_object_becomes_a_record(self):
        record = met.to_record(fixture("met_436535.json"))
        self.assertEqual("met", record["source"])
        self.assertEqual("436535", record["source_id"])
        self.assertEqual("Wheat Field with Cypresses", record["title"])
        self.assertEqual("Vincent van Gogh", record["artist"])
        self.assertEqual("1889", record["date"])
        self.assertEqual("Oil on canvas", record["medium"])
        self.assertEqual("Paintings", record["classification"])
        self.assertEqual("https://www.metmuseum.org/art/collection/search/436535", record["page_url"])
        self.assertTrue(record["image_url"].startswith("https://images.metmuseum.org/"))
        self.assertTrue(record["license"].startswith("CC0 1.0"))

    def test_an_object_that_is_not_public_domain_is_dropped(self):
        self.assertIsNone(met.to_record(fixture("met_10001.json")))

    def test_an_object_without_an_image_is_dropped(self):
        self.assertIsNone(met.to_record(fixture("met_10002.json")))

    def test_an_object_with_an_http_page_is_dropped(self):
        data = fixture("met_436535.json")
        data["objectURL"] = "http://www.metmuseum.org/x"
        self.assertIsNone(met.to_record(data))

    def test_an_object_without_a_title_is_dropped(self):
        data = fixture("met_436535.json")
        data["title"] = " "
        self.assertIsNone(met.to_record(data))

    def test_garbage_is_dropped(self):
        self.assertIsNone(met.to_record(None))
        self.assertIsNone(met.to_record([]))

    def test_an_empty_artist_stays_empty(self):
        self.assertEqual("", met.to_record(fixture("met_10003.json"))["artist"])

    def test_the_origin_falls_back_to_the_culture(self):
        self.assertEqual("Iran", met.to_record(fixture("met_10003.json"))["origin"])

    def test_the_origin_prefers_the_country(self):
        data = fixture("met_10003.json")
        data["country"] = "Turkey"
        self.assertEqual("Turkey", met.to_record(data)["origin"])

    def test_discover_keeps_only_usable_records_and_counts_the_rest(self):
        client, _, _ = make_client(self.tmp, met_routes(), interval=0.0)
        records, skipped = met.discover(client, "anything", limit=10)
        self.assertEqual(["436535", "10003"], [record["source_id"] for record in records])
        self.assertEqual(2, skipped)

    def test_discover_stops_at_the_limit(self):
        client, opener, _ = make_client(self.tmp, met_routes(), interval=0.0)
        records, _ = met.discover(client, "anything", limit=1)
        self.assertEqual(1, len(records))
        self.assertEqual(2, len(opener.calls))

    def test_a_search_without_results_gives_nothing(self):
        client, _, _ = make_client(self.tmp, [("/v1.1/search", {"total": 0, "objectIDs": None})], interval=0.0)
        self.assertEqual(([], 0), met.discover(client, "none"))


class ArticTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp()

    def test_the_search_url_filters_public_domain(self):
        url = artic.search_url("seurat", 1, 50)
        self.assertIn("/api/v1/artworks/search?", url)
        self.assertIn("query%5Bterm%5D%5Bis_public_domain%5D=true", url)
        self.assertIn("q=seurat", url)
        self.assertIn("limit=50", url)

    def test_a_public_domain_artwork_becomes_a_record(self):
        payload = fixture("artic_27992.json")
        record = artic.to_record(payload["data"], artic.iiif_base(payload))
        self.assertEqual("artic", record["source"])
        self.assertEqual("27992", record["source_id"])
        self.assertEqual("Georges Seurat", record["artist"])
        self.assertEqual("1884–86", record["date"])
        self.assertEqual("France", record["origin"])
        self.assertEqual("Oil on canvas", record["medium"])
        self.assertEqual("painting", record["classification"])
        self.assertEqual("https://www.artic.edu/artworks/27992", record["page_url"])
        self.assertEqual("https://www.artic.edu/iiif/2/2d484387-2509-5e8e-2c43-22f9981972eb/full/843,/0/default.jpg", record["image_url"])
        self.assertEqual("A painting of people relaxing in a park by a river", record["alt_hint"])
        self.assertTrue(record["license"].startswith("CC0 1.0"))

    def test_the_iiif_base_comes_from_the_response_config(self):
        self.assertEqual("https://example.test/iiif", artic.iiif_base({"config": {"iiif_url": "https://example.test/iiif"}}))

    def test_the_iiif_base_has_a_default(self):
        self.assertEqual(artic.DEFAULT_IIIF, artic.iiif_base({}))

    def test_a_trailing_slash_in_the_iiif_base_is_harmless(self):
        data = fixture("artic_27992.json")["data"]
        record = artic.to_record(data, "https://example.test/iiif/")
        self.assertTrue(record["image_url"].startswith("https://example.test/iiif/2d48"))

    def test_the_artist_falls_back_to_the_first_line_of_artist_display(self):
        data = fixture("artic_27992.json")["data"]
        data["artist_title"] = None
        self.assertEqual("Georges Seurat", artic.to_record(data)["artist"])

    def test_an_unknown_artist_stays_empty(self):
        data = fixture("artic_27992.json")["data"]
        data["artist_title"] = "Unknown"
        data["artist_display"] = "Unknown"
        self.assertEqual("", artic.to_record(data)["artist"])

    def test_missing_optional_fields_do_not_break_the_record(self):
        data = {"id": 5, "title": "Plain", "is_public_domain": True, "image_id": "abc"}
        record = artic.to_record(data)
        self.assertEqual("", record["medium"])
        self.assertEqual("", record["dimensions"])
        self.assertEqual("", record["artist"])

    def test_an_artwork_that_is_not_public_domain_is_dropped(self):
        self.assertIsNone(artic.to_record(fixture("artic_20001.json")["data"]))

    def test_an_artwork_without_an_image_is_dropped(self):
        self.assertIsNone(artic.to_record(fixture("artic_20002.json")["data"]))

    def test_discover_keeps_only_usable_records(self):
        client, _, _ = make_client(self.tmp, artic_routes(), interval=0.0)
        records, skipped = artic.discover(client, "anything", limit=10)
        self.assertEqual(["27992"], [record["source_id"] for record in records])
        self.assertEqual(2, skipped)

    def test_the_search_rows_are_only_used_for_their_ids(self):
        client, opener, _ = make_client(self.tmp, artic_routes(), interval=0.0)
        artic.discover(client, "anything", limit=10)
        self.assertEqual(4, len(opener.calls))


if __name__ == "__main__":
    unittest.main()
