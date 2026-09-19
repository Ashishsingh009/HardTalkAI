# Shipaton demo script — HardTalkAI (≤2 min)

**Lane:** Career Coaching / Leadership (Coach Heather)  
**Who films:** Ashish, one sitting, phone or emulator.  
**Product to show:** practice → score → retry (not a chatbot).  
**This file is the camera brief.** Do not improvise new scenarios. Do not wait for Play Console or a live purchase. **Do** film HardTalk Pro: tap a locked drill, show the paywall, dismiss, then run the free raise.

Record **Android** if you have an emulator or device. Recap (`Round complete`) only exists there. Fall back to **web** at `http://localhost:5173` only if Android is down — see [Web fallback](#web-fallback-if-android-is-down).

Canned counterpart lines below assume **no** `OPENAI_API_KEY`. Scores and moods still match if the key is set; Dana/Sam/Priya’s *wording* may differ. Prefer filming **without** the key so the replies in this brief appear on screen.

---

## One-sitting checklist (not on camera)

Do this once, then record. Target: setup 5–8 min, take 1–2.

1. Start FastAPI (bind all interfaces):

   ```bash
   .venv/bin/uvicorn app.main:app --app-dir server --host 0.0.0.0 --port 3001 --reload
   curl -s http://localhost:3001/api/health
   ```

   Expect `"status":"ok"` and `"scenarios":10`. Engine `"builtin"` is what this script was timed against.

2. **Android (preferred)**

   - Emulator: `cd client/HardTalkAi && ./gradlew :androidApp:installDebug`  
     Debug API is already `http://10.0.2.2:3001`.
   - Physical phone: same command with your LAN IP, uvicorn still on `0.0.0.0`:

     ```bash
     ./gradlew :androidApp:installDebug -Phardtalk.apiBaseUrl=http://192.168.1.10:3001
     ```

3. **Web fallback:** `pnpm --filter @hardtalkai/web dev` → `http://localhost:5173`.

4. Paste the [clipboard pack](#clipboard-pack-paste-do-not-type-live) into three notes / a doc, in order. Recording time is lost to typing.

5. Do Not Disturb. Portrait **9:16** (phone) or landscape **16:9** (emulator window). Start *after* the splash spinner. The catalog no longer shows `API: http://…` — if you still see it, you are on an old build.

6. **30-second dry run (no camera):** catalog should show **Free practice** on raise and **Pro** on Sam. Tap Sam → **Unlock HardTalk Pro** sheet. Dismiss **Not now**. Open raise → paste Line 1 → confirm Dana **guarded** and Assertiveness near **0**. If that fails, FastAPI is the wrong catalog or the line got edited.

---

## Clipboard pack (paste, do not type live)

Copy these four blocks exactly. Punctuation matters for scores.

**Line 1 — Dana, hedged (turn 1, must look weak)**

```
Sorry to bother — maybe I just deserve more if that's okay?
```

**Line 2 — Dana, strong (turn 2)**

```
I appreciate you making time. I'd like a raise: I shipped the payments rewrite and cut latency 40%.
```

**Line 3 — Dana, numbered closer (turn 3 → recap)**

```
I'd like 12% or a band bump this cycle. I understand the freeze — what number can you walk into Friday with?
```

**Line 4 — Sam (one turn after recap)**

```
I hear that payments was late. The last two checkout dates still missed, and on-call is covering it. I'd like a weekly check so this doesn't repeat.
```

**Optional Line 5 — Priya (only if the recap+Sam beat finishes before 1:50)**

```
I know leadership loved the demo. I can't take the dashboard this sprint — payments cutover is already the commitment. I'd like you to find another owner, or slip it off Friday's readout.
```

---

## Shot clock (A-roll, ≤2:00)

Voiceover is optional. If you skip VO, the UI still tells the story — just pause a beat on the badge, the guarded scores, and the recap.

| Time | You do | Say (VO) | Must be on screen |
| --- | --- | --- | --- |
| 0:00–0:08 | Catalog is already open. Do not scroll yet. | “HardTalkAI is career coaching with Coach Heather. Practice the talk before the 1:1.” | Title **HardTalk AI**, subtitle **Career coaching · Coach Heather**, lede **Raise is free; unlock the rest with HardTalk Pro.** |
| 0:08–0:18 | Tap **Give a teammate critical feedback**. Hold the sheet. Tap **Not now**. | “Ten manager drills. Raise is free. The rest is HardTalk Pro.” | **Pro** chip on Sam. Sheet title **Unlock HardTalk Pro**. Copy that the raise stays free. Buttons **Unlock HardTalk Pro**, **Restore purchase**, **Not now**. |
| 0:18–0:26 | Tap raise. Stop on Dana’s opening + Coach Heather. | “Dana has ten minutes and a freeze.” | Opening: *I've got to jump to the staffing review in ten minutes…* Coach Heather **What to practice** (name a number, tie to impact, stay collaborative). Header: **Dana · Guarded · Turn 0 of 3** (or Turn 0/3 equivalent). **Free practice** already proven on the catalog. |
| 0:26–0:42 | Paste **Line 1**. Send. Hold the feedback card. | “If you hedge, the room closes.” | **Turn 1 coaching**. Overall about **31**. Assertiveness **0**. Tips about *I'd like…* and hedging. Mood: **Dana is guarded** / *defensive and cautious*. Dana: *Look, everyone thinks they deserve a raise… packet as no-change…* |
| 0:42–1:00 | Paste **Line 2**. Send. Hold scores + mood change. | “Retry with impact and a clear ask.” | **Turn 2 coaching**. Overall about **63**. Clarity **80**, Empathy **57**, Assertiveness **52**, green deltas. Mood: **Dana is opening up**. Dana: *That's a case I can take upstairs… freeze on the packet…* Sparkline T1 → T2. |
| 1:00–1:16 | Paste **Line 3**. Send. Wait for recap — do not tap anything. | “Third turn ends the round.” | Composer gone. Screen title **Round complete**. **How scores moved** **31 → 67** (overall **+36**). Sparkline T1·31 / T2·63 / T3·67. **Biggest lift: Assertiveness +52**. **Still to practice: Assertiveness**. |
| 1:16–1:28 | Scroll the recap once. Stop on takeaway + CTAs. | “Practice, score, retry — not a chatbot.” | **What to try next:** *Assertiveness improved (0 → 52) but is still the limiter. Lead with a clear "I'd like…" so the ask cannot be missed.* Buttons **Try this scenario again** and **Choose another scenario**. |
| 1:28–1:36 | Tap **Choose another scenario**. Hold the catalog on Sam’s **Pro** chip. Do **not** open Sam unless HardTalk Pro is already unlocked. | “Same catalog, paid drills stay behind Pro.” | Catalog with **Free practice** on raise and **Pro** on Sam / Priya. |
| 1:36–1:52 | Freeze on the catalog. Optional: if Pro is unlocked, tap Sam, paste **Line 4**, send. | “Name the misses, acknowledge the constraint, land a next step.” | Default: Pro chips readable. If unlocked: **Turn 1 coaching** overall about **60** (Clarity **70** / Empathy **57** / Assertiveness **52**). Sam **opening up**. |
| 1:52–2:00 | Stop recording. | “HardTalkAI — rehearse the 1:1 before you’re in it.” | Catalog (or Sam scores if Pro was unlocked). |

**If you are under 1:50 and Pro is unlocked:** tap **Say no to extra work**, paste **Line 5**, send, freeze on Priya **opening up** (overall about **67**). Cut by 2:15. This is extra; judges already saw the loop and the paywall.

**If you are over time:** skip Priya. Recap + Pro catalog is the product.

---

## What “good” looks like (builtin engine)

Verified against `POST /api/chat` with `engine: builtin`. Use this as the dry-run answer key. Off by a point or two is fine; **guarded then opening up** is not optional.

### Dana — three-turn round

| Turn | Line | Overall | C / E / A | Tone | Counterpart (builtin) |
| --- | --- | --- | --- | --- | --- |
| 1 | Hedged | 31 | 46 / 47 / **0** | guarded | “Look, everyone thinks they deserve a raise. I already filed the packet as no-change…” |
| 2 | Strong | 63 | 80 / 57 / 52 | warming | “That's a case I can take upstairs. I still have a freeze on the packet…” |
| 3 | Numbered closer | 67 | 80 / 69 / 52 | warming | “Okay — those numbers are real, and eighteen months at the same band…” |

Recap takeaway (Android): assertiveness lifted **0 → 52** and is still the limiter.

### Sam — one turn

Overall **60**, warming. Builtin: “Thanks for saying it that way — that's fair. The last two dates did slip…”

### Priya — optional one turn

Overall **67**, warming. Builtin: “Ah — I hear you that payments is already the sprint…”

---

## Web fallback (if Android is down)

Web is the same catalog and the same scores. It is **not** the same loop:

- No 3-turn cap, no **Round complete**, no **Try this scenario again**.
- Still show **Free practice** on raise. Web is **not** paywalled — say that in the description. After Line 3, VO: “That’s a three-turn round. On Android you get a recap here, and Pro drills sit behind RevenueCat.” Then click **← Choose another scenario**.

Say in the video description that the judged recap UI is Android. Do not pretend web has the recap screen.

---

## Do not film

- The default camera path is **text rehearsal** (no mic). Do not film Call / microphone permission unless `OPENAI_API_KEY` is set and you explicitly want that extra shot. Call is the counterpart, not Coach Heather.
- A free-form chat that is not one of the three demo drills.
- Opening calibration / skip-level / stolen-credit just to flex catalog length. Ten cards plus the paywall in the first 18 seconds is enough.
- Play Store, settings, Privacy policy (listing shots are a different checklist). **Do** film the HardTalk Pro sheet.
- Failed “is FastAPI running?” error. Fix API, then record.
- The debug API URL (old builds only — current catalog hides it).
- Hedged Line 1 that accidentally includes `I'd like` or a number — that will not go guarded.
- iOS. Android is the end-to-end path this week.

---

## Export

- Length: **90–120 seconds** A-roll. Optional Priya can push to ~2:10; trim silence, not the recap.
- File: `hardtalkai-shipaton-demo.mp4` (H.264).
- Description paste for Devpost / YouTube:

  > HardTalkAI — Career Coaching (Coach Heather). Rehearse asking Dana for a raise (free drill): hedge, get scored, retry with a number, recap. HardTalk Pro unlocks the rest of the catalog (RevenueCat / Play Billing). Practice → score → retry. Default demo is typed rehearsal; Android can optionally call the counterpart when OpenAI is configured.

- Still grab **Play screenshots** from `docs/play-store-listing.md` in a later sitting if needed. This recording is the **demo**, not the store shot list.
