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

The first pilot tests industrial cutting tools and machining-related products between Türkiye and Poland/EU. Domain logic remains geography- and category-neutral.

**Reason:** Public demand/supply signals and the user's manufacturing background make this a grounded first validation target.

## D-010 — One complete vertical slice before broad automation
**Date:** 2026-11-17  
**Status:** Accepted

Build capture → evidence → normalization → transparent match → review → draft → user-controlled handoff before expanding to large-scale crawling, inbox sync or autonomous app operation.

**Reason:** A small end-to-end commercial outcome is more valuable than many partial integrations.
