# Testing

[← Back to README](../README.md)

All integration tests use **Testcontainers** (PostgreSQL in Docker) — no external DB needed.

```bash
./gradlew test            # full suite
```

JaCoCo enforces **80% minimum line coverage**. Reports: `build/reports/jacoco/test/`.

## Test suite

| Test | Coverage |
|---|---|
| `AcademicYearIT` / `CohortIT` / `TrackIT` / `GroupIT` | Core structure CRUD |
| `SemesterIT` / `SemesterFinalizeIT` | Semester CRUD, credit validation |
| `StudentIT` / `StudentBusinessIT` | Student CRUD, group/track history, eligibility |
| `TeacherIT` | Teacher CRUD, credentials email, course offerings |
| `CourseIT` / `CourseOfferingIT` / `CourseOfferingExamIT` | Course & offering CRUD, teacher assignment, exams |
| `ExamIT` / `GradeIT` / `GradeWriteIT` | Exam CRUD, grade recording, audit trail |
| `GradeDisputeIT` / `GradeDisputeWriteIT` | Dispute queue, role-based filtering, create/resolve |
| `TranscriptIT` | Transcript generation (PDF golden files) |
| `DiplomaIT` | Diploma generation |
| `SecurityIT` | Auth, role enforcement |