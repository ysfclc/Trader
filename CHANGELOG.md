## 2026-11-24 — Fail-closed discovery provenance

- Discovery findings are persisted inside a Room transaction only while the parent discovery run and matching source run are both RUNNING. This prevents findings from being attached to stale, completed, cancelled or unrelated source runs.
- Discovery adapters must return findings tagged with the source currently being executed; a mismatched source identity fails that source instead of silently misattributing evidence.
- A discovery request with no enabled/selected source is rejected before a run record is created, so an empty execution cannot appear as COMPLETED.
- Added regression tests for mismatched and finished source runs, adapter source-ID mismatch and empty source selection.
- Bumped the stable-signed debug build to 0.4.0 / versionCode 4 without changing the application ID or signing configuration.
- CI run [45394348906](https://github.com/ysfclc/Trader/actions/runs/45394348906) passed `./gradlew --no-daemon clean testDebugUnitTest assembleDebug lintDebug` in 4m45s. APK signature, persistent certificate fingerprint and exact package/version checks passed. Installable debug APK: [14919883504](https://github.com/ysfclc/Trader/actions/runs/45394348906/artifacts/14919883504); test reports: [14920726257](https://github.com/ysfclc/Trader/actions/runs/45394348906/artifacts/14920726257).

## 2026-11-24 — Company dossiers and durable discovery findings

- Added a Companies workspace inside Pipeline without removing the existing Opportunities view.
- Create company dossiers manually or from a Discover result. The original result is retained as a source snapshot only; the operator confirms the canonical company name and website.
- Add source-linked claims. Every claim starts unreviewed and requires explicit human review before it can be marked supported, contradicted or rejected.
- Added Room v7→v8 migration and encrypted backup snapshot v5 for organization profiles, sources and claims.
- Fixed the legacy v1→v8 migration test after CI run 45367079177 exposed that the test builder omitted `MIGRATION_7_8`. The corrected test now asserts all current tables.
- CI run [45369038355](https://github.com/ysfclc/Trader/actions/runs/45369038355) passed: 134 tests, 0 failures/errors/skips; `assembleDebug`, `lintDebug`, APK signature, package and version checks all passed.
- Verified APK identity: `com.yusufculcu.trader.debug`, `0.3.0-debug`, versionCode 3. Persistent installable artifact: [14908537777](https://github.com/ysfclc/Trader/actions/runs/45369038355/artifacts/14908537777).
- Physical-device migration/restore, install-over-existing and live-source acceptance remain unverified.

## 2026-11-24 — Discovery finding persistence and stable-signed APK

- Restored durable discovery finding records, Room v6→v7 migration, per-run history observation and encrypted backup snapshot v4. This closes a data-retention gap; it does not materially change the visible Discover experience.
- Fixed the legacy v1→v7 migration test to register the final 6→7 migration. Commit a4950eb17a606b9f89520ec4f78b6fcb7e4f2505.
- CI run 45345752081 passed tests, lint, APK assembly, persistent-certificate verification and package/version assertions; artifact trader-installable-apk (14898566003) was uploaded.
- Continuity incident: the older installed APK used an ephemeral CI signing certificate, while the new APK uses the persistent Trader key. Android rejects this signature change as an in-place update. The user had to uninstall; backup-before-transition was not enforced. Future releases must use the persistent key and must never require uninstalling.
- Next user-visible milestone: Organization dossier and evidence-backed qualification, not another persistence-only phase.

## 2026-11-21 — Evidence-backed discovery foundation (PR #18 in progress)

- Added versioned ProofAsset and DiscoveryStrategy models with fail-closed validation for claims, evidence references, target constraints, bounded query plans and disqualifiers.
- Added Room schema v4→v5 migration, durable proof/strategy tables and DAOs, a stable JSON codec and a repository that validates evidence references and strategy-to-asset links before saving.
- Extended encrypted workspace snapshot v2→v3 to preserve proof assets and discovery strategies. Restore validates record metadata, payload consistency, evidence provenance and foreign-key relationships; legacy v1/v2 snapshots remain readable with empty new record groups.
- Added persistence and encrypted backup/restore tests. CI and device migration acceptance pending.

## 2026-11-20 — Trader local AI foundation (in progress)

- Accepted D-021: local AI is the intelligence backbone for discovery, qualification, research and outreach preparation; Memo-2 supplies patterns only.
- Added Trader-owned pinned LiteRT-LM model catalog profiles for small, analyst and deep model roles.
- Added real CPU LiteRT-LM inference adapter with bounded prompt/response, background execution and explicit model-path/size validation. No canned responses or silent cloud fallback.
- Added bounded evidence-aware context packets that label fact verification state, retain source links and timestamps, include unknowns and signal when context is truncated.
- Added resumable pinned model downloads, SHA-256 verification before install, storage and RAM preflight, active-model selection and Model Hub UI with a real local inference smoke-test action. Model import/scan, Context Engine, orchestrator and discovery integration remain open.
- Catalog/context tests and the full CI pipeline passed in run 349 (65 tests, lint, debug APK assembly, signature and package identity); physical inference on the Redmi remains unverified.

## 2026-11-20 — Authenticated browser research capture (PR #17 in progress)

- Added a Capture action in Trader's browser. It extracts only readable visible page text/selected text, shows an editable review sheet, and saves to a selected active case or a new Research record.
- Saved captures retain title, sanitized base URL and capture time; evidence is UNREVIEWED until the user verifies it. Database creation/attachment is atomic and rejects invalid URLs, short payloads and archived destinations.
- Added the MEMO_BROWSER_CAPTURE_V1 Android share-sheet contract to Trader's receiver. Versioned Memo captures become new source-linked research records; normal shared text/URLs preserve their prior intake path.
- Strip URL query strings and fragments in Memo before sharing and independently in Trader before persistence; this prevents temporary OAuth codes/session values from being propagated in source URLs. No cookies, browser storage, passwords, hidden fields or authentication tokens are read or transferred.
- Trader CI run 358 passed 76 tests (0 failures/errors/skipped), lint, debug APK assembly, signature and package identity. Memo sender PR #15 passed CI run 1009 (Android tests, lint, build/signature and install-over-existing data-preservation); it remains draft/unmerged and the source-only run published no APK artifact. Trader's final import-review dialog and Activity-recreation guard await a clean CI rerun.

## 2026-11-20 — Trader authenticated browser sessions (PR #17 in progress)

- Fixed the missing login foundations: Trader now configures an app-local persistent WebView cookie store, enables per-WebView third-party cookies for SSO, flushes cookies on navigation/lifecycle/disposal, and supports user-initiated authentication popup windows.
- Added Android autofill and website file-upload chooser support. Camera/microphone website permissions remain denied until a dedicated user-consent flow exists.
- Detects Google's OAuth identity endpoint and reopens the original relying-party website in the system browser. Google prohibits embedded WebView sign-in; Chrome cookies are not copied into Trader. Other identity providers may have their own restrictions.
- Added URL/provider-routing regression tests. The app calls Android CookieManager directly to enable third-party cookies; the Robolectric shadow did not accurately report this platform setting, so cookie behavior remains a real-device acceptance check. CI run 349 passed 65 tests, lint, debug APK assembly, signature and package identity. Physical-device login/process-restart acceptance is still pending.

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


## 2026-11-21 — Experimental public search adapter (prototype)

- Added a bounded user-triggered DuckDuckGo HTML search adapter: query length and result limits, HTTPS-only external result validation, request timeouts, response byte cap, URL deduplication and retrieval timestamps.
- Added parser/query unit tests for encoding, result extraction, redirect handling, unsafe URLs, duplicates and field truncation.
- Not wired to Discover UI, persistent candidate dossiers, or AI qualification; CI and live-source/device validation pending.

## 2026-11-21 — Extensible source ecosystem

- Locked the adapter / configured source instance / watch rule separation and expanded the source taxonomy across feeds, company sites, registers, procurement, job/project markets, communities, federated social, developer ecosystems, reviews, authorized group workspaces and webhooks.
- Recorded Reddit, Meta/Facebook, Bluesky, Mastodon, Telegram and YouTube access constraints in docs/SOURCE_ECOSYSTEM.md.
- First working delivery is public RSS/Atom configuration and manual refresh plus one-shot search. No continuous monitoring or social API connection is claimed.


## 2026-11-21 — Discover and first public-source ingestion (PR #18)

- Added a user-configurable local source list for public RSS/Atom feeds with enable/pause/remove and last-check/error health; no API secrets are stored.
- Added bounded RSS 2.0/Atom fetching, DocDecl-disabled XML parsing, public-host checks, native item IDs, publication/retrieval timestamps and URL deduplication.
- Added an explicit built-in adapter registry, one-shot DuckDuckGo search and the Discover landing screen in English, Polish and Turkish.
- Users can manually check a feed, open a result inside Trader's WebView, and save it to Work as source-linked UNREVIEWED research evidence. Repeated source URLs resolve to existing research records.
- Added a one-tap real Hacker News front-page RSS example; other targets remain user-configurable.
- CI validation run 385 passed 97 tests (0 failures, 0 errors, 0 skipped), lint, debug APK assembly, signature verification and exact package/version assertions. APK artifact 14698585569: 69,441,948 bytes, SHA-256 b28d0b089962fb3481425ceae14dfb4d4449527a15f7d5bd3fd4408b2664f210.
- Limitations: feed checks are manual only; source configurations are not yet in encrypted backups; no device/live-feed test, normalized company dossiers, AI qualification or social-platform adapters are claimed.


## 2026-11-21 — Persistent Android identity and launcher icon (candidate 0.2.0)

- Added adaptive launcher and round icons and wired them into the Android manifest.
- Kept `com.yusufculcu.trader.debug` unchanged; incremented versionCode 1 → 2 and versionName 0.1.0 → 0.2.0.
- Added optional stable development signing through a dedicated Trader keystore supplied only via GitHub Actions secrets. CI labels the no-secret APK as CI-only and does not call it installable.
- Generated the development keystore outside the repository; fingerprint and secret setup are documented in `docs/ANDROID_BUILD.md`. User configuration and CI verification remain pending.
- No Room schema or application data location changed. In-place updates with the same package and certificate preserve app-private data; uninstall/clear-storage does not.


## 2026-11-21 — Discovery settings backup continuity

- Bumped the encrypted workspace snapshot payload from v1 to v2 while keeping the outer AES-GCM file format stable.
- Included public RSS/Atom source configurations, enabled state and last-check/error health in encrypted backups; provider credentials, browser cookies and signing material are excluded.
- Kept legacy snapshot-v1 backups readable (source list defaults to empty). Restore validates source identity/adapter/HTTPS URL and replaces the full list; preference changes are compensated if the Room restore transaction fails.
- Added a source-settings round-trip test and wired the app's actual DiscoverySourceStore into export/restore. CI verification pending.
