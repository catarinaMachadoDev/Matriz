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

Prioritize requirements visible to users and permission boundaries. Unit tests isolate title validation. API tests verify saved behavior and rollback fixtures. Browser tests use a random port, a dedicated database, unique project names, and scoped cleanup. Locators use accessible names and assertions wait for rendered results. See [verification results](verification.md).
