from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_reports_ok():
    res = client.get("/api/health")
    assert res.status_code == 200
    body = res.json()
    assert body["status"] == "ok"
    assert body["scenarios"] == 3


def test_scenarios_returns_all():
    body = client.get("/api/scenarios").json()
    ids = {s["id"] for s in body["scenarios"]}
    assert ids == {"ask-for-raise", "give-feedback", "decline-request"}


def test_chat_returns_reply_and_feedback():
    res = client.post(
        "/api/chat",
        json={
            "scenarioId": "ask-for-raise",
            "message": "I appreciate your time. I'd like a raise: I shipped 3 launches and cut latency 40%. What's possible?",
            "history": [],
        },
    )
    assert res.status_code == 200
    body = res.json()
    assert "reply" in body
    assert "feedback" in body
    assert "mood" in body
    assert set(body["feedback"]) >= {"clarity", "empathy", "assertiveness", "overall", "tips"}


def test_chat_validates_missing_message():
    res = client.post("/api/chat", json={"scenarioId": "ask-for-raise"})
    assert res.status_code == 400
    assert res.json()["error"] == "message is required"


def test_chat_unknown_scenario():
    res = client.post("/api/chat", json={"scenarioId": "nope", "message": "hi there"})
    assert res.status_code == 404
