from app.engine import score_message
from app.models import ChatTurn
from app.scenarios import find_scenario
from app.voice import clean_speech, complete_round, persona_voice, realtime_instructions
from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)

RAISE_STRONG = (
    "I appreciate you making time. I'd like a raise: I shipped the payments "
    "rewrite and cut latency 40%."
)
RAISE_HEDGED = "Sorry to bother — maybe I just deserve more if that's okay?"


def test_health_reports_voice_false_without_key(monkeypatch):
    monkeypatch.delenv("OPENAI_API_KEY", raising=False)
    monkeypatch.setattr("app.voice.is_available", lambda: False)
    monkeypatch.setattr("app.ai.engine_name", lambda: "builtin")
    res = client.get("/api/health")
    assert res.status_code == 200
    body = res.json()
    assert body["status"] == "ok"
    assert body["voice"] is False
    assert body["engine"] == "builtin"


def test_voice_session_requires_scenario_id():
    res = client.post("/api/voice/session", json={})
    assert res.status_code == 400
    assert res.json()["error"] == "scenarioId is required"


def test_voice_session_unknown_scenario():
    res = client.post("/api/voice/session", json={"scenarioId": "nope"})
    assert res.status_code == 404


def test_voice_session_503_without_openai(monkeypatch):
    monkeypatch.setattr("app.voice.is_available", lambda: False)
    res = client.post("/api/voice/session", json={"scenarioId": "ask-for-raise"})
    assert res.status_code == 503
    assert "Typed practice still works" in res.json()["error"]


def test_voice_session_mints_ephemeral_secret(monkeypatch):
    monkeypatch.setattr("app.voice.is_available", lambda: True)

    def fake_mint(payload: dict) -> dict:
        session = payload["session"]
        assert session["type"] == "realtime"
        assert session["instructions"]
        assert "Coach Heather" not in session["instructions"]
        assert session["audio"]["output"]["voice"] == "coral"
        return {"value": "ek_test_secret"}

    monkeypatch.setattr("app.voice.mint_client_secret", fake_mint)
    res = client.post("/api/voice/session", json={"scenarioId": "ask-for-raise"})
    assert res.status_code == 200
    body = res.json()
    assert body["clientSecret"] == "ek_test_secret"
    assert body["realtimeUrl"] == "https://api.openai.com/v1/realtime/calls"
    assert body["personaName"] == "Dana"
    assert body["maxUserTurns"] == 3
    assert body["maxDurationSeconds"] == 90
    assert body["opening"]
    assert "Dana" in body["instructions"]
    assert "Coach Heather" not in body["instructions"]


def test_voice_session_openai_failure_is_502(monkeypatch):
    monkeypatch.setattr("app.voice.is_available", lambda: True)

    from app.voice import VoiceProviderError

    def fail(_payload: dict) -> dict:
        raise VoiceProviderError("OpenAI client secret failed (401): nope")

    monkeypatch.setattr("app.voice.mint_client_secret", fail)
    res = client.post("/api/voice/session", json={"scenarioId": "ask-for-raise"})
    assert res.status_code == 502
    assert "401" in res.json()["error"]


def test_clean_speech_strips_fillers_and_adds_period():
    assert clean_speech("um, uh I'd like a raise") == "I'd like a raise."
    assert clean_speech("  What is possible?  ") == "What is possible?"
    assert clean_speech("   ") == ""


def test_voice_complete_scores_transcript_like_chat():
    scenario = find_scenario("ask-for-raise")
    assert scenario is not None
    result = complete_round(
        scenario,
        [
            ChatTurn(role="counterpart", content=scenario.opening),
            ChatTurn(role="user", content="um " + RAISE_HEDGED),
            ChatTurn(role="counterpart", content="Budgets are locked."),
            ChatTurn(role="user", content=RAISE_STRONG),
            ChatTurn(role="counterpart", content="Walk me through the number."),
            ChatTurn(role="user", content=RAISE_STRONG),
        ],
    )
    users = [m for m in result.messages if m.role == "user"]
    assert len(users) == 3
    assert users[0].feedback is not None
    assert users[0].feedback.assertiveness <= 10
    assert users[1].feedback is not None
    assert users[1].feedback.overall >= 60
    assert result.mood
    # Opening filler cleanup should not send "um" into the scorer.
    assert not users[0].content.lower().startswith("um")
    expected = score_message(clean_speech("um " + RAISE_HEDGED))
    assert users[0].feedback.assertiveness == expected.assertiveness


def test_voice_complete_caps_at_three_user_turns():
    scenario = find_scenario("ask-for-raise")
    assert scenario is not None
    extra = [
        ChatTurn(role="user", content=f"{RAISE_STRONG} Turn {n}")
        for n in range(5)
    ]
    result = complete_round(scenario, extra)
    assert sum(1 for m in result.messages if m.role == "user") == 3
    assert result.messages[0].role == "counterpart"
    assert result.messages[0].content == scenario.opening


def test_voice_complete_endpoint_round_trips():
    res = client.post(
        "/api/voice/complete",
        json={
            "scenarioId": "ask-for-raise",
            "turns": [
                {"role": "counterpart", "content": "What's going on?"},
                {
                    "role": "user",
                    "content": RAISE_STRONG,
                },
            ],
        },
    )
    assert res.status_code == 200
    body = res.json()
    users = [m for m in body["messages"] if m["role"] == "user"]
    assert len(users) == 1
    assert users[0]["feedback"]["overall"] >= 60
    assert "mood" in body


def test_voice_complete_unknown_scenario():
    res = client.post(
        "/api/voice/complete",
        json={"scenarioId": "nope", "turns": []},
    )
    assert res.status_code == 404


def test_voice_complete_requires_scenario():
    res = client.post("/api/voice/complete", json={"turns": []})
    assert res.status_code == 400


def test_typed_chat_still_works_without_openai(monkeypatch):
    monkeypatch.setattr("app.voice.is_available", lambda: False)
    res = client.post(
        "/api/chat",
        json={
            "scenarioId": "ask-for-raise",
            "message": RAISE_STRONG,
            "history": [],
        },
    )
    assert res.status_code == 200
    assert res.json()["feedback"]["overall"] >= 60


def test_instructions_are_counterpart_not_heather():
    scenario = find_scenario("give-feedback")
    assert scenario is not None
    text = realtime_instructions(scenario)
    assert "Sam" in text
    assert "Coach Heather" not in text
    assert "HardTalkAI" not in text
    assert "coaching" in text.lower()  # the prohibition
    assert persona_voice("give-feedback") == "ash"
