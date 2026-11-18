# Trader Task Ledger

This is the active ordered backlog. Preserve completed items and evidence; do not silently delete user requirements. Status labels: **Todo**, **Doing**, **Blocked**, **Done**, **Verified**. Done means implemented; Verified means acceptance-tested.

## T0 — Repository and product foundation

- [x] T0.1 Establish the fresh-start ledger and canonical source of truth in `ysfclc/Trader`.
- [x] T0.2 Inspect relevant existing implementation files/tests and document reuse boundaries in `docs/REUSE_AUDIT.md` (initial targeted pass).
- [x] T0.6 Expand repository audit to BOS, Oddo, Troy, Kovan, Zommo and Nexus; record exact source paths, known gaps and reuse decisions in `docs/ADDITIONAL_REUSE_AUDIT.md`.
- [x] T0.7 Complete source-level audit of QRKey variants and remaining mobile/automation prototypes; record exact findings and no-reuse boundaries in `docs/QRKEY_REUSE_AUDIT.md`.
- [x] T0.3 Pin JDK 17, Gradle 8.13 wrapper, AGP 8.13.0, Kotlin 2.2.20, KSP 2.2.20-2.0.4 and SDK 36; clean CI build, unit tests, lint, APK signature and package identity passed.
- [x] T0.4 Add Gradle wrapper, reproducible CI build and signing-boundary instructions. Production release signing via GitHub secrets remains open.
- [x] T0.5 Define no-fork/no-runtime-dependency boundaries for Memo, EBO, Unipile and other existing projects.

## T1 — Android shell and local data

- [x] T1.0 Implement initial ProblemCase model: original request, desired outcome, constraints, category, source metadata, status and timestamps. A separate Goal entity, urgency, uncertainty and evidence records remain open.
- [x] T1.0a Add a Problem Inbox that accepts a manually described problem or goal without requiring a trade/service/job classification.
- [x] T1.0b Add durable plan-step records linked to ProblemCase. Step dependencies, capability execution contracts, approvals and postconditions remain open.
- [x] T1.1 Create native Kotlin/Compose Android project with a distinct application ID, dark mobile UI and English/Turkish/Polish string resources. Device visual acceptance remains open.
- [ ] T1.2 Expand navigation beyond the first Inbox and Activity surfaces to Discover, Matches, Contacts and specialized trade/service/career workflows.
- [ ] T1.3 Room v1 entities/DAOs and schema export are implemented; schema snapshot policy remains open.
- [x] T1.3a Add and verify Room v1→v2 migration for workflow tables, preserving existing ProblemCase/plan/activity rows (Robolectric test passed in CI runs 111 and 112).
- [ ] T1.4 Add encrypted local backup/export and integrity-verified restore.
- [x] T1.5 Implement manual problem capture and Android share URL/text intake. Quick-note shortcuts and document import remain open.
- [ ] T1.6 Initial capture preserves original text/URL, input kind, capture timestamps and source URI. Evidence entities, language metadata, parser version and verification states remain open.
- [ ] T1.7 Add deterministic deduplication and human-reviewed entity merging.
- [ ] T1.8 Add durable job/task state, cancellation and process-death recovery tests.
- [x] T1.8a Persist workflow runs, steps and append-only events with idempotent creation, exact-payload approvals, safe retry and cancellation (CI runs 111 and 112 passed).

## T2 — Commercial records

- [ ] T2.1 Implement Organization and Person records.
- [ ] T2.2 Implement BuyerRequirement and line items: quantity, specification, target price when stated, destination, timing and evidence.
- [ ] T2.3 Implement SupplierOffer: capability, capacity, MOQ, pricing basis, lead time, certifications and evidence.
- [ ] T2.4 Implement Product/SKU, Match, Quote and Deal as distinct records.
- [ ] T2.5 Implement ServiceNeed and JobOpening using shared organization/source/task primitives.
- [ ] T2.6 Implement contact basis/consent, suppression, representation and commission terms.
- [ ] T2.7 Implement append-only status history and activity log.

## T3 — Discovery and browser/app automation

- [ ] T3.0 Define a capability registry for every tool/automation operation, including inputs/outputs, availability, permissions, side effects, cost, limits and postconditions.
- [x] T3.0a Define and unit-test deterministic workflow run/step lifecycle rules, including approval, safe retry, review and outcome-verification gates.
- [x] T3.0b Verify Room-persisted workflow runs/steps, event history, idempotency, exact-payload approvals, cancellation and migration (CI runs 111 and 112 passed).
- [x] T3.0c Add capability registry enforcement for permission, side-effect, retry-safety, approval and postcondition contracts (PR #4 merged; CI runs 129 and 130 passed).
- [ ] T3.0d Add workflow UI for plan conversion, run/step control, approval, review, retry, cancellation and outcome verification (implemented and CI-verified on `build/workflow-ui`; physical-device acceptance remains open).
- [ ] T3.0e Add user-facing capability browser with truthful availability, missing-permission and policy explanations.
- [ ] T3.1 Define a source-adapter contract including access mode, terms/policy URL, scope, rate limits, required credentials and source health.
- [ ] T3.2 Implement public-web/source search adapters with source attribution.
- [ ] T3.3 Implement a visible browser session with current URL, pause, stop, takeover and postcondition checks.
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

## T5 — Outreach and deal execution

- [ ] T5.1 Generate personalized drafts from verified facts only.
- [ ] T5.2 Implement Android share sheet and email handoff.
- [ ] T5.3 Evaluate Unipile alternatives channel by channel; do not assume inbox sync is available.
- [ ] T5.4 Bind explicit approval to exact recipient, channel, purpose and payload hash before consequential sends.
- [ ] T5.5 Track draft, approved, handed-off, confirmed-sent, failed, uncertain and replied states accurately.
- [ ] T5.6 Add follow-up reminders and deal pipeline.
- [ ] T5.7 Create buyer/supplier introduction dossier and written commission/authority checklist.

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

Do not start broad scraping, bulk outreach, cloud sync or large AI integrations before the local data/evidence workflow is reliable. Build one complete vertical slice at a time. Code presence is not verification.
