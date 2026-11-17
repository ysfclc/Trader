# Trader

**Bring a problem. Find a path. Do the work. Verify the result.**

Trader is an Android-first, local-first personal problem-solving and opportunity-execution system. Its purpose is broader than trading: help the operator understand problems, discover useful information and people, connect needs with available skills or resources, plan work, use tools and automations, and follow through until there is a verifiable result.

Trade, services and careers are initial specialized workflows, not the limit of the problems Trader may accept. A general problem inbox must also support practical, technical, administrative, research and project problems that do not fit a commercial pipeline. Its core mechanisms include universal capture, evidence-backed research, editable prompt/context preparation, durable workflow orchestration, visible browser and bounded app automation, and verified follow-through.

Trader is not a promise that software can solve every human problem, an open marketplace, a generic CRM, a bulk sender or an autonomous agent with unrestricted authority.

## Product laws

1. **A real problem and its outcome come first.** A crawler, dashboard, AI answer or task count is not success. Trader must help move a clearly stated problem toward a verified outcome, whether commercial, professional, technical or personal/administrative.
2. **Trader owns its own runtime and data.** It does not depend on Memo, EBO, Unipile, n8n, a paid model, a paid scraping vendor or a hosted CRM.
3. **Evidence before claims.** Every external fact has a source, capture time and verification state. Unknown is not true.
4. **AI proposes; deterministic code and the user decide.** AI may extract, translate, classify, match and draft. It cannot invent evidence, approve a match, claim representation or send a consequential message on its own.
5. **Human control of external actions.** The exact recipient, channel, content and purpose are visible before external communication or commitment.
6. **Free-first, replaceable adapters.** Core capture, storage, qualification, matching, tasks and drafting must work without paid API credits.
7. **No silent failure or fake progress.** Failed and partial operations remain visible, recoverable and attributable to their source.
8. **Mobile is the primary workspace.** Browser and installed-app workflows are capabilities inside Trader, not a dependency on another assistant app.

## Start here

| Document | Purpose |
|---|---|
| [Project lock](docs/PROJECT_LOCK.md) | Canonical problem-solving vision, opportunity workflows, scope and non-goals |
| [Architecture](docs/ARCHITECTURE.md) | Independent Android runtime, discovery, automation, AI and channel boundaries |
| [Data model](docs/DATA_MODEL.md) | Buyer needs, supplier offers, organizations, evidence, matches and deals |
| [Operating rules](docs/OPERATING_RULES.md) | Safety, quality, privacy, approval, provenance and recovery laws |
| [Decisions](docs/DECISIONS.md) | Locked decisions and reasons |
| [Roadmap](docs/ROADMAP.md) | Phases and acceptance gates |
| [Task ledger](docs/TASKS.md) | Ordered implementation queue |
| [Market and portfolio research](docs/RESEARCH/2026-11-17_MARKET_AND_PORTFOLIO.md) | First Poland–Türkiye pilot, repository map, channels and prospects |
| [Reuse audit](docs/REUSE_AUDIT.md) | Initial code inspection across discovery, Android and automation references |
| [Additional reuse audit](docs/ADDITIONAL_REUSE_AUDIT.md) | BOS, Oddo, Troy, Kovan, Zommo, Nexus and remaining repository triage |
| [QRKey reuse audit](docs/QRKEY_REUSE_AUDIT.md) | QRKey repository-family source audit, reusable patterns and non-reuse decisions |
| [Workflow runtime](docs/WORKFLOW_RUNTIME.md) | Durable run/step/event model, approval binding, lifecycle and execution limits |
| [Capability registry](docs/CAPABILITY_REGISTRY.md) | Explicit capability availability, permissions, side-effect and retry policies |
| [Project state](docs/STATE.md) | Current verified status; updated at every checkpoint |
| [Android build](docs/ANDROID_BUILD.md) | Pinned toolchain, local build, CI artifact and signing boundaries |

## Current state

**Phase 1 — first Android problem workspace merged.** The Kotlin/Compose app, local Room persistence, universal problem capture, share-to-capture, plan steps, status history and English/Turkish/Polish UI are merged to main. CI passed unit tests, lint, APK assembly, signature verification and package identity checks. The debug APK is available from the [successful build workflow](https://github.com/ysfclc/Trader/actions/runs/44106902964). A pure-domain workflow lifecycle contract passed CI. Durable Room workflow persistence, exact-payload approvals and migration tests are merged and passed CI. Capability registry enforcement is in progress; browser/app, AI and communication execution remain unavailable. Physical-device acceptance remains pending. Browser/app automation, AI adapters, live discovery and commercial workflows are not yet implemented.

The first commercial pilot is industrial tooling and machining between Türkiye and Poland/EU. The model remains category-neutral: this is the first test market, not a hard-coded product category.

## Repository rules

- This repository, `ysfclc/Trader`, is the sole source of truth for Trader.
- Preserve the fresh-start ledger and update the state before and after material implementation phases.
- Do not silently discard requirements, regressions or user decisions.
- Never claim that a capability is implemented or verified because a screen, button, document or source file exists.
- Build one complete, evidence-backed vertical slice at a time.
- No external messages are sent by tests, background workers or AI without an explicit user action.
