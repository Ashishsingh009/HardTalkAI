import type { ChatTurn, ReplyResult, Scenario } from "./types";

export async function fetchScenarios(): Promise<Scenario[]> {
  const res = await fetch("/api/scenarios");
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
    headers: { "Content-Type": "application/json" },
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
