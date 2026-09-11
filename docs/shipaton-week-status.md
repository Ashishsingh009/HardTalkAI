# Shipaton week status — HardTalkAI (D1–D7)

**Lane:** Career Coaching / Leadership (Coach Heather)  
**Repo:** https://github.com/Ashishsingh009/HardTalkAI  
**Date:** 11 September 2026  
**This page:** what landed, what is blocked, what to merge.

---

## Snapshot

| Day | Theme | Status | Evidence |
| --- | --- | --- | --- |
| D1 | Android practice loop vs FastAPI | **Done** (open PR) | [#5](https://github.com/Ashishsingh009/HardTalkAI/pull/5) `cursor/android-practice-loop-021a` |
| D2 | Richer coaching + end-of-round recap | **Done** (open PR) | [#6](https://github.com/Ashishsingh009/HardTalkAI/pull/6) `cursor/android-richer-feedback-d177` |
| D3 | RevenueCat / free-tier paywall | **Done** (open PR, stacked on #9) | Android SDK + entitlement `pro` + monthly `hardtalkai_pro_monthly`. Raise stays free. Dashboard products still Ashish-only. |
| D4 | Career catalog → 10 manager drills | **Done** (open PR) | [#7](https://github.com/Ashishsingh009/HardTalkAI/pull/7) `cursor/career-scenarios-d4-00ac` |
| D5 | Play listing + privacy | **Partial** (open PR) | [#8](https://github.com/Ashishsingh009/HardTalkAI/pull/8) `cursor/play-listing-d5-d562` — docs + in-app Privacy. **No** Console, **no** AAB upload. |
| D6 | Demo video | **Script ready; Ashish films** | `docs/shipaton-demo-script.md` (≤2 min, Android preferred). Docs PR [#9](https://github.com/Ashishsingh009/HardTalkAI/pull/9) |
| D7 | Devpost | **Draft ready; Ashish pastes** | `docs/shipaton-devpost.md` (same PR #9) |

PRs **#1–#4** (env, stale bot, FastAPI, KMP scaffold) are already on `main`. The Shipaton product week is **#5–#9** plus the D3 RevenueCat PR stacked on #9.

---

## Merge order (do this, in this order)

Stacked branches. Merging out of order drops recap, catalog, privacy, or the paywall.

```
main
  └─ #5  android-practice-loop-021a      D1  3-turn Android loop
       └─ #6  android-richer-feedback-d177   D2  scores, sparkline, recap
            └─ #7  career-scenarios-d4-00ac      D4  10 drills + Free badge
                 └─ #8  play-listing-d5-d562         D5  privacy + listing stubs
                      └─ #9  shipaton-d6-d7-prep-f533    D6/D7  demo script + Devpost
                           └─ D3  revenuecat-android-d3      Android Pro gate (this PR)
```

1. Merge **#5** into `main`.
2. Retarget / merge **#6**.
3. Retarget / merge **#7**.
4. Retarget / merge **#8**.
5. Merge **#9** (D6/D7 docs PR).
6. Merge **D3** (RevenueCat Android) after **#9**.

Do **not** merge #8 or **#9** onto `main` until #5–#7 are in — #8’s base is the D4 catalog branch, not `main`. Do **not** merge D3 until **#9** is in (or retarget it).

---

## Blocked on Ashish (human / keys)

Client RevenueCat code is in. Play upload and live dashboard objects are still human work.

| Blocker | Why it matters | Workaround in repo |
| --- | --- | --- |
| **RevenueCat dashboard** products (entitlement `pro`, product `hardtalkai_pro_monthly`, current offering `$rc_monthly`) | Test Store purchase sheet needs these IDs. | App code expects those ids. Key goes in gitignored `local.properties` — **do not commit** it. Steps: `docs/revenuecat-android.md`. |
| **Play Console** (account, app, signing keystore, tester list, Play subscription) | D5 store upload, real `goog_` key, Play product. | Paste-ready `docs/privacy-policy.md`, `docs/play-store-listing.md`, `docs/play-console-checklist.md`. In-app Privacy on web + Android. |
| **OpenAI key** | Live in-character replies. | Builtin canned replies. Scores/tips always local. Demo script timed against `engine: builtin`. |
| **Device recording** | D6 video. Cloud agents cannot shoot Ashish’s phone. | Camera brief in `docs/shipaton-demo-script.md`. One sitting, paste-ready lines. |
| **Devpost submit button** | D7. | Draft in `docs/shipaton-devpost.md`. |

Production HTTPS API base URL and a 512×512 icon / 1024×500 feature graphic are also Ashish-only (listed in the Play checklist). Not D6/D7 blockers for a localhost demo.

---

## What each open PR actually is

**[#5 D1](https://github.com/Ashishsingh009/HardTalkAI/pull/5)** — Android talks to FastAPI. Pick a scenario, send a reply, see clarity / empathy / assertiveness, three turns, composer closes, retry or pick another. No new scenarios.

**[#6 D2](https://github.com/Ashishsingh009/HardTalkAI/pull/6)** — Coach Heather panel, per-turn bars + deltas + sparkline, **Round complete** recap (how scores moved + one takeaway). Still three drills at this layer.

**[#7 D4](https://github.com/Ashishsingh009/HardTalkAI/pull/7)** — Catalog grows to **10**. Stable ids: `ask-for-raise`, `give-feedback`, `decline-request`. Raise listed first with **Free practice**. Demo drills: Dana / Sam / Priya.

**[#8 D5](https://github.com/Ashishsingh009/HardTalkAI/pull/8)** — Privacy stub (HTML + FastAPI `/privacy`), Play listing copy, Console checklist, in-app Privacy links. Explicitly **not** Console access, **not** store upload.

**[#9 D6/D7](https://github.com/Ashishsingh009/HardTalkAI/pull/9)** — Demo camera brief + Devpost draft + this status page.

**D3 (this drop)** — RevenueCat Purchases Android SDK, `pro` entitlement gate, monthly paywall (price / purchase / restore). Free raise always playable, including offline / missing key.

---

## D6 / D7 remaining work (Ashish, ~one sitting each)

1. **Film** using `docs/shipaton-demo-script.md` (Android if available, else web localhost). ≤2 min.
2. **Paste** `docs/shipaton-devpost.md` into Devpost, attach the video + repo link, lane = Career Coaching.
3. After the video exists, drop the file into the Devpost gallery; Play screenshots can wait for Console.

---

## Demo quality vs store quality

| Bar | Ready? |
| --- | --- |
| Local demo (builtin engine, raise + Sam, recap on Android) | Yes, after #5–#7 are on the branch you run |
| Reproducible camera script + Devpost copy | Yes (docs in #9) |
| Live LLM counterpart | No — needs `OPENAI_API_KEY` |
| Paywall / free-tier enforcement | Android client yes; needs RC dashboard products + Test Store key for a live purchase |
| Play internal testing | No — blocked on Console + signed AAB |

Shipaton judging can use the local Android (or web) demo. Store upload is still out of scope until Console exists.
