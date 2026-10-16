from __future__ import annotations

import logging
import os
from pathlib import Path

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse

from . import ai
from . import paypal
from . import voice
from .abuse import authorize_voice_session
from .coach import respond
from .invoice_extract import extract_terms, missing_fields, terms_from_send_request
from .models import (
    ChatRequest,
    HealthResponse,
    InvoiceExtractRequest,
    InvoiceSendRequest,
    ScenariosResponse,
    VoiceCompleteRequest,
    VoiceSessionRequest,
)
from .paypal import PayPalError
from .scenarios import INVOICE_SCENARIO_ID, SCENARIOS, find_scenario
from .voice import VoiceProviderError

# repo-root/docs/*.html (this file is server/app/main.py)
DOCS_DIR = Path(__file__).resolve().parents[2] / "docs"
HOME_HTML = DOCS_DIR / "index.html"
PRIVACY_HTML = DOCS_DIR / "privacy-policy.html"

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)-5s %(name)s - %(message)s")
logger = logging.getLogger("hardtalkai")

app = FastAPI(title="HardTalkAI", version="0.1.0")

# Native Android does not use browser CORS. The Vite demo proxies /api same-origin.
# Keep a tight allowlist so a random webpage cannot mint voice sessions.
DEFAULT_CORS_ORIGINS = (
    "http://localhost:5173",
    "http://127.0.0.1:5173",
    "http://localhost:3001",
    "http://127.0.0.1:3001",
)


def cors_allow_origins() -> list[str]:
    origins = list(DEFAULT_CORS_ORIGINS)
    extra = os.getenv("HARDTALK_CORS_ORIGINS", "")
    for part in extra.split(","):
        origin = part.strip()
        if origin and origin not in origins:
            origins.append(origin)
    return origins


app.add_middleware(
    CORSMiddleware,
    allow_origins=cors_allow_origins(),
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=[
        "Authorization",
        "Content-Type",
        "X-HardTalk-Client",
        "X-HardTalk-Voice-Token",
    ],
)


def _error(status: int, message: str) -> JSONResponse:
    return JSONResponse(status_code=status, content={"error": message})


def _html(path: Path, missing: str) -> FileResponse | JSONResponse:
    if not path.is_file():
        return _error(404, missing)
    return FileResponse(path, media_type="text/html; charset=utf-8")


@app.get("/", response_model=None)
def home() -> FileResponse | JSONResponse:
    """HTML home for the FastAPI host so privacy's ← HardTalkAI link is not a JSON 404."""
    return _html(HOME_HTML, "Home page is not available")


@app.get("/privacy", response_model=None)
@app.get("/privacy.html", response_model=None)
def privacy_policy() -> FileResponse | JSONResponse:
    return _html(PRIVACY_HTML, "Privacy policy is not available")


@app.get("/api/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(
        status="ok",
        engine=ai.engine_name(),
        scenarios=len(SCENARIOS),
        voice=voice.is_available(),
        paypal=paypal.paypal_mode(),  # type: ignore[arg-type]
    )


@app.get("/api/scenarios", response_model=ScenariosResponse)
def scenarios() -> ScenariosResponse:
    return ScenariosResponse(scenarios=SCENARIOS)


@app.post("/api/chat")
def chat(body: ChatRequest) -> JSONResponse:
    scenario_id = (body.scenarioId or "").strip()
    if not scenario_id:
        return _error(400, "scenarioId is required")

    message = (body.message or "").strip()
    if not message:
        return _error(400, "message is required")

    scenario = find_scenario(scenario_id)
    if scenario is None:
        return _error(404, f"Unknown scenario: {scenario_id}")

    result = respond(scenario, body.history, message)
    return JSONResponse(content=result.model_dump())


@app.post("/api/voice/session")
def voice_session(body: VoiceSessionRequest, request: Request) -> JSONResponse:
    """Mint a short-lived OpenAI Realtime client secret for the counterpart call."""
    denied = authorize_voice_session(request)
    if denied is not None:
        return denied
    scenario_id = (body.scenarioId or "").strip()
    if not scenario_id:
        return _error(400, "scenarioId is required")
    scenario = find_scenario(scenario_id)
    if scenario is None:
        return _error(404, f"Unknown scenario: {scenario_id}")
    if not voice.is_available():
        return _error(
            503,
            "Voice calls need OPENAI_API_KEY and HARDTALK_VOICE_SECRET on the server. "
            "Typed practice still works.",
        )
    try:
        session = voice.create_session(scenario)
    except VoiceProviderError as exc:
        logger.warning("Voice session mint failed: %s", exc)
        return _error(502, str(exc))
    return JSONResponse(content=session.model_dump())


@app.post("/api/voice/complete")
def voice_complete(body: VoiceCompleteRequest) -> JSONResponse:
    """Score a counterpart-call transcript. Scores stay local; nothing goes to OpenAI."""
    scenario_id = (body.scenarioId or "").strip()
    if not scenario_id:
        return _error(400, "scenarioId is required")
    scenario = find_scenario(scenario_id)
    if scenario is None:
        return _error(404, f"Unknown scenario: {scenario_id}")
    result = voice.complete_round(scenario, body.turns)
    return JSONResponse(content=result.model_dump())


def _require_invoice_scenario(scenario_id: str):
    if not scenario_id:
        return None, _error(400, "scenarioId is required")
    scenario = find_scenario(scenario_id)
    if scenario is None:
        return None, _error(404, f"Unknown scenario: {scenario_id}")
    if scenario.id != INVOICE_SCENARIO_ID:
        return None, _error(
            400,
            "PayPal invoicing is only available after the Chase an overdue invoice drill.",
        )
    return scenario, None


@app.post("/api/invoice/extract")
def invoice_extract(body: InvoiceExtractRequest) -> JSONResponse:
    """Pull amount, currency, due date, recipient, and description from the round."""
    scenario, err = _require_invoice_scenario((body.scenarioId or "").strip())
    if err is not None:
        return err
    result = extract_terms(scenario, body.turns)
    return JSONResponse(content=result.model_dump())


@app.post("/api/invoice/send")
def invoice_send(body: InvoiceSendRequest) -> JSONResponse:
    """Create and send a sandbox (or mock) PayPal invoice. Never called implicitly."""
    scenario, err = _require_invoice_scenario((body.scenarioId or "").strip())
    if err is not None:
        return err
    del scenario  # catalog gate only; terms come from the reviewed form
    terms = terms_from_send_request(body)
    missing = missing_fields(terms)
    if missing:
        return _error(400, "Missing invoice fields: " + ", ".join(missing))
    try:
        result = paypal.create_and_send_invoice(terms)
    except PayPalError as exc:
        logger.warning("PayPal invoice send failed: %s", exc)
        return _error(502, str(exc))
    return JSONResponse(content=result.model_dump())
