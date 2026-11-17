# Matriz task quality plan

| Scenario | Expected result | Level and test |
| --- | --- | --- |
| Title with surrounding spaces | Valid title is returned without surrounding spaces | JUnit, TaskTitleTest |
| Blank title or 121 characters | Validation rejects input | JUnit, TaskTitleTest |
| Title with 120 characters | Maximum length is accepted | JUnit, TaskTitleTest |
| Valid title and mandatory description | API returns 201, then 200 with the saved task and its initial status and priority | PostgreSQL integration, MatrizIntegrationTest |
| Blank fields or oversized description | API returns 400 and persists no task | PostgreSQL integration, MatrizIntegrationTest |
| Signed-in nonmember requests another project's task | API returns 404 and withholds task data | PostgreSQL integration and local Compose verification |
| Member updates project settings | API rejects manager-only changes while allowing task creation | PostgreSQL integration, MatrizIntegrationTest |
| Sign in, open a project, create a task, return to the list | Task title appears in the rendered project list | Java Playwright, TaskBrowserTest |

## Regression and sanity coverage

| Level | Automated coverage |
| --- | --- |
| Unit | Required and optional text boundaries, trimming, NUL rejection, UUID format, status values, pagination, versions and conflict detection. |
| MVC component | Task controller request binding and defaults, validation and conflict response mapping, and the public task response shape. The service is mocked. |
| PostgreSQL integration | Authentication and CSRF, private project routes, manager-only operations, membership across managers, literal search, pagination, OpenAPI fields, project and task CRUD, reopening, progress, assignments, comments and cascade deletion. |
| Browser regression | Task visibility with active filters and pagination, response ordering during navigation, session expiry and account switching, and task draft preservation. |
| Browser sanity | Readiness, sign-in, create a task, confirm it survives a reload, sign out, and confirm the session can no longer access the API. |

The sanity test uses the same dedicated PostgreSQL database and Chromium setup as the full suite. From the repository root, run it on Windows with:

```powershell
$env:MATRIZ_E2E = 'true'
.\mvnw.cmd -B '-Dgroups=sanity' test
```

Run all levels with `.\mvnw.cmd -B verify` and `MATRIZ_E2E=true`. API tests verify saved behavior and rollback fixtures. Browser tests use a random port, a dedicated database, unique project names, and scoped cleanup. Locators use accessible names and assertions wait for rendered results. See [verification results](verification.md).
