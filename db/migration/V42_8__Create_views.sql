-- HEI - Graduate Management System
-- Migration V42_8: derived views
-- Source: HEI_Graduate_Management_MLD.sql (views section)

-- View: a student's average for a given course (per offering) - always
-- derived from actual grades, never from current group membership.
CREATE VIEW v_course_average AS
SELECT
    g.student_id,
    co.offering_id,
    co.course_id,
    SUM(g.score * ex.weight_numerator::decimal / ex.weight_denominator)
        / SUM(ex.weight_numerator::decimal / ex.weight_denominator) AS average
FROM grade g
JOIN exam ex             ON ex.exam_id = g.exam_id
JOIN course_offering co  ON co.offering_id = ex.offering_id
GROUP BY g.student_id, co.offering_id, co.course_id;

-- View: graduation eligibility (average >= 10 in EVERY course assigned
-- over the 3-year track — a single course below 10 disqualifies the student)
CREATE VIEW v_graduation_eligibility AS
SELECT
    s.user_id AS student_id,
    s.cohort_id,
    fn_student_track_at(s.user_id) AS track_id,
    COUNT(*) AS total_courses,
    COUNT(*) FILTER (WHERE ca.average >= 10) AS passed_courses,
    BOOL_AND(ca.average >= 10) AS is_eligible,
    AVG(ca.average) AS overall_average,
    MIN(ca.average) AS min_grade,
    MAX(ca.average) AS max_grade
FROM student s
JOIN v_course_average ca ON ca.student_id = s.user_id
GROUP BY s.user_id, s.cohort_id;
