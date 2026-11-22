# State Ledger
_Last updated: 2026-11-22 — session 18 (durable discovery execution connected to Discover; CI run 44998620615 passed)_

## Product snapshot
Trader is an independent Android-first, local-first, free-first problem-solving and opportunity-execution system. The product loop is capture → understand outcome/constraints → gather evidence/context → discover options/resources/people → plan → execute bounded actions with permission/approval → verify outcomes → retain learning. It is not a trading-only app and must not force every problem into a lead. Trade, services and careers are initial specialist workflows, not product boundaries. Canonical scope: `docs/PROJECT_LOCK.md`; architecture, data, operating rules, product experience, source ecosystem and local AI designs remain authoritative.

## Current implementation
| Area | State | Verified boundary |
|---|---|---|
| Android identity | 🟡 CI-verified candidate; device/update acceptance open | Package `com.yusufculcu.trader.debug`; version `0.2.0-debug`, versionCode 2. Adaptive launcher/round icon wired. CI verified APK signature and exact package/version. Persistent signing secrets were absent, so run 44998620615 emitted a CI-only debug-signed APK. |
| Native shell / EN-PL-TR | 🟡 partial | Discover, Inbox, Pipeline, Activity, Browser and AI tabs exist. Intended Discover / Work / Live / Studio workspace remains unfinished. |
| Local data | 🟡 partial | Room schema v6 stores cases, plans, evidence, opportunities, workflow records, proof assets, discovery strategies, discovery runs and source runs. Encrypted backup snapshot v6 includes all current records plus public feed settings; legacy v1–v5 snapshots remain readable. Physical-device migration/restore acceptance open. |
| Continuity / backup | 🟡 partial | In-place updates retain app-private data only when package ID and signing certificate match; no stable-signed in-place update has been device-tested. Encrypted backup snapshot v6 includes cases, evidence, workflows, opportunities, proof assets, strategies, discovery runs/source runs and public feed settings; credentials and WebView cookies remain excluded. Restore uses validation, a Room transaction and compensating source-preference rollback. |
| Browser | 🟡 partial | Trader WebView, app-local cookies/SSO, popups, autofill, file chooser, Google OAuth fallback and visible-page capture/review. User confirmed login works on Redmi; broader device acceptance remains open. |
| Memo handoff | 🟡 partial | Versioned capture receiver and editable review/redaction are in Trader. Memo sender PR #15 remains draft/unmerged; installed Memo app does not yet expose it. |
| Discover / sources | 🟡 first execution slice CI-verified | Objective-driven DuckDuckGo search plus all enabled local RSS/Atom feeds run through one bounded coordinator. Per-source run history/status, timeout isolation, cancellation, cross-source URL dedupe, manual feed checks, browser handoff and save-to-Work as UNREVIEWED evidence. CI run 44998620615: 115 tests passed, 0 failures/errors/skips; build, lint, signature and package/version checks passed. No live-source or physical-device acceptance. |
| Company / people graph | 🟡 foundation only | ProofAsset/DiscoveryStrategy models, validation, Room migration, repository, codec and encrypted backup are CI-verified. There is no editor UI, strategy-driven execution, normalized Organization/Person record, dossier, identity review or explainable fit engine yet. |
| Automation / AI | 🟡 foundations | Typed capability registry, workflow approval/recovery rules, LiteRT-LM adapter and Model Hub foundations. No verified on-device model inference or durable WorkManager executor. |
| Social/community sources | ⚪ not implemented | Reddit OAuth, Bluesky/Mastodon APIs, authorized groups and Facebook browser-assisted research remain future provider-specific adapters. |
| Outreach | ⚪ not implemented | Drafting/approval/handoff contracts remain planned; no autonomous send. |

## Branches and build
- Canonical repository: `ysfclc/Trader`; canonical product specification: `docs/PROJECT_LOCK.md`.
- Current branch: `feature/discovery-run-persistence`; latest feature commit `9adb46cce95baa733e7c6f346904b313f924230d`.
- Candidate package/version: `com.yusufculcu.trader.debug`, versionCode 2, versionName `0.2.0-debug`.
- CI run 44998620615 validated versionCode 2 / `0.2.0-debug`; artifact SHA-256 `dd3b28aa150c360584abc67a273129bab627b665c7c22ecda3a84124abb463d5` (69,554,096 bytes). It was signed with the runner debug key because persistent signing secrets were not configured; do not assume upgrade compatibility with any previous APK.
- A dedicated PKCS12 development keystore has been generated outside Git for the next stable signing setup. Its SHA-256 certificate fingerprint is pinned in `.github/workflows/android.yml`; the private bundle contains the key, base64 secret value and password. Repository Actions secrets `TRADER_KEYSTORE_BASE64` and `TRADER_KEYSTORE_PASSWORD` remain absent until GitHub sign-in is completed. CI labels builds without the key as CI-only and not for installation.
- Toolchain: JDK 17, Gradle 8.13, AGP 8.13.0, Kotlin Android/Compose 2.4.21, KSP 2.3.12, SDK 36; build: `./gradlew --no-daemon clean testDebugUnitTest assembleDebug lintDebug`.

## Immediate next increments
1. Build the user-facing ProofAsset and DiscoveryStrategy editors; attach real evidence to claims, define problem signatures/targets/queries/disqualifiers, and run a saved strategy through the coordinator.
2. Add normalized Organization and Person records with source-linked claims, aliases/domains, reversible identity resolution and dossier UI.
3. Add deterministic, explainable ProofAsset-to-Organization fit assessments: support, contradictions, unknowns and reviewer override.
4. Capture browser evidence into dossiers, then build requirement/offer extraction and comparison for the Türkiye → Poland/EU industrial tooling pilot.
5. Add approved, recipient-bound draft/handoff and actual reply/outcome tracking; no false sent state.
6. Only then add durable WorkManager automation, local AI orchestration and the Discover / Work / Live / Studio shell; scheduled polling remains gated by backoff/cursor/recovery design.
7. Complete physical-device acceptance for migrations, encrypted backup/restore, signing continuity, browser session, source runs and the first end-to-end journey.

## Known risks / constraints
- CI success is not device acceptance. No current live feed/device smoke test exists. Run 405 used the same 62-file app tree as PR #18 head and passed 98 tests, lint and build.
- Previous ephemeral-signed APK cannot be assumed upgrade-compatible with the new persistent signing identity. If it is already installed, do not attempt a blind reinstall; first protect existing workspace data and separately export source settings because those are not yet in the encrypted backup.
- Android `allowBackup=false` disables OS cloud backup; explicit encrypted Trader export is the recovery mechanism. In-place signed updates retain app-private data.
- Local Room database is not separately SQLCipher-encrypted; do not store provider credentials or highly sensitive secrets there.
- Feed checks are manual; no scheduled monitoring, private group scraping, access-control bypass or auto-send.
- Keep all original evidence/provenance; distinguish source facts, AI inference, assumptions and unknowns.

## Session Handoff

**Done this session**
- Added adaptive launcher and round icons; wired manifest.
- Kept package ID stable; incremented versionCode 1 → 2 and versionName 0.1.0 → 0.2.0.
- Added optional stable Trader development signing to Gradle/CI; generated a dedicated keystore outside the repository. The workflow refuses to label an ephemeral-signed artifact installable.
- Extended encrypted backup snapshot v1 → v2 to include public feed configuration, enabled state and source health; legacy v1 backup remains readable. Added source-list validation, preference rollback on Room restore failure, and an encrypted round-trip test.
- Updated backup copy EN/PL/TR; locked persistence and backup decisions/tasks/risks/coverage/build docs/README/changelog.
- Archived historical handoffs in `docs/STATE_HISTORY.md` to keep the active ledger concise.

**State changed**
- `app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`, `.github/workflows/android.yml`
- `app/src/main/java/com/yusufculcu/trader/data/TraderBackupManager.kt`, `app/src/main/java/com/yusufculcu/trader/discovery/DiscoverySourceStore.kt`, `app/src/main/java/com/yusufculcu/trader/ui/TraderApp.kt`
- `app/src/test/java/com/yusufculcu/trader/data/TraderBackupManagerTest.kt`; EN/PL/TR backup strings
- `app/src/main/res/drawable/ic_trader_launcher_foreground.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml`, `app/src/main/res/values/trader_icon_colors.xml`
- `docs/ANDROID_BUILD.md`, `docs/TASKS.md`, `docs/DECISIONS.md`, `docs/RISKS.md`, `docs/STATE.md`, `docs/STATE_HISTORY.md`, `README.md`, `CHANGELOG.md`

**Verified**
- Build: PASS — `./gradlew --no-daemon clean testDebugUnitTest assembleDebug lintDebug`, CI run 405.
- Tests: 98 passed, 0 failures, 0 errors, 0 skipped (JUnit artifact 14699299934).
- APK package/version: `com.yusufculcu.trader.debug`, `0.2.0-debug`, versionCode 2; signature valid. CI artifact 14699553867 is explicitly CI-only because signing secrets are absent.
- App source tree: all 62 `app/` files byte-for-byte match PR #18 head. No physical-device or live-feed test. Stable certificate fingerprint path not exercised because secrets are not configured.

**Not done / next**
1. User configures `TRADER_KEYSTORE_BASE64` and `TRADER_KEYSTORE_PASSWORD` from the secure setup bundle; then trigger CI and verify stable APK signature.
2. Verify encrypted backup snapshot v2 restore/rollback on the Redmi before any one-time signing transition.
3. Verify in-place update, Room/preferences/WebView continuity on the Redmi.
4. Continue ProofAsset → source evidence → Organization/Person dossier → explainable fit; preserve whole product scope.

**Open questions**
- Whether an older ephemeral-signed Trader APK is currently installed on the Redmi; do not assume it can accept a new signing certificate.

**Risks / landmines**
- Signing secrets are not configured yet; no installable APK has been produced. Do not install the CI-only artifact.
- Existing run-385 APK is signed with a different ephemeral certificate and cannot accept the stable key in place.
- Source settings are now in encrypted backup snapshot v2; device restore acceptance remains open. Browser cookies and provider credentials are intentionally excluded.
- No physical-device acceptance has been performed for this icon/update candidate.


## Session 18 handoff — durable discovery execution connected to Discover

**Implemented on branch** `feature/discovery-run-persistence`, commit `9adb46cce95baa733e7c6f346904b313f924230d`.

- Connected `DiscoveryScreen` to the existing `DiscoveryExecutionCoordinator` and `DiscoveryRunRepository`; the previous UI had no call site to either component.
- Added a query-aware DuckDuckGo adapter to the same registry as configurable RSS/Atom sources. A user objective now runs public search and all enabled local feeds through one bounded execution.
- Persisted run/source progress in the visible UI, with recent run history, source statuses, result counts, timeout isolation, cancellation and cross-source URL deduplication.
- Timeout exceptions are caught before general cancellation, so one slow provider is recorded as `TIMEOUT` and does not cancel other sources.
- Added EN/PL/TR labels and help text; help explicitly says feed items are not guaranteed to match the objective and results remain unverified leads.
- Added coordinator regression coverage for timeout isolation and objective search across two sources.

**Verified**
- CI run [44998620615](https://github.com/ysfclc/Trader/actions/runs/44998620615): 115 tests passed, 0 failures, 0 errors, 0 skipped; `clean testDebugUnitTest assembleDebug lintDebug` passed; APK signature and exact package/version checks passed.
- Debug APK artifact `14745225976`: 69,554,096 bytes; SHA-256 `dd3b28aa150c360584abc67a273129bab627b665c7c22ecda3a84124abb463d5`; CI-only runner signature, not a stable-signed update.

**Still open**
- No proof-asset/strategy editor or strategy-driven query execution.
- No normalized Organization/Person dossiers, evidence-linked company claims or fit assessment.
- No WorkManager scheduler, caching policy, aggregated source health, live-feed smoke test or physical-device acceptance.
- Discover search snippets/feed items remain unreviewed leads until the original source is examined; saved items enter Work as UNREVIEWED evidence.

## Session 17 handoff — proof asset and discovery strategy persistence

**Implemented on branch** `feature/public-search-adapter`, extending PR #18.

- Added versioned `ProofAsset`, evidence-linked `ProofClaim`, `ProblemSignature`, `DiscoveryTarget`, `DiscoveryQuery` and editable `DiscoveryStrategy` domain records.
- Added fail-closed validation for schema versions, IDs, duplicate claims/signatures, evidence requirements for SUPPORTED claims, bounded lists/text/query counts/result limits and timestamps.
- Added Room v4→v5 migration and dedicated tables/DAOs for proof assets and discovery strategies.
- Added a stable JSON codec and repository validation that refuses missing evidence references and orphan strategies.
- Extended encrypted backup snapshot v2→v3; restore validates row/payload identity, strategy→asset links and claim→evidence links, while older snapshots remain readable with empty new groups.
- Added persistence, invalid-reference and encrypted backup/restore tests.

**Not yet done / verification pending**
- CI has not yet verified the new Room migration, persistence and encrypted backup integration. No APK or device acceptance is claimed.
- No UI for editing proof assets or strategies exists yet.
- Next: verify Room migration and backup compatibility; then add DiscoveryRun/SourceRun persistence and cancellation semantics.
