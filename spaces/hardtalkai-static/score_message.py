"""Standalone port of engine.score_message for comparing against the Static Space JS."""
from __future__ import annotations

import json
import math
import re
import sys

EMPATHY_MARKERS = [
    "understand", "i hear", "i know", "appreciate", "thank", "must be",
    "i can imagine", "that makes sense", "i'm sorry", "sorry to hear",
    "how are you", "how do you feel",
]
ASSERTIVE_MARKERS = [
    "i want", "i need", "i'd like", "i would like", "i am asking", "i'm asking",
    "i expect", "i believe", "i think we should", "let's", "i've decided",
    "i cannot", "i can't take", "no,",
]
HEDGES = [
    "maybe", "just", "kind of", "sort of", "i guess", "if that's okay",
    "if it's not too much", "sorry to bother", "i'm not sure", "possibly",
]
AGGRESSIVE_MARKERS = [
    "you always", "you never", "your fault", "ridiculous", "stupid",
    "shut up", "obviously you", "you people",
]
_WHITESPACE = re.compile(r"\s+")
_DIGIT = re.compile(r"\d")


def _count_matches(text: str, markers: list[str]) -> int:
    return sum(1 for m in markers if m in text)


def _clamp(n: float) -> int:
    return max(0, min(100, math.floor(n + 0.5)))


def score_message(message: str) -> dict:
    text = message.lower().strip()
    words = [w for w in _WHITESPACE.split(text) if w]
    word_count = len(words)
    empathy_hits = _count_matches(text, EMPATHY_MARKERS)
    assertive_hits = _count_matches(text, ASSERTIVE_MARKERS)
    hedge_hits = _count_matches(text, HEDGES)
    aggressive_hits = _count_matches(text, AGGRESSIVE_MARKERS)
    has_question = "?" in text
    has_numbers = bool(_DIGIT.search(text))
    empathy = 35 + empathy_hits * 22 + (12 if has_question else 0)
    empathy -= aggressive_hits * 30
    assertiveness = 30 + assertive_hits * 22 - hedge_hits * 12
    if aggressive_hits > 0:
        assertiveness -= 10
    clarity = 45
    if 8 <= word_count <= 60:
        clarity += 25
    elif word_count < 4:
        clarity -= 25
    elif word_count > 90:
        clarity -= 15
    if has_numbers:
        clarity += 10
    clarity -= hedge_hits * 6
    return {
        "clarity": _clamp(float(clarity)),
        "empathy": _clamp(float(empathy)),
        "assertiveness": _clamp(float(assertiveness)),
        "overall": _clamp(
            _clamp(float(clarity)) * 0.34
            + _clamp(float(empathy)) * 0.33
            + _clamp(float(assertiveness)) * 0.33
        ),
    }


if __name__ == "__main__":
    messages = json.loads(sys.argv[1]) if len(sys.argv) > 1 else []
    print(json.dumps([score_message(m) for m in messages]))
