# State Ledger
_Last updated: 2026-11-19 by Continuity Engineer — session 2_

## 1. Product snapshot
Trader is an Android-first, local-first workspace for capturing general problems/opportunities, preserving context and evidence, planning work, using explicitly supported capabilities and verifying outcomes. Trade/services/careers are initial specialist workflows; industrial tooling and machining between Türkiye and Poland/EU is the first pilot, not the product boundary. “Complete” means an end-to-end user outcome is demonstrated, not just that a screen or class exists.

## 2. Status board
| Area | State | Notes |
|---|---|---|
| Android shell / EN-TR-PL | 🟡 partial | Builds in CI; visual/device acceptance open |
| ProblemCase capture | 🟡 partial | Manual + shared text/URL; no file/screenshot/voice |
| Local case workspace | 🟡 partial | Edit/search/archive/restore; direct search/share UI tests and device acceptance open |
| Evidence | 🟡 partial | User-attached, source-linked, user-verified state; no automatic extraction |
| Plans / workflow records | 🟡 partial | Room persistence, events, approval and lifecycle rules; interrupted runs now reconcile safely on startup; execution remains user-driven |
| Capability registry | 🟡 partial | Fail-closed baseline; full inputs/outputs/cost/limits contract incomplete |
| Context packet | 🟡 partial | Builder tested; Android share chooser not device-accepted |
| Encrypted backup/restore | 🟡 partial | PR #8 + main CI verified; physical document-picker/restore acceptance open |
| WorkManager executor / recovery | 🟡 partial | Startup recovery flags ambiguous in-flight steps and pauses between-step runs; no WorkManager executor or automatic resume |
| Browser / installed-app automation | ⚪ not started | No execution adapter |
| AI / live discovery / matching | ⚪ not started | No model, web-source or buyer/supplier match runtime |
| Commercial records / outreach | ⚪ not started | No organizations, people, buyer/supplier records or send adapter |
| Release signing | ⚪ not started | Debug build only; release key not configured |
| Documentation / traceability | ✅ done | 83-line ledger, 12-row coverage matrix, 14-item risk register and changelog are on main |

## 3. Currently in flight
- Device acceptance (T6.0/T6.0a) — install and exercise the current debug APK on the Redmi Note 12 Pro 5G; blocked on a physical Android device and user-driven acceptance.
- Backup acceptance (T1.4) — verify document-picker export/import, wrong-passphrase non-destructive behavior and valid full restore on device.
- Capability contract (T3.0) — complete typed input/output, cost, limit and postcondition metadata before adding executors.

## 4. Known gaps & stubs
- `app/src/main/java/com/yusufculcu/trader/ui/TraderApp.kt` — only Inbox and Activity tabs; Discover/Matches/Contacts navigation is not implemented.
- `app/src/main/java/com/yusufculcu/trader/data/TraderBackupManager.kt` — snapshot compatibility currently requires database schema version 3; future schema changes need backup migrations.
- `app/src/main/java/com/yusufculcu/trader/data/TraderRepository.kt` — no deterministic deduplication or reversible entity-merge workflow.
- No selected-file, screenshot or voice capture; document/attachment storage is not implemented.
- Workflow surface is manual; startup reconciliation handles persisted RUNNING states conservatively, but WorkManager execution, automatic resume and worker-level postcondition checks are not implemented.
- Browser/app automation, AI providers, public-source discovery, buyer/supplier matching and communications are absent.
- Search and ACTION_SEND boundary lack direct UI/instrumentation tests; no physical-device test has been recorded.
- Live Room storage has no dedicated app-level database encryption; encrypted backup protects exported files only.
- No release signing configuration; do not present the debug APK as production.

## 5. Environment & how to run
- Repository: `ysfclc/Trader`; primary branch: `main`.
- Toolchain: JDK 17, Gradle Wrapper 8.13, AGP 8.13.0, Kotlin 2.2.20, KSP 2.2.20-2.0.4, compile SDK 36, min SDK 26, target SDK 35.
- Build/test/lint: `./gradlew --no-daemon clean testDebugUnitTest assembleDebug lintDebug`.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Package/version: `com.yusufculcu.trader.debug` / `0.1.0-debug`. See `docs/ANDROID_BUILD.md` for signing boundaries.

## 6. Next three increments
1. Physical-device acceptance of the current workspace and encrypted backup/restore; log each scenario and any data-loss/regression result before extending features.
2. Specify and implement durable workflow execution/recovery (pause, resume, cancel, idempotency and uncertain side-effect handling) with process-death tests.
3. Add one user-selected file/document capture vertical slice with raw-source preservation, evidence provenance, limits and tests; defer screenshot/voice until separately specified.

## 7. Open questions for the human
- [ ] No product decision blocks the next step. Default: keep backup restore as explicit whole-workspace replacement; any merge behavior requires a separately specified decision.
- [ ] Before public distribution, configure a dedicated Trader release keystore via protected GitHub Actions secrets; never reuse another app's signing identity.

## Session handoff

**Done this session**
- Implemented transactional startup reconciliation for workflows left RUNNING after process interruption.
- In-flight steps become NEEDS_REVIEW and lose stale approval; runs interrupted between steps become PAUSED.
- Added file-backed close/reopen and idempotency coverage; recorded D-017 and updated workflow docs, task ledger, coverage and risk register.

**State changed**
- `app/src/main/java/com/yusufculcu/trader/data/TraderRepository.kt`
- `app/src/main/java/com/yusufculcu/trader/viewmodel/TraderViewModel.kt`
- `app/src/test/java/com/yusufculcu/trader/data/WorkflowPersistenceTest.kt`
- `docs/TASKS.md`, `docs/WORKFLOW_RUNTIME.md`, `docs/COVERAGE.md`, `docs/RISKS.md`, `docs/DECISIONS.md`, `CHANGELOG.md`, `docs/STATE.md`

**Verified**
- Build: pending CI for this branch; do not treat the increment as shipped until it passes.
- Tests: new file-backed process-reopen test added; total expected suite is 38, pending execution.
- Physical device: not tested; requires Redmi Note 12 Pro 5G.

**Not done / next**
1. Run full Android CI and fix any failures before merge.
2. Install on Redmi Note 12 Pro 5G and exercise capture, workflow lifecycle and encrypted backup/restore.
3. Implement WorkManager execution only after this recovery boundary is accepted.

**Open questions**
- None blocking. Ambiguous in-flight steps must remain human-reviewed; never auto-retry.

**Risks / landmines**
- A step marked RUNNING may already have performed its action; recovery moves it to NEEDS_REVIEW and clears its approval.
- Runs interrupted between steps become PAUSED, not automatically resumed.
- Restore replaces the entire Room workspace; export first. Lost backup passphrase is unrecoverable.
- Backup snapshot expects Room schema v3; future schema changes need backup compatibility tests.
- Debug signing is not release signing.
