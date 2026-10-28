"""First-party funnel counts. No third-party SDK, no transcripts, no PII."""

from __future__ import annotations

from collections import deque
from datetime import datetime, timezone
from html import escape
from threading import Lock
from typing import Any

from .scenarios import INVOICE_SCENARIO_ID, find_scenario

MAX_USER_TURNS = 3

MAX_EVENTS = 2000
SESSION_HEADER = "X-HardTalk-Session"
ALLOWED_EVENTS = frozenset(
    {
        "catalog_view",
        "scenario_open",
        "paywall_shown",
        "call_start",
        "turn_scored",
        "round_complete",
        "invoice_review",
        "invoice_send",
    }
)
ALLOWED_CHANNELS = frozenset({"typed", "voice"})
ALLOWED_MODES = frozenset({"mock", "sandbox"})

_lock = Lock()
_events: deque[dict[str, Any]] = deque(maxlen=MAX_EVENTS)


def reset() -> None:
    with _lock:
        _events.clear()


def normalize_session(raw: str | None) -> str:
    token = (raw or "").strip()
    if not token or len(token) > 64:
        return "anon"
    if not all(ch.isalnum() or ch in "-_" for ch in token):
        return "anon"
    return token


def _known_scenario(scenario_id: str | None) -> str | None:
    if not scenario_id:
        return None
    scenario = find_scenario(scenario_id.strip())
    return scenario.id if scenario is not None else None


def record(
    *,
    name: str,
    session: str,
    scenario_id: str | None = None,
    channel: str | None = None,
    turn: int | None = None,
    mode: str | None = None,
) -> bool:
    if name not in ALLOWED_EVENTS:
        return False
    payload: dict[str, Any] = {
        "name": name,
        "session": normalize_session(session),
        "ts": datetime.now(timezone.utc).isoformat(timespec="seconds"),
    }
    known = _known_scenario(scenario_id)
    if known:
        payload["scenarioId"] = known
    if channel in ALLOWED_CHANNELS:
        payload["channel"] = channel
    if isinstance(turn, int) and 1 <= turn <= MAX_USER_TURNS:
        payload["turn"] = turn
    if mode in ALLOWED_MODES:
        payload["mode"] = mode
    with _lock:
        _events.append(payload)
    return True


def snapshot() -> dict[str, Any]:
    with _lock:
        events = list(_events)
    return {
        "eventCount": len(events),
        "sessions": len({event["session"] for event in events}),
        "practice": _funnel(events, invoice_only=False),
        "invoice": _funnel(events, invoice_only=True),
        "byScenario": _by_scenario(events),
        "channels": {
            "typed": _session_count(events, "round_complete", channel="typed"),
            "voice": _session_count(events, "round_complete", channel="voice"),
        },
        "invoiceSend": {
            "mock": _session_count(events, "invoice_send", mode="mock"),
            "sandbox": _session_count(events, "invoice_send", mode="sandbox"),
        },
    }


def _session_count(
    events: list[dict[str, Any]],
    name: str,
    *,
    invoice_only: bool = False,
    channel: str | None = None,
    mode: str | None = None,
) -> int:
    sessions: set[str] = set()
    for event in events:
        if event["name"] != name:
            continue
        if invoice_only and event.get("scenarioId") != INVOICE_SCENARIO_ID:
            continue
        if channel and event.get("channel") != channel:
            continue
        if mode and event.get("mode") != mode:
            continue
        sessions.add(event["session"])
    return len(sessions)


def _sessions_for(events: list[dict[str, Any]], names: set[str], *, invoice_only: bool) -> set[str]:
    matched: set[str] = set()
    for event in events:
        if event["name"] not in names:
            continue
        if invoice_only and event["name"] != "catalog_view":
            if event.get("scenarioId") != INVOICE_SCENARIO_ID:
                continue
        matched.add(event["session"])
    return matched


def _funnel(events: list[dict[str, Any]], *, invoice_only: bool) -> list[dict[str, Any]]:
    steps = [
        ("catalog", "Saw catalog", {"catalog_view"}),
        ("started", "Started a drill", {"scenario_open", "turn_scored", "call_start"}),
        ("completed", "Finished three turns", {"round_complete"}),
        ("invoice_review", "Opened invoice review", {"invoice_review"}),
        ("invoice_send", "Sent PayPal invoice", {"invoice_send"}),
    ]
    rows: list[dict[str, Any]] = []
    previous = 0
    for index, (step_id, label, names) in enumerate(steps):
        count = len(_sessions_for(events, names, invoice_only=invoice_only))
        conversion = None
        if index == 0:
            conversion = 1.0 if count else 0.0
        elif previous:
            conversion = round(count / previous, 3)
        else:
            conversion = 0.0
        rows.append(
            {
                "id": step_id,
                "label": label,
                "sessions": count,
                "conversionFromPrevious": conversion,
            }
        )
        previous = count
    return rows


def _by_scenario(events: list[dict[str, Any]]) -> list[dict[str, Any]]:
    started: dict[str, set[str]] = {}
    completed: dict[str, set[str]] = {}
    for event in events:
        scenario_id = event.get("scenarioId")
        if not scenario_id:
            continue
        if event["name"] in {"scenario_open", "turn_scored", "call_start"}:
            started.setdefault(scenario_id, set()).add(event["session"])
        if event["name"] == "round_complete":
            completed.setdefault(scenario_id, set()).add(event["session"])
    rows = []
    for scenario_id in sorted(set(started) | set(completed)):
        rows.append(
            {
                "id": scenario_id,
                "started": len(started.get(scenario_id, ())),
                "completed": len(completed.get(scenario_id, ())),
            }
        )
    return rows


def render_dashboard_html(data: dict[str, Any] | None = None) -> str:
    data = data if data is not None else snapshot()
    practice = data["practice"]
    invoice = data["invoice"]
    catalog = max((practice[0]["sessions"] if practice else 0), 1)

    def bars(rows: list[dict[str, Any]]) -> str:
        chunks = []
        for row in rows:
            width = max(4, round(100 * row["sessions"] / catalog)) if catalog else 4
            conv = row["conversionFromPrevious"]
            conv_label = f"{conv:.0%}" if isinstance(conv, float) else "—"
            chunks.append(
                "<div class='step'>"
                f"<div class='meta'><strong>{escape(row['label'])}</strong>"
                f"<span>{row['sessions']} sessions · {conv_label} from previous</span></div>"
                f"<div class='track'><div class='fill' style='width:{width}%'></div></div>"
                "</div>"
            )
        return "".join(chunks)

    scenario_rows = "".join(
        "<tr>"
        f"<td><code>{escape(row['id'])}</code></td>"
        f"<td>{row['started']}</td>"
        f"<td>{row['completed']}</td>"
        "</tr>"
        for row in data["byScenario"]
    ) or "<tr><td colspan='3'>No drills started yet.</td></tr>"

    return f"""<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>Funnel analytics — HardTalkAI</title>
    <style>
      :root {{
        --bg: #0f172a;
        --panel: #1e293b;
        --text: #e2e8f0;
        --muted: #94a3b8;
        --accent: #818cf8;
        --border: #334155;
        --fill: #6366f1;
      }}
      * {{ box-sizing: border-box; }}
      body {{
        margin: 0;
        font-family: Inter, system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
        background: radial-gradient(1200px 600px at 10% -10%, #1e293b 0%, var(--bg) 55%);
        color: var(--text);
        line-height: 1.5;
      }}
      main {{ max-width: 46rem; margin: 0 auto; padding: 32px 20px 64px; }}
      a {{ color: var(--accent); }}
      h1 {{ font-size: 1.75rem; margin: 0 0 8px; }}
      h2 {{ font-size: 1.1rem; margin: 28px 0 10px; }}
      .muted {{ color: var(--muted); font-size: 0.95rem; }}
      .card {{
        background: var(--panel);
        border: 1px solid var(--border);
        border-radius: 14px;
        padding: 16px 18px;
        margin: 16px 0;
      }}
      .step {{ margin: 12px 0; }}
      .meta {{ display: flex; justify-content: space-between; gap: 12px; font-size: 0.92rem; }}
      .meta span {{ color: var(--muted); }}
      .track {{ height: 10px; background: #0f172a; border-radius: 999px; margin-top: 6px; overflow: hidden; }}
      .fill {{ height: 100%; background: var(--fill); }}
      table {{ width: 100%; border-collapse: collapse; font-size: 0.92rem; }}
      th, td {{ text-align: left; padding: 8px 6px; border-bottom: 1px solid var(--border); }}
      code {{ font-size: 0.85rem; }}
    </style>
  </head>
  <body>
    <main>
      <p class="muted"><a href="/">← HardTalkAI</a></p>
      <h1>Funnel analytics</h1>
      <p class="muted">
        First-party counts on this FastAPI process.
        {data["sessions"]} sessions · {data["eventCount"]} events.
        No third-party SDK. Counts reset when the server restarts.
      </p>
      <div class="card">
        <p>
          Event name, scenario id, and a random session token only.
          Practice replies, emails, and invoice amounts are not stored here.
        </p>
        <p class="muted">JSON: <a href="/api/analytics/funnel"><code>/api/analytics/funnel</code></a></p>
      </div>
      <h2>Practice loop</h2>
      <div class="card">{bars(practice)}</div>
      <h2>Getting paid (Chase an overdue invoice)</h2>
      <div class="card">
        {bars(invoice)}
        <p class="muted">
          Send mode · mock {data["invoiceSend"]["mock"]} ·
          sandbox {data["invoiceSend"]["sandbox"]}.
          Completes · typed {data["channels"]["typed"]} ·
          voice {data["channels"]["voice"]}.
        </p>
      </div>
      <h2>By scenario</h2>
      <div class="card">
        <table>
          <thead><tr><th>Scenario</th><th>Started</th><th>Completed</th></tr></thead>
          <tbody>{scenario_rows}</tbody>
        </table>
      </div>
    </main>
  </body>
</html>
"""
