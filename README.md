# Trader

**Bring a problem. Find a path. Do the work. Verify the result.**

Trader is one independent Android-first, local-first workspace for turning captured information, needs and opportunities into evidence-backed work and verified outcomes. It is not a collection of separate mini-products, a generic CRM, a public marketplace, a trading terminal or an unrestricted autonomous agent.

## One product, one underlying graph

Every workflow uses the same connected records and lifecycle:

**Problem / goal → source evidence → organizations and people → requirements and offers (when relevant) → comparison and options → human decision → approved next action → observed result → activity history.**

A record is created once and reused across the relevant views. Source facts, user statements, model suggestions, assumptions and unknowns remain distinguishable. AI may help extract, translate, classify and draft; deterministic rules and the operator own decisions. External communication or commitments always require explicit user action.

## First complete workflow: Industrial Sourcing Desk

The first commercial workflow is a focused operator-controlled sourcing desk for Türkiye ↔ Poland/EU industrial tooling and machining.

It takes a real buyer requirement through supplier-offer capture, source-linked evidence, deterministic comparison, a reasoned human shortlist decision and a shareable brief. It does not certify suppliers, infer technical equivalence, send RFQs, promise representation or make commercial commitments.

The workflow is a specialization of Trader's shared problem/evidence/work graph, not a separate product. Do not start another product lane until this workflow is accepted on the target device.

Specification: [Industrial Sourcing Desk](docs/PRODUCTS/INDUSTRIAL_SOURCING_DESK.md).

## How the current app fits together

These are views and capabilities over the same workspace, not separate products:

- **Inbox** captures a problem, goal or incoming context.
- **Discover** runs bounded public-source research and saves findings as unreviewed evidence.
- **Companies / dossiers** hold organization details, buyer requirements, supplier offers and their source evidence.
- **Sourcing** compares buyer requirements with supplier offers and records the operator's decision.
- **Pipeline** is an operational view over work and opportunities, not a second data silo.
- **Activity** is the shared history of changes and decisions.
- **Browser** is the visible research and evidence-capture surface.
- **AI** is an assisting capability, not a separate chatbot product or a prerequisite for core workflows.

The intended longer-term navigation is Discover / Work / Live / Studio. That is a consolidation of these existing views, not permission to add more destinations or duplicate records. Navigation changes must preserve local data and deep links.

## Current verified boundary

See [the state ledger](docs/STATE.md) for the authoritative current build and task status.

- Android package: `com.yusufculcu.trader.debug`; persistent Trader development signing is configured. Keep the package and signing certificate unchanged and increment versionCode for every installable update.
- Latest verified installable build: `0.11.0-debug`, versionCode 11, CI run [45598311208](https://github.com/ysfclc/Trader/actions/runs/45598311208), artifact [15014108093](https://github.com/ysfclc/Trader/actions/runs/45598311208/artifacts/15014108093). The run passed 157 unit tests, lint, APK assembly, exact package/version checks and persistent-certificate equality. Supplier-offer editing and evidence-link reconciliation are included.
- Room schema v12 and encrypted backup snapshot v9 are implemented. Physical-device migration, export/restore and install-over-existing acceptance remain open.
- Discover is a bounded, user-triggered search/feed workflow, not continuous monitoring. Live-source and device acceptance remain open.
- Workflow state and approvals are persisted, but execution is still manually driven. There is no durable WorkManager capability executor.
- The model catalogue/download foundation exists, but verified on-device inference and real-device performance acceptance are not complete. Core sourcing must remain usable without an installed model.
- The Industrial Sourcing Desk is not yet accepted as reliable daily-use software until the real-device completion gate passes.

## The only active delivery sequence

1. On the Redmi Note 12 Pro 5G, export a fresh encrypted workspace backup before installation, then install `0.11.0-debug` over the existing app and verify the Room migration without uninstalling or clearing storage. If the installed APK predates the persistent-signing transition, stop rather than force an update; export and recover data before any uninstall.
2. Complete the real sourcing journey: buyer dossier and requirement; supplier dossier, offer and source evidence; multiple-offer comparison; decision and revision; shortlist share; edit headers, line items and evidence links; inspect Activity; close/reopen; encrypted export and restore.
3. Fix every observed regression and repeat the affected acceptance checks. Add recoverable deletion for important sourcing records before expanding the commercial data model.
4. Only after this gate passes, integrate discovery and optional local AI into the same sourcing journey. AI may propose extracted fields and questions; it must not replace evidence review or deterministic comparison.

Do not start Product/SKU, Match, Quote, Deal, social connectors, autonomous outreach or another product specialization in parallel with this acceptance work.

## Product and engineering laws

1. One canonical repository: `ysfclc/Trader`. The repository and its current state ledger are the source of truth.
2. One connected data graph; do not duplicate records or create parallel mini-products.
3. Evidence before claims. Unknown remains unknown.
4. AI proposes; validated code and the operator decide.
5. No external action without visible scope and explicit user approval.
6. Free-first, local-first and replaceable adapters. No dependency on Memo, EBO, n8n, Unipile, paid AI or a hosted CRM.
7. No fake progress: attempted, completed and verified are different states.
8. CI success is not device acceptance. A screen or source file is not proof that a workflow works.
9. Preserve the fresh-start ledger, user decisions, regressions and acceptance evidence. Never silently drop scope.
10. Work one complete vertical slice at a time; do not widen scope before the current gate passes.

## Canonical documents

- [Project lock](docs/PROJECT_LOCK.md) — product intent and non-goals
- [State ledger](docs/STATE.md) — current implementation, verification and next actions
- [Task ledger](docs/TASKS.md) — ordered implementation backlog
- [Roadmap](docs/ROADMAP.md) — phase gates
- [Architecture](docs/ARCHITECTURE.md) — system boundaries and layers
- [Product experience](docs/PRODUCT_EXPERIENCE.md) — unified interaction model
- [Industrial Sourcing Desk](docs/PRODUCTS/INDUSTRIAL_SOURCING_DESK.md) — first workflow and completion gate
- [Data model](docs/DATA_MODEL.md) — records and relationships
- [Operating rules](docs/OPERATING_RULES.md) — safety, evidence and approval
- [Workflow runtime](docs/WORKFLOW_RUNTIME.md) — durable lifecycle and current executor limitations
- [Capability registry](docs/CAPABILITY_REGISTRY.md) — declared capabilities and availability
- [Local AI architecture](docs/LOCAL_AI_ARCHITECTURE.md) — model, context and orchestration direction
- [Android build and signing](docs/ANDROID_BUILD.md) — toolchain and update safety
- [Coverage matrix](docs/COVERAGE.md) — requirement-to-test traceability
- [Risk register](docs/RISKS.md) — known product and engineering risks
- [Changelog](CHANGELOG.md) — dated implementation and verification history
