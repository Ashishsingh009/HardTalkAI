from app.engine import REPLIES, generate_reply, score_message
from app.scenarios import FREE_SCENARIO_ID, SCENARIOS, find_scenario

ORIGINAL_IDS = {"ask-for-raise", "give-feedback", "decline-request"}
TONES = ("warming", "neutral", "guarded")


def test_rewards_balanced_empathetic_assertive_messages():
    good = score_message(
        "I really appreciate how slammed you've been. I'd like us to agree on a plan "
        "so the deadlines stop slipping. Can we set a weekly check-in?"
    )
    assert good.empathy > 55
    assert good.assertiveness > 45
    assert good.overall > 60


def test_penalizes_aggressive_blaming_language():
    bad = score_message("You always miss deadlines and it's your fault the team looks stupid.")
    assert bad.empathy < 30
    assert any("blaming" in t.lower() for t in bad.tips)


def test_flags_excessive_hedging():
    hedgy = score_message("Sorry to bother, maybe I just kind of wanted to possibly ask something?")
    assert hedgy.assertiveness < 45
    assert any("hedg" in t.lower() for t in hedgy.tips)


def test_warms_up_counterpart_for_strong_message():
    scenario = find_scenario("ask-for-raise")
    assert scenario is not None
    res = generate_reply(
        scenario,
        [],
        "I appreciate you making time. I'd like to talk about a raise: I shipped 3 launches "
        "and cut latency by 40%. What's possible?",
    )
    assert res.reply
    assert "receptive" in res.mood
    assert res.feedback.overall > 60


def test_makes_counterpart_guarded_for_weak_message():
    scenario = find_scenario("give-feedback")
    assert scenario is not None
    res = generate_reply(scenario, [], "you never do your work")
    assert "defensive" in res.mood


def test_catalog_size_and_stable_original_ids():
    ids = [s.id for s in SCENARIOS]
    assert 8 <= len(ids) <= 10
    assert ORIGINAL_IDS <= set(ids)
    assert ids[0] == FREE_SCENARIO_ID == "ask-for-raise"
    raise_scenario = find_scenario(FREE_SCENARIO_ID)
    assert raise_scenario is not None
    assert raise_scenario.free is True
    assert sum(1 for s in SCENARIOS if s.free) == 1


def test_every_scenario_has_pushback_replies_for_all_tones():
    for scenario in SCENARIOS:
        bank = REPLIES.get(scenario.id)
        assert bank, f"missing canned replies for {scenario.id}"
        for tone in TONES:
            lines = bank.get(tone) or []
            assert len(lines) >= 2, f"{scenario.id} needs at least two {tone} replies"
            assert all(line.strip() for line in lines)


def test_raise_and_feedback_replies_are_specific_not_generic():
    raise_guarded = REPLIES["ask-for-raise"]["guarded"][0]
    feedback_guarded = REPLIES["give-feedback"]["guarded"][0]
    decline_guarded = REPLIES["decline-request"]["guarded"][0]
    assert "packet" in raise_guarded.lower() or "upstairs" in raise_guarded.lower()
    assert "payments" in feedback_guarded.lower() or "checkout" in feedback_guarded.lower()
    assert "friday" in decline_guarded.lower() or "readout" in decline_guarded.lower()
