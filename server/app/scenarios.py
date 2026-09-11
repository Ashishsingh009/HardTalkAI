from __future__ import annotations

from .models import Persona, Scenario

# Later free-tier gating (D3 / RevenueCat) should keep this id playable and lock the rest.
# Do not enforce that gate in this catalog — every scenario stays callable.
FREE_SCENARIO_ID = "ask-for-raise"

# Original three ids are stable: ask-for-raise, give-feedback, decline-request.
SCENARIOS: list[Scenario] = [
    Scenario(
        id="ask-for-raise",
        title="Ask your manager for a raise",
        summary=(
            "Comp cycle locks Friday. You've been at the same band for 18 months, "
            "shipped the payments rewrite, and a peer who joined after you already "
            "got a bump. Dana has ten minutes and a no-extra-budget packet to defend."
        ),
        difficulty="moderate",
        persona=Persona(
            name="Dana",
            role="Your engineering manager",
            mood="time-boxed, already drafted 'no extra budget' into the cycle packet",
        ),
        opening=(
            "I've got to jump to the staffing review in ten minutes. You marked this "
            "as can't-wait — what's going on?"
        ),
        goals=[
            "Name the raise or band you want, with a number",
            "Tie it to specific impact, not hours worked",
            "Stay collaborative when Dana cites the freeze",
        ],
        free=True,
    ),
    Scenario(
        id="give-feedback",
        title="Give a teammate critical feedback",
        summary=(
            "Sam missed the last two launch dates. On-call is covering the fallout, "
            "and your skip-level asked whether this is a performance issue. You have "
            "to name the pattern without torching the working relationship."
        ),
        difficulty="hard",
        persona=Persona(
            name="Sam",
            role="A peer on your squad",
            mood="defensive, already blaming the API, expecting to be piled on",
        ),
        opening=(
            "You wanted to chat? If this is about checkout, I already told Dana the "
            "payments API was late — that wasn't my tickets."
        ),
        goals=[
            "Name the specific misses and the impact on the team",
            "Acknowledge real constraints without letting them erase the pattern",
            "Land a concrete next step, not a vague 'try harder'",
        ],
    ),
    Scenario(
        id="decline-request",
        title="Say no to extra work",
        summary=(
            "Priya wants the analytics dashboard on Friday's exec readout. Your team "
            "is already committed to the payments cutover. If you don't decline "
            "cleanly, you will own both."
        ),
        difficulty="warm-up",
        persona=Persona(
            name="Priya",
            role="A product stakeholder",
            mood="warm, persistent, already looping leadership",
        ),
        opening=(
            "Leadership loved the demo. I'm putting the analytics dashboard on the "
            "Friday exec readout — you'll own it this sprint, right?"
        ),
        goals=[
            "Decline clearly without over-apologizing",
            "Name the trade-off: payments cutover vs. dashboard",
            "Offer an alternative owner or a later date, not a maybe",
        ],
    ),
    Scenario(
        id="push-back-deadline",
        title="Push back on an impossible date",
        summary=(
            "Your director already told the CEO the platform rewrite ships in three "
            "weeks. The team knows it's closer to eight. You have to reset the date "
            "without looking like you're sandbagging."
        ),
        difficulty="hard",
        persona=Persona(
            name="Marcus",
            role="Director of Engineering",
            mood="impatient, already on the hook with the CEO, allergic to 'it depends'",
        ),
        opening=(
            "Don't make this a process conversation. I told the CEO three weeks. "
            "What do you need to hit it?"
        ),
        goals=[
            "Reset the date with a concrete, defensible timeline",
            "Show you understand the CEO pressure",
            "Offer a scoped milestone, not a vague 'we'll try'",
        ],
    ),
    Scenario(
        id="advocate-for-report",
        title="Advocate for a direct report in calibration",
        summary=(
            "Your strongest IC is landing as 'meets' in promo calibration. You think "
            "they should be 'exceeds.' Elena is running the room and already has a "
            "stack rank to protect."
        ),
        difficulty="hard",
        persona=Persona(
            name="Elena",
            role="Your skip-level, running promo calibration",
            mood="fair but rushed, protecting the distribution curve",
        ),
        opening=(
            "We're over the exceeds budget already. I need you to tell me why Jordan "
            "isn't a meets — in one minute, please."
        ),
        goals=[
            "Make a specific case with evidence, not adjectives",
            "Acknowledge the curve without folding immediately",
            "Ask for a clear outcome: exceeds, or a documented path",
        ],
    ),
    Scenario(
        id="deliver-slip",
        title="Tell your manager a launch will slip",
        summary=(
            "Status has been green for three weeks. Overnight the load test failed "
            "and Thursday's launch will miss. Jordan already told execs you're on track."
        ),
        difficulty="moderate",
        persona=Persona(
            name="Jordan",
            role="Your engineering manager",
            mood="caught off guard, already told execs you're green",
        ),
        opening=(
            "I just pinged the exec thread that we're green for Thursday. Please tell "
            "me that's still true."
        ),
        goals=[
            "Name the slip and a new date early",
            "Own what changed without drowning them in detail",
            "Bring a mitigation they can share with execs",
        ],
    ),
    Scenario(
        id="ask-for-headcount",
        title="Ask for headcount before the team burns out",
        summary=(
            "Two people have been on-call every weekend. Attrition risk is real. "
            "Chris's default is 'headcount freeze.' You need another engineer, not "
            "another pep talk."
        ),
        difficulty="moderate",
        persona=Persona(
            name="Chris",
            role="Director of Engineering",
            mood="budget-first, 'everyone's tired' energy, wants proof not feelings",
        ),
        opening="I saw your note about staffing. Freeze is still freeze. What's the actual ask?",
        goals=[
            "State the headcount ask clearly",
            "Tie it to risk: on-call load, attrition, missed dates",
            "Offer a phased option if the full ask is blocked",
        ],
    ),
    Scenario(
        id="disagree-up",
        title="Disagree with your skip-level in the room",
        summary=(
            "Avery wants to kill the reliability program to fund a shiny AI demo. "
            "You think that's a production incident waiting to happen. There are six "
            "people on the call."
        ),
        difficulty="hard",
        persona=Persona(
            name="Avery",
            role="VP of Engineering",
            mood="decisive, already sold on the AI demo, dislikes being contradicted in public",
        ),
        opening=(
            "We're pausing the reliability work. The AI demo is the board story this "
            "quarter. Any objections before I close it?"
        ),
        goals=[
            "Disagree on the decision, not on Avery's authority",
            "Name the production risk in concrete terms",
            "Propose a both/and: scoped reliability plus the demo",
        ],
    ),
    Scenario(
        id="reclaim-credit",
        title="Address stolen credit without sounding petty",
        summary=(
            "Riley presented your migration design as 'we' in the all-hands, then "
            "took the follow-up with the VP. You need the record corrected without "
            "becoming the office villain."
        ),
        difficulty="moderate",
        persona=Persona(
            name="Riley",
            role="A peer tech lead",
            mood="charming, 'we're a team' deflection, hates looking small",
        ),
        opening=(
            "Hey — if this is about the all-hands, that was a team win. Let's not "
            "make it weird."
        ),
        goals=[
            "Name the specific moment without attacking character",
            "Ask for a concrete correction: attribution or the VP follow-up",
            "Stay calm when they reframe it as teamwork",
        ],
    ),
    Scenario(
        id="reclaim-1-1",
        title="Reclaim your 1:1 for career, not status",
        summary=(
            "Your weekly 1:1 with Morgan has become a standup. The promo packet is "
            "due next month and you still don't have career coaching. You need to "
            "reset the meeting."
        ),
        difficulty="warm-up",
        persona=Persona(
            name="Morgan",
            role="Your engineering manager",
            mood="friendly, distracted, treats 1:1s like a second standup",
        ),
        opening=(
            "Quick status? I already scanned the board — anything red I should know "
            "before I jump?"
        ),
        goals=[
            "Redirect the 1:1 to career without dismissing the work",
            "Name the promo packet and what you need from them",
            "Agree on a standing agenda so it doesn't snap back",
        ],
    ),
]


def find_scenario(scenario_id: str) -> Scenario | None:
    return next((s for s in SCENARIOS if s.id == scenario_id), None)
