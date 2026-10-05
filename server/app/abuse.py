"""Abuse controls for expensive voice-session minting.

Typed POST /api/chat stays open for the Shipaton demo path. Live calls mint a
billable OpenAI Realtime secret, so that route is gated and rate-limited.
"""

from __future__ import annotations

import os
import secrets
import threading
import time
from collections import defaultdict

from fastapi import Request
from fastapi.responses import JSONResponse

VOICE_CLIENT_HEADER = "X-HardTalk-Client"
VOICE_CLIENT_VALUE = "hardtalk-app"
VOICE_TOKEN_HEADER = "X-HardTalk-Voice-Token"


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


def client_key(request: Request) -> str:
    forwarded = (request.headers.get("x-forwarded-for") or "").strip()
    if forwarded:
        return forwarded.split(",")[0].strip() or "unknown"
    if request.client and request.client.host:
        return request.client.host
    return "unknown"


def _header(request: Request, name: str) -> str:
    return (request.headers.get(name) or "").strip()


def _bearer(request: Request) -> str:
    raw = _header(request, "authorization")
    prefix = "bearer "
    if raw.lower().startswith(prefix):
        return raw[len(prefix) :].strip()
    return ""


def _tokens_match(presented: str, expected: str) -> bool:
    if not presented or len(presented) != len(expected):
        return False
    return secrets.compare_digest(presented, expected)


def authorize_voice_session(request: Request) -> JSONResponse | None:
    """Return an error response when the mint request is not allowed."""
    if _header(request, VOICE_CLIENT_HEADER) != VOICE_CLIENT_VALUE:
        return JSONResponse(
            status_code=401,
            content={"error": "Voice sessions require the official HardTalk client."},
        )
    expected = os.getenv("HARDTALK_VOICE_TOKEN", "").strip()
    if expected:
        presented = _header(request, VOICE_TOKEN_HEADER) or _bearer(request)
        if not _tokens_match(presented, expected):
            return JSONResponse(
                status_code=403,
                content={"error": "Voice sessions require a valid access token."},
            )
    if not _voice_session_limiter.allow(client_key(request)):
        return JSONResponse(
            status_code=429,
            content={"error": "Too many voice session requests. Try again shortly."},
            headers={"Retry-After": "60"},
        )
    return None
