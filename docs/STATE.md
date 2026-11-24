# State Ledger
_Last updated: 2026-11-24 — discovery provenance integrity hardening CI-verified_

## Product snapshot
Trader is an independent Android-first, local-first, free-first problem-solving and opportunity-execution system. The product loop is capture → understand outcome/constraints → gather evidence/context → discover options/resources/people → plan → execute bounded actions with permission/approval → verify outcomes → retain learning. It is not a trading-only app and must not force every problem into a lead. Trade, services and careers are initial specialist workflows, not product boundaries. Canonical scope: `docs/PROJECT_LOCK.md`; architecture, data, operating rules, product experience, source ecosystem and local AI designs remain authoritative.

## Current implementation
| Area | State | Verified boundary |
|---|---|---|
| Android identity | 🟡 stable signing configured; device update acceptance open | Package `com.yusufculcu.trader.debug`; current build `0.4.0-debug`, versionCode 4. Dedicated Trader keystore remains configured. CI signature/package verification and install-over-existing device acceptance are tracked separately. |
| Native shell / EN-PL-TR | 🟡 partial | Discover, Inbox, Pipeline, Activity, Browser and AI tabs exist. Intended Discover / Work / Live / Studio workspace remains unfinished. |
| Local data | 🟡 partial | Room schema v9 stores cases, plans, evidence, opportunities, workflow records, proof assets, discovery strategies, discovery runs, source runs/findings, organizations, organization sources, source-linked claims and company-scoped people/contact records. Encrypted backup snapshot v6 includes these records plus public feed settings; legacy snapshots remain readable. Physical-device migration/restore acceptance open. |
| Continuity / backup | 🟡 partial | In-place updates retain app-private data only when package ID and signing certificate match; physical install-over-existing is still unverified. The earlier one-time signing transition required uninstalling the previous app; no recovery claim is made without an exported .traderbackup. Snapshot v5 includes all current records, including organizations, source snapshots and claims; credentials and WebView cookies remain excluded. Restore uses validation, a Room transaction and compensating source-preference rollback. |
| Browser | 🟡 partial | Trader WebView, app-local cookies/SSO, popups, autofill, file chooser, Google OAuth fallback and visible-page capture/review. User confirmed login works on Redmi; broader device acceptance remains open. |
| Memo handoff | 🟡 partial | Versioned capture receiver and editable review/redaction are in Trader. Memo sender PR #15 remains draft/unmerged; installed Memo app does not yet expose it. |
| Discover / sources | 🟡 first execution slice CI-verified | Objective-driven DuckDuckGo search plus all enabled local RSS/Atom feeds run through one bounded coordinator. Per-source run history/status, timeout isolation, cancellation, cross-source URL dedupe, manual feed checks, browser handoff and save-to-Work as UNREVIEWED evidence. Findings now require an active matching source run; adapter source-ID mismatches fail closed, and zero-source runs are rejected before persistence. CI run 44998620615 verified the first execution slice. Latest CI run 45394348906 passed the full unit-test, lint and debug-build pipeline plus persistent-certificate and package/version checks. No live-source or physical-device acceptance. |
| Company / people graph | 🟡 first dossier slice implemented | Company dossier list and editor are available inside Pipeline. Users can create a dossier manually or from a Discover result, retain source snapshots, author source-linked claims and explicitly review them as supported, contradicted or rejected. Company-scoped contact records are implemented with validation and explicit no-auto-merge behavior. Person aliases/identity review, structured buyer requirements/supplier offers and explainable ProofAsset fit remain incomplete. |
| Automation / AI | 🟡 foundations | Typed capability registry, workflow approval/recovery rules, LiteRT-LM adapter and Model Hub. Curated source metadata checks and consent-gated immutable candidate downloads with exact-size/SHA-256 verification, cancellation, temporary-file cleanup and no automatic install are implemented; current feature CI is pending. No verified on-device model inference or durable WorkManager executor. |
| Social/community sources | ⚪ not implemented | Reddit OAuth, Bluesky/Mastodon APIs, authorized groups and Facebook browser-assisted research remain future provider-specific adapters. |
| Outreach | ⚪ not implemented | Drafting/approval/handoff contracts remain planned; no autonomous send. |

## Branches and build
- Canonical repository: `ysfclc/Trader`; canonical product specification: `docs/PROJECT_LOCK.md`.
- Current branch: `feature/discovery-run-persistence`; latest app implementation commit: [5099d19](https://github.com/ysfclc/Trader/commit/5099d1929176f8b2c380e9747102d662246eee3a).
- Verified package/version: `com.yusufculcu.trader.debug`, versionCode 5, versionName 0.5.0-debug.
- Persistent Trader development signing remains configured in GitHub Actions. Do not change the package ID or signing certificate; every subsequent build must increment versionCode. The latest CI run and artifact are recorded after verification below.
- Toolchain: JDK 17, Gradle 8.13, AGP 8.13.0, Kotlin Android/Compose 2.4.21, KSP 2.3.12, SDK 36; build: `./gradlew --no-daemon clean testDebugUnitTest assembleDebug lintDebug`.

## Immediate next increments
1. Add Person records, aliases and reversible duplicate/identity review without automatic merges.
2. Add structured BuyerRequirement and SupplierOffer records with line items and evidence links.
3. Add deterministic, explainable ProofAsset-to-Organization fit assessments: support, contradictions, unknowns and reviewer override.
4. Capture browser evidence into dossiers, then compare requirements and offers for the Türkiye → Poland/EU industrial tooling pilot.
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

## Latest handoff (2026-11-24)

- Branch: `feature/discovery-run-persistence`; latest app implementation commit: [5099d19](https://github.com/ysfclc/Trader/commit/5099d1929176f8b2c380e9747102d662246eee3a).
- Enforced discovery provenance: findings are persisted transactionally only while the parent discovery run and matching source run are RUNNING; adapter source-ID mismatches fail the source; zero-source requests are rejected before run creation.
- Added regression coverage for cross-source/late findings, adapter source-ID mismatch and empty source selections.
- CI run [45394348906](https://github.com/ysfclc/Trader/actions/runs/45394348906) passed `./gradlew --no-daemon clean testDebugUnitTest assembleDebug lintDebug` in 4m45s. APK signature, persistent certificate fingerprint and exact package/version checks passed.
- Verified APK identity: `com.yusufculcu.trader.debug`, `0.4.0-debug`, versionCode 4. Installable artifact: [14919883504](https://github.com/ysfclc/Trader/actions/runs/45394348906/artifacts/14919883504); test reports: [14920726257](https://github.com/ysfclc/Trader/actions/runs/45394348906/artifacts/14920726257).
- No physical-device update, migration/restore or live-source acceptance is claimed.

## Historical handoffs

Older session-by-session notes are preserved in `docs/STATE_HISTORY.md`.


## Company contact increment (2026-11-24)

- Added company-scoped people/contact records for name, role, email, phone, LinkedIn profile and operator-entered notes. Records can be created, edited and deleted inside a company dossier.
- Added Room v8→v9 migration with company foreign key/cascade and encrypted backup snapshot v6. Older backup snapshots remain readable with an empty contact list.
- Contact validation rejects malformed email and non-LinkedIn profile URLs. Similar names are not merged; aliases and reversible identity review remain future work.
- Implementation committed; CI verification and APK artifact pending. No physical-device acceptance is claimed.
