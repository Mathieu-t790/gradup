-- HEI - Graduate Management System
-- Migration V42_5: courses, offerings, teacher assignments, exams
-- Source: HEI_Graduate_Management_MLD.sql (courses and offerings section)

CREATE TABLE course (
    course_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference        VARCHAR(30) NOT NULL UNIQUE, -- Pro1, Web1... (chosen by staff, not generated)
    title            VARCHAR(200) NOT NULL,
    credits          INT NOT NULL CHECK (credits > 0),
    semester_number  INT NOT NULL CHECK (semester_number BETWEEN 1 AND 6),
    track_id         UUID REFERENCES track(track_id) -- NULL = common to EL+TN
);

-- Instance of a course for a given group / semester
CREATE TABLE course_offering (
    offering_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id          UUID NOT NULL REFERENCES course(course_id),
    group_id           UUID NOT NULL REFERENCES groups(group_id),
    semester_id        UUID NOT NULL REFERENCES semester(semester_id),
    grading_finalized  BOOLEAN NOT NULL DEFAULT FALSE, -- set by the Java service once exam weights sum to exactly 1
    UNIQUE(course_id, group_id, semester_id)
);

CREATE TABLE teacher_assignment (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    offering_id  UUID NOT NULL REFERENCES course_offering(offering_id),
    teacher_id   UUID NOT NULL REFERENCES teacher(user_id),
    UNIQUE(offering_id, teacher_id)
);

CREATE TABLE exam (
    exam_id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    offering_id         UUID NOT NULL REFERENCES course_offering(offering_id),
    label               VARCHAR(100) NOT NULL, -- Midterm, Final exam...
    exam_date           DATE,
    exam_time           TIME,
    weight_numerator    INT NOT NULL CHECK (weight_numerator > 0),
    weight_denominator  INT NOT NULL CHECK (weight_denominator > 0)
);

-- Defense in depth only: rejects a weight sum that would EXCEED 1.
-- Does NOT require the sum to equal exactly 1 - that would block
-- incrementally adding exams (CC1 1/4, CC2 1/4, Final 1/2 one at a time).
-- The exact-equals-1 check belongs to the Java service, run when the
-- teacher/admin finalizes grading (see course_offering.grading_finalized).
CREATE OR REPLACE FUNCTION fn_check_exam_weight_not_exceeded() RETURNS TRIGGER AS $$
DECLARE
    total_weight DECIMAL;
BEGIN
    SELECT COALESCE(SUM(weight_numerator::DECIMAL / weight_denominator), 0)
    INTO total_weight
    FROM exam
    WHERE offering_id = NEW.offering_id
      AND exam_id IS DISTINCT FROM NEW.exam_id;

    total_weight := total_weight + (NEW.weight_numerator::DECIMAL / NEW.weight_denominator);

    IF total_weight > 1 THEN
        RAISE EXCEPTION 'Sum of exam weights for this offering would exceed 1 (got %)', total_weight;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_exam_weight_not_exceeded
BEFORE INSERT OR UPDATE ON exam
FOR EACH ROW EXECUTE FUNCTION fn_check_exam_weight_not_exceeded();

-- Helper for the Java service to call before setting grading_finalized = true
CREATE OR REPLACE FUNCTION fn_offering_exam_weight_sum(p_offering_id UUID)
RETURNS DECIMAL AS $$
    SELECT COALESCE(SUM(weight_numerator::DECIMAL / weight_denominator), 0)
    FROM exam
    WHERE offering_id = p_offering_id;
$$ LANGUAGE sql STABLE;
