---
title: HardTalkAI
emoji: 💬
colorFrom: blue
colorTo: indigo
sdk: static
pinned: false
short_description: Practice career talks with Coach Heather
---

# HardTalkAI

Practice a high-stakes manager conversation, get scored on **clarity**, **empathy**,
and **assertiveness**, then retry. Coach Heather runs a **3-turn** round — this is
practice → score → retry, not an open-ended chatbot.

This Space is a **Static** browser demo. Scoring and counterpart replies run in
your browser (same builtin engine as the FastAPI server). Nothing is posted to
a coaching API.

**Live:** [BhaiAshish/HardTalkAI](https://huggingface.co/spaces/BhaiAshish/HardTalkAI)

**Privacy:** [privacy.html](privacy.html)

**Source:** [github.com/Ashishsingh009/HardTalkAI](https://github.com/Ashishsingh009/HardTalkAI)

## Why Static, not Docker?

Hugging Face now requires **PRO** to host Gradio or Docker Spaces on `cpu-basic`.
Free accounts can always host Static Spaces. A new personal account also cannot
create ZeroGPU Gradio Spaces until it is verified and older than 30 days.

When the GitHub repo `Dockerfile` can run on a Docker Space (PRO), Android and
the Vite web app should point at:

```text
https://BhaiAshish-HardTalkAI-api.hf.space
```

Until then, use this Static Space for the public demo and Play-facing privacy page:

```text
https://bhaiashish-hardtalkai.static.hf.space/privacy.html
```
