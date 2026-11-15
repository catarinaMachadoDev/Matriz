# Matriz requirements

## Source and status

Source: Practical Laboratory Assignment 01, project context. Fact F-01: the client wants teams to organize projects, manage tasks, and track progress. The assignment confirms that detailed features, rules, technologies, and priorities have not yet been defined. The MVP scope, shared task editing, and task rules were approved by the product owner on November 15, 2026; description is mandatory. Remaining details are supported by assumptions P-01 through P-09 in [constraints.md](constraints.md). Existing requirement IDs are retained for traceability.

## Vision and objectives

Provide a shared workspace that answers three questions: what work exists, who is responsible, and what its status is. Proposed usefulness criterion: an authorized member can create a task, assign it to a project member, change its status, and observe the updated progress.

## Proposed users

| Role | Responsibilities |
| --- | --- |
| Authenticated user | Create a project and view projects they belong to |
| Project manager | Project creator; manages members and performs member operations |
| Project member | View and create tasks, assign people, and update statuses within the project |

Roles apply per project: managing one project does not grant access to another. Initial accounts are provisioned through a process still to be defined; public registration and global administration are outside this MVP (P-01).

## Proposed functional requirements

All features below are Must priorities for the proposed MVP. The product owner approved these seven MVP features on November 15, 2026.

| ID | Feature | Rationale | Story | Assumption |
| --- | --- | --- | --- | --- |
| RF-01 | Sign in and sign out | Identify users and protect work | US-01 | P-01, P-02 |
| RF-02 | Create projects and list only accessible projects | Organize and locate work | US-02 | P-02, P-03 |
| RF-03 | Add existing accounts as members | Enable controlled collaboration | US-03 | P-02 |
| RF-04 | Create and list project tasks | Record work to be performed | US-04 | P-03, P-04 |
| RF-05 | Assign or unassign a task owner | Make responsibility explicit | US-05 | P-05 |
| RF-06 | Change task status | Track execution | US-06 | P-04 |
| RF-07 | Display project totals and completion percentage | Support objective progress tracking | US-07 | P-06 |

## Proposed business rules

- Each project has one manager and zero or more additional members. The manager is also a member. Only the manager adds members; ordinary members cannot change permissions.
- Each task belongs to exactly one project and requires a title of 1 to 120 characters after trimming leading and trailing whitespace. Its description is mandatory, containing 1 to 2000 characters after trimming leading and trailing whitespace. A project name contains 1 to 100 characters after the same normalization. Names and titles do not need to be unique.
- Tasks start in “To do”. Allowed statuses are “To do”, “In progress”, and “Done”. Any member can perform any transition between these statuses, including reopening a task.
- The assignee is optional and, when present, must belong to the same project. Adding an existing member again does not duplicate membership.
- Progress = completed tasks / total tasks × 100, rounded to one decimal place. With no tasks, display zero tasks and “No tasks”, without division by zero.
- Invalid or unauthorized requests do not change data. Confirmed data remains available after reloading the application.

## Proposed non-functional requirements

| ID | Requirement and verification | Basis |
| --- | --- | --- |
| RNF-01 | Check identity and project access on every protected operation, including direct requests; test users without a session and members of other projects | P-01, P-02 |
| RNF-02 | Do not expose credentials, sessions, or project data in public messages or logs; review configuration and sample logs before delivery | P-07 |
| RNF-03 | When validation fails, explain the reason to the authorized user, preserve entered fields, and do not save partial data; verify empty and excessively long titles | P-03, P-08 |
| RNF-04 | Support keyboard operation of primary flows, visible focus, and labelled fields; perform manual verification before delivery | P-08 |
| RNF-05 | After confirmed creation or updates, a new query returns the same data; verify again after starting a new session | P-09 |

Latency, availability, capacity, and recovery targets still require agreed values and measurement conditions. They are not approved commitments.

## Outside the MVP

| Future feature | Proposed priority | Reason for deferral |
| --- | --- | --- |
| Comments and attachments | Should later | Useful for collaboration but unnecessary to validate the core flow |
| Due dates, task priorities, and advanced filters | Should later | Require clarification of the working process |
| Notifications and external integrations | Could later | Add dependencies and operational effort |
| Advanced reporting and change history | Could later | The MVP provides current progress only |
| Public registration, account recovery, and global administration | To assess | Identity processes remain pending |
| Deletion, archiving, member removal, and manager transfer | To assess | Require retention rules and treatment of assigned tasks |
| Multiple organizations, native mobile apps, and AI automation | Outside initial scope | Not established by the initial confirmed need |

AI supports development; no AI features within the product have been confirmed.
