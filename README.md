# AI Resume Analyzer

Upload a resume (PDF/DOCX) + paste a job description → get a match score, matched/missing skills, ATS formatting flags, and LLM-generated improvement suggestions.

Built per the [PRD](./PRD.md) — Spring Boot backend, MySQL, JWT auth, PDFBox/POI parsing, skill-overlap scoring, Anthropic API for qualitative suggestions.

## Stack
- Java 17 + Spring Boot 3.2
- Spring Security + JWT (stateless auth)
- MySQL (via Spring Data JPA)
- Apache PDFBox / POI (resume text extraction)
- Anthropic Messages API (suggestions + ATS flags, with graceful fallback if unset)
- Plain HTML/JS frontend for testing (`/frontend/index.html`)

## Project Structure
```
src/main/java/com/ayushmaan/resumeanalyzer/
├── config/          # Security config
├── security/         # JWT util + filter
├── model/            # JPA entities: User, Resume, JobDescription, Analysis
├── repository/        # Spring Data repositories
├── dto/               # Request/response DTOs
├── service/           # Business logic (parsing, matching, LLM, orchestration)
├── controller/         # REST endpoints
└── exception/          # Global exception handler
```

## Setup

### 1. Prerequisites
- Java 17+
- Maven
- MySQL running locally (or update `DB_HOST` for a remote instance)

### 2. Configure environment
Copy `.env.example` → set values, or export as env vars / pass via Railway config:
```bash
cp .env.example .env
```
At minimum set `JWT_SECRET`. `ANTHROPIC_API_KEY` is optional — without it, suggestions fall back to a rule-based response instead of failing.

### 3. Run locally
```bash
mvn spring-boot:run
```
API runs on `http://localhost:8080`.

Open `frontend/index.html` directly in a browser (or serve it) to test the full flow: register → login → upload resume → submit JD → analyze.

### 4. Run with Docker
```bash
docker build -t resume-analyzer .
docker run -p 8080:8080 --env-file .env resume-analyzer
```

### 5. Deploy to Railway
Same pattern as your NexBank deployment:
- Push to GitHub, connect repo to Railway.
- Add a MySQL plugin, Railway auto-injects DB env vars — map them to `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD` in service variables.
- Set `JWT_SECRET` and `ANTHROPIC_API_KEY` in Railway variables.
- Railway sets `PORT` automatically — already wired via `server.port: ${PORT:8080}`.

## API Endpoints

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | No | Create account, returns JWT |
| POST | `/api/auth/login` | No | Login, returns JWT |
| POST | `/api/resumes` | Yes | Upload resume (multipart `file`) |
| GET | `/api/resumes/{id}` | Yes | Get parsed resume |
| GET | `/api/resumes` | Yes | List your resumes |
| POST | `/api/job-descriptions` | Yes | Submit JD text |
| GET | `/api/job-descriptions/{id}` | Yes | Get parsed JD |
| GET | `/api/job-descriptions` | Yes | List your JDs |
| POST | `/api/analysis` | Yes | Run analysis: `{ resumeId, jdId }` |
| GET | `/api/analysis/history` | Yes | Your past analyses |

All authenticated routes require `Authorization: Bearer <token>`.

## How Scoring Works (v1)
1. Resume text is parsed and matched against a technical/soft-skill dictionary (`SkillsDictionary.java`).
2. JD text is parsed the same way to extract required skills.
3. **Match score** = (skills present in both) / (skills required by JD) × 100.
4. The Anthropic API is called with resume + JD text to generate qualitative, non-fabricated suggestions and flag ATS-formatting risks. If no API key is set, a rule-based fallback runs instead so the app never breaks.

This dictionary-matching approach is intentionally simple/explainable for v1. Swapping in TF-IDF cosine similarity or embeddings-based matching later only requires changing `MatchingService` — callers don't need to change.

## Known Limitations (v1)
- Skill extraction is dictionary-based, not full NLP — skills not in `SkillsDictionary.java` won't be detected.
- No column/table-aware PDF parsing — complex resume layouts may extract text out of order.
- No resume rewriting — suggestions only, no auto-tailoring (see PRD backlog).

## Next Steps (from PRD backlog)
- Auto-tailoring: LLM rewrites specific bullets to close skill gaps.
- Multi-resume comparison against one JD.
- Feed analysis scores into the job-application automation pipeline for auto-tailored applications.
