from __future__ import annotations

from .models import Persona, Scenario

SCENARIOS: list[Scenario] = [
    Scenario(
        id="ask-for-raise",
        title="Ask your manager for a raise",
        summary="You have delivered strong results this year and want to make the case for a pay increase.",
        difficulty="moderate",
        persona=Persona(
            name="Dana",
            role="Your engineering manager",
            mood="busy and slightly guarded about budget",
        ),
        opening="Hey, thanks for grabbing time. I've got about ten minutes before my next meeting - what did you want to talk about?",
        goals=[
            "State clearly that you want a raise",
            "Back it up with specific accomplishments",
            "Stay collaborative rather than combative",
        ],
    ),
    Scenario(
        id="give-feedback",
        title="Give a teammate critical feedback",
        summary="A teammate keeps missing deadlines and it is affecting the team. You need to address it honestly.",
        difficulty="hard",
        persona=Persona(
            name="Sam",
            role="A peer on your team",
            mood="defensive and a little stressed",
        ),
        opening="You wanted to chat? Is everything okay? I feel like I've been slammed lately.",
        goals=[
            "Name the specific behavior and its impact",
            "Show empathy for their situation",
            "Agree on a concrete next step",
        ],
    ),
    Scenario(
        id="decline-request",
        title="Say no to extra work",
        summary="Your plate is full, but a stakeholder is pushing another urgent request onto you.",
        difficulty="warm-up",
        persona=Persona(
            name="Priya",
            role="A product stakeholder",
            mood="friendly but persistent",
        ),
        opening="Great news - leadership loved the demo! I'd love for you to take on the analytics dashboard too. You can start today, right?",
        goals=[
            "Decline clearly without over-apologizing",
            "Explain the trade-off honestly",
            "Offer an alternative or timeline",
        ],
    ),
]


def find_scenario(scenario_id: str) -> Scenario | None:
    return next((s for s in SCENARIOS if s.id == scenario_id), None)
