# Features

[← Back to README](../README.md)

| Domain | What it does |
|---|---|
| **Academic structure** | Manage academic years, semesters (1–6), tracks (EL / TN), cohorts, groups |
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

→ See [docs/API.md](API.md) for the corresponding endpoints.