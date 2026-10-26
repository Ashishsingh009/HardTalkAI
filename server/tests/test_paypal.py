import json

import pytest
from app.models import InvoiceTerms
from app.paypal import (
    PayPalClient,
    PayPalError,
    mock_send_response,
    paypal_mode,
    sandbox_api_base,
)

TERMS = InvoiceTerms(
    amount="2400.00",
    currency="USD",
    dueDate="2026-11-01",
    recipientEmail="casey@northwind.example",
    recipientName="Casey Quinn",
    description="Brand site redesign",
)


def test_mock_mode_when_credentials_missing(monkeypatch):
    monkeypatch.delenv("PAYPAL_CLIENT_ID", raising=False)
    monkeypatch.delenv("PAYPAL_CLIENT_SECRET", raising=False)
    assert paypal_mode() == "mock"
    client = PayPalClient(client_id="", client_secret="")
    result = client.create_and_send_invoice(TERMS)
    assert result.mode == "mock"
    assert result.status == "SENT"
    assert result.invoiceId.startswith("INV2-MOCK-")
    assert "sandbox.paypal.com" in result.payerLink
    assert result.invoiceId in result.payerLink


def test_sandbox_api_base_refuses_live(monkeypatch):
    monkeypatch.setenv("PAYPAL_API_BASE", "https://api-m.paypal.com")
    assert sandbox_api_base() == "https://api-m.sandbox.paypal.com"
    monkeypatch.setenv("PAYPAL_API_BASE", "https://api-m.sandbox.paypal.com")
    assert sandbox_api_base() == "https://api-m.sandbox.paypal.com"


def test_client_refuses_live_base_url():
    with pytest.raises(PayPalError, match="sandbox"):
        PayPalClient(
            client_id="id",
            client_secret="secret",
            api_base="https://api-m.paypal.com",
        )


def test_sandbox_create_and_send_uses_invoicing_v2(monkeypatch):
    monkeypatch.setenv("PAYPAL_CLIENT_ID", "sandbox-id")
    monkeypatch.setenv("PAYPAL_CLIENT_SECRET", "sandbox-secret")
    calls: list[tuple[str, str, dict, bytes | None]] = []

    def fake_http(method, url, headers, body, timeout):
        calls.append((method, url, headers, body))
        if url.endswith("/v1/oauth2/token"):
            assert method == "POST"
            assert "Basic " in headers["Authorization"]
            assert b"grant_type=client_credentials" in body
            return 200, {"access_token": "tok_sandbox", "token_type": "Bearer"}
        if method == "POST" and url.endswith("/v2/invoicing/invoices"):
            payload = json.loads(body.decode())
            assert payload["detail"]["currency_code"] == "USD"
            assert payload["items"][0]["unit_amount"]["value"] == "2400.00"
            assert payload["primary_recipients"][0]["billing_info"]["email_address"] == (
                "casey@northwind.example"
            )
            assert payload["items"][0]["name"] == "Brand site redesign"
            assert payload["detail"]["payment_term"]["due_date"] == "2026-11-01"
            assert "Bearer tok_sandbox" in headers["Authorization"]
            return 201, {
                "id": "INV2-TEST-CASEY",
                "status": "DRAFT",
                "links": [
                    {
                        "rel": "self",
                        "href": "https://api-m.sandbox.paypal.com/v2/invoicing/invoices/INV2-TEST-CASEY",
                    }
                ],
            }
        if method == "POST" and url.endswith("/INV2-TEST-CASEY/send"):
            send_body = json.loads(body.decode())
            assert send_body["send_to_recipient"] is True
            return 200, {
                "href": "https://www.sandbox.paypal.com/invoice/p/#INV2-TEST-CASEY",
            }
        if method == "GET" and url.endswith("/INV2-TEST-CASEY"):
            return 200, {
                "id": "INV2-TEST-CASEY",
                "status": "SENT",
                "detail": {
                    "metadata": {
                        "recipient_view_url": (
                            "https://www.sandbox.paypal.com/invoice/payerView/details/INV2-TEST-CASEY"
                        )
                    }
                },
            }
        raise AssertionError(f"unexpected {method} {url}")

    client = PayPalClient(
        http=fake_http,
        client_id="sandbox-id",
        client_secret="sandbox-secret",
        api_base="https://api-m.sandbox.paypal.com",
    )
    result = client.create_and_send_invoice(TERMS)
    assert result.mode == "sandbox"
    assert result.invoiceId == "INV2-TEST-CASEY"
    assert result.status == "SENT"
    assert result.payerLink == "https://www.sandbox.paypal.com/invoice/p/#INV2-TEST-CASEY"
    assert not any("api-m.paypal.com" in url and "sandbox" not in url for _, url, _, _ in calls)
    assert len(calls) == 4


def test_oauth_failure_is_paypal_error():
    def fake_http(method, url, headers, body, timeout):
        return 401, {"error": "invalid_client", "error_description": "Client Authentication failed"}

    client = PayPalClient(
        http=fake_http,
        client_id="bad",
        client_secret="bad",
        api_base="https://api-m.sandbox.paypal.com",
    )
    with pytest.raises(PayPalError, match="OAuth"):
        client.create_and_send_invoice(TERMS)


def test_mock_send_response_is_labeled():
    result = mock_send_response(TERMS)
    assert result.mode == "mock"
    assert "MOCK" in result.invoiceId
