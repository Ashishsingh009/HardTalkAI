from __future__ import annotations

import math
import re
from dataclasses import dataclass

from .models import ChatTurn, Feedback, ReplyResult, Scenario

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
    """JS ``Math.round``-compatible clamp into the 0-100 range."""
    return max(0, min(100, math.floor(n + 0.5)))


@dataclass
class _Signals:
    aggressive_hits: int
    hedge_hits: int
    empathy_hits: int
    assertive_hits: int
    word_count: int


def score_message(message: str) -> Feedback:
    """Analyze the latest user message and produce coaching feedback."""
    text = message.lower().strip()
    words = [w for w in _WHITESPACE.split(text) if w]
    word_count = len(words)

    empathy_hits = _count_matches(text, EMPATHY_MARKERS)
    assertive_hits = _count_matches(text, ASSERTIVE_MARKERS)
    hedge_hits = _count_matches(text, HEDGES)
    aggressive_hits = _count_matches(text, AGGRESSIVE_MARKERS)
    has_question = "?" in text
    has_numbers = bool(_DIGIT.search(text))

    # Empathy: acknowledging the other person and asking about them.
    empathy = 35 + empathy_hits * 22 + (12 if has_question else 0)
    empathy -= aggressive_hits * 30

    # Assertiveness: clear "I" statements and direct asks, minus hedging.
    assertiveness = 30 + assertive_hits * 22 - hedge_hits * 12
    if aggressive_hits > 0:
        assertiveness -= 10  # aggression is not assertiveness

    # Clarity: reasonable length, concreteness, not rambling.
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

    clarity_score = _clamp(float(clarity))
    empathy_score = _clamp(float(empathy))
    assertiveness_score = _clamp(float(assertiveness))
    overall = _clamp(
        clarity_score * 0.34 + empathy_score * 0.33 + assertiveness_score * 0.33
    )

    signals = _Signals(
        aggressive_hits=aggressive_hits,
        hedge_hits=hedge_hits,
        empathy_hits=empathy_hits,
        assertive_hits=assertive_hits,
        word_count=word_count,
    )
    return Feedback(
        clarity=clarity_score,
        empathy=empathy_score,
        assertiveness=assertiveness_score,
        overall=overall,
        tips=_build_tips(clarity_score, empathy_score, assertiveness_score, signals),
    )


def _build_tips(clarity: int, empathy: int, assertiveness: int, s: _Signals) -> list[str]:
    tips: list[str] = []
    if s.aggressive_hits > 0:
        tips.append(
            'Watch out for blaming language like "you always" - it puts people on '
            "the defensive. Describe the behavior, not the character."
        )
    if empathy < 45:
        tips.append(
            'Acknowledge the other person first. A line like "I know you\'ve been '
            'slammed" lowers defenses before you make your point.'
        )
    if assertiveness < 45:
        tips.append(
            'Be more direct. Lead with a clear "I" statement such as "I\'d like..." '
            "so your ask can't be missed."
        )
    if s.hedge_hits >= 2:
        tips.append(
            'You\'re hedging a lot ("just", "maybe", "sorry"). '
            "Trimming qualifiers makes you sound more confident."
        )
    if clarity < 45 and s.word_count < 8:
        tips.append(
            "Add a bit more detail - one concrete example or number makes your point land."
        )
    if clarity < 45 and s.word_count > 90:
        tips.append("Tighten it up. A shorter, focused message is easier to respond to.")
    if not tips:
        tips.append(
            "Nicely balanced - clear, direct, and considerate. "
            "Keep steering toward a concrete next step."
        )
    return tips


# --- Counterpart reply (deterministic fallback) -----------------------------

Tone = str  # "warming" | "neutral" | "guarded"


def reply_tone(f: Feedback) -> Tone:
    if f.overall >= 60 and f.empathy >= 50:
        return "warming"
    if f.overall <= 42 or f.empathy <= 30:
        return "guarded"
    return "neutral"


def mood_label(tone: Tone) -> str:
    return {
        "warming": "opening up and more receptive",
        "guarded": "defensive and cautious",
        "neutral": "listening, weighing what you said",
    }[tone]


REPLIES: dict[str, dict[Tone, list[str]]] = {
    "ask-for-raise": {
        "warming": [
            "Okay, I appreciate you laying that out so clearly. Those results are real. Let me look at what's possible in the budget - walk me through the numbers you have in mind.",
            "That's a fair case, honestly. I can't promise the full amount today, but I want to advocate for you. Can you send me a short summary I can take to my director?",
        ],
        "neutral": [
            "I hear you. Money's tight this cycle, though. What specifically are you basing the number on?",
            "I get that you want more. Help me understand the impact - what did you ship that moved the needle?",
        ],
        "guarded": [
            "Look, everyone thinks they deserve a raise. I need more than 'I work hard' to take this upstairs.",
            "That's a lot to drop on me. Budgets are frozen. What exactly are you expecting me to do here?",
        ],
    },
    "give-feedback": {
        "warming": [
            "Thanks for saying it that way - that's fair. You're right that the deadlines slipped. I've been drowning, but that's on me to flag earlier. What would help?",
            "I appreciate you being honest and not just piling on. Yeah, the launch date thing hurt the team. Let's figure out a way to catch this sooner.",
        ],
        "neutral": [
            "Okay... I didn't realize it was landing that hard on everyone. What deadlines are you talking about specifically?",
            "That's tough to hear. I've had a lot going on. Can you give me an example so I understand?",
        ],
        "guarded": [
            "Wow. I've been putting in a ton of hours, and this is what I get? It's not all on me, you know.",
            "That feels really unfair. Everyone's behind, not just me. Why am I the one getting this talk?",
        ],
    },
    "decline-request": {
        "warming": [
            "Ah, okay - I hear you that you're at capacity. I don't want to burn you out. When could you realistically pick it up?",
            "That's fair, and thanks for being straight with me. Who else do you think could take the dashboard, or should we push it a sprint?",
        ],
        "neutral": [
            "Hmm, but this is pretty important to leadership. Are you sure you can't squeeze it in?",
            "I understand you're busy, but everyone's busy. Isn't there any way to make it work?",
        ],
        "guarded": [
            "I really need this done, though. Can't you just find the time? It won't take long.",
            "That's disappointing. I was counting on you. So you're just saying no?",
        ],
    },
}


def deterministic_reply(scenario: Scenario, tone: Tone, turn: int) -> str:
    bank = REPLIES.get(scenario.id, {}).get(tone)
    if not bank:
        return "Go on - I'm listening."
    return bank[(turn - 1) % len(bank)]


def generate_reply(
    scenario: Scenario, history: list[ChatTurn], user_message: str
) -> ReplyResult:
    """Deterministic, offline reply + coaching feedback."""
    feedback = score_message(user_message)
    turn_number = sum(1 for t in history if t.role == "user") + 1
    tone = reply_tone(feedback)
    reply = deterministic_reply(scenario, tone, turn_number)
    return ReplyResult(reply=reply, feedback=feedback, mood=mood_label(tone))
