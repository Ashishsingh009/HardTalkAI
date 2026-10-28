from fastapi.testclient import TestClient

from app.main import app
from app.scenarios import INVOICE_SCENARIO_ID

client = TestClient(app)

HEADERS = {"X-HardTalk-Session": "session-demo-1"}
INVOICE_SEND = {
    "scenarioId": INVOICE_SCENARIO_ID,
    "amount": "2400.00",
    "currency": "USD",
    "dueDate": "2026-11-01",
    "recipientEmail": "casey@northwind.example",
    "recipientName": "Casey Quinn",
    "description": "Brand site redesign",
}


def _chat(session: str, scenario: str, turn: int) -> None:
    history = []
    for index in range(turn - 1):
        history.append({"role": "user", "content": f"Turn {index + 1} with a clear ask."})
        history.append({"role": "counterpart", "content": "Walk me through it."})
    res = client.post(
        "/api/chat",
        headers={"X-HardTalk-Session": session},
        json={
            "scenarioId": scenario,
            "message": f"Turn {turn}: I'd like a concrete next step on this.",
            "history": history,
        },
    )
    assert res.status_code == 200, res.text


def test_unknown_event_is_rejected_and_message_text_is_dropped():
    res = client.post(
        "/api/analytics/event",
        headers=HEADERS,
        json={"event": "hack", "message": "secret transcript", "scenarioId": "ask-for-raise"},
    )
    assert res.status_code == 400
    funnel = client.get("/api/analytics/funnel").json()
    assert funnel["eventCount"] == 0
    assert "secret transcript" not in str(funnel)


def test_practice_and_invoice_funnel_counts_unique_sessions():
    client.get("/api/scenarios", headers=HEADERS)
    client.post(
        "/api/analytics/event",
        headers=HEADERS,
        json={"event": "scenario_open", "scenarioId": INVOICE_SCENARIO_ID},
    )
    for turn in range(1, 4):
        _chat("session-demo-1", INVOICE_SCENARIO_ID, turn)
    extract = client.post(
        "/api/invoice/extract",
        headers=HEADERS,
        json={
            "scenarioId": INVOICE_SCENARIO_ID,
            "turns": [
                {
                    "role": "user",
                    "content": (
                        "$2,400 USD by 2026-11-01 for casey@northwind.example "
                        "brand site redesign"
                    ),
                }
            ],
        },
    )
    assert extract.status_code == 200
    sent = client.post("/api/invoice/send", headers=HEADERS, json=INVOICE_SEND)
    assert sent.status_code == 200
    assert sent.json()["mode"] == "mock"

    other = {"X-HardTalk-Session": "session-raise-only"}
    client.get("/api/scenarios", headers=other)
    _chat("session-raise-only", "ask-for-raise", 1)

    funnel = client.get("/api/analytics/funnel").json()
    assert funnel["sessions"] == 2
    practice = {row["id"]: row for row in funnel["practice"]}
    invoice = {row["id"]: row for row in funnel["invoice"]}
    assert practice["catalog"]["sessions"] == 2
    assert practice["started"]["sessions"] == 2
    assert practice["completed"]["sessions"] == 1
    assert invoice["started"]["sessions"] == 1
    assert invoice["completed"]["sessions"] == 1
    assert invoice["invoice_review"]["sessions"] == 1
    assert invoice["invoice_send"]["sessions"] == 1
    assert invoice["invoice_send"]["conversionFromPrevious"] == 1.0
    assert funnel["invoiceSend"]["mock"] == 1
    assert funnel["invoiceSend"]["sandbox"] == 0
    assert funnel["channels"]["typed"] == 1
    by_id = {row["id"]: row for row in funnel["byScenario"]}
    assert by_id[INVOICE_SCENARIO_ID]["completed"] == 1
    assert by_id["ask-for-raise"]["started"] == 1
    assert by_id["ask-for-raise"]["completed"] == 0


def test_analytics_dashboard_html_is_first_party():
    client.get("/api/scenarios", headers=HEADERS)
    res = client.get("/analytics")
    assert res.status_code == 200
    assert "text/html" in res.headers["content-type"]
    body = res.text
    assert "Funnel analytics" in body
    assert "No third-party SDK" in body
    assert "/api/analytics/funnel" in body
    assert "Saw catalog" in body
    assert "secret" not in body.lower() or "No third-party" in body
    assert 'href="/"' in body


def test_invalid_session_header_buckets_as_anon():
    client.get("/api/scenarios", headers={"X-HardTalk-Session": "not a token!!!"})
    funnel = client.get("/api/analytics/funnel").json()
    assert funnel["sessions"] == 1
    assert funnel["practice"][0]["sessions"] == 1
