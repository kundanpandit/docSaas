# DocSaaS

Free online PDF, image, OCR and AI document tools. Spring Boot API (`/`) + React UI (`docsaas-ui/`).

## Run locally

Requirements: Java 21+, Node 20+, MySQL 8 with a `docsaas_db` database.

```bash
# 1. secrets (git-ignored)
cp config/application-secrets.example.properties config/application-secrets.properties
#    then edit DB_PASSWORD and JWT_SECRET

# 2. OCR language data (free Tesseract models) into ./tessdata
mkdir -p tessdata
curl -L -o tessdata/eng.traineddata https://github.com/tesseract-ocr/tessdata_fast/raw/main/eng.traineddata
curl -L -o tessdata/hin.traineddata https://github.com/tesseract-ocr/tessdata_fast/raw/main/hin.traineddata

# 3. backend (http://localhost:8080) - tables are created automatically
./mvnw spring-boot:run

# 4. frontend (http://localhost:5173)
cd docsaas-ui && npm install && npm run dev
```

Tools whose engine is missing are reported by `GET /api/v1/tools` and shown as "temporarily
unavailable" in the UI. Nothing else breaks.

## Engines (all free)

| Tool(s) | Engine | Setup |
|---|---|---|
| Merge, split, compress, PDF↔images, image tools | Apache PDFBox, Java ImageIO | none |
| WEBP converter | webp-imageio (bundled native libwebp) | none |
| PDF → Word | PDFBox + Apache POI | none |
| Image to text, scanned PDF OCR | Tesseract via Tess4J | `tessdata/` (above). Windows natives are bundled; on Linux install `libtesseract5` |
| Word → PDF | LibreOffice headless | Install LibreOffice. Auto-detected, or set `LIBREOFFICE_PATH` |
| AI summary, chat with PDF | Any OpenAI-compatible API | Set `AI_API_KEY` (see below) |

### Free AI providers

Set these as environment variables (or in `config/application-secrets.properties`):

| Provider | `AI_BASE_URL` | `AI_MODEL` | Key |
|---|---|---|---|
| Google Gemini (free tier, default) | `https://generativelanguage.googleapis.com/v1beta/openai` | `gemini-2.5-flash` | aistudio.google.com |
| Groq (free tier) | `https://api.groq.com/openai/v1` | `llama-3.3-70b-versatile` | console.groq.com |
| OpenRouter (free models) | `https://openrouter.ai/api/v1` | any model ending in `:free` | openrouter.ai |
| Ollama (local, no key) | `http://localhost:11434/v1` | e.g. `llama3.2` | none; needs a machine with enough RAM |

Free tiers are rate-limited and some providers may use free-tier data for training: check
their terms before sending users' documents. Rate-limit errors are retried automatically.

## Job engine

Jobs run on [JobRunr](https://www.jobrunr.io) (free open-source edition) with MySQL as the queue:

- Jobs survive restarts and crashes; an interrupted job is picked up again by a worker.
- Failures caused by bad input fail immediately with a clear message. Transient failures
  (AI rate limits, LibreOffice busy, crashes) are retried with back-off, up to 3 attempts.
- Jobs stuck for more than 30 minutes are failed so users never wait forever.
- Dashboard: http://localhost:8000/dashboard (user `admin`, password from `JOBRUNR_DASHBOARD_PASSWORD`).

**Scaling:** run more instances against the same database. `JOB_WORKER_ENABLED=false` makes an
instance API-only; `true` makes it process jobs (`JOB_WORKERS` threads each). All instances must
share the storage directory (`STORAGE_DIR`): a shared volume on one host, or network storage
across hosts.

## Deploy with Docker

```bash
cat > .env <<EOF
DB_PASSWORD=<strong password>
JWT_SECRET=<random string, 32+ characters>
JOBRUNR_DASHBOARD_PASSWORD=<strong password>
AI_API_KEY=<free Gemini key>
CORS_ALLOWED_ORIGINS=https://your-domain.com
EOF
docker compose up -d --build
docker compose up -d --scale worker=3    # more processing capacity
```

The image includes LibreOffice and Tesseract (English + Hindi).

## Limits and retention

Configured in `src/main/resources/application.properties`: 50 MB per file, guests 30 jobs per
hour, uploads deleted right after processing, results deleted after 60 minutes.
