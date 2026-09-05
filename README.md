# HardTalkAI

Flight Simulator for Difficult Conversations.

HardTalkAI lets you rehearse high-stakes conversations — asking for a raise, giving
critical feedback, saying no — against an AI counterpart that reacts to your tone, and
gives you live coaching on clarity, empathy, and assertiveness.

## How it works

![How HardTalkAI works: a shared Kotlin Multiplatform module (the coaching engine, scenarios, scoring, and DTOs) is compiled into the web app, the planned Android/iOS apps, and the Ktor server. Apps call the Ktor server over /api (HTTP + JSON) and can also run the same engine on-device, offline. The practice loop: 1) pick a scenario, 2) talk to the AI, 3) get live coaching scores, 4) refine and retry.](docs/how-it-works.svg)

1. **Pick a scenario** — ask for a raise, give critical feedback, or say no to extra work.
2. **Talk to the AI** — it role-plays the counterpart and reacts to your tone.
3. **Get live coaching** — each message is scored on clarity, empathy, and assertiveness, with specific tips.
4. **Refine & retry** — adjust your wording and watch the counterpart warm up.

## Architecture

HardTalkAI is a **Kotlin Multiplatform (KMP)** project. The coaching engine lives once in a
shared Kotlin module and is compiled into every target — clients and server alike — so the
scoring logic and API contracts never drift.

- **`shared/`** — Kotlin Multiplatform module (`commonMain`). The conversation/coaching
  engine, scenario definitions, message scoring, and `@Serializable` DTOs. The `jvm` target
  is enabled today; `js` (web), `androidTarget`, and the iOS targets are stubbed in
  `shared/build.gradle.kts`, ready to enable per platform.
- **`server/`** — Ktor (Kotlin) API on `:3001`, depending on `:shared`. Endpoints:
  `/api/health`, `/api/scenarios`, `/api/chat`.
- **`packages/web/`** — React + Vite + TypeScript UI on `:5173`. Scenario picker, chat, and
  a live feedback panel; proxies `/api` to the Ktor server in development.
- **Android / iOS** — planned Compose Multiplatform / SwiftUI clients that reuse `:shared`.

The engine is deterministic and dependency-free, so the app runs fully offline with no API
keys required.

## Getting started

Requires **JDK 21**, Node 20+, and [pnpm](https://pnpm.io) (repo pins `pnpm@10`). Gradle is
provided via the wrapper (`./gradlew`).

```bash
pnpm install                     # web dependencies

# Terminal 1 — Ktor API (:3001)
./gradlew :server:run

# Terminal 2 — web app (:5173)
pnpm --filter @hardtalkai/web dev
```

Then open http://localhost:5173.

### Useful commands

```bash
./gradlew :server:run                    # run the Ktor API (:3001)
pnpm --filter @hardtalkai/web dev        # run the web app (:5173)
./gradlew :shared:jvmTest :server:test   # run Kotlin engine + server tests
pnpm --filter @hardtalkai/web build      # production build of the web app
./gradlew :server:build                  # build the server (fat jar via shadow)
```

## Cloud Agent environment

`.cursor/environment.json` installs web dependencies with `pnpm install --frozen-lockfile`
and pre-builds the JVM modules (`./gradlew :shared:build :server:build`), starts the `server`
(Ktor) and `web` (Vite) dev servers as terminals, and exposes ports `5173` and `3001`.
