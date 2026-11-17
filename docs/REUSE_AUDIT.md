# Trader — Existing Repository Reuse Audit

**Audit date:** 2026-11-17  
**Scope:** targeted source inspection of the relevant existing projects, including actual implementation files, tests, project status and recent GitHub Actions history. This is not a line-by-line audit of all 43 repositories.  
**Disposition:** build Trader independently; reuse selected patterns and tests as design references. No source files have been copied into Trader.

## Executive decision

Trader should be a clean native Android application with its own Kotlin domain model, Compose UI, Room database, source-adapter contracts, workflow state machine, browser surface and channel handoff layer.

Do not fork Memo, VaultOS or BusyBee. Do not embed EBO or Outbound Sourcer as a service. The existing systems solve different product problems and have different storage models, dependencies and maturity limits.

The highest-value reusable ideas are:
1. Outbound Sourcer: declarative source registration, schema-driven parameters, streamed acquisition, incremental persistence, cancellation and source warnings.
2. VaultOS: Compose/Room/Flow patterns, app-private storage, Android share/document import, chunked work and integrity-minded file operations.
3. BusyBee: preserve raw import evidence, use deterministic fingerprints for duplicate protection, leave missing numeric facts unknown, test messy CSV parsing.
4. Memo-2: pure-Kotlin core contracts, typed workflow attempts, approval bound to exact content, crash-safe treatment of in-flight side effects, bounded prompt packets, redaction and provider health/routing tests.
5. EBO: signal-first commercial thinking and action ladders are concepts only. Do not port its execution path without re-proving every approval gate; its own records document gaps between configured flags and executor enforcement.

## Repository-by-repository findings

### 1. Outbound Sourcer — strongest discovery reference

Repository: `ysfclc/outbound-sourcer`, private, default branch `master`.

Inspected files:
- `backend/app/sources/base.py`
- `backend/app/sources/custom_adapter.py`
- `backend/app/sources/custom_spec.py`
- `backend/app/services/pipeline.py`
- `backend/app/services/ingestion.py`
- `backend/app/services/scorer.py`
- `backend/tests/test_pipeline.py`
- `backend/tests/test_custom_sources.py`
- `backend/tests/test_api.py`
- `README.md`

**What is genuinely useful**
- `SourceAdapter`, `ParamField` and the registry separate a source's identity, configuration schema and search implementation.
- Parameter validation is performed against the declared schema before a job runs.
- The pipeline consumes generator results as they arrive, persists each item in bounded units, updates progress incrementally and checks cancellation between items.
- The ingestion layer preserves source listing versions, uses stable source-local identities, tracks child entities and avoids marking old entities disappeared after an incomplete/blocked fetch.
- Custom source specs demonstrate data-configurable listing URLs, JSON-LD/CSS extraction, pagination and optional JavaScript rendering.
- Tests cover missing required parameters, source failures, no fabricated results on a 403, deterministic sample data, idempotent rows, source config CRUD and a full fixture pipeline.

**What must not be copied as Trader domain logic**
- The base raw-item contract is business-directory-oriented: source ID, name, contact details, website, address, rating and categories. Trader needs separate buyer requirements, supplier offers, products, specification attributes, certificates, quotes, delivery and commercial terms.
- The lead scorer rewards website, email, phone, reviews and rating. These are not proxies for a buyer's purchasing intent or technical supplier fit.
- The current LLM scoring path proposes generic outreach angles. It is not a defensible industrial qualification or matching engine.
- The Python/FastAPI/SQLAlchemy/React architecture is a different hosting and product model from Trader's local-first Android client.
- The custom scraper is not a blanket permission to crawl arbitrary websites. Every Trader adapter must declare and enforce source-specific access policy, allowed scope and rate limits.
- Sample mode and fixture tests prove pipeline behavior, not live-source quality or production coverage.

**Trader disposition:** reimplement the adapter registry, schema validation, stream/progress/cancel pattern and versioned source-ingestion concepts in Kotlin. Design Trader's domain schemas independently. Do not connect to the Outbound Sourcer database or depend on its backend.

**Verification note:** README documents 16 backend tests, and the inspected test files show the cases above. I did not execute the Python test suite in this audit, so this is source/test inspection, not a fresh pass result.

### 2. VaultOS — strongest Android local-data reference, but not a release template

Repository: `ysfclc/VaultOS`, public, default branch `main`.

Inspected files:
- `app/src/main/java/com/vaultos/app/data/VaultDatabase.kt`
- `app/src/main/java/com/vaultos/app/storage/VaultRepository.kt`
- `app/src/main/java/com/vaultos/app/storage/VaultBackupManager.kt`
- `app/src/main/java/com/vaultos/app/storage/FileOperations.kt`
- `app/src/main/java/com/vaultos/app/storage/MetadataExtractor.kt`
- `app/build.gradle.kts`
- `.github/workflows/android-build.yml`
- `docs/STATE.md`
- `docs/PROJECT-STATUS.md`
- `README.md`

**What is useful**
- Native Kotlin/Compose structure and Room repositories are a natural reference for Trader.
- Room's indexed records, DAO queries and Flow observation are a useful pattern for a local searchable workspace.
- Android share intents, SAF document picking, app-private storage, resumable work and bounded file streaming are useful capture/import patterns.
- `FileOperations.kt` has careful temporary-file commit, cancellation checks, bounded buffers and symbolic-link refusal; its tests cover file-operation behavior.
- The project documents Android capability limits instead of claiming universal clipboard or filesystem observation.

**What is not safe to transplant as-is**
- `VaultDatabase.kt` uses Room migrations but sets `exportSchema = false`; VaultOS's own state ledger reports no identified migration regression suite.
- `VaultBackupManager.kt` supports manual SAF backup/restore, but the VaultOS state ledger explicitly says backups are not encrypted and automated round-trip/corruption/path-safety tests were not identified.
- The existing package ID, application identity and signing material are VaultOS-specific. A signing keystore is tracked in that repository; it must not be copied into Trader or used for Trader releases.
- The repository does not have a root LICENSE file in the inspected tree. Treat it as user-owned source, but do not copy blocks blindly or add a cross-project dependency.
- Its hosted GitHub Actions state is not currently a green baseline: the latest three main-branch Android runs observed on 2026-11-17 were failures. The project's own state ledger also reports hosted-runner provisioning and physical-device gates open. A historical Vercel build success is not the same as current GitHub CI or Redmi acceptance.

**Trader disposition:** use the Room/Flow and capture design as architecture guidance, but implement Trader's own schema, migration tests, backup format and key management. New backup data must be encrypted and round-trip/corruption tested before release. Use a new application ID and CI-provided signing secrets.

### 3. BusyBee — useful provenance and test discipline, not a domain or app base

Repository: `ysfclc/BusyBee`, public, default branch `main`.

Inspected files:
- `android/app/src/main/java/com/ysfclc/busybee/drivercommandcentre/DriverMirrorStore.kt`
- `android/app/src/main/java/com/ysfclc/busybee/drivercommandcentre/PlatformNotificationListener.kt`
- `android/app/src/main/java/com/ysfclc/busybee/drivercommandcentre/NativeBridge.kt`
- `android/app/build.gradle.kts`
- `android/app/src/test/java/com/ysfclc/busybee/drivercommandcentre/DriverCsvTest.kt`
- `docs/NATIVE_PROVIDER_BRIDGE.md`
- `docs/PHASE_LOG.md`
- `README.md`
- `.github/workflows/build-signed-apk.yml`

**What is useful**
- The driver mirror records raw imported rows separately from normalized canonical rows.
- Import persistence is transactional and uses a deterministic SHA-256 fingerprint for duplicate protection.
- Missing financial fields remain null/unknown; unlike currencies are not added together.
- `DriverCsvTest.kt` covers quoted commas, escaped quotes, missing numeric values, required headers and comma decimal separators.
- The signed-APK workflow checks for all signing secrets before producing a release; it falls back to a debug build rather than inventing a release signing key.
- The Native Provider Bridge document clearly distinguishes a first-party internal adapter contract from an unrestricted official API supplied by a third party.

**Limits**
- The data model is ride/trip/earning-specific and should not be imported into Trader.
- Current mirror and command-centre storage are separate SQLite databases; the bridge document itself says a unified event bus, capability registry and canonical provider event ledger are not implemented.
- `NativeBridge.kt` is a minimal JNI version call, not a reusable automation bridge.
- The notification listener extracts only visible notification text for recognised ride apps; it is not general installed-app automation.
- BusyBee's public repo has no root LICENSE file in the inspected tree. Reimplement patterns rather than copying modules.
- Latest inspected Android workflow run on 2026-11-06 succeeded in CI; this proves that run's tests/build, not physical-device acceptance or a Trader build.

**Trader disposition:** use the evidence/raw-import and cautious-value principles. Write a Trader-owned provenance model with evidence per fact and explicit freshness. Use BusyBee's release workflow structure as a CI reference, not its app or database.

### 4. Memo-2 — best isolated workflow-safety reference; explicitly not a dependency

Repository: `ysfclc/memo-2`, private, default branch `main`.

Inspected files:
- `core/src/main/kotlin/app/memo/core/engine/WorkflowEngine.kt`
- `core/src/main/kotlin/app/memo/core/policy/ActionPolicy.kt`
- `core/src/main/kotlin/app/memo/core/prompts/ContextPacket.kt`
- `core/src/main/kotlin/app/memo/core/redact/Redactor.kt`
- `core/src/main/kotlin/app/memo/core/route/ModelRouter.kt`
- `core/src/main/kotlin/app/memo/core/domain/Evidence.kt`
- `core/src/test/kotlin/app/memo/core/engine/WorkflowEngineSafetyTest.kt`
- `STATUS.md`
- `app/build.gradle.kts`
- `gradle/wrapper/gradle-wrapper.properties`
- `.github/workflows/build-memo-apk.yml`
- `LICENSE`

**What is useful and test-backed in its own repository**
- A pure-Kotlin core with executor interfaces separated from Android-specific execution.
- Typed attempt results rather than an executor silently deciding workflow state.
- Side-effect execution requires the nearest preceding approval gate.
- Approval is bound to an exact artifact content hash; a changed artifact is not covered by stale approval.
- The engine checkpoints an in-flight action before invoking its executor, and an interrupted side effect recovers as NEEDS_REVIEW rather than auto-running again.
- Per-attempt idempotency keys and bounded retry behavior.
- Bounded context packets with visible truncation, output redaction, provider routing/health/rate-limit abstractions and extensive JVM tests.
- Evidence records retain source, quote and capture time; quoted evidence is expected to be contained in the claimed source before acceptance.

**Explicitly not reusable as an implemented capability**
- `STATUS.md` says browser automation returns an explicit failure and no browser executor is implemented.
- `STATUS.md` says app automation returns an explicit failure and no AccessibilityService exists.
- The status also says LiteRT-LM model files can be downloaded/verified but on-device inference is not implemented.
- The last observed full `Build Memo APK` success in the inspected Actions history was 2026-10-26. The 2026-11-16 runs were dependency-update workflows, not a new full Android build.
- The product shell/runtime and app-specific persistence are not to be imported into Trader.

**License note:** the inspected Memo-2 `LICENSE` is MIT. If any substantial code is ever copied, retain the required copyright and license notice. Current Trader plan is to reimplement the contracts and write Trader-specific tests rather than depend on Memo-2 source.

**Trader disposition:** implement a simpler Trader-owned pure-Kotlin workflow core after the local evidence slice. Reuse the concepts and adversarial test cases: approval hash binding, stop/cancel, durable checkpoint, ambiguous side-effect recovery, idempotency, packet size caps, redaction and explicit no-route. Treat browser/app execution as future work until independently implemented and device verified.

### 5. EBO — commercial workflow concepts only; do not port the execution machinery

Repository: `ysfclc/ebo-n8n`, private, default branch `main`.

Inspected through source search:
- `db/migrations/0096_action_ladder_rungs.sql`
- `db/migrations/0140_campaign_capacity_and_sequence_proposal.sql`
- `apps/cockpit/app/(cockpit)/[slug]/campaigns/[id]/page.tsx`
- `apps/cockpit/app/(cockpit)/[slug]/approvals/actions.ts`
- `docs/TESTING.md`
- `docs/superpowers/specs/2026-08-11-p3-action-budgets.md`
- `docs/superpowers/plans/2026-08-11-close-the-loop-and-action-ladder.md`

**Useful concepts**
- Signals should justify a next action; a sequence should not be a bare send engine.
- Provider-specific capabilities and action ladders can be represented as data.
- Run outcomes, evidence and errors should be inspectable.
- The operator needs a clear approval surface and explicit action budget/limits.

**Why it is not a safe execution base**
- EBO is a web/Postgres/provider integration product with different assumptions from a private local Android tool.
- Its own testing/spec documents record cases where values are stored but not enforced by the actual executor or UI path. In particular, the action-ladder migration comments and testing notes distinguish configuration from enforcement.
- Existing docs report connectors and some approval/journey paths as incomplete. The architecture depends on hosted services and provider integration such as Unipile.
- A complex sequence engine would be unnecessary before Trader can capture and match a real requirement.

**Trader disposition:** retain the signal-first principle and explicit gate verification. Implement Trader's own small state machine and tests; never port EBO sequences, provider assumptions or connector code as trusted behavior.

## Current reuse matrix

| Capability | Best reference | Trader decision |
|---|---|---|
| Source registry and parameter schemas | Outbound Sourcer | Reimplement the interface and policy model in Kotlin |
| Search streaming, partial commits, warnings and cancellation | Outbound Sourcer pipeline | Reimplement as durable local jobs; add process-death recovery tests |
| Buyer/supplier/product domain | None of the existing apps | Design from Trader's DATA_MODEL; do not reuse generic leads/trips/files |
| Local database and Android import | VaultOS | Use Room/Flow and system share/SAF concepts; create a new schema |
| Safe backup/restore | None is fully sufficient | New encrypted format, schema version, hash checks and automated corruption tests |
| Raw evidence and dedupe | BusyBee, Outbound Sourcer ingestion | Reimplement source evidence + normalized record + fingerprint in Trader |
| Workflow approvals and crash recovery | Memo-2 | Reimplement selected pure Kotlin contracts with Trader-specific tests |
| Prompt context and redaction | Memo-2 | Reimplement bounded packet/redaction policy; no model dependency |
| Personal multi-channel messaging | None | Independent per-channel adapter; user handoff first |
| Browser automation | Memo's planned boundary + Outbound Sourcer Playwright | Build visible Android browser capability independently; neither project proves Trader's mobile automation |
| Installed-app automation | BusyBee notification source and Memo-2 unimplemented plan | Start with intents/share; separately test optional AccessibilityService; no generic automation claim |
| Commercial sequence engine | EBO | Do not port; wait until a verified deal workflow exists |
| Android CI and release signing | BusyBee workflow | Adapt secret-only signing + debug fallback + test/lint/APK gates |

## Recommended Trader implementation base

- Native Android project, Kotlin and Jetpack Compose.
- Room/SQLite with exported schema, migrations and migration tests.
- WorkManager only for bounded read-only or safely resumable jobs; side effects always return to an explicit approval flow.
- A pure Kotlin domain/workflow module for deterministic logic, independently testable without Android.
- App-private local evidence and encrypted secrets; encrypted export with integrity verification.
- New application ID, new signing identity and CI-only secrets.
- No Rust/JNI dependency for the initial release.
- No backend, Memo, EBO, Unipile, n8n or paid model required for the first vertical slice.
- First vertical slice: capture a buyer requirement or supplier offer from Android share/manual entry, save immutable source evidence, normalize editable fields, and retrieve it after process death/offline restart.

## Important audit boundary

This audit inspected relevant files and recent action metadata. It did not run any source repository's test suite, build any APK or install an app on a physical device. Existing CI results are attributed to their repository and exact run dates above; they are not evidence that Trader works.
