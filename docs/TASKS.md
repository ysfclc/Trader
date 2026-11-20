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

## T1 — Android shell and local data

- [x] T1.0 Implement initial ProblemCase model: original request, desired outcome, constraints, category, source metadata, status and timestamps. A separate Goal entity, urgency, uncertainty and evidence records remain open.
- [x] T1.0a Add a Problem Inbox that accepts a manually described problem or goal without requiring a trade/service/job classification.
- [x] T1.0b Add durable plan-step records linked to ProblemCase. Step dependencies, capability execution contracts, approvals and postconditions remain open.
- [x] T1.1 Create native Kotlin/Compose Android project with a distinct application ID, dark mobile UI and English/Turkish/Polish string resources. Device visual acceptance remains open.
- [ ] T1.2 Rebuild the product shell around Discover, Work, Live and Studio while preserving Inbox, Pipeline and Activity as linked views; add per-destination state/back-stack and migration tests.
- [x] T1.3 Room entities/DAOs and schema export are implemented; schema snapshot policy remains open.
- [x] T1.3a Add and verify Room v1→v2 migration for workflow tables, preserving existing ProblemCase/plan/activity rows (Robolectric test passed in CI runs 111 and 112).
- [x] T1.3b Add Room v2→v3 migration for reversible case archiving and source-linked evidence records; migrate v1 through both migrations without losing existing records (33 tests passed in CI runs 225, 232 and post-merge 233).
- [x] T1.3c Add Room v3→v4 optional opportunity profiles; legacy schema-v3 encrypted backups remain readable with an empty opportunity set. PR #16 CI run 306 passed (59 tests, 0 failures/errors/skipped); physical-device migration acceptance remains open.
- [x] T1.4 Implement passphrase-protected local backup/export and integrity-verified restore for all eight current Room record groups using AES-256-GCM and PBKDF2-HMAC-SHA256 (210,000 iterations, random 16-byte salt and 12-byte IV); validate the authenticated payload/schema/relationships and replace rows atomically. CI verified in PR #8 and main run 256 (4 backup tests; 37 total tests passed). Device acceptance remains open; see `docs/COVERAGE.md` R-007 and `docs/RISKS.md`.
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
- [ ] T2.1 Implement Organization and Person records.
- [ ] T2.2 Implement BuyerRequirement and line items: quantity, specification, target price when stated, destination, timing and evidence.
- [ ] T2.3 Implement SupplierOffer: capability, capacity, MOQ, pricing basis, lead time, certifications and evidence.
- [ ] T2.4 Implement Product/SKU, Match, Quote and Deal as distinct records.
- [ ] T2.5 Implement ServiceNeed and JobOpening using shared organization/source/task primitives.
- [ ] T2.6 Implement contact basis/consent, suppression, representation and commission terms.
- [ ] T2.7 Implement append-only status history and activity log.

## T3 — Discovery and browser/app automation

- [x] T3.0 Complete the capability contract for all 16 currently registered operations: typed inputs/outputs, availability, permissions/consent, effects/side effects, retry/approval, cost, limit mode and required postcondition. Registry field validation and promotion gates passed PR #12 CI run 279 (44 tests). Adapter-specific numeric limits and semantic policy validation remain mandatory before any adapter-backed descriptor can become AVAILABLE.
- [x] T3.0a Define and unit-test deterministic workflow run/step lifecycle rules, including approval, safe retry, review and outcome-verification gates.
- [x] T3.0b Verify Room-persisted workflow runs/steps, event history, idempotency, exact-payload approvals, cancellation and migration (CI runs 111 and 112 passed).
- [x] T3.0c Add capability registry enforcement for permission, side-effect, retry-safety, approval and postcondition contracts (PR #4 merged; CI runs 129 and 130 passed).
- [x] T3.0d Add user-facing capability browser and workflow UI with truthful availability, missing-permission, approval and postcondition states. PR #6 merged; CI runs 202/203 passed. Physical-device acceptance remains pending.
- [x] T3.0e Generate a source- and verification-aware context packet from a case, plan and evidence records for explicit share-sheet handoff. Unit test passed in CI runs 225 and 232; this is a handoff, not an embedded AI integration.
- [ ] T3.1 Define a source-adapter contract including access mode, terms/policy URL, scope, rate limits, required credentials and source health.
- [ ] T3.1a Define versioned ProofAsset and DiscoveryStrategy records: real user capability/proof, problem signatures, target constraints, query plan and disqualifiers.
- [ ] T3.1b Define durable DiscoveryRun and SourceRun state with bounded execution, cancellation, caching, timestamps, health and explicit failures.
- [ ] T3.2a Implement one real, documented public-source adapter end-to-end; no fixtures or fake result lists.
- [ ] T3.2 Implement public-web/source search adapters with source attribution.
- [x] T3.3a Add first Trader-owned browser screen with address/search, back/forward, stop/reload, title/current URL, progress and share-page handoff. Browser code is in PR #17; physical-device acceptance remains open. Tabs, bookmarks/history, find-in-page, downloads, site-specific session controls and dossier evidence capture remain open.
- [x] T3.3b Add account-login foundations: persistent app-local WebView cookie storage and flush, explicit third-party cookie policy for SSO, user-gesture popup windows, Android autofill and web file chooser; detect Google OAuth and restart the relying-party site in the system browser without attempting embedded Google sign-in or copying Chrome cookies. URL/provider routing regression tests added. CI run 349 passed 65 tests, lint, debug APK build, signature and package identity checks; Android CookieManager behavior and real Redmi account-login/process-restart acceptance remain open.
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
- [ ] T4.8 Add Organization and Person dossiers with aliases/domains, source-linked claims, identity-resolution state and reversible duplicate review.
- [ ] T4.9 Add explainable ProofAsset-to-Organization fit assessments: supporting/contradicting evidence, unknowns, deterministic components and reviewer override.

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
- [ ] T7.1 Complete Model Hub: model scan/import, richer compatibility diagnostics, accelerator capability and runtime error details.
- [ ] T7.2 Context Engine: assemble relevant proof assets, strategies, evidence, organizations, memory and unknowns; redact before external routing.
- [ ] T7.3 Structured-output validation and plan parser; reject malformed output and unregistered capabilities.
- [ ] T7.4 Local orchestrator: task routing, run persistence, cancellation, budgets and process-death recovery.
- [ ] T7.5 Connect local AI to genuine public-source discovery, evidence extraction, company qualification and browser research; acceptance requires a real model on-device.
- [ ] T7.6 Optional frontier escalation with packet preview, explicit consent and no silent fallback.

## T6 — Pilot and verification

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
