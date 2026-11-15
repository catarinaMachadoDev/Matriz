# Matriz

Proposed application for organizing projects, managing tasks, and tracking team progress.

## Current status

Specification Pack for Practical Laboratory Assignment 01, dated November 15, 2026. This deliverable contains analysis and documentation; the application has not been implemented. The product owner has approved the MVP scope, shared task editing, modular monolith, technology-neutral design, and the task rules with mandatory descriptions. Final Architecture Pack review is pending. The initial client request is the only source of confirmed needs; detailed MVP requirements are proposals with explicit assumptions.

The Git repository is located at `C:/Users/catar/Desktop/Matriz`. The existing Maven/Java scaffold is preserved in this same root directory. Its presence does not constitute an approved technology decision for this product.

## Objectives and proposed MVP

Centralize work and make progress visible to authorized members. The proposed MVP includes authenticated access, project creation and viewing, project membership management, task creation, assignee management, status updates, and progress tracking. Details and priorities are documented below.

## Documentation

| File | Contents |
| --- | --- |
| [Requirements](docs/requirements.md) | Facts, users, scope, requirements, and priorities |
| [User stories](docs/user-stories.md) | Backlog, Given/When/Then criteria, and exceptions |
| [Constraints](docs/constraints.md) | Assumptions, questions, pending decisions, and critical review |
| [Agent instructions](AGENTS.md) | Project working rules |

## Architecture Pack

- [Architecture and review](docs/architecture.md)
- [Logical data model and Mermaid ER diagram](docs/database.md)
- [OpenAPI 3.1 task contract](openapi.yaml)
- [Project task wireframe](docs/ui/README.md)

These are design artifacts for Assignment 02, not an application implementation.

## Next phase

Validate scope and assumptions with the product owner before treating them as approved requirements. Then select architecture, technologies, data model, API contracts, and prototypes based on this specification. Application installation, execution, and test commands have not yet been defined.
