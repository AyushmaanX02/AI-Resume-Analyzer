# Product Requirements Document: AI Resume Analyzer

**Author:** Ayushmaan Kapoor
**Date:** August 2026
**Version:** 1.0
**Status:** Draft

---

## 1. Overview

### 1.1 Problem Statement
Job seekers struggle to know whether their resume will pass ATS (Applicant Tracking System) filters or match a specific job description. Manual tailoring is slow and guesswork-driven. There's no fast, objective way to check resume-to-JD fit before applying.

### 1.2 Solution
A web application where a user uploads their resume (PDF/DOCX) and pastes a job description. The system parses both, computes a match score, identifies missing/matched skills, flags ATS-formatting issues, and generates actionable improvement suggestions.

### 1.3 Goals
- Build a full-stack portfolio project demonstrating backend engineering depth (parsing, NLP/LLM integration, REST API design, auth, deployment).
- Produce a genuinely useful personal tool for tailoring resumes to JDs (dogfooding your own job search).
- Ship something demo-able in interviews with a clear technical story.

### 1.4 Non-Goals (v1)
- Not building a full ATS replacement or recruiter-facing product.
- No resume *writing* from scratch — analysis and suggestions only.
- No multi-language resume support (English only for v1).
- No mobile app.

---

## 2. Target User

Primarily **you** (portfolio + personal use), secondarily any student/early-career job seeker applying to SDE roles who wants quick resume-JD fit feedback.

---

## 3. Core Features (v1 Scope)

### 3.1 Resume Upload & Parsing
- Accept PDF and DOCX uploads.
- Extract raw text using Apache PDFBox (PDF) and Apache POI (DOCX).
- Parse into structured sections: Contact Info, Skills, Experience, Education, Projects, Certifications.

### 3.2 Job Description Input
- User pastes JD text (textarea) or uploads a JD file.
- Extract key requirements: required skills, preferred skills, years of experience, role keywords.

### 3.3 Match Scoring
- Compute a **Match Score (0–100)** based on:
  - Keyword/skill overlap between resume and JD (TF-IDF + cosine similarity, or LLM-assisted extraction).
  - Weighted scoring: hard skills > soft skills > generic keywords.
- Display score with a breakdown (e.g., "Skills match: 72%, Experience match: 60%").

### 3.4 Gap Analysis
- **Matched skills**: skills present in both resume and JD.
- **Missing skills**: skills in JD not found in resume.
- **Suggestions**: LLM-generated bullet points on how to reframe existing experience to cover gaps (without fabricating experience).

### 3.5 ATS Formatting Check
- Flag common ATS issues: tables/columns (parsing risk), missing standard section headers, non-standard fonts/icons, missing contact info, file format issues.

### 3.6 Analysis History
- Store past analyses per user (resume version + JD + score + timestamp).
- Allow comparing scores across resume iterations for the same JD.

### 3.7 Auth
- Basic user accounts (JWT-based, reusing your NexBank auth pattern) so history persists per user.

---

## 4. Out of Scope for v1 (Backlog / v2 ideas)
- Browser extension to pull JD directly from job posting pages.
- Auto-tailoring: generate a rewritten resume bullet-by-bullet.
- Integration with your job-application automation pipeline (auto-tailor + auto-apply).
- Multi-resume comparison against one JD.
- Recruiter-side dashboard (batch screening).

---

## 5. Technical Architecture

### 5.1 Stack
| Layer | Technology |
|---|---|
| Backend | Java 17+, Spring Boot 3.x |
| Auth | Spring Security + JWT |
| File Parsing | Apache PDFBox (PDF), Apache POI (DOCX) |
| NLP/Scoring | Hybrid: TF-IDF/cosine similarity (Java, no external cost) + LLM API call (Claude/OpenAI) for qualitative suggestions |
| Database | MySQL (resumes, JDs, analyses, users) |
| Frontend | Vanilla JS SPA or React (reuse NexBank frontend patterns) |
| Deployment | Docker → Railway |

### 5.2 High-Level Flow
1. User uploads resume → `POST /api/resumes` → stored + parsed → structured JSON returned.
2. User submits JD → `POST /api/job-descriptions` → parsed into requirements.
3. User triggers analysis → `POST /api/analysis` (resumeId, jdId) →
   - Backend computes keyword/skill overlap score.
   - Backend calls LLM API with resume text + JD text → returns structured JSON (missing skills, suggestions, ATS flags).
   - Combined result persisted and returned.
4. Frontend renders score breakdown, matched/missing skills, suggestions, ATS warnings.

### 5.3 Key API Endpoints
```
POST   /api/auth/register
POST   /api/auth/login
POST   /api/resumes                 (upload + parse)
GET    /api/resumes/{id}
POST   /api/job-descriptions        (submit + parse)
POST   /api/analysis                (run analysis: resumeId + jdId)
GET    /api/analysis/{id}
GET    /api/analysis/history        (per user)
```

### 5.4 Data Model (simplified)
- **User**: id, email, password_hash, created_at
- **Resume**: id, user_id, filename, raw_text, parsed_sections (JSON), uploaded_at
- **JobDescription**: id, user_id, raw_text, parsed_requirements (JSON), created_at
- **Analysis**: id, user_id, resume_id, jd_id, match_score, matched_skills (JSON), missing_skills (JSON), suggestions (JSON), ats_flags (JSON), created_at

---

## 6. Success Metrics
- **Technical**: End-to-end flow works (upload → analyze → results) with <5s response time for analysis.
- **Personal use**: Used to tailor at least 3 real job applications with measurable resume improvement.
- **Portfolio**: Deployed, documented on GitHub with README + architecture diagram, linkable from resume/LinkedIn.

---

## 7. Milestones

| Phase | Deliverable | Est. Time |
|---|---|---|
| 1 | Project scaffold, Spring Boot setup, DB schema, auth (JWT) | 3–4 days |
| 2 | Resume upload + parsing (PDFBox/POI) | 3–4 days |
| 3 | JD input + parsing | 2 days |
| 4 | TF-IDF matching engine | 3 days |
| 5 | LLM integration for suggestions + ATS flags | 3 days |
| 6 | Frontend (upload UI, results dashboard) | 5 days |
| 7 | History/dashboard, Docker + Railway deployment | 3 days |
| 8 | Polish, README, demo video | 2 days |

**Total estimate:** ~3–4 weeks part-time.

---

## 8. Risks & Open Questions
- **LLM cost/rate limits**: mitigate with caching + TF-IDF fallback if LLM call fails or quota exceeded.
- **PDF parsing accuracy**: resumes with complex layouts (columns, tables) may parse poorly — worth testing PDFBox against your own resume variants early.
- **Scope creep**: tempting to add auto-apply integration immediately — keep v1 focused on analysis only.
- **Open question**: Which LLM API to call — Claude API (Anthropic) vs OpenAI? (Claude API recommended given ecosystem familiarity.)

---

## 9. Appendix
- Reuses auth pattern from NexBank (Spring Security + JWT).
- Reuses Docker/Railway deployment pipeline from NexBank.
- Complements existing job-application automation project (resume tailoring + ATS form-filling) — this analyzer could eventually feed scores back into that pipeline.
