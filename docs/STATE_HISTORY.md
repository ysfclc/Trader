# Trader state history

Historical session handoffs retained from the former state ledger. Current authoritative status is in `docs/STATE.md`.

## Session 12 handoff — experimental public-search adapter

**Done this session**
- Added a bounded user-triggered DuckDuckGo HTML search adapter with query/result bounds, request timeout, response-byte cap, HTTPS-only external result URLs, redirect resolution, URL deduplication, retrieval timestamps and parser field limits.
- Added parser/query tests for URL encoding, result extraction, snippet normalization, redirect handling, unsafe links, deduplication and truncation.
- Updated task, coverage, risk, decision and changelog documentation. PR #18 is open against the browser/discovery foundation branch; it is not merged.

**State changed**
- `app/src/main/java/com/yusufculcu/trader/discovery/DuckDuckGoHtmlSearch.kt`
- `app/src/test/java/com/yusufculcu/trader/discovery/DuckDuckGoHtmlSearchTest.kt`
- `docs/TASKS.md`, `docs/COVERAGE.md`, `docs/RISKS.md`, `docs/DECISIONS.md`, `CHANGELOG.md`, `docs/STATE.md`

**Verified**
- Build: pending GitHub Actions run 379; local build unavailable because this runtime cannot resolve GitHub.
- Tests: pending run 379; no local test execution.
- CI validation branch: `build/public-search-adapter` uses the same adapter source and tests as PR #18 head (source blob SHA verified equal). Run 379 is still in progress at handoff.

**Not done / next**
1. Get a clean CI result and fix any compile/test/lint failures.
2. Add user-triggered Discover query UI and connect the adapter, then persist deduplicated candidate/source records with provenance.
3. Validate live source access and behavior on device; do not label snippets verified.

**Open questions**
- DuckDuckGo HTML access policy/terms and long-term markup stability need review before promoting this adapter beyond experimental use.

**Risks / landmines**
- This adapter is not wired into Discover UI or persistent candidate dossiers; it is not a complete discovery workflow.
- Search snippets are unverified source claims and may be incomplete; do not score or qualify a company from snippets alone.
- No result-page crawling, authentication, CAPTCHA/challenge solving or access-control bypass is implemented.
- No device acceptance or real-source test has been recorded.

## Session 7 handoff — incoming share intent hardening

**Implemented on branch `test/incoming-share-intent`**
- `MainActivity` now reads `Intent.EXTRA_TEXT` as `CharSequence`, matching Android's contract and preserving styled shared text rather than assuming a `String`.
- Incoming intent extraction is isolated in `extractIncomingContent` and has Robolectric tests for ACTION_SEND, ACTION_VIEW, whitespace-only payloads, missing data and unrelated actions.
- This does not prove external chooser behavior or Activity-to-Repository persistence; physical-device acceptance remains open.

**Not yet verified**
- At the Session 7 handoff, CI had not run on the incoming-share branch. PR #15 subsequently merged after CI run #304 succeeded.
- Device acceptance, search UI tests, backup acceptance and WorkManager remain open.

## Session handoff

**Done this session**
- Completed T3.0: typed execution contracts for all 16 registered capabilities, with field validation and fail-closed availability gates.
- Classified remote model inference as remote data disclosure requiring explicit approval and consent.
- Added six capability tests (CapabilityRegistryTest now 12); updated capability docs, T3.0, D-018, coverage, risks, README, changelog and this ledger.
- PR #12 merged to main: `cb929afc81b0e04cf5e7b81d1d199c2a78f4fb8b`.

**State changed**
- `app/src/main/java/com/yusufculcu/trader/domain/CapabilityRegistry.kt`
- `app/src/test/java/com/yusufculcu/trader/domain/CapabilityRegistryTest.kt`
- `docs/CAPABILITY_REGISTRY.md`, `docs/TASKS.md`, `docs/RISKS.md`, `docs/DECISIONS.md`, `docs/COVERAGE.md`
- `README.md`, `CHANGELOG.md`, `docs/STATE.md`

**Verified**
- PR #12 final CI run 285: [run](https://github.com/ysfclc/Trader/actions/runs/44499826271), final PR revision `131d6985a140f335d017c35d37dbfa001743b05b`: **44 tests, 0 failures, 0 errors, 0 skipped** (JUnit artifact `14517540622`).
- Post-merge main CI run 286: [run](https://github.com/ysfclc/Trader/actions/runs/44500309783), merge commit `cb929afc81b0e04cf5e7b81d1d199c2a78f4fb8b`: **44 tests, 0 failures, 0 errors, 0 skipped** (JUnit artifact `14516991778`).
- Full pipeline passed unit tests, lint, debug APK assembly, signature verification and exact package/version assertions.
- No physical-device acceptance was performed.

**Not done / next**
1. Complete physical-device acceptance on Redmi Note 12 Pro 5G: capture, workspace search/archive/restore, workflow transitions/interrupted-run review, Android share chooser, backup export, wrong-passphrase safety and valid restore.
2. Only after acceptance, implement WorkManager execution and recovery.
3. Add one file/document capture vertical slice with original-byte preservation and evidence provenance.

**Open questions**
- No new product decision is required. User-led device acceptance is the current gate.

**Risks / landmines**
- Capability registry is a contract and field validator, not an executor. Adapter-backed capabilities remain NOT_IMPLEMENTED.
- Generic validation does not enforce semantic constraints, URI/origin allowlists, source terms, consent freshness, actual quotas, payload hash re-checks or external postconditions; adapters must enforce these before promotion.
- Remote model inference transmits selected context to a third party and may incur fees; explicit consent is mandatory.
- External send/submit actions require exact approval, prohibit retries and require provider confirmation. Uncertain outcomes go to NEEDS_REVIEW.
- A step marked RUNNING may already have acted; it becomes NEEDS_REVIEW, never auto-retried. Runs interrupted between steps become PAUSED.
- Restore replaces the entire Room workspace; export first. Lost backup passphrase is unrecoverable. Backup snapshot expects Room schema v3.
- Physical-device acceptance and release signing remain open. Debug signing is not production signing.


## Session 5 handoff — cancellation safety (PR #14 merged)

**Implemented and merged** in PR [#14](https://github.com/ysfclc/Trader/pull/14), main commit `4ffd465e7c89a35d3a5c3b6125f8097b246b3b37`:
- Workflow cancellation while a step is RUNNING now moves the run and active step to NEEDS_REVIEW instead of falsely declaring the action cancelled.
- Clears the active step's approval and records that the side effect may already have happened.
- Cancels remaining non-active steps; cancellation between steps remains CANCELLED.
- Added regression tests for active-step uncertainty and cancellation between steps.
- Updated workflow runtime, task ledger (T1.8c), risk register (R-15), coverage matrix and changelog.


**Verification status**
- PR #14 Android CI [run 292](https://github.com/ysfclc/Trader/actions/runs/44587062863) completed successfully: **45 tests, 0 failures, 0 errors, 0 skipped** (JUnit artifact `14561772222`). All workflow steps passed, including lint, debug APK assembly, signature/package verification and artifact upload.
- Artifacts: JUnit report `14561772222`; debug APK `14561732215` (CI debug-signed, not a production release).
- PR #14 merged to `main` as `4ffd465e7c89a35d3a5c3b6125f8097b246b3b37`.
- This is CI verification, not physical-device acceptance; no device test is claimed.


**Next**
- Physical-device acceptance remains an independent open gate.
- WorkManager execution/recovery remains the next major implementation increment after safety gates.


## Session 8 handoff — evidence-backed opportunity workflow

Implemented on feature/evidence-backed-opportunity-loop; PR #16 CI run 306 passed (59 tests, 0 failures/errors/skipped)
- Added an optional one-to-one OpportunityRecord attached to a ProblemCase. A case remains general unless the user chooses to track it in the opportunity pipeline.
- Added trade, service, career and other lanes; organization and contact labels; user-authored fit notes; discovered/qualifying/ready/contacted/waiting/won/lost stages; next action and a local follow-up date.
- Added Pipeline tab with active/closed filter, text search and due/overdue display. Per-case details can create/edit the profile; stage and profile updates append Activity events.
- Added user-triggered contact-route handoff restricted to HTTP(S), mailto: and tel:. Browser opens, email composer opens, dialer opens; no message is sent and no call is placed.
- Room schema migrated from v3 to v4. Encrypted backups now include opportunity profiles; old schema-v3 backup snapshots remain readable and restore without profiles.
- Added repository persistence, rules, contact route, migration and backup compatibility tests. Added English/Polish/Turkish pipeline, form, activity and handoff strings.
- Updated DATA_MODEL, ARCHITECTURE, TASKS, COVERAGE (R-016), RISKS (R-16), DECISIONS (D-019), README and CHANGELOG.

Not yet verified
- PR #16 CI run 306 completed successfully: 59 tests, 0 failures, 0 errors, 0 skipped. Lint, assembleDebug, APK signature verification and exact package/version assertions passed.
- Debug APK artifact 14569058836 was assembled and CI signature/package-verified (19,172,629 bytes; package com.yusufculcu.trader.debug; version 0.1.0-debug; SHA-256 3b3d42cee05ef8ba886d006361f8262e28e33399c1b8bbe48cc7066c56d0037f). It has not been installed on a physical phone. No user-device or real prospect outcome is claimed.
- PR #16 CI run 306 used JUnit artifact 14569574990: 59 tests, 0 failures, 0 errors, 0 skipped. Debug APK artifact ID: 14569058836.
- Follow-up dates are displayed only; they do not schedule background reminders. Discovery, AI qualification, normalized Organization/Person graph, consent, message sending and automatic follow-up remain open.

First intended practical use
- Share a real job, service lead or industrial buyer/supplier source from Chrome/LinkedIn/another Android app into Trader.
- Review the preserved original content/source; add a source-linked evidence note and mark it reviewed only after you verify it.
- Choose Track opportunity; enter its lane, organization, what is needed/offered, why it may fit and the evidence/proof, contact route, stage and next action/date.
- Use Pipeline to find active opportunities, open the case and launch a user-approved contact route. Record any reply or stage change yourself, then set the next follow-up.


## Session 13 handoff — source ecosystem lock and feed-first Discover

**Done this session**
- Researched provider access routes and locked adapter/source-instance/watch-rule separation, monitoring modes and source-family taxonomy in docs/SOURCE_ECOSYSTEM.md.
- Recorded the real Kotlin compilation failure from CI run 379; search prototype is not build-verified.
- Locked the first functional increment: configurable public RSS/Atom feeds, manual refresh, one-shot public search and source-linked research save.

**State changed**
- docs/SOURCE_ECOSYSTEM.md, docs/ARCHITECTURE.md, docs/ROADMAP.md, docs/TASKS.md, docs/DECISIONS.md, docs/RISKS.md, docs/PRODUCT_EXPERIENCE.md, CHANGELOG.md, docs/STATE.md.

**Verified**
- Build: not run for this documentation update.
- Tests: not run for this documentation update.
- Provider research links and access boundaries are recorded in docs/SOURCE_ECOSYSTEM.md.

**Not done / next**
1. Fix run 379 Kotlin errors in DuckDuckGoHtmlSearch.
2. Implement bounded RSS/Atom, configurable local source instances and source health.
3. Add Discover UI, deduplicated evidence persistence and full CI/APK verification.

**Open questions**
- The first local source list is not included in encrypted workspace backup; add a backup migration before treating it as portable.

**Risks / landmines**
- Reddit needs OAuth, deletion reconciliation and short retention; do not archive community content indefinitely.
- Meta Content Library is a restricted research route, not a general Facebook Groups API.
- Feed check is manual only; do not label it background monitoring.
- Run 379 failed at Kotlin compilation; existing DuckDuckGo code must be repaired and verified before promotion.


## Session 14 handoff — Discover and public-source ingestion (PR #18)

**Done this session**
- Locked the provider-neutral source ecosystem: adapter code, configured source instances and watch rules are separate; documented search, feeds, company properties, registers/procurement, marketplaces, communities, federated networks, developer ecosystems, reviews, authorized workspaces, imports and webhooks.
- Added bounded RSS 2.0/Atom reader, local public-feed source manager, explicit adapter registry and one-shot DuckDuckGo search.
- Added Discover as the fresh-install landing page in EN/PL/TR; configure, enable/pause/remove and manually check feeds; open results in Trader browser; save results into Work as deduplicated source-linked UNREVIEWED evidence.
- Added a one-tap real Hacker News front-page RSS example. No seeded/mock content is used.
- CI validation run 385 passed the full suite and build pipeline on a tree identical to PR #18 head.

**State changed**
- Source code: DiscoverySourceAdapter.kt, DiscoverySourceStore.kt, RssAtomFeedAdapter.kt, DuckDuckGoHtmlSearch.kt, DiscoveryScreen.kt, TraderApp.kt, TraderBrowserScreen.kt, TraderApplication.kt, TraderRepository.kt, TraderDatabase.kt.
- Tests: RssAtomFeedAdapterTest.kt, DiscoverySourceStoreTest.kt, DiscoveryAdapterRegistryTest.kt, DuckDuckGoHtmlSearchTest.kt, DiscoveryPersistenceTest.kt.
- Docs/resources: source ecosystem, architecture, roadmap, tasks, coverage, risks, decisions, product experience, README, changelog, state and EN/PL/TR strings.

**Verified**
- Build: PASS — ./gradlew --no-daemon clean testDebugUnitTest assembleDebug lintDebug, CI run 385.
- Tests: 97 passed, 0 failed, 0 errors, 0 skipped (JUnit artifact 14698665260).
- APK: package com.yusufculcu.trader.debug, version 0.1.0-debug; 69,441,948 bytes; SHA-256 b28d0b089962fb3481425ceae14dfb4d4449527a15f7d5bd3fd4408b2664f210 (artifact 14698585569). Signature and exact package/version checks passed.
- Validation branch tree d276e05bc23c3536d5a95410ca59e91ab1249603 is identical to PR #18 head tree. No physical-device or real-feed network smoke test was performed.

**Not done / next**
1. Include source configurations in encrypted backup/restore; source list currently lives in app-private preferences.
2. Add editable watch rules and durable DiscoveryRun/SourceRun state, then implement scheduled checks only after WorkManager recovery/cancellation is verified.
3. Build source-grounded company/person dossiers and explainable proof-asset fit; then add provider-specific community APIs with retention/authorization controls.
4. Verify APK on Redmi and establish signing continuity before update installation; do not uninstall an existing app without protecting its data.

**Open questions**
- Default assumption: include public feed configurations in encrypted workspace backup; credentials must remain outside ordinary source records and be added only with a secure provider-specific store.

**Risks / landmines**
- Source configurations are not yet included in encrypted backup.
- Feed checks are manual only; no scheduler, streaming or background watch is implemented.
- Search/feed content is unverified until inspected; DuckDuckGo markup/source access can change.
- Reddit needs OAuth and deletion/retention enforcement; Meta Content Library is not a general Facebook Groups API; Facebook browser capture is user initiated.
- CI debug signing identity continuity is not established; release signing is not configured.
- No physical-device or live-feed smoke test has been recorded.



---

## Archived from docs/STATE.md on 2026-11-24

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
