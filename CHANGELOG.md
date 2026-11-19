# Changelog

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
