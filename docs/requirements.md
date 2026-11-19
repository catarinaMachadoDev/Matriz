# Matriz requirements

Matriz gives teams an English workspace for organizing projects and tracking tasks. Work from Assignments 01–02 was expanded with the product owner's approval for Assignment 03.

| ID | Requirement |
| --- | --- |
| RF-01 | Sign in and out and view the authenticated account. |
| RF-02 | Create projects as a manager, search accessible projects by name, and view project details. Managers can edit and delete their own projects. |
| RF-03 | Managers can add eligible existing accounts to their project. Membership is unique. |
| RF-04 | Members can create, list, view, edit, and delete tasks in their own projects. |
| RF-05 | Members can add or remove multiple eligible task assignees. Each assignment is unique and requires project membership. |
| RF-06 | Members can update task status and priority and filter by status, priority, or assigned member in the list and Kanban board. |
| RF-07 | The dashboard shows accessible projects and counts across their tasks. |
| RF-08 | Members can add and view task comments. The server sets each author and timestamp. |

Project names are trimmed and must contain 1–100 characters. Project descriptions are optional and capped at 1,000 characters. Task titles and descriptions are mandatory and contain 1–120 and 1–2,000 characters respectively after trimming. Comments contain 1–2,000 characters after trimming. Members select the initial status and priority at creation. The defaults are To do and Medium. Statuses are To do, In progress, and Done. Priorities are Low, Medium, and High. Members can reopen tasks. List pages default to 20 records with a maximum of 100. A project without tasks shows an empty state. Completion is the share of done tasks rounded to one decimal place.

Verify authentication, project authorization, validation, persistence, keyboard use, and error states. Unauthorized project access must withhold resource data. The UI asks for confirmation before deletion. Project deletion removes dependent records, and stale edits return a conflict. Confirmed data persists across backend restarts. See [user stories](user-stories.md) and [verification](verification.md).

Public registration, password recovery, advanced reporting, external integrations, and member removal remain outside the lab scope.
