import json
import tempfile
import unittest
import urllib.error

from curio_ingest.http import FetchError, user_agent
from tests.support import http_error, make_client


class ClientTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp()

    def test_the_contact_is_required(self):
        with self.assertRaises(FetchError):
            user_agent("   ")

    def test_the_contact_goes_into_the_user_agent(self):
        self.assertIn("me@example.test", user_agent("me@example.test"))

    def test_the_agent_is_sent_on_every_request(self):
        client, opener, _ = make_client(self.tmp, [("a.test", {"x": 1})])
        client.get_json("https://a.test/one")
        headers = {key.lower(): value for key, value in opener.calls[0][1].items()}
        self.assertEqual("CurioIngest/1.0 (test@example.test)", headers["user-agent"])

    def test_extra_headers_are_sent(self):
        client, opener, _ = make_client(self.tmp, [("a.test", {"x": 1})])
        client.extra_headers = {"AIC-User-Agent": "agent"}
        client.get_json("https://a.test/one")
        headers = {key.lower(): value for key, value in opener.calls[0][1].items()}
        self.assertEqual("agent", headers["aic-user-agent"])

    def test_a_second_identical_request_is_served_from_the_cache(self):
        client, opener, _ = make_client(self.tmp, [("a.test", {"x": 1})])
        first = client.get_json("https://a.test/one")
        second = client.get_json("https://a.test/one")
        self.assertEqual(first, second)
        self.assertEqual(1, len(opener.calls))

    def test_the_cache_survives_a_new_client(self):
        client, _, _ = make_client(self.tmp, [("a.test", {"x": 1})])
        client.get_json("https://a.test/one")
        other, opener, _ = make_client(self.tmp, [("a.test", {"x": 2})])
        self.assertEqual({"x": 1}, other.get_json("https://a.test/one"))
        self.assertEqual(0, len(opener.calls))

    def test_requests_are_spaced_by_the_minimum_interval(self):
        client, _, clock = make_client(self.tmp, [("a.test", {"x": 1})], interval=1.0)
        client.get_json("https://a.test/one")
        client.get_json("https://a.test/two")
        self.assertEqual(1, len(clock.sleeps))
        self.assertAlmostEqual(1.0, clock.sleeps[0])

    def test_no_wait_before_the_first_request(self):
        client, _, clock = make_client(self.tmp, [("a.test", {"x": 1})])
        client.get_json("https://a.test/one")
        self.assertEqual([], clock.sleeps)

    def test_a_429_is_retried_and_retry_after_is_honoured(self):
        client, opener, clock = make_client(self.tmp, [("a.test", [http_error(429, "7"), {"x": 1}])], interval=0.0)
        self.assertEqual({"x": 1}, client.get_json("https://a.test/one"))
        self.assertEqual(2, len(opener.calls))
        self.assertIn(7.0, clock.sleeps)

    def test_a_503_is_retried(self):
        client, opener, _ = make_client(self.tmp, [("a.test", [http_error(503), {"x": 1}])], interval=0.0)
        client.get_json("https://a.test/one")
        self.assertEqual(2, len(opener.calls))

    def test_a_404_is_not_retried(self):
        client, opener, _ = make_client(self.tmp, [("a.test", http_error(404))], interval=0.0)
        with self.assertRaises(FetchError):
            client.get_json("https://a.test/one")
        self.assertEqual(1, len(opener.calls))

    def test_retries_stop_after_the_maximum(self):
        client, opener, _ = make_client(self.tmp, [("a.test", http_error(500))], interval=0.0)
        with self.assertRaises(FetchError):
            client.get_json("https://a.test/one")
        self.assertEqual(client.max_retries + 1, len(opener.calls))

    def test_a_network_error_becomes_a_fetch_error(self):
        client, _, _ = make_client(self.tmp, [("a.test", urllib.error.URLError("down"))], interval=0.0)
        with self.assertRaises(FetchError):
            client.get_json("https://a.test/one")

    def test_invalid_json_is_not_cached(self):
        client, _, _ = make_client(self.tmp, [("a.test", b"<html>")], interval=0.0)
        with self.assertRaises(FetchError):
            client.get_json("https://a.test/one")
        self.assertFalse(client.cache_path("https://a.test/one").exists())

    def test_bytes_are_not_cached_as_json(self):
        client, _, _ = make_client(self.tmp, [("a.test", b"\xff\xd8binary")], interval=0.0)
        self.assertEqual(b"\xff\xd8binary", client.get_bytes("https://a.test/img.jpg"))
        self.assertFalse(client.cache_path("https://a.test/img.jpg").exists())


if __name__ == "__main__":
    unittest.main()
