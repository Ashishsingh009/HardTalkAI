"""Honesty checks for Shipaton D6/D7 paste-ready assets."""

from pathlib import Path

from app.engine import generate_reply, score_message
from app.scenarios import find_scenario

REPO_ROOT = Path(__file__).resolve().parents[2]
DOCS = REPO_ROOT / "docs"

RAISE_HEDGED = (
    "Sorry to bother — maybe I just deserve more if that's okay?"
)
RAISE_STRONG = (
    "I appreciate you making time. I'd like a raise: I shipped the payments "
    "rewrite and cut latency 40%."
)
RAISE_CLOSER = (
    "I'd like 12% or a band bump this cycle. I understand the freeze — "
    "what number can you walk into Friday with?"
)
SAM_LINE = (
    "I hear that payments was late. The last two checkout dates still missed, "
    "and on-call is covering it. I'd like a weekly check so this doesn't repeat."
)
PRIYA_LINE = (
    "I know leadership loved the demo. I can't take the dashboard this sprint "
    "— payments cutover is already the commitment. I'd like you to find another "
    "owner, or slip it off Friday's readout."
)


def test_shipaton_docs_exist():
    for name in (
        "shipaton-demo-script.md",
        "shipaton-devpost.md",
        "shipaton-week-status.md",
    ):
        path = DOCS / name
        assert path.is_file(), path
        assert len(path.read_text(encoding="utf-8")) > 800


def test_demo_script_is_a_one_sitting_brief():
    text = (DOCS / "shipaton-demo-script.md").read_text(encoding="utf-8")
    assert "≤2 min" in text or "<=2 min" in text
    assert "Android" in text
    assert "localhost:5173" in text
    assert "Free practice" in text
    assert "Round complete" in text
    assert "practice → score → retry" in text
    assert RAISE_HEDGED in text
    assert RAISE_STRONG in text
    assert RAISE_CLOSER in text
    assert SAM_LINE in text
    assert PRIYA_LINE in text
    assert "Dana" in text and "Sam" in text and "Priya" in text
    assert "OPENAI_API_KEY" in text
    assert "RevenueCat" in text
    assert "do not type live" in text.lower() or "Paste, do not type" in text


def test_demo_lines_still_produce_the_camera_story():
    """Ashish pastes these on camera; guarded → warming → recap must survive engine edits."""
    raise_s = find_scenario("ask-for-raise")
    assert raise_s is not None

    t1 = generate_reply(raise_s, [], RAISE_HEDGED)
    assert t1.feedback.assertiveness <= 10
    assert t1.feedback.overall < 45
    assert "defensive" in t1.mood or "cautious" in t1.mood
    assert "packet" in t1.reply.lower() or "upstairs" in t1.reply.lower()

    t2 = generate_reply(raise_s, [], RAISE_STRONG)
    assert t2.feedback.overall >= 60
    assert t2.feedback.assertiveness >= 45
    assert "receptive" in t2.mood

    t3 = generate_reply(raise_s, [], RAISE_CLOSER)
    assert t3.feedback.overall >= 60
    assert t3.feedback.overall > t1.feedback.overall
    assert "receptive" in t3.mood

    sam = find_scenario("give-feedback")
    assert sam is not None
    sam_res = generate_reply(sam, [], SAM_LINE)
    assert sam_res.feedback.overall >= 55
    assert "receptive" in sam_res.mood

    priya = find_scenario("decline-request")
    assert priya is not None
    priya_res = generate_reply(priya, [], PRIYA_LINE)
    assert priya_res.feedback.overall >= 60
    assert score_message(RAISE_HEDGED).tips


def test_devpost_draft_is_paste_ready_and_honest():
    text = (DOCS / "shipaton-devpost.md").read_text(encoding="utf-8")
    assert "HardTalkAI" in text
    assert "Career Coaching" in text
    assert "https://github.com/Ashishsingh009/HardTalkAI" in text
    for heading in (
        "Inspiration",
        "What it does",
        "How we built it",
        "Challenges we ran into",
        "Accomplishments that we're proud of",
        "What's next",
        "Built with",
    ):
        assert heading in text, heading
    assert "FastAPI" in text
    assert "React" in text
    assert "Kotlin Multiplatform" in text or "Compose Multiplatform" in text
    assert "prototype" in text.lower()
    assert "text" in text.lower()
    assert "voice" in text.lower()
    assert "RevenueCat" in text
    assert "OpenAI" in text
    assert "not a chatbot" in text.lower() or "not open-ended" in text.lower()


def test_week_status_names_prs_blockers_and_merge_order():
    text = (DOCS / "shipaton-week-status.md").read_text(encoding="utf-8")
    for pr in ("#5", "#6", "#7", "#8"):
        assert pr in text
    assert "RevenueCat" in text
    assert "Play Console" in text
    assert "OPENAI" in text or "OpenAI" in text
    assert "Merge order" in text
    assert "Merge **#5**" in text
    assert "merge **#6**" in text
    assert "merge **#7**" in text
    assert "merge **#8**" in text
    assert "Merge **#9**" in text or "merge **#9**" in text
    assert "do not commit" in text.lower()
    assert "hardtalkai_pro_monthly" in text
    assert "shipaton-demo-script.md" in text
    assert "shipaton-devpost.md" in text
    # Linear stack appears in the merge-order section as 5 then 6 then 7 then 8
    merge = text[text.index("Merge order") :]
    pos = [merge.index(f"**#{n}**") for n in (5, 6, 7, 8)]
    assert pos == sorted(pos)
