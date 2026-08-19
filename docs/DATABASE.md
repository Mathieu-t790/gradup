# Database

[← Back to README](../README.md)

PostgreSQL with Flyway-managed migrations in `src/main/resources/db/migration/`.

```mermaid
erDiagram
    USERS ||--o| STUDENT : is
    USERS ||--o| TEACHER : is
    USERS ||--o| ADMIN : is
    STUDENT ||--o{ STUDENT_GROUP_HISTORY : has
    STUDENT ||--o{ STUDENT_TRACK_HISTORY : has
    TEACHER ||--o{ TEACHER_ASSIGNMENT : has

    ACADEMIC_YEAR ||--o{ SEMESTER : contains
    SEMESTER ||--o{ SEMESTER_CREDIT_VALIDATION : validates
    SEMESTER ||--o{ COURSE_OFFERING : schedules
    COHORT ||--o{ GROUP : contains
    TRACK ||--o{ COURSE : defines
    COURSE ||--o{ COURSE_OFFERING : offered_as

    COURSE_OFFERING ||--o{ EXAM : has
    EXAM ||--o{ GRADE : produces
    GRADE ||--o{ GRADE_HISTORY : audited_by
    GRADE ||--o{ GRADE_DISPUTE : disputed_by

    STUDENT ||--o{ TRANSCRIPT : owns
    TRANSCRIPT ||--o{ TRANSCRIPT_DETAIL : contains
    COHORT ||--o{ DIPLOMA : issues
```

## Key design decisions

- **Historized tracking** — `student_group_history` and `student_track_history` use `start_date` / `end_date` (`NULL` = current).
- **Grade audit trail** — `grade_history` populated by a DB trigger on `grade.score` UPDATE.
- **UUID primary keys** — all tables use `gen_random_uuid()`.
- **Reference codes** — students `STD\d{5}`, teachers `TCH\d{5}`, admins `ADM\d{5}`.
- **Timestamps** — `TIMESTAMPTZ` everywhere.
- **Frozen scores** — `transcript_detail` freezes scores at PDF generation time.
- **Dispute workflow** — `grade_dispute` is a state machine: `PENDING → REVIEWING → RESOLVED/REJECTED`.