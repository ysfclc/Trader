# Trader Project State

Last updated: 2026-11-19 (PR #6 merged; CI passed; device acceptance pending)

## Repository
- Repository: `ysfclc/Trader`
- Primary branch: `main`
- Project: Trader
- Status: **Phase 2 — durable workflow foundation and first user-facing workflow control surface merged and CI-verified**
- Android source code: first implementation committed on `build/problem-core-android`.
- APK: debug artifact built, downloaded and inspected; device install/launch not yet verified.
- Device acceptance: not yet performed.
- Live discovery or communication integrations: not yet implemented.

## Completed in this checkpoint
- [x] Created native Android app foundation (Kotlin 2.2.20, AGP 8.13.0, Gradle 8.13 wrapper, Compose, Room, WorkManager; compile SDK 36, min SDK 26).
- [x] Implemented universal ProblemCase persistence, manual capture, Android share URL/text capture, problem detail, plan-step persistence, status transitions and append-only activity events.
- [x] Added JVM tests for title normalization and status-transition rules.
- [x] Added system-locale English, Turkish and Polish UI strings and localized activity labels.
- [x] Added pinned Gradle 8.13 wrapper and checksum, plus `docs/ANDROID_BUILD.md` with local build and debug/release signing boundaries.
- [x] Added GitHub Actions Android CI to run unit tests, lint and assemble a debug APK artifact.
- [x] Clean CI run passed: unit tests, lint, debug APK assembly, APK signature verification and package identity assertion; artifact uploaded successfully.
- [x] Downloaded the latest APK artifact, verified the package archive and recorded its SHA-256 below.
- [x] CI ran apksigner verification and asserted the exact debug package ID/version before upload.
- [x] Localized the complete first-slice UI, category labels, statuses and activity event labels for English, Turkish and Polish.
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
- [x] Completed the source-level QRKey repository-family audit; reusable principles and explicit non-reuse boundaries are recorded in `docs/QRKEY_REUSE_AUDIT.md`.
- [x] Merged PR #2 (`3d258d8845732015d2c6f63dfcd139d6629f196f`): deterministic workflow lifecycle rules and QRKey audit; CI run 93 passed.
- [x] Merged PR #3 (`708a3d4e64bed1cca81075de3622a55bb9a9d745`): Room v2 workflow persistence, append-only events, exact-payload approval binding, retry/cancellation rules and migration tests; CI runs 111 and 112 passed.
- [x] Merged PR #4 (`959eb52f677ccdeae2548b76c096e61a799ff135`): capability registry, registry-derived approval/retry policy and fail-closed workflow start checks; CI runs 129 and 130 passed all 30 tests.
- [x] Added and verified a typed capability registry; only implemented local/manual capabilities are executable. Browser, installed-app, AI and communication capabilities remain explicitly unavailable.

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
- [ ] Install and exercise the first ProblemCase → context → plan-step → activity loop on a physical Android device; CI build and unit tests now pass, but device behavior remains unverified.
- [x] Complete the QRKey/mobile-prototype source audit; no QRKey repository contains a suitable native Android browser/app automation runtime for reuse.
- [x] Confirm the pinned Android/Gradle/Kotlin toolchain with a successful clean CI build using the committed wrapper.
- [ ] Verify pause/resume and process-death recovery for persisted workflows; idempotent creation, cancellation and lifecycle gates have repository tests.
- [x] Verify Room v1→v2 workflow migration and durable workflow repository tests; CI runs 111 and 112 passed.
- [ ] Add encrypted local backup/export and integrity-verified restore.
- [ ] Verify share/manual capture, offline retrieval and process-death recovery on Redmi Note 12 Pro 5G.
- [ ] Durable WorkManager execution and process-death recovery. Workflow UI and capability browser are merged and CI-verified; physical-device acceptance remains pending.
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

## Verified build record
- Implementation PR: [#1](https://github.com/ysfclc/Trader/pull/1), merged to `main`
- Merge commit: `0a6dd7ca3fe4ec072058523cae07af22f940424a`
- Source commit used for the APK: `08165377c0ba58f71bf757acf5864c06d75de8cd` (same application source included in the merge)
- Successful push CI run: `44102991766` (run 74)
- Successful pull-request CI run: `44103004289` (run 75)
- Successful workflow lifecycle branch CI run: `44106902964` (run 93)
- Successful capability registry push CI run: `44111705169` (run 129)
- Successful capability registry pull-request CI run: `44111721765` (run 130)
- Push run 74 artifact ID: `14336663733` (`trader-debug-apk`, expires 2026-12-01).
- Pull-request run 75 artifact ID: `14337286960` (`trader-debug-apk`, expires 2026-12-01); GitHub artifact digest `c81fade1c3152f5c81478185b48795f8f030112bca2bb2ea7cb4bcdc3536b50a`.
- Workflow lifecycle run 93 artifact ID: `14339431799` (`trader-debug-apk`, expires 2026-12-01); GitHub artifact digest `b01508aad9612c60ee7004dce1b2683dab93583f0d067fa8cb581539742915ee`.
- Capability registry run 129 artifact ID: `14341082290` (`trader-debug-apk`, expires 2026-12-01); GitHub artifact digest `d1b1b93393c8a9273c70d5d7955c5b800e22e5f56ac37e64fe0745a598f3cd92`.
- APK: `app-debug.apk`, 18,820,225 bytes
- APK SHA-256: `eebe0dd45470dd83d692402f43cb25c6e25c03287a10aca08653db19063ce3b9`
- Package ID: `com.yusufculcu.trader.debug`; version `0.1.0-debug`
- CI tasks passed: `testDebugUnitTest`, `assembleDebug`, `lintDebug`, `apksigner verify` and exact `aapt` package/version assertion.
- Device installation, launch, share-intent behavior and process-death persistence are not yet verified.

## Next implementation checkpoint
1. Install and exercise this APK on the Redmi Note 12 Pro 5G; verify create/share/reopen/plan/status flows and process-death persistence.
2. Fix issues found on-device before extending capture to files/screenshots and adding the capability registry.
3. Implement durable workflow state, pause/resume/cancellation and evidence records.
4. Add browser session and bounded app handoff as independently testable capabilities; only then layer optional AI/prompt-context adapters.

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


### 2026-11-17 — Build pipeline hardening
The first wrapper-based CI attempt exposed that the initially selected wrapper JAR was not executable. Replaced it with the wrapper JAR shipped by the Android Now in Android sample, corrected the distribution URL escaping, pinned the official Gradle 8.13 distribution SHA-256 and switched CI to `./gradlew`. The corrected wrapper is awaiting CI verification; do not treat the build as green until the workflow completes successfully.


### 2026-11-17 — First Android CI build verified
The first local-first Android vertical slice compiled, passed JVM unit tests and lint, and produced a downloadable debug APK through GitHub Actions. The artifact was downloaded and inspected; SHA-256, source commit and CI run IDs are recorded above. A lint failure in the initial attempt was corrected by placing the API 27 navigation-bar theme attribute in `values-v27`. The physical-device acceptance gate remains open.


### 2026-11-17 — APK signature and package identity verified
Added explicit CI gates using apksigner and aapt. Both push run 74 and pull-request run 75 passed, including unit tests, lint, APK build, signature verification and the exact debug application ID/version assertion. Downloaded the latest artifact and updated its SHA-256 record above. Physical-device acceptance remains open.


### 2026-11-17 — First Android foundation merged
PR #1 was marked ready and squash-merged to `main` at `0a6dd7ca3fe4ec072058523cae07af22f940424a`. The feature branch's push and pull-request workflows passed, including unit tests, lint, APK assembly, signature verification and package identity checks. The post-merge main-branch run 81 was cancelled; the successful PR run remains the verified build evidence. A new branch CI run is required after workflow changes. Device acceptance remains open.


### 2026-11-17 — QRKey reuse audit and workflow lifecycle foundation

Completed a targeted source-level audit of all ten identified QRKey repositories. QRKey 2 contributes useful idempotency, transactional side-effect, append-only event and evidence patterns. QRKey's developer automation contributes deterministic workflow routing, bounded context loading, pre-flight state checks, approval gates, timeouts, locking and recovery lessons. None provides a reusable native Android browser/app automation runtime. No QRKey code or runtime dependency is being copied.

Added a pure Kotlin workflow run/step state machine and JVM tests on `build/workflow-lifecycle-and-qrkey-audit`. The state contract distinguishes approval, safe retry, manual review and outcome verification. GitHub Actions run 93 passed the full Android CI pipeline. At that checkpoint workflow runs were not yet persisted; the next checkpoint adds Room persistence, with migration and repository tests pending CI. Capabilities are not yet executed, and no workflow completion is claimed.


### 2026-11-17 — Workflow lifecycle CI verified

GitHub Actions run 93 (`44106902964`) passed the complete Android pipeline on `build/workflow-lifecycle-and-qrkey-audit`: JVM unit tests (including the new workflow rules), lint, debug APK assembly, APK signature verification and package identity assertion. Artifact `14339431799` was uploaded (GitHub digest `b01508aad9612c60ee7004dce1b2683dab93583f0d067fa8cb581539742915ee`). This verifies compilation and CI tests, not physical-device behavior or durable workflow execution.


### 2026-11-17 — Durable workflow persistence implementation

Added Room v2 entities and DAOs for workflow runs, ordered steps and append-only events; a v1→v2 migration; idempotent workflow creation; exact-payload SHA-256 approval binding; approval invalidation when inputs change; lifecycle transitions; safe retry gates; explicit review states; automatic cancellation of non-terminal steps; and separate outcome verification. Added Robolectric tests for persistence, approval, retry, cancellation and migration/data preservation.

This implementation is on `build/workflow-persistence`. CI has not yet verified this checkpoint. No workflow UI, WorkManager executor, browser/app automation or external action adapter is claimed.


### 2026-11-17 — Workflow persistence merged and capability registry started

PR #3 merged Room v2 workflow runs, ordered steps and append-only events, v1→v2 migration, idempotent workflow creation, exact-payload SHA-256 approval binding, retry/review gates and cancellation. CI runs 111 and 112 passed the full pipeline, including Robolectric persistence and migration tests. Artifact 14340281041 is recorded above.

The next branch, `build/capability-registry`, adds a policy catalogue for implemented, unavailable and unregistered capabilities. It derives mandatory approval/retry settings and blocks starting capabilities that are not implemented. CI verification is pending.


### 2026-11-17 — Capability registry merged

PR #4 merged at `959eb52f677ccdeae2548b76c096e61a799ff135`. The registry records availability, effects, permissions, retry policy, mandatory approval and visible-surface requirements. Workflow creation derives safety metadata from registry policy, unknown capabilities fail closed, and runs with no implemented work cannot start. CI runs 129 and 130 passed all 30 unit/Robolectric tests, lint, APK assembly, signature verification and package identity checks. Artifact 14341082290 is recorded above.

Current boundary: Trader can capture problems, persist workflow plans and let the user manually drive supported steps. It does not yet execute browser, installed-app, AI, network-discovery or communication capabilities. Durable WorkManager execution and process-death recovery are the next major implementation phase.


### 2026-11-19 — First workflow control surface implementation

Implemented on `build/workflow-control-surface`:
- Added a workflow panel to each ProblemCase detail, creating a persisted workflow from its ordered plan steps.
- Added persisted workflow selection and live observation of workflow runs, ordered steps and append-only events.
- Added user-visible lifecycle controls for draft review, ready, start, pause, resume and cancel.
- Added manual step start/completion controls for executable capabilities, exact-payload approval affordance, and explicit skip for capabilities that are not implemented.
- Added outcome verification requiring a non-empty user evidence note; completing steps alone does not verify the ProblemCase outcome.
- Added an expandable capability availability catalogue; unimplemented capabilities are shown as unavailable rather than presented as working automation.
- Added English, Turkish and Polish strings for workflow controls and states.
- Updated the task ledger and workflow runtime documentation.

This surface is a manually driven workflow tracker, not an autonomous executor. It does not add WorkManager execution, process-death recovery, browser/app automation, AI, network discovery or external communication. PR #6 merged as `1f39c0a8060692ea80c31650b9c8cd861f500b2f`. Pull-request CI run `44436731548` and branch CI run `44436723330` passed, including tests, lint, APK assembly, signature and package identity verification. APK artifact `14489377527` (18,340,107 bytes) was downloaded and inspected; SHA-256: `4b063962d3fbc29a152a37ce4026c8c1ab51e526fb35234762ed756c8e74b446`. Physical-device acceptance remains pending.


### CI evidence — merged workflow control surface
- PR: [#6](https://github.com/ysfclc/Trader/pull/6), merged to `main` as `1f39c0a8060692ea80c31650b9c8cd861f500b2f`
- Successful CI: [pull-request run 203](https://github.com/ysfclc/Trader/actions/runs/44436731548) and [branch run 202](https://github.com/ysfclc/Trader/actions/runs/44436723330)
- Debug APK artifact: `14489377527`, 18,340,107 bytes, package `com.yusufculcu.trader.debug`, version `0.1.0-debug`
- APK SHA-256: `4b063962d3fbc29a152a37ce4026c8c1ab51e526fb35234762ed756c8e74b446`
- CI verified APK signature and package identity. Device install, launch and workflow acceptance are not yet verified.
