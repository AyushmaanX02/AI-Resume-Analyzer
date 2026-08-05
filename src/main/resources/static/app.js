/* ==========================================================================
   AI RESUME ANALYZER - FRONTEND APPLICATION JS
   REST API Integrations, Auth State, Tab Routing, Dynamic Score Gauges
   ========================================================================== */

const API_BASE = "/api";
let currentToken = localStorage.getItem("resume_analyzer_token") || null;
let currentUser = JSON.parse(localStorage.getItem("resume_analyzer_user") || "null");

let activeResumeId = null;
let activeJdId = null;
let activeResumeFilename = null;
let activeJdTitle = null;

// SAMPLE DATA FOR 1-CLICK DEMO
const SAMPLE_RESUME_TEXT = `
Ayushmaan Kapoor
Software Development Engineer | ayushmaan@example.com | +1 (555) 019-2834
LinkedIn: linkedin.com/in/ayushmaan-kapoor | GitHub: github.com/ayushmaan

SUMMARY
Full-Stack & Backend Engineer with 2+ years of experience building resilient microservices using Java 17, Spring Boot, REST APIs, PostgreSQL, and Docker. Strong background in data structures, algorithms, system design, and continuous integration pipelines.

EDUCATION
B.S. in Computer Science - Tech Institute of Engineering (2020 - 2024)

TECHNICAL SKILLS
- Languages: Java, Python, JavaScript, SQL, HTML/CSS
- Frameworks & Libraries: Spring Boot, Spring Security, Hibernate/JPA, Express.js, React
- Databases & Storage: MySQL, PostgreSQL, Redis, MongoDB
- DevOps & Tools: Docker, Git, Maven, AWS (S3, EC2), JUnit, Mockito, REST API design

EXPERIENCE
Software Engineer Intern | NexTech Systems (June 2023 - Present)
- Architected high-throughput REST APIs in Java 17 and Spring Boot for user authentication and transactional data processing.
- Implemented JWT authentication and Spring Security filters, reducing unauthorized API access attempts to 0%.
- Optimized database queries using Spring Data JPA and MySQL indexing, improving search response time by 45%.
- Wrote automated unit and integration tests using JUnit 5 and Mockito, achieving 88% test coverage.

PROJECTS
FinTech Payment Gateway Microservice | Java, Spring Boot, MySQL, Docker
- Built an idempotent payment processing API handling 1,000+ simulated concurrent transactions per minute.
- Deployed multi-container environment with Docker Compose and automated build pipelines using GitHub Actions.

AI Resume Analyzer | Spring Boot, NLP, Anthropic API, Vanilla JS
- Built an automated ATS parsing and skill-matching engine evaluating resume fit against job descriptions.
`;

const SAMPLE_JD_TEXT = `
Position: SDE-I Backend Engineer
Company: Acme Cloud Solutions

About the Role:
We are looking for a high-performing SDE-I Backend Engineer to build scalable cloud microservices and developer APIs. You will work closely with cross-functional teams to design, write, and maintain clean, performant Java services.

Key Responsibilities:
- Design and maintain production REST APIs using Java 17 and Spring Boot.
- Implement robust security models using JWT and OAuth2.
- Optimize database schemas and queries in MySQL and Redis caching.
- Build CI/CD pipelines and package services into Docker containers.
- Collaborate with frontend engineers to integrate web applications.

Required Qualifications & Skills:
- 1+ years of experience in backend development with Java, Spring Boot, and REST APIs.
- Proficiency in relational databases (MySQL or PostgreSQL).
- Experience with Docker, Git, and unit testing frameworks (JUnit/Mockito).
- Strong understanding of data structures, algorithms, and microservices architecture.
- Knowledge of cloud services (AWS or Railway) and Redis is a plus.
`;

// INITIALIZATION
document.addEventListener("DOMContentLoaded", () => {
  initAuthUI();
  setupDragAndDrop();
  loadSavedLibrary();
});

// AUTHENTICATION FUNCTIONS
function initAuthUI() {
  const container = document.getElementById("auth-status-container");
  if (currentToken && currentUser) {
    container.innerHTML = `
      <div class="user-badge" style="display: flex; align-items: center; gap: 10px;">
        <span style="font-size: 0.88rem; font-weight: 600; color: var(--text-main);">👤 ${currentUser.fullName || currentUser.email}</span>
        <button class="btn btn-secondary btn-sm" onclick="logout()">Logout</button>
      </div>
    `;
  } else {
    container.innerHTML = `
      <button class="btn btn-secondary btn-sm" id="auth-modal-btn" onclick="openAuthModal()">Sign In / Register</button>
    `;
  }
}

function openAuthModal() {
  document.getElementById("auth-modal").classList.remove("hidden");
}

function closeAuthModal() {
  document.getElementById("auth-modal").classList.add("hidden");
  document.getElementById("auth-error-msg").classList.add("hidden");
}

function toggleAuthTab(tab) {
  const isLogin = tab === 'login';
  document.getElementById("auth-tab-login").classList.toggle("active", isLogin);
  document.getElementById("auth-tab-register").classList.toggle("active", !isLogin);
  document.getElementById("fullname-group").style.display = isLogin ? "none" : "block";
  document.getElementById("auth-modal-title").innerText = isLogin ? "Sign In" : "Create Account";
  document.getElementById("auth-submit-btn").innerText = isLogin ? "Sign In" : "Register";
}

async function handleAuthSubmit(e) {
  e.preventDefault();
  const isLogin = document.getElementById("auth-tab-login").classList.contains("active");
  const email = document.getElementById("auth-email").value.trim();
  const password = document.getElementById("auth-password").value;
  const fullName = document.getElementById("auth-fullname").value.trim();
  const errorBox = document.getElementById("auth-error-msg");

  errorBox.classList.add("hidden");

  const endpoint = isLogin ? `${API_BASE}/auth/login` : `${API_BASE}/auth/register`;
  const bodyData = isLogin ? { email, password } : { email, password, fullName };

  try {
    const res = await fetch(endpoint, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(bodyData)
    });

    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || "Authentication failed");
    }

    currentToken = data.token;
    currentUser = { email: data.email, fullName: data.fullName };

    localStorage.setItem("resume_analyzer_token", currentToken);
    localStorage.setItem("resume_analyzer_user", JSON.stringify(currentUser));

    resetAppState();
    initAuthUI();
    closeAuthModal();
    loadSavedLibrary();
  } catch (err) {
    errorBox.innerText = err.message;
    errorBox.classList.remove("hidden");
  }
}

function logout() {
  currentToken = null;
  currentUser = null;
  localStorage.removeItem("resume_analyzer_token");
  localStorage.removeItem("resume_analyzer_user");
  resetAppState();
  initAuthUI();
}

function resetAppState() {
  activeResumeId = null;
  activeJdId = null;
  activeResumeFilename = null;
  activeJdTitle = null;

  const jdTitle = document.getElementById("jd-title-input");
  if (jdTitle) jdTitle.value = "";
  const jdText = document.getElementById("jd-text-input");
  if (jdText) jdText.value = "";
  const fileInput = document.getElementById("resume-file-input");
  if (fileInput) fileInput.value = "";

  const resumeBadge = document.getElementById("resume-status-badge");
  if (resumeBadge) resumeBadge.classList.add("hidden");
  const jdBadge = document.getElementById("jd-status-badge");
  if (jdBadge) jdBadge.classList.add("hidden");
  const resultsContainer = document.getElementById("results-container");
  if (resultsContainer) resultsContainer.classList.add("hidden");

  const resumeSelect = document.getElementById("resume-select");
  if (resumeSelect) resumeSelect.innerHTML = `<option value="">-- Choose from uploaded resumes --</option>`;
  const jdSelect = document.getElementById("jd-select");
  if (jdSelect) jdSelect.innerHTML = `<option value="">-- Choose saved JD --</option>`;

  const libResumes = document.getElementById("library-resumes-list");
  if (libResumes) libResumes.innerHTML = `<p class="empty-text">No resumes uploaded yet.</p>`;
  const libJds = document.getElementById("library-jds-list");
  if (libJds) libJds.innerHTML = `<p class="empty-text">No job descriptions saved yet.</p>`;
  const historyTable = document.getElementById("history-table-body");
  if (historyTable) historyTable.innerHTML = `<tr><td colspan="7" class="empty-table-cell">No past analyses recorded yet.</td></tr>`;
}

function getAuthHeaders() {
  const headers = {};
  if (currentToken) {
    headers["Authorization"] = `Bearer ${currentToken}`;
  }
  return headers;
}

// TAB NAVIGATION
function switchTab(tabName) {
  document.querySelectorAll(".tab-btn").forEach(btn => btn.classList.remove("active"));
  document.querySelectorAll(".tab-content").forEach(content => content.classList.remove("active"));

  document.getElementById(`tab-${tabName}-btn`).classList.add("active");
  document.getElementById(`view-${tabName}`).classList.add("active");

  if (tabName === "history") {
    loadHistory();
  } else if (tabName === "library") {
    loadSavedLibrary();
  }
}

// DRAG AND DROP & FILE UPLOAD
function setupDragAndDrop() {
  const zone = document.getElementById("resume-drop-zone");
  if (!zone) return;

  ["dragenter", "dragover"].forEach(eventName => {
    zone.addEventListener(eventName, (e) => {
      e.preventDefault();
      zone.classList.add("dragover");
    }, false);
  });

  ["dragleave", "drop"].forEach(eventName => {
    zone.addEventListener(eventName, (e) => {
      e.preventDefault();
      zone.classList.remove("dragover");
    }, false);
  });

  zone.addEventListener("drop", (e) => {
    const dt = e.dataTransfer;
    const files = dt.files;
    if (files.length > 0) {
      uploadResumeFile(files[0]);
    }
  });
}

function handleFileSelected(event) {
  const file = event.target.files[0];
  if (file) {
    uploadResumeFile(file);
  }
}

async function uploadResumeFile(file) {
  if (!ensureAuth()) return;

  const formData = new FormData();
  formData.append("file", file);

  try {
    const res = await fetch(`${API_BASE}/resumes`, {
      method: "POST",
      headers: getAuthHeaders(),
      body: formData
    });

    if (!res.ok) {
      const err = await res.json();
      throw new Error(err.message || "Failed to upload resume");
    }

    const data = await res.json();
    setResumeState(data.id, data.filename, data.parsedSectionsJson);
    loadSavedLibrary();
  } catch (err) {
    alert("Upload failed: " + err.message);
  }
}

function setResumeState(id, filename, parsedJsonStr) {
  activeResumeId = id;
  activeResumeFilename = filename;

  document.getElementById("resume-filename-text").innerText = filename;
  document.getElementById("resume-id-tag").innerText = `ID: #${id}`;
  document.getElementById("resume-status-badge").classList.remove("hidden");

  let parsed = {};
  try {
    parsed = typeof parsedJsonStr === "string" ? JSON.parse(parsedJsonStr) : parsedJsonStr;
  } catch (e) {}

  const skillsCount = (parsed.skills || []).length;
  const wordCount = parsed.wordCount || 0;
  const hasContact = parsed.hasContactInfo ? "✅ Contact Info" : "⚠️ Contact Info Missing";

  document.getElementById("resume-parsed-summary").innerText = 
    `Parsed: ${skillsCount} skills detected | Word count: ${wordCount} | ${hasContact}`;
}

// JOB DESCRIPTION SUBMISSION
async function submitJobDescription() {
  if (!ensureAuth()) return;

  const title = document.getElementById("jd-title-input").value.trim() || "Untitled Job Description";
  const rawText = document.getElementById("jd-text-input").value.trim();

  if (!rawText) {
    alert("Please paste the job description text.");
    return;
  }

  try {
    const res = await fetch(`${API_BASE}/job-descriptions`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...getAuthHeaders()
      },
      body: JSON.stringify({ title, rawText })
    });

    if (!res.ok) {
      const err = await res.json();
      throw new Error(err.message || "Failed to submit JD");
    }

    const data = await res.json();
    setJdState(data.id, data.title);
    loadSavedLibrary();
  } catch (err) {
    alert("JD submission failed: " + err.message);
  }
}

function setJdState(id, title) {
  activeJdId = id;
  activeJdTitle = title;

  document.getElementById("jd-title-text").innerText = title;
  document.getElementById("jd-id-tag").innerText = `ID: #${id}`;
  document.getElementById("jd-status-badge").classList.remove("hidden");
}

// 1-CLICK DEMO LOAD
async function loadSampleData() {
  if (!ensureAuth()) return;

  // Create mock file for sample resume
  const blob = new Blob([SAMPLE_RESUME_TEXT], { type: "text/plain" });
  const file = new File([blob], "Ayushmaan_Kapoor_Resume_SDE.pdf", { type: "application/pdf" });

  document.getElementById("jd-title-input").value = "SDE-I Backend Engineer at Acme Cloud Solutions";
  document.getElementById("jd-text-input").value = SAMPLE_JD_TEXT;

  // Submit sample resume & JD
  await uploadResumeFile(file);
  await submitJobDescription();

  // Trigger analysis immediately
  runAnalysis();
}

// RUN AI ANALYSIS
async function runAnalysis() {
  if (!ensureAuth()) return;

  if (!activeResumeId || !activeJdId) {
    alert("Please ensure both a resume is uploaded (Step 1) and a Job Description is submitted (Step 2).");
    return;
  }

  const btn = document.getElementById("run-analysis-btn");
  btn.innerHTML = `<span class="spinner"></span> Analyzing Matching Profile...`;
  btn.disabled = true;

  try {
    const res = await fetch(`${API_BASE}/analysis`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...getAuthHeaders()
      },
      body: JSON.stringify({ resumeId: activeResumeId, jdId: activeJdId })
    });

    if (!res.ok) {
      const err = await res.json();
      throw new Error(err.message || "Analysis failed");
    }

    const data = await res.json();
    renderAnalysisResults(data);
  } catch (err) {
    alert("Analysis Error: " + err.message);
  } finally {
    btn.innerHTML = `<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg> Run AI Match Analysis`;
    btn.disabled = false;
  }
}

// RENDER ANALYSIS RESULTS & BREAKDOWN
function renderAnalysisResults(data) {
  const container = document.getElementById("results-container");
  container.classList.remove("hidden");
  container.scrollIntoView({ behavior: "smooth" });

  const score = data.matchScore ?? 0;
  const skillsScore = data.skillsMatchScore ?? score;
  const expScore = data.experienceMatchScore ?? score;
  const kwScore = data.keywordScore ?? score;

  // Animated score counter
  animateScoreCounter(score);

  // Score gauge circle offset
  const circle = document.getElementById("score-gauge-circle");
  const circumference = 314;
  const offset = circumference - (score / 100) * circumference;
  circle.style.strokeDashoffset = offset;

  // Rating badge
  const ratingBadge = document.getElementById("score-rating-badge");
  const descText = document.getElementById("score-description");

  if (score >= 80) {
    ratingBadge.className = "rating-badge rating-high";
    ratingBadge.innerText = "Excellent Match (Strong Fit)";
    descText.innerText = "Your profile strongly matches key JD skills and technical domain context.";
  } else if (score >= 60) {
    ratingBadge.className = "rating-badge rating-good";
    ratingBadge.innerText = "Good Fit (Minor Gaps)";
    descText.innerText = "Solid overlap. Add missing keywords and quantify bullet achievements to maximize impact.";
  } else if (score >= 40) {
    ratingBadge.className = "rating-badge rating-mod";
    ratingBadge.innerText = "Moderate Match (Tailoring Needed)";
    descText.innerText = "Key technical requirements missing. Review skill gap analysis and suggestions below.";
  } else {
    ratingBadge.className = "rating-badge rating-low";
    ratingBadge.innerText = "Low Match (Needs Re-framing)";
    descText.innerText = "Significant skill gaps detected compared to job requirements.";
  }

  // Progress breakdown bars
  document.getElementById("skills-score-val").innerText = `${skillsScore}%`;
  document.getElementById("skills-progress-fill").style.width = `${skillsScore}%`;

  document.getElementById("exp-score-val").innerText = `${expScore}%`;
  document.getElementById("exp-progress-fill").style.width = `${expScore}%`;

  document.getElementById("kw-score-val").innerText = `${kwScore}%`;
  document.getElementById("kw-progress-fill").style.width = `${kwScore}%`;

  // Matched & Missing Skills
  const matchedContainer = document.getElementById("matched-skills-container");
  const missingContainer = document.getElementById("missing-skills-container");

  const matchedList = data.matchedSkills || [];
  const missingList = data.missingSkills || [];

  document.getElementById("matched-count").innerText = matchedList.length;
  document.getElementById("missing-count").innerText = missingList.length;

  matchedContainer.innerHTML = matchedList.length > 0 
    ? matchedList.map(s => `<span class="skill-pill matched">✓ ${escapeHtml(s)}</span>`).join("")
    : `<span class="empty-text">No exact skill overlap detected</span>`;

  missingContainer.innerHTML = missingList.length > 0 
    ? missingList.map(s => `<span class="skill-pill missing">✕ ${escapeHtml(s)}</span>`).join("")
    : `<span class="empty-text">No missing required skills! Excellent fit.</span>`;

  // ATS Flags
  const atsContainer = document.getElementById("ats-flags-container");
  const flags = data.atsFlags || [];
  if (flags.length === 0) {
    atsContainer.innerHTML = `
      <div class="ats-flag-item ats-ok">
        <span class="flag-icon">✅</span>
        <span>Standard resume section headers and clean layout detected. Low ATS parsing risk.</span>
      </div>
    `;
  } else {
    atsContainer.innerHTML = flags.map(flag => `
      <div class="ats-flag-item ats-warn">
        <span class="flag-icon">⚠️</span>
        <span>${escapeHtml(flag)}</span>
      </div>
    `).join("");
  }

  // Suggestions & Reframing Tips
  const sugContainer = document.getElementById("suggestions-container");
  const suggestions = data.suggestions || [];
  if (suggestions.length === 0) {
    sugContainer.innerHTML = `<div class="suggestion-item"><p>Your resume matches all key JD requirements!</p></div>`;
  } else {
    sugContainer.innerHTML = suggestions.map((sug, idx) => `
      <div class="suggestion-item">
        <p><strong>Reframing Tip ${idx + 1}:</strong> ${escapeHtml(sug)}</p>
        <button class="btn btn-secondary btn-sm" onclick="copyText('${escapeJsString(sug)}')">Copy</button>
      </div>
    `).join("");
  }

  document.getElementById("analysis-timestamp-badge").innerText = `Analyzed on ${new Date().toLocaleTimeString()}`;
}

function animateScoreCounter(target) {
  const element = document.getElementById("score-number-display");
  let current = 0;
  const step = target / 30;
  const timer = setInterval(() => {
    current += step;
    if (current >= target) {
      element.innerText = target;
      clearInterval(timer);
    } else {
      element.innerText = Math.round(current * 10) / 10;
    }
  }, 20);
}

// HISTORY & COMPARISON TAB
async function loadHistory() {
  if (!ensureAuth()) return;

  try {
    const res = await fetch(`${API_BASE}/analysis/history`, {
      headers: getAuthHeaders()
    });

    if (!res.ok) throw new Error("Failed to load history");

    const history = await res.json();
    const tbody = document.getElementById("history-table-body");

    if (history.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" class="empty-table-cell">No past analyses recorded yet.</td></tr>`;
      return;
    }

    tbody.innerHTML = history.map(item => `
      <tr>
        <td>#${item.id}</td>
        <td>${new Date(item.createdAt).toLocaleDateString()} ${new Date(item.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</td>
        <td>📄 ${escapeHtml(item.resumeFilename || 'Resume #' + item.resumeId)}</td>
        <td>💼 ${escapeHtml(item.jdTitle || 'Job #' + item.jdId)}</td>
        <td><strong style="color: var(--accent-indigo);">${item.matchScore}%</strong></td>
        <td>${item.skillsMatchScore ?? '-'}%</td>
        <td>
          <button class="btn btn-secondary btn-sm" onclick="viewHistoryDetail(${item.id})">View Details</button>
        </td>
      </tr>
    `).join("");
  } catch (err) {
    console.error(err);
  }
}

async function viewHistoryDetail(id) {
  try {
    const res = await fetch(`${API_BASE}/analysis/${id}`, {
      headers: getAuthHeaders()
    });
    if (!res.ok) throw new Error("Failed to fetch analysis details");

    const data = await res.json();
    switchTab('analyzer');
    renderAnalysisResults(data);
  } catch (err) {
    alert("Error loading detail: " + err.message);
  }
}

// SAVED LIBRARY TAB
async function loadSavedLibrary() {
  if (!currentToken) return;

  try {
    // Load Resumes
    const resResumes = await fetch(`${API_BASE}/resumes`, { headers: getAuthHeaders() });
    if (resResumes.ok) {
      const resumes = await resResumes.json();
      renderResumesLibrary(resumes);
    }

    // Load Job Descriptions
    const resJds = await fetch(`${API_BASE}/job-descriptions`, { headers: getAuthHeaders() });
    if (resJds.ok) {
      const jds = await resJds.json();
      renderJdsLibrary(jds);
    }
  } catch (err) {
    console.error(err);
  }
}

function renderResumesLibrary(resumes) {
  const list = document.getElementById("library-resumes-list");
  const select = document.getElementById("resume-select");

  select.innerHTML = `<option value="">-- Choose from uploaded resumes --</option>` +
    resumes.map(r => `<option value="${r.id}">${escapeHtml(r.filename)} (Uploaded ${new Date(r.uploadedAt).toLocaleDateString()})</option>`).join("");

  if (resumes.length === 0) {
    list.innerHTML = `<p class="empty-text">No resumes uploaded yet.</p>`;
    return;
  }

  list.innerHTML = resumes.map(r => `
    <div class="library-item-card">
      <div>
        <strong>📄 ${escapeHtml(r.filename)}</strong>
        <p style="font-size: 0.78rem; color: var(--text-muted);">ID: #${r.id} | ${new Date(r.uploadedAt).toLocaleDateString()}</p>
      </div>
      <button class="btn btn-secondary btn-sm" onclick="selectSavedResume(${r.id})">Use in Analyzer</button>
    </div>
  `).join("");
}

function renderJdsLibrary(jds) {
  const list = document.getElementById("library-jds-list");
  const select = document.getElementById("jd-select");

  select.innerHTML = `<option value="">-- Choose saved JD --</option>` +
    jds.map(j => `<option value="${j.id}">${escapeHtml(j.title || 'Job #' + j.id)}</option>`).join("");

  if (jds.length === 0) {
    list.innerHTML = `<p class="empty-text">No job descriptions saved yet.</p>`;
    return;
  }

  list.innerHTML = jds.map(j => `
    <div class="library-item-card">
      <div>
        <strong>💼 ${escapeHtml(j.title || 'Untitled JD')}</strong>
        <p style="font-size: 0.78rem; color: var(--text-muted);">ID: #${j.id} | ${new Date(j.createdAt).toLocaleDateString()}</p>
      </div>
      <button class="btn btn-secondary btn-sm" onclick="selectSavedJd(${j.id})">Use in Analyzer</button>
    </div>
  `).join("");
}

async function selectSavedResume(id) {
  if (!id) return;
  try {
    const res = await fetch(`${API_BASE}/resumes/${id}`, { headers: getAuthHeaders() });
    if (!res.ok) throw new Error("Failed to fetch resume");
    const data = await res.json();
    setResumeState(data.id, data.filename, data.parsedSectionsJson);
    switchTab('analyzer');
  } catch (err) {
    alert("Error: " + err.message);
  }
}

async function selectSavedJd(id) {
  if (!id) return;
  try {
    const res = await fetch(`${API_BASE}/job-descriptions/${id}`, { headers: getAuthHeaders() });
    if (!res.ok) throw new Error("Failed to fetch JD");
    const data = await res.json();
    setJdState(data.id, data.title);
    document.getElementById("jd-title-input").value = data.title || "";
    document.getElementById("jd-text-input").value = data.rawText || "";
    switchTab('analyzer');
  } catch (err) {
    alert("Error: " + err.message);
  }
}

// UTILITY HELPERS
function ensureAuth() {
  if (!currentToken) {
    openAuthModal();
    return false;
  }
  return true;
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/[&<>"']/g, function(m) {
    return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[m];
  });
}

function escapeJsString(str) {
  if (!str) return '';
  return str.replace(/'/g, "\\'").replace(/"/g, '\\"');
}

function copyText(text) {
  navigator.clipboard.writeText(text).then(() => {
    alert("Copied recommendation to clipboard!");
  }).catch(() => {
    alert("Failed to copy text.");
  });
}
