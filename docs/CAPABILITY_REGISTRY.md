# Capability Registry

**Status:** Merged in PR #4; CI runs 129 and 130 passed all 30 unit/Robolectric tests.  
**Purpose:** Make Trader's available operations, permission requirements and side-effect policies explicit. A planned capability is not automatically an executable capability.

## Capability contract

Every registered capability declares:
- stable ID, title and description;
- implementation availability;
- effect category;
- retry policy;
- whether explicit approval is mandatory;
- required permissions or consent;
- whether a visible interaction surface is required;
- offline support and postcondition expectations.

## Policy rules

- **AVAILABLE** means the operation is implemented and may be considered for execution, subject to permission and workflow gates.
- **NOT_IMPLEMENTED** means the user may plan a step, but Trader must not start it.
- **BLOCKED** means policy forbids execution.
- Unknown IDs are treated as unregistered: planning is allowed, execution is denied, approval is required and retry is prohibited.
- External side effects always require explicit approval and use `RetryPolicy.NEVER`.
- `SAFE` retry is permitted by the registry for deterministic/idempotent operations.
- `USER_CONFIRMED` retry is allowed only when the user explicitly marks the step safe.
- `NEVER` retry cannot be overridden by a workflow draft.
- The repository derives mandatory approval and retry-safety metadata from the registry; the caller cannot weaken a capability's policy.
- Starting a step checks that the capability is implemented. A future UI must also show the descriptor, missing permissions, approval requirement and postcondition before the user starts it.

## Current registry

| Capability ID | Availability | Effect | Retry | Approval |
|---|---|---|---|---|
| `manual.task` | Available | User task | User-confirmed | Optional user gate |
| `problem.capture.manual` | Available | Local write | Safe | No |
| `problem.capture.share` | Available | Local write | Safe | No |
| `plan.step.manage` | Available | Local write | Safe | No |
| `activity.read` | Available | Local read | Safe | No |
| `browser.visible.open` | Not implemented | Browser interaction | Safe | No |
| `web.public.search` | Not implemented | Network read | Safe | No |
| `web.page.fetch` | Not implemented | Network read | Safe | No |
| `android.app.handoff` | Not implemented | App handoff | User-confirmed | No |
| `android.accessibility.action` | Not implemented | App interaction | Never | Yes |
| `ai.context.prepare` | Not implemented | Local compute | Safe | No |
| `ai.inference.local` | Not implemented | Local compute | User-confirmed | No |
| `ai.inference.remote` | Not implemented | Network read | User-confirmed | Yes |
| `communication.draft` | Not implemented | Local write | User-confirmed | No |
| `communication.send` | Not implemented | External side effect | Never | Yes |
| `browser.submit` | Not implemented | External side effect | Never | Yes |

## Current enforcement

Workflow creation normalizes each step's retry and approval fields from the registry. Unimplemented and unknown capabilities remain visible as plan steps but cannot transition into RUNNING. External side-effect steps are automatically marked as approval-required and non-retryable.

This prevents a workflow definition from claiming that a browser submit, message send or unregistered action is safe to execute simply because a draft supplied permissive flags.

## Limitations and next gates

- The registry is currently a Kotlin domain catalogue, not yet a user-facing capability browser.
- No browser, app, network, AI or communication executor is implemented by registering its descriptor.
- Permission discovery and runtime permission checks still need concrete Android adapters.
- Each executable adapter must provide typed inputs/outputs, cancellation, timeouts, idempotency, evidence capture and a verifiable postcondition.
- External sends/submits must bind approval to the exact recipient, channel and payload hash immediately before execution.


## Merge and verification record

PR #4 merged at `959eb52f677ccdeae2548b76c096e61a799ff135`. CI runs 129 and 130 passed the full Android pipeline. The capability registry tests verify unique IDs, mandatory approval for external effects, non-retryable external actions, explicit permission declarations and plan-only treatment of unknown/unimplemented capabilities.
