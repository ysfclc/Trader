# Capability Registry

**Status:** Typed execution contract baseline completed for every currently registered capability. Registry availability and safety policy remain fail-closed.  
**Purpose:** Define what an operation accepts, what it returns, what it can change, what it costs, which limits apply and how its outcome is verified. A planned capability is not automatically executable.

## Contract model

Every registered `CapabilityDescriptor` declares:

- Stable ID, title, purpose and availability (`AVAILABLE`, `NOT_IMPLEMENTED` or `BLOCKED`).
- Typed input and output fields, required/optional state, sensitivity flags and cross-field/policy constraints.
- Effect category plus an explicit list of local, device, network or external side effects.
- Retry policy, mandatory approval and required permission/consent identifiers.
- Cost kind and a user-readable disclosure, including whether explicit consent is required.
- A limit mode and optional numeric ceilings for input/output bytes, timeout, requests per minute and concurrent runs.
- A required postcondition, verification method and whether evidence must be retained.
- Offline and visible-surface requirements.

### Field types

| Type | Meaning |
|---|---|
| `IDENTIFIER` | Trader-owned record or capability identifier |
| `STRING` / `TEXT` | Short value / free-form text |
| `URI` | URI or URL string; adapter must still validate allowed schemes and origins |
| `BOOLEAN` | Boolean value |
| `INTEGER` / `DECIMAL` | Numeric value |
| `STRING_LIST` | List of strings |
| `OBJECT` / `OBJECT_LIST` | Structured map / list of structured maps |

Sensitive fields are marked in the schema so future logs, prompts and UI summaries can redact them. The validator checks required fields, primitive/container types and unexpected keys. Cross-field constraints and authorization rules are recorded on the contract but must also be enforced by the operation-specific adapter; generic type validation is not a substitute for policy validation.

### Cost declarations

| Kind | Meaning |
|---|---|
| `NONE` | No direct external charge expected |
| `USER_TIME` | Human effort; Trader does not execute the task |
| `DEVICE_RESOURCES` | Local storage, memory, CPU/GPU or battery |
| `NETWORK_DATA` | May consume mobile or Wi-Fi data |
| `PROVIDER_BILLED` | A selected provider may charge; current price and explicit consent are required |
| `UNKNOWN_UNTIL_ADAPTER_SELECTED` | Cost cannot be estimated until a permitted adapter/provider is selected; do not enable silently |

No provider price or quota is invented in the registry. Provider-billed and unknown-cost operations require explicit consent.

### Limit modes

| Mode | Meaning |
|---|---|
| `USER_CONTROLLED` | A human performs the task; no automated execution limit is claimed |
| `PLATFORM_BOUNDED` | Android, Room/SQLite, device resources or target-app behavior provide the present boundary; no unverified product-specific numeric cap is claimed |
| `ADAPTER_MUST_DECLARE` | The future adapter must declare applicable byte ceilings, timeout, rate limits and concurrency before the capability can become `AVAILABLE` |

The descriptor has numeric fields for `maxInputBytes`, `maxOutputBytes`, `timeoutMillis`, `requestsPerMinute` and `maxConcurrentRuns`. These remain unset where no concrete adapter/provider has been selected. The registry rejects any `AVAILABLE` capability still marked `ADAPTER_MUST_DECLARE`.

### Postcondition verification

- `LOCAL_READ_BACK`: read persisted local state back and compare it with the requested change.
- `STRUCTURAL_VALIDATION`: validate returned structure, provenance and bounds.
- `USER_CONFIRMATION`: an external or target-app outcome cannot be inferred from launch/attempt alone.
- `SOURCE_READ_BACK`: observe the resulting source/page state and preserve its origin.
- `PROVIDER_CONFIRMATION`: require a provider receipt or an explicit uncertain state; uncertain side effects are never retried automatically.

## Registered operation contracts

All field names below are exact schema keys. A trailing `?` means optional.

| Capability ID | State / effect | Inputs → outputs | Cost | Limits | Required postcondition |
|---|---|---|---|---|---|
| `manual.task` | Available · user task | `stepId, title, instructions?, expectedOutcome` → `reportedOutcome, evidenceNote?, userConfirmed` | User time | User controlled | User confirms result; preserve supplied evidence |
| `problem.capture.manual` | Available · local write | `title, originalDescription, desiredOutcome?, constraints?, category?` → `problemCaseId, capturedAt, status` | Device resources | Platform bounded | Read saved ProblemCase back |
| `problem.capture.share` | Available · local write | `sharedText?, sharedUri?, mimeType?, sourcePackage?` → `problemCaseId, originalContent, inputKind, capturedAt` | Device resources | Platform bounded | Read back original content and source metadata |
| `plan.step.manage` | Available · local write | `problemCaseId, operation, stepId?, title?, expectedResult?, position?` → `stepId, persistedState, position` | Device resources | Platform bounded | Read back step, parent and ordering |
| `activity.read` | Available · local read | `problemCaseId?, eventTypes?, after?, limit?` → `events, returnedCount, hasMore` | Device resources | Platform bounded | Validate filters and deterministic order |
| `browser.visible.open` | Not implemented · browser interaction | `url, sessionPurpose, allowedOrigins?` → `sessionId, currentUrl, pageTitle?, sessionState` | Network data | Adapter must declare | Confirm visible current URL/origin |
| `web.public.search` | Not implemented · network read | `query, sourceScope, locale?, maxResults?` → `results, capturedAt, sourceHealth` | Unknown until adapter | Adapter must declare | Every result has source URL/time; failures stay visible |
| `web.page.fetch` | Not implemented · network read | `url, sourcePolicyId, maxBytes?` → `finalUrl, httpStatus, mimeType, rawEvidenceRef, capturedAt` | Unknown until adapter | Adapter must declare | Preserve raw/source metadata; distinguish blocked and failed |
| `android.app.handoff` | Not implemented · app handoff | `targetUri, intentAction, packageName?, handoffPurpose` → `launchState, resolvedPackage?, userReportedOutcome?` | Device resources | Platform bounded | Distinguish launch from user-confirmed completion |
| `android.accessibility.action` | Not implemented · app interaction | `packageName, screenScope, targetDescription, action, approvedPayloadHash` → `observedActionState, resultingScreenEvidenceRef?, userReviewRequired` | Device resources | Adapter must declare | Observe expected UI and preserve evidence; ambiguity requires review |
| `ai.context.prepare` | Not implemented · local compute | `problemCaseId, evidenceIds?, planStepIds?, instructions?, maxContextBytes` → `contextPacketId, content, sourceIds, omittedCount, truncated` | Device resources | Platform bounded | Honor byte ceiling and preserve source references |
| `ai.inference.local` | Not implemented · local compute | `modelId, contextPacketId, prompt, maxOutputTokens` → `text, modelId, inputTokens?, outputTokens?, finishReason` | Device resources | Adapter must declare | Validate bounds; generated claims remain suggestions |
| `ai.inference.remote` | Not implemented · remote data disclosure | `providerId, modelId, contextPacketId, prompt, maxOutputTokens, remoteConsentId` → `text, providerRequestId?, usage?, actualCost?, finishReason` | Provider billed | Adapter must declare | Validate response/usage and record provider + consent |
| `communication.draft` | Not implemented · local write | `recipientReference?, channel, purpose, language, verifiedFactIds, instructions?` → `draftId, content, sourceFactIds, unverifiedClaims` | Device resources | Platform bounded | Read draft back; ground or flag every factual claim |
| `communication.send` | Not implemented · external side effect | `recipientReference, channel, purpose, payload, payloadSha256, approvalRecordId` → `outcomeState, providerMessageId?, sentAt?, receiptEvidenceRef?` | Unknown until adapter | Adapter must declare | Provider confirmation or explicit uncertain state with evidence |
| `browser.submit` | Not implemented · external side effect | `browserSessionId, targetUrl, formValues, payloadSha256, approvalRecordId` → `outcomeState, confirmationText?, responseUrl?, evidenceRef?` | Unknown until adapter | Adapter must declare | Source-observed confirmation or human review |

`ai.inference.remote` is classified as `REMOTE_DATA_DISCLOSURE`, not a harmless read: it sends selected context to a third party, requires approval and per-provider consent, and may incur provider charges. External message/form submissions require approval bound to the exact recipient/destination and payload, prohibit retries, and require provider confirmation.

## Enforcement and validation

- `CapabilityRegistry.validateInputs(id, values)` and `validateOutputs(id, values)` check required fields, value types and unknown keys.
- `assess(id)` allows planning for known and unknown IDs but denies execution for unimplemented, blocked, unknown or permission/consent-incomplete capabilities.
- Retry and approval rules are normalized by the registry; workflow drafts cannot weaken them.
- Every currently executable descriptor has a declared limit mode other than `ADAPTER_MUST_DECLARE`.
- Remote data disclosure requires approval and explicit consent.
- External side effects require approval, explicit cost consent, `RetryPolicy.NEVER` and provider confirmation.

## Explicit limits

- Generic schema validation does not enforce free-text cross-field constraints, host allowlists, site terms, contact basis, consent freshness, target-app scope, payload hash re-checks or provider quotas. The selected adapter must enforce those rules before execution.
- No browser, app interaction, network search/fetch, AI inference, communication or form-submit executor is implemented by this registry.
- `AVAILABLE` only applies to the five current local/manual operations. All adapter-backed operations remain `NOT_IMPLEMENTED`.
- Adapter-specific numerical ceilings must be recorded in the descriptor and covered by tests before changing an adapter-backed capability to `AVAILABLE`.

## Merge and verification record

The typed baseline and registry enforcement originated in PR #4. This increment adds typed input/output schemas, runtime field validation and explicit cost/limit/postcondition declarations for all 16 registered capabilities. CI evidence is recorded in the PR and `docs/COVERAGE.md`.
