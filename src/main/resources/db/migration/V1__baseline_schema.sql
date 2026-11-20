-- V1: GradPath AI baseline schema.
-- Identical to the tables created by the earlier version of GradPath AI. Every statement is idempotent
-- (CREATE ... IF NOT EXISTS / ADD COLUMN IF NOT EXISTS), and an existing database is baselined at this
-- version by Flyway, so existing accounts and records are never dropped or rewritten.

CREATE TABLE IF NOT EXISTS users (
    id            SERIAL PRIMARY KEY,
    email         VARCHAR(160) NOT NULL,
    name          VARCHAR(120) NOT NULL,
    password_hash VARCHAR(300),
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS ix_users_email ON users (email);

CREATE TABLE IF NOT EXISTS students (
    id                     INTEGER PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    university             VARCHAR(160) NOT NULL,
    degree                 VARCHAR(40)  NOT NULL,
    department             VARCHAR(80)  NOT NULL,
    graduation_year        INTEGER      NOT NULL,
    cgpa                   DOUBLE PRECISION NOT NULL,
    skills                 TEXT         NOT NULL,
    learning_skills        TEXT         NOT NULL,
    certifications         TEXT         NOT NULL,
    projects               INTEGER      NOT NULL,
    internships            INTEGER      NOT NULL,
    work_experience_months INTEGER      NOT NULL,
    communication          INTEGER      NOT NULL,
    extracurricular        INTEGER      NOT NULL,
    target_career          VARCHAR(80)  NOT NULL,
    is_demo                BOOLEAN      NOT NULL,
    phone                  VARCHAR(30)  NOT NULL,
    city                   VARCHAR(80)  NOT NULL,
    portfolio_url          VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS career_officers (
    id     INTEGER PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    office VARCHAR(120) NOT NULL
);

CREATE TABLE IF NOT EXISTS auth_sessions (
    id         SERIAL PRIMARY KEY,
    user_id    INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_auth_sessions_user_id ON auth_sessions (user_id);
CREATE INDEX IF NOT EXISTS ix_auth_sessions_expires_at ON auth_sessions (expires_at);
CREATE UNIQUE INDEX IF NOT EXISTS ix_auth_sessions_token_hash ON auth_sessions (token_hash);

CREATE TABLE IF NOT EXISTS jobs (
    id          SERIAL PRIMARY KEY,
    title       VARCHAR(140) NOT NULL,
    company     VARCHAR(140) NOT NULL,
    category    VARCHAR(80)  NOT NULL,
    level       VARCHAR(40)  NOT NULL,
    skills      TEXT         NOT NULL,
    description TEXT         NOT NULL,
    location    VARCHAR(120) NOT NULL,
    is_demo     BOOLEAN      NOT NULL,
    created_by  INTEGER,
    is_open     BOOLEAN      NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS career_goals (
    id          SERIAL PRIMARY KEY,
    student_id  INTEGER NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    title       VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_career_goals_student_id ON career_goals (student_id);

CREATE TABLE IF NOT EXISTS goal_tasks (
    id         SERIAL PRIMARY KEY,
    goal_id    INTEGER NOT NULL REFERENCES career_goals (id) ON DELETE CASCADE,
    title      VARCHAR(200) NOT NULL,
    done       BOOLEAN NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_goal_tasks_goal_id ON goal_tasks (goal_id);

CREATE TABLE IF NOT EXISTS placement_predictions (
    id          SERIAL PRIMARY KEY,
    student_id  INTEGER NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    probability DOUBLE PRECISION NOT NULL,
    category    VARCHAR(40) NOT NULL,
    model_name  VARCHAR(80) NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_placement_predictions_student_id ON placement_predictions (student_id);

CREATE TABLE IF NOT EXISTS activity_log (
    id         SERIAL PRIMARY KEY,
    user_id    INTEGER,
    actor_name VARCHAR(120) NOT NULL,
    action     VARCHAR(80)  NOT NULL,
    detail     VARCHAR(300) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_activity_log_user_id ON activity_log (user_id);

CREATE TABLE IF NOT EXISTS job_applications (
    id                 SERIAL PRIMARY KEY,
    job_id             INTEGER NOT NULL REFERENCES jobs (id) ON DELETE CASCADE,
    student_id         INTEGER NOT NULL REFERENCES students (id) ON DELETE CASCADE,
    phone              VARCHAR(30)  NOT NULL,
    portfolio_url      VARCHAR(200) NOT NULL,
    availability       VARCHAR(60)  NOT NULL,
    cover_note         TEXT NOT NULL,
    resume_text        TEXT NOT NULL,
    skill_match        INTEGER NOT NULL,
    resume_match       INTEGER NOT NULL,
    readiness_category VARCHAR(40) NOT NULL,
    readiness_pct      DOUBLE PRECISION NOT NULL,
    fit_score          INTEGER NOT NULL,
    status             VARCHAR(30) NOT NULL,
    created_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    resume_pdf         BYTEA,
    CONSTRAINT uq_application_job_student UNIQUE (job_id, student_id)
);
CREATE INDEX IF NOT EXISTS ix_job_applications_job_id ON job_applications (job_id);
CREATE INDEX IF NOT EXISTS ix_job_applications_student_id ON job_applications (student_id);
CREATE INDEX IF NOT EXISTS ix_job_applications_fit_score ON job_applications (fit_score);

CREATE TABLE IF NOT EXISTS messages (
    id           SERIAL PRIMARY KEY,
    sender_id    INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recipient_id INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    job_id       INTEGER,
    body         TEXT NOT NULL,
    is_read      BOOLEAN NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_messages_sender_id ON messages (sender_id);
CREATE INDEX IF NOT EXISTS ix_messages_recipient_id ON messages (recipient_id);

-- Compatibility columns for databases created by older versions.
ALTER TABLE students ADD COLUMN IF NOT EXISTS phone VARCHAR(30) DEFAULT '' NOT NULL;
ALTER TABLE students ADD COLUMN IF NOT EXISTS city VARCHAR(80) DEFAULT '' NOT NULL;
ALTER TABLE students ADD COLUMN IF NOT EXISTS portfolio_url VARCHAR(200) DEFAULT '' NOT NULL;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS created_by INTEGER;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS is_open BOOLEAN DEFAULT TRUE NOT NULL;
ALTER TABLE job_applications ADD COLUMN IF NOT EXISTS resume_pdf BYTEA;
