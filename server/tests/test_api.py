from pathlib import Path

from fastapi.testclient import TestClient

from app.main import app
from app.scenarios import FREE_SCENARIO_ID, SCENARIOS

REPO_ROOT = Path(__file__).resolve().parents[2]

client = TestClient(app)

ORIGINAL_IDS = {"ask-for-raise", "give-feedback", "decline-request"}


def test_health_reports_ok():
    res = client.get("/api/health")
    assert res.status_code == 200
    body = res.json()
    assert body["status"] == "ok"
    assert 8 <= body["scenarios"] <= 10
    assert body["scenarios"] == len(SCENARIOS)


def test_scenarios_returns_career_catalog():
    body = client.get("/api/scenarios").json()
    scenarios = body["scenarios"]
    ids = [s["id"] for s in scenarios]
    assert 8 <= len(ids) <= 10
    assert len(ids) == len(set(ids))
    assert ORIGINAL_IDS <= set(ids)
    assert ids[0] == FREE_SCENARIO_ID == "ask-for-raise"

    by_id = {s["id"]: s for s in scenarios}
    free_ids = {s["id"] for s in scenarios if s.get("free")}
    assert free_ids == {FREE_SCENARIO_ID}
    assert by_id["ask-for-raise"]["free"] is True
    assert by_id["ask-for-raise"]["persona"]["name"] == "Dana"
    assert by_id["give-feedback"]["persona"]["name"] == "Sam"
    assert by_id["decline-request"]["persona"]["name"] == "Priya"
    for scenario in scenarios:
        assert scenario["title"]
        assert scenario["summary"]
        assert scenario["opening"]
        assert scenario["goals"]
        assert scenario["persona"]["name"]
        assert scenario["difficulty"] in {"warm-up", "moderate", "hard"}


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
    assert "Go on - I'm listening." not in body["reply"]


def test_chat_new_leadership_scenario_has_canned_reply():
    res = client.post(
        "/api/chat",
        json={
            "scenarioId": "disagree-up",
            "message": "I hear the board pressure. I'd like us to keep a thin reliability slice — last quarter we had 3 Sev-1s. Can we scope both?",
            "history": [],
        },
    )
    assert res.status_code == 200
    assert "Go on - I'm listening." not in res.json()["reply"]


def test_chat_validates_missing_message():
    res = client.post("/api/chat", json={"scenarioId": "ask-for-raise"})
    assert res.status_code == 400
    assert res.json()["error"] == "message is required"


def test_chat_unknown_scenario():
    res = client.post("/api/chat", json={"scenarioId": "nope", "message": "hi there"})
    assert res.status_code == 404


def test_root_serves_html_home():
    res = client.get("/")
    assert res.status_code == 200
    assert "text/html" in res.headers["content-type"]
    body = res.text
    assert "HardTalkAI" in body
    assert "Coach Heather" in body
    assert 'href="/privacy"' in body
    assert "Not Found" not in body


def test_privacy_policy_html():
    for path in ("/privacy", "/privacy.html"):
        res = client.get(path)
        assert res.status_code == 200, path
        assert "text/html" in res.headers["content-type"]
        body = res.text.lower()
        assert "hardtalkai" in body
        assert "hello@hardtalk.ai" in body
        assert "aashish2k2@gmail.com" in body
        assert "openai" in body
        assert "do not sell" in body
        assert "prototype" in body
        assert 'href="/"' in res.text
        assert "← HardTalkAI" in res.text


def test_privacy_back_link_reaches_html_home_not_json_404():
    html = (REPO_ROOT / "docs" / "privacy-policy.html").read_text(encoding="utf-8")
    assert 'class="back"' in html
    assert '<a href="/">← HardTalkAI</a>' in html
    res = client.get("/")
    assert res.status_code == 200
    assert "text/html" in res.headers["content-type"]
    assert res.headers["content-type"].startswith("text/html")
    assert "HardTalkAI" in res.text
    assert res.text.strip()[:1] != "{"


def test_play_docs_exist_and_stay_honest():
    md = (REPO_ROOT / "docs" / "privacy-policy.md").read_text(encoding="utf-8")
    html = (REPO_ROOT / "docs" / "privacy-policy.html").read_text(encoding="utf-8")
    listing = (REPO_ROOT / "docs" / "play-store-listing.md").read_text(encoding="utf-8")
    checklist = (REPO_ROOT / "docs" / "play-console-checklist.md").read_text(encoding="utf-8")
    for text in (md, html):
        assert "hello@hardtalk.ai" in text
        assert "aashish2k2@gmail.com" in text
        assert "OpenAI" in text
        assert "prototype" in text.lower()
        assert "do not sell" in text.lower()
        assert "RevenueCat" in text or "do not take payments" in text.lower()
    assert "not legal advice" in md.lower()
    short = "Practice manager talks. Coach Heather scores clarity, empathy & assertiveness."
    assert short in listing
    assert len(short) <= 80
    assert "Free practice" in listing or "Free badge" in listing
    assert "Round complete" in listing
    assert "bundleRelease" in checklist
    assert "RevenueCat" in checklist
    assert "Play credentials" in checklist or "credentials" in checklist.lower()
