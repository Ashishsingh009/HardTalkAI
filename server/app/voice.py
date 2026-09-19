"""Voice practice: live counterpart call, Coach Heather recap after hang-up.

The OpenAI Realtime agent is the **scenario counterpart** (Dana, Sam, …), not
Coach Heather. Heather still scores the transcript locally after the call.
Typed POST /api/chat is unchanged and does not require a realtime key.
"""

from __future__ import annotations

import json
import logging
import os
import re
import urllib.error
import urllib.request

from . import ai
from .engine import mood_label, reply_tone, score_message
from .models import (
    ChatTurn,
    ScoredVoiceTurn,
    Scenario,
    VoiceCompleteResponse,
    VoiceSessionResponse,
)

logger = logging.getLogger("hardtalkai.voice")

# Keep in lockstep with PracticeLoop.MAX_USER_TURNS / MAX_CALL_DURATION_SECONDS.
MAX_USER_TURNS = 3
MAX_CALL_DURATION_SECONDS = 90
CLIENT_SECRET_TTL_SECONDS = 600
REALTIME_MODEL = os.getenv("OPENAI_REALTIME_MODEL", "gpt-realtime")
CLIENT_SECRETS_URL = "https://api.openai.com/v1/realtime/client_secrets"
REALTIME_CALLS_URL = "https://api.openai.com/v1/realtime/calls"

_FILLERS = re.compile(
    r"^\s*((um+|uh+|er+|ah+|hmm+|mm+)\b\s*,?\s*)+",
    re.IGNORECASE,
)
_WHITESPACE = re.compile(r"\s+")

# Stock OpenAI voices — one per drill, not cloned.
_PERSONA_VOICES: dict[str, str] = {
    "ask-for-raise": "coral",
    "give-feedback": "ash",
    "decline-request": "marin",
    "push-back-deadline": "echo",
    "advocate-for-report": "sage",
    "deliver-slip": "ballad",
    "ask-for-headcount": "verse",
    "disagree-up": "cedar",
    "reclaim-credit": "alloy",
    "reclaim-1-1": "shimmer",
}


class VoiceProviderError(RuntimeError):
    """OpenAI client-secret mint failed."""


def is_available() -> bool:
    return ai.is_ai_enabled()


def persona_voice(scenario_id: str) -> str:
    return _PERSONA_VOICES.get(scenario_id, "alloy")


def realtime_instructions(scenario: Scenario) -> str:
    """Counterpart-only prompt. Do not let the model coach or mention Heather."""
    return (
        f"You are {scenario.persona.name}, {scenario.persona.role}. "
        f"You are role-playing the other side of a difficult workplace conversation "
        f"so the user can practice. Scenario: {scenario.summary} "
        f"Your starting disposition: {scenario.persona.mood}. "
        "Stay fully in character. Speak one to three natural conversational sentences. "
        "The user may interrupt you; stop talking immediately if they do, then listen. "
        "Do not break character. Do not give coaching, scores, tips, or meta commentary. "
        "Do not mention a coach, this app, or that you are an AI. "
        f"When the 1:1 starts, your first spoken line must be exactly: {scenario.opening}"
    )


def clean_speech(text: str) -> str:
    """Light cleanup so the typed scoring rubric still works on transcripts."""
    cleaned = _WHITESPACE.sub(" ", (text or "").strip())
    cleaned = _FILLERS.sub("", cleaned).strip(" ,")
    if not cleaned:
        return ""
    if cleaned[-1] not in ".!?":
        cleaned += "."
    return cleaned


def mint_client_secret(payload: dict) -> dict:
    """POST /v1/realtime/client_secrets. Tests monkeypatch this."""
    api_key = os.getenv("OPENAI_API_KEY", "").strip()
    if not api_key:
        raise VoiceProviderError("OPENAI_API_KEY is not set")
    body = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        CLIENT_SECRETS_URL,
        data=body,
        method="POST",
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
            "OpenAI-Safety-Identifier": "hardtalkai-practice",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")[:400]
        raise VoiceProviderError(f"OpenAI client secret failed ({exc.code}): {detail}") from exc
    except urllib.error.URLError as exc:
        raise VoiceProviderError(f"OpenAI client secret failed: {exc}") from exc


def create_session(scenario: Scenario) -> VoiceSessionResponse:
    if not is_available():
        raise VoiceProviderError("Voice calls need OPENAI_API_KEY on the server")

    instructions = realtime_instructions(scenario)
    voice = persona_voice(scenario.id)
    payload = {
        "expires_after": {"anchor": "created_at", "seconds": CLIENT_SECRET_TTL_SECONDS},
        "session": {
            "type": "realtime",
            "model": REALTIME_MODEL,
            "instructions": instructions,
            "audio": {
                "input": {
                    "transcription": {"model": "gpt-4o-mini-transcribe"},
                },
                "output": {"voice": voice},
            },
        },
    }
    raw = mint_client_secret(payload)
    secret = raw.get("value") or (raw.get("client_secret") or {}).get("value")
    if not secret:
        raise VoiceProviderError("OpenAI did not return a client secret")
    return VoiceSessionResponse(
        clientSecret=secret,
        realtimeUrl=REALTIME_CALLS_URL,
        model=REALTIME_MODEL,
        voice=voice,
        opening=scenario.opening,
        instructions=instructions,
        personaName=scenario.persona.name,
        maxUserTurns=MAX_USER_TURNS,
        maxDurationSeconds=MAX_CALL_DURATION_SECONDS,
    )


def complete_round(scenario: Scenario, turns: list[ChatTurn]) -> VoiceCompleteResponse:
    """Score up to three user utterances. Counterpart lines are not scored."""
    cleaned: list[ChatTurn] = []
    for turn in turns:
        content = clean_speech(turn.content)
        if not content:
            continue
        cleaned.append(ChatTurn(role=turn.role, content=content))

    if not cleaned or cleaned[0].role != "counterpart":
        cleaned.insert(0, ChatTurn(role="counterpart", content=scenario.opening))

    messages: list[ScoredVoiceTurn] = []
    user_count = 0
    last_feedback = None
    for turn in cleaned:
        if turn.role == "user":
            if user_count >= MAX_USER_TURNS:
                continue
            user_count += 1
            feedback = score_message(turn.content)
            last_feedback = feedback
            messages.append(
                ScoredVoiceTurn(role="user", content=turn.content, feedback=feedback),
            )
        else:
            messages.append(
                ScoredVoiceTurn(role="counterpart", content=turn.content, feedback=None),
            )

    mood = (
        mood_label(reply_tone(last_feedback))
        if last_feedback is not None
        else scenario.persona.mood
    )
    return VoiceCompleteResponse(messages=messages, mood=mood)
