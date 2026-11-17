# QRKey Reuse Audit — Trader

**Date:** 2026-11-17  
**Purpose:** Complete the source-level QRKey/mobile-prototype review required before selecting reusable components for Trader.  
**Decision:** Reuse architectural lessons only. Do not copy or add runtime dependencies on QRKey repositories.

## Scope reviewed

The installed QRKey repository family was identified and checked across ten repositories:

| Repository | Reviewed material | Finding |
|---|---|---|
| `ysfclc/qrkey-v2` | README, handoff, decisions, next steps, domain QRKey/ledger code, operations dispatcher | Current product implementation is a Next.js/Postgres consumer hub and business portal. Valuable domain transaction patterns; no Android automation runtime. |
| `ysfclc/qrkey-kiro-main` | Automation guide, workflow router, dispatch worker, task manager, orchestrator, CLI, safety controls, state-check plan/specs | Rich software-development agent orchestration system. Not an end-user mobile automation engine. |
| `ysfclc/qrkey-kiro` | README and repository tree | Earlier v1 business portal and consumer web app. |
| `ysfclc/qrkey` | Recent history and code-search references | Early static consumer/business QR coupon site. |
| `ysfclc/qrkey-app` | Repository tree and recent history | Next.js web application, not a native Android app. |
| `ysfclc/qrkey-fail` | Recent history | Early implementation snapshot. |
| `ysfclc/qrkey-vercel-vp` | Recent history | Earlier Vercel prototype. |
| `ysfclc/qrkey-vercel-vp-mf` | Recent history | UI/UX and QR generator iteration. |
| `ysfclc/qrkey-claude-code-production` | README and recent history | v0-synced UI prototype repository. |
| `ysfclc/qrkey-antigravity` | README and recent history | v0-synced UI prototype repository. |

The QRKey v2 demo documentation explicitly describes the v1 portal reuse and v2 hub/backend boundary. Its v2 hub is a web product, not a native phone automation service.

## Reusable engineering principles

### 1. Idempotency and transactional side effects

QRKey 2's `server/domain/ledger.ts` posts balanced transactions behind a unique idempotency key. `server/domain/qrkeys.ts` combines transaction boundaries, row locking and a unique redemption constraint to prevent duplicate redemption under concurrent scans.

**Trader adaptation:** every future capability execution needs a stable operation/idempotency key, a durable state transition and a postcondition check. Ambiguous external submits must become `NEEDS_REVIEW`; they must not be blindly retried. Use local Room transactions and unique constraints where appropriate, not QRKey's TypeScript/Drizzle implementation.

### 2. Append-only history

QRKey 2 records business events and ledger transactions rather than silently rewriting the history of a redemption or credit movement.

**Trader adaptation:** retain append-only workflow/activity events and source provenance. Current status can be a projection; corrections should add events rather than erase what happened.

### 3. Deterministic workflow routing and bounded context

QRKey's developer automation classifies work deterministically, loads only the relevant workflow guide and retrieves deeper references on demand. The workflow documentation explicitly targets smaller prompts and reduced context overhead.

**Trader adaptation:** build a context assembler that starts with the active ProblemCase, selected evidence and required task schema. Retrieve only relevant files/history; never dump the entire local library into a model prompt. Keep deterministic routing ahead of optional AI classification.

### 4. Pre-flight state checks and approval gates

QRKey's task-178 state-check protocol checks whether a stage is already complete and whether approval is pending before spawning another agent. The pipeline also separates planning, implementation, audit and closure.

**Trader adaptation:** before resuming a workflow, inspect its durable state and avoid duplicate execution. Require explicit approval for exact external actions. Keep approval state separate from execution state.

### 5. Safety, resource and recovery controls

QRKey's automation system contains process locks, concurrency limits, timeouts, retries, cost limits and failure logs.

**Trader adaptation:** implement bounded concurrency, cancellation, per-capability limits, visible failure records and recoverable jobs. These controls must be native to Trader's workflow engine and Android lifecycle.

## Explicit non-reuse decisions

- Do not import QRKey's Node.js detached-process dispatcher, task-manager, agent personas, file-based pipeline, CLI, Streamlit dashboard or Claude CLI invocation into Trader.
- Do not copy the QRKey v2 Next.js/Drizzle/Postgres domain modules into the Android application.
- Do not make Trader depend on QRKey's Vercel app, database, webhook contract, n8n/Make history or authentication model.
- Do not treat Playwright MCP browser QA or QRKey's web scanner as proof of installed-app automation.
- No reviewed QRKey repository provides a verified, reusable Android browser/app automation runtime for Trader.

## Consequence for Trader architecture

Trader remains a native Android application with its own Room-backed data, capability contracts, workflow lifecycle, visible browser adapter, user-granted app handoff and optional bounded AccessibilityService. The next implementation should use the reusable principles above while preserving Trader's own state model, tests and safety gates.

## Audit limits

This was a targeted source-level audit of the identified QRKey family and the most relevant automation/domain files, not a line-by-line review of every file in all ten repositories. No QRKey code was copied. No QRKey repository was modified.
