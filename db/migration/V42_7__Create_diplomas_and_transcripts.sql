-- HEI - Graduate Management System
-- Migration V42_7: diplomas and transcripts
-- Source: HEI_Graduate_Management_MLD.sql (graduation and transcripts section)

CREATE TABLE diploma (
    diploma_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id         UUID NOT NULL UNIQUE REFERENCES student(user_id),
    cohort_id          UUID NOT NULL REFERENCES cohort(cohort_id),
    track_id           UUID NOT NULL REFERENCES track(track_id),
    overall_average    DECIMAL(5,2) NOT NULL,
    rank               INT NOT NULL,
    graduation_date    DATE NOT NULL DEFAULT CURRENT_DATE,
    list_generated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- type: PROVISIONAL (mid-year, incomplete) | FULL (year-end, complete) | DIPLOMA (final certificate)
-- overall_average / credits_earned only populated for FULL and DIPLOMA
CREATE TABLE transcript (
    transcript_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id         UUID NOT NULL REFERENCES student(user_id),
    type               VARCHAR(20) NOT NULL CHECK (type IN ('PROVISIONAL','FULL','DIPLOMA')),
    semester_id        UUID REFERENCES semester(semester_id),
    academic_year_id   UUID REFERENCES academic_year(academic_year_id),
    diploma_id         UUID REFERENCES diploma(diploma_id), -- set when type = DIPLOMA
    overall_average    DECIMAL(5,2),
    credits_earned     INT,
    generated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    storage_key        VARCHAR(500), -- S3 object key
    sent_at            TIMESTAMPTZ,
    recipient_email    VARCHAR(255),
    CHECK (type = 'PROVISIONAL' OR (overall_average IS NOT NULL AND credits_earned IS NOT NULL)),
    CHECK (type != 'PROVISIONAL' OR (overall_average IS NULL AND credits_earned IS NULL)),
    CHECK (type = 'DIPLOMA' OR diploma_id IS NULL)
);

-- Freezes exactly which offerings/scores fed a given transcript, so a later
-- grade correction never silently changes a document already handed out.
CREATE TABLE transcript_detail (
    detail_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transcript_id   UUID NOT NULL REFERENCES transcript(transcript_id) ON DELETE CASCADE,
    offering_id     UUID NOT NULL REFERENCES course_offering(offering_id),
    course_score    DECIMAL(5,2),
    credits_earned  BOOLEAN NOT NULL, -- TRUE if course_score >= 10 at generation time
    UNIQUE(transcript_id, offering_id)
);
CREATE INDEX idx_transcript_detail_transcript ON transcript_detail(transcript_id);
