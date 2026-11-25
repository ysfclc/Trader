# Trader

**Bring a problem. Find a path. Do the work. Verify the result.**

Trader is an Android-first, local-first personal problem-solving and opportunity-execution system. Its purpose is broader than trading: help the operator understand problems, discover useful information and people, connect needs with available skills or resources, plan work, use tools and automations, and follow through until there is a verifiable result.

Trade, services and careers are initial specialized workflows, not the limit of the problems Trader may accept. A general problem inbox must also support practical, technical, administrative, research and project problems that do not fit a commercial pipeline. Its core mechanisms include universal capture, evidence-backed research, editable prompt/context preparation, durable workflow orchestration, visible browser and bounded app automation, and verified follow-through.

Trader is not a promise that software can solve every human problem, an open marketplace, a generic CRM, a bulk sender or an autonomous agent with unrestricted authority.

## First product: Industrial Sourcing Desk

Trader's first complete commercial product is an operator-controlled sourcing workspace for Türkiye ↔ Poland/EU industrial tooling and machining. It takes a buyer requirement through supplier-offer comparison to a documented shortlist. See [the product definition](docs/PRODUCTS/INDUSTRIAL_SOURCING_DESK.md).

The workflow lives in the app: create a buyer requirement in a company dossier, record supplier offers against supplier dossiers, compare the stated product/specification/quantity/MOQ/price fields, record a human decision with rationale, and share a shortlist brief. Every offer can link to source evidence. Unknowns remain unknown; a shortlist is not a certification or commercial commitment. No messages or RFQs are sent automatically.

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

The first product, **Industrial Sourcing Desk**, is implemented on `feature/discovery-run-persistence` and CI-verified. It supports buyer requirements, supplier offers, conservative comparison, recorded human shortlist decisions and a user-initiated share handoff. The product boundary and known limitations are in [the product definition](docs/PRODUCTS/INDUSTRIAL_SOURCING_DESK.md).

- Android build: `0.8.0-debug`, versionCode 8, package `com.yusufculcu.trader.debug`.
- Database: Room schema v11; encrypted backup snapshot v8.
- CI: [45574277490](https://github.com/ysfclc/Trader/actions/runs/45574277490) and docs-aligned verification [45575324059](https://github.com/ysfclc/Trader/actions/runs/45575324059) passed build, tests, lint, package/version and persistent signing-certificate checks.
- Installable APK: [artifact 15003658514](https://github.com/ysfclc/Trader/actions/runs/45575324059/artifacts/15003658514).
- Not yet proven: install-over-existing on the Redmi, real-data sourcing journey, migration and encrypted backup/restore on-device.

The next work stays inside this product until the real-device acceptance gate is closed. No second product is being started in parallel.

## Repository rules

- This repository, `ysfclc/Trader`, is the sole source of truth for Trader.
- Preserve the fresh-start ledger and update the state before and after material implementation phases.
- Do not silently discard requirements, regressions or user decisions.
- Never claim that a capability is implemented or verified because a screen, button, document or source file exists.
- Build one complete, evidence-backed vertical slice at a time.
- No external messages are sent by tests, background workers or AI without an explicit user action.


## First practical workflow in progress

PR #17 provides the Trader-owned authenticated browser, visible-page evidence capture and source-linked research intake from Memo's Android Sharesheet. PR #18 adds the first public-source Discover workflow: DuckDuckGo one-shot search, user-configurable RSS/Atom feeds, manual checks, per-source status, open in Trader browser and save as unreviewed evidence. CI run 385 passed 97 tests, lint, APK build/signature/package checks on a tree matching PR #18 app code. Company qualification, scheduled monitoring, social connectors and automatic outreach remain future increments. Memo's sender PR #15 is still draft/unmerged; its capture button is not yet available in the installed Memo app. See the state ledger for acceptance boundaries.


## Current discovery implementation — PR #18

The open feature branch feature/public-search-adapter adds the first working Discover/source-ingestion path on top of the Trader browser. Fresh installs land on Discover. Users can run a one-shot DuckDuckGo search, add arbitrary permitted public RSS/Atom URLs, enable/pause/remove sources, manually check feeds, inspect per-source health, open results inside Trader's WebView and save items into Work as source-linked UNREVIEWED evidence. Repeated source URLs resolve to existing research evidence. A one-tap Hacker News front-page feed is included as a real example, not mock data.

PR #18 CI validation run 385 used a source tree identical to the PR head and passed 97 tests (0 failures, 0 errors, 0 skipped), lint, debug APK assembly, signature verification and package/version checks. See docs/SOURCE_ECOSYSTEM.md for the wider provider-neutral source plan and current access limits.

This is not continuous monitoring: feed checks are manual only. Configured public feeds are included in encrypted backup snapshot v2; legacy snapshot v1 restores with an empty feed list. Credentials/cookies are never included. Reddit, Facebook, Bluesky, Mastodon and private-community adapters are not implemented. Normalized company/person dossiers, AI signal qualification and outbound sending are also not included. No physical-device or live-feed smoke test has been recorded. The main branch does not yet contain this open feature.


## App identity and safe updates

The Android launcher icon is now defined as an adaptive icon. The debug package ID remains `com.yusufculcu.trader.debug`; current CI-verified version is `0.8.0-debug` / versionCode 8. CI only publishes a `trader-installable-apk` when the dedicated Trader development keystore is configured through repository Actions secrets. Without those secrets, the artifact is explicitly named `trader-ci-debug-apk-not-for-installation`. Do not uninstall or clear app storage as a routine update step. See [ANDROID_BUILD.md](docs/ANDROID_BUILD.md) for the one-time signing setup and migration caveat.


## Industrial Sourcing Desk — current build

The feature branch `feature/discovery-run-persistence` contains the first usable sourcing product. CI run [45574277490](https://github.com/ysfclc/Trader/actions/runs/45574277490) passed the clean Android build, unit tests, lint, package/version assertion and persistent signing-certificate verification. Installable artifact: [15003891645](https://github.com/ysfclc/Trader/actions/runs/45574277490/artifacts/15003891645). Build identity: `com.yusufculcu.trader.debug`, `0.8.0-debug`, versionCode 8. Physical-device update and sourcing-workflow acceptance remain open.
