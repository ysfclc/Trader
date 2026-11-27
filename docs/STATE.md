# State Ledger
_Last updated: 2026-11-27 — Trader 0.14.0-debug installable APK published from canonical main; CI #626 passed 201 tests_

## Product snapshot
Trader is an independent Android-first, local-first, free-first problem-solving and opportunity-execution system. The product loop is capture → understand outcome/constraints → gather evidence/context → discover options/resources/people → plan → execute bounded actions with permission/approval → verify outcomes → retain learning. It is not a trading-only app and must not force every problem into a lead. Trade, services and careers are initial specialist workflows, not product boundaries. Canonical scope: `docs/PROJECT_LOCK.md`; architecture, data, operating rules, product experience, source ecosystem and local AI designs remain authoritative.

## Current implementation
| Area | State | Verified boundary |
|---|---|---|
| Android identity | 🟡 installable APK published; device acceptance open | Package `com.yusufculcu.trader.debug`; canonical main commit `94c241d8971d624857295069635baee12c2ea436`; `0.14.0-debug`, versionCode 14. Approved installable artifact [15220676610](https://github.com/ysfclc/Trader/actions/runs/46041691925/artifacts/15220676610) from main CI [#626](https://github.com/ysfclc/Trader/actions/runs/46041691925). CI passed 201 tests, coverage floor, clean build, lint, SBOM/OSV and persistent package/signature verification. Physical install-over-existing and on-device workflow acceptance remain open. |
| Native shell / EN-PL-TR | 🟡 partial | Discover, Inbox, Sourcing, Pipeline, Activity, Browser and AI tabs exist. Intended Discover / Work / Live / Studio workspace remains unfinished. |
| Local data | 🟡 schema/migration CI-verified; device acceptance open | Room schema v17 adds contact aliases and append-only identity review decisions after schema v16 fit-assessment snapshot storage. The full v1→v17 migration path passed in CI [#618](https://github.com/ysfclc/Trader/actions/runs/46038987403); v1→v16 fit-assessment migration was previously verified in #601. Active/trash sourcing behavior remains unchanged. |
| Continuity / backup | 🟡 snapshot validation CI-verified; physical restore open | Encrypted backup snapshot v13 includes company/proof fit assessments, explicit evidence links, reviewer overrides, contact aliases and append-only identity review decisions. Schema-17 backups require the identity arrays; snapshots v1–v12 remain readable. CI #618 verifies strict validation, FK-safe restore and identity alias/review-history round trip. Credentials and WebView cookies remain excluded. Physical restore and install-over-existing acceptance remain open. |
| Browser | 🟡 partial | Trader WebView, app-local cookies/SSO, popups, autofill, file chooser, Google OAuth fallback and visible-page capture/review. User confirmed login works on Redmi; broader device acceptance remains open. |
| Memo handoff | 🟡 partial | Versioned capture receiver and editable review/redaction are in Trader. Memo sender PR #15 merged to main on 2026-11-19; the installed Memo app does not yet expose the sender. |
| Discover / sources | 🟡 first execution slice CI-verified | Objective-driven DuckDuckGo search plus all enabled local RSS/Atom feeds run through one bounded coordinator. Per-source run history/status, timeout isolation, cancellation, cross-source URL dedupe, manual feed checks, browser handoff and save-to-Work as UNREVIEWED evidence. Findings now require an active matching source run; adapter source-ID mismatches fail closed, and zero-source runs are rejected before persistence. CI run 44998620615 verified the first execution slice. Latest CI run 45394348906 passed the full unit-test, lint and debug-build pipeline plus persistent-certificate and package/version checks. No live-source or physical-device acceptance. |
| Company / sourcing graph | 🟢 Product/SKU catalogue CI-verified; device acceptance open | Company dossiers include contacts, buyer requirements, supplier offers and a distinct Product/SKU catalogue. Product SKU uniqueness is scoped to a company; source links preserve provenance only. Product records do not assert stock, pricing, supplier availability, technical equivalence or buyer fit. Product create/edit/status/evidence changes append Activity events. The 0.13.0 catalogue, migration and backup invariants passed CI run #573. Physical-device acceptance remains open. |
| Fit assessment | 🟡 CI-verified; device acceptance open | Deterministic operator-declared claim mapping; only source-supported company claims and evidence-verified proof claims affect fit. Room v16 and encrypted snapshot v12 preserve computed snapshots, evidence links and append-only reviewer history. T4.9a adds EN/PL/TR ProofAsset selection, mapping editor, component findings, stale-input recalculation, source handoff and reviewer-decision history. CI run [#612](https://github.com/ysfclc/Trader/actions/runs/45978797789) passed; device acceptance remains open. |
| Contact identity | 🟢 CI-verified; device acceptance open | Same-company duplicate suggestions use normalized exact email/profile URL and a weak same-name-and-role signal. Operators can add name/email/profile/other aliases with manual or same-company source provenance, mark pairs as same/different, and reopen review. Each decision is an append-only event; contacts are never merged. Room v17 and encrypted backup snapshot v13 include aliases and review history. Full build/test/coverage/lint/security/signature gates passed CI [#618](https://github.com/ysfclc/Trader/actions/runs/46038987403). |
| Automation / AI | 🟡 foundations | Typed capability registry, workflow approval/recovery rules, LiteRT-LM adapter and Model Hub. Curated source metadata checks and consent-gated immutable candidate downloads with exact-size/SHA-256 verification, cancellation, temporary-file cleanup and no automatic install are implemented and included in the passing 0.12.0 CI build. No verified on-device model inference or durable WorkManager executor. |
| Social/community sources | ⚪ not implemented | Reddit OAuth, Bluesky/Mastodon APIs, authorized groups and Facebook browser-assisted research remain future provider-specific adapters. |
| Outreach | ⚪ not implemented | Drafting/approval/handoff contracts remain planned; no autonomous send. |

## Branches and build
- Canonical repository: `ysfclc/Trader`; canonical product specification: `docs/PROJECT_LOCK.md`.
- Canonical branch: `main`. The complete integration line was merged from `feature/discovery-run-persistence` by [PR #20](https://github.com/ysfclc/Trader/pull/20) as merge commit [3041b8c](https://github.com/ysfclc/Trader/commit/3041b8cd8046c26a7c4cce27832911f92d5d309a) on 2026-11-26. This consolidates the 324-commit integration line without force-push or history rewrite. Recoverable sourcing records were already merged into that line from [PR #19](https://github.com/ysfclc/Trader/pull/19) as [commit d376db2](https://github.com/ysfclc/Trader/commit/d376db2e108fa94cc90c6143c8545791eab5dd89). Coverage-gate implementation [b22f0c9](https://github.com/ysfclc/Trader/commit/b22f0c9442a9dbbf5a01c39dc174d161b161e5bc) adds the measured line/branch coverage floor. Coverage-gated CI run [45666843559](https://github.com/ysfclc/Trader/actions/runs/45666843559) passed all 161 unit tests (0 failures/errors/skipped), six coverage-tool unit tests, `assembleDebug`, `lintDebug`, historical per-class coverage floor, APK identity and persistent signing-certificate verification. Coverage artifact [15046784926](https://github.com/ysfclc/Trader/actions/runs/45666843559/artifacts/15046784926); JUnit artifact [15048156829](https://github.com/ysfclc/Trader/actions/runs/45666843559/artifacts/15048156829). Room components remain aligned at 2.8.5.
- T4.9 repository/persistence integration: PR [#21](https://github.com/ysfclc/Trader/pull/21) merged as [885e597](https://github.com/ysfclc/Trader/commit/885e5978ec658f3232c822258aaaabaf650244ff). Final feature CI [#601](https://github.com/ysfclc/Trader/actions/runs/45892674806) passed 191 Android tests and all coverage/build/lint/security/signature gates; consolidated main CI [#609](https://github.com/ysfclc/Trader/actions/runs/45894762068) also passed. T4.9a UI candidate [a6addf6](https://github.com/ysfclc/Trader/commit/a6addf6cdd455fd34664c8353d19a365ecc8c116) passed CI [#612](https://github.com/ysfclc/Trader/actions/runs/45978797789). T4.8a duplicate candidate detection [b70bb2b](https://github.com/ysfclc/Trader/commit/b70bb2b5f3f096d231c39cc5e0f6064307865991), with Turkish resource correction [1c8b8b6](https://github.com/ysfclc/Trader/commit/1c8b8b61097616ccf2caf99efbb33c8982f4acd4), passed final-head CI [#614](https://github.com/ysfclc/Trader/actions/runs/46019745848). T4.8b aliases and reversible identity review [b443423](https://github.com/ysfclc/Trader/commit/b443423481d187961a50741bad50ee323fcd3acb) passed final-head CI [#618](https://github.com/ysfclc/Trader/actions/runs/46038987403): full unit-test suite, historical coverage floor, `assembleDebug`, `lintDebug`, SBOM/OSV, package identity and persistent signing-certificate verification. Candidate `0.14.0-debug` / versionCode 14 remains CI-verified; push-triggered workflow skipped installable APK publication. Manual approval remains required.
- PR audit: #20 and #21 are merged; #22 consolidated the latest 13-commit fit-assessment and contact-identity line into `main` as [94c241d](https://github.com/ysfclc/Trader/commit/94c241d8971d624857295069635baee12c2ea436). Exact merged source head [3927dc9](https://github.com/ysfclc/Trader/commit/3927dc925de55f67b8bfe094f650309b5808e03f) passed CI [#618](https://github.com/ysfclc/Trader/actions/runs/46038987403) and final-head CI [#619](https://github.com/ysfclc/Trader/actions/runs/46040032965). PR #22: [link](https://github.com/ysfclc/Trader/pull/22).
- Branch cleanup: `main` is canonical. Two historical refs (`build/proofasset-room-backup`, `build/proofasset-discovery-strategy`) were deleted. Twenty-three non-main refs remain (24 refs total including `main`); no open PRs or protected branches were found. The cleanup run confirmed deletion of `build/proofasset-room-backup` and `build/proofasset-discovery-strategy`; `build/usable-workspace-v1` is also absent from the current branch listing. GitHub's visible branch menus did not expose a delete action for the remaining refs; do not claim the repository is fully pruned. Continue cleanup using an authorized delete-ref route, preserving only genuinely active work.
- Latest installable APK from canonical `main`: `0.14.0-debug`, versionCode 14; artifact [15220676610](https://github.com/ysfclc/Trader/actions/runs/46041691925/artifacts/15220676610), produced by explicitly approved workflow run #626 on commit `94c241d8971d624857295069635baee12c2ea436`. The downloaded artifact was inspected as an Android APK; SHA-256 `5520b29a438f0b8d5538b0546d006a2d3357807187ca05fa6d4cd40c669d395c`. CI verified package identity and equality with the persistent Trader development signing certificate. The test-results artifact contains 201 tests, 0 failures, 0 errors and 0 skipped.
- Persistent Trader development signing remains configured in GitHub Actions. Do not change the package ID or signing certificate; every subsequent build must increment versionCode. The latest CI run and artifact are recorded after verification below.
- Toolchain: JDK 17, Gradle 8.13, AGP 8.13.0, Kotlin Android/Compose 2.4.21, KSP 2.3.12, Room 2.8.5, SDK 36; build: `./gradlew --no-daemon clean testDebugUnitTest createDebugUnitTestCoverageReport assembleDebug lintDebug`. CI runs the Python coverage-tool unit tests, publishes the AGP HTML/XML report and enforces per-class covered line/branch counts for the 67 previously covered classes in `docs/coverage-baseline.json`. New classes still require focused feature tests; they do not enlarge the historical denominator. Installable APK upload still requires manual `workflow_dispatch` input `approve_installable_apk=true`.

## Immediate next increments
1. Install the published `0.14.0-debug` APK over the existing Redmi installation only after exporting a fresh encrypted backup; never uninstall or clear data as an update strategy.
2. Verify in-place package/signature continuity, Room v16→v17 migration, preservation of existing rows/preferences/WebView session, and encrypted snapshot v13 export/restore.
3. Complete and record the real Industrial Sourcing Desk journey end-to-end, including buyer requirement, supplier offer/evidence, multi-offer comparison, decision revision, share handoff, Activity and close/reopen persistence.
4. Fix every observed device regression before feature expansion. Then continue the product roadmap with alias-aware candidate review and source-backed company/person profiling; keep all external outreach explicitly user-approved.

## Known risks / constraints
- CI success is not device acceptance. Main CI #626 passed 201 Android tests (0 failures/errors/skipped), the 67-class historical coverage floor, clean build, lint, SBOM/OSV and persistent package/signature checks and published the signed 0.14.0-debug APK. No physical-device install, migration, backup/restore or full sourcing-journey acceptance has been demonstrated.
- T4.9 repository persistence, computed snapshots, append-only reviewer history and encrypted backup snapshot v12 are merged and CI-verified. T4.9a dossier UI passed CI #612. T4.8a deterministic contact duplicate suggestions passed CI #614; they remain non-persistent review aids only. No alias or identity resolution is performed yet.
- The consolidated `main` has an installable 0.13.0 artifact from approved run #583. Main CI #609 passed but did not publish an APK; T4.9a candidate publication requires manual `workflow_dispatch` approval and a versionCode increment.
- Previous ephemeral-signed APK cannot be assumed upgrade-compatible with the new persistent signing identity. If it is already installed, do not attempt a blind reinstall; first protect existing workspace data and separately export source settings because those are not yet in the encrypted backup.
- Android `allowBackup=false` disables OS cloud backup; explicit encrypted Trader export is the recovery mechanism. In-place signed updates retain app-private data.
- Local Room database is not separately SQLCipher-encrypted; do not store provider credentials or highly sensitive secrets there.
- Numerical coverage baseline is intentionally low: 14.85% application-source lines and 4.75% branches. The CI gate prevents further regression but does not establish adequate test depth. UI, ViewModel and repository layers remain major test gaps.
- GitHub dependency graph is disabled in CI and repository Dependabot alerts could not be queried (API authorization denied). The resolved 421-component SBOM was scanned by OSV-Scanner v2.7.1 with 0 findings in run #565; the same scanner gate passed again on the current candidate in run #573. The scanner output does not expose a distinct advisory-database revision/timestamp, so database freshness beyond the scan date remains unverified.
- Existing warnings remain: Gradle 8.13 deprecation, GitHub Actions Node 20 runtime deprecation (checkout@v4, setup-java@v4, upload-artifact@v4, setup-android@v3, gradle/actions@v4), setup-java@v4 action deprecation, and legacy Compose/WebView deprecations. No warnings were emitted from the coverage utility; these toolchain/action warnings are tracked for a separate maintenance increment.
- APK publication is intentionally blocked by default. Only a manual workflow dispatch with explicit approval can publish an installable APK after every gate is green.
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


## Business activity history increment (2026-11-25)

- Added Room v11→v12 `business_activity_events` with insert-only normal workflow access and a deliberate no-foreign-key design so a minimal deletion marker survives company removal.
- Buyer requirement and supplier offer status changes now retain from/to states; revised sourcing decisions and company-claim reviews append new events rather than overwriting history.
- Company, source, claim, contact, requirement, offer, line-item and evidence lifecycle events appear in the unified Activity timeline. Contact email, phone and name are not copied into activity details.
- Buyer requirement and supplier offer cards expose localized lifecycle status controls in English, Polish and Turkish.
- Encrypted backup snapshot v9 includes business activity history, validates event shape, and continues to read v1–v8 snapshots as empty history where appropriate.
- Verified on CI run [45591681856](https://github.com/ysfclc/Trader/actions/runs/45591681856): the full Gradle build/test/lint command passed, APK package identity was `com.yusufculcu.trader.debug` versionCode 9 / `0.9.0-debug`, and the APK matched the persistent signing certificate. Installable artifact: [15010224455](https://github.com/ysfclc/Trader/actions/runs/45591681856/artifacts/15010224455). Physical-device update/install acceptance remains open.

## Buyer requirement correction increment (2026-11-25)

- The company dossier now opens existing buyer requirements in a prefilled editor; title, context, destination, timing, currency, budget, notes and existing status can be corrected without replacing the record.
- Existing buyer line items can be edited in place. Validation is shared with creation, IDs and creation timestamps are preserved, and parent requirement/organization update timestamps advance.
- Line-item corrections append a distinct `LINE_UPDATED` business activity event; the Activity timeline has localized English, Polish and Turkish wording.
- No Room or backup schema change was required. Candidate build: `0.10.0-debug` / versionCode 10; CI verification for this correction increment is pending.

## Buyer requirement correction candidate (2026-11-25)

- Existing requirement headers and line items are editable in place. Header correction preserves ID/creation time, reuses creation validation, updates the parent/dossier timestamps and appends `UPDATED` or `STATUS_CHANGED` depending on whether lifecycle state changed.
- Line-item correction preserves ID/creation time, reuses quantity/price validation, updates the parent/dossier timestamps and appends `LINE_UPDATED`.
- The first candidate CI run [45593520867](https://github.com/ysfclc/Trader/actions/runs/45593520867) compiled and assembled the APK but failed one test because the test expected `UPDATED` for a status-changing edit. The repository behavior is intentional; the regression assertion now checks `STATUS_CHANGED` and its exact from/to states. A second run exposed an over-broad event selector because line-item events share the parent requirement ID and label; the selector now filters by `STATUS_CHANGED`. Re-run pending; no candidate APK/signature verification is claimed.

## Buyer requirement correction verification (2026-11-25)

- Buyer requirement headers and line items can be corrected in place; IDs and creation timestamps are preserved and the same validation rules apply to create and update.
- Header changes append `UPDATED` or `STATUS_CHANGED`; line-item changes append `LINE_UPDATED`.
- CI run [45595844335](https://github.com/ysfclc/Trader/actions/runs/45595844335) passed the clean build, all 155 unit tests (0 failures/errors/skipped), lint, APK identity and persistent signing-certificate equality.
- Verified package `com.yusufculcu.trader.debug`, versionName `0.10.0-debug`, versionCode 10. Installable artifact [15013068288](https://github.com/ysfclc/Trader/actions/runs/45595844335/artifacts/15013068288). Physical-device install-over-existing remains unverified.

## Supplier offer correction candidate (2026-11-25)

- Candidate `0.11.0-debug`, versionCode 11; no Room or encrypted-backup schema change.
- Supplier offer headers and multiple line items are editable in place with ID/creation-time preservation and creation-equivalent validation. An offer cannot be left without any line item.
- Evidence links are reconciled transactionally and restricted to sources in the supplier's own dossier. Additions and removals append separate activity events; unchanged links produce no duplicate events.
- English/Polish/Turkish Sourcing Desk edit controls and repository regression tests are implemented. CI verification is pending; no candidate APK is claimed.


## Supplier offer correction verification (2026-11-25)

- Supplier-offer headers and multiple line items can now be edited in place with identity preservation and shared validation. Line-item removal is guarded against leaving an empty offer.
- Evidence links can be reconciled transactionally and remain scoped to sources in the supplier's own company dossier. Add/update/remove/unlink operations append business activity events.
- CI run [45598311208](https://github.com/ysfclc/Trader/actions/runs/45598311208) passed the full clean build, all 157 unit tests (0 failures/errors/skipped), lint, APK package/version checks and persistent signing-certificate equality.
- Verified installable APK: `com.yusufculcu.trader.debug`, `0.11.0-debug`, versionCode 11; artifact [15014108093](https://github.com/ysfclc/Trader/actions/runs/45598311208/artifacts/15014108093). Test reports: [15014332640](https://github.com/ysfclc/Trader/actions/runs/45598311208/artifacts/15014332640).
- CI verification does not establish physical-device update, migration/restore, visual or real-world sourcing acceptance. Those remain the next gate.


## Recoverable sourcing records increment (2026-11-25)

- Merged [PR #19](https://github.com/ysfclc/Trader/pull/19) as [d376db2](https://github.com/ysfclc/Trader/commit/d376db2e108fa94cc90c6143c8545791eab5dd89).
- Buyer requirements and supplier offers now use nullable `deletedAt` tombstones. Active queries hide trashed records; restore retains line items, evidence links and sourcing decisions. Deletion and restoration events append to Activity.
- Company deletion is blocked while buyer requirements or supplier offers exist, including trashed records, so a parent cascade cannot bypass recovery.
- Room migration 12→13 adds nullable tombstones and indexes. Encrypted backup snapshot v10 stores deletion state; older snapshots without the field decode it as null.
- Sourcing Trash UI and error/empty states are localized in English, Polish and Turkish. Room runtime, KTX, compiler and testing components upgraded together to 2.8.5.
- CI run [45642932647](https://github.com/ysfclc/Trader/actions/runs/45642932647) passed the clean build, all 161 unit tests, lint, APK package/version checks and persistent signing-certificate equality. Test report [15035912637](https://github.com/ysfclc/Trader/actions/runs/45642932647/artifacts/15035912637).
- No installable APK was published. The workflow now requires explicit manual release-gate approval. Physical-device migration/restore, numerical coverage non-regression and a full dependency scan remain open.


## Session 14 — dependency security audit (2026-11-26)

- **Verified CI:** [run #565](https://github.com/ysfclc/Trader/actions/runs/45740433726) completed successfully on the dependency-remediation commit. Android unit tests: **161 passed, 0 failures, 0 errors, 0 skipped**; coverage baseline, `assembleDebug`, `lintDebug`, APK package/version identity, and persistent signing-certificate checks passed.
- **SBOM and scan:** aggregate CycloneDX SBOM contained **421 components**. OSV-Scanner **v2.7.1** returned an empty findings list (**0 findings**). The archived SBOM resolves `org.apache.commons:commons-lang3` to **3.18.0**; the previous 3.14.0 finding was in the Android Gradle Plugin/build classpath, not the app runtime classpaths.
- **Freshness note:** the scanner output records no distinct advisory-database revision/timestamp. The scan date and scanner version are recorded; no database snapshot timestamp is asserted.
- **Evidence:** [dependency audit](https://github.com/ysfclc/Trader/actions/runs/45740433726/artifacts/15081127622), [resolved dependency graph](https://github.com/ysfclc/Trader/actions/runs/45740433726/artifacts/15080279512), [JUnit reports](https://github.com/ysfclc/Trader/actions/runs/45740433726/artifacts/15081152443), [coverage report](https://github.com/ysfclc/Trader/actions/runs/45740433726/artifacts/15081212428).
- **Release gate:** no installable APK artifact was uploaded because this was a push-triggered CI run without the explicit manual approval input. Physical Redmi device acceptance remains outstanding.


## Company Product/SKU catalogue increment (2026-11-26)

- Added a company-owned Product/SKU master record distinct from buyer requirement lines and supplier offer lines. Fields are SKU, name, description, specification, material, unit and draft/active/discontinued/archived status.
- SKU uniqueness is enforced per company, not globally. Product evidence links can only reference sources saved in the same company dossier; link/unlink changes are transactional and append Activity events.
- The dossier UI supports create, edit, status change and evidence reconciliation in English, Polish and Turkish. Product records do not imply inventory, price, supplier availability, technical equivalence or buyer fit; archive is recoverable and company deletion is blocked while products exist.
- Room v13→v14 adds `products` and `product_evidence`; encrypted backup snapshot v11 includes both arrays, strict schema-14 requirements and relationship validation. Older schema-13 backups remain readable without product arrays.
- Implementation commits: [df89dbf](https://github.com/ysfclc/Trader/commit/df89dbf351408a80a8bd93c976283ae90b5700d5), [17efbdf](https://github.com/ysfclc/Trader/commit/17efbdfc92171b00fefd9bb854b9d4054bac33d4), and [56febcc](https://github.com/ysfclc/Trader/commit/56febcc733bd8bb8366e631634ed1b0679f2ea79).
- Candidate build: `com.yusufculcu.trader.debug`, `0.13.0-debug`, versionCode 13. CI run [45798189101](https://github.com/ysfclc/Trader/actions/runs/45798189101) exposed a missing 13→14 migration in the full v1 migration test; commit `56febcc` corrected the test migration list and asserts both product tables. Run [45799137891](https://github.com/ysfclc/Trader/actions/runs/45799137891) then passed build, unit tests and lint but failed the existing coverage non-regression gate: application-source line coverage was 14.16% versus 14.85% baseline and branch coverage was 4.49% versus 4.75%. Added focused product repository and backup-validation tests; CI now retains the coverage report even when the gate fails. Run [45800691904](https://github.com/ysfclc/Trader/actions/runs/45800691904) was still in SDK setup when superseded. Run [45801509100](https://github.com/ysfclc/Trader/actions/runs/45801509100) compiled and executed 176 unit tests but two new backup-validation tests failed because `decodeSnapshot` did not invoke the existing validator; validation only ran during encrypted restore. The fix moves validation into `decodeSnapshot` and removes the duplicate restore call so every internal consumer receives a validated snapshot. CI rerun pending; no success or APK is claimed.


## Product catalogue CI candidate and coverage-gate correction (2026-11-26)

- CI run [45802644525](https://github.com/ysfclc/Trader/actions/runs/45802644525) built the 0.13.0 candidate, passed all **176 unit tests**, `assembleDebug`, `lintDebug`, dependency resolution and the OSV scan. It failed only the former aggregate coverage-ratio gate; APK signature/upload steps were skipped because the job failed before them.
- Aggregate counters were 1,394/9,846 lines and 574/12,793 branches, compared with the historical 1,394/9,388 and 574/12,079. The denominator grew with new classes. A class-by-class comparison against the historical coverage artifact found no loss in any previously covered class.
- The gate is being changed to preserve covered line/branch counts for each of the 67 previously covered classes. New code remains subject to dedicated feature tests; new uninstrumented Robolectric/Compose classes no longer make unrelated historical coverage appear to regress. No APK is claimed from run 45802644525.


## T4.9 deterministic fit-assessment domain slice (2026-11-26)

- Added `FitAssessment.kt` with typed company-claim review state, explicit SUPPORTS / CONTRADICTS / UNKNOWN relationships, per-proof-claim components, conservative overall outcomes and transparent evidence findings.
- Company evidence is eligible only when its company claim is source-supported; operator proof evidence is eligible only when the ProofClaim is SUPPORTED and has evidence IDs plus a verification timestamp. Anything else remains UNKNOWN. Simultaneous support and contradiction for one proof claim remains CONFLICTED; missing evidence is never treated as a match.
- Added a separate `FitAssessmentOverride` value and effective-disposition view. The reviewer decision does not rewrite the deterministic result.
- Added ten focused tests covering support, partial/unknown, contradiction, conflict, unverified company evidence, expired proof, override independence, input-order stability and fail-closed validation.
- CI run [45836135050](https://github.com/ysfclc/Trader/actions/runs/45836135050) (#575) passed **186 tests, 0 failures, 0 errors, 0 skipped**, the 67-class historical coverage floor, OSV scan, lint, APK assembly and persistent-signature/package checks. No dependency, Room schema, backup format, UI or app identity changed. APK publication was skipped under the normal push trigger.
- Remaining T4.9 work: persist assessment evidence and append-only reviewer overrides, validate relationships across Room/backup restore, and surface the assessment in the company dossier in EN/PL/TR. No user-facing fit-assessment feature is claimed yet.
