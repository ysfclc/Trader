# Trader Workflow Runtime

**Status:** Implementation added; CI verification pending.  
**Scope:** Durable workflow planning and lifecycle tracking. This is not yet a capability executor.

## Data model

- `workflow_runs`: a workflow instance linked to one ProblemCase, with a unique idempotency key, lifecycle status and timestamps.
- `workflow_steps`: ordered steps with a capability ID, bounded input payload, expected postcondition, retry-safety declaration, approval requirement and attempt metadata.
- `workflow_events`: append-only event history for creation, approvals, input changes, state transitions, failure/review and cancellation.

Deleting a parent ProblemCase cascades to its workflow runs; deleting a workflow run cascades to its steps and events. Event records have insert/read DAO methods only; there are no update/delete APIs.

## Lifecycle rules

Run states:

`DRAFT → READY → RUNNING → PAUSED / WAITING_FOR_APPROVAL / FAILED / NEEDS_REVIEW / AWAITING_VERIFICATION → VERIFIED`

Cancellation is allowed from non-terminal states. A failed run can return to READY only with explicit retry authorization. A review-required run can return to READY only after explicit review resolution. A run cannot become VERIFIED without an explicit verification decision and a non-empty evidence note.

Step states include `PENDING`, `RUNNING`, `WAITING_FOR_APPROVAL`, `COMPLETED`, `FAILED`, `NEEDS_REVIEW`, `SKIPPED` and `CANCELLED`.

A run moves to AWAITING_VERIFICATION only when every step is COMPLETED or SKIPPED. Completing a step is not proof that the ProblemCase outcome was achieved.

## Idempotency and approvals

- Workflow creation accepts a caller-supplied idempotency key and stores it under a unique index. Repeating the same key and same workflow definition returns the existing run. Reusing the key for a different definition is rejected.
- Every step input is hashed together with its capability ID using SHA-256.
- An approval stores the exact current payload hash. Editing the input clears the previous approval.
- An approval-required step cannot start unless the approved hash matches the current payload hash.
- Failure or ambiguous completion clears the approval for another attempt. Ambiguous external side effects should be recorded as NEEDS_REVIEW, not retried blindly.

The approval hash is a data-layer safety gate. There is not yet a UI that displays and confirms every recipient/channel/payload, nor an external send executor. Those must exist before any consequential capability can run.

## Migration and tests

Room database version 2 adds the three workflow tables and indexes through `MIGRATION_1_2`. Existing ProblemCase, plan-step and activity-event rows are retained.

Robolectric tests cover:
- idempotent workflow creation and event recording;
- step progression and separate outcome verification;
- exact-payload approval and approval invalidation after input edits;
- review-required ambiguous outcomes and retry restrictions;
- safe retry authorization;
- cancellation of non-terminal steps;
- version 1 to version 2 migration with existing row preservation.

## Explicit limitations

- No browser, installed-app, AI, communication or network capability is executed by this layer.
- No WorkManager queue or process-death resumption has been wired yet.
- Retry safety and capability IDs are currently workflow-definition metadata. A capability registry must validate them before an executor is allowed to run a step.
- UI surfaces for workflow creation, approval review, pause/resume and evidence verification are still pending.

## Next implementation gates

1. Run unit tests, lint, APK assembly and Room migration/persistence tests in CI.
2. Build the capability registry and validate step definitions against declared permission, side-effect, retry and approval policies.
3. Add workflow UI to create a run from a ProblemCase plan and inspect its complete event history.
4. Add durable WorkManager execution with unique work IDs, cancellation, process-death recovery and postcondition checks.
5. Only then add visible browser and bounded Android app adapters.
