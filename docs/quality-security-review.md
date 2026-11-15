# Quality and security review

Review scope covers task validation, project authorization, error behavior, and the latest Java increment. Requirements and the Spring Boot controllers are the basis for review.

| Area | Evidence and conclusion | Decision |
| --- | --- | --- |
| Title and description | The task service validates normalized lengths before insertion. JUnit covers the title boundary and integration tests cover invalid input and absence of persisted rows. | Preserve the approved 1–120 title and mandatory 1–2000 description. |
| Project isolation | ProjectService checks membership before returning project data. TaskService also scopes task lookup to the project. MatrizIntegrationTest checks denied read/write access, while the Compose scenario uses separate real demo sessions. | Keep these checks as regression coverage. See verification for execution evidence. |
| Manager-only actions | ProjectService checks manager identity. Integration tests distinguish member task permissions from project management. | Retain the role distinction. |
| Stale edits | Updates use the record's version and return 409 after a competing edit. | Preserve the conflict outcome. |
| Credentials | Local configuration and environment files are ignored and excluded from the build context. The CI password applies only to its isolated database fixture. | Keep real credentials out of the repository and image. |
| Static analysis | No dedicated static-analysis workflow was present in the starting project. | Rely on compile, tests and focused review for this assignment. |

The browser test checks the user-visible task list after creation, in addition to the task detail page. Database assertions and permission tests remain separate so failures can be attributed to a specific boundary. See [execution evidence](verification.md).
