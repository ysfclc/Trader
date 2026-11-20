## 2026-11-20 — Trader local AI foundation (in progress)

- Accepted D-021: local AI is the intelligence backbone for discovery, qualification, research and outreach preparation; Memo-2 supplies patterns only.
- Added Trader-owned pinned LiteRT-LM model catalog profiles for small, analyst and deep model roles.
- Added real CPU LiteRT-LM inference adapter with bounded prompt/response, background execution and explicit model-path/size validation. No canned responses or silent cloud fallback.
- Added bounded evidence-aware context packets that label fact verification state, retain source links and timestamps, include unknowns and signal when context is truncated.
- Added catalog/context foundation tests; CI verification pending. Model download/install/verification UI, context engine, orchestrator and discovery integration remain open.

## 2026-11-20 — Trader browser baseline (PR #17 in progress)

- Added a Trader-owned WebView browser screen as a bottom navigation destination with address/search, back/forward, stop/reload, title/current URL, loading progress and page-share handoff.
- Added Internet permission. This initial browser is not multi-tab, does not persist session state/bookmarks/history, does not capture page evidence into dossiers, and does not automate form submissions or outreach.
- Documented the discovery-first product direction, company evidence/provenance, visible browser runtime and exact approval model in `docs/PRODUCT_EXPERIENCE.md`.
- CI and physical-device acceptance pending; do not claim a verified APK until CI completes.


## 2026-11-19 — Evidence-backed opportunity pipeline (PR #16 in progress)

- Added optional one-to-one opportunity profile linked to a universal ProblemCase: trade/service/career/other lane, organization and contact labels, fit notes, lifecycle stage, next action and local follow-up date.
- Added a Pipeline tab with active/closed views, search, due/overdue display and navigation back to the source case; opportunity create/update/stage changes are recorded in Activity.
- Added explicit, scheme-restricted contact handoff to HTTP(S) browser, mailto composer or tel dialer. The app does not send messages or place calls.
- Added Room v3→v4 migration and included opportunity profiles in encrypted backup. Schema-v3 backup snapshots remain restorable without opportunity records.
- Added persistence, validation, legacy-backup, migration and safe-contact-route tests, plus EN/PL/TR strings.
- No automatic prospect discovery, AI fit qualification, normalized Organization/Person entities, consent tracking, message sending, calls or background reminders are implemented. PR #16 CI run 306 passed **59 tests, 0 failures, 0 errors, 0 skipped**, lint, debug APK assembly, signature verification and package/version checks. Physical-device acceptance is not claimed.

# Changelog

## 2026-11-19 — Incoming Android share intent hardening

- Read `Intent.EXTRA_TEXT` as its documented `CharSequence` type instead of assuming it is a `String`, preserving styled shared text.
- Extracted incoming intent parsing into a small testable boundary and added Robolectric tests for ACTION_SEND, ACTION_VIEW, whitespace-only content, missing payloads and unrelated actions.
- This verifies intent parsing only; Android chooser behavior and Activity-to-database capture remain physical-device/integration acceptance items.


## 2026-11-19 — Workflow cancellation safety

- Cancelling a workflow with an active RUNNING step now preserves the active step and run as NEEDS_REVIEW, clears approval, and cancels only remaining steps; cancellation between steps still completes as CANCELLED.
- Added regression tests for active-step ambiguity and between-step cancellation. PR #14 CI run 292 passed **45 tests (0 failures/errors/skipped)**, lint, debug APK assembly, signature verification and package/version assertions; merged as `4ffd465e7c89a35d3a5c3b6125f8097b246b3b37`.


## 2026-11-19 — Typed capability execution contracts

- Completed typed input/output contracts for all 16 registered capabilities, including required/optional and sensitive fields, cross-field constraints, availability, permissions/consent and explicit side effects.
- Added cost disclosures, operational limit modes and required postconditions with verification methods; the registry prevents adapter-backed operations from being marked AVAILABLE while limits remain undeclared.
- Added generic input/output field validation for required values, primitive/container types and unexpected keys; documented that adapters still own semantic policy enforcement and concrete quotas.
- Classified remote model inference as a data-disclosure effect requiring explicit approval and consent. External send/submit operations still require exact approval, prohibit retries and require provider confirmation.
- Added coverage for complete contracts, validation failures, unknown capabilities and data-disclosure controls.
- Merged [PR #12](https://github.com/ysfclc/Trader/pull/12) as `cb929afc81b0e04cf5e7b81d1d199c2a78f4fb8b`. PR CI [run 279](https://github.com/ysfclc/Trader/actions/runs/44498767973) and final-head CI [run 285](https://github.com/ysfclc/Trader/actions/runs/44499826271) passed; run 285 reports 44 tests, 0 failures, 0 errors and 0 skipped. Post-merge main CI [run 286](https://github.com/ysfclc/Trader/actions/runs/44500309783) also passed 44 tests, 0 failures, 0 errors and 0 skipped, plus lint, debug APK assembly, signature and package/version checks. JUnit artifacts: `14517540622` (PR final head) and `14516991778` (main).


## 2026-11-19 — Conservative interrupted-workflow recovery

- Added transactional startup reconciliation for workflows left RUNNING after process interruption.
- In-flight steps become NEEDS_REVIEW with prior approval cleared; runs interrupted between steps become PAUSED. Append-only events explain each recovery decision.
- Added a file-backed close/reopen test covering both paths, repeated-call idempotency and explicit human resolution.
- Added English, Turkish and Polish UI controls to review the interrupted run and reset a step only after confirming the action did not complete.
- PR #11 CI [run 271](https://github.com/ysfclc/Trader/actions/runs/44489948440) passed: 38 tests, 0 failures, 0 errors, 0 skipped; lint, debug APK build, signature and package checks passed.
- This is not WorkManager execution or automatic resume; no capability is executed during recovery.

## 2026-11-19 — State and requirement traceability reconciliation (PR #9)

- Merged [PR #9](https://github.com/ysfclc/Trader/pull/9) as `b0539a960b67d300ccf426a54f05d6860b43406c`.
- Reconciled the current state ledger with the code and CI evidence after PR #8; moved historical milestones into this changelog.
- Added a 12-row requirement-to-implementation/test/docs matrix and a 14-item risk register; appended D-016 and corrected README, tasks and roadmap.
- The change is documentation-only; application source did not change.
- PR CI [run 257](https://github.com/ysfclc/Trader/actions/runs/44485329566) and post-merge main CI [run 258](https://github.com/ysfclc/Trader/actions/runs/44485773498) passed.

Project history is recorded as dated milestones. Detailed current status belongs in `docs/STATE.md`; outstanding work belongs in `docs/TASKS.md`.

## 2026-11-19 — Encrypted workspace backup and restore (PR #8)

- Merged [PR #8](https://github.com/ysfclc/Trader/pull/8) at `d6ee9a3926df27c78af1c4328ed01989dafe349f`.
- Added a passphrase-protected `.traderbackup` export/import for all seven persisted Room record groups.
- Uses AES-256-GCM with PBKDF2-HMAC-SHA256 (210,000 iterations), random salt/IV and an authenticated versioned header. Restore validates data and replaces workspace rows atomically; it is not a merge.
- Added destructive-restore acknowledgement, English/Turkish/Polish strings and round-trip/replacement/wrong-passphrase/tamper/minimum-passphrase tests.
- PR CI [run 255](https://github.com/ysfclc/Trader/actions/runs/44478899807) and post-merge main CI [run 256](https://github.com/ysfclc/Trader/actions/runs/44479460220) passed the complete Gradle test/lint/build pipeline and APK signature/package checks.
- Main CI report: 37 tests, 0 failures, 0 errors, 0 skipped. Debug APK: 19,046,757 bytes; SHA-256 `faf4b1abbfa0227b5c274d34ab7bc59be570350c28c89073442cf63d54f0ec76`.
- Physical-device backup/restore and existing workspace acceptance remain open.

## 2026-11-19 — Local workspace v1 (PR #7)

- Merged [PR #7](https://github.com/ysfclc/Trader/pull/7) at `afec456b6861cf7ae639152185cdb64a9743fb87`.
- Added case editing, active/archived search, soft archive/restore, source-linked evidence, expected-result plan steps and a source/verification-aware context packet for user-triggered Android share handoff.
- PR CI run 232 and post-merge main run 233 passed 33 tests, lint, APK assembly, signature and package identity checks.
- Device UI/share acceptance remained open.

## 2026-11-19 — Workflow control surface (PR #6)

- Merged [PR #6](https://github.com/ysfclc/Trader/pull/6) at `1f39c0a8060692ea80c31650b9c8cd861f500b2f`.
- Added a manually driven workflow panel with review/ready/start/pause/resume/cancel controls, ordered steps, event history, explicit unavailable-capability states and evidence-required outcome verification.
- CI runs 202/203 passed; no autonomous executor or device acceptance was claimed.

## 2026-11-17 — Workflow and capability foundation (PRs #2–#4)

- PR #2: deterministic workflow lifecycle rules and QRKey pattern audit; CI run 93 passed.
- PR #3: Room v2 workflow persistence, append-only events, exact-payload approval binding, retry/cancel rules and migration tests; CI runs 111/112 passed.
- PR #4: typed capability registry and fail-closed execution gates; CI runs 129/130 passed (30 tests at that checkpoint).

## 2026-11-17 — Initial Android foundation (PR #1)

- Merged [PR #1](https://github.com/ysfclc/Trader/pull/1).
- Added Compose/Room Android shell, universal ProblemCase inbox, manual and Android share-intent URL/text intake, plan steps, activity history, status rules and EN/TR/PL strings.
- CI runs 74/75 passed tests, lint, debug APK assembly, signature verification and package identity checks.

## 2026-11-17 — Product scope and repository lock

- Locked Trader as an independent Android-first, local-first problem-solving and opportunity-execution system, not a trading-only app.
- Set industrial tooling/machining between Türkiye and Poland/EU as the first market pilot, not a hard-coded product category.
- Audited reference repositories, including QRKey, and documented no-fork/no-runtime-dependency decisions.
