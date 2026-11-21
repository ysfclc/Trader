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

**Latest application features:** PR [#8](https://github.com/ysfclc/Trader/pull/8) added passphrase-protected encrypted export/restore for all seven persisted Room record groups. PR [#11](https://github.com/ysfclc/Trader/pull/11) adds conservative startup recovery for interrupted workflows, with explicit human review before a step can be reset. Pull-request CI [run 255](https://github.com/ysfclc/Trader/actions/runs/44478899807) and post-merge main CI [run 256](https://github.com/ysfclc/Trader/actions/runs/44479460220) passed. Run 256 reports **37 tests, 0 failures, 0 errors and 0 skipped**, plus lint, debug APK assembly, signature verification and package identity checks. The documentation-only state/coverage reconciliation was merged as [PR #9](https://github.com/ysfclc/Trader/pull/9); its PR CI run 257 and post-merge main run 258 also passed. PR #11 CI [run 271](https://github.com/ysfclc/Trader/actions/runs/44489948440) passed **38 tests, 0 failures, 0 errors and 0 skipped**, plus lint, debug APK assembly, signature verification and package identity checks. Post-merge main CI [run 276](https://github.com/ysfclc/Trader/actions/runs/44490819804) also passed the full verification/build/signature/package pipeline. PR [#15](https://github.com/ysfclc/Trader/pull/15) hardened incoming Android share-text parsing and merged as 9911be2f97e8e0fa55b1c4dbbd5483e96f90322a after CI run 304 passed. PR [#12](https://github.com/ysfclc/Trader/pull/12) added the typed capability contracts and merged as `cb929afc81b0e04cf5e7b81d1d199c2a78f4fb8b`. PR CI run 279, final-head run 285 and post-merge main run [286](https://github.com/ysfclc/Trader/actions/runs/44500309783) passed **44 tests, 0 failures, 0 errors and 0 skipped**, plus lint, debug APK assembly, signature and package identity checks.

The capability registry now defines typed input/output schemas, cost disclosures, limit policies and required outcome checks for all 16 registered operations; generic field validation rejects missing, mistyped and unexpected values. Adapter-specific policy checks and numeric quotas remain required before any unimplemented adapter can be enabled. The current main build is an offline ProblemCase/evidence/planning/workflow workspace. The feature branch feature/evidence-backed-opportunity-loop adds an optional opportunity profile and Pipeline: manually track a real trade, service or career lead; record organization/contact labels, fit evidence, stage, next action and a date; retain the source case; open a supported contact route by explicit user action. It does not search for prospects, verify a company/person, send messages, place calls or schedule reminders. PR #16 CI run 306 passed 59 tests, lint, debug APK assembly, signature verification and exact package/version checks. Physical-device acceptance remains open. The broader gaps remain physical-device acceptance, WorkManager execution/recovery, file/screenshot/voice capture, deduplication, browser/app automation, embedded AI, live discovery/matching, normalized commercial records and external communication adapters.

PR #8 main debug APK artifact (CI run 256): `com.yusufculcu.trader.debug`, version `0.1.0-debug`, 19,046,757 bytes, SHA-256 `faf4b1abbfa0227b5c274d34ab7bc59be570350c28c89073442cf63d54f0ec76`. It is a CI debug-signed sideload artifact, **not** a production release. Do not uninstall an existing installation to resolve a signing conflict without first protecting local data.

Use [PROJECT_LOCK.md](docs/PROJECT_LOCK.md) as the canonical product specification; there is no separate `docs/SPEC.md`. Coverage and limitations are tracked in [COVERAGE.md](docs/COVERAGE.md), [RISKS.md](docs/RISKS.md) and [TASKS.md](docs/TASKS.md). Physical-device verification is not inferred from CI.

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

The Android launcher icon is now defined as an adaptive icon. The debug package ID remains `com.yusufculcu.trader.debug`; candidate version is `0.2.0-debug` / versionCode 2. CI only publishes a `trader-installable-apk` when the dedicated Trader development keystore is configured through repository Actions secrets. Without those secrets, the artifact is explicitly named `trader-ci-debug-apk-not-for-installation`. Do not uninstall or clear app storage as a routine update step. See [ANDROID_BUILD.md](docs/ANDROID_BUILD.md) for the one-time signing setup and migration caveat.
