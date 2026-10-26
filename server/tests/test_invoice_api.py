from fastapi.testclient import TestClient

from app.main import app
from app.scenarios import INVOICE_SCENARIO_ID

client = TestClient(app)

COMPLETE_TURNS = [
    {
        "role": "counterpart",
        "content": "Can we push the $2,400 to next quarter? Don't send PayPal yet.",
    },
    {
        "role": "user",
        "content": (
            "I want to keep the relationship. $2,400 USD is overdue. "
            "Can we lock 2026-11-01 if I invoice casey@northwind.example "
            "for the brand site redesign?"
        ),
    },
    {
        "role": "counterpart",
        "content": "Yes — $2,400 USD due 2026-11-01 at casey@northwind.example.",
    },
]


def test_extract_rejects_non_invoice_scenario():
    res = client.post(
        "/api/invoice/extract",
        json={"scenarioId": "ask-for-raise", "turns": COMPLETE_TURNS},
    )
    assert res.status_code == 400
    assert "overdue invoice" in res.json()["error"].lower()


def test_extract_unknown_scenario():
    res = client.post(
        "/api/invoice/extract",
        json={"scenarioId": "nope", "turns": COMPLETE_TURNS},
    )
    assert res.status_code == 404


def test_extract_returns_terms_without_sending_to_paypal(monkeypatch):
    monkeypatch.setattr(
        "app.paypal.create_and_send_invoice",
        lambda *_a, **_k: (_ for _ in ()).throw(AssertionError("must not send")),
    )
    res = client.post(
        "/api/invoice/extract",
        json={"scenarioId": INVOICE_SCENARIO_ID, "turns": COMPLETE_TURNS},
    )
    assert res.status_code == 200
    body = res.json()
    assert body["terms"]["amount"] == "2400.00"
    assert body["terms"]["currency"] == "USD"
    assert body["terms"]["recipientEmail"] == "casey@northwind.example"
    assert body["missing"] == []
    assert body["source"] in {"heuristic", "ai"}


def test_send_requires_reviewed_fields(monkeypatch):
    monkeypatch.delenv("PAYPAL_CLIENT_ID", raising=False)
    monkeypatch.delenv("PAYPAL_CLIENT_SECRET", raising=False)
    res = client.post(
        "/api/invoice/send",
        json={"scenarioId": INVOICE_SCENARIO_ID, "amount": "2400.00"},
    )
    assert res.status_code == 400
    assert "Missing invoice fields" in res.json()["error"]


def test_send_mock_mode_returns_labeled_invoice(monkeypatch):
    monkeypatch.delenv("PAYPAL_CLIENT_ID", raising=False)
    monkeypatch.delenv("PAYPAL_CLIENT_SECRET", raising=False)
    res = client.post(
        "/api/invoice/send",
        json={
            "scenarioId": INVOICE_SCENARIO_ID,
            "amount": "2400.00",
            "currency": "USD",
            "dueDate": "2026-11-01",
            "recipientEmail": "casey@northwind.example",
            "recipientName": "Casey Quinn",
            "description": "Brand site redesign",
        },
    )
    assert res.status_code == 200
    body = res.json()
    assert body["mode"] == "mock"
    assert body["status"] == "SENT"
    assert body["invoiceId"].startswith("INV2-MOCK-")
    assert "sandbox.paypal.com" in body["payerLink"]


def test_send_sandbox_uses_injected_client(monkeypatch):
    from app.models import InvoiceSendResponse

    monkeypatch.setenv("PAYPAL_CLIENT_ID", "id")
    monkeypatch.setenv("PAYPAL_CLIENT_SECRET", "secret")

    def fake_send(terms):
        assert terms.amount == "2400.00"
        assert terms.recipientEmail == "casey@northwind.example"
        return InvoiceSendResponse(
            invoiceId="INV2-LIVE-TEST",
            status="SENT",
            payerLink="https://www.sandbox.paypal.com/invoice/p/#INV2-LIVE-TEST",
            mode="sandbox",
        )

    monkeypatch.setattr("app.paypal.create_and_send_invoice", fake_send)
    res = client.post(
        "/api/invoice/send",
        json={
            "scenarioId": INVOICE_SCENARIO_ID,
            "amount": "2400.00",
            "currency": "USD",
            "recipientEmail": "casey@northwind.example",
            "description": "Brand site redesign",
        },
    )
    assert res.status_code == 200
    body = res.json()
    assert body["mode"] == "sandbox"
    assert body["invoiceId"] == "INV2-LIVE-TEST"


def test_health_reports_paypal_mock_without_credentials(monkeypatch):
    monkeypatch.delenv("PAYPAL_CLIENT_ID", raising=False)
    monkeypatch.delenv("PAYPAL_CLIENT_SECRET", raising=False)
    res = client.get("/api/health")
    assert res.status_code == 200
    assert res.json()["paypal"] == "mock"


def test_chat_still_scores_the_invoice_scenario():
    res = client.post(
        "/api/chat",
        json={
            "scenarioId": INVOICE_SCENARIO_ID,
            "message": (
                "I appreciate the relationship. The $2,400 brand site is 18 days "
                "overdue. Can we lock a pay-by date this month?"
            ),
            "history": [],
        },
    )
    assert res.status_code == 200
    assert "Go on - I'm listening." not in res.json()["reply"]
    assert "feedback" in res.json()
