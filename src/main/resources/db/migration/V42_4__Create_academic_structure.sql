-- HEI - Graduate Management System
-- Migration V42_4: academic structure (years, semesters, groups, tracks history)
-- Source: HEI_Graduate_Management_MLD.sql (academic structure section)

CREATE TABLE academic_year (
    academic_year_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    label             VARCHAR(20) NOT NULL UNIQUE, -- e.g. 2024-2025
    start_date        DATE NOT NULL,
    end_date          DATE NOT NULL
);

CREATE TABLE semester (
    semester_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    number             INT NOT NULL CHECK (number BETWEEN 1 AND 6),
    academic_year_id   UUID NOT NULL REFERENCES academic_year(academic_year_id),
    start_date         DATE NOT NULL,
    end_date           DATE NOT NULL,
    UNIQUE(number, academic_year_id)
);

-- Explicit checkpoint: an admin action ("finalize semester"), computed and
-- inserted by the Java service - NOT a synchronous DB trigger (would block
-- incrementally adding course offerings one at a time).
CREATE TABLE semester_credit_validation (
    validation_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    semester_id     UUID NOT NULL REFERENCES semester(semester_id),
    track_id        UUID REFERENCES track(track_id),
    total_credits   INT NOT NULL CHECK (total_credits = 30),
    validated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    validated_by    UUID NOT NULL REFERENCES users(user_id),
    UNIQUE(semester_id, track_id)
);

CREATE TABLE groups (
    group_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference  VARCHAR(20) NOT NULL, -- K1, K2, N1...
    cohort_id  UUID NOT NULL REFERENCES cohort(cohort_id),
    track_id   UUID REFERENCES track(track_id), -- NULL = common-core group
    UNIQUE(reference, cohort_id)
);

-- Historized tracking of student mobility between groups
CREATE TABLE student_group_history (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id     UUID NOT NULL REFERENCES student(user_id),
    group_id       UUID NOT NULL REFERENCES groups(group_id),
    start_date     DATE NOT NULL,
    end_date       DATE, -- NULL = current assignment
    change_reason  VARCHAR(255)
);
CREATE INDEX idx_sgh_student ON student_group_history(student_id);

-- Historized tracking of specialization changes (EL / TN)
CREATE TABLE student_track_history (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id     UUID NOT NULL REFERENCES student(user_id),
    track_id       UUID NOT NULL REFERENCES track(track_id),
    start_date     DATE NOT NULL,
    end_date       DATE, -- NULL = current
    change_reason  VARCHAR(255)
);
CREATE INDEX idx_sth_student ON student_track_history(student_id);

-- Helper: track valid for a student on a given date (defaults to today)
CREATE OR REPLACE FUNCTION fn_student_track_at(p_student_id UUID, p_date DATE DEFAULT CURRENT_DATE)
RETURNS UUID AS $$
    SELECT track_id
    FROM student_track_history
    WHERE student_id = p_student_id
      AND start_date <= p_date
      AND (end_date IS NULL OR end_date >= p_date)
    ORDER BY start_date DESC
    LIMIT 1;
$$ LANGUAGE sql STABLE;
