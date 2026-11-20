# State Ledger
_Last updated: 2026-11-20 — session 9 (discovery-first experience direction)_

## 1. Product snapshot
Trader is an Android-first, local-first problem-solving and opportunity-execution workspace. Its next priority is proof-asset-led company discovery, evidence-backed company dossiers, a Trader-owned in-app browser and controlled outreach preparation. Trade/services/careers remain initial specialist workflows, not the product boundary. The current Inbox/Pipeline/Activity shell is a foundation only; it is not the intended user experience. “Complete” means an end-to-end user outcome is demonstrated, not just that a screen or class exists.

## 2. Status board
| Area | State | Notes |
|---|---|---|
| Android shell / EN-TR-PL | 🟡 partial | Builds in CI; current Inbox/Pipeline/Activity shell is being redesigned around Discover/Work/Live/Studio; visual/device acceptance open |
| ProblemCase capture | 🟡 partial | Manual + shared text/URL; incoming intent extraction CI-tested; no file/screenshot/voice |
| Local case workspace | 🟡 partial | Edit/search/archive/restore; direct search/share UI tests and device acceptance open |
| Evidence | 🟡 partial | User-attached, source-linked, user-verified state; no automatic extraction |
| Plans / workflow records | 🟡 partial | Room persistence, events, approval and lifecycle rules; interrupted runs reconcile safely; active-step cancellation preserves uncertainty for review; current execution remains user-driven |
| Capability registry | 🟡 partial | All 16 registered operations have typed input/output, cost, limit-mode and postcondition contracts; generic field validation and fail-closed promotion invariants pass CI; adapter-specific semantics/quotas remain open |
| Context packet | 🟡 partial | Builder tested; Android share chooser not device-accepted |
| Encrypted backup/restore | 🟡 partial | PR #8 and main CI verified; physical document-picker/export/restore acceptance open |
| WorkManager executor / recovery | 🟡 partial | Startup recovery flags ambiguous in-flight steps and pauses between-step runs; no WorkManager executor or automatic resume |
| Browser / installed-app automation | ⚪ not started | No Trader-owned browser session yet; no execution adapter |
| AI / live discovery / matching | ⚪ not started | No real public-source adapter, ProofAsset/DiscoveryStrategy runtime, company dossier or explainable fit engine |
| Opportunity tracking | 🟡 partial | Optional one-to-one manual profile is implemented; normalized organizations/people, buyer/supplier graph, consent, discovery and send adapter remain absent |
| Release signing | ⚪ not started | Debug build only; release key not configured |
| Documentation / traceability | ✅ done | Product lock, task ledger, coverage matrix, risk register, capability contracts, decision log and changelog are maintained on main |

## 3. Currently in flight
- Discovery-first product direction is documented on `feature/discovery-browser-workspace` in `docs/PRODUCT_EXPERIENCE.md` and D-020. The branch now adds a first Trader-owned WebView browser screen (address/search, back/forward, stop/reload, title/current URL, progress, share handoff); CI and device acceptance are pending. No discovery source adapter or evidence capture into dossiers exists yet.
- The product's next acceptance target is a real ProofAsset → DiscoveryStrategy → permitted public source → candidate dossier → Trader browser research → individualized outreach draft/handoff journey. Do not deliver another APK as if this target were met until it is implemented and CI-verified.

## 3a. Previously in flight (historical)
- Opportunity workflow PR #16 — code and test suite are on feature/evidence-backed-opportunity-loop; PR #16 CI run 306 passed. APK assembly and signature/package checks passed; physical-device acceptance is still open.
- Device acceptance (T6.0/T6.0a) — install and exercise the current debug APK on the Redmi Note 12 Pro 5G; requires physical-device and user-driven acceptance.
- Backup acceptance (T1.4) — verify document-picker export/import, wrong-passphrase non-destructive behavior and valid full restore on device.

## 4. Known gaps & stubs
- TraderApp.kt — main currently has Inbox and Activity; the feature branch adds Opportunity Pipeline. Discover/Matches/Contacts are not implemented.
- TraderBackupManager.kt — feature branch reads schema-v3 and v4 snapshots; older v1/v2 snapshots remain unsupported. Future schema changes need explicit backup migrations.
- `app/src/main/java/com/yusufculcu/trader/data/TraderRepository.kt` — no deterministic deduplication or reversible entity-merge workflow.
- No selected-file, screenshot or voice capture; document/attachment storage is not implemented.
- Workflow surface is manual; startup reconciliation handles persisted RUNNING states conservatively, and active-step cancellation routes to NEEDS_REVIEW rather than falsely declaring cancellation. WorkManager execution, automatic resume and worker-level postcondition checks are not implemented.
- Capability contracts are declarative. Generic validation checks required fields, primitive/container types and unknown keys, but concrete adapters must still enforce cross-field rules, URI/origin scope, source access policy, permission/consent freshness, payload binding and actual numeric ceilings before execution.
- Browser/app automation, AI providers, public-source discovery, buyer/supplier matching, normalized Organization/Person records, consent tracking and communication send adapters are absent. Safe HTTP(S)/mailto/tel contact-route handoff only opens another app.
- Search has no dedicated UI test; incoming ACTION_SEND/ACTION_VIEW extraction has Robolectric coverage but no Activity-to-database integration test; no physical-device test has been recorded.
- Live Room storage has no dedicated app-level database encryption; encrypted backup protects exported files only.
- No release signing configuration; do not present the debug build as production.

## 5. Environment & how to run
- Repository: `ysfclc/Trader`; primary branch: `main`.
- Toolchain: JDK 17, Gradle Wrapper 8.13, AGP 8.13.0, Kotlin 2.2.20, KSP 2.2.20-2.0.4, compile SDK 36, min SDK 26, target SDK 35.
- Build/test/lint: `./gradlew --no-daemon clean testDebugUnitTest assembleDebug lintDebug`.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Package/version: `com.yusufculcu.trader.debug` / `0.1.0-debug`. See `docs/ANDROID_BUILD.md` for signing boundaries.

## 6. Next three increments
1. Build the first real discovery source vertical slice: ProofAsset + editable DiscoveryStrategy + bounded public-source search + source health/provenance + deduplicated candidates. No seeded or mock results.
2. Build the Trader-owned in-app browser and connect page capture/evidence to the active company dossier and visible Live execution controls.
3. Add company/person dossiers, explainable fit and individualized outreach drafts with exact approval and verified handoff semantics. Only then deliver the next APK as a meaningful discovery-to-outreach pilot; device acceptance remains separate. No automatic send or ambiguous retry.

## 7. Open questions for the human
- [ ] No product decision blocks the implementation plan. Keep backup restore as explicit whole-workspace replacement; any merge behavior requires a separately specified decision.
- [ ] Before public distribution, configure a dedicated Trader release keystore via protected GitHub Actions secrets; never reuse another app's signing identity.

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
