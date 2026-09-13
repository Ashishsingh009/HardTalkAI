import { generateReply } from "./engine.js";

const MAX_USER_TURNS = 3;
const root = document.getElementById("app");

const state = {
  data: null,
  active: null,
  turns: [],
  mood: "",
  input: "",
  error: null,
};

function scoreHue(value) {
  return Math.round((value / 100) * 120);
}

function escapeHtml(s) {
  return String(s)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function feedbackCard(feedback, turnNumber) {
  const rows = [
    ["Clarity", feedback.clarity],
    ["Empathy", feedback.empathy],
    ["Assertiveness", feedback.assertiveness],
  ]
    .map(
      ([label, value]) => `
      <div class="score-row">
        <span>${label}</span>
        <div class="score-track">
          <div class="score-fill" style="width:${value}%;background:hsl(${scoreHue(value)} 72% 45%)"></div>
        </div>
        <span class="score-value">${value}</span>
      </div>`,
    )
    .join("");
  const tips = feedback.tips.map((t) => `<li>${escapeHtml(t)}</li>`).join("");
  return `
    <div class="feedback-card">
      <h4>Turn ${turnNumber} coaching · overall ${feedback.overall}</h4>
      ${rows}
      <ul class="feedback-tips">${tips}</ul>
    </div>`;
}

function scoredTurns() {
  return state.turns.filter((t) => t.role === "user" && t.feedback).length;
}

function roundComplete() {
  return scoredTurns() >= MAX_USER_TURNS;
}

function averages() {
  const scored = state.turns.filter((t) => t.role === "user" && t.feedback);
  if (!scored.length) return null;
  const keys = ["clarity", "empathy", "assertiveness", "overall"];
  const out = {};
  for (const k of keys) {
    out[k] = Math.round(scored.reduce((n, t) => n + t.feedback[k], 0) / scored.length);
  }
  return out;
}

function startScenario(scenario) {
  state.active = scenario;
  state.turns = [{ role: "counterpart", content: scenario.opening }];
  state.mood = scenario.persona.mood;
  state.input = "";
  state.error = null;
  render();
  document.getElementById("composer-input")?.focus();
}

function reset() {
  state.active = null;
  state.turns = [];
  state.mood = "";
  state.input = "";
  state.error = null;
  render();
}

function send() {
  const message = state.input.trim();
  if (!state.active || !message || roundComplete()) return;
  const history = state.turns.slice();
  const result = generateReply(state.active, history, message, state.data.replies);
  const userTurn = { role: "user", content: message, feedback: result.feedback };
  state.turns = [...history, userTurn, { role: "counterpart", content: result.reply }];
  state.mood = result.mood;
  state.input = "";
  render();
  const box = document.querySelector(".messages");
  if (box) box.scrollTop = box.scrollHeight;
}

function renderCatalog() {
  const cards = state.data.scenarios
    .map(
      (s) => `
      <button class="scenario-card${s.free ? " scenario-card-free" : ""}" data-id="${s.id}">
        <div class="badge-row">
          <span class="badge badge-${s.difficulty}">${escapeHtml(s.difficulty)}</span>
          ${s.free ? '<span class="badge badge-free">Free practice</span>' : ""}
        </div>
        <h3>${escapeHtml(s.title)}</h3>
        <p>${escapeHtml(s.summary)}</p>
        <div class="persona-line">You'll talk to <strong>${escapeHtml(s.persona.name)}</strong> — ${escapeHtml(s.persona.role)}</div>
      </button>`,
    )
    .join("");
  root.innerHTML = `
    ${header(false)}
    <p class="catalog-lede">
      Practice a manager conversation, get scored on clarity, empathy, and
      assertiveness, then retry. ${state.data.scenarios.length} career drills.
      Three scored turns, then a recap. Scoring runs in this browser — nothing is
      sent to a coaching API.
    </p>
    <section class="scenario-grid">${cards}</section>
    <p class="note">
      Android still needs a FastAPI host for live coaching.
      Hugging Face Docker Spaces require PRO on this account — see the README.
    </p>`;
  root.querySelectorAll(".scenario-card").forEach((btn) => {
    btn.addEventListener("click", () => {
      const scenario = state.data.scenarios.find((s) => s.id === btn.dataset.id);
      startScenario(scenario);
    });
  });
}

function header(inRound) {
  return `
    <header class="app-header">
      <div class="brand">
        <div class="logo" aria-hidden="true">💬</div>
        <div>
          <h1>HardTalkAI</h1>
          <p>Career Coaching · Coach Heather</p>
        </div>
      </div>
      <nav class="header-links">
        ${inRound ? '<button class="ghost-btn" id="back-btn">← Catalog</button>' : ""}
        <a class="privacy-link" href="./privacy.html">Privacy</a>
      </nav>
    </header>`;
}

function renderRound() {
  const s = state.active;
  const userCount = scoredTurns();
  const goalsHidden = userCount >= 1;
  const complete = roundComplete();
  const avg = averages();
  const turnLabel = `${Math.min(userCount, MAX_USER_TURNS)}/${MAX_USER_TURNS}`;

  let userTurnIndex = 0;
  const messages = state.turns
    .map((t) => {
      if (t.role === "user") {
        userTurnIndex += 1;
        return `
          <div class="msg msg-user">
            <div class="msg-author">You</div>
            <div class="msg-bubble">${escapeHtml(t.content)}</div>
            ${t.feedback ? feedbackCard(t.feedback, userTurnIndex) : ""}
          </div>`;
      }
      return `
        <div class="msg msg-counterpart">
          <div class="msg-author">${escapeHtml(s.persona.name)}</div>
          <div class="msg-bubble">${escapeHtml(t.content)}</div>
        </div>`;
    })
    .join("");

  const recap = complete && avg
    ? `
      <div class="recap">
        <h3>Round complete</h3>
        <p>Three scored turns. Clarity ${avg.clarity} · Empathy ${avg.empathy} · Assertiveness ${avg.assertiveness} · Overall ${avg.overall}. Retry the same drill or pick another.</p>
        <button class="retry-btn" id="retry-btn">Retry this drill</button>
      </div>`
    : "";

  root.innerHTML = `
    ${header(true)}
    ${state.error ? `<div class="banner">${escapeHtml(state.error)}</div>` : ""}
    <section class="conversation">
      <aside class="scenario-panel">
        <div class="badge-row">
          <span class="badge badge-${s.difficulty}">${escapeHtml(s.difficulty)}</span>
          ${s.free ? '<span class="badge badge-free">Free practice</span>' : ""}
        </div>
        <h2>${escapeHtml(s.title)}</h2>
        <p>${escapeHtml(s.summary)}</p>
        <div class="persona-box">
          <div class="persona-name">${escapeHtml(s.persona.name)}</div>
          <div class="persona-role">${escapeHtml(s.persona.role)}</div>
          <div class="persona-mood">${escapeHtml(s.persona.name)} · ${escapeHtml(state.mood)} · ${turnLabel}</div>
        </div>
        <h4>Your goals</h4>
        <ul class="goals${goalsHidden ? " collapsed" : ""}">
          ${s.goals.map((g) => `<li>${escapeHtml(g)}</li>`).join("")}
        </ul>
      </aside>
      <div class="chat">
        <div class="messages">${messages}${recap}</div>
        ${
          complete
            ? ""
            : `<div class="composer">
                <textarea id="composer-input" rows="2" placeholder="Respond to ${escapeHtml(s.persona.name)}… (Enter to send)">${escapeHtml(state.input)}</textarea>
                <button class="send-btn" id="send-btn" ${state.input.trim() ? "" : "disabled"}>Send</button>
              </div>`
        }
      </div>
    </section>`;

  document.getElementById("back-btn")?.addEventListener("click", reset);
  document.getElementById("retry-btn")?.addEventListener("click", () => startScenario(s));
  const input = document.getElementById("composer-input");
  const sendBtn = document.getElementById("send-btn");
  if (input) {
    input.addEventListener("input", () => {
      state.input = input.value;
      if (sendBtn) sendBtn.disabled = !state.input.trim();
    });
    input.addEventListener("keydown", (e) => {
      if (e.key === "Enter" && !e.shiftKey) {
        e.preventDefault();
        state.input = input.value;
        send();
      }
    });
    sendBtn?.addEventListener("click", () => {
      state.input = input.value;
      send();
    });
    const box = document.querySelector(".messages");
    if (box) box.scrollTop = box.scrollHeight;
  }
}

function render() {
  if (!state.active) renderCatalog();
  else renderRound();
}

const data = await fetch("./data.json").then((r) => {
  if (!r.ok) throw new Error(`Could not load scenarios (${r.status})`);
  return r.json();
});
state.data = data;
render();
