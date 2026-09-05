import express from "express";
import cors from "cors";
import { scenarios, findScenario } from "./scenarios.js";
import { generateReply } from "./engine.js";
import type { ChatTurn } from "./types.js";

const app = express();
app.use(cors());
app.use(express.json({ limit: "1mb" }));

const PORT = Number(process.env.PORT ?? 3001);

app.get("/api/health", (_req, res) => {
  res.json({ status: "ok", engine: "builtin", scenarios: scenarios.length });
});

app.get("/api/scenarios", (_req, res) => {
  res.json({
    scenarios: scenarios.map((s) => ({
      id: s.id,
      title: s.title,
      summary: s.summary,
      difficulty: s.difficulty,
      persona: s.persona,
      opening: s.opening,
      goals: s.goals,
    })),
  });
});

interface ChatRequestBody {
  scenarioId?: string;
  message?: string;
  history?: ChatTurn[];
}

app.post("/api/chat", (req, res) => {
  const { scenarioId, message, history } = req.body as ChatRequestBody;

  if (!scenarioId || typeof scenarioId !== "string") {
    return res.status(400).json({ error: "scenarioId is required" });
  }
  if (!message || typeof message !== "string" || !message.trim()) {
    return res.status(400).json({ error: "message is required" });
  }

  const scenario = findScenario(scenarioId);
  if (!scenario) {
    return res.status(404).json({ error: `Unknown scenario: ${scenarioId}` });
  }

  const safeHistory: ChatTurn[] = Array.isArray(history) ? history : [];
  const result = generateReply(scenario, safeHistory, message);
  res.json(result);
});

// Only start listening when run directly (not when imported by tests).
const isMain =
  process.argv[1] && import.meta.url === `file://${process.argv[1]}`;
if (isMain) {
  app.listen(PORT, () => {
    // eslint-disable-next-line no-console
    console.log(`HardTalkAI server listening on http://localhost:${PORT}`);
  });
}

export { app };
