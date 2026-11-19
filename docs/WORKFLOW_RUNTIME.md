# Trader Workflow Runtime

**Status:** Room v2 workflow persistence (PR #3), capability registry enforcement (PR #4) and the first user-facing workflow/capability surface (PR #6) are merged; CI passed. Physical-device acceptance remains pending.  
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

## Process interruption recovery

On ViewModel startup, Trader reconciles persisted runs still marked RUNNING. If one or more steps were RUNNING, those steps and the run move to NEEDS_REVIEW, the previous step approval is cleared, and append-only events explain that the operator must determine whether the action completed. If no step was RUNNING, the run moves to PAUSED so the operator can inspect and resume it.

The reconciliation runs in one Room transaction and is idempotent: a second startup does not append duplicate recovery events. It is a conservative recovery boundary, not a WorkManager executor; no capability is automatically resumed or executed.

## Explicit limitations

- No browser, installed-app, AI, communication or network capability is executed by this layer.
- No WorkManager execution queue or automatic process-death resumption has been wired yet. Startup reconciliation only pauses or flags interrupted work for human review.
- Retry and approval policies are now normalized against the Kotlin capability registry. Unimplemented and unknown capabilities may be planned but cannot transition to RUNNING.
- A first UI surface now creates a workflow from saved plan steps, displays persisted runs/steps/events, exposes exact-step approval, supports manual start/complete and explicit skip of unavailable steps, pause/resume/cancel, and requires a non-empty user evidence note before outcome verification. This is a manually driven workflow surface, not an autonomous executor.

## Next implementation gates

1. Verify the workflow UI on a physical Android device, including create, approve, start, complete, skip, pause/resume, cancel and outcome verification.
2. Add durable WorkManager execution with unique work IDs, cancellation, process-death recovery and postcondition checks.
3. Add visible browser and bounded Android app adapters only after their capability contracts and permission gates are in place.
