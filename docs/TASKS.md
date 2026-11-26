# Trader Task Ledger

This is the active ordered backlog. Preserve completed items and evidence; do not silently delete user requirements. Status labels: **Todo**, **Doing**, **Blocked**, **Done**, **Verified**. Done means implemented; Verified means acceptance-tested.

## T0 — Repository and product foundation

- [x] T0.1 Establish the fresh-start ledger and canonical source of truth in `ysfclc/Trader`.
- [x] T0.2 Inspect relevant existing implementation files/tests and document reuse boundaries in `docs/REUSE_AUDIT.md` (initial targeted pass).
- [x] T0.6 Expand repository audit to BOS, Oddo, Troy, Kovan, Zommo and Nexus; record exact source paths, known gaps and reuse decisions in `docs/ADDITIONAL_REUSE_AUDIT.md`.
- [x] T0.7 Complete source-level audit of QRKey variants and remaining mobile/automation prototypes; record exact findings and no-reuse boundaries in `docs/QRKEY_REUSE_AUDIT.md`.
- [x] T0.3 Pin JDK 17, Gradle 8.13 wrapper, AGP 8.13.0, Kotlin Android/Compose 2.4.21, KSP 2.3.12 and SDK 36; clean CI build, unit tests, lint, APK signature and package identity passed at CI run 341. LiteRT-LM 0.18.0 requires Kotlin metadata 2.4; keep compiler/plugin and KSP versions aligned.
- [x] T0.4 Add Gradle wrapper, reproducible CI build and signing-boundary instructions. Production release signing via GitHub secrets remains open.
- [x] T0.5 Define no-fork/no-runtime-dependency boundaries for Memo, EBO, Unipile and other existing projects.
- [x] T0.8 Establish numerical debug unit-test line/branch coverage baseline and enforce a CI non-regression floor; retain HTML/XML evidence. Original baseline `docs/coverage-baseline.json`: 14.85% lines (1,394/9,388), 4.75% branches (574/12,079), excluding Room-generated `_Impl` classes. The gate preserves covered line/branch counts per each of the 67 previously covered classes; newly added Robolectric/Compose classes do not inflate the historical denominator. Feature-specific behavioral tests remain required for new code. Eight standard-library coverage utility tests exercise class floors, denominator expansion, missing classes, regressions and baseline integrity.
- [x] T0.9 **Verified** — Resolved 421-component CycloneDX SBOM scanned with OSV-Scanner v2.7.1; 0 findings in CI run [45743560784](https://github.com/ysfclc/Trader/actions/runs/45743560784). The same scanner gate passed again on the 0.13.0 candidate in run [45807900617](https://github.com/ysfclc/Trader/actions/runs/45807900617). The scanner does not expose a distinct advisory-database revision/timestamp; freshness is recorded only by scan date/tool version.

## T1 — Android shell and local data

- [x] T1.0 Implement initial ProblemCase model: original request, desired outcome, constraints, category, source metadata, status and timestamps. A separate Goal entity, urgency, uncertainty and evidence records remain open.
- [x] T1.0a Add a Problem Inbox that accepts a manually described problem or goal without requiring a trade/service/job classification.
- [x] T1.0b Add durable plan-step records linked to ProblemCase. Step dependencies, capability execution contracts, approvals and postconditions remain open.
- [x] T1.1 Create native Kotlin/Compose Android project with a distinct application ID, dark mobile UI and English/Turkish/Polish string resources. Device visual acceptance remains open.
- [x] T1.1a Add adaptive Trader launcher/round icons and wire them through the manifest; physical launcher rendering on Redmi remains unverified.
- [x] T1.1b Preserve debug install identity: keep `com.yusufculcu.trader.debug`, increment versionCode monotonically, and use the dedicated persistent Trader development keystore configured in GitHub Actions. CI run 45394348906 verified the APK signature against that certificate and asserted package `com.yusufculcu.trader.debug`, versionCode 4 and versionName `0.4.0-debug`. Physical in-place update acceptance remains T1.1c.
- [ ] T1.1c Verify an in-place update on the Redmi using two successive APKs signed by the same stable development certificate; prove Room rows, source preferences and browser session remain intact.
- [ ] T1.2 Rebuild the product shell around Discover, Work, Live and Studio while preserving Inbox, Pipeline and Activity as linked views; add per-destination state/back-stack and migration tests.
- [x] T1.3 Room entities/DAOs and schema export are implemented; schema snapshot policy remains open.
- [x] T1.3a Add and verify Room v1→v2 migration for workflow tables, preserving existing ProblemCase/plan/activity rows (Robolectric test passed in CI runs 111 and 112).
- [x] T1.3b Add Room v2→v3 migration for reversible case archiving and source-linked evidence records; migrate v1 through both migrations without losing existing records (33 tests passed in CI runs 225, 232 and post-merge 233).
- [x] T1.3c Add Room v3→v4 optional opportunity profiles; legacy schema-v3 encrypted backups remain readable with an empty opportunity set. PR #16 CI run 306 passed (59 tests, 0 failures/errors/skipped); physical-device migration acceptance remains open.
- [x] T1.4 Implement passphrase-protected local backup/export and integrity-verified restore using AES-256-GCM and PBKDF2-HMAC-SHA256 (210,000 iterations, random 16-byte salt and 12-byte IV); validate the authenticated payload/schema/relationships and replace rows atomically. Current snapshot schema v5 contains all current Room groups, including organization dossiers, source records and reviewable claims; older snapshots remain backward-readable. CI and device acceptance are tracked separately; see `docs/COVERAGE.md` R-007 and `docs/RISKS.md`.
- [x] T1.4a Extend encrypted workspace backup to include non-secret discovery source configurations and health; validate adapter/URL/identity bounds, restore full source lists and compensate preferences if the Room transaction fails. Never include signing secrets, browser cookies or provider credentials. CI run 405 passed source-settings round-trip; snapshot v4 additionally contains durable discovery run/source-run/finding history; snapshot v5 adds organization dossiers, sources and claims. Physical-device restore acceptance remains open.
- [x] T1.5 Implement manual problem capture and Android share URL/text intake. Incoming intent extraction now has Robolectric coverage for styled CharSequence text, VIEW URLs, blank/missing payloads and unrelated actions; Android chooser/device acceptance, quick-note shortcuts and document import remain open.
- [x] T1.6 Preserve original text/URL, input kind, capture timestamp and source URI; add separate evidence records with captured content, source URL/type/time and user-controlled verification state. Language metadata, parser version, automatic extraction and file/screenshot/voice evidence remain open.
- [ ] T1.7 Add deterministic deduplication and human-reviewed entity merging.
- [ ] T1.8 Complete durable job/task execution, cancellation and process-death recovery. Conservative persisted-workflow recovery is implemented in T1.8b; WorkManager execution and end-to-end worker recovery remain open.
- [x] T1.9 Implement local case editing, active/archived search, reversible archive/restore, source-linked evidence, expected-result plan steps and context-packet share handoff (CI runs 225, 232, post-merge 233 and main 256 passed; device acceptance and dedicated search/share UI tests remain open).
- [x] T1.8a Persist workflow runs, steps and append-only events with idempotent creation, exact-payload approvals, safe retry and cancellation (CI runs 111 and 112 passed).
- [x] T1.8b Reconcile persisted RUNNING workflows on app startup: interrupted RUNNING steps become NEEDS_REVIEW (never blindly retried); runs interrupted between steps become PAUSED. File-backed close/reopen test verifies recovery, stale-approval revocation, idempotency and guarded human resolution (PR #11 CI run 271 passed; 38 tests total).
- [x] T1.8c Cancellation safety: if a workflow is cancelled with an active RUNNING step, preserve that step as NEEDS_REVIEW (clear its approval), cancel only the remaining steps, and keep the run under review; cancellation between steps remains CANCELLED. Regression tests passed in PR #14 CI run 292; physical-device acceptance remains open.

## T2 — Commercial records

- [x] T2.0 Add an optional one-to-one OpportunityRecord per ProblemCase for a manually tracked trade/service/career/other opportunity: organization and contact labels, fit notes, explicit stage, next action and follow-up date; preserve source/evidence, log stage changes, include it in encrypted backup/restore, and allow scheme-restricted user handoff to browser/email composer/dialer. This does not implement normalized Organization/Person records, discovery, send, call, or background reminders. Implemented on feature/evidence-backed-opportunity-loop; PR #16 CI run 306 passed (59 tests, 0 failures/errors/skipped); physical-device acceptance remains open.
- [x] T2.1 Organization dossier first slice: canonical company profile, normalized website domain, source snapshots, source-linked claims and explicit unreviewed/supported/contradicted/rejected review states. CI run 45369038355 passed 134 tests, lint, APK assembly and persistent-signature/package checks. Person records, aliases, reversible identity resolution and physical-device acceptance remain open.
- [x] T2.2a Add company-scoped Person/contact records with validated email and LinkedIn URL, editable/deletable details, cascading company deletion and encrypted backup snapshot v6. Similar names are not merged automatically; identity review/aliases remain separate work.
- [x] T2.2 Implement BuyerRequirement and line items: quantity, specification, target price when stated, destination, timing and same-company source evidence. Room v9→v10, encrypted backup snapshot v7, EN/PL/TR dossier UI and CI run 45574277490 verified.
- [x] T2.3 Implement SupplierOffer and line items for product, specification, material, available quantity, MOQ, unit price/currency and lead time; same-company source evidence, deterministic comparison and human sourcing decisions. Room v10→v11, encrypted backup snapshot v8, EN/PL/TR Sourcing Desk UI and CI run 45574277490 verified. Certification validation remains explicitly unimplemented.
- [~] T2.4 Implement Product/SKU, Match, Quote and Deal as distinct records. The company Product/SKU catalogue is implemented in T2.4a; persistent Match, Quote and Deal records remain open.
- [x] T2.4a Add a company-owned Product/SKU catalogue with validated product master fields, per-company SKU uniqueness, lifecycle status, source evidence links, Activity history, Room v13→v14 migration, encrypted backup snapshot v11 and EN/PL/TR dossier UI. Product records do not imply inventory, price, supplier availability, technical equivalence or buyer fit. CI run [45807900617](https://github.com/ysfclc/Trader/actions/runs/45807900617) passed the full v1→v14 migration, product repository and backup validation tests, coverage floor, lint, APK assembly, package identity and persistent-signature checks. No installable artifact was uploaded because explicit manual approval is required.
- [ ] T2.5 Implement ServiceNeed and JobOpening using shared organization/source/task primitives.
- [ ] T2.6 Implement contact basis/consent, suppression, representation and commission terms.
- [x] T2.7 Implement append-only business status history and a unified Activity timeline for company, contact, claim, buyer-requirement, supplier-offer and sourcing-decision events. Room v11→v12, encrypted backup snapshot v9, EN/PL/TR status controls and restore compatibility are implemented. CI run [45591681856](https://github.com/ysfclc/Trader/actions/runs/45591681856) passed the full build/test/lint workflow and persistent APK signature check; installable artifact ID 15010224455 is available.
- [x] T2.8 Expose buyer-requirement header and line-item correction in the company dossier. Preserve IDs/creation timestamps, validate with the same repository rules as creation, and append `UPDATED`/`STATUS_CHANGED` header events plus `LINE_UPDATED` line-item events. English/Polish/Turkish UI and regression tests are CI-verified in run [45595844335](https://github.com/ysfclc/Trader/actions/runs/45595844335); persistent-signature APK artifact [15013068288](https://github.com/ysfclc/Trader/actions/runs/45595844335/artifacts/15013068288).
- [x] T2.9 **Verified** — Expose supplier-offer header and multi-line-item correction, including in-place identity-preserving edits, guarded line-item removal, transactional evidence-link reconciliation and append-only activity history. English/Polish/Turkish Sourcing Desk controls and regression tests passed CI run [45598311208](https://github.com/ysfclc/Trader/actions/runs/45598311208): 157 tests, lint, APK assembly and persistent-signature/package/version verification. Installable artifact [15014108093](https://github.com/ysfclc/Trader/actions/runs/45598311208/artifacts/15014108093).
- [x] T2.10 Add durable recovery for deleted sourcing records; do not make hard deletion the only path for important buyer/offer data. Implemented and CI-verified in PR #19 / commit `d376db2`: Room 12→13 tombstones, active/trash queries, restore with child/evidence/review preservation, append-only DELETED/RESTORED activity events, company-cascade guard, encrypted backup snapshot v10 and EN/PL/TR Trash UI. CI run 45642932647 passed 161 tests, lint, build and persistent-certificate verification. Physical-device acceptance remains T1.1c.
 Event rows are insert-only in normal product flows, company deletion retains a minimal history marker, and contact email/phone/name are not copied into activity details.

## T3 — Discovery and browser/app automation

- [x] T3.0 Complete the capability contract for all 16 currently registered operations: typed inputs/outputs, availability, permissions/consent, effects/side effects, retry/approval, cost, limit mode and required postcondition. Registry field validation and promotion gates passed PR #12 CI run 279 (44 tests). Adapter-specific numeric limits and semantic policy validation remain mandatory before any adapter-backed descriptor can become AVAILABLE.
- [x] T3.0a Define and unit-test deterministic workflow run/step lifecycle rules, including approval, safe retry, review and outcome-verification gates.
- [x] T3.0b Verify Room-persisted workflow runs/steps, event history, idempotency, exact-payload approvals, cancellation and migration (CI runs 111 and 112 passed).
- [x] T3.0c Add capability registry enforcement for permission, side-effect, retry-safety, approval and postcondition contracts (PR #4 merged; CI runs 129 and 130 passed).
- [x] T3.0d Add user-facing capability browser and workflow UI with truthful availability, missing-permission, approval and postcondition states. PR #6 merged; CI runs 202/203 passed. Physical-device acceptance remains pending.
- [x] T3.0e Generate a source- and verification-aware context packet from a case, plan and evidence records for explicit share-sheet handoff. Unit test passed in CI runs 225 and 232; this is a handoff, not an embedded AI integration.
- [x] T3.1 Define the source-adapter contract and provider access model, including access modes, terms/policy, scope, credentials, limits, health and failure states in docs/SOURCE_ECOSYSTEM.md. Executable provider-specific policy gates remain a follow-up.
- [~] T3.1a Define and persist versioned ProofAsset and DiscoveryStrategy records: real user capability/proof, evidence-linked claims, problem signatures, target constraints, bounded source query plans and disqualifiers. Room v4→v5 migration, repository validation, JSON codec and encrypted backup compatibility are CI-verified (run 44998620615). User-facing editors, strategy-driven query execution and device acceptance remain open.

- [~] T3.1e Add user-facing ProofAsset and DiscoveryStrategy editors: the strategy workshop now exposes every generated problem signature and its signal/role/phrase/must-have lists, all target constraints, every query field, disqualifiers and result limits, with add/remove controls and validation before approval. Proof-claim evidence review and strategy-driven execution/run linkage remain open; CI/device acceptance is pending.
- [~] T3.1b Persist DiscoveryRun and SourceRun lifecycle in Room v5→v6: source-level status, result counts, cancellation, interrupted-run reconciliation, migration and encrypted backup are implemented and CI-verified. The bounded coordinator is connected to Discover with objective-driven public search, enabled-feed execution, per-source timeout isolation, URL dedupe, cancellation and visible run history. Durable finding records and run-history reopening are tracked in T3.1f. Cache policy, aggregated source health, WorkManager scheduling and device acceptance remain open.
- [x] T3.1f Persist each DiscoveryDocument against its originating DiscoveryRun, reopen saved findings from run history, add Room v6→v7 migration and encrypted backup snapshot v4, and validate the complete v1→v8 migration path. CI run 45369038355 passed 134 tests, lint, APK assembly and stable-signature/package checks; physical-device migration and restore acceptance remain open.
- [x] T3.1g Enforce finding-to-active-source-run provenance transactionally, reject adapter source-ID mismatches and refuse zero-source discovery runs before persistence. CI run 45394348906 passed `testDebugUnitTest`, `assembleDebug`, `lintDebug`, package/version and persistent-certificate checks; physical-device acceptance remains open.
- [x] T3.2a Implement one real public-source adapter end-to-end: configurable RSS/Atom feed → bounded fetch/parse → Discover results → source-linked unreviewed evidence; CI run 385 passed.
- [x] T3.2b Stabilize and CI-verify the bounded DuckDuckGo HTML search adapter; integrated one-shot search into Discover. CI run 385 passed.
- [x] T3.1c Lock the provider-neutral source ecosystem, adapter/source-instance/watch-rule split, source modes and provider access matrix in docs/SOURCE_ECOSYSTEM.md.
- [x] T3.1d Implement configurable local public-feed source instances with add, enable/pause, remove and last-check/error health state. Stored in app-private preferences; encrypted backup remains open.
- [x] T3.2c Implement bounded RSS 2.0/Atom fetch, DocDecl-disabled XML parsing, public-host checks, native item identity, timestamps, URL dedupe and parser tests; CI run 385 passed.
- [x] T3.2d Add Discover UI: objective-driven bounded public search plus enabled RSS/Atom sources through one persisted multi-source run; show recent run/source status, progress/cancellation, feed health, result dedupe, browser handoff and save-to-Work as source-linked UNREVIEWED evidence. EN/PL/TR. CI run 44998620615 passed 115 tests; live-source and physical-device acceptance remain open.
- [x] T3.2e Include configured public feed instances, enabled state and last-check/error health in encrypted backup/restore; credentials/cookies are excluded. CI run 405 passed; physical-device restore acceptance remains open.
- [ ] T3.2f Add scheduled polling only after durable WorkManager execution, pause/backoff/cursor and recovery contracts.
- [ ] T3.2g Add provider-specific community adapters: Bluesky/Mastodon public APIs, Reddit OAuth with deletion/retention reconciliation, authorized Telegram/Discord/Slack, and browser-assisted Facebook capture unless formal access is approved.
- [x] T3.2 Deliver first source adapters with attribution: DuckDuckGo one-shot public search and configurable RSS/Atom manual feed refresh. Further providers remain T3.2g.
- [x] T3.3a Add first Trader-owned browser screen with address/search, back/forward, stop/reload, title/current URL, progress and share-page handoff. Browser code is in PR #17; physical-device acceptance remains open. Tabs, bookmarks/history, find-in-page, downloads, site-specific session controls and dossier evidence capture remain open.
- [x] T3.3b Add account-login foundations: persistent app-local WebView cookie storage and flush, explicit third-party cookie policy for SSO, user-gesture popup windows, Android autofill and web file chooser; detect Google OAuth and restart the relying-party site in the system browser without attempting embedded Google sign-in or copying Chrome cookies. CI run 349 passed 65 tests; run 358 passed the full build/test/lint/signature/package pipeline. User confirmed embedded account login works on Redmi; provider-by-provider and process-restart coverage remains open.
- [x] T3.3c Connect authenticated Trader browser to source-linked evidence capture: extract only visible page text/selection, review/edit before saving, attach to an active case or create a research record, retain title/base URL/capture time and mark evidence UNREVIEWED. Strip URL query and fragment on capture and again at persistence. Parser, persistence, unsafe-URL, size-bound, URL sanitization and no-cookie/storage-read tests passed in CI run 358 (76 tests, 0 failures/errors/skipped); full device acceptance remains open.
- [x] T3.3d Add a versioned MEMO_BROWSER_CAPTURE_V1 Android share-sheet contract and Trader receiver. Memo exports only user-requested visible page text, summary, title and sanitized base URL; Trader recognizes the payload as research evidence. Ordinary shared links/text retain their existing intake behavior. Trader share parsing tests passed in CI run 358; Memo PR #15 sender passed Android tests, lint, build/signature and install-over-existing data-preservation checks in run 1009. The PR remains draft/unmerged and the source-only verification run did not publish an installable APK artifact.
- [x] T3.3e Route Memo payloads through an explicit editable review/redaction dialog before evidence persistence; preserve pending review state across Activity recreation and consume the incoming share intent to prevent duplicate imports. Added route tests; final CI run 385 passed the 97-test suite, lint, APK build, signature and package checks.
- [ ] T3.3 Implement full browser baseline with tabs, bookmarks/history, find-in-page, downloads and cookie/session controls.
- [ ] T3.3a Add visible browser task controls, pause/stop/takeover, origin scope, login/CAPTCHA/OTP/consent-wall detection and evidence capture into the active dossier.
- [ ] T3.4 Investigate Android intents, Custom Tabs, WebView and AccessibilityService limitations; Accessibility must not be the sole automation foundation.
- [ ] T3.5 Add durable queue, pause/resume, retry, cancellation and failure inspection.
- [ ] T3.6 Add source-specific access checks, rate limits, caching and backoff.
- [ ] T3.7 Keep discovery targeted to permitted, relevant public information; no credential bypass or access-control circumvention.

## T4 — AI and matching

- [ ] T4.1 Define a Trader-owned provider-neutral AI interface; no Memo runtime dependency.
- [ ] T4.2 Implement deterministic extraction, normalization and source-claim linking.
- [ ] T4.3 Add optional on-device model discovery and inference adapter only after device feasibility tests.
- [ ] T4.4 Add optional external model adapters; paid credits must never be required for core flows.
- [ ] T4.5 Implement bounded context packets, redaction, prompt versioning and explicit remote-model consent.
- [ ] T4.6 Implement explainable buyer-to-supplier matching and mandatory-constraint contradiction checks.
- [ ] T4.7 Add human review for uncertain or high-impact claims.
- [~] T4.8 Organization dossier foundation is implemented with source-linked claims and human review states. Person records, aliases, identity-resolution state and reversible duplicate review remain open.
- [~] T4.9 **Domain + Room schema CI-verified** — `FitAssessment.kt` computes explicit supporting/contradicting/unknown evidence from eligible claims; Room v15 now defines assessment, evidence-link and reviewer-override tables with v14→v15 migration. CI run [45854262041](https://github.com/ysfclc/Trader/actions/runs/45854262041) passed 187 tests, including the focused migration constraints test and full v1→v15 migration. Repository transaction/ownership validation, full computed-result snapshot, encrypted backup snapshot v11→v12, EN/PL/TR dossier UI and device acceptance remain open.

## T5 — Outreach and deal execution

- [ ] T5.1 Generate personalized drafts from verified facts only.
- [ ] T5.2 Implement approved message drafting and Android share/email handoff.
- [x] T5.2a Add user-initiated scheme-restricted contact-route handoff (browser, email composer or dialer); it does not send or place calls. CI run 306 passed in PR #16; physical-device acceptance remains open.
- [ ] T5.3 Evaluate Unipile alternatives channel by channel; do not assume inbox sync is available.
- [ ] T5.4 Bind explicit approval to exact recipient, channel, purpose and payload hash before consequential sends.
- [ ] T5.5 Track draft, approved, handed-off, confirmed-sent, failed, uncertain and replied states accurately.
- [ ] T5.6 Add durable follow-up notifications/reminders and the full deal pipeline; the current Pipeline only displays manually entered dates and does not schedule notifications.
- [ ] T5.7 Create buyer/supplier introduction dossier and written commission/authority checklist.
- [ ] T5.8 Generate individualized outreach drafts from verified facts and a selected ProofAsset; preserve claim provenance and flag unsupported statements.
- [ ] T5.9 Add channel capability declarations for discover/read/draft/handoff/send/reply/verify; never present a browser or Android intent handoff as a confirmed send.

## T7 — Local AI foundation (Memo-2 inspired, Trader-owned)

- [x] T7.0 Accept architecture: local AI is Trader's intelligence backbone, not a chatbot; Memo-2 is pattern reference only. See `docs/LOCAL_AI_ARCHITECTURE.md`.
- [x] T7.0a Add Trader-owned pinned model catalog (small orchestrator, analyst, deep model profiles) and role metadata.
- [x] T7.0b Add real LiteRT-LM inference adapter with CPU execution, off-main-thread operation, bounded input/output and pinned SHA-256 verification; CI build/test/lint verification passed in run 349; on-device LiteRT-LM inference remains open.
- [~] T7.0d Add resumable pinned model download, SHA-256 verified install, private storage, storage/RAM preflight, active model selection and Model Hub UI with an actual inference smoke test. Code exists; CI build verification passed in run 349; model installation/inference on the target device remains unverified.
- [x] T7.0c Add bounded ContextPacket with explicit fact status, provenance links, unknowns and truncation notice.
- [~] T7.1 Expand Model Hub into a curated model library with publisher/source/revision/hash, artifact format, license-review status, strengths, limitations, device guidance and per-model output-token settings. User-triggered immutable metadata checks and cached statuses are implemented; explicit temporary candidate download/byte-count/SHA-256 verification is implemented and never installs or repins candidates. Human release approval/repinning, automatic notifications, model scan/import, accelerator capability and full runtime diagnostics remain open.
- [~] T7.2 Context Engine: bounded context packet rendering and a discovery workshop now assemble a selected problem, plan steps, source-linked evidence and a user-owned proof asset; the prompt builder treats supplied content as untrusted data. Cross-case relevance ranking, organization dossiers, sensitive-data redaction and external-routing controls remain open.
- [~] T7.3 Structured-output validation: strict bounded DiscoveryStrategy parsing, registered-adapter checks, app-owned IDs/timestamps and deterministic repository validation are implemented. Parser acceptance tests passed in CI run 45064444679 (14/14 local AI foundation tests); generic workflow/capability plan parsing and registered-capability/input validation remain open.
- [~] T7.4 Local orchestrator: a local-only discovery draft generator now routes through the active verified model and bounded prompt, returns an unpersisted parsed draft, and assigns identity only when the reviewed draft is materialized. General task routing, durable inference run persistence, cancellation UI, model memory budgets and process-death recovery remain open.
- [ ] T7.5 Connect local AI to genuine public-source discovery, evidence extraction, company qualification and browser research; acceptance requires a real model on-device.
- [ ] T7.6 Optional frontier escalation with packet preview, explicit consent and no silent fallback.

## T6 — Pilot and verification

- [ ] T6.0b Verify signed update continuity on Redmi across two releases, including Room, preferences and WebView session retention; no uninstall/clear-data as an update strategy.
- [ ] T6.0 Verify one non-commercial ProblemCase end-to-end: capture → context/evidence → plan → supported actions → postcondition verification → user-confirmed closure.
- [ ] T6.0a Verify pause, resume, cancellation and process-death recovery for a multi-step workflow, including truthful partial/failed states.
- [ ] T6.1 Start with industrial tooling and machining between Türkiye and Poland/EU.
- [ ] T6.2 Build manually verified buyer and supplier records from live, cited evidence.
- [ ] T6.3 Validate one real buyer requirement against supplier technical evidence.
- [ ] T6.4 Run approved individualized outreach and record actual outcomes.
- [ ] T6.5 Verify capture → qualify → match → draft → approve → contact → follow-up on device.
- [x] T6.6 Record APK hash, source commit, test evidence, CI run and known issues (see `docs/STATE.md`).

## Product-scope guardrail

Do not treat trade, services and careers as an exhaustive list of supported problems. Every user problem must have a universal ProblemCase path, even when no specialized workflow exists yet. Do not promise a solution when required data, permissions, tools, professional judgment or external decisions are unavailable.

## Current execution rule

Do not start broad scraping, bulk outreach, cloud sync or large AI integrations before the local data/evidence workflow is reliable. Build one complete vertical slice at a time. Code presence is not verification. Use `docs/COVERAGE.md` for requirement traceability and `docs/RISKS.md` for unresolved acceptance/security risks. A checked implementation task does not imply physical-device acceptance.


## Discovery run continuity increment (2026-11-24)

- [x] Restore durable findings under their originating run with Room v6→v7 migration and cascading deletion.
- [x] Persist findings from both direct and saved-strategy execution; expose saved findings from run history.
- [x] Include findings in encrypted snapshot v4 with bounded-field and parent-run validation; restore in FK-safe order.
- [ ] CI and physical-device migration/backup acceptance remain open until verified.


## Organization dossier vertical slice (2026-11-24)

- [x] Add Room v7→v8 organization, source and claim tables with cascading foreign keys and domain lookup.
- [x] Add manual company dossier creation and creation from a Discover result; preserve the original result as a source snapshot without asserting it proves company identity.
- [x] Add source-linked claims that start UNREVIEWED and can be explicitly marked SUPPORTED, CONTRADICTED or REJECTED by the operator; reset remains available.
- [x] Include all dossier records in encrypted backup snapshot v5 with parent/source validation and FK-safe restore order.
- [ ] CI and physical-device migration/backup acceptance remain open until verified.

## Release continuity and visible-delivery gate (2026-11-24)

- [x] Persistent Trader development signing configured in GitHub Actions; CI run 45345752081 verified the APK against the configured keystore and uploaded the installable artifact.
- [x] Repair legacy migration test to include Room migration 6→7; commit a4950eb; CI run 45345752081 passed.
- [ ] On Redmi, prove the next APK updates the currently installed stable-signed app in place (adb install -r) without uninstalling or losing app data.
- [ ] Do not call a phase complete based only on tests or schema work: ship a visible app capability and provide a concise before/after summary with every APK.
- [ ] Next visible vertical slice: explainable ProofAsset-to-Organization fit assessments with deterministic supporting, contradicting and unknown components, evidence provenance and an explicit reviewer override; do not infer fit from unreviewed claims.

## T2 — Buyer requirements

- [x] T2.1 Add company-scoped buyer requirement records with lifecycle status, destination, required-by timing, optional currency/budget and operator notes.
- [x] T2.2 Add normalized line items with specification, exact decimal quantity text, unit and optional target unit price in minor units.
- [x] T2.3 Link requirements to saved company sources as evidence; reject cross-company links and avoid automatic fact verification.
- [x] T2.4 Add Room v9→v10 migration and encrypted backup snapshot v7 with validation and FK-safe restore order.
- [x] T2.5 Add EN/PL/TR company-dossier UI for requirement creation, line items and evidence linking.
- [x] T2.6 CI run 45570394101 passed unit tests, lint, build, package/version and persistent signing-certificate checks; physical-device acceptance remains open.
- [x] T2.7 Implement append-only business status history and a unified Activity timeline for company, contact, claim, buyer-requirement, supplier-offer and sourcing-decision events. Room v11→v12, encrypted backup snapshot v9, EN/PL/TR status controls and restore compatibility are implemented; CI verification is pending. Event rows are insert-only in normal product flows, company deletion retains a minimal history marker, and contact email/phone/name are not copied into activity details.


## Product P1 — Industrial Sourcing Desk

- [x] P1.1 Deliver the buyer-request → supplier-offer → comparison → human-shortlist workflow inside Trader.
- [x] P1.2 Persist supplier offers, offer line items, evidence links and sourcing decisions in Room v11; expose buyer-request and supplier-offer lifecycle status controls with immutable transition history in Room v12.
- [x] P1.3 Preserve all sourcing records and business activity history in encrypted backup snapshot v10, including recoverable deletion state, with validation and safe restore ordering.
- [x] P1.4 Provide EN/PL/TR sourcing workflow and explicit shareable shortlist handoff.
- [x] P1.5 CI run 45574277490 passed clean build, tests, lint and persistent APK identity/signature checks.
- [ ] P1.6 Complete physical-device install-over-existing, migration/restore and real buyer-to-shortlist acceptance.
- [ ] P1.7 Finish the product's edit/correction lifecycle before starting a second product.
