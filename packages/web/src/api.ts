import type { ChatTurn, ReplyResult, Scenario } from "./types";

const SESSION_KEY = "hardtalk-session";

function sessionId(): string {
  try {
    const existing = sessionStorage.getItem(SESSION_KEY);
    if (existing) return existing;
    const created = crypto.randomUUID().replace(/-/g, "").slice(0, 32);
    sessionStorage.setItem(SESSION_KEY, created);
    return created;
  } catch {
    return "anon";
  }
}

function jsonHeaders(): HeadersInit {
  return {
    "Content-Type": "application/json",
    "X-HardTalk-Session": sessionId(),
  };
}

export async function fetchScenarios(): Promise<Scenario[]> {
  const res = await fetch("/api/scenarios", { headers: jsonHeaders() });
  if (!res.ok) throw new Error(`Failed to load scenarios (${res.status})`);
  const data = (await res.json()) as { scenarios: Scenario[] };
  return data.scenarios;
}

export async function sendMessage(
  scenarioId: string,
  message: string,
  history: ChatTurn[],
): Promise<ReplyResult> {
  const res = await fetch("/api/chat", {
    method: "POST",
    headers: jsonHeaders(),
    body: JSON.stringify({
      scenarioId,
      message,
      history: history.map(({ role, content }) => ({ role, content })),
    }),
  });
  if (!res.ok) {
    const err = (await res.json().catch(() => ({}))) as { error?: string };
    throw new Error(err.error ?? `Request failed (${res.status})`);
  }
  return (await res.json()) as ReplyResult;
}

export function trackFunnelEvent(event: string, scenarioId?: string): void {
  void fetch("/api/analytics/event", {
    method: "POST",
    headers: jsonHeaders(),
    body: JSON.stringify({ event, scenarioId }),
    keepalive: true,
  }).catch(() => undefined);
}
