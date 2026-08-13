-- HEI - Graduate Management System
-- Migration V42_3: reference code generation + users + academic base entities
-- Source: HEI_Graduate_Management_MLD.sql (users & reference section)

CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- for gen_random_uuid()

-- ---------- Reference code generation (STDyyNNN, TCHyyNNN, ADMyyNNN) ----------
CREATE TABLE reference_counter (
    role           VARCHAR(20) NOT NULL,
    year_suffix    CHAR(2) NOT NULL,
    last_sequence  INT NOT NULL DEFAULT 0,
    PRIMARY KEY (role, year_suffix)
);

CREATE OR REPLACE FUNCTION fn_generate_user_reference(p_role VARCHAR)
RETURNS VARCHAR AS $$
DECLARE
    v_prefix      VARCHAR(3);
    v_year_suffix CHAR(2);
    v_seq         INT;
BEGIN
    v_prefix := CASE p_role
        WHEN 'STUDENT' THEN 'STD'
        WHEN 'TEACHER' THEN 'TCH'
        WHEN 'ADMIN'   THEN 'ADM'
    END;
    v_year_suffix := LPAD((EXTRACT(YEAR FROM CURRENT_DATE)::INT % 100)::TEXT, 2, '0');

    INSERT INTO reference_counter(role, year_suffix, last_sequence)
    VALUES (p_role, v_year_suffix, 1)
    ON CONFLICT (role, year_suffix)
    DO UPDATE SET last_sequence = reference_counter.last_sequence + 1
    RETURNING last_sequence INTO v_seq;

    RETURN v_prefix || v_year_suffix || LPAD(v_seq::TEXT, 3, '0');
END;
$$ LANGUAGE plpgsql;

-- ---------- Users ----------
CREATE TABLE users (
    user_id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference      VARCHAR(10) UNIQUE, -- STD24192, TCH21005, ADM24003... set by trigger below
    last_name      VARCHAR(100) NOT NULL,
    first_name     VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    phone          VARCHAR(30),
    role           VARCHAR(20) NOT NULL CHECK (role IN ('STUDENT','TEACHER','ADMIN')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_reference_format CHECK (reference ~ '^(STD|TCH|ADM)[0-9]{5}$')
);

CREATE OR REPLACE FUNCTION fn_set_user_reference() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.reference IS NULL THEN
        NEW.reference := fn_generate_user_reference(NEW.role);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_set_user_reference
BEFORE INSERT ON users
FOR EACH ROW EXECUTE FUNCTION fn_set_user_reference();

-- ---------- Academic base ----------
CREATE TABLE cohort (
    cohort_id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    label                    VARCHAR(100) NOT NULL,
    entry_year               INT NOT NULL,
    expected_graduation_year INT NOT NULL
);

CREATE TABLE track (
    track_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code     VARCHAR(10) NOT NULL UNIQUE CHECK (code IN ('EL','TN')),
    label    VARCHAR(100) NOT NULL
);

CREATE TABLE student (
    user_id          UUID PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    date_of_birth    DATE,
    cohort_id        UUID NOT NULL REFERENCES cohort(cohort_id),
    enrollment_date  DATE NOT NULL DEFAULT CURRENT_DATE
    -- identifier lives in users.reference; track is historized, see student_track_history
);

CREATE TABLE teacher (
    user_id   UUID PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    specialty VARCHAR(150)
);

CREATE TABLE admin (
    user_id    UUID PRIMARY KEY REFERENCES users(user_id) ON DELETE CASCADE,
    role_title VARCHAR(150)
);
