"""Coaching service: combines local scoring with an optional AI reply."""

from __future__ import annotations

import logging

from . import ai
from .engine import deterministic_reply, mood_label, reply_tone, score_message
from .models import ChatTurn, ReplyResult, Scenario

logger = logging.getLogger("hardtalkai.coach")


def respond(scenario: Scenario, history: list[ChatTurn], message: str) -> ReplyResult:
    feedback = score_message(message)
    tone = reply_tone(feedback)
    turn_number = sum(1 for t in history if t.role == "user") + 1

    reply: str
    if ai.is_ai_enabled():
        try:
            reply = ai.generate_ai_reply(scenario, history, message, tone)
        except Exception as exc:  # noqa: BLE001 - fall back to offline engine
            logger.warning("AI reply failed, falling back to builtin engine: %s", exc)
            reply = deterministic_reply(scenario, tone, turn_number)
    else:
        reply = deterministic_reply(scenario, tone, turn_number)

    return ReplyResult(reply=reply, feedback=feedback, mood=mood_label(tone))
