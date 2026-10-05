from starlette.requests import Request

from app.abuse import (
    MIN_VOICE_SECRET_LENGTH,
    VOICE_CLIENT_HEADER,
    VOICE_CLIENT_VALUE,
    VOICE_SECRET_ENV,
    client_key,
    first_forwarded_for,
    issue_voice_access_token,
    socket_peer,
    SlidingWindowLimiter,
    verify_voice_access_token,
    voice_secret_configured,
    voice_signing_payload,
)
import hmac
import hashlib


SECRET = "test-voice-secret-16"
INSTALL = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
NOW = 1_700_000_000


def test_sliding_window_limiter_caps_requests():
    limiter = SlidingWindowLimiter(max_requests=2, window_seconds=60)
    assert limiter.allow("1.1.1.1", now=100.0) is True
    assert limiter.allow("1.1.1.1", now=101.0) is True
    assert limiter.allow("1.1.1.1", now=102.0) is False
    assert limiter.allow("2.2.2.2", now=102.0) is True


def test_sliding_window_limiter_expires_hits():
    limiter = SlidingWindowLimiter(max_requests=1, window_seconds=10)
    assert limiter.allow("ip", now=0.0) is True
    assert limiter.allow("ip", now=9.0) is False
    assert limiter.allow("ip", now=10.1) is True


def test_client_header_constants_match_app():
    assert VOICE_CLIENT_HEADER == "X-HardTalk-Client"
    assert VOICE_CLIENT_VALUE == "hardtalk-app"
    assert MIN_VOICE_SECRET_LENGTH == 16
    assert VOICE_SECRET_ENV == "HARDTALK_VOICE_SECRET"


def test_hmac_matches_kotlin_vector():
    payload = voice_signing_payload(INSTALL, NOW + 120)
    expected = hmac.new(SECRET.encode(), payload.encode(), hashlib.sha256).hexdigest()
    assert expected == "4445f880dd53f46eff38108f96e275e3e779d66b4d75a7a5ffdea279c3c9364b"
    token = issue_voice_access_token(SECRET, INSTALL, NOW)
    assert verify_voice_access_token(SECRET, token, now=NOW)["installId"] == INSTALL
    assert verify_voice_access_token("other-voice-secret-16", token, now=NOW) is None
    assert verify_voice_access_token(SECRET, token, now=NOW + 200) is None


def test_voice_secret_configured_requires_length(monkeypatch):
    monkeypatch.delenv(VOICE_SECRET_ENV, raising=False)
    assert voice_secret_configured() is False
    monkeypatch.setenv(VOICE_SECRET_ENV, "short")
    assert voice_secret_configured() is False
    monkeypatch.setenv(VOICE_SECRET_ENV, SECRET)
    assert voice_secret_configured() is True


def _request(peer: str, xff: str | None = None) -> Request:
    headers = []
    if xff:
        headers.append((b"x-forwarded-for", xff.encode()))
    return Request(
        {
            "type": "http",
            "asgi": {"spec_version": "2.3", "version": "3.0"},
            "http_version": "1.1",
            "method": "POST",
            "scheme": "http",
            "path": "/api/voice/session",
            "raw_path": b"/api/voice/session",
            "query_string": b"",
            "headers": headers,
            "client": (peer, 12345),
            "server": ("test", 80),
        },
    )


def test_rate_limit_key_uses_socket_peer_not_client_xff():
    request = _request("203.0.113.9", xff="198.51.100.1, 10.0.0.1")
    assert socket_peer(request) == "203.0.113.9"
    assert first_forwarded_for(request) == "198.51.100.1"
    assert client_key(request, INSTALL) == f"203.0.113.9|{INSTALL}"


def test_rate_limit_key_uses_xff_only_from_trusted_proxy(monkeypatch):
    monkeypatch.setenv("HARDTALK_TRUSTED_PROXIES", "10.0.0.5, 10.0.0.6")
    trusted = _request("10.0.0.5", xff="198.51.100.7")
    assert client_key(trusted, INSTALL) == f"198.51.100.7|{INSTALL}"
    untrusted = _request("203.0.113.9", xff="198.51.100.7")
    assert client_key(untrusted, INSTALL) == f"203.0.113.9|{INSTALL}"
