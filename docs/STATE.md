# State Ledger
_Last updated: 2026-11-24 — organization dossier vertical slice implemented; automated CI verification pending_

## Product snapshot
Trader is an independent Android-first, local-first, free-first problem-solving and opportunity-execution system. The product loop is capture → understand outcome/constraints → gather evidence/context → discover options/resources/people → plan → execute bounded actions with permission/approval → verify outcomes → retain learning. It is not a trading-only app and must not force every problem into a lead. Trade, services and careers are initial specialist workflows, not product boundaries. Canonical scope: `docs/PROJECT_LOCK.md`; architecture, data, operating rules, product experience, source ecosystem and local AI designs remain authoritative.

## Current implementation
| Area | State | Verified boundary |
|---|---|---|
| Android identity | 🟡 stable signing configured; device update acceptance open | Package `com.yusufculcu.trader.debug`; next build `0.3.0-debug`, versionCode 3. Dedicated Trader keystore remains configured. CI signature/package verification and install-over-existing device acceptance are tracked separately. |
| Native shell / EN-PL-TR | 🟡 partial | Discover, Inbox, Pipeline, Activity, Browser and AI tabs exist. Intended Discover / Work / Live / Studio workspace remains unfinished. |
| Local data | 🟡 partial | Room schema v8 stores cases, plans, evidence, opportunities, workflow records, proof assets, discovery strategies, discovery runs, source runs/findings, organizations, organization sources and source-linked claims. Encrypted backup snapshot v5 includes these records plus public feed settings; legacy snapshots remain readable. Physical-device migration/restore acceptance open. |
| Continuity / backup | 🟡 partial | In-place updates retain app-private data only when package ID and signing certificate match; physical install-over-existing is still unverified. The earlier one-time signing transition required uninstalling the previous app; no recovery claim is made without an exported .traderbackup. Snapshot v5 includes all current records, including organizations, source snapshots and claims; credentials and WebView cookies remain excluded. Restore uses validation, a Room transaction and compensating source-preference rollback. |
| Browser | 🟡 partial | Trader WebView, app-local cookies/SSO, popups, autofill, file chooser, Google OAuth fallback and visible-page capture/review. User confirmed login works on Redmi; broader device acceptance remains open. |
| Memo handoff | 🟡 partial | Versioned capture receiver and editable review/redaction are in Trader. Memo sender PR #15 remains draft/unmerged; installed Memo app does not yet expose it. |
| Discover / sources | 🟡 first execution slice CI-verified | Objective-driven DuckDuckGo search plus all enabled local RSS/Atom feeds run through one bounded coordinator. Per-source run history/status, timeout isolation, cancellation, cross-source URL dedupe, manual feed checks, browser handoff and save-to-Work as UNREVIEWED evidence. CI run 44998620615: 115 tests passed, 0 failures/errors/skips; build, lint, signature and package/version checks passed. No live-source or physical-device acceptance. |
| Company / people graph | 🟡 first dossier slice implemented | Company dossier list and editor are available inside Pipeline. Users can create a dossier manually or from a Discover result, retain source snapshots, author source-linked claims and explicitly review them as supported, contradicted or rejected. Person records, alias/identity review, structured buyer requirements/supplier offers and explainable ProofAsset fit remain incomplete. |
| Automation / AI | 🟡 foundations | Typed capability registry, workflow approval/recovery rules, LiteRT-LM adapter and Model Hub. Curated source metadata checks and consent-gated immutable candidate downloads with exact-size/SHA-256 verification, cancellation, temporary-file cleanup and no automatic install are implemented; current feature CI is pending. No verified on-device model inference or durable WorkManager executor. |
| Social/community sources | ⚪ not implemented | Reddit OAuth, Bluesky/Mastodon APIs, authorized groups and Facebook browser-assisted research remain future provider-specific adapters. |
| Outreach | ⚪ not implemented | Drafting/approval/handoff contracts remain planned; no autonomous send. |

## Branches and build
- Canonical repository: `ysfclc/Trader`; canonical product specification: `docs/PROJECT_LOCK.md`.
- Current branch: `feature/discovery-run-persistence`; latest commit is the organization dossier vertical-slice implementation recorded in the current handoff.
- Candidate package/version: `com.yusufculcu.trader.debug`, versionCode 3, versionName `0.3.0-debug`.
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

## Organization dossier handoff (2026-11-24)

- Added the first visible company dossier workspace inside Pipeline, preserving the existing Opportunities view as a sibling segment.
- A Discover result can now start a new company dossier. The result is saved as source material only; the operator must confirm the canonical company name and website.
- Company profiles retain name, HTTPS website/domain, country, city, industry and user notes. Sources are immutable snapshots; claims are separate records linked to a source and begin unreviewed.
- Explicit user review can mark a claim supported, contradicted or rejected, and can be reset. No AI-generated or search-derived claim is automatically verified.
- Room schema v7→v8 and encrypted backup snapshot v4→v5 preserve dossier/source/claim relationships. CI verification is pending; device migration and backup/restore acceptance remain open.

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


## Session 19 handoff — immutable candidate model byte verification

**Implemented on branch** `feature/discovery-run-persistence`, latest commit `3e85b2aff2046939caa3384fe0f5977beb806cde`.

- Added a user-confirmed candidate integrity flow for changed upstream model artifacts. It uses the full immutable 40-character source revision, metadata-declared byte count and SHA-256.
- Downloads the complete candidate into an app-private temporary cache file, enforces exact size while hashing the bytes, and deletes the temporary file after success, failure or cancellation. A 150 MB free-space reserve is required; candidate verification is capped at 8 GB.
- Supports cancellation by disconnecting the active HTTP request, serializes candidate verification to one active download, throttles progress updates to 8 MB intervals and treats cleanup failure as a failed verification rather than success.
- Enforces HTTPS after redirects and requests identity content encoding so the measured byte count applies to the artifact stream.
- The Model Hub binds a displayed byte-verification result to the candidate's exact source revision. A result from an older candidate cannot hide the action for a newer candidate.
- Source metadata policy distinguishes an absent artifact (`ARTIFACT_MISSING`) from a present artifact with missing/malformed integrity metadata (`ARTIFACT_UNVERIFIED`).
- A successful byte check proves only that downloaded bytes match the digest declared by upstream metadata. It does not approve the model, verify license/usage rights, prove LiteRT-LM compatibility, establish quality/device performance, install, activate or repin a candidate.

**CI status at handoff**
- Latest commit: `3e85b2aff2046939caa3384fe0f5977beb806cde`.
- Android CI run [45052932934](https://github.com/ysfclc/Trader/actions/runs/45052932934) was queued/in progress at handoff; no pass or failure is claimed yet.
- Earlier run 45052696109 for parent commit 3db0c69 was cancelled by branch concurrency after the follow-up hardening commit. It is not a failed build.
- No installable APK is claimed from this checkpoint. The previous green build (run 45006357134) uploaded only the explicitly non-installable CI APK because stable signing secrets were not configured at that time.

**Still open**
1. Confirm latest CI run 45052932934 passes tests, lint, APK package/signature checks and inspect whether the persistent installable artifact step actually ran.
2. On the Redmi, exercise a real candidate check only after an upstream change exists; record device/runtime/storage/network evidence. No physical-device model download or inference is claimed.
3. Complete the human model-card/license review, LiteRT-LM compatibility and structured-output quality testing, plus target-device memory/latency/thermal checks before any catalogue repinning.
4. Continue T7.2 Context Engine and T7.3 structured-output validation only after the local model lifecycle is accepted; preserve deterministic validation and explicit fact provenance.

**Risks / landmines**
- Candidate byte verification downloads the entire model (up to 8 GB) to temporary app cache; the user explicitly confirms the transfer and must have the candidate size plus 150 MB reserve.
- A byte-hash match is not publisher trust or release approval. Never promote a candidate automatically.
- If cleanup fails, the UI reports failure and requires app-cache cleanup; it does not claim the candidate is safely discarded.
- Stable APK signing secrets must be present in GitHub Actions before CI can publish an installable update. Never put the private keystore or password in chat or commit either to Git.


## Session 20 handoff — discovery strategy prompt construction

**Implemented:** `TraderDiscoveryStrategyPromptBuilder` on `feature/discovery-run-persistence`, commit `d0406ae8ffcdee5bbff96cf39294e1c4a56ef1a0`.

- Bounded task, context and proof asset sections; only caller-supplied registered source-adapter IDs are exposed.
- Prompt explicitly treats captured context and proof content as untrusted data, prohibits following embedded instructions, and prohibits contacting people or executing workflows.
- Requires a strict schema and limits; no IDs or timestamps are model-authored.
- Unit tests added for prompt assembly, untrusted context retention, allowed adapter listing, size bounds and empty adapter rejection.
- This is prompt construction only; it is not yet wired to an inference action or user-facing strategy review/save flow.
- CI run 45058592503 for commit d0406ae was pending when the checkpoint was recorded. No build/test pass claimed.


## Session 21 handoff — local discovery strategy workshop

Implemented on `feature/discovery-run-persistence`:
- `TraderDiscoveryStrategyDraftGenerator` calls the active pinned model through the local inference adapter, assembles the bounded prompt, and validates generated JSON against the current proof asset and registered adapters.
- Context rendering uses the prompt builder's 18,000-character ceiling.
- Model Hub now includes a Discovery Strategy Workshop: select a problem; create/select a user-owned proof asset; load that problem's plan and evidence; generate locally; edit the first problem signature and first query; explicitly approve and save through `DiscoveryStrategyRepository`.
- Strategy generation does not run Discover, send communications, or create contacts. Other draft fields remain unchanged in this first review UI; full field-by-field editing remains future work.
- Generator rethrows coroutine cancellation rather than converting cancellation into a normal inference error.
- CI run 45058633990 failed three parser acceptance tests (valid JSON, fenced JSON, adapter error detail). Diagnostic assertions were added; later runs were superseded before completing. No build/test success or installable APK is claimed.


## Session 22 handoff — complete discovery-strategy review surface

**Implemented on** `feature/discovery-run-persistence`, commit `cbc12eb03ca4479558029e644499cce3a810a2f1`.

- Replaced the partial first-signature/first-query editor with full review of every generated problem signature: problem statement, observable signals, buyer roles, search phrases and must-have conditions.
- Added editing for every target constraint: countries, regions, languages, organization types, company size and excluded domains.
- Exposed all query fields: registered source adapter ID, query text, language, region and per-query result limit; users can add and remove queries.
- Exposed editable disqualifiers and the per-source result cap. Signature and query collections support add/remove; list editors support adding/removing individual lines.
- Approval now runs deterministic strategy validation and checks each query against the currently registered adapter allowlist before materialization and repository save. No discovery run or communication is triggered by approval.
- Added a round-trip regression test for multiple signatures, all target constraints, multiple query fields, disqualifiers and limits through draft materialization and the persistence codec.

**Verification status**
- CI run [45091643997](https://github.com/ysfclc/Trader/actions/runs/45091643997) was in progress when this handoff was written. Do not claim this editor change passes until the run completes.
- Prior commit [6ba28d1](https://github.com/ysfclc/Trader/commit/6ba28d1c3780f521b170afc45398f02d5f11103c) is green: run [45064444679](https://github.com/ysfclc/Trader/actions/runs/45064444679), 122 tests passed, zero failures/errors/skips; build, lint and APK package/signature checks passed.
- No physical-device UI test or stable-signed APK is claimed. The persistent signing key bundle is not available in this session and Actions signing secrets remain unconfigured; CI-only APKs must not be treated as update-compatible installs.

**Next**
1. Finish CI verification of the full review editor and round-trip test.
2. Add ProofAsset claim evidence selection and verification-state editing; keep unverified claims visibly unverified.
3. Connect a reviewed saved strategy to the discovery coordinator and persist its strategy ID on the run.
4. Continue normalized Organization/Person records, source-linked dossiers and explainable proof-to-organization fit.
5. Obtain the existing private Trader signing bundle and configure the two repository Actions secrets; then publish a stable-signed APK and perform backup-first Redmi installation/update acceptance.


## 2026-11-24 execution note

The prior findings-persistence implementation was inadvertently reverted while the run-history UI still referenced it, causing CI compilation failure. Repair restores the Room v7 findings entity/DAO and 6→7 migration, persists each accepted finding from the coordinator, registers the migration, and includes findings in encrypted backup format v4. CI verification is pending; device migration/restore acceptance remains open.


## Session 23 handoff — signing transition, CI recovery and product reset (2026-11-24)

**Verified in repository/CI**
- Stable Trader signing secrets are now present in Actions. Run 45345752081 passed the full test/build/lint/signature/package pipeline and uploaded installable artifact trader-installable-apk (artifact ID 14898566003).
- The failed predecessor run 45344874970 exposed a missing MIGRATION_6_7 registration in a legacy migration test. Commit a4950eb17a606b9f89520ec4f78b6fcb7e4f2505 fixed it; the subsequent run passed.
- Discovery findings now persist under their originating run and are included in encrypted backup snapshot v4. This is infrastructure, not a user-visible feature milestone.

**User-impact incident**
- The earlier installed APK was from the ephemeral CI-signing period. Android cannot update an APK signed by that temporary key with the new persistent Trader key, even though the package ID is unchanged. The user had to uninstall the old app. The migration should have been gated on a confirmed encrypted export; that safeguard was not enforced. If no .traderbackup was exported before uninstall, the old app-private data is not recoverable from the new install.
- From this point forward, stable signing is fixed. All subsequent updates must preserve com.yusufculcu.trader.debug, increment versionCode, use the same persistent certificate and be tested as in-place updates. Never advise uninstalling as a routine fix.

**Product direction reset**
- Stop treating green CI runs and invisible persistence work as user-facing progress. Next implementation milestone is the Organization dossier vertical slice: source-to-organization identity, evidence-backed company profile, buyer requirements/supplier capabilities, and explainable fit with unknowns/contradictions.
- Each milestone must end with a visible, usable change in the installed app, a short before/after description, and a signed APK. Infrastructure-only changes are allowed only when they directly unblock or protect that slice.
- No physical-device acceptance is claimed by CI.
