# Workflow UI

**Status:** Implemented and CI-verified on `build/workflow-ui` at `ecfb6a4f9c7e4e00ddbb29825b2a6c46319e1bde`; physical-device acceptance remains open.

## Entry points

- A dedicated **Workflows** tab lists all persisted runs and links them to their parent ProblemCase.
- Each ProblemCase detail page shows its workflow runs and offers **Create workflow from plan** when plan steps exist and there is no active run.
- A workflow detail screen displays its current state, parent problem, ordered steps and append-only event history.

## User controls

- Prepare a draft workflow and start it when at least one step has an implemented capability.
- Pause, resume and cancel a running workflow.
- Start and manually complete available steps.
- Approve a pending step only after its exact stored input payload is displayed.
- Approve all waiting steps before resuming a workflow paused at an approval gate.
- Explicitly skip an unavailable step in a mixed workflow; it is recorded as SKIPPED, never as completed.
- Authorize retry only when every failed step is marked retry-safe.
- Resolve a NEEDS_REVIEW state with a written review note.
- Verify the overall outcome with a written evidence note.

## Truthful execution boundary

The current executable workflow step is `manual.task`: Trader tracks work that the user performs. It does not run a browser, installed app, AI model, network search or communication action.

Unimplemented capabilities remain visible as plan-only steps and cannot start. In a mixed workflow, the user may explicitly skip an unavailable step. A run with no executable work cannot start.

A step marked COMPLETED means the user confirmed that step; it does not establish that the overall problem was solved. The run enters AWAITING_VERIFICATION only after all steps are completed or explicitly skipped, and becomes VERIFIED only after a separate evidence note.

## Remaining gates

- CI build and tests for this UI branch.
- Physical-device visual and interaction acceptance on Redmi Note 12 Pro 5G.
- Capability browser and permission inspection screen.
- Durable WorkManager executor and process-death recovery.
- Visible browser and bounded installed-app adapters.
