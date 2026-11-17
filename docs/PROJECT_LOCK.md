# Trader — Project Lock

**Status:** LOCKED BASELINE  
**Date:** 2026-11-17  
**Repository:** `ysfclc/Trader`  
**Canonical branch:** `main`

This document records the product intent that governs design, implementation and prioritization. A new idea does not change the lock automatically. Change the product only through an explicit user decision and a recorded decision entry.

## 1. The actual problem

The operator's problem is not simply "how to trade". It is how to turn a broad set of existing resources into reliable progress and income without having to manually coordinate everything.

Those resources include manufacturing and CNC/mould knowledge; B2B sales and partner-management experience; a substantial Turkish manufacturer/supplier network and access to Poland/EU; automation, data, software-building and AI-integration skills; and existing products, repositories, research, contacts and partially solved workflows.

But useful information, needs, opportunities, tools and conversations are fragmented across websites, browser sessions, installed apps, files, messages, repositories and separate projects. The operator repeatedly has to discover context, capture it, understand it, compare options, formulate a plan, move between tools, perform actions, follow up and verify whether anything actually worked. Work and evidence can be lost between those steps.

The deeper product problem is:

**A person has problems to solve and resources to use, but lacks one reliable, controllable system that can connect context → evidence → options → plan → tools/actions → verified outcome.**

Income generation is an important outcome, but not the only valid reason to use Trader.

## 2. Product statement

Trader is intended to be a personal problem-solving and opportunity-execution system. The operator can bring it a problem, goal, opportunity or piece of information from the web, browser, installed apps, files, messages, voice or manual entry. Trader should help clarify what is known and unknown, find relevant evidence and resources, identify possible paths, break the work into steps, use appropriate tools and automations, request approval where needed, and record the result.

The general loop is:

**Capture the situation → understand the desired outcome and constraints → gather evidence and context → discover options, people, tools or resources → create a justified plan → execute bounded steps → verify the result → retain useful learning.**

The system must distinguish:
- a **problem** the operator wants to resolve;
- a **goal** or desired outcome;
- an **opportunity** that may create income, work or value;
- a **task/action** that advances a plan;
- a **fact, inference, assumption or unknown**;
- a **proposed action** versus an action that was actually completed and verified.

Trader must not force every incoming item into a trade lead, job lead or generic task. It should identify the appropriate workflow, or say honestly when it lacks a capability.

The first commercial loop remains important:

**Find demand ↔ find supply → preserve evidence → normalize requirements and offers → assess technical/commercial fit → establish authority and viability → prepare an introduction → obtain approval → use an authorized channel → follow up → record the outcome.**

This is one specialized workflow inside the broader problem-solving system, not the definition of the whole product.

## 3. Initial workflow specializations

Trade, services and careers are the first structured opportunity workflows. They share common source, organization, person, evidence, plan, task, communication and outcome infrastructure. They are not an exhaustive taxonomy of what Trader may help with.

A new workflow specialization may be added when a repeated problem needs distinct domain rules, data and acceptance tests. Do not create a new hard-coded category for every example.

### A. Trade — primary lane

- Buyer requirements and RFQs.
- Manufacturers, suppliers, distributors, wholesalers and importers.
- Products, SKUs, categories and technical specifications.
- Supplier catalogues, certificates and capability statements.
- Buyer/supplier matching with explainable fit and missing requirements.
- RFQ, quotation, MOQ, capacity, stock and lead time.
- Pricing, currency, freight, insurance, duties/taxes and landed-cost assumptions.
- Incoterms, payment terms, delivery, warranty and returns.
- Authorized introduction, representation and commission/success-fee agreements.
- Deal pipeline and follow-up.

Trader's first trade workflow is an introducer/sourcing tool for the operator. It does not assume the operator buys, stocks, imports, insures, certifies or resells goods. Any change to that commercial role requires a separate legal, financial and operational decision.

### B. Services — second lane

Find organizations with a demonstrated problem that could be addressed with the operator's existing software, automation, AI integration, data and operational-system skills.

Examples of solution evidence include a company's stated workflow problem, manual process, disconnected systems, hiring need, public RFP or explicit request for an implementation partner.

Do not create generic "AI leads". Connect a specific observed problem to a specific demonstrated capability and show what remains to be built.

### C. Careers — third lane

Find relevant vacancies, hiring organizations and public hiring contacts; compare requirements to the operator's actual experience; prepare truthful applications and outreach; track replies and follow-up.

Never invent qualifications, language proficiency, employment outcomes or customer results.

## 4. Target users and operating mode

**Initial user:** one operator using an Android phone, with an existing Turkish manufacturer/supplier network, Poland/EU commercial access and B2B experience.

**Primary client:** Android. The first supported device target is the operator's Redmi Note 12 Pro 5G. The project must still use standard Android APIs and avoid device-vendor assumptions.

**Languages:** English, Turkish and Polish are first-class from the beginning. Language is a per-record and per-message property; translated text never overwrites original source text.

**Operating mode:** local-first and free-first. Trader's core must remain useful offline for reviewing saved evidence, managing records, matching, drafting, tasks and deal tracking. Online discovery runs when the user requests it or schedules an allowed source job.

## 5. Product principles

- **Operator ownership:** the operator can inspect, export, correct and delete their own working records, subject to immutable audit/evidence retention rules and legal deletion obligations.
- **Evidence is durable:** preserve source URLs, timestamps, relevant excerpts or file references, raw values, parser versions and reviewer decisions.
- **No false equivalence:** a similar product name is not proof of technical equivalence.
- **Unknown is not pass:** absent certification, capacity, price, availability, country of origin or delivery capability stays unknown until verified.
- **Match explanation:** show which buyer requirements the supplier satisfies, which it does not satisfy and which cannot yet be assessed.
- **Human authority:** AI does not make commercial commitments, set a binding price, accept an order, promise delivery, assert agency or send an external message autonomously.
- **Replaceable capabilities:** sources, parsers, AI models, browser engines and communication channels are adapters; no vendor owns core data or business logic.
- **No forced SaaS:** do not introduce a server, cloud subscription or API fee unless a concrete capability requires it and the user approves the cost.
- **Real outcomes:** track introductions, replies, quotes, qualified opportunities and won/lost deals rather than vanity counts.

## 6. First market hypothesis

Start with industrial tooling, cutting tools and machining-related products between Türkiye and Poland/EU.

The initial research found:
- a public Polish buyer request for carbide milling cutters, welding tools, steel bars for machining and other industrial products;
- Turkish manufacturers with published carbide milling tool product ranges;
- Turkish steel suppliers with published alloy-steel bar ranges;
- the operator's direct familiarity with machining, moulds, manufacturing and industrial purchasing language.

This is a testable hypothesis, not a locked exclusive market. The schema, application and source system must be capable of finding opportunities in other categories.

## 7. What Trader is not

- Not Memo 2, Memo 1, a Memo plugin or a Memo rebrand.
- Not EBO rebuilt inside Android.
- Not an Unipile clone or a reverse-engineered social inbox product.
- Not an unattended crawler that ignores site rules.
- Not an auto-DM, spam or bulk-contact machine.
- Not a public multi-vendor marketplace in the first release.
- Not a claim that one app can autonomously solve every personal, legal, medical, financial or relationship problem.
- Not a generic life-dashboard whose screens collect unrelated data without helping complete a real problem.
- Not limited to trade simply because the working repository is named Trader.
- Not an importer of record, customs broker, payment processor, escrow service, quality certifier or legal adviser.
- Not a replacement for human product inspection, supplier due diligence or contract review.
- Not a promise that a deal, job or specific amount of income will result.

## 8. Success definition

The first meaningful milestone is not "the app builds". It is a working general problem loop: a real problem or goal can be captured from a supported source, its context and constraints can be preserved, evidence and unknowns can be distinguished, a useful plan can be produced, actions can be tracked, and completion can be verified.

The first commercial proof remains a fully traceable pilot in which Trader:
1. captures a real buyer request;
2. captures one or more real Turkish supplier offers;
3. retains source evidence and original language;
4. maps the buyer's requested attributes to supplier evidence;
5. highlights every missing technical/commercial condition;
6. records a qualified introduction route and the supplier's authority;
7. drafts a multilingual introduction for review;
8. hands off to a real user-controlled channel;
9. stores the actual reply and next action.

Longer-term success is repeatable deals and commercial relationships, not scraping volume.
