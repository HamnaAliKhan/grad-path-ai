# GradPath AI

**Graduate Career Placement Prediction System** - a career-readiness platform that connects university students
with their Career Office.

Students build a profile, understand their readiness with a transparent **60-rule** score, find skill gaps, explore
careers and jobs, analyse resumes, set goals, apply to jobs and message Career Officers. Career Officers review students
and applications, manage job listings, communicate with students and view reports.

> GradPath AI is an educational decision-support project. Readiness and job-fit scores are rule-based estimates for
> guidance only - **not** guarantees of placement, hiring decisions, or measures of a person's worth.

## Technology

| Area | Technology |
|---|---|
| Language / framework | Java 21, Spring Boot 3.4 (Spring MVC, Spring Security, Spring Data JPA) |
| User interface | Thymeleaf server-side templates, HTML, CSS (dark-blue GradPath design system), vanilla JavaScript |
| Charts | Plotly.js 2.35 (loaded from cdnjs with Subresource Integrity) |
| Database | Neon PostgreSQL (hosted); H2 in PostgreSQL mode for local development and tests |
| Persistence / migrations | JPA / Hibernate, Flyway (`src/main/resources/db/migration`) |
| Resume text extraction | Apache PDFBox 3 |
| Passwords | BCrypt (Spring Security Crypto) |
| Career assistant | Built-in deterministic rule-based assistant; optional Groq / OpenAI / Gemini / Anthropic |
| Placement model | Logistic regression (L2, Newton/IRLS) written, trained and evaluated in plain Java (`ai.gradpath.ml`) |
| Tests | JUnit 5, AssertJ, Spring Boot Test, MockMvc |

There is **no Python** anywhere in this repository. The official readiness score is the 60-rule engine; a small
logistic-regression model, trained in Java, adds a clearly labelled **secondary** placement estimate (see below).

## Features

### Public
- Landing page explaining GradPath AI, sign-in, student registration and Career Officer registration (requires the
  private `OFFICER_ACCESS_CODE`; officer registration is disabled when no code is configured).
- Optional demo accounts (`ENABLE_DEMO_LOGIN`, off by default) clearly labelled as fictional.
- "Keep me signed in for 30 days" with revocable server-side sessions and "sign out of all devices".

### Students
| Page | What it does |
|---|---|
| Onboarding / My Profile | Name, phone, city, portfolio, university, degree, department, graduation year, CGPA with grading scale (4.0 / 5.0 / 10.0), target career, confident and learning skills, certifications, projects, internships, work-experience months, communication and extracurricular ratings, profile completion. No sensitive demographic data. |
| Overview | Welcome, profile completion, CGPA / projects / internships, readiness score and category, strengths and improvement areas, skills overview, goal progress, next steps, top matching jobs, application statuses, unread messages, recent activity. |
| Placement Readiness | Score gauge, factor breakdown with weights, rule numbers and calculations, plain-language explanations, strengths / weaknesses, "biggest levers", what-if simulator (never saved), next steps, score history. Secondary model placement estimate with per-feature effects (also shown in the what-if simulator). |
| About the Model | How the placement model was trained, test metrics (ROC-AUC, accuracy, precision, recall, F1, Brier), ROC and calibration charts, coefficients and limitations. Also available to officers. |
| Skill-Gap Analysis | Matched / learning / missing skills for an illustrative career, importance, learning tips, project ideas. |
| Career Explorer | Five career paths with descriptions, responsibilities, skills, preparation advice and project ideas. |
| Job Opportunities | Search by title/company, filter by category and level, listings ranked by skill match, details with missing skills, applied state. |
| Applications | Apply with PDF resume, phone, availability, link, cover note and consent; duplicates and closed jobs are rejected; tracker shows status and skill / resume / readiness fit. |
| Resume Analyzer | PDF text extraction, keyword coverage, matched / missing skills, word count, structure checks, tips, project ideas. |
| Career Goals | Create, edit, delete goals; add, complete, remove tasks; automatic progress. |
| Messages | Direct student-officer messaging with unread indicators, timestamps and read status (auto-refresh). |
| Career Assistant | Concise career guidance. Uses the built-in assistant unless an AI provider is configured; AI providers never receive names, emails or phone numbers and fall back clearly to the built-in assistant. |

### Career Officers
| Page | What it does |
|---|---|
| Overview | Student record counts, average CGPA, readiness categories, internship experience, open jobs, applications awaiting review, unread messages, charts, recent profiles, quick links. |
| Students | Directory with readiness information and a profile review page (contact details only after the student has applied), including the secondary model estimate. |
| Applications | Applications ranked by job fit, detail page with resume preview/download, status updates (Submitted, Under review, Shortlisted, Interview, Offer, Not selected), ranked CSV. |
| Job Management | Create, edit, open/close and delete listings. Officers can change only their own listings; demo listings are read-only. |
| Reports & Analytics | Readiness, departments, CGPA, internships, skill gaps, applications and job activity charts with plain-language explanations; CSV downloads without names or contact details. |

## The readiness score (60 deterministic rules)

`ai.gradpath.readiness.ReadinessRuleEngine` adds seven factor scores (maximum points in brackets):
CGPA (30), Projects (20), Internships / experience (15), Technical skills (15), Certifications (5),
Communication (10), Extracurricular (5) = **100**.

* Rules 1-57 are tier rules; exactly one tier rule applies per factor (see the in-app **How the Score Works** page or
  the `*Scorer` classes).
* Rule 58: zero internships but at least 12 months of relevant work experience gives at least 5 experience points
  (never stacked with internship points).
* Rule 59: profile completion below 50% marks the result **Preliminary** and lists the missing information.
* Rule 60: below 40 = Needs Improvement, 40 to below 70 = Developing, 70+ = Job Ready.

Inputs are validated (out-of-range values produce a clear error, never a silent fallback). The score is a readiness
score, not a probability of employment.

**Job fit** (ranking aid only) = 55% skill match + 25% resume keyword match + 20% readiness category points
(Job Ready 100, Developing 60, Needs Improvement 25). Strong 75+, Good 55-74, Partial 35-54, Low below 35.

## Architecture

```text
Browser ──HTTPS──> Spring Boot (Tomcat)
                    ├─ security/   SessionTokenFilter, CSRF, role rules, BCrypt, login throttle
                    ├─ web/        Controllers (thin) + Thymeleaf templates + ChartFactory (Plotly specs)
                    ├─ service/    Business logic (auth, profiles, applications, jobs, goals, messages, reports, assistant)
                    ├─ readiness/  60-rule engine (FactorScorer template method + 7 scorers)
                    ├─ ml/         Placement model: CSV loader, logistic regression (IRLS), CV trainer, metrics, JSON store
                    ├─ reference/  Illustrative career catalog and skill aliases
                    ├─ repository/ Spring Data JPA repositories
                    └─ domain/     JPA entities ──JDBC──> Neon PostgreSQL (schema by Flyway)
```

### Object-oriented design and patterns

| Concept / pattern | Where |
|---|---|
| Inheritance & polymorphism | Abstract `User` with `Student` and `CareerOfficer` (joined tables); `FactorScorer` with 7 scorer subclasses |
| Composition | `Student` owns `CareerGoal`s, which own `GoalTask`s (cascade + orphan removal) |
| Aggregation | `CareerOfficer` has `Job`s that survive the officer (FK `ON DELETE SET NULL`) |
| Association class | `JobApplication` links `Student` and `Job` |
| Template Method | `FactorScorer.score()` fixes the algorithm; subclasses provide measurement/wording; `ExperienceScorer.adjust()` adds rule 58 |
| Strategy | `AssistantStrategy` implemented by `RuleBasedAssistant` and `LlmAssistant` |
| Factory | `UserFactory` (role to subclass), `AssistantFactory` (configuration to strategy) |
| Facade | `ReadinessService` (engine + history), `PlacementModelService` (model file + features + explanation), `LlmAssistant` (four provider APIs behind `reply()`) |
| Observer | Services publish `ActivityEvent`s; `ActivityService.onActivity` observes and stores them |
| Repository | `BaseRepository<T>` and the repository interfaces |
| Singleton | Spring-managed beans such as the connection pool, `ReadinessRuleEngine` and `PlacementModelService` (model loaded once) |

UML diagrams (PlantUML sources and PNGs) and the CCP report draft are in [`report/`](report/).

## Project structure

```text
src/main/java/ai/gradpath/
  config/      settings (GradPathProperties), .env loader, Neon datasource, security, demo seeder, start-up
  domain/      JPA entities
  readiness/   rule engine (60 rules)
  ml/          placement model (dataset, logistic regression, trainer, metrics, model file, train-model command)
  reference/   career catalog
  repository/  Spring Data repositories
  security/    session filter, cookies, password hashing, login throttle, onboarding gate
  service/     business services (+ service/assistant)
  validation/  input validators
  web/         controllers, view helpers, chart specifications
src/main/resources/
  db/migration/   Flyway migrations (V1 baseline, V2 additive)
  ml/             placement-model.json (bundled trained model)
  templates/      Thymeleaf pages, layouts and fragments
  static/         css/app.css, js/*.js, favicon
src/test/java/    JUnit tests (unit + integration on in-memory H2)
report/           UML diagrams and CCP report draft
```

## Setup

### Prerequisites
* Java 21 (JDK), e.g. Eclipse Temurin. Maven is **not** required - the Maven wrapper (`mvnw`) downloads it.
* A Neon PostgreSQL database for the hosted setup (optional for local development).

### Configuration (environment variables)

Copy `.env.example` to `.env` (git-ignored) for local use, or set the same names as environment variables /
deployment secrets. Real environment variables override `.env`. Secrets are never logged.

| Variable | Purpose | Default |
|---|---|---|
| `DATABASE_URL` | Neon connection string (`postgresql://user:password@host/db?sslmode=require`) or a `jdbc:postgresql://` URL. Empty = local H2 file `data/local/gradpath` | empty |
| `OFFICER_ACCESS_CODE` | Code required for Career Officer registration. Empty disables officer registration | empty |
| `ENABLE_DEMO_LOGIN` | Allow the public demo accounts to sign in (keep `false` in public deployments) | `false` |
| `SEED_DEMO_DATA` | Insert missing fictional demo records at start-up (never overwrites or deletes data) | `false` |
| `CGPA_SCALE` | Default grading scale for new students (4.0, 5.0 or 10.0) | `4.0` |
| `COOKIE_SECURE` | Always mark the session cookie `Secure` (set `true` behind HTTPS) | `false` |
| `CHATBOT_PROVIDER` | `rule`, `groq`, `openai`, `gemini` or `anthropic` | `rule` |
| `GROQ_API_KEY`, `GROQ_MODEL` (and `OPENAI_*`, `GEMINI_*`, `ANTHROPIC_*`) | Optional AI provider credentials | empty |
| `PLACEMENT_MODEL_PATH` | Optional placement model file produced by `train-model`; empty = the model bundled in the jar | empty |
| `PORT` | HTTP port | `8080` |

### Neon database
1. Create a Neon project and copy its connection string from the Neon console.
2. Set `DATABASE_URL` (in `.env` locally, or as a deployment secret).
3. Start the app. Flyway runs automatically:
   * a new, empty database gets the full schema (V1 + V2);
   * an existing GradPath database (created by the earlier version) is **baselined** at V1 and receives only the
     additive V2 migration (new columns with defaults, indexes, foreign keys and check constraints). No user data is
     deleted. Readiness records from the earlier ML version are kept but marked legacy and are not shown as rule results.
   * Existing accounts keep working: legacy PBKDF2 password hashes are verified once and upgraded to BCrypt at the next sign-in.

### Run

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
# then open http://localhost:8080
```

or build a jar: `./mvnw -DskipTests package` and `java -jar target/gradpath-ai.jar`.
Container: `docker build -t gradpath-ai .` then `docker run -p 8080:8080 -e DATABASE_URL=... -e OFFICER_ACCESS_CODE=... -e COOKIE_SECURE=true gradpath-ai`.

### Demo accounts (local evaluation only)
With `SEED_DEMO_DATA=true` and `ENABLE_DEMO_LOGIN=true` the landing page shows a **Quick demo** tab:
student `demo@gradpath.ai` / `Demo@1234`, officer `officer@gradpath.ai` / `Officer@1234`. Demo data uses fictional
names, `@demo.gradpath.ai` addresses and "(fictional)" company names; demo listings are read-only and demo accounts can
only message other demo accounts. When demo login is disabled, demo passwords are removed and their sessions revoked.

### Tests

```bash
./mvnw test
```

167 tests cover: every rule-tier boundary, rules 58-60, score limits and all three categories, input validation,
password hashing and legacy upgrade, login throttling and generic errors, registration and the officer access code,
revocable sessions, role-based access to every page, CSRF, cookies, onboarding gate, skill matching, job-fit formula
and labels, PDF resume extraction and analysis, application validation / duplicates / closed jobs, job ownership,
goal progress and ownership, messaging rules, profile validation, readiness history, the what-if endpoint (never
saved), the career assistant (deterministic answers, PII redaction, AI fallback) and the Flyway migrations.
Tests use an in-memory H2 database and never read `.env`, so they cannot reach Neon or an AI provider.

## Placement model (secondary estimate, pure Java)

The 60-rule score is the **official** readiness measure: it alone sets the category and the readiness part of job
fit. Next to it, a logistic-regression model gives a **secondary, data-driven estimate** of the chance of placement,
with a per-feature explanation. It is written, trained and evaluated in plain Java (`src/main/java/ai/gradpath/ml`) -
no Python, no ML library.

| Step | What happens |
|---|---|
| Features | CGPA as a share of its scale, projects, internship (yes/no), communication (0-10), extracurricular (0-10). No sensitive data (IQ, gender, age ...) is read, even if the CSV contains it. |
| Split | Stratified 80% training / 20% held-out test, fixed seed. |
| Tuning | Stratified 5-fold cross-validation over L2 penalties 0, 0.1, 1, 10, 100, 1000; scaler fitted inside each fold; best mean ROC-AUC wins (simpler model on near-ties). |
| Algorithm | Logistic regression trained with Newton's method (IRLS) - deterministic, converges in a few iterations. |
| Threshold | The probability that maximises F1 on the out-of-fold predictions. |
| Evaluation | Once, on the test rows: ROC-AUC, accuracy, precision, recall, F1, Brier score, log loss, confusion matrix, ROC and calibration curves. |
| Output | `src/main/resources/ml/placement-model.json` - coefficients and metrics only, no student data. |

**The bundled model is a practice model trained on synthetic data**, because the Kaggle `placement.csv` is not in the
repository (licence) - the UI labels it "Practice model" everywhere. To train on real data:

```bash
# put a licensed placement CSV at data/kaggle/placement.csv (git-ignored), then
./mvnw -B package -DskipTests
java -jar target/gradpath-ai.jar train-model data/kaggle/placement.csv     # overwrites the bundled model file
./mvnw -B package                                                          # rebuild with the new model
```

Accepted column names (case-insensitive): `CGPA`/`GPA`, `Internship_Experience`/`Internships`, `Projects_Completed`/
`Projects`, `Communication_Skills`, `Extra_Curricular_Score`, `Placement`/`Placed`/`PlacementStatus` (Yes/No,
Placed/Not placed, 1/0). The CGPA scale (4, 5 or 10) is detected automatically. Options: `--out <file>`, `--seed <n>`,
`--synthetic`. To use a model file without rebuilding, set `PLACEMENT_MODEL_PATH`. Without any model, the estimate is
simply hidden.

## Security and privacy
* BCrypt password hashes; generic sign-in errors; per-email and per-address throttling.
* Random 256-bit session tokens in `HttpOnly`, `SameSite=Lax` (and `Secure` on HTTPS) cookies; only SHA-256 hashes are
  stored; sessions are revocable.
* CSRF protection on every state-changing request; Content-Security-Policy and other security headers; Thymeleaf
  output escaping.
* Role checks on every route (`/student/**`, `/officer/**`) and on service methods (`@PreAuthorize`), plus ownership
  checks for goals, tasks, listings, applications and conversations.
* Resume PDFs are type/size/page checked, parsed only for text, stored in the database and served only to Career
  Officers (and the submitting student) with `nosniff`.
* CSV exports exclude names and contact details (the ranked applicant list contains candidate names only).
* No sensitive demographic data is collected or used in any score.

## Features from the earlier version that were not preserved

| Feature | Reason |
|---|---|
| scikit-learn training, ONNX inference and the gradient-boosting model | Replaced by a logistic-regression model implemented and trained in Java (no Python). The rule engine stays the official score. |
| Python Streamlit UI and pytest tests | Replaced by the Java Spring MVC + Thymeleaf UI and JUnit tests (no Python allowed). |
| Separate REST API for the Streamlit client | No longer needed - the Java web app renders pages directly. Small JSON endpoints remain for the what-if simulator and assistant. |
| Probability as the readiness score | The official score is the 0-100 rule score; the model probability is shown only as a secondary estimate. Earlier records are kept in the database as "legacy" and not displayed as rule results. |

## Limitations
* Educational project: authentication is not a replacement for an institutional identity provider or a security review.
* Career requirements, skill aliases and demo listings are illustrative examples, not verified employer data.
* Keyword-based resume analysis cannot judge the quality of experience.
* Charts need internet access to load Plotly from the CDN (figures are also shown as text/tables).

## Authors
* **Amna Mohsin** - [GitHub](https://github.com/amna-mohsin)
* **Hamna Ali Khan** - [GitHub](https://github.com/HamnaAliKhan)

## License
[MIT](LICENSE)
