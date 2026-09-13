# Host FastAPI on Hugging Face Spaces (free)

Yes. This repo’s coaching API is a **Docker Space**: one container, no database, HTTPS included. Free **CPU basic** hardware is enough (no GPU).

The Space iframe opens `/`, which already serves the HardTalkAI HTML home. Android/web call `/api/health`, `/api/scenarios`, and `/api/chat`. Privacy is at `/privacy`.

## 1. Create the Space

1. Open [huggingface.co/new-space](https://huggingface.co/new-space) (sign in with Hugging Face).
2. **Space name:** `HardTalkAI` (or similar).
3. **SDK:** **Docker** (not Gradio / Streamlit).
4. **Hardware:** CPU basic (free).
5. **Visibility:** Public (needed for Play privacy URL and the Android app).
6. Create the Space.

## 2. Put this repo in the Space

**Option A — GitHub (simplest)**  
Space **Settings → Connected GitHub repository** → `Ashishsingh009/HardTalkAI` → pick the branch that has this `Dockerfile` → save. HF rebuilds on push.

**Option B — push from this clone**

```bash
git remote add hf https://huggingface.co/spaces/YOUR_HF_USER/HardTalkAI
git push hf HEAD:main
```

Use a [Hugging Face access token](https://huggingface.co/settings/tokens) with **Write** if Git asks for a password.

If the Space README has no Docker YAML yet, paste this at the **top** of the Space `README.md`:

```yaml
---
title: HardTalkAI
emoji: ✈️
colorFrom: blue
colorTo: indigo
sdk: docker
app_port: 7860
pinned: false
---
```

`app_port: 7860` must match the Dockerfile `EXPOSE` / uvicorn port.

## 3. Optional OpenAI secret

Space **Settings → Variables and secrets → New secret**:

- Name: `OPENAI_API_KEY`
- Value: your key

Do **not** bake the key into the Dockerfile. Runtime secrets become env vars; FastAPI already reads `OPENAI_API_KEY`. Without it, builtin replies still work.

## 4. Wait for the build

Logs should end with `Uvicorn running on http://0.0.0.0:7860`.

Public app host (not the huggingface.co/spaces page):

```text
https://YOUR_HF_USER-HardTalkAI.hf.space
```

HF also serves the same app at:

```text
https://huggingface.co/spaces/YOUR_HF_USER/HardTalkAI
```

Check:

```bash
curl -s https://YOUR_HF_USER-HardTalkAI.hf.space/api/health
curl -sI https://YOUR_HF_USER-HardTalkAI.hf.space/privacy
```

Expect `"status":"ok"` and HTML for privacy.

## 5. Point Android at the Space

```bash
cd client/HardTalkAi
./gradlew :androidApp:installDebug \
  -Phardtalk.apiBaseUrl=https://YOUR_HF_USER-HardTalkAI.hf.space
```

No trailing slash. Must be `https://`.

Play privacy URL:

```text
https://YOUR_HF_USER-HardTalkAI.hf.space/privacy
```

Web (dev, proxy API to the Space):

```bash
VITE_API_TARGET=https://YOUR_HF_USER-HardTalkAI.hf.space pnpm --filter @hardtalkai/web dev
```

## Notes

- Free Spaces **sleep** after idle; the first request can take ~30–60s.
- CORS is already `*` on this API, so the web app can call the Space origin.
- Rebuild after Dockerfile or `server/` changes. Secrets can be changed without a rebuild.
- Do not use Gradio/Streamlit SDK for this app; it is FastAPI, not a Gradio demo.
