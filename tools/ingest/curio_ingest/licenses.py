ALLOWED_PREFIXES = (
    "CC0 1.0",
    "Public domain",
)


def is_allowed(license_text):
    text = (license_text or "").strip()
    return any(text.startswith(prefix) for prefix in ALLOWED_PREFIXES)
