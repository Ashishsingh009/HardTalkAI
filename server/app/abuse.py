"""Abuse controls for expensive voice-session minting.

Typed POST /api/chat stays open for the Shipaton demo path. Live calls mint a
billable OpenAI Realtime secret, so that route fails closed unless the operator
configures ``HARDTALK_VOICE_SECRET`` and the Android build presents a short-lived
HMAC token signed with the same secret.
"""

from __future__ import annotations

import hashlib
import hmac
import os
import re
import threading
import time
from collections import defaultdict

from fastapi import Request
from fastapi.responses import JSONResponse

VOICE_CLIENT_HEADER = "X-HardTalk-Client"
VOICE_CLIENT_VALUE = "hardtalk-app"
VOICE_TOKEN_HEADER = "X-HardTalk-Voice-Token"
VOICE_SECRET_ENV = "HARDTALK_VOICE_SECRET"
TRUSTED_PROXIES_ENV = "HARDTALK_TRUSTED_PROXIES"
MIN_VOICE_SECRET_LENGTH = 16
VOICE_TOKEN_TTL_SECONDS = 120
VOICE_TOKEN_SKEW_SECONDS = 30
VOICE_TOKEN_VERSION = "v1"

_INSTALL_ID = re.compile(r"^[0-9a-f]{32}$")
_SIGNATURE = re.compile(r"^[0-9a-f]{64}$")


class SlidingWindowLimiter:
    def __init__(self, max_requests: int, window_seconds: float) -> None:
        self.max_requests = max(1, int(max_requests))
        self.window_seconds = float(window_seconds)
        self._hits: dict[str, list[float]] = defaultdict(list)
        self._lock = threading.Lock()

    def allow(self, key: str, now: float | None = None) -> bool:
        stamp = time.monotonic() if now is None else now
        window_start = stamp - self.window_seconds
        with self._lock:
            recent = [ts for ts in self._hits[key] if ts > window_start]
            if len(recent) >= self.max_requests:
                self._hits[key] = recent
                return False
            recent.append(stamp)
            self._hits[key] = recent
            return True

    def reset(self) -> None:
        with self._lock:
            self._hits.clear()


def _int_env(name: str, default: int) -> int:
    raw = os.getenv(name, "").strip()
    if not raw:
        return default
    try:
        return max(1, int(raw))
    except ValueError:
        return default


def default_voice_session_limiter() -> SlidingWindowLimiter:
    return SlidingWindowLimiter(
        max_requests=_int_env("VOICE_SESSION_RATE_LIMIT", 8),
        window_seconds=float(_int_env("VOICE_SESSION_RATE_WINDOW", 60)),
    )


_voice_session_limiter = default_voice_session_limiter()


def reset_voice_session_limiter(
    limiter: SlidingWindowLimiter | None = None,
) -> SlidingWindowLimiter:
    global _voice_session_limiter
    _voice_session_limiter = limiter or default_voice_session_limiter()
    return _voice_session_limiter


def voice_secret() -> str:
    return os.getenv(VOICE_SECRET_ENV, "").strip()


def voice_secret_configured() -> bool:
    return len(voice_secret()) >= MIN_VOICE_SECRET_LENGTH


def trusted_proxy_hosts() -> set[str]:
    raw = os.getenv(TRUSTED_PROXIES_ENV, "")
    return {part.strip() for part in raw.split(",") if part.strip()}


def socket_peer(request: Request) -> str:
    if request.client and request.client.host:
        return request.client.host
    return "unknown"


def first_forwarded_for(request: Request) -> str:
    raw = (request.headers.get("x-forwarded-for") or "").strip()
    if not raw:
        return ""
    return raw.split(",")[0].strip()


def client_key(request: Request) -> str:
    """Rate-limit by TCP peer only. Install IDs are HMAC identity, not buckets."""
    peer = socket_peer(request)
    if peer in trusted_proxy_hosts():
        forwarded = first_forwarded_for(request)
        if forwarded:
            return forwarded
    return peer


def _header(request: Request, name: str) -> str:
    return (request.headers.get(name) or "").strip()


def voice_signing_payload(install_id: str, expires_at: int) -> str:
    return f"{VOICE_TOKEN_VERSION}\n{install_id}\n{expires_at}"


def issue_voice_access_token(
    secret: str,
    install_id: str,
    now: int,
    ttl_seconds: int = VOICE_TOKEN_TTL_SECONDS,
) -> str:
    expires_at = int(now) + int(ttl_seconds)
    signature = hmac.new(
        secret.encode("utf-8"),
        voice_signing_payload(install_id, expires_at).encode("utf-8"),
        hashlib.sha256,
    ).hexdigest()
    return f"{VOICE_TOKEN_VERSION}.{install_id}.{expires_at}.{signature}"


def verify_voice_access_token(
    secret: str,
    raw: str,
    now: int | None = None,
) -> dict[str, int | str] | None:
    if len(secret) < MIN_VOICE_SECRET_LENGTH:
        return None
    parts = (raw or "").strip().split(".")
    if len(parts) != 4:
        return None
    version, install_id, expires_raw, signature = parts
    if version != VOICE_TOKEN_VERSION:
        return None
    if not _INSTALL_ID.fullmatch(install_id) or not _SIGNATURE.fullmatch(signature):
        return None
    try:
        expires_at = int(expires_raw)
    except ValueError:
        return None
    stamp = int(time.time() if now is None else now)
    if stamp > expires_at + VOICE_TOKEN_SKEW_SECONDS:
        return None
    if expires_at - stamp > VOICE_TOKEN_TTL_SECONDS + VOICE_TOKEN_SKEW_SECONDS:
        return None
    expected = hmac.new(
        secret.encode("utf-8"),
        voice_signing_payload(install_id, expires_at).encode("utf-8"),
        hashlib.sha256,
    ).hexdigest()
    if not hmac.compare_digest(expected, signature):
        return None
    return {"installId": install_id, "expiresAt": expires_at}


def authorize_voice_session(request: Request) -> JSONResponse | None:
    """Return an error response when the mint request is not allowed."""
    secret = voice_secret()
    if len(secret) < MIN_VOICE_SECRET_LENGTH:
        return JSONResponse(
            status_code=503,
            content={
                "error": (
                    "Voice calls need HARDTALK_VOICE_SECRET on the server. "
                    "Typed practice still works."
                ),
            },
        )
    if _header(request, VOICE_CLIENT_HEADER) != VOICE_CLIENT_VALUE:
        return JSONResponse(
            status_code=401,
            content={"error": "Voice sessions require the official HardTalk client."},
        )
    presented = _header(request, VOICE_TOKEN_HEADER)
    claims = verify_voice_access_token(secret, presented)
    if claims is None:
        return JSONResponse(
            status_code=403,
            content={"error": "Voice sessions require a valid signed access token."},
        )
    key = client_key(request)
    if not _voice_session_limiter.allow(key):
        return JSONResponse(
            status_code=429,
            content={"error": "Too many voice session requests. Try again shortly."},
            headers={"Retry-After": "60"},
        )
    return None
