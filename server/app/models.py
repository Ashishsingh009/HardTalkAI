from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, Field

Difficulty = Literal["warm-up", "moderate", "hard"]
Role = Literal["user", "counterpart"]


class Persona(BaseModel):
    name: str
    role: str
    # Short description of the counterpart's disposition at the start.
    mood: str


class Scenario(BaseModel):
    id: str
    title: str
    summary: str
    difficulty: Difficulty
    persona: Persona
    # The first line the counterpart says to open the conversation.
    opening: str
    # Coaching goals the user is trying to achieve in this conversation.
    goals: list[str]
    # Catalog hint for a later free-tier gate (D3). Only the polished raise is free.
    # Do not enforce gating here — clients may badge it, but every scenario stays playable.
    free: bool = False


class ChatTurn(BaseModel):
    role: Role
    content: str


class Feedback(BaseModel):
    # 0-100 sub-scores for the latest user message.
    clarity: int
    empathy: int
    assertiveness: int
    # 0-100 blended score.
    overall: int
    # Human-readable coaching tips.
    tips: list[str]


class ReplyResult(BaseModel):
    reply: str
    feedback: Feedback
    # How the counterpart currently feels, after this exchange.
    mood: str


class ChatRequest(BaseModel):
    scenarioId: str | None = None
    message: str | None = None
    history: list[ChatTurn] = Field(default_factory=list)


class ScenariosResponse(BaseModel):
    scenarios: list[Scenario]


class HealthResponse(BaseModel):
    status: str
    engine: str
    scenarios: int
