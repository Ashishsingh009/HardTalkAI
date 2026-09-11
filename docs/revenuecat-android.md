# RevenueCat Android (Shipaton D3)

HardTalkAI’s **free tier is one scenario**: `ask-for-raise` (`free: true` in `GET /api/scenarios`).
**HardTalk Pro** is a **monthly** subscription. The Android client unlocks the rest of the
catalog when RevenueCat reports entitlement `pro` as active.

FastAPI does **not** enforce the gate. Every scenario stays callable on the server and on web.
The Android app is the paywall.

iOS is not wired to RevenueCat in this drop. The shared UI still shows locks; purchase is
Android-only.

---

## 1. Put the public SDK key on your machine (do not commit it)

Copy the example file and add the Test Store public SDK key (the `test_…` key from the D3 brief
or RevenueCat → **Project settings → API keys**):

```bash
cd client/HardTalkAi
cp local.properties.example local.properties
# then set sdk.dir and:
# revenuecat.androidApiKey=test_YOUR_TEST_STORE_KEY
```

`local.properties` is gitignored. Alternatives that also feed `BuildConfig.REVENUECAT_API_KEY`:

| Source | Debug | Release |
| --- | --- | --- |
| `revenuecat.androidApiKey` in `local.properties` or `-P` | yes | ignored |
| `REVENUECAT_ANDROID_API_KEY` env | yes | ignored |
| `revenuecat.playApiKey` / `REVENUECAT_PLAY_API_KEY` | unused | yes (`goog_…` only) |

Debug builds inject the Test Store key. Release builds inject the Play key. A `test_` key is
**stripped from release** so you cannot accidentally ship Test Store to Play.

Never commit the raw key. Never submit a Play AAB that still has a `test_` key.

---

## 2. Dashboard setup Ashish must create (cannot be done from app code)

Use **exactly** these identifiers — they are hardcoded in
`ai.hardtalk.source.domain.billing.ProEntitlement`.

### Entitlement

| Field | Value |
| --- | --- |
| Identifier | `pro` |
| Attach | the monthly product below |

### Product (Test Store first, then Play)

| Field | Value |
| --- | --- |
| Product id | `hardtalkai_pro_monthly` |
| Type | Subscription |
| Duration | 1 month |
| Store | RevenueCat **Test Store** for debug; **Google Play** for release |

### Offering / package

| Field | Value |
| --- | --- |
| Offering id | `default` (and mark it **Current**) |
| Package | `$rc_monthly` (Monthly) |
| Package product | `hardtalkai_pro_monthly` |

### RevenueCat clicks

1. Project → **Apps and providers** → enable **Test Store** if it is not already on.
2. Copy the Test Store public API key (`test_…`) into `local.properties` as
   `revenuecat.androidApiKey`.
3. **Product catalog → Products** → New product → id `hardtalkai_pro_monthly`, monthly
   subscription, Test Store.
4. **Product catalog → Entitlements** → New entitlement → id `pro` → attach
   `hardtalkai_pro_monthly`.
5. **Product catalog → Offerings** → `default` → add package **Monthly** (`$rc_monthly`)
   pointing at `hardtalkai_pro_monthly` → make this offering **Current**.

If the current offering has no monthly package, the paywall shows “Price unavailable” and
the free raise drill still works.

### Google Play product (when Console exists)

1. Play Console → HardTalkAI (`ai.hardtalk.source`) → **Monetize → Subscriptions**.
2. Create subscription product id **`hardtalkai_pro_monthly`** (must match).
3. Add a **base plan** billed every 1 month (id can be `monthly`).
4. Activate the product / base plan.
5. In RevenueCat, add a **Play Store** app with the same package name, paste the
   Play service-account JSON (never commit it), and import/link product
   `hardtalkai_pro_monthly` to entitlement `pro` and offering `default` / `$rc_monthly`.
6. Copy the Play public SDK key (`goog_…`) into `revenuecat.playApiKey` for **release**
   builds.

Play products cannot be created from this repo.

---

## 3. How the Android gate behaves

| User | Catalog |
| --- | --- |
| Unpaid / RC offline / missing key | **Ask your manager for a raise** starts. Other cards show **Locked** and open the paywall. |
| Entitlement `pro` active | Every drill starts. Header shows **Pro**. |

Practice → score → retry is unchanged. There is no open chat.

Offline / sandbox:

- Missing key or `Purchases.configure` failure → app still launches; free drill works.
- Offerings fetch fails → paywall explains it; **Restore** still tries; free drill works.
- Cached `CustomerInfo` from a previous Test Store purchase still unlocks Pro without a
  network round-trip when the SDK has a cache.

---

## 4. Test with the Test Store (sandbox)

1. Start FastAPI: `.venv/bin/uvicorn app.main:app --app-dir server --host 0.0.0.0 --port 3001 --reload`
2. `cd client/HardTalkAi && ./gradlew :androidApp:installDebug`
3. Open the catalog. Raise is **Free practice**. Tap a locked card → paywall with the
   monthly price from the offering.
4. **Subscribe**. Test Store shows a sheet: **Successful Purchase** / **Failed Purchase** /
   **Cancel**. Choose success → catalog unlocks.
5. **Restore purchases** after reinstall (same anonymous id on the device, or after
   `Purchases.logIn` if you later add accounts).
6. Kill network / airplane mode: raise still starts; locked cards stay locked unless Pro
   was already cached.

Do **not** use the Test Store key in a Play-uploaded AAB.

---

## 5. Code map

| Piece | Where |
| --- | --- |
| Entitlement id / product id | `shared/.../domain/billing/ProEntitlement.kt` |
| Gate | `Scenario.isPlayable`, `ScenarioListScreen`, `App` |
| Paywall UI | `shared/.../presentation/paywall/` |
| RevenueCat SDK | `:androidApp` (`purchases` 10.15.1) |
| Configure | `HardTalkApplication` → `RevenueCatEntitlementRepository.create` |
| Key → BuildConfig | `androidApp/build.gradle.kts` from `local.properties` |
