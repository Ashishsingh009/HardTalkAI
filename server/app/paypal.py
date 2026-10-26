"""PayPal Invoicing v2 (sandbox only) with a labeled mock fallback.

Live PayPal is refused even if someone points ``PAYPAL_API_BASE`` at production.
Credentials come from ``PAYPAL_CLIENT_ID`` / ``PAYPAL_CLIENT_SECRET`` and are
never logged. When they are missing, ``create_and_send_invoice`` still returns
an invoice id, status, and payer link so the Android flow and tests can run.
"""

from __future__ import annotations

import base64
import json
import logging
import os
import uuid
from datetime import date, timedelta
from typing import Any, Callable, Mapping
from urllib import error as urllib_error
from urllib import parse as urllib_parse
from urllib import request as urllib_request

from .models import InvoiceSendResponse, InvoiceTerms

logger = logging.getLogger("hardtalkai.paypal")

SANDBOX_API_BASE = "https://api-m.sandbox.paypal.com"
SANDBOX_PAYER_HOST = "https://www.sandbox.paypal.com"
LIVE_API_HINT = "api-m.paypal.com"

HttpFn = Callable[[str, str, dict[str, str], bytes | None, float], tuple[int, Any]]


class PayPalError(RuntimeError):
    """Sandbox invoice create/send failed."""


def credentials_configured() -> bool:
    return bool(
        os.getenv("PAYPAL_CLIENT_ID", "").strip()
        and os.getenv("PAYPAL_CLIENT_SECRET", "").strip()
    )


def paypal_mode() -> str:
    return "sandbox" if credentials_configured() else "mock"


def sandbox_api_base() -> str:
    """Always sandbox. Ignore a live override so this build cannot charge real money."""
    raw = os.getenv("PAYPAL_API_BASE", SANDBOX_API_BASE).strip().rstrip("/")
    if "sandbox" in raw.lower():
        return raw
    if raw:
        logger.warning("Refusing non-sandbox PAYPAL_API_BASE=%s; using sandbox", raw)
    return SANDBOX_API_BASE


def _default_http(
    method: str,
    url: str,
    headers: dict[str, str],
    body: bytes | None,
    timeout: float,
) -> tuple[int, Any]:
    request = urllib_request.Request(url, data=body, method=method, headers=headers)
    try:
        with urllib_request.urlopen(request, timeout=timeout) as response:
            raw = response.read().decode("utf-8")
            payload: Any = json.loads(raw) if raw.strip() else {}
            return response.status, payload
    except urllib_error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")
        try:
            payload = json.loads(detail) if detail.strip() else {"error": detail[:400]}
        except json.JSONDecodeError:
            payload = {"error": detail[:400]}
        return exc.code, payload
    except urllib_error.URLError as exc:
        raise PayPalError(f"PayPal sandbox unreachable: {exc}") from exc


def _error_detail(payload: Any) -> str:
    if isinstance(payload, dict):
        if payload.get("message"):
            details = payload.get("details") or []
            extra = ""
            if isinstance(details, list) and details:
                extra = " " + json.dumps(details[:3])
            return f"{payload.get('name', 'PAYPAL_ERROR')}: {payload['message']}{extra}"
        if payload.get("error"):
            return str(payload["error"])
        return json.dumps(payload)[:400]
    return str(payload)[:400]


def _split_name(name: str | None) -> dict[str, str]:
    parts = (name or "Client").strip().split()
    if not parts:
        return {"given_name": "Client"}
    if len(parts) == 1:
        return {"given_name": parts[0][:140]}
    return {
        "given_name": parts[0][:140],
        "surname": " ".join(parts[1:])[:140],
    }


def _due_date_for_paypal(raw: str | None, today: date | None = None) -> str:
    today = today or date.today()
    if not raw:
        return (today + timedelta(days=7)).isoformat()
    try:
        parsed = date.fromisoformat(raw[:10])
    except ValueError:
        return (today + timedelta(days=7)).isoformat()
    if parsed < today:
        # PayPal often rejects a due date in the past; keep the promise in the note.
        return today.isoformat()
    return parsed.isoformat()


def mock_send_response(terms: InvoiceTerms) -> InvoiceSendResponse:
    invoice_id = f"INV2-MOCK-{uuid.uuid4().hex[:12].upper()}"
    return InvoiceSendResponse(
        invoiceId=invoice_id,
        status="SENT",
        payerLink=f"{SANDBOX_PAYER_HOST}/invoice/payer/v1/{invoice_id}",
        mode="mock",
    )


class PayPalClient:
    def __init__(
        self,
        *,
        http: HttpFn | None = None,
        client_id: str | None = None,
        client_secret: str | None = None,
        api_base: str | None = None,
        timeout: float = 20,
    ) -> None:
        self._http = http or _default_http
        self._client_id = (client_id if client_id is not None else os.getenv("PAYPAL_CLIENT_ID", "")).strip()
        self._client_secret = (
            client_secret if client_secret is not None else os.getenv("PAYPAL_CLIENT_SECRET", "")
        ).strip()
        self._api_base = (api_base or sandbox_api_base()).rstrip("/")
        self._timeout = timeout
        if LIVE_API_HINT in self._api_base and "sandbox" not in self._api_base:
            raise PayPalError("Live PayPal is disabled; sandbox only")

    @property
    def configured(self) -> bool:
        return bool(self._client_id and self._client_secret)

    def create_and_send_invoice(
        self,
        terms: InvoiceTerms,
        *,
        today: date | None = None,
    ) -> InvoiceSendResponse:
        if not self.configured:
            logger.info("PayPal credentials missing; returning mock invoice")
            return mock_send_response(terms)
        token = self._oauth_token()
        invoice_id = self._create_draft(token, terms, today=today)
        payer_link = self._send_invoice(token, invoice_id)
        status, href = self._fetch_status_and_link(token, invoice_id)
        return InvoiceSendResponse(
            invoiceId=invoice_id,
            status=status or "SENT",
            payerLink=payer_link or href or f"{SANDBOX_PAYER_HOST}/invoice/payer/v1/{invoice_id}",
            mode="sandbox",
        )

    def _request(
        self,
        method: str,
        path: str,
        *,
        headers: Mapping[str, str],
        body: bytes | None = None,
    ) -> tuple[int, Any]:
        url = path if path.startswith("http") else f"{self._api_base}{path}"
        status, payload = self._http(method, url, dict(headers), body, self._timeout)
        return status, payload

    def _oauth_token(self) -> str:
        basic = base64.b64encode(f"{self._client_id}:{self._client_secret}".encode()).decode()
        status, payload = self._request(
            "POST",
            "/v1/oauth2/token",
            headers={
                "Authorization": f"Basic {basic}",
                "Content-Type": "application/x-www-form-urlencoded",
                "Accept": "application/json",
            },
            body=urllib_parse.urlencode({"grant_type": "client_credentials"}).encode(),
        )
        if status >= 400 or not isinstance(payload, dict) or not payload.get("access_token"):
            raise PayPalError(f"PayPal OAuth failed ({status}): {_error_detail(payload)}")
        return str(payload["access_token"])

    def _auth_headers(self, token: str, extra: Mapping[str, str] | None = None) -> dict[str, str]:
        headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "Accept": "application/json",
        }
        if extra:
            headers.update(extra)
        return headers

    def _create_draft(
        self,
        token: str,
        terms: InvoiceTerms,
        *,
        today: date | None = None,
    ) -> str:
        today = today or date.today()
        due = _due_date_for_paypal(terms.dueDate, today)
        note_bits = [
            "Agreed during a HardTalk practice conversation (PayPal sandbox).",
        ]
        if terms.dueDate and terms.dueDate != due:
            note_bits.append(f"Originally agreed due date: {terms.dueDate}.")
        payload = {
            "detail": {
                "currency_code": terms.currency or "USD",
                "note": " ".join(note_bits),
                "payment_term": {
                    "term_type": "DUE_ON_DATE",
                    "due_date": due,
                },
            },
            "primary_recipients": [
                {
                    "billing_info": {
                        "email_address": terms.recipientEmail,
                        "name": _split_name(terms.recipientName),
                    }
                }
            ],
            "items": [
                {
                    "name": (terms.description or "Freelance work")[:120],
                    "quantity": "1",
                    "unit_amount": {
                        "currency_code": terms.currency or "USD",
                        "value": terms.amount,
                    },
                }
            ],
        }
        status, body = self._request(
            "POST",
            "/v2/invoicing/invoices",
            headers=self._auth_headers(token, {"Prefer": "return=representation"}),
            body=json.dumps(payload).encode(),
        )
        if status >= 400 or not isinstance(body, dict):
            raise PayPalError(f"Create invoice failed ({status}): {_error_detail(body)}")
        invoice_id = body.get("id")
        if not invoice_id:
            href = _rel_href(body.get("links"), "self")
            invoice_id = href.rstrip("/").split("/")[-1] if href else None
        if not invoice_id:
            raise PayPalError("PayPal create invoice returned no id")
        return str(invoice_id)

    def _send_invoice(self, token: str, invoice_id: str) -> str | None:
        status, body = self._request(
            "POST",
            f"/v2/invoicing/invoices/{invoice_id}/send",
            headers=self._auth_headers(token),
            body=json.dumps(
                {
                    "send_to_recipient": True,
                    "send_to_invoicer": True,
                }
            ).encode(),
        )
        if status >= 400:
            raise PayPalError(f"Send invoice failed ({status}): {_error_detail(body)}")
        if isinstance(body, dict):
            href = body.get("href")
            if href:
                return str(href)
        return None

    def _fetch_status_and_link(self, token: str, invoice_id: str) -> tuple[str | None, str | None]:
        status, body = self._request(
            "GET",
            f"/v2/invoicing/invoices/{invoice_id}",
            headers=self._auth_headers(token),
        )
        if status >= 400 or not isinstance(body, dict):
            logger.warning("PayPal GET invoice failed (%s): %s", status, _error_detail(body))
            return None, None
        invoice_status = body.get("status")
        metadata = (body.get("detail") or {}).get("metadata") or {}
        href = (
            metadata.get("recipient_view_url")
            or _rel_href(body.get("links"), "payer-view")
            or _rel_href(body.get("links"), "self")
        )
        return (str(invoice_status) if invoice_status else None, str(href) if href else None)


def _rel_href(links: Any, rel: str) -> str:
    if not isinstance(links, list):
        return ""
    for link in links:
        if isinstance(link, dict) and link.get("rel") == rel and link.get("href"):
            return str(link["href"])
    return ""


def create_and_send_invoice(terms: InvoiceTerms) -> InvoiceSendResponse:
    return PayPalClient().create_and_send_invoice(terms)
