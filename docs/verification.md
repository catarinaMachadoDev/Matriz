# Matriz verification

Verified locally on November 15, 2026 with Java 21, Maven Wrapper 3.9.16, PostgreSQL 17, Chromium through Playwright for Java, and Docker Compose v2.

| Check | Observed result |
| --- | --- |
| Java unit tests | 3 passed. Valid title normalization, blank title rejection, and the 120/121 boundary behaved as expected. |
| PostgreSQL API integration | 9 passed. Covered authentication, CSRF, logout, valid task creation and retrieval, validation, project isolation, roles, version conflicts, assignments, comments, cascading deletion, filters, and counts. |
| Chromium end to end | 1 passed. Alex signed in, opened a project, created a task, and returned to the project list where the new task was visible. The test used a random application port and cleaned up its uniquely named project. |
| Application packaging | A clean temporary copy produced the executable Spring Boot JAR using the Maven Wrapper. |
| Docker configuration and build | Compose configuration validated and both containers started healthy. Flyway connected to PostgreSQL and validated the migration. |
| Authorization with real sessions | Alex created a task through the Compose API. Taylor signed in to a separate session and received 404 on both read and update. The task retained its original data. |
| Persistence | After both containers were recreated, Alex signed in again and read back the same task from the PostgreSQL volume. The disposable Compose project was then removed. |
| Local delivery review | OpenAPI parsed with 21 operations, required path parameters, and 122 resolving references. Environment and local credential files are ignored and excluded from the container context. |
| GitHub Actions | Workflow configured for push and pull requests. Hosted run [43774443610](https://github.com/catarinaMachadoDev/Matriz/actions/runs/43774443610) passed all 13 tests and built the image for commit 92ddfdf. CI now also defines a database-backed container smoke test. |

## Reproduce

Follow the Java and Docker prerequisites in the [README](../README.md). Use the dedicated PostgreSQL test database, install Chromium, set MATRIZ_E2E=true, and run Maven verify with the wrapper. Against a disposable Compose project, sign in as Alex and create a task in Website launch. Use Taylor's independent session to check that the resource is unavailable. Recreate the containers while keeping the volume, then sign in as Alex and confirm the task remains. Check container health and recent logs as described in the README.

## Problems resolved

- The first browser run selected both task and project description fields. The test now requires the exact label of the task form field.
- The initial Linux build selected a Maven tarball while the wrapper pinned the ZIP checksum. Installing unzip in the builder made the wrapper download and verify the ZIP.
- The running Windows application held the target JAR open. The test phase had passed all 13 cases, and packaging succeeded from a fresh directory. If this occurs locally, stop the instance running from that JAR before rebuilding in the same directory.

## AI use and human decisions

The owner chose Java/Spring Boot with PostgreSQL and approved the expanded MVP and existing validation limits. AI translated those decisions into scoped tests, including the title boundary. The proposed browser locator was corrected after Chromium found two matching fields. AI prepared a single application container and a PostgreSQL service, and Compose health, access, and persistence checks confirmed the configuration locally.

See the [quality and security review](quality-security-review.md). The hosted run above records the verified baseline. Subsequent changes need their own run before delivery.
