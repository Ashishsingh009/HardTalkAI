# Hugging Face Docker Space — https://huggingface.co/docs/hub/spaces-sdks-docker
FROM python:3.12-slim

RUN useradd -m -u 1000 user
USER user
ENV HOME=/home/user \
    PATH=/home/user/.local/bin:$PATH \
    PYTHONUNBUFFERED=1

WORKDIR $HOME/app

COPY --chown=user server/requirements.txt server/requirements.txt
RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir -r server/requirements.txt

COPY --chown=user server ./server
COPY --chown=user docs ./docs

EXPOSE 7860

CMD ["uvicorn", "app.main:app", "--app-dir", "server", "--host", "0.0.0.0", "--port", "7860"]
