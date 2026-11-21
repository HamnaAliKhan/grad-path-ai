-- V2: Java web application (rule-based readiness engine).
-- Additive only: new columns get safe defaults, existing rows are kept. The only data changes are
--   * flagging earlier readiness records as "legacy" (they came from the removed ML / demo scorer), and
--   * clearing dangling references (ids that point to rows that no longer exist) so foreign keys can be added.
-- No user, student, job, application, goal or message row is deleted.

-- Students: grading scale used for CGPA normalisation (default 4.0).
ALTER TABLE students ADD COLUMN IF NOT EXISTS cgpa_scale DOUBLE PRECISION DEFAULT 4.0 NOT NULL;

-- Career officers: mark the fictional demo officer explicitly.
ALTER TABLE career_officers ADD COLUMN IF NOT EXISTS is_demo BOOLEAN DEFAULT FALSE NOT NULL;
UPDATE career_officers SET is_demo = TRUE
 WHERE id IN (SELECT id FROM users WHERE email = 'officer@gradpath.ai');

-- Sign-in sessions: persistent ("keep me signed in") flag and last use, so sessions can be listed and revoked.
ALTER TABLE auth_sessions ADD COLUMN IF NOT EXISTS persistent BOOLEAN DEFAULT TRUE NOT NULL;
ALTER TABLE auth_sessions ADD COLUMN IF NOT EXISTS last_used_at TIMESTAMP WITHOUT TIME ZONE;

-- Readiness history. The "probability" column now stores the 0-100 rule-based readiness score.
-- Rows written before this migration are marked legacy and are not shown as rule-engine results.
ALTER TABLE placement_predictions ADD COLUMN IF NOT EXISTS is_legacy BOOLEAN DEFAULT TRUE NOT NULL;
ALTER TABLE placement_predictions ALTER COLUMN is_legacy SET DEFAULT FALSE;
ALTER TABLE placement_predictions ADD COLUMN IF NOT EXISTS rule_engine_version VARCHAR(40) DEFAULT 'legacy' NOT NULL;
ALTER TABLE placement_predictions ADD COLUMN IF NOT EXISTS preliminary BOOLEAN DEFAULT FALSE NOT NULL;
CREATE INDEX IF NOT EXISTS ix_placement_predictions_student_legacy ON placement_predictions (student_id, is_legacy);

-- Applications: original resume file name and last status change.
ALTER TABLE job_applications ADD COLUMN IF NOT EXISTS resume_filename VARCHAR(160) DEFAULT '' NOT NULL;
ALTER TABLE job_applications ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE;
CREATE INDEX IF NOT EXISTS ix_job_applications_status ON job_applications (status);

-- Jobs: last edit time and lookup indexes.
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE;
CREATE INDEX IF NOT EXISTS ix_jobs_created_by ON jobs (created_by);
CREATE INDEX IF NOT EXISTS ix_jobs_is_open ON jobs (is_open);

-- Messages and activity: lookup indexes.
CREATE INDEX IF NOT EXISTS ix_messages_recipient_unread ON messages (recipient_id, is_read);
CREATE INDEX IF NOT EXISTS ix_activity_log_action ON activity_log (action);

-- Referential integrity for columns that previously had no foreign key.
UPDATE jobs SET created_by = NULL
 WHERE created_by IS NOT NULL AND created_by NOT IN (SELECT id FROM career_officers);
ALTER TABLE jobs ADD CONSTRAINT fk_jobs_created_by
    FOREIGN KEY (created_by) REFERENCES career_officers (id) ON DELETE SET NULL;

UPDATE messages SET job_id = NULL
 WHERE job_id IS NOT NULL AND job_id NOT IN (SELECT id FROM jobs);
ALTER TABLE messages ADD CONSTRAINT fk_messages_job
    FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE SET NULL;

UPDATE activity_log SET user_id = NULL
 WHERE user_id IS NOT NULL AND user_id NOT IN (SELECT id FROM users);
ALTER TABLE activity_log ADD CONSTRAINT fk_activity_log_user
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL;

-- Value constraints that match the application's validation rules.
ALTER TABLE job_applications ADD CONSTRAINT ck_job_applications_status
    CHECK (status IN ('Submitted', 'Under review', 'Shortlisted', 'Interview', 'Offer', 'Not selected'));
ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role IN ('student', 'officer'));
