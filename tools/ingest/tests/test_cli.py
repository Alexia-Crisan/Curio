import contextlib
import io
import json
import os
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from curio_ingest import cli
from curio_ingest.http import Client
from tests.support import FakeOpener, TEMPLATE, fixture, record
from tests.test_sources import artic_routes, met_routes

META = Path(__file__).resolve().parent.parent / "themes" / "art.json"


def run(argv, routes, contact="me@example.test"):
    opener = FakeOpener(routes)
    real = Client.__init__

    def init(self, cache_dir, agent, min_interval, extra_headers=None, opener_=None, sleep=None, clock=None, max_retries=3):
        real(self, cache_dir, agent, 0.0, extra_headers, opener, lambda seconds: None, clock, max_retries)

    out = io.StringIO()
    err = io.StringIO()
    env = {"CURIO_INGEST_CONTACT": contact} if contact else {}
    with mock.patch.dict(os.environ, env, clear=True), mock.patch.object(Client, "__init__", init):
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            code = cli.main(argv)
    return code, out.getvalue(), err.getvalue(), opener


class FetchTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.base = ["--cache", str(self.tmp / "cache")]

    def test_fetch_without_a_contact_fails_before_any_request(self):
        code, _, err, opener = run(self.base + ["fetch", "--source", "met", "--query", "x", "--records", str(self.tmp / "r")], met_routes(), contact="")
        self.assertEqual(2, code)
        self.assertIn("CURIO_INGEST_CONTACT", err)
        self.assertEqual(0, len(opener.calls))

    def test_fetch_met_writes_one_file_per_usable_record_with_the_date(self):
        code, out, _, _ = run(self.base + ["fetch", "--source", "met", "--query", "x", "--records", str(self.tmp / "r")], met_routes())
        self.assertEqual(0, code)
        files = sorted(path.name for path in (self.tmp / "r" / "met").iterdir())
        self.assertEqual(["10003.json", "436535.json"], files)
        data = json.loads((self.tmp / "r" / "met" / "436535.json").read_text(encoding="utf-8"))
        self.assertRegex(data["retrieved_at"], r"^\d{4}-\d{2}-\d{2}$")
        self.assertIn("fetched 2 records", out)
        self.assertIn("skipped 2", out)

    def test_fetch_artic_sends_the_aic_header(self):
        code, _, _, opener = run(self.base + ["fetch", "--source", "artic", "--query", "x", "--records", str(self.tmp / "r")], artic_routes())
        self.assertEqual(0, code)
        headers = {key.lower() for key in opener.calls[0][1]}
        self.assertIn("aic-user-agent", headers)
        self.assertTrue((self.tmp / "r" / "artic" / "27992.json").exists())

    def test_fetch_met_does_not_send_the_aic_header(self):
        _, _, _, opener = run(self.base + ["fetch", "--source", "met", "--query", "x", "--records", str(self.tmp / "r")], met_routes())
        self.assertNotIn("aic-user-agent", {key.lower() for key in opener.calls[0][1]})

    def test_a_failing_request_is_an_error_not_a_traceback(self):
        code, _, err, _ = run(self.base + ["fetch", "--source", "met", "--query", "x", "--records", str(self.tmp / "r")], [])
        self.assertEqual(2, code)
        self.assertIn("ERROR", err)


class PipelineTest(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())
        self.base = ["--cache", str(self.tmp / "cache")]
        records = self.tmp / "records" / "met"
        records.mkdir(parents=True)
        for n in range(1, 7):
            (records / (str(n) + ".json")).write_text(json.dumps(record(n)), encoding="utf-8")

    def build(self, *extra):
        return run(self.base + ["build", "--template", str(TEMPLATE), "--records", str(self.tmp / "records"), "--out", str(self.tmp / "cand")] + list(extra), [])

    def test_build_reports_and_writes(self):
        code, out, _, _ = self.build()
        self.assertEqual(0, code)
        self.assertIn("candidates built 6", out)
        self.assertEqual(6, len(list((self.tmp / "cand").glob("*.json"))))

    def test_the_second_build_keeps_the_existing_files(self):
        self.build()
        _, out, _, _ = self.build()
        self.assertIn("written 0", out)
        self.assertIn("kept existing 6", out)

    def test_images_are_downloaded_once_and_flagged_when_large(self):
        self.build()
        routes = [("images.metmuseum.org/1.jpg", b"x" * (300 * 1024 + 1)), ("images.metmuseum.org", b"jpg")]
        code, out, _, opener = run(self.base + ["images", "--candidates", str(self.tmp / "cand"), "--images", str(self.tmp / "img")], routes)
        self.assertEqual(0, code)
        self.assertIn("TOO LARGE", out)
        self.assertEqual(6, len(opener.calls))
        self.assertTrue((self.tmp / "img" / "art" / "met-1.jpg").exists())
        code, out, _, opener = run(self.base + ["images", "--candidates", str(self.tmp / "cand"), "--images", str(self.tmp / "img")], routes)
        self.assertEqual(0, len(opener.calls))
        self.assertIn("downloaded 0", out)

    def test_promote_is_a_dry_run_without_write(self):
        self.build()
        for path in (self.tmp / "cand").glob("*.json"):
            data = json.loads(path.read_text(encoding="utf-8"))
            data["status"] = "approved"
            data["review"]["reviewer"] = "Dev B"
            data["discoveries"][0]["mediaAlt"] = "A painting"
            path.write_text(json.dumps(data), encoding="utf-8")
            image = self.tmp / "media" / data["discoveries"][0]["media"]
            image.parent.mkdir(parents=True, exist_ok=True)
            image.write_bytes(b"jpg")
        target = self.tmp / "out" / "art.json"
        argv = self.base + ["promote", "--theme", "art", "--candidates", str(self.tmp / "cand"), "--target", str(target), "--theme-meta", str(META), "--media-root", str(self.tmp / "media")]
        code, out, _, _ = run(argv, [])
        self.assertEqual(0, code)
        self.assertIn("dry run", out)
        self.assertFalse(target.exists())
        code, out, _, _ = run(argv + ["--write"], [])
        self.assertEqual(0, code)
        self.assertTrue(target.exists())
        self.assertEqual(36, len(json.loads(target.read_text(encoding="utf-8"))["discoveries"]))

    def test_promote_returns_1_when_something_is_refused(self):
        self.build()
        path = self.tmp / "cand" / "met-1.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        data["status"] = "approved"
        path.write_text(json.dumps(data), encoding="utf-8")
        argv = self.base + ["promote", "--theme", "art", "--candidates", str(self.tmp / "cand"), "--target", str(self.tmp / "o.json"), "--theme-meta", str(META), "--media-root", str(self.tmp / "media")]
        code, out, _, _ = run(argv, [])
        self.assertEqual(1, code)
        self.assertIn("REFUSED met-1.json", out)


if __name__ == "__main__":
    unittest.main()
