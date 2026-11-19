# Initial backlog

Status: MVP scope and task rules approved by the product owner on November 15, 2026; remaining details depend on assumptions in [constraints.md](constraints.md). All stories are Must priorities in the proposed MVP. Acceptance criteria specify future tests; they are not tests already executed.

## US-01 — Access (RF-01)

As a user, I want to sign in and sign out so that I can access work under my own identity.

- Given a previously provisioned account, When I provide valid credentials, Then I receive authenticated access to my projects.
- Given invalid credentials, When I try to sign in, Then access is denied with a generic message that does not confirm whether the account exists.
- Given an active session, When I sign out, Then that session no longer authorizes new protected operations.
- Given no valid session, When I request project data, Then no data is returned and no changes are made.

## US-02 — Projects (RF-02)

As a user with project creation permission, I want to create and view my projects so that I can organize separate areas of work.

- Given a valid session and project creation permission, When I create a project with a name of 1 to 100 characters after trimming surrounding whitespace, Then the project is saved and I become its manager and a member.
- Given projects A and B and membership only in A, When I view the list, Then I see A and do not see B.
- Given an empty name, whitespace only, or 101 characters after normalization, When I submit it, Then I receive a validation error and no project is created.
- Given no accessible projects, When I view the list, Then I see an explicit empty state.

## US-03 — Members (RF-03)

As a project manager, I want to add an existing user as a member so that we can collaborate.

- Given I am the manager and the specified account exists, When I add it, Then that account can view and manage tasks in this project.
- Given an account that is already a member, When I add it again, Then only one membership exists and I am informed that the user already belongs to the project.
- Given I am a member without the manager role, When I try to add someone, Then the request is denied and membership remains unchanged.
- Given a nonexistent account, When I try to add it, Then the operation fails without creating an account or membership. Managers select from existing demo accounts.

## US-04 — Task creation (RF-04)

As a member, I want to create a task within a project so that I can record work to be performed.

- Given project access and a valid title of 1 to 120 characters, When I submit the task with a mandatory description of 1 to 2000 characters after trimming surrounding whitespace, Then, with the default selections, it appears in that project as “To do” at Medium priority with no assignees.
- Given confirmed creation, When I reload the application, Then the task remains in the list with the same data.
- Given an empty title, whitespace only, 121 title characters, or a missing, blank, whitespace-only, or 2001-character description, When I submit it, Then no task is created and the entered fields are preserved with an error indication.
- Given a nonexistent or inaccessible project, When I try to create a task, Then the operation is denied without creating data or revealing another project's data.

## US-05 — Assignee (RF-05)

As a member, I want to assign one or more project members so that responsibility for its execution is clear.

- Given a task in my project and a project member, When I assign that member, Then the task displays that assignee alongside existing assignees. Repeating the action leaves one assignment.
- Given an assigned task, When I remove the assignment, Then only that assignee is removed. An empty list displays “Unassigned”.
- Given a user who does not belong to the project, When I try to assign that user, Then the operation is denied and the previous assignment is preserved.

## US-06 — Status (RF-06)

As a member, I want to update a task's status so that I can communicate work progress.

- Given an accessible task, When I choose one of the three defined statuses, Then the new status is saved and displayed on the next query.
- Given a completed task, When I change it to “In progress”, Then it is reopened and no longer counted as completed.
- Given a value outside the allowed statuses, When I submit it, Then the request is rejected without changing the task.
- Given a task in another project I do not belong to, When I directly attempt a status change, Then access is denied and the previous status remains.

## US-07 — Progress (RF-07)

As a member, I want to view project progress so that I can understand pending and completed work.

- Given four tasks, two done, one in progress, and one to do, When I view progress, Then I see a total of 4, respective counts of 2/1/1, and 50.0% completion.
- Given three tasks and one completed task, When I view progress, Then I see 33.3%.
- Given a project with no tasks, When I view progress, Then I see a total of 0 and “No tasks”.
- Given a confirmed status change, When I refresh the progress view, Then the counts reflect the new status. Real-time updates are not required.

## Exception coverage

The criteria cover invalid sessions, cross-project access, blank inputs, length boundaries, nonexistent accounts, duplicate membership, external assignees, unknown statuses, reopening, and division by zero. Stale project or task versions produce a conflict. Retrying a write after a network failure requires checking its outcome first.

## US-08 — Comments (RF-08)

As a project member, I want to add and read task comments so the team can follow the discussion.

- Given a project member and a nonblank comment of up to 2,000 characters, when the member submits it, then the task shows the comment with the authenticated author and server timestamp.
- Given an empty or oversized comment, when the member submits it, then the API rejects it and saves no comment.

## Assignment 03 additions to existing stories

- US-02: Search by project name. Only managers can edit or delete a project. A confirmed deletion removes dependent records.
- US-04: A member can open, edit or delete a task. Deleting a task removes its comments and assignments.
- US-06: A member can choose status and priority at creation and combine status, priority, and assigned-member filters. A task with multiple assignees matches each of them. Project progress continues to reflect all tasks. An edit using a stale version returns a conflict.
