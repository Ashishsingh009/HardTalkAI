import { useEffect, useRef, useState } from "react";
import { fetchScenarios, sendMessage } from "./api";
import type { ChatTurn, Feedback, Scenario } from "./types";

function ScoreBar({ label, value }: { label: string; value: number }) {
  const hue = Math.round((value / 100) * 120); // red -> green
  return (
    <div className="score-row">
      <span className="score-label">{label}</span>
      <div className="score-track">
        <div
          className="score-fill"
          style={{ width: `${value}%`, background: `hsl(${hue} 70% 45%)` }}
        />
      </div>
      <span className="score-value">{value}</span>
    </div>
  );
}

function FeedbackCard({ feedback }: { feedback: Feedback }) {
  return (
    <div className="feedback-card">
      <div className="feedback-scores">
        <ScoreBar label="Clarity" value={feedback.clarity} />
        <ScoreBar label="Empathy" value={feedback.empathy} />
        <ScoreBar label="Assertiveness" value={feedback.assertiveness} />
      </div>
      <ul className="feedback-tips">
        {feedback.tips.map((tip, i) => (
          <li key={i}>{tip}</li>
        ))}
      </ul>
    </div>
  );
}

export function App() {
  const [scenarios, setScenarios] = useState<Scenario[]>([]);
  const [active, setActive] = useState<Scenario | null>(null);
  const [turns, setTurns] = useState<ChatTurn[]>([]);
  const [input, setInput] = useState("");
  const [mood, setMood] = useState<string>("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    fetchScenarios()
      .then(setScenarios)
      .catch((e: Error) => setError(e.message));
  }, []);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" });
  }, [turns, loading]);

  function startScenario(scenario: Scenario) {
    setActive(scenario);
    setTurns([{ role: "counterpart", content: scenario.opening }]);
    setMood(scenario.persona.mood);
    setInput("");
    setError(null);
  }

  function reset() {
    setActive(null);
    setTurns([]);
    setMood("");
    setError(null);
  }

  async function handleSend() {
    if (!active || !input.trim() || loading) return;
    const message = input.trim();
    const userTurn: ChatTurn = { role: "user", content: message };
    const nextTurns = [...turns, userTurn];
    setTurns(nextTurns);
    setInput("");
    setLoading(true);
    setError(null);
    try {
      const result = await sendMessage(active.id, message, turns);
      setTurns((prev) => {
        const copy = [...prev];
        // attach feedback to the user's message we just added
        for (let i = copy.length - 1; i >= 0; i--) {
          if (copy[i].role === "user" && !copy[i].feedback) {
            copy[i] = { ...copy[i], feedback: result.feedback };
            break;
          }
        }
        return [...copy, { role: "counterpart", content: result.reply }];
      });
      setMood(result.mood);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }

  function onKeyDown(e: React.KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  }

  return (
    <div className="app">
      <header className="app-header">
        <div className="brand">
          <span className="logo" aria-hidden>✈️</span>
          <div>
            <h1>HardTalkAI</h1>
            <p>Career Coaching · Coach Heather</p>
          </div>
        </div>
        {active && (
          <button className="ghost-btn" onClick={reset}>
            ← Choose another scenario
          </button>
        )}
      </header>

      {error && <div className="banner error">{error}</div>}

      {!active ? (
        <>
          <p className="catalog-lede">
            Practice a manager conversation, get scored on clarity, empathy, and
            assertiveness, then retry. {scenarios.length > 0 ? `${scenarios.length} career drills.` : ""}{" "}
            The raise is the free-tier practice — the rest stay playable here.
          </p>
          <section className="scenario-grid">
            {scenarios.length === 0 && !error && <p className="muted">Loading scenarios…</p>}
            {scenarios.map((s) => (
              <button
                key={s.id}
                className={`scenario-card${s.free ? " scenario-card-free" : ""}`}
                onClick={() => startScenario(s)}
              >
                <div className="badge-row">
                  <span className={`badge badge-${s.difficulty}`}>{s.difficulty}</span>
                  {s.free && <span className="badge badge-free">Free practice</span>}
                </div>
                <h3>{s.title}</h3>
                <p>{s.summary}</p>
                <div className="persona-line">
                  You'll talk to <strong>{s.persona.name}</strong> — {s.persona.role}
                </div>
              </button>
            ))}
          </section>
        </>
      ) : (
        <section className="conversation">
          <aside className="scenario-panel">
            <div className="badge-row">
              <span className={`badge badge-${active.difficulty}`}>{active.difficulty}</span>
              {active.free && <span className="badge badge-free">Free practice</span>}
            </div>
            <h2>{active.title}</h2>
            <p>{active.summary}</p>
            <div className="persona-box">
              <div className="persona-name">{active.persona.name}</div>
              <div className="persona-role">{active.persona.role}</div>
              <div className="persona-mood">Mood: {mood}</div>
            </div>
            <h4>Your goals</h4>
            <ul className="goals">
              {active.goals.map((g, i) => (
                <li key={i}>{g}</li>
              ))}
            </ul>
          </aside>

          <div className="chat">
            <div className="messages" ref={scrollRef}>
              {turns.map((t, i) => (
                <div key={i} className={`msg msg-${t.role}`}>
                  <div className="msg-author">
                    {t.role === "user" ? "You" : active.persona.name}
                  </div>
                  <div className="msg-bubble">{t.content}</div>
                  {t.feedback && <FeedbackCard feedback={t.feedback} />}
                </div>
              ))}
              {loading && (
                <div className="msg msg-counterpart">
                  <div className="msg-author">{active.persona.name}</div>
                  <div className="msg-bubble typing">…thinking</div>
                </div>
              )}
            </div>
            <div className="composer">
              <textarea
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={onKeyDown}
                placeholder={`Respond to ${active.persona.name}… (Enter to send, Shift+Enter for a new line)`}
                rows={3}
              />
              <button className="send-btn" onClick={handleSend} disabled={loading || !input.trim()}>
                {loading ? "Sending…" : "Send"}
              </button>
            </div>
          </div>
        </section>
      )}
    </div>
  );
}
