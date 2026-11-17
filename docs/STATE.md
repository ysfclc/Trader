# Trader Project State

Last updated: 2026-11-17 (product scope correction: general problem solving)

## Repository
- Repository: `ysfclc/Trader`
- Primary branch: `main`
- Project: Trader
- Status: **Phase 1 — first Android problem workspace implementation**
- Android source code: first implementation committed on `build/problem-core-android`.
- APK: CI build pending; no artifact verified yet.
- Device acceptance: not yet performed.
- Live discovery or communication integrations: not yet implemented.

## Completed in this checkpoint
- [x] Created native Android app foundation (Kotlin 2.2.20, AGP 8.13.0, Compose, Room, WorkManager; compile SDK 36, min SDK 26).
- [x] Implemented universal ProblemCase persistence, manual capture, Android share URL/text capture, problem detail, plan-step persistence, status transitions and append-only activity events.
- [x] Added JVM tests for title normalization and status-transition rules.
- [x] Added GitHub Actions Android CI to run unit tests, lint and assemble a debug APK artifact.
- [x] Opened draft PR #1 for implementation review.
- [x] Corrected the product centre of gravity: Trader is a general problem-solving and opportunity-execution system, not a trading-only app.
- [x] Added universal ProblemCase, Goal, Plan/PlanStep and Capability concepts to the locked product, architecture and data model.
- [x] Reframed trade, services and careers as initial specialized workflows, not an exhaustive limit on the problems Trader can accept.
- [x] Initialized the previously empty repository with a first commit.
- [x] Added README and locked product/architecture/data/operating documents.
- [x] Added the phased roadmap, implementation task ledger and state ledger.
- [x] Established industrial tooling and machining between Türkiye and Poland/EU as the first pilot, without hard-coding that category into the product architecture.
- [x] Inspected source files, tests, project status and CI metadata in Outbound Sourcer, VaultOS, BusyBee, Memo-2 and selected EBO files.
- [x] Expanded the audit to BOS, Oddo, Troy, Kovan, Zommo and Nexus; recorded exact source paths, design strengths, implementation gaps and dated CI observations in `docs/ADDITIONAL_REUSE_AUDIT.md`.
- [x] Triaged the remaining repository families without claiming their source code was reviewed.
- [x] Added `docs/REUSE_AUDIT.md` with exact source paths, reusable patterns, known limitations, build-history observations and the no-fork/no-dependency decision.
- [x] Verified the current Trader tree contains the README and all linked baseline documents.

## Expanded audit conclusion
- BOS is a major architecture reference: deterministic business runtime, event/provenance model, capability execution and scoped AI. Its current implementation is not a safe Trader dependency: generalized ingestion is still marked not started, the workflow executor is partial, ADR-038 migration T-385 remains active in the inspected task ledger, and latest observed CI run (2026-09-23) failed.
- Troy is the strongest domain reference for metadata-first qualification, message segmentation, multi-entity capture, source-company clustering and explainable matching. Relevant implementation inspected is under `legacy/oddo-troy/`; July status reports are historical and outreach remained disabled in that snapshot.
- Oddo contributes marketplace/dispatch concepts, but WhatsApp relies on Unipile, Telegram listener is a scaffold and extraction depends on paid Anthropic API.
- Kovan contributes deterministic technical QC principles, not Trader code. Zommo contributes configurable service-business taxonomy, not an automation runtime (its product spec says the executor is absent). Nexus contributes development task/provenance/permission-gate practices, not an end-user runtime.

## Reuse audit conclusion
- Outbound Sourcer: adapt source registry/schema, streaming pipeline, incremental persistence, cancellation and truthful warnings. Do not reuse its business-directory lead score or generic-company schema.
- VaultOS: adapt Compose/Room/local-first and Android import concepts. Do not copy its database, backup code, signing identity or unencrypted backup behavior as-is. Its three latest observed GitHub main-branch Android runs failed; device acceptance remains open in its own ledger.
- BusyBee: adapt raw-evidence, transactional import, deterministic fingerprinting and unknown-value handling. Do not reuse driver-specific storage or claim its notification listener is general app automation. Its latest inspected Android workflow on 2026-11-06 succeeded in CI.
- Memo-2: adapt approval-hash, crash-recovery, bounded-context and redaction concepts with Trader-owned tests. It is not a runtime or source dependency. Its own status says browser/app automation and on-device LiteRT-LM inference are not implemented.
- EBO: retain signal-first and explicit-gate principles only. Its own documents record enforcement gaps; do not port its sequence/executor path.

The audit was targeted to the most relevant projects and files. It was not a line-by-line review of all 43 repositories. No source tests or APK builds were run by this audit.

## Not yet verified in Trader
- [ ] Finish and verify the first ProblemCase → context → plan-step → activity loop; current code is not yet CI- or device-verified.
- [ ] Complete the remaining QRKey/mobile-prototype source audit before selecting any further code for reuse.
- [ ] Confirm the pinned Android/Gradle/Kotlin toolchain with a successful clean CI build; local Gradle wrapper is not yet included.
- [ ] Verify Room persistence and add migration tests plus encrypted backup/restore.
- [ ] Verify share/manual capture, offline retrieval and process-death recovery on Redmi Note 12 Pro 5G.
- [ ] Browser/app automation.
- [ ] AI provider adapters or on-device inference.
- [ ] Buyer/supplier matching.
- [ ] Communication adapters and outreach.
- [ ] Any real buyer/supplier match, reply, deal or revenue.

## Locked constraints
- Trader accepts general user problems and goals; trade, services and careers are initial specialized workflows only.
- Every case can be captured and managed even when no dedicated workflow or automated capability exists.
- Trader must distinguish assistance from actual resolution and never claim success without verification.
- Memo is not a dependency; Trader owns its runtime, workflow, storage and recovery behavior.
- EBO, n8n, Unipile, Apollo, hosted CRM and paid AI are not core dependencies.
- Core capture, storage, qualification, matching and drafting must work without paid API credits.
- Buyer requirements and supplier offers remain distinct domain objects.
- Every external fact has source provenance, capture time and verification state.
- External communication and commercial commitments require explicit user approval.
- Do not bypass authentication, CAPTCHA, paywalls, bot protection or access controls.
- Unknown technical/commercial values are not treated as passing requirements or zero-cost values.
- Preserve every bug, regression and user requirement in the task ledger until verified resolved.

## Next implementation checkpoint
1. Pin a clean Android toolchain for Trader, based on compatible tested versions rather than copying any other app wholesale.
2. Create the first standalone Android foundation and CI workflow with a new application ID and CI-only signing secrets.
3. Implement one vertical slice: share/manual capture → evidence persistence → offline retrieval → process-death recovery.
4. Build and install on Redmi Note 12 Pro 5G before adding large-scale discovery or app automation.

## Change log
### 2026-11-17 — Initial repository lock
Repository initialized and product definition committed. No application implementation claimed.

### 2026-11-17 — Targeted repository reuse audit
Inspected relevant implementation files and test coverage across five existing codebases. Added `docs/REUSE_AUDIT.md`. Decision: Trader is built independently, adapting patterns only and retaining no required dependency on existing apps. Android source and APK still do not exist.

### 2026-11-17 — Expanded audit: BOS and adjacent projects
Added `docs/ADDITIONAL_REUSE_AUDIT.md`. Corrected the initial audit scope: BOS, Oddo and Troy are key references; Kovan, Zommo and Nexus were also inspected. Remaining repositories are explicitly marked as triaged but not source-audited. No code copied; Trader architecture remains independent.


### 2026-11-17 — Product scope correction: not trading-only
The user clarified that the app is intended to help solve the wider set of problems they face, not only trading. The canonical scope is now a personal problem-solving and opportunity-execution system: capture context, understand desired outcomes and constraints, gather evidence, discover options/resources, plan, use authorized tools/automations, verify and retain useful learning. Trade/services/careers remain initial specialized workflows. This is an architecture direction, not a claim that the app can solve every kind of problem or already implements these capabilities.


### 2026-11-17 — First Android implementation checkpoint
Created `build/problem-core-android` and draft PR #1. The first source slice includes a Compose inbox/detail/activity UI, Room-backed ProblemCase/plan-step/activity data, manual and Android share-intent capture, deterministic status transitions, JVM unit tests and an Actions workflow that builds a debug APK. CI is running; no successful build, install or device acceptance is claimed yet. The implementation deliberately does not claim AI, browser automation, installed-app automation, live web discovery or external messaging.
