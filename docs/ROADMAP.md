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

Exit gate: capture and manage a real non-commercial problem and a commercial opportunity, preserve context/evidence, edit/search/archive/restore cases, share a useful context packet, retrieve it offline, and export/restore an encrypted workspace backup; no account, paid AI or cloud required. Backup/restore is CI-verified (PR #8 and main run 256); physical-device acceptance of the workspace and document-picker backup flow remains open.

## Phase 2 — Context, company discovery and qualification

Priority correction: deliver a proof-asset-led company discovery journey before expanding manual pipeline features. The first prospecting method searches for public evidence that a company is already experiencing, discussing, procuring or hiring to solve a problem the user's real product/service/skill can address. Do not start from generic industry lists or bulk outreach.
- [x] Package a ProblemCase, plan and evidence into an explicit user-shareable context packet for assistant handoff; embedded AI and automatic context-return remain open.
- [ ] Build a capability registry for discovery, parsing, browser, app handoff, repository and analysis operations; every capability declares limits and postconditions.
- [ ] Resolve a ProblemCase into a structured context packet, missing-information questions and evidence-backed candidate paths.
- [ ] Build documented source adapters for public web search, public company pages, directories, public tenders/RFQs and user-authorized browser sessions; enable each only after access, terms, scope, cost and rate limits are explicit.
- [ ] Create versioned ProofAsset and DiscoveryStrategy records; derive editable problem signatures, target constraints, query plans and disqualifiers.
- [ ] Persist bounded DiscoveryRun/SourceRun state with caching, cancellation, source health and failure reporting.
- [ ] Build normalized Organization/Person dossiers with source-linked claims, identity-resolution review and reversible deduplication.
- [ ] Explain fit against selected proof assets, including supporting/contradicting evidence and unknowns.
- [ ] Add a Trader-owned in-app browser with normal browsing fundamentals, persistent tabs/session controls, evidence capture and visible automation takeover/pause/stop.
- [ ] Make every adapter report limits, errors, timestamps and source provenance.
- [ ] Extract structured requirements and offers using deterministic parsing first.
- [ ] Add optional local AI adapter for translation, extraction, classification and drafting.
- [ ] Build qualification checklists and evidence-backed buyer/supplier profiles.

Exit gate: a user can investigate a real ProblemCase, distinguish facts from unknowns, and produce evidence-backed candidate paths. A trade case must also be capable of buyer/supplier qualification with traceable evidence.

## Phase 2a — Local intelligence foundation
- [x] Accept Trader-owned local AI architecture based on Memo-2 patterns: model roles, context packets, routing, memory and bounded structured planning.
- [x] Add initial pinned model catalog, LiteRT-LM CPU inference adapter with SHA verification, bounded evidence-aware context packet, resumable verified downloads, storage/RAM preflight, Model Hub screen and real inference smoke-test action (build/device verification pending).
- [ ] Model Hub with verified install/import/download, resume, storage/RAM preflight and diagnostics.
- [ ] Context Engine and provenance-aware local memory.
- [ ] Structured plan validation, capability routing and persisted orchestrator runs.
- [ ] Connect inference to real source discovery and company dossier qualification.
- [ ] Connect research agent to Trader browser with visible state, capture, pause/takeover and postcondition verification.
- [ ] Optional external model escalation with user-visible context boundary.

Exit gate: a compatible model is installed and verified on-device; Trader performs actual local inference; generated structured plans are validated; one real discovery and dossier workflow is powered by local AI; errors and resource limits are surfaced.

## Phase 3 — Matching and commercial workflow

- [ ] Produce individualized outreach drafts grounded in verified company facts and a selected proof asset; flag every unsupported claim.
- [ ] Show real channel capabilities (read/draft/handoff/send/reply/verify) and do not conflate app launch with a completed send.
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

Exit gate: capture and manage a real non-commercial problem and a commercial opportunity, preserve context/evidence, edit/search/archive/restore cases, share a useful context packet, retrieve it offline, and export/restore an encrypted workspace backup; no account, paid AI or cloud required. Backup/restore is CI-verified (PR #8 and main run 256); physical-device acceptance of the workspace and document-picker backup flow remains open.

## Phase 2 — Context, company discovery and qualification

Priority correction: deliver a proof-asset-led company discovery journey before expanding manual pipeline features. The first prospecting method searches for public evidence that a company is already experiencing, discussing, procuring or hiring to solve a problem the user's real product/service/skill can address. Do not start from generic industry lists or bulk outreach.
- [x] Package a ProblemCase, plan and evidence into an explicit user-shareable context packet for assistant handoff; embedded AI and automatic context-return remain open.
- [ ] Build a capability registry for discovery, parsing, browser, app handoff, repository and analysis operations; every capability declares limits and postconditions.
- [ ] Resolve a ProblemCase into a structured context packet, missing-information questions and evidence-backed candidate paths.
- [ ] Build a configurable source manager and adapter registry. Source instances (feed URLs, communities, company pages, registries and user-selected groups) are data, not hardcoded providers.
- [ ] Deliver public RSS/Atom feeds and one-shot public search as the first working discovery inputs, with user-triggered refresh, source health and evidence-backed save.
- [ ] Expand to public registers/procurement, company/career pages, job/project marketplaces, developer/product ecosystems, reviews, federated social networks and authorized communities using the common adapter contract.
- [ ] Add Reddit OAuth only after app registration and deletion/retention controls; add Bluesky/Mastodon public adapters with provider-specific constraints; keep Facebook groups browser-assisted unless formal API access is approved; support Telegram/Discord/Slack only with explicit group/workspace authorization.
- [ ] Create versioned ProofAsset and DiscoveryStrategy records; derive editable problem signatures, target constraints, query plans and disqualifiers.
- [ ] Persist bounded DiscoveryRun/SourceRun state with caching, cancellation, source health and failure reporting.
- [ ] Build normalized Organization/Person dossiers with source-linked claims, identity-resolution review and reversible deduplication.
- [ ] Explain fit against selected proof assets, including supporting/contradicting evidence and unknowns.
- [ ] Add a Trader-owned in-app browser with normal browsing fundamentals, persistent tabs/session controls, evidence capture and visible automation takeover/pause/stop.
- [ ] Make every adapter report limits, errors, timestamps and source provenance.
- [ ] Extract structured requirements and offers using deterministic parsing first.
- [ ] Add optional local AI adapter for translation, extraction, classification and drafting.
- [ ] Build qualification checklists and evidence-backed buyer/supplier profiles.

Exit gate: a user can investigate a real ProblemCase, distinguish facts from unknowns, and produce evidence-backed candidate paths. A trade case must also be capable of buyer/supplier qualification with traceable evidence.

## Phase 2a — Local intelligence foundation
- [x] Accept Trader-owned local AI architecture based on Memo-2 patterns: model roles, context packets, routing, memory and bounded structured planning.
- [x] Add initial pinned model catalog, LiteRT-LM CPU inference adapter with SHA verification, bounded evidence-aware context packet, resumable verified downloads, storage/RAM preflight, Model Hub screen and real inference smoke-test action (build/device verification pending).
- [ ] Model Hub with verified install/import/download, resume, storage/RAM preflight and diagnostics.
- [ ] Context Engine and provenance-aware local memory.
- [ ] Structured plan validation, capability routing and persisted orchestrator runs.
- [ ] Connect inference to real source discovery and company dossier qualification.
- [ ] Connect research agent to Trader browser with visible state, capture, pause/takeover and postcondition verification.
- [ ] Optional external model escalation with user-visible context boundary.

Exit gate: a compatible model is installed and verified on-device; Trader performs actual local inference; generated structured plans are validated; one real discovery and dossier workflow is powered by local AI; errors and resource limits are surfaced.

## Phase 3 — Matching and commercial workflow

- [ ] Produce individualized outreach drafts grounded in verified company facts and a selected proof asset; flag every unsupported claim.
- [ ] Show real channel capabilities (read/draft/handoff/send/reply/verify) and do not conflate app launch with a completed send.
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
