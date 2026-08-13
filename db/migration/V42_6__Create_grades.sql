-- HEI - Graduate Management System
-- Migration V42_6: grades, grade history, grade disputes
-- Source: HEI_Graduate_Management_MLD.sql (exams and grades section)
--
-- Note: per team decision, a dispute moves directly from PENDING to
-- RESOLVED or REJECTED (no REVIEWING state). This matches the generated
-- DisputeStatus enum (PENDING, RESOLVED, REJECTED) in the API contract.
-- The correct CHECK constraint and partial index are defined directly here.

CREATE TABLE grade (
    grade_id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id   UUID NOT NULL REFERENCES student(user_id),
    exam_id      UUID NOT NULL REFERENCES exam(exam_id),
    score        DECIMAL(4,2) NOT NULL CHECK (score BETWEEN 0 AND 20),
    recorded_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    recorded_by  UUID NOT NULL REFERENCES users(user_id),
    UNIQUE(student_id, exam_id)
);
CREATE INDEX idx_grade_student ON grade(student_id);

CREATE TABLE grade_history (
    history_id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    grade_id      UUID NOT NULL REFERENCES grade(grade_id),
    old_score     DECIMAL(4,2),
    new_score     DECIMAL(4,2) NOT NULL,
    modified_by   UUID NOT NULL REFERENCES users(user_id),
    modified_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    reason        VARCHAR(255)
);

-- Dispute workflow, distinct from grade_history (a log): this is a
-- stateful process that may or may not end in a grade change.
-- Status: PENDING -> RESOLVED | REJECTED (single teacher/admin action).
CREATE TABLE grade_dispute (
    dispute_id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    grade_id              UUID NOT NULL REFERENCES grade(grade_id),
    student_id            UUID NOT NULL REFERENCES student(user_id),
    reason                TEXT NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                          CHECK (status IN ('PENDING', 'RESOLVED', 'REJECTED')),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at           TIMESTAMPTZ,
    resolved_by           UUID REFERENCES users(user_id),
    resolution_note       TEXT,
    resulting_history_id  UUID REFERENCES grade_history(history_id) -- links to the actual value change, if any
);
CREATE INDEX idx_grade_dispute_active ON grade_dispute(status) WHERE status = 'PENDING';

-- Trigger: automatically log every grade modification
CREATE OR REPLACE FUNCTION fn_log_grade_change() RETURNS TRIGGER AS $$
BEGIN
    IF OLD.score IS DISTINCT FROM NEW.score THEN
        INSERT INTO grade_history(grade_id, old_score, new_score, modified_by, reason)
        VALUES (OLD.grade_id, OLD.score, NEW.score, NEW.recorded_by, 'Grade modification');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_log_grade_change
BEFORE UPDATE ON grade
FOR EACH ROW EXECUTE FUNCTION fn_log_grade_change();
