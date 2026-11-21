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

