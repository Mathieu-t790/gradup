# Architecture

[← Back to README](../README.md)

## Layered view

```mermaid
flowchart TB
    subgraph L1["REST Controllers"]
        direction LR
        A1[AcademicYear] --- A2[Cohort] --- A3[Track] --- A4[Group]
        A5[Semester] --- A6[Student] --- A7[Teacher] --- A8[Course]
        A9[CourseOffering] --- A10[Exam] --- A11[Grade]
        A12[GradeDispute] --- A13[Transcript] --- A14[Diploma]
    end

    L1 --> L2["Services (19)<br/>business logic · validation · event publishing"]
    L2 --> L3["MapStruct Mappers (17)"]
    L2 --> L4["Custom Authorizers (4)"]
    L2 --> L5["Spring Data JPA Repositories (27)"]
    L5 --> L6[("PostgreSQL<br/>Flyway migrations (8)")]
    L2 --> L7["AWS: SES · S3 · SQS · EventBridge"]
    L2 --> L8["Thymeleaf → PDF (Flying Saucer)"]
```

## Design patterns

- **OpenAPI-first** — REST models are generated from [`doc/api.yml`](../doc/api.yml) via OpenAPI Generator; the spec is the single source of truth.
- **Domain / JPA separation** — pure Java records (`model.*`) for business logic; JPA entities (`repository.model.J*`) for persistence.
- **Custom authorizers** — route-level Spring Security authorization managers: `GradeAuthorizer`, `OfferingAuthorizer`, `StudentAuthorizer`, `OwnerAuthorizer`.
- **Event-driven** — EventBridge + SQS for durable async processing (transcript generation notifications, UUID tracking).
- **Virtual threads** — `Workers.java` dispatches async tasks on Java 21 virtual threads.

## Project structure

```
gradup/
├── doc/api.yml                    # OpenAPI 3.0.3 spec (source of truth)
├── src/main/java/app/mata/gradup/
│   ├── endpoint/rest/controller/  # REST controllers (14 domains + health)
│   ├── endpoint/event/            # EventBridge/SQS producers & consumers
│   ├── security/                  # SecurityConf, authorizers, error handlers
│   ├── service/                   # Business logic (19 services)
│   ├── repository/                # Spring Data JPA (27 repos)
│   ├── model/                     # Domain records (pure Java)
│   ├── mapper/                    # MapStruct mappers (17)
│   ├── mail/ · file/ · handler/   # SES, S3, Lambda handlers
│   └── concurrency/                # Virtual thread workers
├── src/main/resources/db/migration/ # Flyway SQL
└── src/test/java/app/mata/gradup/   # Integration tests (20 classes)
```