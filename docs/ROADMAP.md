# Trader Roadmap

This roadmap is ordered by real problem outcomes, evidence and execution reliability, not feature count. Commercial proof is one important outcome, not the only acceptance target. Each phase must leave a usable, testable result.

## Phase 0 — Product lock
- [x] Establish Trader as an independent Android-first product.
- [x] Define trade, services and careers as initial specialized opportunity workflows, not the product boundary.
- [x] Lock a universal ProblemCase → evidence → options → plan → approved execution → verified outcome lifecycle.
- [x] Define evidence, consent, human approval and free-first requirements.
- [ ] Keep repository ledger and acceptance criteria current.

Exit gate: product vision, architecture, data model and operating rules agree.

## Phase 1 — Local problem workspace
- [ ] Create Android project, build configuration and repeatable signed debug/release APK workflow.
- [x] Implement local database, migrations, and passphrase-protected encrypted backup/export plus validated transactional restore (AES-256-GCM; PBKDF2-HMAC-SHA256). CI/device acceptance is tracked separately.
- [ ] Add universal Problem Inbox for URLs, shared text, notes, screenshots, voice, documents and manually described problems/goals.
- [x] Preserve original input, desired outcome, constraints and source URI in a ProblemCase. Urgency, deadline and unresolved-question fields remain open.
- [x] Store source URL, capture time, raw capture and separate user-attached evidence with source reference and user-controlled verification state. Automated extraction/parser provenance remains open.
- [ ] Implement organizations, people, buyer needs, supplier offers, services and job opportunities as specialized records linked to a universal ProblemCase where relevant.
- [ ] Implement goals, plans, plan steps, capability requirements and verifiable completion criteria.
- [x] Add text search, active/archived filters, reversible archive/restore and activity trail. Deduplication remains open.

Exit gate: capture and manage a real non-commercial problem and a commercial opportunity, preserve context/evidence, edit/search/archive/restore cases, share a useful context packet, and retrieve it offline; no account, paid AI or cloud required. Device acceptance and backup/restore remain open.

## Phase 2 — Context, discovery and qualification
- [x] Package a ProblemCase, plan and evidence into an explicit user-shareable context packet for assistant handoff; embedded AI and automatic context-return remain open.
- [ ] Build a capability registry for discovery, parsing, browser, app handoff, repository and analysis operations; every capability declares limits and postconditions.
- [ ] Resolve a ProblemCase into a structured context packet, missing-information questions and evidence-backed candidate paths.
- [ ] Build source adapters for public web search, public company pages, directories and user-authorized browser sessions.
- [ ] Add Android browser automation and carefully scoped installed-app automation.
- [ ] Make every adapter report limits, errors, timestamps and source provenance.
- [ ] Extract structured requirements and offers using deterministic parsing first.
- [ ] Add optional local AI adapter for translation, extraction, classification and drafting.
- [ ] Build qualification checklists and evidence-backed buyer/supplier profiles.

Exit gate: a user can investigate a real ProblemCase, distinguish facts from unknowns, and produce evidence-backed candidate paths. A trade case must also be capable of buyer/supplier qualification with traceable evidence.

## Phase 3 — Matching and commercial workflow
- [ ] Match buyer requirements to supplier capabilities with explainable scoring.
- [ ] Surface missing facts, incompatibilities and questions instead of fabricating fit.
- [ ] Add side-by-side comparison, shortlist and introduction dossier.
- [ ] Track consent, representation, commission terms, contact history and deal stages.
- [ ] Generate tailored outreach in Turkish, Polish and English.
- [ ] Require explicit user approval before sending or making commitments.

Exit gate: produce a credible, reviewable introduction package for a real opportunity.

## Phase 4 — Communication adapters
- [ ] Implement email and Android share/intents first.
- [ ] Evaluate free/low-cost alternatives to Unipile per channel and region.
- [ ] Support user-operated browser/app workflows where permitted and technically reliable.
- [ ] Add sending only through authorized accounts, with preview, recipient confirmation and result logging.
- [ ] Add replies and follow-up capture without pretending unsupported channels are integrated.

Exit gate: at least one reliable, user-authorized communication path end-to-end.

## Phase 5 — Live commercial pilot
- [ ] Run a narrow Türkiye ↔ Poland/EU industrial tooling and machining pilot.
- [ ] Build a manually verified set of buyer needs and supplier capabilities.
- [ ] Contact a small, relevant set of prospects with individualized messages.
- [ ] Record replies, objections, quote requests, introductions and deal outcomes.
- [ ] Validate sourcing terms, delivery, quality, certification, customs/VAT and commission arrangements with qualified sources.
- [ ] Measure qualified matches and conversations, not scraped record volume.

Exit gate: evidence of genuine buyer interest and a repeatable path to a paid introduction or transaction.

## Phase 6 — Expand
- [ ] Add further product categories only after the pilot yields repeatable learning.
- [ ] Expand geography, sources and channel adapters.
- [ ] Add collaboration, optional encrypted sync and multi-device support.
- [ ] Add new problem-solving specializations only when repeated real cases justify their own domain rules and acceptance tests.
- [ ] Deepen supported browser, app, file and repository workflows without creating hidden or unbounded automation.
- [ ] Consider monetization only after a proven user workflow exists.

## Release rule
A phase is not complete because code compiles or a screen exists. It is complete only when its exit gate is demonstrated on a real device with test evidence and known limitations recorded.
