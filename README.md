# SPE Calculator CI/CD

A Calculator REST API built with Spring Boot, delivered through an automated Jenkins CI/CD pipeline that builds, tests, containerises, publishes and deploys it with Docker and Docker Compose.

Mini-project for **CSE 816 — Software Production Engineering**.

- **Docker Hub:** [manavimt2023535/spe-calculator](https://hub.docker.com/r/manavimt2023535/spe-calculator)
- **Stack:** Java 17 · Spring Boot 4.1.2 · Maven · JUnit 5 + MockMvc · Spring Boot Actuator · Docker · Docker Compose · Nginx · Jenkins

---

## API

All operations are `GET` requests that take two numeric query parameters, `a` and `b`.

| Operation | Endpoint | Example |
|---|---|---|
| Add | `/api/v1/add?a=10&b=5` | `15.0` |
| Subtract | `/api/v1/subtract?a=10&b=5` | `5.0` |
| Multiply | `/api/v1/multiply?a=10&b=5` | `50.0` |
| Divide | `/api/v1/divide?a=10&b=5` | `2.0` |
| Health | `/actuator/health` | `"status":"UP"` |

**Successful response**

```json
{"operation":"add","a":10.0,"b":5.0,"result":15.0}
```

**Error response** (HTTP 400). Returned for division by zero and for missing or non-numeric parameters:

```json
{
  "timestamp": "2026-10-30T16:12:50.602048912Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Division by zero is not allowed",
  "path": "/api/v1/divide"
}
```

For a missing or invalid parameter, `message` is `"Missing or invalid request parameter"`.

---

## Prerequisites

- Java 17 (only needed to build and run without Docker)
- Docker with the Compose plugin (`docker compose`)
- Maven is optional because the repository includes the Maven Wrapper (`./mvnw`)

---

## Build

```bash
./mvnw clean package          # compile, run tests and package the JAR
./mvnw clean package -DskipTests   # package without running tests
```

The executable JAR is written to `target/`.

## Test

```bash
./mvnw test
```

The suite (`CalculatorApplicationTests`) uses JUnit 5 and MockMvc to cover:

- all four operations
- division by zero
- a missing parameter
- an invalid parameter type
- the `timestamp` and `path` fields in error responses

JUnit XML reports are written to `target/surefire-reports/`.

---

## Run

### Option 1: Locally with Maven

```bash
./mvnw spring-boot:run
```

The API is available at `http://localhost:8080`.

### Option 2: Single Docker container

```bash
# build locally
docker build -t spe-calculator:local .
docker run --rm -p 8080:8080 spe-calculator:local

# or run a published image (replace 9 with any tag on Docker Hub)
docker run --rm -p 8080:8080 manavimt2023535/spe-calculator:9
```

### Option 3: Full stack with Docker Compose (API + Nginx)

```bash
# build the image from source and start both services
docker compose up -d --build

# or deploy a specific published image
CALCULATOR_IMAGE=docker.io/manavimt2023535/spe-calculator:9 docker compose up -d

docker compose ps        # calculator should show (healthy)
docker compose down      # stop the stack
```

Nginx listens on **host port 8081** and proxies to the calculator service. The calculator's port 8080 is only exposed inside the Compose network.

## Call the API

The examples below use the Compose stack (port 8081). Use port 8080 for options 1 and 2.

> Always quote the URL. Without quotes, Bash treats `&` as "run in background" and drops `b`.

```bash
curl "http://localhost:8081/api/v1/add?a=10&b=5"
curl "http://localhost:8081/api/v1/subtract?a=10&b=5"
curl "http://localhost:8081/api/v1/multiply?a=10&b=5"
curl "http://localhost:8081/api/v1/divide?a=10&b=5"
curl -i "http://localhost:8081/api/v1/divide?a=10&b=0"   # HTTP 400
curl "http://localhost:8081/actuator/health"
```

---

## Docker and Compose details

**Dockerfile (multi-stage)**

- **Build stage:** `maven:3.9-eclipse-temurin-17`. It copies `pom.xml` and the Maven Wrapper and runs `dependency:go-offline` before copying `src/`, so dependency layers stay cached between builds.
- **Runtime stage:** `eclipse-temurin:17-jre-jammy`. It copies only the built JAR.
- Runs as a non-root user (UID 10001).
- Declares `EXPOSE 8080`.
- Has a `HEALTHCHECK` on `/actuator/health`.
- `.dockerignore` excludes `.git`, `target/`, IDE files and `.env` files.

**`compose.yaml`**

| Service | Details |
|---|---|
| `calculator` | Image from `CALCULATOR_IMAGE` (defaults to a local build), `SERVER_PORT=8080`, `restart: unless-stopped`, health check on `/actuator/health` |
| `nginx` | `nginx:alpine`, ports `8081:80`, config from `nginx/default.conf`, starts after `calculator` is healthy |

Both services share the bridge network `calculator-network`.

---

## CI/CD pipeline (Jenkins)

The declarative pipeline in `Jenkinsfile` runs on every new commit to `main`. Jenkins uses **Poll SCM**; no GitHub webhook is configured.

| # | Stage | What it does |
|---|---|---|
| 1 | Checkout | Checks out the repository and prints the latest commit |
| 2 | Build | `./mvnw -B clean package -DskipTests` |
| 3 | Test | `./mvnw -B test`; publishes Surefire XML with the `junit` step; a failing test fails the build |
| 4 | Docker Build | `docker build --pull`, tagged `docker.io/manavimt2023535/spe-calculator:<BUILD_NUMBER>` |
| 5 | Docker Hub Push | Logs in with the Jenkins credential `dockerhub-credentials` and pushes the tag |
| 6 | Deploy | `docker compose -p calculator pull calculator`, then `up -d --no-build --force-recreate` |
| 7 | Health Check | Polls `http://localhost:8081/actuator/health` up to 30 times, 5 s apart; prints container status and logs and fails the build if unhealthy |

**Post actions**

- On success or failure, Jenkins sends an HTML email (via the Email Extension plugin) with the job, build number, status and build URL. The success email also includes the image tag.
- `disableConcurrentBuilds()` and `--force-recreate` let the pipeline be re-run safely over a running deployment.

### Jenkins setup

1. Install these plugins:
   - Pipeline
   - Git
   - JUnit
   - Credentials Binding
   - Email Extension
   - Timestamper
2. Give the Jenkins user access to Docker: `sudo usermod -aG docker jenkins`, then restart Jenkins.
3. Add a **Username with password** credential with ID `dockerhub-credentials`, using a Docker Hub access token as the password.
4. Configure email under *Manage Jenkins → System → Extended E-mail Notification*:
   - SMTP server `smtp.gmail.com`, port 587, TLS enabled
   - Authentication with a Gmail App Password stored as a Jenkins credential
5. Create a **Pipeline** job:
   - Definition: *Pipeline script from SCM* pointing to this repository's `main` branch, script path `Jenkinsfile`
   - Enable **Poll SCM** with a schedule such as `H/2 * * * *`

No credentials are stored in this repository. The Docker Hub token and Gmail App Password live only in the Jenkins credentials store.

---

## Project structure

```
.
├── src/main/java/com/spe/calculator/
│   ├── CalculatorApplication.java
│   ├── controller/CalculatorController.java    # REST endpoints
│   ├── exception/GlobalExceptionHandler.java   # structured 400 errors
│   └── model/{CalculatorResponse,ApiError}.java
├── src/main/resources/application.properties   # port 8080, Actuator health
├── src/test/java/com/spe/calculator/CalculatorApplicationTests.java
├── nginx/default.conf      # reverse proxy to calculator:8080
├── Dockerfile              # multi-stage build
├── .dockerignore
├── compose.yaml            # calculator + nginx
├── Jenkinsfile             # declarative CI/CD pipeline
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/   # Maven Wrapper
└── .gitignore
```

## Git workflow

Changes are made on feature branches and merged into `main` through Pull Requests. For example, `feature/project-documentation` was merged via PR #1. Each merge to `main` is picked up by Jenkins and deployed automatically.
