# Trader — Expanded Repository Audit (BOS and adjacent projects)

**Audit date:** 2026-11-17  
**Purpose:** Correct the earlier targeted audit, which omitted BOS and several highly relevant projects.  
**Scope:** Read repository trees, project/architecture/status documents, selected production source files, tests and available GitHub Actions history for the projects below. This is a targeted relevance audit, not a line-by-line audit of every file in all 43 pre-Trader repositories. No code has been copied into Trader.

## Executive decision

The earlier reuse audit was incomplete. **BOS is one of the most important references for Trader and should have been included from the start.** Oddo and Troy are also more directly relevant to the trade/discovery problem than several projects previously covered.

This does not mean Trader should become a BOS tenant or a Troy/Oddo fork. Trader remains a clean, native Android-first application with its own local domain and runtime. It should selectively implement proven ideas from these systems and keep optional integration seams open for later.

## 1. BOS — most important missing reference

Repository: `ysfclc/bos`, private, default branch `master`. Tree contained 1,262 files at audit time.

Inspected:
- `README.md`
- `BOS_Architecture_Brief.md`
- `docs/2-architecture/ARCHITECTURE.md`
- `docs/2-architecture/PRODUCT_ARCHITECTURE.md`
- `docs/2-architecture/CURRENT_STATE_AUDIT.md`
- `docs/2-architecture/INGESTION_CONTRACT.md`
- `docs/2-architecture/ADR-014-MULTI_SOURCE_INGESTION_CONTRACT.md`
- `docs/2-architecture/WORKFLOW_ENGINE.md`
- `docs/2-architecture/SECURITY_MODEL.md`
- `docs/2-architecture/ADR-038-CAPABILITY_LAYER_IS_THE_ONLY_AUTHORIZATION_AUTHORITY.md`
- `apps/api/src/events/events.service.ts`
- `apps/api/src/capability-engine/capability-executor.service.ts`
- `apps/api/src/capability-engine/capability-authorization.util.ts`
- `apps/api/src/capability-engine/capability-executor.service.test.ts`
- `apps/api/src/workflows/workflow-executor.service.ts`
- `apps/api/src/workflows/workflow-executor.service.test.ts`
- `docs/tasks/2_active/T-385-dev-login-jwt-carries-no-capabilities-capability-guard-reads.md`
- `.github/workflows/ci.yml`

### What is genuinely valuable for Trader

BOS is the strongest architectural reference for a business runtime:
- Deterministic domain mechanics first; AI is a scoped proposal worker, not an authority.
- Append-only event history, source provenance, correlation and audit.
- Entity/relationship graph thinking for companies, people, products, requirements, offers and deals.
- Capability contracts for side effects, including authorization, approval, execution and outcome recording.
- Source/connector manifests that describe what a provider can read, write, listen for and emit.
- Context packages that expose only task-relevant information to an AI worker.
- Separation between definitions/configuration and runtime instances.
- Signal-first workflow thinking: a source event should create a useful next action, not merely feed a generic send sequence.

The most useful BOS idea for Trader is its **ingestion contract**: retain the raw source payload, hash it, represent claims with source identity and observed time, then deterministically project current state. Conflicting facts should be visible and reviewable, not silently overwritten by whichever crawler ran last.

### What the actual status says — important caveats

Do not confuse BOS's substantial codebase or detailed architecture with a fully verified, production-safe runtime:
- `CURRENT_STATE_AUDIT.md` is a snapshot dated 2026-07-24 and explicitly distinguishes implemented, partial and absent. It reports real entity CRUD, relationships, events, audit, capability handlers and workflow definitions/executor, but also major AI context wiring and multi-agent-room gaps. It was subsequently corrected in places; it is not a current live test report.
- `INGESTION_CONTRACT.md` labels the generalized connector ingestion contract **NOT STARTED** in its status header. ADR-014 specifies event-sourced claims and deterministic projection, but that specification must not be mistaken for every connector being implemented.
- `WORKFLOW_ENGINE.md` labels the current executor **PARTIAL**: in-process execution, no durable retries/crash recovery/scheduler in that implementation, a 100-step ceiling, and a `new Function()` condition evaluator. The durable StepRunner is the intended replacement, not proof it has landed.
- ADR-038 documents a serious authorization split: a boolean guard cannot represent a pending approval, and direct controller writes can bypass the capability approval queue. The task ledger still contains T-385 as **ACTIVE** on 2026-09-22, with the one-commit migration to repoint six controllers and remove the old guard/decorator still owed in the inspected task file. Do not repair this by simply making the old guard permissive.
- `resolveCaller()` documents an identity/backfill gap in its own comments; the related migration and seed changes need live verification before relying on the path.
- The latest three observed BOS main/master CI runs in the Actions API were failures, most recently 2026-09-23 (run 35891883944). That is not proof the code is currently broken, but it is not a green CI baseline.

### Trader decision

**Adopt the architecture principles, not BOS as a dependency.** Reimplement a small Android-sized subset:
1. evidence/event ledger;
2. source registry and field-level provenance;
3. explicit capability/action contracts;
4. approval bound to exact recipient/content/intent;
5. durable, inspectable workflow state;
6. scoped AI context and redaction.

Do not import BOS's NestJS/Postgres/Redis/MinIO runtime, its current authorization implementation, or its current workflow executor into the initial Trader app. Keep a future optional BOS connector possible, but no runtime coupling.

## 2. Oddo — useful marketplace and dispatch domain, weak ingestion base

Repository: `ysfclc/oddo`, private, default branch `master`. Tree contained 965 files.

Inspected:
- `docs/superpowers/plans/2026-05-25-oddo-full-vision.md`
- `lib/services/ingestion-service.ts`
- `services/ingestion/whatsapp-listener.ts`
- `services/ingestion/telegram-listener.ts`
- `services/ingestion/extractor.ts`
- `app/api/internal/whatsapp-webhook/route.ts`
- `app/api/cron/match-loads/route.ts`
- `lib/services/dispatch-service.ts`
- `package.json`

### Useful
- Real-world marketplace concepts: supply, demand, claimable offers, matching, notifications and state transitions.
- Separate raw inbound message capture from extraction and promotion.
- Idempotent insert by source message identity before extraction.
- A clear dispatch service boundary between matching and delivery.
- Driver/load matching and event-triggered matching are useful examples for a later supplier/buyer match lifecycle.

### Limits
- WhatsApp listener is a Unipile webhook integration, not a free native Android listener.
- Telegram listener is explicitly a scaffold; its Telegraf handler is commented out.
- Extraction calls Anthropic directly and silently degrades exceptions into an invalid extraction result. It is not a free/offline baseline.
- Webhook authentication is conditional on `UNIPILE_WEBHOOK_SECRET` being configured; Trader should fail closed if a source webhook is used.
- The existing extraction/dedup shape is freight-specific and should not be reused for industrial SKU/technical matching.
- Latest observed Oddo E2E Actions runs were failures on 2026-06-13 (runs 27462066253, 27458838424 and 27458588045). This is historical CI evidence, not a current app/device acceptance result.

### Trader decision
Use the demand/supply marketplace concepts and the raw-before-extract sequence. Do not depend on Unipile, Claude, the freight schema or the listener implementation. Implement Trader's own channel and source adapters.

## 3. Troy — strongest domain-specific discovery and qualification reference

Repository: `ysfclc/Troy`, private, default branch `main`. Tree contained 1,677 files.

Inspected:
- `TROY.md`
- `docs/reports/2026-07-18-state-of-troy/01-vision-alignment.md`
- `docs/reports/2026-07-18-state-of-troy/03-architecture.md`
- `docs/tasks/2026-07-02-TROY-STATE-LEDGER.md`
- `docs/tasks/ACTIVE-FUCKUP-LOG.md`
- `docs/vision/2026-06-23-troy-automation-engine-node-model.md`
- `legacy/oddo-troy/lib/services/troy/script-extract.ts`
- `legacy/oddo-troy/lib/services/troy/script-extract.test.ts`
- `legacy/oddo-troy/lib/services/troy/troy-segment.test.ts`
- `legacy/oddo-troy/lib/services/troy/driver-matching.ts`
- `legacy/oddo-troy/lib/services/troy/publish-service.ts`
- `legacy/oddo-troy/lib/services/ingestion-service.ts`
- relevant source/qualification/matching and verification reports.

### Useful
- **Metadata-first qualification:** deterministic code identifies candidate spans and whether a message has useful signals; it does not prematurely decide which number is price or merge values from separate loads. This was a deliberate correction after real extraction bugs.
- Segmentation of one noisy message into multiple independent opportunity units.
- Turkish-aware lexicon/gazetteer matching and preservation of character offsets into the original message.
- Separate entities from one message: loads, drivers, sources/companies and knowledge.
- Source-company clustering and operator-reviewed merges.
- Deterministic, explainable matching with dimension-level reasons instead of an opaque AI score.
- Qualification, review and publication are distinct gates.
- Real-corpus regression fixtures and test-driven handling of messy Turkish broker messages.
- The operator's active-fuckup log and explicit built-vs-wired audits are valuable project governance patterns.

### Limits / maturity
- The most relevant service files inspected are under `legacy/oddo-troy/`; treat them as historical/legacy code until current deployment and ownership are re-established.
- `TROY.md` and the state ledger contain July 2026 status. The 2026-07-18 report records an engine activation change but only 165/7,573 corpus records processed at that snapshot; it also reports outreach still disabled and no recorded go-direct conversations. These are dated observations, not November live metrics.
- Auto-publish was deliberately disabled in the inspected `publish-service.ts`.
- The automation-engine design document describes a configurable node canvas, but its own earlier notes distinguish the hardcoded stage implementation from the canvas runtime. A designed node system is not proof every live ingestion path uses it.
- The current Troy Actions query returned no workflow runs for `main`; do not claim CI verification from the repository history we inspected.

### Trader decision
This is a high-value reference for the first industrial sourcing pilot's **discovery mechanics**, not its freight-specific data. Reimplement:
- source capture → raw evidence;
- candidate metadata → deterministic qualification;
- segmentation;
- entity extraction with source-linked claims;
- organization/contact clustering;
- explicit review queues;
- explainable matching and a human-controlled publish/contact gate.

Do not copy Turkish freight lexicons, load fields, price assumptions, driver matching weights or legacy service modules as-is. Translate the method to industrial products: material/specification, tolerance, dimensions, grade, process, certification, capacity, MOQ, delivery and Incoterms.

## 4. Kovan — manufacturing and deterministic quality-control reference

Repository: `ysfclc/Kovan`, private, default branch `master`. Tree contained 1,877 files.

Inspected:
- `docs/ARCHITECTURE-VISION.md`
- `docs/IMPLEMENTATION-PLAN.md`
- `engine/registry.py`
- `engine/qc.py`
- `tests/test_generate.py`
- `tests/test_qc.py`
- `pyproject.toml`

The project is an AI-assisted parametric 3D truck-parts design engine. Its own vision explicitly says a pipeline passing is not the same as producing correct, high-quality geometry. The deterministic QC layer checks positive volume, watertight meshes, bounding-box limits and anchor positions. Its implementation plan reports approximately 306 tests green at the time of the document, while explicitly distinguishing a clean pipeline from recognisable, correct output.

### Trader decision
Not a product or runtime dependency. The transferable lesson is **domain-specific deterministic verification**: AI extraction of a supplier's claimed capability is not enough. Match critical specifications with explicit validators and evidence. The user's manufacturing/CNC background can define the first industrial verification rules. No 3D/CAD code is needed for Trader's initial vertical slice.

## 5. Zommo — service-business and automation reference

Repository: `ysfclc/zommo.app`, private, default branch `fullstack`. Tree contained 3,156 files.

Inspected:
- `README.md`
- `docs/superpowers/specs/2026-03-17-zommo-product-vision-design.md`
- `lib/knowledge-base/architecture/system-overview.md`
- business-type registry and model/service paths in the tree.

### Useful
- Config-driven business types and typed offering models.
- A consistent separation between customer-facing experience, business-owner operations and shared service/model packages.
- Explicit flow registries rather than hardcoding every industry.
- Booking, retention and customer-history concepts may inform the services opportunity lane.

### Limits
- The March 17 product spec explicitly says communication-rule CRUD exists but the automation execution engine does not.
- The system overview documents a known Google OAuth token-signature verification gap at that snapshot; do not reuse its authentication path without a current security review.
- This is a multi-tenant web booking SaaS, not a mobile-first B2B sourcing engine.

### Trader decision
Use only the configurable-category and service-opportunity concepts. No Zommo dependency, booking schema or auth code in Trader.

## 6. Nexus — development orchestration reference, not a Trader agent runtime

Repository: `ysfclc/nexus`, private, default branch `main`. Tree contained 4,422 files.

Inspected:
- `README-NEXUS.md`
- `RECOVERY.md`
- `engine/capsule/task-creator.js`
- `engine/core/runtime/executor.js`
- task-capsule schema and test paths.

### Useful
- Structured task capsules with acceptance criteria, dependencies, affected files and evidence.
- Explicit stage transitions, locks, cancellation/kill-switch concepts and health monitoring.
- Permission checks at the executor boundary, not just at the agent prompt.
- Regression tests for races, data loss, task transitions, context generation and permission enforcement.
- The evolution loop (discover → measure → decide → execute → verify → learn) is a useful engineering governance model.

### Limits
- Nexus is a developer-work orchestration system with local files, worktrees, CLI agents and process management. It is not an end-user commercial automation runtime.
- Its recovery guide contains environment-specific destructive cleanup commands. Do not copy recovery scripts or runtime assumptions into Trader.
- Its own README/CLI is historically coupled to project paths and QRKey overlays.

### Trader decision
Use the engineering discipline (task ledger, evidence, locks, explicit verification and permission-at-executor) as process, not as a shipped dependency or imported runtime.

## 7. Other repository families — triage status

The following are visible in the linked GitHub account inventory but were not source-audited in this pass. They must not be described as reviewed or safe to reuse.

| Repository / family | Current disposition for Trader |
|---|---|
| `qrkey`, `qrkey-app`, `qrkey-fail`, `qrkey-v2`, `qrkey-vercel-vp`, `qrkey-vercel-vp-mf`, `qrkey-kiro`, `qrkey-kiro-main`, `qrkey-claude-code-production`, `qrkey-antigravity` | Separate QRKey product/version family. Needs its own consolidation audit before any shared automation/browser code is considered. |
| `memo`, `memo-2`, `SynthOS`, `Synthos-Mobile-`, `Synthos-fieldwork-`, `Syth`, `Ground-Up` | Assistant/mobile/workspace lineage. Memo-2 and Memo have already had targeted audits; remaining prototypes are not yet source-audited. Trader must not inherit the Memo runtime. |
| `MOSAIC`, `VaultOS`, `BusyBee`, `Threadline`, `PocketForge` | Mobile/native utilities. VaultOS and BusyBee have targeted audits; MOSAIC, Threadline and PocketForge need separate code/CI verification before considering any specific component. |
| `Blueprint-Contour-Extractor` | Potentially relevant to industrial drawings/spec extraction, but no source audit performed in this pass. Candidate for later research only. |
| `portfolio`, `Yusuf-Personal-Site` | Evidence and positioning for the services/career lanes; not a runtime dependency. |
| `gatherframe`, `Piotrait-`, `gappinar`, `nedra`, `Hands` | Different product experiments; no current Trader dependency identified. Not source-audited in this pass. |
| `cursor-backup`, `backup`, `pg-nodes-vscode`, `kaggle-ollama`, `ME` | Developer/data/personal-context utilities. No Trader dependency identified. The private `ME` repository may contain personal material and is not to be mined for Trader data. |

The linked GitHub account exposed 43 repositories before Trader was created (44 now including Trader). The table above is a triage, not a claim that all 43 were reviewed line-by-line.

## 8. Consolidated reuse decisions for Trader

| Need in Trader | Best reference | Decision |
|---|---|---|
| Evidence/event model, provenance and source claims | BOS ADR-014 + Outbound Sourcer ingestion + BusyBee | Reimplement a compact local-first model |
| Source adapters, parameter schema and incremental jobs | Outbound Sourcer | Reimplement in Kotlin |
| Metadata-first extraction, segmentation and qualification | Troy | Reimplement, adapting from freight to industrial requirements |
| Buyer/supplier marketplace and match lifecycle | Oddo + Troy | Use domain concepts; design Trader records independently |
| Organization/contact clustering | Troy + BOS graph model | Reimplement with evidence and human merge review |
| Approval/capability enforcement | BOS + Memo-2 + Nexus | Reimplement small contracts; require exact-content approval |
| Durable workflows and event waits | BOS design + Memo-2 crash recovery + Nexus | Keep initial state machine small; do not import current BOS/Troy engine |
| Industrial technical validation | Kovan + user's manufacturing knowledge | Define explicit domain validators; no CAD dependency |
| Service-business taxonomy | Zommo | Reuse concepts only for the services lane |
| Mobile UI/storage/capture | VaultOS + BusyBee | New Trader-owned Compose/Room implementation |
| Browser and installed-app automation | No inspected repository proves a reliable reusable mobile executor | Build independently, staged and device-tested |
| AI orchestration | BOS/Memo-2/Nexus concepts | AI optional; no existing runtime dependency |

## 9. Updated build order implication

Trader's first vertical slice should now include a **source/evidence inbox** that is designed around the BOS/Troy lessons:
1. capture a URL, text, message or document;
2. preserve original content, source identity, capture time and hash;
3. segment multi-opportunity content without losing the original;
4. extract candidate facts without prematurely resolving ambiguity;
5. qualify and show missing/contradictory fields;
6. allow human correction and evidence-linked confirmation;
7. create buyer requirement or supplier offer records;
8. match only when mandatory technical constraints are not contradicted.

The first slice should not begin with a general node-canvas, a multi-agent swarm, an always-on WhatsApp listener or bulk crawling. Those are later capabilities after the evidence and domain records are trustworthy.

## Verification boundary

This pass inspected source/document/test files and Actions metadata through GitHub. It did not execute any project test suite, build an APK, run the BOS/Oddo/Troy/Kovan deployment, inspect live databases or install Trader on a device. CI observations are dated and repository-specific. Any component chosen for actual reuse must be re-read at its current commit and independently tested in Trader.
