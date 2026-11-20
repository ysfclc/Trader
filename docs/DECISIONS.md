# Trader — Architecture Decision Records

Decision log. New entries are append-only; do not silently rewrite an earlier decision. A reversal requires a new decision with rationale and migration impact.

## D-001 — Trader is a fresh, independent repository
**Date:** 2026-11-17  
**Status:** Accepted

The sole source of truth is `ysfclc/Trader`. The prior temporary documentation branch in Outbound Sourcer remains historical reference only. Do not continue implementing Trader there.

**Reason:** The product deserves a clean project boundary, clean Android identity and a canonical ledger. The repository was confirmed empty before initialization.

## D-002 — Android-first and local-first
**Date:** 2026-11-17  
**Status:** Accepted

Native Android is the primary client. Local database, evidence, matching, tasks and drafts work without a hosted backend or paid API.

**Reason:** The user's working environment and product intent are mobile-centered. Local-first operation reduces cost and makes data ownership explicit.

## D-003 — Memo is not a runtime dependency
**Date:** 2026-11-17  
**Status:** Accepted

Trader has its own app shell, workflow engine, persistence, browser surface, automation service, AI adapter and approval system. Reuse only small verified concepts or independently reviewed utilities.

**Reason:** Memo's path has become unreliable for the user. The current repository audits also show material automation gaps; Trader cannot inherit unverified capability claims.

## D-004 — No required Unipile replacement
**Date:** 2026-11-17  
**Status:** Accepted

No single provider is required for inbox sync. Channel capability is implemented individually: email APIs where authorized, Telegram Bot API, WhatsApp share/deep-link handoff, LinkedIn visible browser/app handoff and other supported Android intents.

**Reason:** There is no assumed free, safe drop-in replacement for every personal inbox operation. Channel operations are distinct capabilities.

## D-005 — Pluggable, evidence-preserving discovery
**Date:** 2026-11-17  
**Status:** Accepted

Public website/API/file/browser/manual sources implement a Trader-owned adapter contract. Raw evidence is stored before normalization or AI enrichment.

**Reason:** Broad discovery requires many source types, variable access policies and transparent failures. A universal scraper would be fragile and non-compliant.

## D-006 — Demand and supply are different domain objects
**Date:** 2026-11-17  
**Status:** Accepted

BuyerRequirement, SupplierOffer, Product, Match, Quote and Deal are separate records. Services and jobs have their own records while sharing organizations, people, source evidence and tasks.

**Reason:** Buyer requests and supplier claims have different fields, validation rules and lifecycles.

## D-007 — Human-approved external actions
**Date:** 2026-11-17  
**Status:** Accepted

Any consequential external communication or commitment requires approval bound to the exact recipient, channel and payload.

**Reason:** Prevent accidental sends, false representation, unsafe promises and irreversible actions.

## D-008 — AI is optional, bounded and untrusted
**Date:** 2026-11-17  
**Status:** Accepted

Local rules work without AI. Optional models help extract, translate, classify and draft, but every result is schema-checked and source-grounded.

**Reason:** No hard dependence on credits, and no model-generated content should become a commercial fact by default.

## D-009 — First vertical is industrial tooling and machining, not a hard-coded market
**Date:** 2026-11-17  
**Status:** Accepted

The first pilot tests industrial cutting tools and machining-related products between Türkiye and Poland/EU. Domain logic remains category- and geography-neutral.

**Reason:** Public demand/supply signals and the user's manufacturing background make this a grounded first validation target.

## D-010 — One complete vertical slice before broad automation
**Date:** 2026-11-17  
**Status:** Accepted

Build capture → evidence → normalization → transparent match → review → draft → user-controlled handoff before expanding to large-scale crawling, inbox sync or autonomous app operation.

**Reason:** A small end-to-end commercial outcome is more valuable than many partial integrations.

## D-011 — Reimplement patterns; do not fork existing products
**Date:** 2026-11-17  
**Status:** Accepted

Trader owns its own Android app, domain, storage, source adapters, browser/app automation and workflow code. Existing repositories are reference implementations only. No required module, database, service or runtime from Memo, EBO, VaultOS, BusyBee or Outbound Sourcer is imported.

**Reason:** Those projects solve different domains and carry different maturity gaps. A fresh, testable boundary prevents inherited bugs and architecture coupling. Exact reuse boundaries are recorded in `docs/REUSE_AUDIT.md`.

## D-012 — Evidence-first source ingestion
**Date:** 2026-11-17  
**Status:** Accepted

Every capture stores the original source/evidence before normalization, AI enrichment or matching. Derived claims retain source IDs and verification state. Unknown, contradicted, expired and human-verified facts remain distinct.

**Reason:** A trade decision depends on technical and commercial facts; normalized data without traceable evidence is not sufficient.

## D-013 — CI signing is separate from source
**Date:** 2026-11-17  
**Status:** Accepted

Trader will use a new application ID and CI-only signing secrets. Never copy a keystore, signing identity or private build material from VaultOS, BusyBee, Memo or another project into Trader.

**Reason:** Application identity and release-update continuity must be isolated. Signing material must not be committed to source control.


## D-014 — Trader is a general problem-solving system with specialized opportunity workflows
**Date:** 2026-11-17  
**Status:** Accepted

Trader's primary purpose is to help the operator move problems and goals from fragmented context to evidence-backed options, a workable plan, authorized execution and verified outcomes. Trade, services and careers are initial specialized workflows, not the exhaustive product scope. A universal ProblemCase and capability/plan model must exist independently of those commercial lanes.

**Reason:** The user's stated need is broader than trading or lead generation. The recurring difficulty is converting scattered information, skills, contacts, tools and opportunities into reliable progress and income. A trading-only architecture would repeat the mistake of narrowing the product around one monetization path.

## D-015 — No promise of universal autonomous problem resolution
**Date:** 2026-11-17  
**Status:** Accepted

Trader must accept problems from outside its current specialized workflows, but it must clearly state whether it can solve, partially assist, or only organize/research a case. It cannot promise to resolve every problem or replace qualified professional judgment, external authority, money, consent or real-world action. Every automated capability must declare its permissions, limits, side effects and verifiable postconditions.

**Reason:** Broad usefulness requires extensible intake and orchestration, not pretending the app has universal access or authority.


## Decision — QRKey reuse boundaries

**Date:** 2026-11-17  
**Status:** Accepted

Trader will reuse engineering principles from QRKey's product and automation repositories, not its code or runtime. Adopt idempotent side-effect keys, append-only event history, deterministic workflow routing, bounded context loading, pre-flight state checks, approval gates, cancellation, timeouts and explicit recovery states. Do not import the Node/Claude development-agent pipeline, web product stack or treat web QA automation as Android app automation.

**Reason:** QRKey's most useful automation system is designed to orchestrate software-development agents and repository tasks. It is not a consumer mobile automation engine. Trader needs an independent Android runtime with explicit capability contracts and user-visible execution.


## D-016 — Portable encrypted workspace backup with explicit replacement semantics
**Date:** 2026-11-19  
**Status:** Accepted

Trader's local workspace backup is a user-triggered, portable `.traderbackup` file. The snapshot contains all seven current Room record groups and is encrypted with AES-256-GCM. A PBKDF2-HMAC-SHA256 key is derived from a user passphrase using a random salt; the random IV and versioned header are authenticated as GCM additional authenticated data. The passphrase is not persisted by the backup manager.

Restore validates the authenticated payload, supported file/database schema, identifiers, relationships and ordered-step uniqueness before replacing all workspace rows in one Room transaction. Restore is intentionally **replacement, not merge**, and the UI requires explicit destructive-restore acknowledgement. A lost passphrase cannot be recovered. Android preferences and files outside the current Room workspace are not included.

**Reason:** A local-first product needs a user-owned recovery path without a cloud account or paid service. Atomic replacement and authenticated encryption reduce corruption and tampering risk, while the explicit replace semantics prevent a misleading merge promise.

**Consequences:** Keep the format version and database schema version explicit. Every Room schema change must include backup compatibility/migration tests before release. Device acceptance remains a separate release gate; CI passing does not prove document-picker or real-device restore behavior.

## D-017 — Conservative recovery for interrupted workflow steps
**Date:** 2026-11-19  
**Status:** Accepted

On app startup, persisted workflows left in RUNNING are reconciled transactionally. If any step was RUNNING, those steps and the run move to NEEDS_REVIEW, the previous step approval is cleared, and an append-only event records that the side effect may have happened. If no step was RUNNING, the run moves to PAUSED. The operation is idempotent.

**Reason:** Process death can occur after an action starts but before its outcome is persisted. Automatically replaying that step could duplicate an external or otherwise consequential action. Pausing between steps preserves safe resumability; an ambiguous in-flight step requires human review.

**Consequences:** This is recovery bookkeeping only, not a WorkManager executor or automatic resume mechanism. Any future worker must use the same conservative ambiguity rule, persist checkpoints and verify postconditions before recording completion.


## D-018 — Every registered capability must carry a typed execution contract
**Date:** 2026-11-19  
**Status:** Accepted

Each registered capability declares typed input and output fields, required/optional and sensitive attributes, availability, permission/consent requirements, effect and side effects, retry/approval rules, cost disclosure, limit mode and a required postcondition with a verification method. Registry-level validation rejects missing required values, wrong primitive/container types and undeclared fields.

**Reason:** An executor must not infer its input shape, cost, operational limits or definition of success from a free-form workflow title. A contract makes the boundary inspectable before an adapter is implemented.

**Consequences:** The generic validator does not replace adapter-specific semantic or policy checks. Adapter-backed capabilities remain NOT_IMPLEMENTED until the selected adapter declares concrete applicable byte/time/rate/concurrency ceilings, enforces its source/permission/consent policy and verifies postconditions. Unknown provider cost must be shown and explicitly consented to; remote model context transmission is a separate data-disclosure effect.


## D-019 — Keep initial opportunity tracking as an optional one-to-one ProblemCase profile
**Date:** 2026-11-19  
**Status:** Accepted

The initial mobile opportunity workflow uses one optional OpportunityRecord per ProblemCase. It supports the trade, service, career and other lanes with a manually entered organization label, opportunity name, contact label/route, user-authored fit note, stage, next action and local follow-up date. It is not a canonical Organization/Person graph, buyer requirement, supplier offer, deal or consent record.

**Reason:** The first practical use case needs one compact place to move a captured real-world lead from discovery to qualification, contact and follow-up while preserving its source context. Forcing every ProblemCase to become an opportunity would violate the universal problem model; building the complete commercial graph before validating the workflow would overbuild the first slice.

**Consequences:** User explicitly opts into opportunity tracking. Create/update/stage changes are recorded in ProblemCase activity. Contact route opens only a browser, email composer or dialer after explicit user action; Trader does not send/place calls. Follow-up dates are displayed but do not trigger notifications. Room v3→v4 adds the optional profile, and schema-v3 encrypted backups remain restorable with no opportunity records. Normalize organizations/people and move to multiple opportunities per case only when a validated use case justifies the schema change.


## D-020 — Discovery-first experience and Trader-owned browser
**Date:** 2026-11-20  
**Status:** Accepted

Trader's next product phase prioritizes an automated, evidence-backed company discovery and outreach-preparation experience. Discovery starts from a user-selected real ProofAsset (product, working project, service capability, domain skill or documented result), derives editable problem signatures, and searches permitted public sources for companies showing relevant intent or need. Results become source-linked company dossiers with explainable fit, unknowns and an individualized outreach path. Generic industry lists, ungrounded AI scoring and bulk outreach are not acceptable substitutes.

Trader will provide its own visible in-app browser. It is a runtime surface that can be opened from discovery, a company dossier or a workflow; it is not a link-out masquerading as a browser and not a Memo dependency. Browser interaction must be observable, interruptible, origin-aware and capable of preserving evidence. Consequential external submissions remain separate, exact-payload-approved actions with observable postconditions.

The primary experience is organized around Discover, Work, Live and Studio. Inbox, Pipeline and Activity become useful views within that workspace rather than defining the whole product. Browser and app sessions appear contextually in Live and may expand to a full-screen runtime. Existing local records, package identity, database and backup compatibility must be preserved through navigation and schema changes.

**Reason:** The current Inbox/Pipeline/Activity experience makes Trader look like a manual tracker and does not expose the user's priority workflow: find companies that already have relevant problems, understand the evidence, approach them through the right channel and verify the outcome. Memo's persistent context, visible execution and takeover patterns are useful references, but Trader owns its own product architecture, runtime and data.

**Consequences:** The first genuinely useful release must complete ProofAsset → DiscoveryStrategy → real permitted source results → deduplicated candidate dossier → evidence-backed fit → Trader browser research → individualized outreach draft → approved handoff/verified outcome. Do not ship seeded results, fake connected channels or browser link-outs as completion.


## D-021 — Local AI is Trader's intelligence backbone
**Date:** 2026-11-20  
**Status:** Accepted

Trader adopts Memo-2's verified architectural patterns—model roles/lifecycle, local inference, bounded context packets, structured planning, routing, persistent context and checkpointed execution—but implements and owns them independently. Local inference is the default intelligence path for discovery strategy, query planning, evidence extraction, qualification, research and outreach preparation. Models are task-routed; one model may serve multiple roles. Model installation is optional and never forced at first launch. The first runtime targets LiteRT-LM .litertlm models with CPU correctness before accelerator investigation. External AI is explicit escalation only, with preview/redaction of the context packet and no silent fallback. Model output is untrusted and cannot grant capabilities or bypass exact approval gates.

**Reason:** Trader needs to automate the actual opportunity-finding and research loop. A chatbot or AI decoration would not solve this. Local-first inference keeps private workspace context on-device and avoids mandatory cloud credits.

**Consequences:** model catalog/runtime, Context Engine, memory, structured orchestrator and AI-backed discovery become ordered implementation milestones with real inference and device acceptance. See `docs/LOCAL_AI_ARCHITECTURE.md`. Memo remains a reference, not a dependency.
