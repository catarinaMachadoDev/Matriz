# Matriz

Matriz is a team workspace for managing projects, tasks, assignments, comments, and progress. The Java 21 / Spring Boot application serves the HTML, CSS, and JavaScript interface and its JSON API. PostgreSQL stores application data and Flyway applies migrations at startup.

## Run with Docker Compose

You need Docker with Compose v2. From the project directory, copy the example environment file and set a fresh local database password.

```powershell
Copy-Item .env.example .env
notepad .env
docker compose config -q
docker compose up -d --build --wait
docker compose ps
```

Open http://localhost:8081. Change `MATRIZ_HTTP_PORT` in .env if that port is occupied. Compose publishes the app on loopback and keeps PostgreSQL on its internal network. The Compose development setup uses HTTP cookies. A named volume preserves records across `docker compose down` and subsequent startup. Use `docker compose logs --tail=50` to diagnose startup.

When demo seeding is enabled, use the fictional accounts `alex`, `sam`, or `taylor` with the shared demonstration password `MatrizDemo!2026`. Alex manages Website launch, Sam is a member, and Taylor manages a separate project. Seeding runs once per database. Keep demo seeding disabled on any shared deployment.

## Run directly with Java

Install JDK 21 and PostgreSQL. Create a PostgreSQL login named `matriz` and databases named `matriz` and `matriz_test` owned by that role. Configure the variables shown in [.env.example](.env.example) in your shell, or put Spring properties in the ignored `.local/application.properties`. Maven Wrapper pins Maven. The application does not automatically load the Compose .env file.

```powershell
$env:MATRIZ_DB_URL = 'jdbc:postgresql://localhost:5432/matriz'
$env:MATRIZ_DB_USER = 'matriz'
# Set MATRIZ_DB_PASSWORD securely in this shell before running.
$env:MATRIZ_DEMO_ENABLED = 'true'
$env:MATRIZ_SECURE_COOKIE = 'false'
$env:MATRIZ_PORT = '8081'
.\mvnw.cmd spring-boot:run
```

The HTTP cookie setting above is for this local loopback setup. The application defaults to secure cookies.

## Tests

Run tests against the dedicated PostgreSQL test database. `MATRIZ_TEST_DB_URL` defaults to `jdbc:postgresql://127.0.0.1:5432/matriz_test`. Use the same username and password settings as above or configure credentials for the test instance. Tests create fictional data in that database.

```powershell
.\mvnw.cmd -B test
.\mvnw.cmd -B test-compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java '-Dexec.mainClass=com.microsoft.playwright.CLI' '-Dexec.classpathScope=test' '-Dexec.args=install chromium'
$env:MATRIZ_E2E = 'true'
.\mvnw.cmd -B verify
```

On Linux, use `sh ./mvnw` and install Chromium with `install --with-deps chromium`. GitHub Actions uses a dedicated PostgreSQL service and builds the container image after tests pass.

## Project documentation

- [Requirements](docs/requirements.md), [user stories](docs/user-stories.md), and [constraints](docs/constraints.md)
- [Architecture](docs/architecture.md), [data model](docs/database.md), and [OpenAPI](openapi.yaml)
- [Assignment 03 plan](docs/implementation-plan.md), [test plan](docs/test-plan.md), [quality and security review](docs/quality-security-review.md), and [delivery verification](docs/verification.md)
- [Changelog](CHANGELOG.md) and [release candidate notes](RELEASE_NOTES.md)
