# API Reference

[← Back to README](../README.md)

Full spec: [`doc/api.yml`](../doc/api.yml) (1750+ lines, OpenAPI 3.0.3) — or run the app and open `/swagger-ui/index.html`.

Auth is session-based (cookie). All endpoints are prefixed with `/`.

## Roles

| Role | Access |
|---|---|
| `ADMIN` | Full access — manages structure, teachers, students, grades |
| `TEACHER` | Views assigned offerings, exams, grades; resolves disputes |
| `STUDENT` | Views own profile, grades, transcripts; creates grade disputes |

## Key endpoints

```
GET  /ping                                    → health check
POST /login                                   → authenticate (form)

GET  /academic-years        POST /academic-years                (ADMIN)
GET  /semesters?academicYearId=
POST /semesters                                                 (ADMIN)
POST /semesters/{id}/finalize                                   (ADMIN)

GET  /cohorts               POST /cohorts                       (ADMIN)
GET  /tracks                POST /tracks                        (ADMIN)
GET  /groups?cohortId=&trackId=
POST /groups                                                    (ADMIN)

GET  /students?cohortId=&groupId=             → paginated list
POST /students                                                  (ADMIN)
GET  /students/{id}                           → detail
PATCH /students/{id}                                             (ADMIN)
GET  /students/{id}/grades
GET  /students/{id}/graduation-eligibility
GET  /students/{id}/transcripts
POST /students/{id}/transcripts               → generate (PDF)
GET  /students/{id}/disputes

GET  /teachers               POST /teachers                     (ADMIN)
GET  /teachers/{id}/course-offerings

GET  /courses?trackId=&semesterNumber=
POST /courses                POST /courses  PATCH /courses/{id} (ADMIN)

GET  /course-offerings                        → paginated
POST /course-offerings                                          (ADMIN)
POST   /course-offerings/{id}/teachers/{id}   → assign           (ADMIN)
DELETE /course-offerings/{id}/teachers/{id}   → unassign         (ADMIN)

GET  /course-offerings/{id}/exams
POST /course-offerings/{id}/exams
PATCH /exams/{id}
GET  /exams/{id}/grades
POST /exams/{id}/grades
PUT  /grades/{id}                             → update (audit trail)
GET  /grades/{id}/history

GET  /disputes?status=PENDING                 → paginated
POST /grades/{id}/disputes                    → create (STUDENT)
PATCH /disputes/{id}                          → resolve

GET  /cohorts/{id}/diplomas                   → generate (PDF)
GET  /cohorts/{id}/diplomas/export            → export (XLSX)
POST /cohorts/{id}/diplomas/generate          → generate all      (ADMIN)
```