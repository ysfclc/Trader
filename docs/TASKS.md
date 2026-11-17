# Trader Task Ledger

This is the active ordered backlog. Preserve completed items and evidence; do not silently delete user requirements. Status labels: **Todo**, **Doing**, **Blocked**, **Done**, **Verified**. Done means implemented; Verified means acceptance-tested.

## T0 — Repository and product foundation
- [ ] T0.1 Add a fresh-start ledger and keep `docs/STATE.md` updated.
- [ ] T0.2 Verify all repository documents and links.
- [ ] T0.3 Define Android language, minimum SDK and build toolchain after inspecting the target device and current project constraints.
- [ ] T0.4 Add reproducible build and signing instructions without committing secrets.

## T1 — Android shell and local data
- [ ] T1.1 Create native Android project with a clean, premium, responsive mobile UI.
- [ ] T1.2 Establish navigation around Inbox, Discover, Matches, Contacts and Activity.
- [ ] T1.3 Add Room database, schema migrations and repository layer.
- [ ] T1.4 Add local backup/export and restore.
- [ ] T1.5 Add universal capture: share URL/text into Trader, quick note and manual entry.
- [ ] T1.6 Add evidence records, source timestamps and verification states.
- [ ] T1.7 Add deduplication and merge review.

## T2 — Commercial records
- [ ] T2.1 Implement Organization and Person records.
- [ ] T2.2 Implement BuyerNeed with quantity, specification, target price, destination, timing and evidence.
- [ ] T2.3 Implement SupplierOffer with capability, capacity, MOQ, pricing basis, lead time, certifications and evidence.
- [ ] T2.4 Implement ServiceOpportunity and CareerOpportunity.
- [ ] T2.5 Implement contact consent, relationship, representation and commission notes.
- [ ] T2.6 Implement status history and activity log.

## T3 — Discovery and browser/app automation
- [ ] T3.1 Define source-adapter contract and permission boundaries.
- [ ] T3.2 Implement public-web search adapter with source attribution.
- [ ] T3.3 Implement browser capture and user-visible automation sessions.
- [ ] T3.4 Investigate Android intents, Custom Tabs, WebView and accessibility limitations; do not make Accessibility the sole automation foundation.
- [ ] T3.5 Add queue, pause/resume, retry, cancellation and failure inspection.
- [ ] T3.6 Add rate limits, robots/terms checks and source-specific policies.
- [ ] T3.7 Keep scraping targeted to relevant public information; no credential bypass or access-control circumvention.

## T4 — AI and matching
- [ ] T4.1 Define provider-neutral AI interface.
- [ ] T4.2 Implement deterministic extraction and normalization.
- [ ] T4.3 Add optional on-device model discovery and inference adapter.
- [ ] T4.4 Add optional external model adapters; paid credits must never be required for core flows.
- [ ] T4.5 Implement translation and structured extraction with confidence and evidence references.
- [ ] T4.6 Implement explainable buyer-to-supplier matching and contradiction checks.
- [ ] T4.7 Add human review for uncertain or high-impact claims.

## T5 — Outreach and deal execution
- [ ] T5.1 Generate personalized drafts from verified facts only.
- [ ] T5.2 Implement Android share sheet and email handoff.
- [ ] T5.3 Compare viable free Unipile alternatives channel by channel.
- [ ] T5.4 Add explicit recipient/content/channel approval before sending.
- [ ] T5.5 Track sent, failed, replied and manually confirmed states accurately.
- [ ] T5.6 Add follow-up reminders and deal pipeline.
- [ ] T5.7 Create buyer/supplier introduction dossier and commission/terms checklist.

## T6 — Pilot and verification
- [ ] T6.1 Start with industrial tooling and machining between Türkiye and Poland/EU.
- [ ] T6.2 Build first manually verified buyer and supplier records.
- [ ] T6.3 Validate one real buyer requirement and supplier fit.
- [ ] T6.4 Run approved outreach and record outcomes.
- [ ] T6.5 Verify the full capture → qualify → match → draft → approve → contact → follow-up workflow on device.
- [ ] T6.6 Record APK hash, build commit, test evidence and known issues.

## Current execution rule
Do not start broad scraping, bulk outreach, cloud sync or a large AI integration before the local data model and evidence workflow are reliable. Deliver one complete vertical slice at a time.
