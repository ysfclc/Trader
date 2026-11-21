# State Ledger
_Last updated: 2026-11-21 — session 16 (proof asset and discovery strategy domain foundation; CI pending)_

## Product snapshot
Trader is an independent Android-first, local-first, free-first problem-solving and opportunity-execution system. The product loop is capture → understand outcome/constraints → gather evidence/context → discover options/resources/people → plan → execute bounded actions with permission/approval → verify outcomes → retain learning. It is not a trading-only app and must not force every problem into a lead. Trade, services and careers are initial specialist workflows, not product boundaries. Canonical scope: `docs/PROJECT_LOCK.md`; architecture, data, operating rules, product experience, source ecosystem and local AI designs remain authoritative.

## Current implementation
| Area | State | Verified boundary |
|---|---|---|
| Android identity | 🟡 implementation pending CI | Package stays `com.yusufculcu.trader.debug`; candidate version `0.2.0-debug`, versionCode 2. Adaptive launcher/round icon wired. Stable signing support implemented; Actions secrets not yet configured. |
| Native shell / EN-PL-TR | 🟡 partial | Discover, Inbox, Pipeline, Activity, Browser and AI tabs exist. Intended Discover / Work / Live / Studio workspace remains unfinished. |
| Local data | 🟡 partial | Room-backed cases, plans, evidence, opportunities, workflow records; encrypted backup snapshot v2 now includes public discovery feed settings and preserves legacy v1 import. Physical-device restore acceptance open. |
| Continuity / backup | 🟡 partial | Same package + signing certificate + in-place update preserves app-private data. Uninstall/clear-storage destroys it. Public discovery feed settings in preferences are included in encrypted backup snapshot v2; credentials/cookies are excluded. Restore uses validated preference replacement with compensating rollback if Room restore fails. |
| Browser | 🟡 partial | Trader WebView, app-local cookies/SSO, popups, autofill, file chooser, Google OAuth fallback and visible-page capture/review. User confirmed login works on Redmi; broader device acceptance remains open. |
| Memo handoff | 🟡 partial | Versioned capture receiver and editable review/redaction are in Trader. Memo sender PR #15 remains draft/unmerged; installed Memo app does not yet expose it. |
| Discover / sources | 🟡 first slice CI-verified | One-shot DuckDuckGo search; user-configurable RSS/Atom feeds; manual checks, source health, browser handoff and source-linked UNREVIEWED evidence. CI run 405: 98 tests passed (0 failures/errors/skips), lint/build/signature/package checks. No live-feed or physical-device acceptance. |
| Company / people graph | 🟡 domain foundation | Versioned ProofAsset/DiscoveryStrategy domain models and validation are added on `build/proofasset-discovery-strategy`; Room persistence, backup integration, company dossiers and explainable fit remain open. |
| Automation / AI | 🟡 foundations | Typed capability registry, workflow approval/recovery rules, LiteRT-LM adapter and Model Hub foundations. No verified on-device model inference or durable WorkManager executor. |
| Social/community sources | ⚪ not implemented | Reddit OAuth, Bluesky/Mastodon APIs, authorized groups and Facebook browser-assisted research remain future provider-specific adapters. |
| Outreach | ⚪ not implemented | Drafting/approval/handoff contracts remain planned; no autonomous send. |

## Branches and build
- Canonical repository: `ysfclc/Trader`; canonical product specification: `docs/PROJECT_LOCK.md`.
- Current branch: `feature/public-search-adapter`; PR #18 open, not merged.
- Candidate package/version: `com.yusufculcu.trader.debug`, versionCode 2, versionName `0.2.0-debug`.
- Previous CI run 385 validated versionCode 1 / `0.1.0-debug` using an ephemeral runner debug key. That certificate cannot be assumed compatible with the new stable Trader key.
- A dedicated PKCS12 development keystore has been generated outside Git for the next stable signing setup. Its SHA-256 certificate fingerprint is pinned in `.github/workflows/android.yml`; the private bundle contains the key, base64 secret value and password. Repository Actions secrets `TRADER_KEYSTORE_BASE64` and `TRADER_KEYSTORE_PASSWORD` remain absent until GitHub sign-in is completed. CI labels builds without the key as CI-only and not for installation.
- Toolchain: JDK 17, Gradle 8.13, AGP 8.13.0, Kotlin Android/Compose 2.4.21, KSP 2.3.12, SDK 36; build: `./gradlew --no-daemon clean testDebugUnitTest assembleDebug lintDebug`.

## Immediate next increments
1. Configure the two signing secrets; run CI; verify stable certificate fingerprint and build an installable candidate. Never uninstall/clear data as routine update procedure.
2. Verify source-settings backup snapshot v2 and restore/rollback on Redmi; keep credentials/cookies/signing material excluded.
3. Verify in-place update and backup/restore on Redmi Note 12 Pro 5G, including Room data, feed settings and WebView session.
4. Persist ProofAsset/DiscoveryStrategy with a Room migration and encrypted-backup schema migration; then add durable DiscoveryRun/SourceRun, source-grounded Organization/Person dossiers and explainable matching.
5. Add scheduled polling only after durable WorkManager cancellation, backoff, cursor and process-death recovery are proven. Then add community adapters with explicit permissions and retention/deletion controls.
6. Complete Work/Live/Studio UX, browser tabs/history/downloads and the end-to-end problem → plan → action → verified outcome loop.

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


## Session 16 handoff — proof assets and discovery strategy foundation

**Implemented on branch** `build/proofasset-discovery-strategy`, based on PR #18 head `ec03edf10e76c309b5291fda568af65206b95aa1`.

- Added versioned `ProofAsset`, evidence-linked `ProofClaim`, `ProblemSignature`, `DiscoveryTarget`, `DiscoveryQuery` and editable `DiscoveryStrategy` domain records.
- Added fail-closed validation for schema versions, IDs, duplicate claims/signatures, evidence requirements for SUPPORTED claims, bounded lists/text/query counts/result limits and timestamps.
- Added 5 unit tests covering valid strategy, evidence-backed claims, missing asset, bounded query, duplicate signature IDs and unsupported schema versions.

**Not yet done / verification pending**
- Room persistence, migrations and encrypted backup compatibility are not implemented; these domain models are not yet user-editable in the app.
- CI has not yet completed for this branch. No APK or device acceptance is claimed.
- Next: persist these records without losing existing v4 data; bump encrypted backup snapshot schema and test legacy v1/v2 compatibility; add DiscoveryRun/SourceRun persistence and cancellation semantics.
