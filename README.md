# HardTalkAI

Flight Simulator for Difficult Conversations.

HardTalkAI lets you rehearse high-stakes conversations — asking for a raise, giving
critical feedback, saying no — against an AI counterpart that reacts to your tone, and
gives you live coaching on clarity, empathy, and assertiveness.

## How it works

![How HardTalkAI works: the browser talks to the React/Vite web UI, which proxies to the Express API and coaching engine; the counterpart reply and live scores flow back to you. The practice loop: 1) pick a scenario, 2) talk to the AI, 3) get live coaching scores, 4) refine and retry.](docs/how-it-works.svg)

1. **Pick a scenario** — ask for a raise, give critical feedback, or say no to extra work.
2. **Talk to the AI** — it role-plays the counterpart and reacts to your tone.
3. **Get live coaching** — each message is scored on clarity, empathy, and assertiveness, with specific tips.
4. **Refine & retry** — adjust your wording and watch the counterpart warm up.

## Stack

- **`packages/server`** — Express + TypeScript API. Serves scenarios and runs the
  conversation/coaching engine (`/api/scenarios`, `/api/chat`, `/api/health`).
- **`packages/web`** — React + Vite + TypeScript UI. Scenario picker, chat, and a live
  feedback panel. Proxies `/api` to the server in development.

The conversation engine is a deterministic, dependency-free "coach" so the app runs fully
offline with no API keys required.

## Getting started

Requires Node 20+ and [pnpm](https://pnpm.io) (repo pins `pnpm@10`).

```bash
pnpm install
pnpm dev        # runs the server (:3001) and web (:5173) together
```

Then open http://localhost:5173.

### Useful commands

```bash
pnpm dev         # run server + web in watch mode
pnpm build       # typecheck + build server and web
pnpm typecheck   # typecheck both packages
pnpm test        # run server engine unit tests
```

## Cloud Agent environment

`.cursor/environment.json` installs dependencies with `pnpm install --frozen-lockfile`,
starts the `server` and `web` dev servers as terminals, and exposes ports `5173` and
`3001`.
