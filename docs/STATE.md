# State Ledger
_Last updated: 2026-11-25 — Industrial Sourcing Desk 0.8.0 CI-verified; device acceptance open_

## Product snapshot
Trader is an independent Android-first, local-first, free-first problem-solving and opportunity-execution system. The product loop is capture → understand outcome/constraints → gather evidence/context → discover options/resources/people → plan → execute bounded actions with permission/approval → verify outcomes → retain learning. It is not a trading-only app and must not force every problem into a lead. Trade, services and careers are initial specialist workflows, not product boundaries. Canonical scope: `docs/PROJECT_LOCK.md`; architecture, data, operating rules, product experience, source ecosystem and local AI designs remain authoritative.

## Current implementation
| Area | State | Verified boundary |
|---|---|---|
| Android identity | 🟡 stable signing configured; device update acceptance open | Package `com.yusufculcu.trader.debug`; current build `0.8.0-debug`, versionCode 8. Dedicated Trader keystore remains configured. CI signature/package verification and install-over-existing device acceptance are tracked separately. |
| Native shell / EN-PL-TR | 🟡 partial | Discover, Inbox, Sourcing, Pipeline, Activity, Browser and AI tabs exist. Intended Discover / Work / Live / Studio workspace remains unfinished. |
| Local data | 🟡 partial | Room schema v11 stores cases, plans, evidence, opportunities, workflow records, proof assets, discovery strategies, discovery runs, source runs/findings, organizations, organization sources, source-linked claims, company-scoped people/contact records, structured buyer requirements and line items, supplier offers and line items, source-evidence links, and human sourcing reviews. Encrypted backup snapshot v8 includes these records plus public feed settings; legacy snapshots remain readable. Physical-device migration/restore acceptance open. |
| Continuity / backup | 🟡 partial | In-place updates retain app-private data only when package ID and signing certificate match; physical install-over-existing is still unverified. The earlier one-time signing transition required uninstalling the previous app; no recovery claim is made without an exported .traderbackup. Snapshot v8 includes all current records, including organizations, source snapshots, claims, company contacts, buyer requirements, supplier offers, evidence links and sourcing decisions; credentials and WebView cookies remain excluded. Restore uses validation, a Room transaction and compensating source-preference rollback. |
| Browser | 🟡 partial | Trader WebView, app-local cookies/SSO, popups, autofill, file chooser, Google OAuth fallback and visible-page capture/review. User confirmed login works on Redmi; broader device acceptance remains open. |
| Memo handoff | 🟡 partial | Versioned capture receiver and editable review/redaction are in Trader. Memo sender PR #15 remains draft/unmerged; installed Memo app does not yet expose it. |
| Discover / sources | 🟡 first execution slice CI-verified | Objective-driven DuckDuckGo search plus all enabled local RSS/Atom feeds run through one bounded coordinator. Per-source run history/status, timeout isolation, cancellation, cross-source URL dedupe, manual feed checks, browser handoff and save-to-Work as UNREVIEWED evidence. Findings now require an active matching source run; adapter source-ID mismatches fail closed, and zero-source runs are rejected before persistence. CI run 44998620615 verified the first execution slice. Latest CI run 45394348906 passed the full unit-test, lint and debug-build pipeline plus persistent-certificate and package/version checks. No live-source or physical-device acceptance. |
| Company / sourcing graph | 🟡 first product CI-verified | Company dossiers, contacts, buyer requirements and supplier offers are stored with source links. Industrial Sourcing Desk compares buyer line items with supplier offer line items and persists human review decisions. Exact comparison rules are documented in `docs/PRODUCTS/INDUSTRIAL_SOURCING_DESK.md`; unknowns are never promoted to fit. Physical-device acceptance remains open. |
| Automation / AI | 🟡 foundations | Typed capability registry, workflow approval/recovery rules, LiteRT-LM adapter and Model Hub. Curated source metadata checks and consent-gated immutable candidate downloads with exact-size/SHA-256 verification, cancellation, temporary-file cleanup and no automatic install are implemented; current feature CI is pending. No verified on-device model inference or durable WorkManager executor. |
| Social/community sources | ⚪ not implemented | Reddit OAuth, Bluesky/Mastodon APIs, authorized groups and Facebook browser-assisted research remain future provider-specific adapters. |
| Outreach | ⚪ not implemented | Drafting/approval/handoff contracts remain planned; no autonomous send. |

## Branches and build
- Canonical repository: `ysfclc/Trader`; canonical product specification: `docs/PROJECT_LOCK.md`.
- Current branch: `feature/discovery-run-persistence`; latest app implementation commit: [b530d4e](https://github.com/ysfclc/Trader/commit/b530d4e05a347f0ff9c2520eaf6f7a3861058d27).
- Current package/version target: `com.yusufculcu.trader.debug`, versionCode 8, versionName `0.8.0-debug`; CI run [45574277490](https://github.com/ysfclc/Trader/actions/runs/45574277490) passed build, unit tests, lint, APK package/version and persistent signing-certificate checks. Installable artifact: [15003891645](https://github.com/ysfclc/Trader/actions/runs/45574277490/artifacts/15003891645).
- Persistent Trader development signing remains configured in GitHub Actions. Do not change the package ID or signing certificate; every subsequent build must increment versionCode. The latest CI run and artifact are recorded after verification below.
- Toolchain: JDK 17, Gradle 8.13, AGP 8.13.0, Kotlin Android/Compose 2.4.21, KSP 2.3.12, SDK 36; build: `./gradlew --no-daemon clean testDebugUnitTest assembleDebug lintDebug`.

## Immediate next increments
1. Complete physical-device acceptance of the Industrial Sourcing Desk: install-over-existing, Room 10→11 migration, encrypted backup/restore and a real buyer-to-shortlist journey.
2. Close operational gaps in the same sourcing product: editable offer/request records, multi-item offer maintenance, current source review and visible lifecycle handling.
3. Extend comparisons only where the buyer and supplier data can be compared deterministically; otherwise preserve explicit unknowns and ask for human review.
4. Add the next product only after the sourcing journey is usable and device-accepted. Do not expand into unrelated product areas in parallel.

## Known risks / constraints
- CI success is not device acceptance. No current live feed/device smoke test exists. Run 405 used the same 62-file app tree as PR #18 head and passed 98 tests, lint and build.
- Previous ephemeral-signed APK cannot be assumed upgrade-compatible with the new persistent signing identity. If it is already installed, do not attempt a blind reinstall; first protect existing workspace data and separately export source settings because those are not yet in the encrypted backup.
- Android `allowBackup=false` disables OS cloud backup; explicit encrypted Trader export is the recovery mechanism. In-place signed updates retain app-private data.
- Local Room database is not separately SQLCipher-encrypted; do not store provider credentials or highly sensitive secrets there.
- Feed checks are manual; no scheduled monitoring, private group scraping, access-control bypass or auto-send.
- Keep all original evidence/provenance; distinguish source facts, AI inference, assumptions and unknowns.

## Historical handoff (2026-11-24)

- Branch: `feature/discovery-run-persistence`; latest app implementation commit: [5099d19](https://github.com/ysfclc/Trader/commit/5099d1929176f8b2c380e9747102d662246eee3a).
- Enforced discovery provenance: findings are persisted transactionally only while the parent discovery run and matching source run are RUNNING; adapter source-ID mismatches fail the source; zero-source requests are rejected before run creation.
- Added regression coverage for cross-source/late findings, adapter source-ID mismatch and empty source selections.
- CI run [45394348906](https://github.com/ysfclc/Trader/actions/runs/45394348906) passed `./gradlew --no-daemon clean testDebugUnitTest assembleDebug lintDebug` in 4m45s. APK signature, persistent certificate fingerprint and exact package/version checks passed.
- Verified APK identity: `com.yusufculcu.trader.debug`, `0.4.0-debug`, versionCode 4. Installable artifact: [14919883504](https://github.com/ysfclc/Trader/actions/runs/45394348906/artifacts/14919883504); test reports: [14920726257](https://github.com/ysfclc/Trader/actions/runs/45394348906/artifacts/14920726257).
- No physical-device update, migration/restore or live-source acceptance is claimed.

## Historical handoffs

Older session-by-session notes are preserved in `docs/STATE_HISTORY.md`.


## Company contact increment (2026-11-25)

- Added company-scoped people/contact records for name, role, email, phone, LinkedIn profile and operator-entered notes. Records can be created, edited and deleted inside a company dossier.
- Added Room v8→v9 migration with company foreign key/cascade and encrypted backup snapshot v6. Older backup snapshots remain readable with an empty contact list.
- Contact validation rejects malformed email and non-LinkedIn profile URLs. Similar names are not merged; aliases and reversible identity review remain future work.
- Implementation committed in [16cec6a](https://github.com/ysfclc/Trader/commit/16cec6a703cc0b1d07c7c7d3c1c9c00f123da72c); CI run [45570394101](https://github.com/ysfclc/Trader/actions/runs/45570394101) verified the buyer-requirement increment and installable artifact. No physical-device acceptance is claimed.


## Buyer requirement storage increment (2026-11-25)

- Added company-scoped BuyerRequirement records with lifecycle state, destination, required-by text, optional currency/budget and operator notes.
- Added normalized line items. Quantity is retained as validated decimal text to avoid binary floating-point loss; target prices are integer minor units.
- Added evidence links constrained to saved sources on the same company dossier.
- Added Room v9→v10 migration and encrypted backup snapshot v7 support. Dossier UI and acceptance tests remain next.


## Buyer requirement dossier UI (2026-11-25)

- Added an EN/PL/TR buyer-requirements section inside each company dossier.
- Operators can create a requirement, add structured product/service line items and link saved dossier sources as evidence. Linking a source does not auto-verify a claim.
- Deleting a requirement cascades its line items and evidence links. Physical-device UX acceptance remains open.

## Buyer requirement UI handoff (2026-11-25)

- Latest app commit: [16cec6a](https://github.com/ysfclc/Trader/commit/16cec6a703cc0b1d07c7c7d3c1c9c00f123da72c).
- Added Room v9→v10 buyer requirement storage: requirement headers, normalized line items, and evidence links constrained to sources in the same company dossier.
- Added encrypted backup snapshot v7 with strict required-array checks, cross-record reference validation and FK-safe restore order.
- Added EN/PL/TR dossier UI to create requirements, add line items and link saved sources; no automatic fact verification or person merge.
- CI run [45570394101](https://github.com/ysfclc/Trader/actions/runs/45570394101) passed build, tests, lint and persistent certificate/package/version verification. Verified identity: `com.yusufculcu.trader.debug`, `0.7.0-debug`, versionCode 7. Installable artifact: [15002260997](https://github.com/ysfclc/Trader/actions/runs/45570394101/artifacts/15002260997).
- Physical-device update, migration/restore and visual acceptance remain open.


## Industrial Sourcing Desk handoff (2026-11-25)

- Documentation alignment CI run [45575324059](https://github.com/ysfclc/Trader/actions/runs/45575324059) passed the same full build, test, lint and signing checks after the product handoff docs were committed.

- Product definition: `docs/PRODUCTS/INDUSTRIAL_SOURCING_DESK.md`.
- Latest implementation commit: [b530d4e](https://github.com/ysfclc/Trader/commit/b530d4e05a347f0ff9c2520eaf6f7a3861058d27).
- Room v10→v11 adds supplier offers, offer line items, offer evidence links and buyer-to-supplier sourcing decisions.
- Encrypted backup snapshot v8 preserves all sourcing records; restore validates company/source/requirement/offer relationships and inserts in FK-safe order.
- The Sourcing destination connects existing buyer requirements to supplier offers, displays deterministic comparisons and allows a human review decision with rationale. Shortlists are shareable through an explicit Android Sharesheet action.
- CI run [45574277490](https://github.com/ysfclc/Trader/actions/runs/45574277490) passed the full clean build, unit tests, lint, package/version and persistent-certificate checks. Verified artifact [15003891645](https://github.com/ysfclc/Trader/actions/runs/45574277490/artifacts/15003891645).
- Device update, real-data workflow, and backup/restore acceptance remain open; CI is not physical-device proof.
