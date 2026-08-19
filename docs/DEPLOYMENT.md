# Deployment

[← Back to README](../README.md)

GradUp is generated from the [poja-starter-template](https://github.com/poja-app/poja-starter-template)
and deployed on **Poja** (https://poja.io) as an **AWS Lambda** application, region `eu-west-3`.

## Environments = branches

Each environment is a Git branch: **pushing to that branch deploys it**.

| Environment | Branch    | Trigger                                             |
|-------------|-----------|-----------------------------------------------------|
| Preprod     | `preprod` | push to `preprod` (or manual `workflow_dispatch`)   |
| Prod        | `prod`    | push to `prod` (or manual `workflow_dispatch`)      |

Every push (and every pull request) also runs **CI** — [`.github/workflows/ci.yml`](../.github/workflows/ci.yml):
`./gradlew test` (regenerates and publishes the OpenAPI client, then runs the full test suite with
JaCoCo ≥ 80 %) plus a Google-Java-Format diff check (`format.sh`).

## How the deployment works

Deployment is driven by [`.github/workflows/cd-compute.yml`](../.github/workflows/cd-compute.yml):

1. **Checkout** the pushed branch and report the run state to the Poja API
   (`https://api.prod.poja.io/gh-repos/<owner>/<repo>/github-workflow-state`).
2. **Build** the Lambda bundle:
   - `sam build` — the SAM `template.yml` (Lambda + function URL, S3, SQS, EventBridge, IAM) is
     downloaded from the Poja API for the target environment.
   - `compileJava` depends on `publishJavaClientToMavenLocal`: the OpenAPI client is regenerated
     from [`doc/api.yml`](../doc/api.yml) and published to the local Maven repo via
     `.shell/publish_gen_to_maven_local.sh` (see `build.gradle`).
3. **Upload** the resulting `.aws-sam` bundle to a Poja-managed bucket.
4. **Trigger the deployment** on the Poja API
   (`PUT .../env-deploys?environment_type=PREPROD|PROD`): Poja provisions/updates the
   CloudFormation stack and applies the **environment configuration** (env vars below).

The environment configuration is **not stored in the repo**: it is managed in the
[Poja portal](https://poja.io) as an "env conf". A deploy uses the latest saved conf unless an
explicit `env_conf_id` is supplied.

## Deploying

### Normal flow

Push the branch you want to go live:

```bash
git push origin preprod   # deploy to preprod
git push origin prod      # deploy to prod (after validating on preprod)
```

### Manual redeploy / custom config

In **Actions → CD Compute → Run workflow**:

| Input           | Effect                                                              |
|-----------------|---------------------------------------------------------------------|
| `env_conf_id`   | Deploy using a specific environment config (creates a new deployment). |
| `deployment_id` | Redeploy an existing deployment, reusing its config.                |

Both inputs are optional; when neither is given, the latest saved env conf is used.

## What is deployed

- **Compute** — AWS Lambda, Java 21 (corretto), Spring Boot 3.2.2 via
  `aws-serverless-java-container-springboot3`, exposed through a **Lambda function URL**
  (`https://<url-id>.lambda-url.<region>.on.aws/`). A companion **worker** Lambda consumes the
  SQS event stacks for async processing.
- **Async** — SQS queues + an EventBridge bus: durable, event-driven processing
  (transcript generation and email dispatch).
- **Storage** — S3 bucket: generated transcript PDFs and diploma XLSX exports.
- **Mail** — SES: credentials emails and transcript emails.
- **Database** — external **Neon PostgreSQL**, migrated by Flyway at startup. Also stores the
  Spring Session JDBC `SPRING_SESSION` tables, so login sessions survive Lambda cold starts.

## Environment variables

Set per environment in the Poja env conf:

| Variable | Spring property | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` | Neon PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `spring.datasource.username` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` | DB password |
| `SPRING_SESSION_STORE_TYPE` | `spring.session.store-type` | Session store — `jdbc` (sessions persist in PostgreSQL, survive Lambda cold starts) |
| `SPRING_SESSION_JDBC_INITIALIZE_SCHEMA` | `spring.session.jdbc.initialize-schema` | `always` — auto-creates the `SPRING_SESSION` tables on non-embedded databases |
| `SPRING_DATA_WEB_PAGEABLE_DEFAULT_PAGE_SIZE` | `spring.data.web.pageable.default-page-size` | Default page size (`50`) |
| `SPRING_DATA_WEB_PAGEABLE_MAX_PAGE_SIZE` | `spring.data.web.pageable.max-page-size` | Max page size (`200`) |
| `ADMIN_EMAIL` | `admin.email` | Bootstrap admin email |
| `ADMIN_PASSWORD` | `admin.password` | Bootstrap admin password |

Local development uses a standard Spring Boot embedded Tomcat with a local/Docker/Neon PostgreSQL
and the datasource env vars above.

## Prerequisites (local development)

- **Java 21** (eclipse-temurin or equivalent)
- **Docker** (Testcontainers PostgreSQL for the test suite)
- **Gradle 8.5+** (wrapper included)
- **PostgreSQL** (Neon, local, or Docker) and the `SPRING_DATASOURCE_*` env vars
- `chmod +x gradlew .shell/publish_gen_to_maven_local.sh` (generated-client publication step)