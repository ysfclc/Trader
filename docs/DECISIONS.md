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
