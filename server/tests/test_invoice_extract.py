from datetime import date

from app.invoice_extract import (
    extract_heuristic,
    extract_terms,
    merge_terms,
    missing_fields,
    parse_model_terms,
)
from app.models import ChatTurn, InvoiceTerms
from app.scenarios import INVOICE_SCENARIO_ID, find_scenario

TODAY = date(2026, 10, 16)

COMPLETE_TURNS = [
    ChatTurn(
        role="counterpart",
        content=(
            "Hey — I know the brand site has been live. Cash is tight this month. "
            "Can we push the $2,400 to next quarter?"
        ),
    ),
    ChatTurn(
        role="user",
        content=(
            "I hear cash is tight, and I want to keep working together. "
            "The brand site redesign has been live for three weeks and $2,400 USD "
            "is 18 days overdue. Can we lock November 1 and I'll send the invoice "
            "to casey@northwind.example?"
        ),
    ),
    ChatTurn(
        role="counterpart",
        content=(
            "That's fair — $2,400 USD was the number. I can do 2026-11-01 if you "
            "send the invoice to casey@northwind.example. One line: brand site redesign."
        ),
    ),
]


def test_heuristic_extracts_amount_email_date_and_description():
    scenario = find_scenario(INVOICE_SCENARIO_ID)
    terms = extract_heuristic(COMPLETE_TURNS, scenario, today=TODAY)
    assert terms.amount == "2400.00"
    assert terms.currency == "USD"
    assert terms.dueDate == "2026-11-01"
    assert terms.recipientEmail == "casey@northwind.example"
    assert terms.recipientName == "Casey Quinn"
    assert terms.description is not None
    assert "brand" in terms.description.lower()
    assert missing_fields(terms) == []


def test_heuristic_reports_missing_when_transcript_has_no_terms():
    turns = [
        ChatTurn(role="counterpart", content="Can we talk later?"),
        ChatTurn(role="user", content="I would like to get paid sometime."),
    ]
    terms = extract_heuristic(turns, find_scenario(INVOICE_SCENARIO_ID), today=TODAY)
    assert terms.amount is None
    assert terms.recipientEmail is None
    missing = missing_fields(terms)
    assert "amount" in missing
    assert "recipientEmail" in missing
    assert "description" in missing


def test_named_month_date_and_dollar_sign_imply_usd():
    turns = [
        ChatTurn(
            role="user",
            content="Please pay $1,800 by Nov 15 for the logo refresh.",
        )
    ]
    terms = extract_heuristic(turns, today=TODAY)
    assert terms.amount == "1800.00"
    assert terms.currency == "USD"
    assert terms.dueDate == "2026-11-15"
    assert "logo" in (terms.description or "").lower()


def test_net_days_becomes_an_iso_due_date():
    turns = [ChatTurn(role="counterpart", content="Fine — net 10 on 500 USD.")]
    terms = extract_heuristic(turns, today=TODAY)
    assert terms.amount == "500.00"
    assert terms.dueDate == "2026-10-26"


def test_parse_model_terms_accepts_snake_or_camel_and_strips_junk():
    terms = parse_model_terms(
        {
            "amount": "$2,400.5",
            "currency": "usd",
            "due_date": "November 1, 2026",
            "recipient_email": "Casey@Northwind.example",
            "recipient_name": "Casey Quinn",
            "description": "Brand site redesign",
        },
        today=TODAY,
    )
    assert terms.amount == "2400.50"
    assert terms.currency == "USD"
    assert terms.dueDate == "2026-11-01"
    assert terms.recipientEmail == "casey@northwind.example"


def test_merge_prefers_ai_then_fills_from_heuristic():
    ai_terms = InvoiceTerms(amount="2400.00", recipientEmail=None, description="Brand site")
    heuristic = InvoiceTerms(
        amount="999.00",
        currency="USD",
        recipientEmail="casey@northwind.example",
        description="ignored",
    )
    merged = merge_terms(ai_terms, heuristic)
    assert merged.amount == "2400.00"
    assert merged.currency == "USD"
    assert merged.recipientEmail == "casey@northwind.example"
    assert merged.description == "Brand site"


def test_extract_terms_uses_heuristic_without_openai(monkeypatch):
    monkeypatch.delenv("OPENAI_API_KEY", raising=False)
    scenario = find_scenario(INVOICE_SCENARIO_ID)
    result = extract_terms(scenario, COMPLETE_TURNS, today=TODAY)
    assert result.source == "heuristic"
    assert result.terms.amount == "2400.00"
    assert result.missing == []
