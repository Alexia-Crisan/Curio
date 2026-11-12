import io
import json
import urllib.error
from pathlib import Path

from curio_ingest.http import Client
from curio_ingest.records import make_record

FIXTURES = Path(__file__).resolve().parent / "fixtures"
TEMPLATE = Path(__file__).resolve().parent.parent / "templates" / "art.json"


def fixture(name):
    return json.loads((FIXTURES / name).read_text(encoding="utf-8"))


class FakeResponse:
    def __init__(self, body):
        self.body = body

    def read(self):
        return self.body

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        return False


class FakeOpener:
    def __init__(self, routes):
        self.routes = routes
        self.calls = []

    def __call__(self, request, timeout=None):
        url = request.full_url
        self.calls.append((url, dict(request.header_items())))
        for needle, answer in self.routes:
            if needle in url:
                if isinstance(answer, list):
                    item = answer.pop(0) if len(answer) > 1 else answer[0]
                else:
                    item = answer
                if isinstance(item, Exception):
                    raise item
                body = item if isinstance(item, bytes) else json.dumps(item).encode("utf-8")
                return FakeResponse(body)
        raise urllib.error.HTTPError(url, 404, "not found", {}, io.BytesIO(b""))


def http_error(code, retry_after=None):
    headers = {"Retry-After": retry_after} if retry_after else {}
    return urllib.error.HTTPError("https://example.test", code, "error", headers, io.BytesIO(b""))


class Clock:
    def __init__(self):
        self.now = 1000.0
        self.sleeps = []

    def monotonic(self):
        return self.now

    def sleep(self, seconds):
        self.sleeps.append(seconds)
        self.now += seconds


def make_client(tmp, routes, interval=1.0, agent="CurioIngest/1.0 (test@example.test)"):
    clock = Clock()
    opener = FakeOpener(routes)
    client = Client(tmp, agent, interval, opener=opener, sleep=clock.sleep, clock=clock.monotonic)
    return client, opener, clock


def record(n, **overrides):
    values = dict(
        source="met",
        source_id=str(n),
        museum="Metropolitan Museum of Art",
        page_url="https://www.metmuseum.org/art/collection/search/" + str(n),
        title="Work " + str(n),
        artist="Artist " + str(n),
        date="18" + str(n % 100).zfill(2),
        origin="Place " + str(n),
        medium="Oil on canvas",
        dimensions=str(n) + " × 10 cm",
        credit_line="Gift " + str(n),
        classification="Paintings",
        image_url="https://images.metmuseum.org/" + str(n) + ".jpg",
        alt_hint="",
        license="CC0 1.0 (Met Open Access); public-domain work",
        retrieved_at="2026-11-10",
    )
    values.update(overrides)
    return make_record(**values)


def batch(count=6, **overrides):
    return [record(n, **overrides) for n in range(1, count + 1)]
