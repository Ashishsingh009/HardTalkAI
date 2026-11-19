# Move FastAPI off Hugging Face Spaces

Week 1 write-up for Play closed testing. No infra is changed in this PR.

The coaching API still lives in `server/` (FastAPI + uvicorn). The Hugging Face
**Static** Space (`spaces/BhaiAshish/HardTalkAI`) can keep serving the public
demo and privacy HTML. It cannot serve `/api`. The Docker FastAPI Space on this
free account returns **HTTP 402** (`docs/huggingface-space.md`). The privacy
policy says a **store build must call an HTTPS API host**, not debug
`http://10.0.2.2:3001`.

## What the process actually needs

The app is one Python process. Coaching scores stay local. Optional outbound
HTTPS goes to OpenAI and PayPal sandbox. There is **no database today**.
Funnel analytics (`server/app/analytics.py`) is an in-memory `deque` of 2,000
events and **vanishes on restart**. There is **no RevenueCat webhook** in
FastAPI; HardTalk Pro is a client-side Play/RevenueCat gate.

### Secrets (set on the host, never in git)

| Env var | Required? | Used for |
| --- | --- | --- |
| `OPENAI_API_KEY` | No | Typed counterpart replies + Realtime Call mint. Unset → builtin replies, `voice: false`. |
| `OPENAI_MODEL` | No | Default `gpt-4o-mini`. |
| `OPENAI_REALTIME_MODEL` | No | Default `gpt-realtime`. |
| `HARDTALK_VOICE_SECRET` | For Call | 16+ chars; must match Android `local.properties` `hardtalk.voiceSecret`. |
| `HARDTALK_TRUSTED_PROXIES` | If Call is on | Exact TCP peer IPs that may set `X-Forwarded-For` (see caveat below). |
| `HARDTALK_CORS_ORIGINS` | If a browser origin calls the API | Extra CORS allowlist. Native Android does not use CORS. |
| `PAYPAL_CLIENT_ID` / `PAYPAL_CLIENT_SECRET` | No | PayPal Invoicing v2 **sandbox**. Unset → labeled mock invoice. |
| `PAYPAL_API_BASE` | No | Must contain `sandbox` or it is ignored. Live `api-m.paypal.com` is refused. |
| `VOICE_SESSION_RATE_LIMIT` / `VOICE_SESSION_RATE_WINDOW` | No | Defaults 8 requests / 60s per client key. |

Root `Dockerfile` already copies `server/` + `docs/` and listens on **7860**
(Hugging Face Spaces convention). Platforms that inject `PORT=8080` (Cloud Run,
some Render setups) need either that port mapped or a later one-line CMD change.

## Persistence

Do **not** buy managed Postgres for closed testing.

1. **Ship as-is (day 1).** In-memory analytics is honest: `/analytics` already
   says counts reset on restart. Fine while you confirm the host stays up.
2. **Next small change:** SQLite file on a 1 GB volume (`HARDTALK_ANALYTICS_PATH`)
   for the same 2,000-event log. Cost is ~$0.15–0.25/month. No schema migration
   tool needed.
3. **Skip Postgres** until you want accounts, Durable Play entitlements on the
   server, or more than one replica.

## Host comparison (solo developer in India, small budget)

Prices are list rates as of late 2026 for **one always-on small web process**.
They move; check the vendor calculator before paying.

| | **Fly.io** (recommended) | **Render Starter** | **Cloud Run** |
| --- | --- | --- | --- |
| Always-on HTTPS | Yes (`*.fly.dev`, auto cert) | Yes (`*.onrender.com`) | Yes (`*.run.app`) |
| India-ish region | **Mumbai `bom`** | Singapore (no Mumbai) | Mumbai `asia-south1` |
| Shape | `shared-cpu-1x` + 512 MB | 0.5 CPU + 512 MB | 1 vCPU + 512 MB |
| Monthly (always on) | **~$4–6** | **$7** | ~$8–15 with min instances = 1 |
| Scale-to-zero | Machines can stop | Free web **sleeps** (~1 min wake); paid does not | Default: ~$0–3 at closed-testing traffic, **5–15s cold start** |
| Volume for SQLite | ~$0.15/GB | $0.25/GB disk | Use Cloud Storage or skip |
| Ops | `flyctl` + `Dockerfile` | Dashboard + GitHub | gcloud + Artifact Registry + billing account |
| Sleep / 402 | No | Paid Starter does not sleep | No (unless scaled to zero) |

**Railway Hobby** is a close fourth: $5 plan includes $5 usage; a 512 MB
always-on service is often ~$5 before CPU. Fine if you already have an account.
No Mumbai region. Caps help, surprise usage does not.

**Recommendation:** **Fly.io, Mumbai, one `shared-cpu-1x` 512 MB Machine that
stays running**, plus a 1 GB volume when you add SQLite. Roughly **₹350–500 /
$4–6 per month**, HTTPS included, low latency from India, existing Dockerfile
works. Pick **Render Starter ($7)** if you want a GitHub-connected dashboard
and will tolerate Singapore RTT. Pick **Cloud Run scale-to-zero** only if you
accept cold starts on the first catalog fetch (bad for a Play tester who opens
the app once).

Hugging Face Docker Spaces stay a non-option on this free account (402 / PRO).

## Caveat: voice rate-limit behind a proxy

`HARDTALK_TRUSTED_PROXIES` is an **exact IP** allowlist, not CIDR. On Fly /
Render / Cloud Run the TCP peer is the platform proxy, so every Call mint
shares one 8/min bucket unless you list that proxy IP (which can change).

For closed testing: leave Call off (`HARDTALK_VOICE_SECRET` unset) **or** raise
`VOICE_SESSION_RATE_LIMIT` so a handful of testers share a bucket. A later
change should accept CIDR (or `Fly-Client-IP`). Do not blindly trust
`X-Forwarded-For` from the public internet.

## Concrete migration steps

1. Create a Fly account, install `flyctl`, `fly auth login`.
2. From the repo root (where `Dockerfile` lives):

   ```bash
   fly launch --name hardtalkai-api --region bom --no-deploy
   ```

   Set the Fly HTTP service **internal port to 7860** to match the Dockerfile.
   Do not enable extra Postgres.

3. Secrets (only what you actually use):

   ```bash
   fly secrets set OPENAI_API_KEY=sk-... HARDTALK_VOICE_SECRET='…'
   fly secrets set PAYPAL_CLIENT_ID=... PAYPAL_CLIENT_SECRET=...
   ```

   Skip PayPal secrets to keep mock invoices. Skip the voice secret to keep
   Call hidden (`voice: false` on `/api/health`).

4. Deploy and check:

   ```bash
   fly deploy
   curl -sS https://hardtalkai-api.fly.dev/api/health
   curl -sSI https://hardtalkai-api.fly.dev/privacy
   ```

   Expect `"status":"ok"` and HTML for privacy. First deploy can take a minute.

5. Optional volume (after you add SQLite):

   ```bash
   fly volumes create hardtalk_data --region bom --size 1
   ```

6. CORS: native Android needs none. If the Vite web app should hit the Fly
   origin directly, set `HARDTALK_CORS_ORIGINS=https://your-web-origin`.

7. Point the **Android release** binary at the new host (no trailing slash):

   ```bash
   cd client/HardTalkAi
   ./gradlew :androidApp:bundleRelease \
     -Phardtalk.apiBaseUrl=https://hardtalkai-api.fly.dev
   ```

   `assembleRelease` / `bundleRelease` now **fail** if that property is missing
   or not `https://`, so a store AAB cannot silently ship the emulator HTTP
   default. Debug is unchanged (`http://10.0.2.2:3001`). Physical debug
   against Fly:

   ```bash
   ./gradlew :androidApp:installDebug \
     -Phardtalk.apiBaseUrl=https://hardtalkai-api.fly.dev
   ```

8. Play Console: paste the same `https://…` origin into any “backend URL”
   notes you keep; Data safety still lists the coaching API as the destination
   for practice text. Privacy HTML is already on the Static Space; FastAPI
   `/privacy` is a second copy once the API is public.

9. After testers install: `GET /analytics` (in-memory until SQLite exists).
   Restarting the Machine still wipes counts.

Render equivalent: New Web Service → this GitHub repo → Docker → instance
Starter → set the port to 7860 → same env vars as secrets. Cloud Run
equivalent: build the Dockerfile, deploy to `asia-south1`, either override
the container command to `--port 8080` or map 7860, set min instances to 1
if you need always-on.

## Out of scope here

- Changing the Dockerfile `CMD` to honor `$PORT`
- SQLite persistence code
- CIDR-aware `HARDTALK_TRUSTED_PROXIES`
- Custom domain (`api.hardtalk.ai`) — add later with Fly certs if the domain
  inbox is real
- RevenueCat server webhooks
