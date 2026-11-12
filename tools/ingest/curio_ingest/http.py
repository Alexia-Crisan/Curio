import hashlib
import json
import os
import time
import urllib.error
import urllib.request
from pathlib import Path

CONTACT_VARIABLE = "CURIO_INGEST_CONTACT"


class FetchError(Exception):
    pass


def user_agent(contact=None):
    value = contact if contact is not None else os.environ.get(CONTACT_VARIABLE, "")
    value = value.strip()
    if not value:
        raise FetchError("Set " + CONTACT_VARIABLE + " to your contact email before fetching")
    return "CurioIngest/1.0 (" + value + ") python-urllib"


class Client:
    def __init__(self, cache_dir, agent, min_interval, extra_headers=None, opener=None, sleep=None, clock=None, max_retries=3):
        self.cache_dir = Path(cache_dir)
        self.agent = agent
        self.min_interval = min_interval
        self.extra_headers = dict(extra_headers or {})
        self.opener = opener or urllib.request.urlopen
        self.sleep = sleep or time.sleep
        self.clock = clock or time.monotonic
        self.max_retries = max_retries
        self.last_request = None
        self.requests_made = 0

    def cache_path(self, url):
        digest = hashlib.sha256(url.encode("utf-8")).hexdigest()
        return self.cache_dir / (digest + ".json")

    def get_json(self, url):
        path = self.cache_path(url)
        if path.is_file():
            return json.loads(path.read_text(encoding="utf-8"))
        body = self.request(url, "application/json")
        try:
            payload = json.loads(body.decode("utf-8"))
        except ValueError as error:
            raise FetchError("Invalid JSON from " + url + ": " + str(error))
        self.cache_dir.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")
        return payload

    def get_bytes(self, url):
        return self.request(url, "*/*")

    def wait(self):
        if self.last_request is None:
            return
        remaining = self.min_interval - (self.clock() - self.last_request)
        if remaining > 0:
            self.sleep(remaining)

    def request(self, url, accept):
        headers = {"User-Agent": self.agent, "Accept": accept}
        headers.update(self.extra_headers)
        for attempt in range(self.max_retries + 1):
            self.wait()
            self.last_request = self.clock()
            self.requests_made += 1
            try:
                req = urllib.request.Request(url, headers=headers)
                with self.opener(req, timeout=30) as response:
                    return response.read()
            except urllib.error.HTTPError as error:
                retryable = error.code in (429, 500, 502, 503, 504)
                if not retryable or attempt == self.max_retries:
                    raise FetchError("HTTP " + str(error.code) + " for " + url)
                self.sleep(self.retry_delay(error, attempt))
            except urllib.error.URLError as error:
                if attempt == self.max_retries:
                    raise FetchError("Network error for " + url + ": " + str(error.reason))
                self.sleep(2 ** attempt)
        raise FetchError("Giving up on " + url)

    def retry_delay(self, error, attempt):
        header = error.headers.get("Retry-After") if error.headers else None
        if header:
            try:
                return max(float(header), 0.0)
            except ValueError:
                pass
        return float(2 ** (attempt + 1))
