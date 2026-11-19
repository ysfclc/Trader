# State Ledger
_Last updated: 2026-11-19 by Continuity Engineer — session 1_

## 1. Product snapshot
Trader is an Android-first, local-first workspace for capturing general problems/opportunities, preserving context and evidence, planning work, using explicitly supported capabilities and verifying outcomes. Trade/services/careers are initial specialist workflows; industrial tooling and machining between Türkiye and Poland/EU is the first pilot, not the product boundary. “Complete” means an end-to-end user outcome is demonstrated, not just that a screen or class exists.

## 2. Status board
| Area | State | Notes |
|---|---|---|
| Android shell / EN-TR-PL | 🟡 partial | Builds in CI; visual/device acceptance open |
| ProblemCase capture | 🟡 partial | Manual + shared text/URL; no file/screenshot/voice |
| Local case workspace | 🟡 partial | Edit/search/archive/restore; direct search/share UI tests and device acceptance open |
| Evidence | 🟡 partial | User-attached, source-linked, user-verified state; no automatic extraction |
| Plans / workflow records | 🟡 partial | Room persistence, events, approval and lifecycle rules; user-driven only |
| Capability registry | 🟡 partial | Fail-closed baseline; full inputs/outputs/cost/limits contract incomplete |
| Context packet | 🟡 partial | Builder tested; Android share chooser not device-accepted |
| Encrypted backup/restore | 🟡 partial | PR #8 + main CI verified; physical document-picker/restore acceptance open |
| WorkManager executor / recovery | ⚪ not started | No durable automatic step executor or process-death recovery |
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
- Workflow surface is manual; WorkManager is a dependency but no durable worker execution or process-death recovery is implemented.
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
- Reconciled the PR #8 backup implementation and CI evidence against the stale project ledger.
- Added the 12-row requirement coverage matrix and 14-item risk register; appended D-016 and corrected README, task and roadmap status.
- Merged the documentation-only continuity update as PR #9; no application source code changed.

**State changed**
- `docs/STATE.md`, `docs/COVERAGE.md`, `docs/RISKS.md`, `docs/DECISIONS.md`, `docs/TASKS.md`, `docs/ROADMAP.md`, `README.md` and `CHANGELOG.md`.

**Verified**
- Build: PASS, `./gradlew --no-daemon clean testDebugUnitTest assembleDebug lintDebug` (PR CI run 257 and post-merge main run 258).
- Tests: 37 passed, 0 failed, 0 errors, 0 skipped (main run 256 JUnit artifact `14507792856`; runs 257/258 also passed the full Android verification job).
- APK: CI signature/package checks passed; downloaded archive ZIP integrity checked; SHA-256 `faf4b1abbfa0227b5c274d34ab7bc59be570350c28c89073442cf63d54f0ec76`.

**Not done / next**
1. Install on Redmi Note 12 Pro 5G and run the workspace + backup/restore acceptance checklist.
2. Record device results and update R-01/T6.0; fix any defect before starting a new feature increment.

**Open questions**
- None blocking. Preserve whole-workspace replacement and separate release-signing setup.

**Risks / landmines**
- Restore replaces the entire Room workspace; export the current state first.
- Lost backup passphrase is unrecoverable.
- Current backup snapshot is tied to Room schema v3.
- Debug APK signing is not release signing; do not uninstall over a signature conflict without protecting local data.
