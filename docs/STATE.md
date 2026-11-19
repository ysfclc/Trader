# State Ledger
_Last updated: 2026-11-19 by Continuity Engineer — session 3_

## 1. Product snapshot
Trader is an Android-first, local-first workspace for capturing general problems/opportunities, preserving context and evidence, planning work, using explicitly supported capabilities and verifying outcomes. Trade/services/careers are initial specialist workflows; industrial tooling and machining between Türkiye and Poland/EU is the first pilot, not the product boundary. “Complete” means an end-to-end user outcome is demonstrated, not just that a screen or class exists.

## 2. Status board
| Area | State | Notes |
|---|---|---|
| Android shell / EN-TR-PL | 🟡 partial | Builds in CI; visual/device acceptance open |
| ProblemCase capture | 🟡 partial | Manual + shared text/URL; no file/screenshot/voice |
| Local case workspace | 🟡 partial | Edit/search/archive/restore; direct search/share UI tests and device acceptance open |
| Evidence | 🟡 partial | User-attached, source-linked, user-verified state; no automatic extraction |
| Plans / workflow records | 🟡 partial | Room persistence, events, approval and lifecycle rules; interrupted runs reconcile safely on startup; current execution remains user-driven |
| Capability registry | 🟡 partial | All 16 registered operations have typed input/output, cost, limit-mode and postcondition contracts; generic field validation and fail-closed promotion invariants pass CI; adapter-specific semantics/quotas remain open |
| Context packet | 🟡 partial | Builder tested; Android share chooser not device-accepted |
| Encrypted backup/restore | 🟡 partial | PR #8 and main CI verified; physical document-picker/export/restore acceptance open |
| WorkManager executor / recovery | 🟡 partial | Startup recovery flags ambiguous in-flight steps and pauses between-step runs; no WorkManager executor or automatic resume |
| Browser / installed-app automation | ⚪ not started | No execution adapter |
| AI / live discovery / matching | ⚪ not started | No model, web-source or buyer/supplier match runtime |
| Commercial records / outreach | ⚪ not started | No organizations, people, buyer/supplier records or send adapter |
| Release signing | ⚪ not started | Debug build only; release key not configured |
| Documentation / traceability | ✅ done | Product lock, task ledger, coverage matrix, risk register, capability contracts, decision log and changelog are maintained on the PR branch |

## 3. Currently in flight
- Device acceptance (T6.0/T6.0a) — install and exercise the current debug APK on the Redmi Note 12 Pro 5G; requires physical-device and user-driven acceptance.
- Backup acceptance (T1.4) — verify document-picker export/import, wrong-passphrase non-destructive behavior and valid full restore on device.
- PR #12 merge — typed capability contracts are CI-verified on the final application-source head; documentation and state-ledger updates are being included before merge.

## 4. Known gaps & stubs
- `app/src/main/java/com/yusufculcu/trader/ui/TraderApp.kt` — only Inbox and Activity tabs; Discover/Matches/Contacts navigation is not implemented.
- `app/src/main/java/com/yusufculcu/trader/data/TraderBackupManager.kt` — snapshot compatibility currently requires database schema version 3; future schema changes need backup migrations.
- `app/src/main/java/com/yusufculcu/trader/data/TraderRepository.kt` — no deterministic deduplication or reversible entity-merge workflow.
- No selected-file, screenshot or voice capture; document/attachment storage is not implemented.
- Workflow surface is manual; startup reconciliation handles persisted RUNNING states conservatively, but WorkManager execution, automatic resume and worker-level postcondition checks are not implemented.
- Capability contracts are declarative. Generic validation checks required fields, primitive/container types and unknown keys, but concrete adapters must still enforce cross-field rules, URI/origin scope, source access policy, permission/consent freshness, payload binding and actual numeric ceilings before execution.
- Browser/app automation, AI providers, public-source discovery, buyer/supplier matching and communications are absent.
- Search and ACTION_SEND boundary lack direct UI/instrumentation tests; no physical-device test has been recorded.
- Live Room storage has no dedicated app-level database encryption; encrypted backup protects exported files only.
- No release signing configuration; do not present the debug build as production.

## 5. Environment & how to run
- Repository: `ysfclc/Trader`; primary branch: `main`.
- Toolchain: JDK 17, Gradle Wrapper 8.13, AGP 8.13.0, Kotlin 2.2.20, KSP 2.2.20-2.0.4, compile SDK 36, min SDK 26, target SDK 35.
- Build/test/lint: `./gradlew --no-daemon clean testDebugUnitTest assembleDebug lintDebug`.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Package/version: `com.yusufculcu.trader.debug` / `0.1.0-debug`. See `docs/ANDROID_BUILD.md` for signing boundaries.

## 6. Next three increments
1. Physical-device acceptance of the current workspace and encrypted backup/restore on the Redmi Note 12 Pro 5G; log each scenario and any data-loss/regression result before extending execution features.
2. After that gate, implement durable WorkManager execution/recovery (pause, resume, cancel, unique work IDs, idempotency and uncertain side-effect handling) with process-death and postcondition tests.
3. Add one user-selected file/document capture vertical slice with raw-source preservation, evidence provenance, declared limits and tests; defer screenshot/voice until separately specified.

## 7. Open questions for the human
- [ ] No product decision blocks the implementation plan. Keep backup restore as explicit whole-workspace replacement; any merge behavior requires a separately specified decision.
- [ ] Before public distribution, configure a dedicated Trader release keystore via protected GitHub Actions secrets; never reuse another app's signing identity.

## Session handoff

**Done this session**
- Closed the previous merge gate: post-merge main CI run #276 passed the Android verification/build, tests, APK signature and package checks.
- Completed the typed execution-contract baseline for all 16 registered capabilities: input/output field types, required/optional and sensitive flags, side-effect declarations, cost disclosure, limit mode, retry/approval, permissions/consent and required postconditions.
- Added registry input/output validation for required fields, primitive/container types and unexpected keys, plus promotion invariants for adapter limits and external-action policy.
- Classified remote model inference as remote data disclosure requiring approval and explicit consent.
- Added six capability tests (CapabilityRegistryTest now 12); updated T3.0, capability docs, D-018, coverage, risks, README and changelog.

**State changed**
- `app/src/main/java/com/yusufculcu/trader/domain/CapabilityRegistry.kt`
- `app/src/test/java/com/yusufculcu/trader/domain/CapabilityRegistryTest.kt`
- `docs/CAPABILITY_REGISTRY.md`, `docs/TASKS.md`, `docs/RISKS.md`, `docs/DECISIONS.md`, `docs/COVERAGE.md`
- `README.md`, `CHANGELOG.md`, `docs/STATE.md`

**Verified**
- PR #12 Android CI run 279 passed on application-source head `9edf8f9b62844caebddd95e9e813733b16fdda46`: 44 tests, 0 failures, 0 errors, 0 skipped (JUnit artifact `14516142308`).
- The full pipeline passed Gradle unit tests, lint, debug APK assembly, APK signature verification and exact package/version assertions.
- The PR's final docs/state commit will receive its own CI run before merge. Main post-merge CI #276 from the preceding recovery increment passed.
- No physical-device acceptance was performed.

**Not done / next**
1. Complete physical-device acceptance on the Redmi Note 12 Pro 5G: capture, workspace search/archive/restore, workflow transitions and interrupted-run review, Android share chooser, backup export, wrong-passphrase safety and valid restore.
2. Only after that acceptance, implement WorkManager execution and recovery.
3. Add one file/document capture vertical slice with original-byte preservation and evidence provenance.

**Open questions**
- No new product decision is required. User-led device acceptance is the current gate.

**Risks / landmines**
- The capability registry is a contract and field validator, not an executor. Adapter-backed capabilities remain NOT_IMPLEMENTED.
- Generic field validation does not enforce semantic constraints, URI/origin allowlists, source terms, consent freshness, actual quotas, payload hash re-checks or external postconditions; every adapter must enforce these before promotion.
- Remote model inference transmits selected context to a third party and may incur fees; explicit user consent is mandatory.
- External send/submit actions require exact approval, prohibit retries and require provider confirmation. An uncertain outcome must go to NEEDS_REVIEW.
- A step marked RUNNING may already have acted; it becomes NEEDS_REVIEW, never auto-retried. Runs interrupted between steps become PAUSED.
- Restore replaces the entire Room workspace; export first. A lost backup passphrase is unrecoverable. Backup snapshot expects Room schema v3.
- Physical-device acceptance and release signing remain open. Debug signing is not production signing.
