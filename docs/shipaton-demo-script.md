# Shipaton demo script — HardTalkAI (≤2 min)

**Lane:** Career Coaching / Leadership (Coach Heather)  
**Who films:** Ashish, one sitting, phone or emulator.  
**Product to show:** practice → score → retry (not a chatbot), then the **HardTalk Pro / RevenueCat** gate.  
**This file is the camera brief.** Do not improvise new scenarios. Do not wait for OpenAI. **Do film HardTalk Pro / RevenueCat** — judges need monetization on camera.

Record **Android** if you have an emulator or device. Recap (`Round complete`) and the **PaywallScrim** only exist there. Fall back to **web** at `http://localhost:5173` only if Android is down — see [Web fallback](#web-fallback-if-android-is-down). Web may not show the same Pro gate; prefer Android for the judged monetization shot.

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

2. **Android (preferred)** — billing must be live, not ungated.

   - Put the RevenueCat **Android / Google public SDK key** in `client/HardTalkAi/local.properties` as `hardtalk.revenuecatGoogleApiKey` (or the matching Gradle property). Purchases must be configured.
   - Play product + entitlement `hardtalk_pro` (`HardTalkBilling.ENTITLEMENT_PRO`) must be live for **sandbox**. Copy in `PaywallScrim` is a **one-time Play purchase**, not a subscription tier.
   - Film with **`hasPro=false`** and **`ungated=false`**. If everything is unlocked, the paywall will not show — fix billing state before recording. An empty debug SDK key keeps the catalog ungated; that is the wrong film state.
   - Emulator: `cd client/HardTalkAi && ./gradlew :androidApp:installDebug`  
     Debug API is already `http://10.0.2.2:3001`.
   - Physical phone: same command with your LAN IP, uvicorn still on `0.0.0.0`:

     ```bash
     ./gradlew :androidApp:installDebug -Phardtalk.apiBaseUrl=http://192.168.1.10:3001
     ```

3. **Web fallback:** `pnpm --filter @hardtalkai/web dev` → `http://localhost:5173`. Use only if Android is down. Pro / paywall is Android + RevenueCat; web may leave every card playable.

4. Paste the [clipboard pack](#clipboard-pack-paste-do-not-type-live) into notes / a doc, in order. Recording time is lost to typing. Path A needs Line 4; Line 5 is extra.

5. Do Not Disturb. Portrait **9:16** (phone) or landscape **16:9** (emulator window). Start *after* the splash spinner. Crop or ignore the catalog debug line `API: http://…` (still in `ScenarioListScreen`).

6. **Dry run (no camera):**
   - Open raise → paste Line 1 → confirm Dana **guarded** and Assertiveness near **0**. If that fails, FastAPI is the wrong catalog or the line got edited.
   - Back to catalog: raise stays playable (**Free practice**). Tap locked **Give a teammate critical feedback** (Sam) and confirm **Unlock HardTalk Pro** opens. If Sam opens the chat instead, you are Pro or ungated — stop and fix billing.

---

## Clipboard pack (paste, do not type live)

Copy these blocks exactly. Punctuation matters for scores.

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

**Line 4 — Sam (Path A only: one turn after a real sandbox unlock)**

```
I hear that payments was late. The last two checkout dates still missed, and on-call is covering it. I'd like a weekly check so this doesn't repeat.
```

**Optional Line 5 — Priya (only if Path A finishes before ~1:50)**

```
I know leadership loved the demo. I can't take the dashboard this sprint — payments cutover is already the commitment. I'd like you to find another owner, or slip it off Friday's readout.
```

---

## Shot clock (A-roll, ≤2:00)

Voiceover is optional. If you skip VO, the UI still tells the story — pause on the **Free practice** chip, a **Pro** chip, the guarded scores, the recap, and the paywall.

Tighten Dana if the Pro beat is slipping. **The HardTalk Pro / RevenueCat hold is required**, not optional filler. Do not fake a purchase off-camera.

| Time | You do | Say (VO) | Must be on screen |
| --- | --- | --- | --- |
| 0:00–0:06 | Catalog is already open. Do not scroll yet. | “HardTalkAI is career coaching with Coach Heather. Practice the talk before the 1:1.” | Title **HardTalk AI**, subtitle **Career coaching · Coach Heather**, scored-drill lede. When not Pro: **N manager conversations. Raise is free; unlock the rest with HardTalk Pro.** |
| 0:06–0:16 | Hold the first card. Finger or cursor on **Free practice**. Briefly show the next 1–2 titles so a **Pro** chip is readable (Sam and/or Priya). Do not open them yet. | “Ten manager drills. Raise is free practice; the rest is HardTalk Pro.” | **Ask your manager for a raise** · **Free practice** chip · **You'll talk to Dana**. Next card **Give a teammate critical feedback** with a **Pro** chip (Priya **Say no to extra work** also **Pro** if visible). |
| 0:16–0:22 | Tap raise. Stop on Dana’s opening + Coach Heather. | “Dana has ten minutes and a freeze.” | Opening: *I've got to jump to the staffing review in ten minutes…* Coach Heather **What to practice** (name a number, tie to impact, stay collaborative). Header: **Dana · Guarded · Turn 0 of 3**. |
| 0:22–0:36 | Paste **Line 1**. Send. Hold the feedback card. | “If you hedge, the room closes.” | **Turn 1 coaching**. Overall about **31**. Assertiveness **0**. Tips about *I'd like…* and hedging. Mood: **Dana is guarded** / *defensive and cautious*. Dana: *Look, everyone thinks they deserve a raise… packet as no-change…* |
| 0:36–0:50 | Paste **Line 2**. Send. Hold scores + mood change. | “Retry with impact and a clear ask.” | **Turn 2 coaching**. Overall about **63**. Clarity **80**, Empathy **57**, Assertiveness **52**, green deltas. Mood: **Dana is opening up**. Dana: *That's a case I can take upstairs… freeze on the packet…* Sparkline T1 → T2. |
| 0:50–1:04 | Paste **Line 3**. Send. Wait for recap — do not tap anything. | “Third turn ends the round.” | Composer gone. Screen title **Round complete**. **How scores moved** **31 → 67** (overall **+36**). Sparkline T1·31 / T2·63 / T3·67. **Biggest lift: Assertiveness +52**. **Still to practice: Assertiveness**. |
| 1:04–1:12 | Scroll the recap once. Stop on takeaway + CTAs. | “Practice, score, retry — not a chatbot.” | **What to try next:** *Assertiveness improved (0 → 52) but is still the limiter. Lead with a clear "I'd like…" so the ask cannot be missed.* Buttons **Try this scenario again** and **Choose another scenario**. |
| 1:12–1:24 | Tap **Choose another scenario**. Tap locked **Give a teammate critical feedback** (or any **Pro** card). **Hold the paywall.** | “Raise stays free. HardTalk Pro unlocks the rest — a one-time Play purchase through RevenueCat.” | **PaywallScrim:** title **Unlock HardTalk Pro**. Body like `"Give a teammate critical feedback" is a Pro drill. The raise stays free. Unlock the rest of the catalog with a one-time Play purchase.` Primary CTA **Unlock for $X** or **Unlock HardTalk Pro**. **Restore purchase**. Dismiss / **Not now**. Name **RevenueCat** once here or in the Devpost / YouTube blurb. |

**Path A (preferred if sandbox purchase works)** — stay in the same take.

| Time | You do | Say (VO) | Must be on screen |
| --- | --- | --- | --- |
| 1:24–1:36 | Complete **Unlock** on camera. Wait until Sam’s chat actually opens. | “Same loop, now unlocked.” | Purchase finishes. Catalog lock lifts. Sam’s opening: *If this is about checkout, I already told Dana the payments API was late…* |
| 1:36–1:52 | Paste **Line 4**. Send. Hold Sam’s reply + scores. | “Name the misses, acknowledge the constraint, land a next step.” | **Turn 1 coaching** overall about **60** (Clarity **70** / Empathy **57** / Assertiveness **52**). Sam **opening up**. Reply about the last two dates and a weekly check. |
| 1:52–2:00 | Freeze on Sam’s scores. Stop recording. | “HardTalkAI — rehearse the 1:1 before you’re in it.” | Feedback bars still readable. |

**Path B (if the sandbox purchase is flaky)** — do not fake Unlock off-camera.

| Time | You do | Say (VO) | Must be on screen |
| --- | --- | --- | --- |
| 1:24–1:32 | Freeze on **Unlock HardTalk Pro** for **4–6 seconds**. Dismiss is optional. | “Free raise; Pro unlocks the rest on Play.” | Full paywall readable: title, Pro-drill body, primary CTA, **Restore purchase**. |
| 1:32–2:00 | Crisp close. Stop. Do not cut to an already-unlocked Sam from a previous session. | “HardTalkAI — rehearse the 1:1 before you’re in it.” | Paywall or catalog with **Free practice** + **Pro** chips still in frame. |

**If Path A finishes before ~1:50:** tap **← Scenarios**, open **Say no to extra work**, paste **Line 5**, send, freeze on Priya **opening up** (overall about **67**). Cut by 2:15. This is extra; judges already saw the loop and the paywall.

**If you are over time:** skip Priya. Recap + paywall (Path A or B) is the product. Never drop the Pro beat to squeeze in Sam without a real unlock.

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

### Sam — one turn (Path A only)

Overall **60**, warming. Builtin: “Thanks for saying it that way — that's fair. The last two dates did slip…”

### Priya — optional one turn

Overall **67**, warming. Builtin: “Ah — I hear you that payments is already the sprint…”

---

## Web fallback (if Android is down)

Web is the same catalog and the same scores. It is **not** the same loop, and it is **not** the judged monetization path:

- No 3-turn cap, no **Round complete**, no **Try this scenario again**.
- After Line 3, VO: “That’s a three-turn round. On Android you get a recap here.” Then click **← Choose another scenario**.
- **HardTalk Pro / PaywallScrim is Android + RevenueCat.** Web may not lock Sam/Priya or show **Unlock HardTalk Pro**. Do not pretend the web catalog is the Play / RevenueCat gate.
- Still show **Free practice** on raise, Line 1 guarded, Line 2–3 warming. If web has no paywall, say so in the video description and film Android as soon as you can.

Say in the video description that the judged recap **and** the HardTalk Pro beat are Android. Do not pretend web has the recap screen or the RevenueCat paywall.

---

## Do not film

- Voice input, microphone permission, or “talking to” the phone. This build is **text rehearsal**.
- A free-form chat that is not one of the demo drills.
- Opening calibration / skip-level / stolen-credit just to flex catalog length. The raise card + one **Pro** chip in the first 16 seconds is enough.
- A fake purchase: do not unlock off-camera, then cut back as if Path A happened live.
- Settings, Privacy policy, or Play Console (listing shots are a different checklist). The **in-app** paywall *is* the monetization shot.
- Failed “is FastAPI running?” error. Fix API, then record.
- Hedged Line 1 that accidentally includes `I'd like` or a number — that will not go guarded.
- An ungated / already-Pro catalog. If every card opens, you are not filming the product judges asked for.
- iOS. Android is the end-to-end path this week.

---

## Export

- Length: **90–120 seconds** A-roll. The Pro beat is not optional filler. Optional Priya (Path A only) can push to ~2:10; trim silence, not the recap or the paywall hold.
- File: `hardtalkai-shipaton-demo.mp4` (H.264).
- Description paste for Devpost / YouTube:

  > HardTalkAI — Career Coaching (Coach Heather). Rehearse asking Dana for a raise (free drill): hedge, get scored, retry with a number, recap. Then HardTalk Pro via RevenueCat — locked drills show a Pro chip and Unlock HardTalk Pro (one-time Play purchase); the raise stays free. Path A: unlock and one turn of critical feedback with Sam. Practice → score → retry. Text rehearsal prototype; counterpart replies are on-device canned unless OpenAI is configured.

- Still grab **Play screenshots** from `docs/play-store-listing.md` in a later sitting if needed. This recording is the **demo**, not the store shot list.
