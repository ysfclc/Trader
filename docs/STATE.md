# State Ledger
_Last updated: 2026-11-21 — session 15 (app identity, launcher and persistent update path; CI pending)_

## Product snapshot
Trader is an independent Android-first, local-first, free-first problem-solving and opportunity-execution system. The product loop is capture → understand outcome/constraints → gather evidence/context → discover options/resources/people → plan → execute bounded actions with permission/approval → verify outcomes → retain learning. It is not a trading-only app and must not force every problem into a lead. Trade, services and careers are initial specialist workflows, not product boundaries. Canonical scope: `docs/PROJECT_LOCK.md`; architecture, data, operating rules, product experience, source ecosystem and local AI designs remain authoritative.

## Current implementation
| Area | State | Verified boundary |
|---|---|---|
| Android identity | 🟡 implementation pending CI | Package stays `com.yusufculcu.trader.debug`; candidate version `0.2.0-debug`, versionCode 2. Adaptive launcher/round icon wired. Stable signing support implemented; Actions secrets not yet configured. |
| Native shell / EN-PL-TR | 🟡 partial | Discover, Inbox, Pipeline, Activity, Browser and AI tabs exist. Intended Discover / Work / Live / Studio workspace remains unfinished. |
| Local data | 🟡 partial | Room-backed cases, plans, evidence, opportunities, workflow records; schema migrations and encrypted export/restore exist. Physical-device restore acceptance open. |
| Continuity / backup | 🟡 partial | Same package + signing certificate + in-place update preserves app-private data. Uninstall/clear-storage destroys it. Public discovery feed settings in preferences are now included in encrypted backup snapshot v2; credentials/cookies are excluded. |
| Browser | 🟡 partial | Trader WebView, app-local cookies/SSO, popups, autofill, file chooser, Google OAuth fallback and visible-page capture/review. User confirmed login works on Redmi; broader device acceptance remains open. |
| Memo handoff | 🟡 partial | Versioned capture receiver and editable review/redaction are in Trader. Memo sender PR #15 remains draft/unmerged; installed Memo app does not yet expose it. |
| Discover / sources | 🟡 first slice CI-verified | One-shot DuckDuckGo search; user-configurable RSS/Atom feeds; manual checks, source health, browser handoff and source-linked UNREVIEWED evidence. CI run 385: 97 tests passed plus lint/build/signature/package checks. No live-feed or physical-device acceptance. |
| Company / people graph | ⚪ not implemented | Normalized Organization/Person, ProofAsset/DiscoveryStrategy, company dossiers, explainable fit and qualification remain open. |
| Automation / AI | 🟡 foundations | Typed capability registry, workflow approval/recovery rules, LiteRT-LM adapter and Model Hub foundations. No verified on-device model inference or durable WorkManager executor. |
| Social/community sources | ⚪ not implemented | Reddit OAuth, Bluesky/Mastodon APIs, authorized groups and Facebook browser-assisted research remain future provider-specific adapters. |
| Outreach | ⚪ not implemented | Drafting/approval/handoff contracts remain planned; no autonomous send. |

## Branches and build
- Canonical repository: `ysfclc/Trader`; canonical product specification: `docs/PROJECT_LOCK.md`.
- Current branch: `feature/public-search-adapter`; PR #18 open, not merged.
- Candidate package/version: `com.yusufculcu.trader.debug`, versionCode 2, versionName `0.2.0-debug`.
- Previous CI run 385 validated versionCode 1 / `0.1.0-debug` using an ephemeral runner debug key. That certificate cannot be assumed compatible with the new stable Trader key.
- A dedicated development keystore has been generated outside Git. Configure repository Actions secrets `TRADER_KEYSTORE_BASE64` and `TRADER_KEYSTORE_PASSWORD` using the private setup bundle. CI labels builds without the key as CI-only and not for installation. No new APK is verified yet.
- Toolchain: JDK 17, Gradle 8.13, AGP 8.13.0, Kotlin Android/Compose 2.4.21, KSP 2.3.12, SDK 36; build: `./gradlew --no-daemon clean testDebugUnitTest assembleDebug lintDebug`.

## Immediate next increments
1. Configure the two signing secrets; run CI; verify stable certificate fingerprint and build an installable candidate. Never uninstall/clear data as routine update procedure.
2. Verify source-settings backup snapshot v2 and restore/rollback tests on device; keep credentials/cookies/signing material excluded.
3. Verify in-place update and backup/restore on Redmi Note 12 Pro 5G, including Room data, feed settings and WebView session.
4. Implement ProofAsset + DiscoveryStrategy + durable DiscoveryRun/SourceRun, then source-grounded Organization/Person dossiers and explainable matching.
5. Add scheduled polling only after durable WorkManager cancellation, backoff, cursor and process-death recovery are proven. Then add community adapters with explicit permissions and retention/deletion controls.
6. Complete Work/Live/Studio UX, browser tabs/history/downloads and the end-to-end problem → plan → action → verified outcome loop.

## Known risks / constraints
- CI success is not device acceptance. No current live feed/device smoke test exists.
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
- Locked persistent app identity/update behavior in D-026 and updated build/task/risk/changelog/README documentation.
- Archived historical handoffs in `docs/STATE_HISTORY.md` to keep the active ledger concise.

**State changed**
- `app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`, `.github/workflows/android.yml`
- `app/src/main/res/drawable/ic_trader_launcher_foreground.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml`, `app/src/main/res/values/trader_icon_colors.xml`
- `docs/ANDROID_BUILD.md`, `docs/TASKS.md`, `docs/DECISIONS.md`, `docs/RISKS.md`, `docs/STATE.md`, `docs/STATE_HISTORY.md`, `README.md`, `CHANGELOG.md`

**Verified**
- Build: pending new CI run; no local Android SDK/Gradle build was run in this environment.
- Tests: pending new CI run; previous 97-test run 385 validates the prior source tree only.
- XML/manifest and Gradle/workflow changes require CI validation. Stable signing secret setup and fingerprint check are not yet complete.

**Not done / next**
1. User configures `TRADER_KEYSTORE_BASE64` and `TRADER_KEYSTORE_PASSWORD` from the secure setup bundle; then trigger CI and verify stable APK signature.
2. Add discovery source settings to encrypted backup/restore before any one-time signing transition.
3. Verify in-place update, Room/preferences/WebView continuity on the Redmi.
4. Continue ProofAsset → source evidence → Organization/Person dossier → explainable fit; preserve whole product scope.

**Open questions**
- Whether an older ephemeral-signed Trader APK is currently installed on the Redmi; do not assume it can accept a new signing certificate.

**Risks / landmines**
- Signing secrets are not configured yet; no new APK is safe to install as an update.
- Existing run-385 APK is signed with a different ephemeral certificate.
- Source settings are now in encrypted backup snapshot v2; device restore acceptance remains open.
- No physical-device acceptance has been performed for this icon/update candidate.
