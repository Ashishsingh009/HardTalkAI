"""Extract agreed PayPal invoice terms from a practice transcript.

When ``OPENAI_API_KEY`` is set, an LLM fills the fields; otherwise (and on any
AI failure) a heuristic parser runs. Missing fields are listed so the client
can ask the user to edit them before anything is sent to PayPal.
"""

from __future__ import annotations

import json
import logging
import os
import re
from datetime import date, timedelta

from . import ai
from .models import ChatTurn, InvoiceExtractResponse, InvoiceTerms, Scenario

logger = logging.getLogger("hardtalkai.invoice")

REQUIRED_FIELDS = ("amount", "currency", "recipientEmail", "description")
OPTIONAL_FIELDS = ("dueDate", "recipientName")
ALL_FIELDS = REQUIRED_FIELDS + OPTIONAL_FIELDS

_AMOUNT_RE = re.compile(
    r"(?:"
    r"(?:USD|US\$|\$)\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)"
    r"|"
    r"\b([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)\s*(?:USD|dollars?)\b"
    r")",
    re.IGNORECASE,
)
_EMAIL_RE = re.compile(r"\b[A-Z0-9._%+\-]+@[A-Z0-9.\-]+\.[A-Z]{2,}\b", re.IGNORECASE)
_ISO_DATE_RE = re.compile(r"\b(20\d{2}-\d{2}-\d{2})\b")
_CURRENCY_RE = re.compile(r"\b(USD|EUR|GBP|CAD|AUD)\b", re.IGNORECASE)
_WORK_RE = re.compile(
    r"\b(brand site redesign|brand site|website redesign|logo (?:design|refresh)|"
    r"retainer|consulting|freelance (?:work|invoice))\b",
    re.IGNORECASE,
)

_MONTHS = {
    "january": 1,
    "jan": 1,
    "february": 2,
    "feb": 2,
    "march": 3,
    "mar": 3,
    "april": 4,
    "apr": 4,
    "may": 5,
    "june": 6,
    "jun": 6,
    "july": 7,
    "jul": 7,
    "august": 8,
    "aug": 8,
    "september": 9,
    "sep": 9,
    "sept": 9,
    "october": 10,
    "oct": 10,
    "november": 11,
    "nov": 11,
    "december": 12,
    "dec": 12,
}
_NAMED_DATE_RE = re.compile(
    r"\b(?:"
    r"(?P<m1>jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|"
    r"jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|"
    r"dec(?:ember)?)\s+(?P<d1>\d{1,2})(?:st|nd|rd|th)?(?:,?\s+(?P<y1>20\d{2}))?"
    r"|"
    r"(?P<d2>\d{1,2})(?:st|nd|rd|th)?\s+(?P<m2>jan(?:uary)?|feb(?:ruary)?|"
    r"mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|"
    r"sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)"
    r"(?:,?\s+(?P<y2>20\d{2}))?"
    r")\b",
    re.IGNORECASE,
)
_NET_DAYS_RE = re.compile(r"\bnet\s+(\d{1,2})\b", re.IGNORECASE)

_EXTRACT_SYSTEM = (
    "You extract invoice terms the parties actually agreed (or the client "
    "accepted) in a practice conversation. Return JSON only with keys: "
    "amount (decimal string like \"2400.00\" or null), currency (ISO 4217 or null), "
    "dueDate (YYYY-MM-DD or null), recipientEmail, recipientName, description. "
    "Do not invent an email or amount that is not in the transcript. "
    "If the client named a pay-by date, use it. Short description, one line."
)


def missing_fields(terms: InvoiceTerms) -> list[str]:
    missing: list[str] = []
    mapping = terms.model_dump()
    for field in REQUIRED_FIELDS:
        value = mapping.get(field)
        if value is None or not str(value).strip():
            missing.append(field)
    return missing


def normalize_amount(raw: str | None) -> str | None:
    if not raw:
        return None
    cleaned = re.sub(r"[^\d.]", "", str(raw).replace(",", ""))
    if not cleaned or cleaned.count(".") > 1:
        return None
    try:
        value = float(cleaned)
    except ValueError:
        return None
    if value <= 0 or value > 1_000_000:
        return None
    return f"{value:.2f}"


def normalize_currency(raw: str | None) -> str | None:
    if not raw:
        return None
    code = str(raw).strip().upper()
    if len(code) == 3 and code.isalpha():
        return code
    return None


def normalize_email(raw: str | None) -> str | None:
    if not raw:
        return None
    match = _EMAIL_RE.search(str(raw).strip())
    return match.group(0).lower() if match else None


def normalize_due_date(raw: str | None, today: date | None = None) -> str | None:
    if not raw:
        return None
    text = str(raw).strip()
    iso = _ISO_DATE_RE.search(text)
    if iso:
        try:
            parsed = date.fromisoformat(iso.group(1))
        except ValueError:
            parsed = None
        if parsed is not None:
            return parsed.isoformat()
    named = _parse_named_date(text, today or date.today())
    return named


def _parse_named_date(text: str, today: date) -> str | None:
    match = _NAMED_DATE_RE.search(text)
    if not match:
        return None
    month_token = match.group("m1") or match.group("m2")
    day_token = match.group("d1") or match.group("d2")
    year_token = match.group("y1") or match.group("y2")
    if not month_token or not day_token:
        return None
    month = _MONTHS.get(month_token.lower())
    if month is None:
        return None
    year = int(year_token) if year_token else today.year
    try:
        parsed = date(year, month, int(day_token))
    except ValueError:
        return None
    if year_token is None and parsed < today - timedelta(days=30):
        try:
            parsed = date(today.year + 1, month, int(day_token))
        except ValueError:
            return None
    return parsed.isoformat()


def normalize_terms(raw: InvoiceTerms | dict, today: date | None = None) -> InvoiceTerms:
    data = raw.model_dump() if isinstance(raw, InvoiceTerms) else dict(raw)
    amount = normalize_amount(data.get("amount"))
    currency = normalize_currency(data.get("currency"))
    if amount and not currency:
        currency = "USD"
    description = (data.get("description") or "").strip() or None
    if description and len(description) > 120:
        description = description[:117].rstrip() + "…"
    name = (data.get("recipientName") or "").strip() or None
    return InvoiceTerms(
        amount=amount,
        currency=currency,
        dueDate=normalize_due_date(data.get("dueDate"), today),
        recipientEmail=normalize_email(data.get("recipientEmail")),
        recipientName=name,
        description=description,
    )


def _transcript_text(turns: list[ChatTurn]) -> str:
    return "\n".join(f"{turn.role}: {turn.content}" for turn in turns if turn.content)


def extract_heuristic(
    turns: list[ChatTurn],
    scenario: Scenario | None = None,
    today: date | None = None,
) -> InvoiceTerms:
    """Regex/keyword extraction. Safe to run in tests with no API key."""
    today = today or date.today()
    text = _transcript_text(turns)
    amount = None
    amount_match = _AMOUNT_RE.search(text)
    if amount_match:
        amount = normalize_amount(amount_match.group(1) or amount_match.group(2))

    currency = None
    currency_match = _CURRENCY_RE.search(text)
    if currency_match:
        currency = normalize_currency(currency_match.group(1))
    elif "$" in text:
        currency = "USD"
    if amount and not currency:
        currency = "USD"

    email_match = _EMAIL_RE.search(text)
    email = email_match.group(0).lower() if email_match else None

    due = normalize_due_date(text, today)
    if due is None:
        net = _NET_DAYS_RE.search(text)
        if net:
            due = (today + timedelta(days=int(net.group(1)))).isoformat()

    work_matches = list(_WORK_RE.finditer(text))
    description = None
    if work_matches:
        # Prefer the longest phrase so "brand site redesign" wins over "brand site".
        description = max(work_matches, key=lambda m: len(m.group(0))).group(0).strip()
        description = description[0].upper() + description[1:]

    name = None
    if scenario is not None:
        name = scenario.persona.name

    return normalize_terms(
        {
            "amount": amount,
            "currency": currency,
            "dueDate": due,
            "recipientEmail": email,
            "recipientName": name,
            "description": description,
        },
        today=today,
    )


def merge_terms(primary: InvoiceTerms, fallback: InvoiceTerms) -> InvoiceTerms:
    """Fill empty primary fields from fallback (AI first, heuristic backup)."""
    merged = {}
    a = primary.model_dump()
    b = fallback.model_dump()
    for field in ALL_FIELDS:
        merged[field] = a.get(field) or b.get(field)
    return InvoiceTerms(**merged)


def parse_model_terms(payload: dict, today: date | None = None) -> InvoiceTerms:
    """Validate a model JSON object into InvoiceTerms. Used by tests and AI."""
    return normalize_terms(
        {
            "amount": payload.get("amount"),
            "currency": payload.get("currency"),
            "dueDate": payload.get("dueDate") or payload.get("due_date"),
            "recipientEmail": payload.get("recipientEmail") or payload.get("recipient_email"),
            "recipientName": payload.get("recipientName") or payload.get("recipient_name"),
            "description": payload.get("description"),
        },
        today=today,
    )


def _extract_with_openai(turns: list[ChatTurn], scenario: Scenario) -> InvoiceTerms | None:
    from openai import OpenAI

    client = OpenAI()
    model = os.getenv("OPENAI_MODEL", ai.DEFAULT_MODEL)
    user_blob = json.dumps(
        {
            "scenario": scenario.title,
            "persona": scenario.persona.name,
            "turns": [{"role": t.role, "content": t.content} for t in turns],
        },
        ensure_ascii=False,
    )
    response = client.chat.completions.create(
        model=model,
        messages=[
            {"role": "system", "content": _EXTRACT_SYSTEM},
            {"role": "user", "content": user_blob},
        ],
        temperature=0,
        max_tokens=300,
        response_format={"type": "json_object"},
    )
    content = (response.choices[0].message.content or "").strip()
    if not content:
        return None
    payload = json.loads(content)
    if not isinstance(payload, dict):
        return None
    return parse_model_terms(payload)


def extract_terms(
    scenario: Scenario,
    turns: list[ChatTurn],
    today: date | None = None,
) -> InvoiceExtractResponse:
    heuristic = extract_heuristic(turns, scenario, today=today)
    source: str = "heuristic"
    terms = heuristic
    if ai.is_ai_enabled():
        try:
            ai_terms = _extract_with_openai(turns, scenario)
        except Exception as exc:  # noqa: BLE001 — fall back; never block the recap
            logger.warning("Invoice term extraction via OpenAI failed: %s", exc)
            ai_terms = None
        if ai_terms is not None:
            terms = merge_terms(ai_terms, heuristic)
            source = "ai"
    terms = normalize_terms(terms, today=today)
    return InvoiceExtractResponse(
        terms=terms,
        missing=missing_fields(terms),
        source=source,  # type: ignore[arg-type]
    )


def terms_from_send_request(body) -> InvoiceTerms:
    return normalize_terms(
        {
            "amount": body.amount,
            "currency": body.currency,
            "dueDate": body.dueDate,
            "recipientEmail": body.recipientEmail,
            "recipientName": body.recipientName,
            "description": body.description,
        }
    )
