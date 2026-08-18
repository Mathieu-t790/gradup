# GradUp

> HEI Graduate Management System — API for managing students, teachers, courses, grades, transcripts, and diplomas across the 3-year EL/TN tracks at HEI.

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     REST Controllers                     │
│  (AcademicYear · Cohort · Track · Group · Semester ·     │
│   Student · Teacher · Course · CourseOffering · Exam ·   │
│   Grade · GradeDispute · Transcript · Diploma)           │
├─────────────────────────────────────────────────────────┤
│                      Services (19)                       │
│  Business logic, validation, event publishing            │
├─────────────────────────────────────────────────────────┤
│  MapStruct Mappers (17)  │  Custom Authorizers (4)       │
├─────────────────────────────────────────────────────────┤
│               Spring Data JPA Repositories (27)          │
├─────────────────────────────────────────────────────────┤
│              PostgreSQL · Flyway Migrations (8)           │
├─────────────────────────────────────────────────────────┤
│   AWS (SES · S3 · SQS · EventBridge)  │  Thymeleaf PDF  │
└─────────────────────────────────────────────────────────┘
```

**Design patterns:**
- **OpenAPI-first**: REST models are generated from `doc/api.yml` via OpenAPI Generator — the spec is the single source of truth.
- **Domain / JPA separation**: Pure Java records (`model.*`) for business logic; JPA entities (`repository.model.J*`) for persistence.
- **Custom authorizers**: Route-level Spring Security authorization managers (`GradeAuthorizer`, `OfferingAuthorizer`, `StudentAuthorizer`, `OwnerAuthorizer`).
- **Event-driven**: EventBridge + SQS for durable, asynchronous processing (transcript generation notifications, UUID tracking).
- **Virtual threads**: `Workers.java` dispatches async tasks on Java 21 virtual threads.

---

## Features

| Domain | What it does |
|--------|-------------|
| **Academic structure** | Manage academic years, semesters (1-6), tracks (EL / TN), cohorts, groups |
| **Students** | Create, update, track group/track history, list paginated with filters |
| **Teachers** | Create with auto-generated credentials, list assigned course offerings |
| **Courses** | CRUD with track binding and semester number |
| **Course offerings** | Link course + group + semester, assign/unassign teachers, finalize grading |
| **Exams** | Create/update per offering with weight fractions |
| **Grades** | Record, update (with automatic history audit trail), list per exam |
| **Grade disputes** | Students create disputes; ADMIN/TEACHER resolves (RESOLVED / REJECTED) with optional score override |
| **Transcripts** | Generate provisional / full / diploma transcripts as PDF (Flying Saucer + Thymeleaf) |
| **Diplomas** | Generate per-cohort or bulk diploma PDFs, export as XLSX |
| **Graduation eligibility** | Compute credits, GPA, rank, pass/fail per student |
| **Semester finalize** | Admin action: validate 30 credits per track at semester end |
| **Email** | Send credential emails on teacher creation (AWS SES) |
| **Health checks** | `/ping`, `/health/db`, `/health/bucket`, `/health/email`, `/health/event` |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 (virtual threads) |
| Framework | Spring Boot 3.2.2 |
| Security | Spring Security (session-based, role-based access) |
| ORM | Spring Data JPA + Hibernate |
| Database | PostgreSQL (Neon in production, Testcontainers in tests) |
| Migrations | Flyway 9.22 |
| API spec | OpenAPI 3.0.3 (`doc/api.yml`) |
| Code generation | OpenAPI Generator 7.7 (Java + TypeScript clients) |
| Mapping | MapStruct 1.6 |
| PDF | Flying Saucer 9.1 + Thymeleaf XHTML |
| Email | AWS SES via Jakarta Mail |
| File storage | AWS S3 |
| Async | AWS SQS + EventBridge |
| Build | Gradle 8.5 |
| Coverage | JaCoCo (80% line minimum) |
| Testing | JUnit 5 + Testcontainers + Spring Boot Test |
| Utilities | Lombok, Jackson, reflections |

---

## Prerequisites

- **Java 21** (eclipse-temurin or equivalent)
- **Docker** (for Testcontainers PostgreSQL in tests)
- **Gradle 8.5+** (wrapper included)
- **PostgreSQL** (Neon, local, or Docker for development)

---

## Getting Started

### 1. Clone

```bash
git clone https://github.com/Mathieu-t790/gradup.git
cd gradup
```

### 2. Environment Variables

The application expects these environment variables (or system properties):

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/gradup` |
| `SPRING_DATASOURCE_USERNAME` | DB username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | `secret` |
| `AWS SES_*` | Email configuration | — |
| `AWS S3_*` | Bucket configuration | — |
| `AWS SQS_*` | Queue configuration | — |

### 3. Run

```bash
./gradlew bootRun
```

The API starts on `http://localhost:8080`.

### 4. Swagger UI

Open `http://localhost:8080/swagger-ui/index.html` for interactive API documentation.

---

## API Overview

All endpoints are prefixed with `/`. Authentication is session-based (cookie).

### Roles

| Role | Access |
|------|--------|
| `ADMIN` | Full access — manages structure, teachers, students, grades |
| `TEACHER` | Views assigned offerings, exams, grades; resolves disputes |
| `STUDENT` | Views own profile, grades, transcripts; creates grade disputes |

### Key Endpoints

```
GET  /ping                                    → health check
POST /login                                   → authenticate (form)

GET  /academic-years                          → list years
POST /academic-years                          → create year (ADMIN)

GET  /semesters?academicYearId=               → list semesters
POST /semesters                               → create semester (ADMIN)
POST /semesters/{id}/finalize                 → finalize credits (ADMIN)

GET  /cohorts                                 → list cohorts
POST /cohorts                                 → create cohort (ADMIN)

GET  /tracks                                  → list tracks (EL, TN)
POST /tracks                                  → create track (ADMIN)

GET  /groups?cohortId=&trackId=               → list groups
POST /groups                                  → create group (ADMIN)

GET  /students?cohortId=&groupId=             → list students (paginated)
POST /students                                → create student (ADMIN)
GET  /students/{id}                           → student detail
PATCH /students/{id}                          → update student (ADMIN)
GET  /students/{id}/grades                    → student grades
GET  /students/{id}/graduation-eligibility    → eligibility check
GET  /students/{id}/transcripts               → list transcripts
POST /students/{id}/transcripts               → generate transcript (PDF)
GET  /students/{id}/disputes                  → student disputes

GET  /teachers                                → list teachers (ADMIN)
POST /teachers                                → create teacher (ADMIN)
GET  /teachers/{id}/course-offerings          → teacher's offerings

GET  /courses?trackId=&semesterNumber=        → list courses
POST /courses                                 → create course (ADMIN)
PATCH /courses/{id}                           → update course (ADMIN)

GET  /course-offerings                        → list offerings (paginated)
POST /course-offerings                        → create offering (ADMIN)
POST /course-offerings/{id}/teachers/{id}     → assign teacher (ADMIN)
DELETE /course-offerings/{id}/teachers/{id}   → unassign teacher (ADMIN)

GET  /course-offerings/{id}/exams             → list exams
POST /course-offerings/{id}/exams             → create exam
PATCH /exams/{id}                             → update exam

GET  /exams/{id}/grades                       → list grades
POST /exams/{id}/grades                       → record grade
PUT  /grades/{id}                             → update grade (audit trail)
GET  /grades/{id}/history                     → grade change history

GET  /disputes?status=PENDING                 → list disputes (paginated)
POST /grades/{id}/disputes                    → create dispute (STUDENT)
PATCH /disputes/{id}                          → resolve dispute

GET  /cohorts/{id}/diplomas                   → generate diplomas (PDF)
GET  /cohorts/{id}/diplomas/export            → export diplomas (XLSX)
POST /cohorts/{id}/diplomas/generate          → generate all diplomas (ADMIN)
```

Full specification: [`doc/api.yml`](doc/api.yml) (1750+ lines)

---

## Database

PostgreSQL with Flyway-managed migrations in `src/main/resources/db/migration/`.

```
users ─┬─ student ──── student_group_history
       │              ──── student_track_history
       ├─ teacher ──── teacher_assignment
       └─ admin

academic_year ─── semester ──── semester_credit_validation
                               ──── course_offering ──── exam ──── grade ──── grade_history
                                                 │                    ──── grade_dispute
cohort ──── group                                │
track  ──── course ──────────────────────────────┘

diploma ──── transcript ──── transcript_detail
```

Key design decisions:
- **Historized tracking**: `student_group_history` and `student_track_history` with `start_date` / `end_date` (NULL = current).
- **Grade audit trail**: `grade_history` table populated by DB trigger on `grade.score` UPDATE.
- **UUID primary keys**: All tables use `gen_random_uuid()`.
- **Reference codes**: Students (`STD\d{5}`), teachers (`TCH\d{5}`), admins (`ADM\d{5}`).

---

## Testing

All integration tests use **Testcontainers** (PostgreSQL in Docker) — no external DB needed.

### Run a single test class

```bash
./run-test.sh TeacherIT
```

### Run all tests

```bash
./gradlew test
```

### Test suite

| Test | Coverage |
|------|----------|
| `AcademicYearIT` | Year CRUD |
| `CohortIT` | Cohort CRUD |
| `TrackIT` | Track CRUD |
| `GroupIT` | Group CRUD |
| `SemesterIT` | Semester CRUD + finalize |
| `SemesterFinalizeIT` | Credit validation |
| `StudentIT` | Student CRUD, group/track history |
| `StudentBusinessIT` | Eligibility, graduation |
| `TeacherIT` | Teacher CRUD, credentials email, course offerings |
| `CourseIT` | Course CRUD |
| `CourseOfferingIT` | Offering CRUD, teacher assignment |
| `CourseOfferingExamIT` | Exam + grade recording |
| `ExamIT` | Exam CRUD |
| `GradeIT` | Grade CRUD, history |
| `GradeWriteIT` | Grade update audit trail |
| `GradeDisputeIT` | Dispute queue, role-based filtering |
| `GradeDisputeWriteIT` | Dispute create/resolve |
| `TranscriptIT` | Transcript generation (PDF golden files) |
| `DiplomaIT` | Diploma generation |
| `SecurityIT` | Auth, role enforcement |

### Coverage

JaCoCo enforces **80% minimum line coverage**. Reports are generated at `build/reports/jacoco/test/`.

---

## Project Structure

```
gradup/
├── doc/
│   └── api.yml                          # OpenAPI 3.0.3 spec (source of truth)
├── src/
│   ├── main/java/app/mata/gradup/
│   │   ├── endpoint/
│   │   │   ├── rest/controller/         # REST controllers (14 domain + health)
│   │   │   ├── event/                   # EventBridge/SQS producers & consumers
│   │   │   └── PageConf.java            # Pagination config
│   │   ├── security/
│   │   │   ├── conf/                    # SecurityConf, RoutePolicy, Crypto
│   │   │   ├── authorization/           # Custom authorizers
│   │   │   ├── userDetails/             # JUserDetails, UserDetailsService
│   │   │   └── error/                   # 401/403 handlers
│   │   ├── service/                     # Business logic (19 services)
│   │   │   └── utils/                   # PDF, HTML, XLSX renderers, ranking
│   │   ├── repository/                  # Spring Data JPA (27 repos)
│   │   │   └── model/                   # JPA entities (J* prefix)
│   │   ├── model/                       # Domain records (pure Java)
│   │   ├── mapper/                      # MapStruct mappers (17)
│   │   ├── mail/                        # Email (SES), EmailAddressVerifier
│   │   ├── file/                        # S3 bucket, hash, zip utilities
│   │   ├── handler/                     # AWS Lambda handlers
│   │   ├── exception/                   # Custom exceptions + ErrorHandler
│   │   ├── concurrency/                 # Virtual thread workers
│   │   └── GradUpApplication.java       # Entry point
│   ├── main/resources/
│   │   ├── db/migration/                # Flyway SQL (8 migrations)
│   │   ├── templates/                   # Thymeleaf (PDF, email)
│   │   └── xlsx/                        # XLSX templates
│   └── test/java/app/mata/gradup/
│       ├── conf/
│       │   ├── SecuredFacadeIT.java     # Base test class (login helpers)
│       │   └── TestDataSeeder.java      # One-liner test data factory
│       └── *IT.java                     # Integration tests (20 classes)
├── build.gradle                         # Build config, OpenAPI gen, JaCoCo
├── run-test.sh                          # Fast single-test runner
└── format.sh                            # Code formatter
```

---

## Deployment

The application is deployed as an **AWS Lambda** behind API Gateway, using `aws-serverless-java-container-springboot3`.

- **SQS**: Durable async processing (transcript notifications)
- **EventBridge**: Event-driven architecture for cross-service communication
- **S3**: File storage for generated PDFs and exports
- **SES**: Transactional email delivery
- **Neon PostgreSQL**: Serverless PostgreSQL database

Local development uses a standard Spring Boot embedded Tomcat with Docker/Neon PostgreSQL.

---

## License

Proprietary — HEI (Haute Ecole d'Informatique).
