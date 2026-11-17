# Trader

**Find demand. Find supply. Make the connection. Turn it into a deal.**

Trader is an Android-first, local-first commercial discovery and execution workspace. It helps one operator find people and companies that want to buy something, find manufacturers and suppliers that can provide it, establish whether the two sides genuinely fit, and manage an authorized commercial introduction.

It also supports two related opportunity lanes:
- **Services:** find businesses with a visible operational problem that can be solved through software, automation, data or implementation services.
- **Careers:** find relevant jobs and hiring contacts, assess fit honestly, prepare applications and track follow-up.

Trader is not an open marketplace, a generic CRM, a bulk email sender or an autonomous agent with permission to act on the user's behalf.

## Product laws

1. **The commercial outcome comes first.** A crawler, dashboard or AI answer is not a successful outcome. A verified buyer need, qualified supplier, credible introduction, response or commercial next step is.
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
| [Project lock](docs/PROJECT_LOCK.md) | Canonical product vision, target user, scope and non-goals |
| [Architecture](docs/ARCHITECTURE.md) | Independent Android runtime, discovery, automation, AI and channel boundaries |
| [Data model](docs/DATA_MODEL.md) | Buyer needs, supplier offers, organizations, evidence, matches and deals |
| [Operating rules](docs/OPERATING_RULES.md) | Safety, quality, privacy, approval, provenance and recovery laws |
| [Decisions](docs/DECISIONS.md) | Locked decisions and reasons |
| [Roadmap](docs/ROADMAP.md) | Phases and acceptance gates |
| [Task ledger](docs/TASKS.md) | Ordered implementation queue |
| [Market and portfolio research](docs/RESEARCH/2026-11-17_MARKET_AND_PORTFOLIO.md) | First Poland–Türkiye pilot, repository reuse map, channels and prospects |
| [Project state](docs/STATE.md) | Current verified status; updated at every checkpoint |

## Current state

**Phase 0 — product definition and repository lock.** The project is documented. Android source code, APK generation, device acceptance and live channel integrations have not yet been implemented in this repository.

The first commercial pilot is industrial tooling and machining between Türkiye and Poland/EU. The model remains category-neutral: this is the first test market, not a hard-coded product category.

## Repository rules

- This repository, `ysfclc/Trader`, is the sole source of truth for Trader.
- Preserve the fresh-start ledger and update the state before and after material implementation phases.
- Do not silently discard requirements, regressions or user decisions.
- Never claim that a capability is implemented or verified because a screen, button, document or source file exists.
- Build one complete, evidence-backed vertical slice at a time.
- No external messages are sent by tests, background workers or AI without an explicit user action.
