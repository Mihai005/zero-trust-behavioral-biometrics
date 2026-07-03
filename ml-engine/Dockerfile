FROM python:3.12-slim

WORKDIR /app

ENV PYTHONUNBUFFERED=1

RUN apt-get update && apt-get install -y gcc \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .

RUN python - <<'PY'
from pathlib import Path

requirements = Path('requirements.txt').read_text(encoding='utf-16')
Path('/tmp/requirements.txt').write_text(requirements, encoding='utf-8')
PY

RUN pip install --no-cache-dir -r /tmp/requirements.txt

COPY . .

CMD ["python", "-u", "biometric_engine.py"]
