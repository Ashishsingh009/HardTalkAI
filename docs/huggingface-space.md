# Host HardTalkAI on Hugging Face Spaces

Hugging Face **2026 hardware policy:** Static Spaces are free for everyone.
**Gradio and Docker Spaces on `cpu-basic` require PRO.** A new personal account
also cannot create ZeroGPU Gradio Spaces until it is verified and older than
30 days (or you subscribe). Creating those SDKs as `BhaiAshish` currently
returns HTTP **402**.

## Live Static Space (this account)

Public demo + privacy page, scoring in the browser (same builtin engine as FastAPI):

```text
https://huggingface.co/spaces/BhaiAshish/HardTalkAI
https://bhaiashish-hardtalkai.static.hf.space
https://bhaiashish-hardtalkai.static.hf.space/privacy.html
```

Source for that Space lives in `spaces/hardtalkai-static/`. Re-upload:

```bash
hf upload BhaiAshish/HardTalkAI spaces/hardtalkai-static --repo-type space \
  --exclude "**/__pycache__/**" --exclude "score_message.py"
```

This Static Space **cannot** serve `/api/health`, `/api/scenarios`, or `/api/chat`.
Android still needs a FastAPI origin.

Play privacy URL while FastAPI is unhosted:

```text
https://bhaiashish-hardtalkai.static.hf.space/privacy.html
```

## Docker Space (FastAPI) — needs PRO

The repo root `Dockerfile` is the FastAPI host: Python 3.12, uid 1000, uvicorn
on port **7860**, copies `server/` + `docs/`. Use this when the Hugging Face
account can pay for `cpu-basic`.

1. Create a **new** Space (do not overwrite the Static demo), e.g. `HardTalkAI-api`.
2. SDK: **Docker**. Hardware: CPU basic. Public.
3. Upload this git repo (or connect GitHub). Space README frontmatter:

```yaml
---
title: HardTalkAI API
emoji: ✈️
colorFrom: blue
colorTo: indigo
sdk: docker
app_port: 7860
pinned: false
---
```

4. Optional secret: `OPENAI_API_KEY`. Without it, builtin replies still work.
5. Check:

```bash
curl -s https://BhaiAshish-HardTalkAI-api.hf.space/api/health
curl -sI https://BhaiAshish-HardTalkAI-api.hf.space/privacy
```

Expect `"status":"ok"` and HTML for privacy.

Point Android at the Docker host (no trailing slash, must be `https://`):

```bash
cd client/HardTalkAi
./gradlew :androidApp:installDebug \
  -Phardtalk.apiBaseUrl=https://BhaiAshish-HardTalkAI-api.hf.space
```

Web (dev, proxy API to the Space):

```bash
VITE_API_TARGET=https://BhaiAshish-HardTalkAI-api.hf.space pnpm --filter @hardtalkai/web dev
```

## Notes

- Paid Spaces **sleep** after idle; the first request can take ~30–60s.
- CORS is already `*` on the FastAPI app.
- Rebuild Docker after `Dockerfile` or `server/` changes. Secrets can change without a rebuild.
- Gradio/Streamlit SDKs are the wrong shape for this FastAPI API. The Static Space is the free public demo.
