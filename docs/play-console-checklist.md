# Play Console checklist (when access lands)

Play Console is **not** available yet. Nothing here requires Play credentials in the repo. RevenueCat Android wiring lives in the app + [`revenuecat-android.md`](revenuecat-android.md).

Privacy text, listing copy, and the screenshot shot list already live in:

- `docs/privacy-policy.md` / `docs/privacy-policy.html`
- `docs/play-store-listing.md`

---

## 0. Ashish-only (blocked on a human)

Do these before or while filling the console. The agent cannot.

- [ ] Play developer account (one-time registration) and **app created** named HardTalkAI
- [ ] Confirm **application id** `ai.hardtalk.source` — or change it in Gradle **before** the first AAB upload (permanent)
- [ ] Create an **upload keystore** on your machine. Do **not** commit `.jks` / passwords / Play JSON keys
- [ ] Host the privacy policy on **public HTTPS** (GitHub Pages from `/docs`, or FastAPI `/privacy` on a public host). Blob URL is a stopgap: `https://github.com/Ashishsingh009/HardTalkAI/blob/main/docs/privacy-policy.md`
- [ ] Confirm `hello@hardtalk.ai` is a real inbox, or use `aashish2k2@gmail.com` in the listing
- [ ] Capture Android screenshots from the [shot list](play-store-listing.md#screenshot-shot-list-capture-on-android)
- [ ] Export **512×512** icon and **1024×500** feature graphic
- [ ] Production API **HTTPS** base URL for release (debug default `http://10.0.2.2:3001` is emulator-only)
- [ ] Testers’ Gmail addresses for the internal track
- [ ] Run the IARC content-rating questionnaire (answers drafted in the listing doc)

## 1. What build to upload

Play wants an **Android App Bundle**, not a debug APK.

From `client/HardTalkAi`:

```bash
./gradlew :androidApp:bundleRelease
```

Output (unsigned until you add signing):

`client/HardTalkAi/androidApp/build/outputs/bundle/release/androidApp-release.aab`

Today `androidApp/build.gradle.kts` has **no release signing config** (`versionCode 1`, `versionName 1.0`, `applicationId ai.hardtalk.source`). `bundleRelease` will not be Play-acceptable until Ashish wires an upload key (or Play App Signing with an upload keystore).

Do **not** upload `assembleDebug` / emulator APKs.

Release should **not** ship `usesCleartextTraffic` toward production. Point `hardtalk.apiBaseUrl` at the HTTPS API when you cut the store binary, for example:

```bash
./gradlew :androidApp:bundleRelease -Phardtalk.apiBaseUrl=https://YOUR_HOST
```

(That Gradle property already feeds `BuildConfig.API_BASE_URL`.)

Min SDK is whatever the version catalog says (API 24+). No separate iOS store build in this lane.

## 2. Internal testing track (first upload)

1. Play Console → the HardTalkAI app → **Testing → Internal testing**
2. Create a release (“Internal 1” / `1 (1.0)`)
3. Upload the **signed** AAB from step 1
4. Release notes (internal):

   ```
   First internal build: career catalog, scored practice round, recap. Raise is free; other drills use HardTalk Pro (RevenueCat).
   ```

5. **Testers** tab → email list (or Google Group) → add Ashish + whoever should install
6. Copy the **opt-in URL**, send it to testers (they need the Play Store app and the same Google account)
7. Testers may need hours after the first AAB for processing; “not found” usually means the account is not on the list or the release is still in review/processing

Internal testing still needs a **package name**, **AAB**, and usually **App signing**. Store listing can stay incomplete for a while, but **privacy policy URL** and **Data safety** block production and often block wider tracks.

## 3. Console fields to paste (no credentials in git)

| Console section | Source |
| --- | --- |
| Store listing name / short / full | `docs/play-store-listing.md` |
| Privacy policy URL | hosted `docs/privacy-policy.html` or GitHub markdown |
| Data safety | cheat-sheet in the listing doc (messages to API; optional OpenAI; no ads/sale) |
| Content rating | IARC notes in the listing doc |
| Category | Education (or Business) |
| Ads declaration | **No** |
| App access | No restrictions / no login |
| Target age | Adult career coaching; follow questionnaire output |
| News / COVID / data-safety extras | No, unless a form forces a click |

In-app **Privacy** (web header + Android catalog) already points at this policy. After you host Pages, you can change `LegalLinks.PRIVACY_POLICY_URL` to the Pages URL.

## 4. RevenueCat / Play Billing (D3)

Client code is in. Ashish still has to create dashboard objects (cannot be done from git):

- Entitlement id **`pro`**
- Product id **`hardtalkai_pro_monthly`** (monthly)
- Current offering **`default`**, package **`$rc_monthly`**
- Test Store public key in `client/HardTalkAi/local.properties` as `revenuecat.androidApiKey`
- Later: Play subscription with the same product id + `goog_` key as `revenuecat.playApiKey`

Full steps: [`revenuecat-android.md`](revenuecat-android.md). Do **not** put API keys, Play JSON, or keystores in git.

## 5. Explicitly out of scope

- **Play API keys, service accounts, `google-services.json`, keystores** — do not put them in the repo.
- Production rollout / closed–open testing — after internal testers can install.
- iOS App Store / iOS RevenueCat — not this drop.

## 6. Smoke the internal build

- Install from the internal-track link (not sideload)
- Catalog shows ~10 career drills and **Free practice** on the raise
- Locked cards open the Pro paywall; raise still starts without a purchase
- Complete one 3-turn round → recap
- Open **Privacy** from the catalog; policy loads
- Confirm the binary talks to the **HTTPS** API, not `10.0.2.2`
