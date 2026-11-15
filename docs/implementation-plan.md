# Assignment 03 implementation plan

Status: scope and stack approved on November 15, 2026. Application increments implemented. See verification.md for observed results and remaining delivery steps. Commit messages require separate approval.

## Confirmed decisions

- Modular monolith using Java/Spring Boot, with HTML/CSS/JavaScript served by the same application and JSON REST APIs.
- PostgreSQL as a separately installed database service. No H2 substitution.
- Server-managed cookie sessions and CSRF protection.
- Expand the MVP to project/task CRUD, multiple assignees, comments, priorities, project search, task filters, and a dashboard.
- Retain task titles of 1–120 characters and mandatory descriptions of 1–2000 characters after trimming. Reject blank fields.
- Only project managers edit/delete projects and manage membership. Project members may manage tasks, assignments, and add/list comments.
- Explicit UI confirmation before deletion. Deleting a project removes its dependent tasks, comments, assignments, and memberships atomically. Deleting a task removes its comments and assignments.
- Seed fictional accounts and projects with different memberships without resetting application data on restart.
- Continue English project content. Ask the owner about material product decisions; present each commit message for approval before committing.

## Reconciliation with earlier documents

| Earlier decision | Assignment 03 decision | Artifacts to update |
| --- | --- | --- |
| Technology-neutral | Approved Java/Spring Boot + PostgreSQL + plain web frontend | architecture, README, AGENTS |
| At most one assignee | Zero or more eligible project members; unique task/user pair | requirements, stories, model, API, UI |
| Comments and priority deferred | Included; low/medium/high priority, default medium; authored dated comments | requirements, stories, model, API, UI |
| Deletion deferred | Manager-only project deletion; members may delete tasks; cascade dependent records | requirements, constraints, model, API, UI |
| Only create/list task API | Complete authentication, projects, membership, tasks, assignments, comments and dashboard contracts | OpenAPI |
| Static task wireframe | Responsive login, dashboard, projects and task detail views | UI notes and implementation |
| No account provisioning approach | Explicit opt-in fictional demo seed; no public registration or password recovery | constraints, README |

The worksheet's sample title range 3–150 does not override the owner's approved 1–120 range. Descriptions remain mandatory. Status API values remain todo/in_progress/done, with English labels.

## Increment sequence

| Increment | Components and files | Dependencies | Acceptance and verification |
| --- | --- | --- | --- |
| 0. Reconcile design | requirements, stories, constraints, architecture, database, openapi.yaml | Approved scope | Review access matrix, field limits, many-to-many assignments and deletion behavior; validate API references |
| 1. Bootstrap and persistence | pom.xml, Maven wrapper, application entry point, environment configuration, Flyway migrations, explicit demo seeder | Local PostgreSQL connection | Reproducible schema setup; app starts; second start preserves existing rows; no secrets committed |
| 2. Authentication | identity module, Spring Security, login/profile/logout endpoints, login UI | Users and database | Valid/invalid sign-in; session fixation protection; CSRF checks; logout invalidates session; unauthenticated access denied |
| 3. Projects and membership | projects module and UI, search, pagination, eligible users | Authenticated identity | Create/list/detail/edit/delete; manager enforcement; cross-project 404; persisted project survives reload and backend restart |
| 4. Task management | tasks module, forms/detail, status, priority and filters | Project authorization | Required-field validation; full CRUD; pagination and global counts; unauthorized direct requests rejected; no success UI on failure |
| 5. Collaboration | assignments and comments services/repositories, UI controls | Tasks and membership | Multiple unique eligible assignees; remove assignments; author/date derived by backend; comments survive reload |
| 6. Dashboard and integration | dashboard query, shared frontend components, responsive styles | Project/task/collaboration APIs | Counts from all relevant tasks; refresh affected lists/counts after mutation; loading/empty/error states; preserve failed form input |
| 7. Verification and delivery review | tests, docs/verification.md, README, AGENTS, API alignment | All increments | Automated validation, authorization, create/read persistence and regression tests; manual UI paths; restart persistence; clean-build instructions; reviewed diff |

For each increment: explain the plan, implement the approved scope, run relevant checks, inspect the diff and record real evidence before proceeding. Any material deviation returns to the owner. No invented test or manual-verification results.

## Proposed implementation details for review

- Java 21; select and pin an available supported Spring Boot release when dependencies are resolved.
- Spring Security for session authentication, password hashing and CSRF; avoid custom cryptography.
- Flyway versioned PostgreSQL migrations; application startup does not drop or recreate data.
- Session timeout: proposed 30 minutes of inactivity. Logout invalidates the session. Restart may require sign-in again while business data persists.
- Production cookies use Secure/HttpOnly/SameSite=Lax. For local development, choose local HTTPS or an explicitly documented localhost-only HTTP development exception before testing browser sessions.
- Project descriptions optional up to 1000 characters; project names retain 1–100. Comments required, trimmed, 1–2000. Comment editing/deletion is not in this assignment scope.
- UUID resource identifiers; status and priority enumerations enforced in application and schema.
- Optimistic locking for project/task edits: stale updates receive 409 rather than overwriting another member's changes. Define the version field in the completed API.
- Default pagination 20, maximum 100; deterministic ordering. No automatic retry of writes after an uncertain network outcome.
- Use environment-based PostgreSQL credentials and an ignored local configuration file. Commit only fictional configuration examples. Never reuse or reset an unrelated database.

## Environment findings and prerequisites

Java 21 is installed. Maven is not on PATH; provide a Maven wrapper so users do not need a global Maven installation. PostgreSQL service availability and application database credentials must be established before migration/startup verification. Installing a database service, changing machine configuration, or resetting existing data is not inferred from selecting PostgreSQL.

## Progress and evidence

- [x] Read Assignment 03 and compare with the earlier pack.
- [x] Obtain approval for expanded scope, retained validation, permissions/deletion, stack and demo data.
- [x] Draft incremental implementation plan.
- [x] Approve proposed behavior details and establish PostgreSQL connection.
- [x] Reconcile specification and full API contract.
- [x] Implement increments 1–6.
- [x] Record local verification and delivery limitations.
- [ ] Present commit message and wait for approval.