# Trader Product Roadmap

_Last reconciled: 2026-11-27 — canonical main, installable 0.14.0-debug, physical-device acceptance open._

## 1. Product north star

Trader is one Android-first, local-first, free-first system for turning a problem, goal, opportunity or useful piece of information into a verified outcome.

**Capture → understand the desired outcome and constraints → gather evidence/context → discover options, people and resources → plan → execute bounded steps with permission → verify what happened → retain useful learning.**

Trader is not a generic CRM, public marketplace, trading terminal, autonomous broker or unrestricted agent. Trade, services and careers are specialist workflows over one shared graph, not separate products. Income generation is an important outcome, not the only valid reason to use Trader.

Canonical intent and non-goals live in [PROJECT_LOCK.md](PROJECT_LOCK.md). The detailed interaction model and original product brainstorm live in [PRODUCT_EXPERIENCE.md](PRODUCT_EXPERIENCE.md). This roadmap sequences that scope; it does not replace it.

## 2. One product, one connected graph

A record is created once and reused across the relevant views:

**Problem / goal → source evidence → organizations and people → needs and offers (when relevant) → comparison/options → human decision → approved next action → observed result → activity history.**

The app should converge toward four primary workspaces:

- **Discover** — proof-asset-led research, source health, candidate organizations and new signals.
- **Work** — the persistent graph of problems, goals, organizations, people, evidence, requirements, offers and follow-ups.
- **Live** — active/recent runs, browser sessions, approvals, checkpoints, failures, cancellation and uncertain outcomes.
- **Studio** — editable dossiers, comparisons, shortlist briefs, research and other deliverables with source manifests and revisions.

Current Inbox, Pipeline, Sourcing, Activity, Browser and AI surfaces are capabilities/views over that same graph. Do not add a destination for every entity or create duplicate data silos. Navigation changes must preserve local records, back-stack behavior and deep links.

## 3. Current release baseline

**Release status:** installable candidate; physical-device acceptance still open.

- **Build:** `0.14.0-debug`, versionCode 14
- **Package:** `com.yusufculcu.trader.debug`
- **Canonical main commit:** `94c241d8971d624857295069635baee12c2ea436`
- **Approved main CI:** [run #626](https://github.com/ysfclc/Trader/actions/runs/46041691925)
- **Installable APK:** [artifact 15220676610](https://github.com/ysfclc/Trader/actions/runs/46041691925/artifacts/15220676610)
- **Verification:** 201 Android unit tests, 0 failures/errors/skips; coverage floor, clean build, lint, SBOM/OSV and persistent package/signature checks passed.
- **Room / backup:** schema v17; encrypted backup snapshot v13.
- **Signing:** persistent Trader development certificate is configured and CI-verified. Keep package ID and certificate unchanged; increment versionCode for every installable update.

This is a signed installable development APK. It is **not** proof that the app has passed a physical-device workflow or that every screen works correctly on the Redmi. CI is not product acceptance.

## 4. Active delivery gate — prove the current product on the Redmi

**This is the only active implementation priority. Do not start another feature lane until this gate passes.**

Target device: Redmi Note 12 Pro 5G, Android 14.

1. Export a fresh encrypted workspace backup and retain its passphrase independently.
2. Install `0.14.0-debug` over the existing Trader installation. Do not uninstall, clear storage or reset app data as an update strategy. Confirm package identity and that Android accepts the existing signing certificate.
3. Verify Room v16→v17 migration and preservation of existing companies, contacts, cases, evidence, sourcing records, fit assessments, aliases and review history. Verify preferences and browser login/session behavior separately; they are not all part of the encrypted workspace backup.
4. Exercise navigation and the real screens. Every visible control must respond without crashes, dead ends, stale state or misleading success messages.
5. Complete one real Industrial Sourcing Desk journey:
   - create/select a buyer company dossier;
   - capture a real buyer requirement and its line items;
   - create/select a supplier dossier;
   - capture a real supplier offer and line items with source evidence;
   - compare multiple offers and inspect supported, contradicted and unknown information;
   - record a reasoned shortlist / needs-information / not-suitable decision and revise it;
   - share a shortlist brief through Android Sharesheet;
   - inspect Activity, close/reopen the app and confirm the same records remain.
6. Export and restore encrypted snapshot v13 in a controlled test workspace. Never overwrite the only copy of real data. Confirm companies, people, source claims, products, requirements, offers, fit assessments, aliases and reviewer history survive.
7. Record every crash, broken control, data loss, incorrect status, confusing screen and manual workaround. Fix the actual defects and repeat the affected tests before adding features.

**Known navigation defect found in the source audit:** the Sourcing empty-state `Open Companies` action currently switches to Pipeline, while Pipeline initializes on Opportunities. The next code increment must route that action directly to Companies and add a regression UI test; the current 0.14.0 APK contains this defect. The user can reach Companies manually via Pipeline → Companies, but this workaround is not acceptance.

**Exit gate:** the operator can complete and repeat that journey on the target phone without crashes, data loss, misleading status or workarounds, and can recover the workspace from the encrypted backup. Until then, this phase remains open.

## 5. Product workstreams after device acceptance

### Workstream A — Complete the Industrial Sourcing Desk

**Already implemented and CI-verified:**
- company dossiers, company-scoped people/contacts, source snapshots and reviewable source-linked claims;
- buyer requirements and editable line items;
- supplier offers and editable line items, with company-scoped evidence links;
- company-owned Product/SKU catalogue;
- deterministic comparison rules and explicit human sourcing decisions;
- recoverable trash/restore for buyer requirements and supplier offers;
- shared Activity history;
- deterministic company-to-proof fit assessment with supporting, contradicting and unknown findings, plus separate append-only reviewer overrides;
- same-company contact duplicate suggestions, persistent aliases and reversible append-only identity decisions;
- English, Polish and Turkish UI; Room migrations and encrypted backup continuity.

**Still open:**
- physical-device acceptance of every step above;
- persistent Match, Quote and Deal records as separate business entities;
- contact basis/consent, representation authority and commission terms;
- a real manually verified buyer/supplier pilot and its actual outcomes;
- any supplier certification, quality, stock, availability, capacity or commercial fact that is not backed by a source.

No exact wording match is proof of technical equivalence. Missing data remains unknown. A supplier statement is not independent verification. Trader does not certify suppliers, send RFQs, accept orders or make commitments.

### Workstream B — Proof-asset-led discovery and company intelligence

The first prospecting method starts with something the operator can genuinely deliver: a working product, prototype, documented capability, service, industrial skill or verified result. It searches for evidence that an organization is already experiencing, discussing, procuring, funding or hiring to solve a relevant problem. It does not start from generic industry lists or bulk outreach.

**Already implemented and CI-verified:**
- ProofAsset and DiscoveryStrategy domain/persistence foundations;
- configurable source instances and adapter registry;
- bounded one-shot DuckDuckGo search and user-configured RSS/Atom feeds;
- user-triggered discovery runs with source-level status, failure isolation, cancellation and deduplication;
- persisted run/source/finding history and save-to-work as UNREVIEWED evidence;
- browser handoff and source provenance;
- deterministic contact duplicate candidates, saved aliases and reversible operator review.

**Next after the device gate:**
1. Make saved aliases additional review-only signals in candidate detection; never auto-merge contacts.
2. Build source-backed company/person profile extraction with claim-level source, timestamp, excerpt and extraction provenance.
3. Validate extracted claims deterministically and require operator review before they become supported facts.
4. Explain fit against the selected ProofAsset using verified facts, contradictions and explicit unknowns.
5. Validate live-source reliability, access mode, terms, rate limits and failure behavior before enabling any new adapter.

Discover is user-triggered, not continuous monitoring. No scraping behind login walls, credential bypass or unapproved community/group access.

### Workstream C — Local intelligence

Trader-owned local AI is an assisting capability, not a chatbot product and not a prerequisite for core sourcing.

**Implemented foundations:** pinned model catalogue and role metadata, LiteRT-LM adapter, bounded context packets with provenance/unknowns, candidate download size/hash verification, storage/RAM preflight and structured discovery draft validation.

**Still open:** verified model installation and inference on the Redmi, model resource/performance diagnostics, context redaction, provenance-aware memory, general task routing, durable inference runs and explicit consent-gated external-model escalation.

AI may extract, translate, classify, explain and draft. Deterministic code and the operator own decisions. Never invent qualifications, evidence, prices, capacity, technical equivalence or outcomes.

### Workstream D — Durable execution and visible automation

**Implemented foundations:** typed capability contracts, exact-payload approvals, persisted workflow runs/steps/events, conservative interruption recovery, cancellation uncertainty and manual workflow controls.

**Still open:** durable WorkManager execution, idempotency, retries, process-death recovery of real adapters, verified postconditions, visible browser task controls, pause/stop/takeover and supported app-specific integrations.

A capability must remain unavailable until its adapter declares and enforces permissions, data disclosure, numeric limits, timeouts, side effects, cancellation and postconditions. Never imply that a workflow screen means automation is running.

### Workstream E — Communication and deal execution

**Not yet implemented as a sending integration:** verified-fact outreach drafts, channel capability matrix, exact recipient/channel/purpose/payload approval, authorized send adapters, confirmed-sent/failed/uncertain/replied states, reply capture, durable reminders and follow-up.

Start with safe user-operated Android share/email handoff. A handoff is not a confirmed send. No generic bulk outreach, autonomous messages, calls, quotes, promises or commercial commitments.

### Workstream F — Broader problem-solving and expansion

After the first commercial workflow is accepted, extend the same graph to services, careers and non-commercial problems. Add user-selected documents, screenshots and voice capture one modality at a time with provenance and review. Deepen supported browser/app/file/repository capabilities without hidden or unbounded automation.

Only expand geography, source families, product categories, collaboration, sync or monetization when real repeated use justifies them. Core workflows must remain useful offline and must not require paid AI, hosted CRM or third-party automation services.

## 6. Product laws and release gates

1. Evidence before claims; facts, user statements, model suggestions, assumptions and unknowns stay distinguishable.
2. Unknown is not a pass. No fake percentage score or inferred technical equivalence.
3. AI proposes; validated code and the operator decide.
4. External actions require visible scope and explicit approval.
5. Preserve source records and provenance; identity decisions are reviewable and reversible.
6. Preserve local data, package identity and signing certificate across updates.
7. Work one complete vertical slice at a time.
8. Keep attempted, completed, verified and device-accepted states separate.
9. Do not call a feature complete because a screen exists or CI is green.
10. No new product specialization until the current release gate is demonstrated on the target device.

## 7. Canonical project records

- [PROJECT_LOCK.md](PROJECT_LOCK.md) — product intent, constraints and non-goals.
- [PRODUCT_EXPERIENCE.md](PRODUCT_EXPERIENCE.md) — original interaction and discovery brainstorm.
- [PRODUCTS/INDUSTRIAL_SOURCING_DESK.md](PRODUCTS/INDUSTRIAL_SOURCING_DESK.md) — first commercial workflow.
- [TASKS.md](TASKS.md) — implementation backlog and testable tasks.
- [STATE.md](STATE.md) — exact current implementation and verification state.
- [COVERAGE.md](COVERAGE.md) — requirement-to-code/test/device-acceptance traceability.
- [RISKS.md](RISKS.md) — active risks and mitigations.
- [ARCHITECTURE.md](ARCHITECTURE.md), [DATA_MODEL.md](DATA_MODEL.md), [OPERATING_RULES.md](OPERATING_RULES.md) and [LOCAL_AI_ARCHITECTURE.md](LOCAL_AI_ARCHITECTURE.md) — system boundaries and contracts.
