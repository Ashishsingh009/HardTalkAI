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
            "Okay — those numbers are real, and eighteen months at the same band is going to look bad in calibration. I can't promise the full amount before Friday, but I'll fight for a number. What figure do you need me to walk in with?",
            "That's a case I can take upstairs. I still have a freeze on the packet, so help me with a one-pager: impact, the number, and what happens if we wait a cycle.",
        ],
        "neutral": [
            "I hear you. Freeze is freeze though, and I already told finance we're flat. Walk me through the impact in a way I can defend in ten minutes.",
            "Everyone wants a bump this cycle. What specifically did you ship that a director who doesn't know your name would recognize?",
        ],
        "guarded": [
            "Look, everyone thinks they deserve a raise. I already filed the packet as no-change. What, specifically, am I supposed to take upstairs that isn't 'I work hard'?",
            "This is a lot to drop with ten minutes on the clock. Budgets are locked. What exactly are you expecting me to do before Friday?",
        ],
    },
    "give-feedback": {
        "warming": [
            "Thanks for saying it that way — that's fair. The last two dates did slip, and on-call covering it is on me to flag earlier. What would a weekly check look like so this doesn't repeat?",
            "I appreciate you not just piling on. Yeah, checkout landing late hurt the squad. Let's pick one concrete change for the next two sprints.",
        ],
        "neutral": [
            "Okay... I didn't realize it was landing that hard on everyone. Which dates are you talking about, specifically — and what do you want me to do differently?",
            "That's tough to hear. The API really was late. Can you separate what was on me from what was payments, so I know what to own?",
        ],
        "guarded": [
            "Wow. I've been covering the API mess for weeks and this is the conversation? Why am I the one in the room when checkout was late because of payments?",
            "That feels like I'm being made the example. Everyone slipped. Why is this my performance issue and not a team staffing problem?",
        ],
    },
    "decline-request": {
        "warming": [
            "Ah — I hear you that payments is already the sprint. I don't want to blow that up. Who else could own the dashboard, or do we slip it off Friday's readout?",
            "That's fair, and thanks for being straight. I'll take 'not this sprint.' If I find another owner, can you give them a one-hour context dump?",
        ],
        "neutral": [
            "Hmm. This is on the exec readout though. Are you sure there's no way to squeeze a thin version in alongside payments?",
            "I understand you're at capacity, but leadership is going to ask why analytics isn't there. Isn't there any slice you can ship?",
        ],
        "guarded": [
            "I really need this on Friday. Can't you just find the time? It's a dashboard — it won't take long.",
            "That's disappointing. I already put your name on the readout. So you're just saying no, with leadership watching?",
        ],
    },
    "push-back-deadline": {
        "warming": [
            "Alright. I don't love it, but a six-week plan with a demo in three is something I can take to the CEO. Write the milestone dates down before I walk into that room.",
            "Okay — you're not sandbagging, you're saving me from a public miss. What's the smallest thing we can put in front of the CEO in three weeks so this doesn't look like a slip?",
        ],
        "neutral": [
            "I already said three weeks. If I walk that back I look unreliable. What would have to be true for three weeks to still be honest?",
            "Help me understand the eight-week number. Is that padded, or is three weeks actually going to blow up in production?",
        ],
        "guarded": [
            "I didn't ask for a lecture on process. I told the CEO three weeks. Are you telling me you can't do your job, or are you telling me no?",
            "This sounds like sandbagging. Other teams hit dates like this. Why is yours the exception?",
        ],
    },
    "advocate-for-report": {
        "warming": [
            "Those examples actually change the picture. I still have a curve to protect, but I can mark Jordan as a debate instead of a meets. What else is in the packet if I'm challenged?",
            "Okay. I'll hold a exceeds slot if you send me two artifacts I can read tonight — not adjectives, evidence. If the room still says meets, we document a path this quarter.",
        ],
        "neutral": [
            "I hear you like Jordan. Everyone likes their person. What did they do that a skeptical director would call exceeds, not solid-meets?",
            "We're over the budget. If I move Jordan up, someone else drops. Who are you willing to trade, or is there a different ask?",
        ],
        "guarded": [
            "We don't have room, and 'they're high potential' is not a calibration argument. Unless you have evidence in the next thirty seconds, Jordan stays meets.",
            "I asked for one minute. I'm not reopening the stack rank because you feel strongly. What's the actual exceeds bar they cleared?",
        ],
    },
    "deliver-slip": {
        "warming": [
            "Thank you for not letting me walk into execs still saying green. Give me the new date, what broke, and one mitigation I can put in the thread in the next hour.",
            "Okay. I'll take the slip upstairs if you give me a clean narrative: load test failed, here's the patch window, here's what customers will see. Don't make me invent it.",
        ],
        "neutral": [
            "I just told them green. How long have we known the load test was shaky, and what is the earliest honest date now?",
            "I need more than 'it failed overnight.' What still ships Thursday, and what are we actually moving?",
        ],
        "guarded": [
            "I pinged the thread ten minutes ago. You're telling me now? How am I supposed to look like I run this team?",
            "This is the third time status was green until it wasn't. What exactly do you want me to say to execs — that we were surprised again?",
        ],
    },
    "ask-for-headcount": {
        "warming": [
            "The weekend on-call pattern is the part I can't ignore. I can't open a req tomorrow, but I'll take a contractor-or-backfill option to finance. Send me the last six weeks of pages and who held them.",
            "Alright. Freeze is still freeze, but attrition in that squad is more expensive. Draft a one-req case: role, risk if we wait, and what we stop doing without it.",
        ],
        "neutral": [
            "Everyone's tired. That's not a headcount case. What broke — a date, a resignation risk, a paging number I can put in a spreadsheet?",
            "I can ask. They will say no. What's the smallest version of this ask if a full req is dead until next FY?",
        ],
        "guarded": [
            "Freeze means freeze. I'm not walking into finance with 'the team feels burned out.' Come back when you have a business risk, not a vibe.",
            "If I staff every tired team we'd have no budget. Why is yours the exception, and why isn't this just a prioritization problem?",
        ],
    },
    "disagree-up": {
        "warming": [
            "I don't love being challenged on a board call, but the incident risk is real. Fine — we keep a thin reliability slice. You own making the AI demo still look like progress. Don't make me regret this.",
            "Okay. We'll do both, scoped. Put a one-pager in the channel before standup: what reliability work continues, what pauses, and how the demo still hits the board date.",
        ],
        "neutral": [
            "I hear the incident fear. The board still wants an AI story. If we don't pause reliability, what do you actually cut so the demo still ships?",
            "You're objecting in a room of six. Give me the concrete production risk, not a philosophy of reliability, or I'm closing this.",
        ],
        "guarded": [
            "I wasn't asking for a debate. The demo is the board story. If you want to die on reliability hill, say that clearly so I know where you stand.",
            "This is not the forum. I already aligned with the CEO. Any other 'objections,' or can we move on?",
        ],
    },
    "reclaim-credit": {
        "warming": [
            "You're right that I framed it as we, and I did take the VP follow-up. That wasn't the intent. I'll correct the attribution in the recap and loop you on the next one. Fair?",
            "Okay, I hear it. I'll send the VP a note that the design was yours and ask to add you to the thread. I don't want this turning into a thing.",
        ],
        "neutral": [
            "It was a team design. I'm not sure what you want me to do retroactively — an email? A shout-out? Spell out the correction.",
            "I introduced it as we because that's how it was built. Which part do you feel got erased, specifically?",
        ],
        "guarded": [
            "Come on. You're making this petty. It was a team win and I was the one in the room. Are you saying I stole something?",
            "If we start litigating who said 'we' in all-hands, nobody's going to want to present. Can we drop this?",
        ],
    },
    "reclaim-1-1": {
        "warming": [
            "You're right — I've been running these like standup. Let's use the rest of this one on the promo packet. What do you need from me before next month?",
            "Fair. Status can live in the doc. I'll hold the back half of these 1:1s for career. What's the first thing that belongs on that agenda?",
        ],
        "neutral": [
            "I scanned the board because I don't want surprises. We can talk career, but is anything actually red this week, or can I let that go?",
            "Promo packet, got it. I have eight minutes. What specifically do you need me to do — write a rec, review bullets, or something else?",
        ],
        "guarded": [
            "If something's on fire I need to know. Career stuff can wait a week. Anything actually blocked, or is this a process conversation?",
            "I have another meeting. If this isn't a delivery issue, can we put 'career chat' on next week's calendar instead of hijacking this one?",
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
