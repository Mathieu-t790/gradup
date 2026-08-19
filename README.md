<div align="center">

# GradUp

**Graduate management system for HEI (and can be for others too)** — students, teachers, courses, grades, transcripts & diplomas across the 3-year EL/TN tracks.

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.2-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Neon-4169E1?logo=postgresql&logoColor=white)](https://neon.tech)
[![Coverage](https://img.shields.io/badge/coverage-80%25%20min-brightgreen)](docs/TESTING.md)
[![License](https://img.shields.io/badge/license-Proprietary-lightgrey)](#license)

[Quick Start](#quick-start) · [Docs](#documentation) · [API Reference](doc/api.yml)

</div>

---

## Overview

GradUp is a Spring Boot REST API that manages the full academic lifecycle at HEI: academic years, cohorts, course offerings, exams, grades (with dispute workflow), transcripts and diplomas. Deployed on **[POJA](https://poja.io)** as an AWS Lambda with async processing via SQS/EventBridge.

```mermaid
flowchart TB
    subgraph API["REST Layer"]
        C[Controllers<br/>14 domains]
    end
    subgraph BIZ["Business Layer"]
        S[19 Services] --> M[MapStruct Mappers]
        S --> AUTH[Custom Authorizers]
    end
    subgraph DATA["Data Layer"]
        R[27 JPA Repositories] --> PG[(PostgreSQL<br/>8 migrations)]
    end
    subgraph ASYNC["Async & I/O"]
        SQS[SQS + EventBridge]
        PDF[Thymeleaf → PDF]
        AWS[S3 · SES]
    end

    C --> S
    S --> R
    S --> SQS
    S --> PDF
    S --> AWS
```

**Highlights:** OpenAPI-first codegen · domain/JPA separation · event-driven transcript generation · Java 21 virtual threads.

→ Full breakdown in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

## Quick Start

```bash
git clone https://github.com/Mathieu-t790/gradup.git && cd gradup
./gradlew bootRun
```

Set these env vars first — see [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md#environment-variables) for the full list:

```
SPRING_DATASOURCE_URL / _USERNAME / _PASSWORD
```

API docs live at `http://localhost:8080/swagger-ui/index.html` once running.

## Documentation

| | |
|---|---|
| 🏗️ [Architecture](docs/ARCHITECTURE.md) | Design patterns, layering, event flow |
| ✨ [Features](docs/FEATURES.md) | What GradUp does, domain by domain |
| 🔌 [API Reference](docs/API.md) | Key endpoints, roles, [full OpenAPI spec](doc/api.yml) |
| 🗄️ [Database](docs/DATABASE.md) | Schema, migrations, design decisions |
| 🧪 [Testing](docs/TESTING.md) | Test suite, coverage, running tests |
| 🚀 [Deployment](docs/DEPLOYMENT.md) | AWS Lambda setup, env vars, infra |

## Tech Stack

Java 21 · Spring Boot 3.2.2 · Spring Security · PostgreSQL/Flyway · MapStruct · Flying Saucer + Thymeleaf · AWS (Lambda, SES, S3, SQS, EventBridge) · Gradle · JUnit 5 + Testcontainers

## License

Proprietary : © 2026 GradUp contributors. All rights reserved. Open to licensing, partnerships, or acquisition — see [LICENSE](LICENSE).


<sub>Generated from [poja-app/poja-starter-template](https://github.com/poja-app/poja-starter-template)</sub>
