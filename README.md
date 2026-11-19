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
| [Project state](docs/STATE.md) | Compact current status and next actions |
| [Coverage matrix](docs/COVERAGE.md) | Requirement → implementation → tests → documentation traceability |
| [Risk register](docs/RISKS.md) | Current product, runtime, security and acceptance risks |
| [Changelog](CHANGELOG.md) | Dated project milestones and verification evidence |
| [Android build](docs/ANDROID_BUILD.md) | Pinned toolchain, local build, CI artifact and signing boundaries |

## Current state

**Latest application feature:** PR [#8](https://github.com/ysfclc/Trader/pull/8) added passphrase-protected encrypted export/restore for all seven persisted Room record groups. Pull-request CI [run 255](https://github.com/ysfclc/Trader/actions/runs/44478899807) and post-merge main CI [run 256](https://github.com/ysfclc/Trader/actions/runs/44479460220) passed. Run 256 reports **37 tests, 0 failures, 0 errors and 0 skipped**, plus lint, debug APK assembly, signature verification and package identity checks. The documentation-only state/coverage reconciliation was merged as [PR #9](https://github.com/ysfclc/Trader/pull/9); its PR CI run 257 and post-merge main run 258 also passed.

The current app is a useful offline ProblemCase/evidence/planning/workflow workspace. It supports manual and Android text/URL share capture, case editing/search/archive/restore, source-linked evidence, expected-result plan steps, manually driven workflow controls, capability availability display, context-packet share handoff, and encrypted local backup/restore. It is **not an autonomous executor**. Physical-device acceptance (including capture, backup and restore), WorkManager execution/process-death recovery, file/screenshot/voice capture, deduplication, browser/app automation, embedded AI, live discovery/matching, commercial records and external communication remain open.

PR #8 main debug APK artifact (CI run 256): `com.yusufculcu.trader.debug`, version `0.1.0-debug`, 19,046,757 bytes, SHA-256 `faf4b1abbfa0227b5c274d34ab7bc59be570350c28c89073442cf63d54f0ec76`. It is a CI debug-signed sideload artifact, **not** a production release. Do not uninstall an existing installation to resolve a signing conflict without first protecting local data.

Use [PROJECT_LOCK.md](docs/PROJECT_LOCK.md) as the canonical product specification; there is no separate `docs/SPEC.md`. Coverage and limitations are tracked in [COVERAGE.md](docs/COVERAGE.md), [RISKS.md](docs/RISKS.md) and [TASKS.md](docs/TASKS.md). Physical-device verification is not inferred from CI.

## Repository rules

- This repository, `ysfclc/Trader`, is the sole source of truth for Trader.
- Preserve the fresh-start ledger and update the state before and after material implementation phases.
- Do not silently discard requirements, regressions or user decisions.
- Never claim that a capability is implemented or verified because a screen, button, document or source file exists.
- Build one complete, evidence-backed vertical slice at a time.
- No external messages are sent by tests, background workers or AI without an explicit user action.
