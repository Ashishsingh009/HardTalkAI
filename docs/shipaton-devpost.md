# Devpost draft — HardTalkAI

Paste into the Shipaton / Devpost form. Trim if a field is shorter than this file. Do **not** claim voice, multi-persona live coaching, a Play listing, or payments — those are not in this build.

**Lane (required):** Career Coaching (Leadership / Coach Heather)  
**Repo:** https://github.com/Ashishsingh009/HardTalkAI  
**Demo video:** attach Ashish’s ≤2 min recording from `docs/shipaton-demo-script.md` (Android preferred).

---

## Project name

HardTalkAI

## Tagline

Practice the career conversation before the 1:1 — Coach Heather scores you, then you retry.

*(118 characters. Alternate, 78 characters: `Rehearse high-stakes career talks with Coach Heather — score, refine, retry.`)*

## Elevator pitch (if the form has a short “about”)

HardTalkAI is a flight simulator for difficult manager talks. Pick a drill, type what you would actually say, get scored on clarity / empathy / assertiveness, and run it again. It is a scored practice round, not a chatbot.

---

## Inspiration

The talks that stall careers are short and high-stakes: asking for a raise with a freeze on the packet, telling a peer the last two launches slipped, saying no when your name is already on Friday’s exec readout. People either avoid them or wing them.

We wanted a private rehearsal room that **pushes back**. A counterpart who goes guarded when you hedge, and Coach Heather who names the miss (usually assertiveness) instead of cheering every message. Shipaton’s Career Coaching lane (Leadership / Heather) is exactly that loop: practice → score → retry.

This week is a prototype you can demo in two minutes, not a shipped coach-in-your-pocket.

---

## What it does

HardTalkAI is **career coaching**, not open-ended chat.

1. **Pick a manager drill** from a catalog of 10 scenarios (raise, critical feedback, saying no, impossible date, calibration advocacy, launch slip, headcount, skip-level disagreement, stolen credit, reclaiming a 1:1).
2. **Talk to the counterpart in text** — Dana, Sam, Priya, and the rest stay in character and react to your tone (warming / neutral / guarded).
3. **Get scored every turn** on clarity, empathy, and assertiveness, plus specific tips (“lead with *I'd like…*”, stop hedging, add a number).
4. **Retry.** On Android the round is three scored turns, then a **recap**: how overall moved (e.g. 31 → 67), biggest lift, one thing to try next, **Try this scenario again** / **Choose another scenario**.

The raise drill (**Ask your manager for a raise**, Dana) is marked **Free practice** in the catalog. On Android the rest of the catalog is locked behind HardTalk Pro (RevenueCat); web stays ungated for this demo.

**Honest about the prototype**

- Rehearsal is **typed text**. There is no voice input, no live spoken multi-persona session, and no in-call interruption.
- Coaching scores and tips are computed on our FastAPI server with a deterministic rubric (markers for hedges, “I” asks, empathy, numbers). They do not require an LLM.
- Counterpart *replies* are canned in-character lines unless the operator sets `OPENAI_API_KEY`. Without a key the app still runs fully offline. We did not enable OpenAI for the default demo so the video is reproducible.
- Web (`localhost:5173`) shares the catalog and scores. The finite round + recap is the **Android / Compose Multiplatform** loop.
- No accounts, no analytics SDK, no Play upload in this submission. Android Pro is wired to RevenueCat (Test Store key, not a store AAB).

---

## How we built it

Three clients, one coaching API.

- **FastAPI (Python)** on `:3001` — `GET /api/scenarios`, `POST /api/chat`, `/api/health`, privacy HTML at `/privacy`. The engine scores the latest user message, picks a tone, and either returns a canned reply or calls OpenAI (`gpt-4o-mini` by default) in character. Scores never go to OpenAI.
- **React + Vite + TypeScript** web app on `:5173` — scenario grid, conversation, live score bars. Dev proxy to the API.
- **Kotlin Multiplatform / Compose Multiplatform** (`client/HardTalkAi`) — shared UI, Ktor Client, kotlinx.serialization DTOs that match the FastAPI models. Android is the judged path: catalog with a Free badge, Coach Heather panel, per-turn bars + sparkline, three-turn recap. iOS uses the same shared UI; we did not cut an iOS store build this week.

Play-facing copy (privacy stub, listing, internal-testing checklist) lives in `docs/` so a Console upload is paste, not invention. Android billing is RevenueCat Test Store in debug; we did not upload an AAB.

---

## Challenges we ran into

- **Keep it a drill, not a chatbot.** The web UI will happily keep scoring turn 4+. Android had to close the composer after three turns and invent a recap from the *existing* feedback payload (no extra API fields). That tension is still visible if you demo web.
- **Stacking a week of PRs.** D1–D5 landed as stacked branches (`#5` → `#6` → `#7` → `#8`). Catalog size, recap UI, and privacy all assume that merge order. Landing them out of order would drop the Free badge or the recap.
- **Play Console and a live Play product.** RevenueCat Android is in the client (entitlement `pro`, monthly `hardtalkai_pro_monthly`). Creating the dashboard product, a `goog_` key, and a signed AAB is still Ashish-only. We did not fake a store listing URL.
- **OpenAI key.** Live in-character replies stay optional. Raise is `free: true` and stays playable without any cloud billing key.
- **Scoring that is demoable without an LLM.** A rubric that punishes “maybe / just / if that's okay” and rewards “I'd like” + a number is easy to game and easy to show. We accepted that honesty: it is a coaching *gym*, not a judge of real performance reviews.

---

## Accomplishments that we're proud of

- A **two-minute story** you can film in one sitting: Free raise → hedged ask (Dana guarded, assertiveness 0) → numbered retry (Dana opens up) → recap 31 → 67 → one turn of Sam.
- **10 career drills** with distinct openings and canned pushback (warming / neutral / guarded), not three cloned templates.
- **Android recap** that names one limiter (“assertiveness improved 0 → 52 but is still the limiter”) instead of a vanity overall score.
- Same FastAPI catalog on web and Android. Privacy stub + Play listing draft ready when Console exists.
- The app runs **with zero cloud keys**. Judges can clone and demo.

---

## What we learned

- Finite rounds photograph better than “chat with an AI manager.” The recap is the product shot.
- Counterpart mood has to *move* on camera. One hedged line and one strong line is the whole trick.
- Store and billing work is mostly human: keys, screenshots, a signing keystore. Code can only leave paste-ready docs.

---

## What's next

- **Play Console** internal testing: signed AAB, hosted privacy URL, screenshots from `docs/play-store-listing.md`. Blocked on account access. Swap the Test Store `test_` key for a `goog_` Play key before any store upload.
- **OpenAI replies** in the demo environment so Dana/Sam are less canned, with the same local scores.
- Later, not this submission: voice rehearsal, iOS store, real persistence. We will not pretend those shipped.

---

## Built with

Python, FastAPI, Uvicorn, Pydantic, OpenAI API (optional), React, TypeScript, Vite, Kotlin, Kotlin Multiplatform, Compose Multiplatform, Ktor, kotlinx.serialization, Android, Material 3, RevenueCat, pytest, Gradle

*(Devpost tag list — tick what the form offers, paste the rest into “other”:)*

`python` · `fastapi` · `react` · `typescript` · `vite` · `kotlin` · `kotlin-multiplatform` · `jetpack-compose` · `android` · `ktor` · `openai` · `career-coaching`

---

## Try it out

- **GitHub:** https://github.com/Ashishsingh009/HardTalkAI
- **Local web:** clone, `pnpm install`, `python3 -m venv .venv && .venv/bin/pip install -r server/requirements-dev.txt`, then FastAPI `:3001` + Vite `:5173` (see README).
- **Android:** `cd client/HardTalkAi && ./gradlew :androidApp:installDebug` against local FastAPI (`10.0.2.2:3001` on emulator).
- **Play Store:** not listed. Internal testing is blocked on Console + signing.
- **Live AI replies:** set `OPENAI_API_KEY` on the server; omit it for the reproducible canned demo.

---

## Cover / gallery notes

Use Android screenshots, not browser chrome:

1. Catalog with **Free practice** on the raise, plus Sam/Priya cards.
2. Hedged raise: Turn 1 coaching, assertiveness 0, Dana guarded.
3. **Round complete** recap (31 → 67).
4. Optional: Sam warming after the critical-feedback line.

Shot list detail: `docs/play-store-listing.md`. Camera script: `docs/shipaton-demo-script.md`.
