# Project tasks wireframe

[Open the wireframe](wireframe.png)

Low-fidelity, static desktop wireframe for the approved project task screen. Fictional data: four tasks, two done, one in progress, one to do; overall progress 50.0%. English interface. No functional application code is included.

## Traceability

- Project navigation: RF-02; membership navigation (manager only): RF-03.
- Task list and mandatory title/description form: RF-04; selected create/list API operations.
- Assignee and status controls: RF-05 and RF-06; their API contracts are future work.
- Progress counts: RF-07, across the full project; not derived from a single list page.
- Sign out: RF-01; its API is future work.

## Interaction states

Loading: show a task-list loading indicator. Empty list: show “No tasks yet”. Form submission: disable repeated submission and show “Creating…”. Validation: show field-specific errors and preserve entered values. Both title and description are required; reject whitespace-only values after trimming. Success: clear the form only after confirmed creation, then refresh task data. Network/server failure: preserve the form and show a retry message; do not automatically resend an uncertain create operation. Session expiry: request sign-in. Inaccessible project: generic not-found view without project details.

Keyboard focus follows navigation, task controls, pagination, then the creation form. Inputs have visible labels and required indicators; error text must be associated with the field. Status is conveyed in text, not color alone. Mobile layout, detailed accessibility review, and user evaluation remain pending. This image illustrates one state; it does not demonstrate functioning controls.
