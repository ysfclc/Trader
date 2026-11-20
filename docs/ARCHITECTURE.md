# Trader — Architecture

**Architecture status:** baseline locked; implementation follows phased acceptance gates.  
**Primary client:** native Android, Kotlin and Jetpack Compose.  
**Data ownership:** local-first; no mandatory hosted backend.

## 1. System boundary

Trader is an independent application with its own:
- Android application shell and navigation;
- domain model and workflow engine;
- local database and migration strategy;
- source registry, fetch policy and parsers;
- browser and app-automation adapters;
- AI/model routing and prompt/context controls;
- communication handoff adapters;
- audit/evidence trail, settings, backup/export and tests.

Trader does not embed, import or call Memo as a required runtime. Memo may be studied as a historical source of ideas, but a Memo bug, APK, service, database, release or integration must never block Trader.

EBO, n8n, Unipile, Apollo and other existing systems are not dependencies. Proven patterns can be reimplemented behind Trader-owned contracts.

## 2. Android technology baseline

Initial implementation target:
- Kotlin.
- Jetpack Compose UI.
- AndroidX Navigation Compose.
- Room over SQLite for durable structured records.
- WorkManager for bounded, resumable scheduled and background work.
- Android Keystore for encryption-key protection.
- Coroutines and Flow for asynchronous work and observable state.
- OkHttp or an equivalent maintained HTTP client.
- Jsoup or equivalent for static HTML parsing.
- Android WebView in a dedicated, visible browser surface for JavaScript-heavy sites and user-observed browser workflows.
- Android system share intents, document picker and deep links for user-directed handoffs.

Use a stable Android/Gradle/Kotlin dependency set and pin compatible versions. Do not copy VaultOS credentials, signing keys, package identity or application-specific components. Establish a new Trader package ID and signing configuration as part of the Android foundation.

The first device target is Redmi Note 12 Pro 5G, but core logic must remain testable on JVM without an Android device.

## 3. Layers and modules

### problem
A user-owned ProblemCase/Goal intake and resolution model. It preserves the original request, desired outcome, constraints, stakes, relevant context, unresolved questions, evidence, candidate paths, selected plan and verified outcome. It can route a case to a specialized workflow (trade, services, careers or a future domain) without forcing every case into one of those types.

### evidence and context handoff

The local-first evidence path stores user-attached text/notes and an optional source URI as a separate record linked to a ProblemCase. Verification is explicitly user-controlled and defaults to unreviewed. Source links are not fetched automatically. A context-packet builder composes the ProblemCase, ordered plan, evidence content/source/verification labels and bounded assistant instructions. The Android share sheet is the only handoff; no external message or AI request is sent without a user action.

Cases can be archived and restored by a nullable archive timestamp. This is soft archival, not deletion. The Room v2→v3 migration adds the archive timestamp and evidence table while preserving existing data. Room v3→v4 adds an optional one-to-one OpportunityEntity per ProblemCase. Existing cases migrate without a profile; old schema-v3 encrypted backups remain restorable with an empty opportunity set.

The Compose Opportunity Pipeline is a projection of active ProblemCases plus explicitly created opportunity profiles. The profile stores a manually selected lane, organization/contact labels, a user-authored fit note, stage, next action and a local follow-up date. It does not replace the ProblemCase/evidence source record or instantiate normalized Organization/Person entities. Stage changes and profile edits append activity events.

A contact route is only a user-initiated handoff: HTTP(S) to a browser, mailto: to a compose app, or tel: to the dialer. There is no send/call adapter, automatic discovery, or follow-up notification. Dates are visible when the user opens Pipeline; no background reminder is promised.

### capabilities
A Trader-owned registry of available capabilities: source discovery, browser sessions, file parsing, app handoff, repository operations, deterministic analysis, optional AI, communication and other tool adapters. Each capability declares inputs, outputs, permissions, cost/availability, limits, side effects, preconditions and verifiable postconditions. A plan may only invoke capabilities actually registered and available.

### app
Compose surfaces, navigation, permission explanations, user-triggered workflows, browser session UI and lifecycle integration.

### domain
Entities and invariants: ProblemCase, Goal, Plan, PlanStep, Capability, Organization, Person, Product, BuyerRequirement, SupplierOffer, ServiceNeed, JobOpening, Source, Evidence, Match, Quote, Deal, Communication, ConsentBasis, Task and Event.

### data
Room entities/DAOs, repository implementations, migrations, encrypted local secrets, backup/restore, import/export and database integrity checks.

### discovery
Source registry, adapters, fetch policies, rate limits, retry/cancellation, parsing and source-health reports.

### browser
Visible WebView sessions, navigation policy, user takeover, DOM inspection/extraction where permitted, bounded scripted actions, screenshots/evidence where appropriate and postcondition checks.

### app_automation
Separate optional Android AccessibilityService adapter for user-enabled, foreground, bounded interactions with installed apps. It must clearly disclose its capability and permission scope, stop immediately on user request, avoid password/payment/OTP fields and hand unexpected states to the user.

### matching
Deterministic requirement-to-offer comparisons, explicit unknowns, compatibility rules, explanation, confidence components and reviewer overrides.

### workflow
Typed plans, durable checkpoints, append-only events, cancellation, retry policy, idempotency keys and exact-payload approval gates.

### ai
Optional model interface, local model discovery/runtime adapter, bring-your-own-key providers, prompt templates, context selection, output schemas, redaction and cost accounting. AI is optional.

### channels
Capability registry for email, Telegram, WhatsApp, LinkedIn, phone, browser and Android share handoff. Each channel declares the operations it really supports: discover, read, draft, send, sync history or receive replies.

### security
Credential vault, network policy, redaction, audit verification, permissions, data export/deletion and kill switch for active automation.

## 4. Source adapter contract

Each discovery source declares:
- stable source ID, name, source type and public documentation/terms URL;
- permitted access mode: official API, public HTML, user-visible browser, user-provided file, share/import or manual entry;
- query parameters and expected fields;
- authentication and permissions, if any;
- allowed regions, domain scope, request limits and delay policy;
- parser version and raw-item schema;
- known restrictions and source health;
- status: available, credential-required, user-action-required, blocked, experimental or disabled.

The source adapter returns raw evidence before enrichment. Every normalized claim links to the raw record and source. A source returning no data must produce an explicit zero-result or failure state, never fabricated fixtures presented as live results.

Source-specific adapters remain independent. A single global crawler with hard-coded assumptions is not the architecture.

## 5. Browser automation

Browser automation is a first-class Trader capability, but it is not unrestricted.

Required behavior:
- visible session with current URL and activity state;
- user can pause, stop, take over and close;
- allowlisted or user-confirmed navigation scope;
- bounded action plans and rate limits;
- evidence capture for important extraction and state changes;
- explicit detection of login, CAPTCHA, OTP, consent walls and unexpected UI;
- no attempt to bypass authentication, paywalls, bot protection or access controls;
- no external submission until the exact action reaches an approval gate;
- postcondition check before the workflow records success;
- interrupted or ambiguous submit results become NEEDS_REVIEW, not automatic retries.

Static public pages should use lightweight HTTP fetching when allowed. JavaScript sites may require the visible WebView. Do not run a large browser for every source when a simple permitted fetch is sufficient.

## 6. Installed-app automation

Android does not grant ordinary apps universal access to other apps' private data or unrestricted control. Trader must work within public Android APIs and user-granted capabilities.

Preferred order:
1. official API or supported Android intent;
2. system share/document picker/deep link;
3. visible browser handoff;
4. user-enabled, explicit AccessibilityService for bounded interactions where appropriate;
5. manual action with prefilled content when automation is not supported or reliable.

Accessibility is not the default foundation and must not be used to bypass app protections, extract passwords, read protected private content or perform hidden actions. The user must be able to see and stop it.

## 7. AI and prompt-feeding architecture

AI providers are optional adapters, not product dependencies.

Default path:
1. deterministic fetch/import;
2. deterministic parse and schema validation;
3. deterministic deduplication;
4. deterministic matching and constraint checks;
5. templates and rules for simple drafting;
6. optional AI for messy extraction, translation, synonym mapping, qualification questions, summaries and message drafts.

Every AI job receives a bounded context packet with source IDs, permitted fields and an explicit task schema. It must not receive all private records by default.

AI output is untrusted:
- validate against a declared schema;
- attach evidence references to extracted claims;
- distinguish quoted source text, normalized fact, inference and unknown;
- reject fabricated identifiers, URLs, prices, certificates or contact details;
- require human review for material technical equivalence and commercial claims.

Prompt templates, model policies and budgets should be inspectable and editable. Trader must build context packets from the current ProblemCase, selected evidence, relevant history and explicitly permitted files; it must not dump all user data into a prompt by default. The operator can inspect and edit the prepared instructions and context before a consequential or remote model call. Record provider, model, prompt/template version, context IDs, consent and cost where available. Do not trigger a paid provider or remote model silently.

The workflow engine may sequence deterministic tools, local or remote model calls, visible browser steps, supported app handoffs, user tasks and approval gates. Every step has a typed input/output contract, timeout/cancellation behavior, durable state and an expected postcondition. Prompt generation is one step in a workflow, not a substitute for workflow execution.

## 8. Communication and Unipile alternatives

There is no safe assumption that a free one-for-one Unipile replacement exists for personal LinkedIn, WhatsApp, Telegram and email inbox sync.

Trader must use independent channel adapters:
- **Email:** user-controlled draft/share first; official Gmail or other mail APIs can be an optional later connector with explicit OAuth and quota handling.
- **Telegram:** Bot API for bot interactions; this does not provide the user's personal inbox history.
- **WhatsApp:** Android share/deep link for user-composed messages initially. Official WhatsApp Business Platform is a separate later integration with account, template, policy and potential messaging charges.
- **LinkedIn:** user-visible browser/app handoff for personal-account activity. Do not assume an unrestricted personal messaging API. Avoid unofficial session/cookie scraping.
- **Other apps:** supported intents/deep links or user-enabled bounded app automation only.

The UI must show per-channel capability truthfully. "Connected" does not mean every operation is available. Read, send, sync history and reply ingestion are separate capabilities.

## 9. General problem-solving workflow

The problem workflow is not an AI chat that merely returns advice. It is a durable, inspectable process:

1. Capture the user's problem, goal or situation in its original form.
2. Clarify the desired outcome, constraints, urgency, risk and missing information.
3. Gather relevant sources, files, app-provided information and user-approved context.
4. Separate source facts, user statements, inferences, assumptions and unknowns.
5. Identify candidate solutions, people, services, products, jobs, projects or tools.
6. Build a plan from capabilities that are actually available, with dependencies and expected postconditions.
7. Execute reversible/read-only steps where authorized; pause for approval before consequential side effects.
8. Verify each important result and preserve failures, partial completion and next steps.
9. Let the user close, revise or resume the case; retain only useful learning under user-controlled privacy and retention settings.

A problem may be handled manually, through a specialized workflow or with tool automation. A plan must expose what Trader can do itself, what requires the user, what requires an external service and what is currently unsupported. Never manufacture a solution path just to make a case appear actionable.

## 10. Data and event flow

All meaningful activity is recorded as events:
- source discovered;
- page fetched;
- evidence captured;
- field normalized;
- human reviewed;
- organization/contact linked;
- match generated or overridden;
- quote received;
- draft created/edited/approved;
- message handed off or sent;
- reply recorded;
- task completed;
- deal state changed.

Events are append-only. Current UI state is a derived projection and can be rebuilt. Corrections append a new event referencing the superseded fact; they do not silently rewrite audit history.

## 11. Reliability and recovery

- Every background job has a durable ID, state, timestamps, source, progress counters and warnings.
- Work is committed in bounded units; cancellation is honored between units.
- A retry must be safe and idempotent.
- Never automatically repeat an ambiguous external send or submit.
- Store raw input before AI enrichment.
- Keep partial extraction results when a later page fails.
- Failed records remain inspectable and retryable.
- App process death resumes read-only work safely; side-effecting actions require renewed verification/approval.
- Database migration tests, backup round-trips and corruption recovery are release gates.

## 12. Network and hosting

The initial workflow should not require a backend. Use local persistence and user-triggered network access.

A backend may be introduced only for a specific evidenced need such as shared multi-device sync, scheduled source monitoring, team use or secure webhook receipt. Before doing so, document provider cost, data residency, encryption, backup, authentication, retention, abuse controls and exit/migration path.

## 13. Security

- Secrets and OAuth tokens are kept outside ordinary logs and source records.
- Credentials are never inserted into AI prompts or screenshots.
- Sensitive context is redacted before remote AI use.
- User-provided cookies are not exported or scraped.
- No hidden background recording of third-party apps.
- Automation has an immediate stop mechanism.
- The exact approved destination and payload are checked again before execution.
- No fake success: a side effect needs a source-confirmed postcondition or is marked uncertain.


## 10. Discovery-first company workflow

The next product priority is a problem-led prospecting loop that starts from a user-approved ProofAsset and searches for companies showing public evidence of a relevant need. It is not a generic lead scraper. The detailed experience contract is in `docs/PRODUCT_EXPERIENCE.md`.

The discovery pipeline is:

`ProofAsset → DiscoveryStrategy → DiscoveryRun/SourceRun → raw SourceDocument → Organization/Person resolution → EvidenceClaim → FitAssessment → OutreachDraft → approved channel action → receipt/reply/follow-up`.

Keep each stage independently testable and resumable. Source adapters are provider-specific and must declare access mode, terms/policy, authentication, origin scope, rate limits, byte/time/concurrency limits, cost, freshness and failure states. A run may return partial results when a source fails, but the failure must be visible. Cache raw results and retain canonical source URLs and capture timestamps. Deduplication produces reviewable match candidates rather than silently merging records.

Proof assets and discovery strategies are user-owned and versioned. Strategy generation can suggest problem signatures, search phrases, buyer roles and disqualifiers, but the user can inspect/edit them. Discovery queries should find observable signals (public requests, hiring, procurement, product/process changes, public announcements or directory evidence) that the company is already addressing a problem. Fit scoring must show its components and evidence; a model-generated score alone is not evidence.

## 11. Trader-owned browser runtime

Trader owns a visible Android WebView session and its lifecycle. It must support ordinary browsing fundamentals and expose current origin, loading state, tabs, navigation, bookmarks/history, find-in-page, downloads, session/cookie controls and page-to-dossier evidence capture. Browser sessions may be opened contextually from Discover, Work or a workflow and are surfaced in Live for inspection and takeover.

Automation is an explicit task over a visible session. Each step has an origin scope, timeout, cancellation path, expected postcondition and evidence requirement. Login, CAPTCHA, OTP, consent walls and unexpected page states pause for the user. Do not bypass access controls. Form submission and messaging are separate external side effects, require exact payload/recipient/destination approval and are never blindly retried after an ambiguous result.

The browser is not a Memo dependency. Do not copy Memo code, services, storage, signing identity or runtime. Reuse only product-neutral interaction lessons: visible work, contextual runtime panes, explicit checkpoints, user takeover and safe recovery.
