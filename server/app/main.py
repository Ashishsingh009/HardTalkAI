from __future__ import annotations

import logging
from pathlib import Path

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, JSONResponse

from . import ai
from .coach import respond
from .models import ChatRequest, HealthResponse, ScenariosResponse
from .scenarios import SCENARIOS, find_scenario

# repo-root/docs/*.html (this file is server/app/main.py)
DOCS_DIR = Path(__file__).resolve().parents[2] / "docs"
HOME_HTML = DOCS_DIR / "index.html"
PRIVACY_HTML = DOCS_DIR / "privacy-policy.html"

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)-5s %(name)s - %(message)s")
logger = logging.getLogger("hardtalkai")

app = FastAPI(title="HardTalkAI", version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["*"],
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
    return HealthResponse(status="ok", engine=ai.engine_name(), scenarios=len(SCENARIOS))


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
