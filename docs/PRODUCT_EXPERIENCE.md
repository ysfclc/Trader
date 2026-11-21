# Trader — Discovery and Execution Experience

**Status:** implementation direction for the next product phase  
**Date:** 2026-11-20  
**Repository:** `ysfclc/Trader`  
**Boundary:** Trader is independent. Memo is a UX/runtime reference only and is never a required dependency.

## 1. Product correction

Trader must not present itself primarily as a manual case tracker. Inbox, opportunity records and activity are supporting views over a larger operating loop:

**Find a relevant signal → identify the organization and people → preserve evidence → explain fit → prepare an approach → work through the right channel → verify what happened → retain the relationship and next step.**

Company discovery and outreach are a priority use case, not the product boundary. The same loop must remain usable for careers, services, trade and non-commercial problems.

The purpose of automation is to reduce repeated searching, copying, context reconstruction and channel switching. It is not to maximize scraped volume or send generic bulk messages.

## 2. First prospecting method: problem-led, proof-backed discovery

Start from something the user can genuinely deliver: an existing product, working prototype, repository, service capability, domain skill or documented result. Do not begin with a generic industry list or generic CV.

1. **Select a proof asset.** Examples include a working Android product, an automation workflow, an industrial machining capability or a documented commercial result. The user controls which assets are available for external use.
2. **Derive problem signatures.** Describe the concrete problem the asset addresses, the observable language a company might use, adjacent terms, likely buyer roles and disqualifiers. Keep the strategy editable and versioned.
3. **Search for intent signals.** Search permitted public sources for evidence that a company is already experiencing, discussing, funding, procuring or hiring to solve that problem. Possible source families include company websites, public tenders/RFQs, job postings, public directories, partner/vendor pages, public announcements and user-authorized browser sessions. A source is enabled only after its access mode, terms, authentication, rate limits and failure behavior are documented.
4. **Resolve candidates.** Normalize company names/domains, detect likely duplicates, and preserve the original source item. Never merge uncertain entities silently.
5. **Build a company dossier.** Extract claims such as what the company does, the observed need, relevant products/processes, geography and public contact routes. Every claim links to a source, capture time, excerpt and extraction method.
6. **Explain fit.** Separate observed facts, user-supplied facts, deterministic matches, model-generated suggestions and unknowns. Explain why the company is relevant to the selected proof asset, what evidence supports that view, what contradicts it and what is still missing.
7. **Prepare a specific approach.** Generate an individualized draft using verified facts and the selected proof asset. Do not invent relationships, authority, customer results, product capabilities or a company's intent.
8. **Reach out in context.** Open the target in Trader's visible browser or a supported Android communication handoff. Show the exact destination and payload. External submission requires a current approval bound to recipient, channel, content hash and scope. If a provider cannot be safely automated, leave the user in control with a prepared draft or handoff.
9. **Verify and remember.** Record a provider receipt or observable page state. If the result is ambiguous, mark it for review and never blindly retry. Preserve replies, objections, follow-up and the evidence that changed the assessment.

This is targeted research and relationship development, not indiscriminate scraping or bulk outreach.

## 3. Main experience

The interface should feel like one operating workspace, not a collection of disconnected CRUD screens. Use Memo's useful interaction principles—persistent context, visible execution, checkpoints, takeover and recovery—without copying Memo's product identity, navigation wholesale or runtime.

### Primary destinations

- **Discover** — create and run discovery strategies; inspect search progress, source health, candidate companies, relevance explanations and new signals. This should become the default landing destination once a real discovery run is available.
- **Work** — the persistent graph of companies, people, opportunities, cases, sources, evidence, projects and follow-ups. Inbox and Pipeline become filtered views within this workspace, not the whole product.
- **Live** — active and recent runs, browser/app sessions, approvals, checkpoints, failures, cancellation and uncertain outcomes. The browser opens here as a visible runtime pane and can expand to a full-screen browsing workspace.
- **Studio** — research dossiers, comparison briefs, outreach drafts and other editable deliverables with source manifests and revision history.

Do not add a bottom tab for every entity or capability. Do not ship a primary destination until its core journey is functional. Preserve deep links and the current local records during navigation migration.

### Contextual browser

Browser is a Trader-owned capability and a persistent execution surface, not an external-browser-only link and not a separate mini-product.

The usable browser baseline includes:
- visible address/search bar, back, forward, reload/stop and loading/progress state;
- multiple tabs with explicit tab identity and recoverable session state;
- page title, current origin and security/navigation state;
- bookmarks and recent history, plus find-in-page;
- downloads with truthful progress/result and safe file handling;
- page capture into a case/company dossier with URL, title, capture time and selected evidence;
- clear cookie/session controls and a way to distinguish normal and private sessions;
- a visible automation/task panel showing the requested action, current step, target origin, evidence and pause/stop/takeover controls.

Do not silently navigate to a different origin, submit forms, send messages or execute destructive actions. Detect login, CAPTCHA, OTP, consent walls and unexpected states; pause for the user. Never bypass access controls or hide automation. Browser state and evidence must be owned by Trader, not Memo.

#### Capturing authenticated research

The browser must turn a useful page into durable Trader evidence. The user taps Capture, then sees an editable review sheet showing page title, source and either selected text or readable visible page text. The user can redact/edit content and save it to an existing active case or create a new Research record. Saved evidence retains source URL and capture time and begins UNREVIEWED. Capture reads rendered body text, selected text, visible headings and page description only; it must never inspect cookies, browser storage, passwords, hidden form values or tokens. Page text can still include private information, so the review step is mandatory.

Memo may share a capture through Android Sharesheet using MEMO_BROWSER_CAPTURE_V1. Trader recognizes that versioned payload and stores it as a new Research record with unreviewed source evidence; ordinary text/URL shares remain unchanged. The bridge exports only the sanitized base page URL (no query or fragment), title, summary and bounded visible text. It does not export or transfer Memo's account session.

#### Account and identity sessions

Account login is a core browser requirement, not a later convenience. For providers that permit embedded browsing, Trader must maintain its own persistent WebView profile: accept first-party cookies, explicitly configure third-party cookies per WebView where SSO requires them, retain WebView DOM storage, flush cookie state at lifecycle boundaries, support user-initiated popup/consent windows, Android autofill and website file-upload selection. The same app-local cookie jar is used by Trader's main and popup WebViews; sessions must survive leaving the browser screen and a normal app process restart.

Identity providers may prohibit embedded user agents. Google explicitly blocks Google sign-in inside Android WebViews. When Trader detects Google's OAuth endpoint, it must reopen the original relying-party website in the default system browser so that the full authentication flow starts there. A system browser has its own cookie store; Trader must never claim that Chrome sign-in authenticates its embedded WebView, and must not scrape, copy or inject browser cookies. Other provider restrictions, MFA, passkeys, CAPTCHA and device permission requests must be surfaced and handled through a supported user path rather than spoofing a browser or bypassing access controls.

CI tests cover URL/provider routing and compile the Android cookie configuration. Actual CookieManager behavior cannot be established by the current JVM/Robolectric test environment. Device acceptance must separately verify password login, a supported SSO flow, session persistence after process restart and the external Google OAuth fallback on the target Android device.

### Company and contact workspace

A company is a durable entity, not just a text label on an opportunity. Its dossier should connect:
- canonical name, domains, locations and identity-resolution state;
- people and public role/contact routes, with source and confidence per field;
- observed needs/signals and their dates;
- evidence and source documents;
- fit assessments against selected proof assets;
- opportunities, conversations, outreach drafts, handoffs, replies and follow-ups.

Show uncertainty and provenance at field level. A company match or person match is a proposal until resolved. Support reversible merge/review rather than silent deduplication.

### Outreach workspace

Channels are adapters with explicit capability declarations. For each channel, show whether Trader can discover, read, draft, hand off, send, receive replies and verify outcomes. Do not imply that LinkedIn, WhatsApp, email or a website form is integrated merely because a URL can be opened.

A browser session can help the user research, prepare and operate a site visibly. Sending remains a distinct action with an exact approval. An Android intent that opens an email composer is a handoff, not a sent message. A click is not a receipt.

## 4. Core architecture

Keep the existing ProblemCase, Evidence, Plan, Workflow, Activity and optional Opportunity profile. Extend rather than replace them.

Introduce Trader-owned contracts and entities in deliberate migrations:
- `ProofAsset` and `DiscoveryStrategy`: what the user can offer, problem signatures, target constraints, query plan and version.
- `DiscoveryRun` and `SourceRun`: bounded run state, source, query, timestamps, limits, health, counts and errors.
- `Organization` and `Person`: normalized entities with aliases/domains and explicit identity-resolution state.
- `SourceDocument` and `EvidenceClaim`: immutable source capture plus individual claims, excerpts, parser/model provenance, freshness and review status.
- `FitAssessment`: explainable components, supporting/contradicting evidence, missing facts and reviewer override.
- `OutreachDraft`, `ChannelSession` and `Interaction`: recipient, channel, exact content, approval, handoff/send state, receipt, reply and follow-up.
- Reusable capability adapters for public search, source fetch/parse, visible browser, optional AI extraction/drafting and communication channels.

Keep raw source data separate from normalized claims. AI may propose structure and language; deterministic code owns schema validation, source policy, deduplication proposals, scoring arithmetic, approvals, workflow state and verification. Never elevate a generated claim to verified fact automatically.

The capability registry remains fail-closed. Every network/browser/channel adapter must declare actual access mode, origin scope, permission/consent, data disclosure, cost, byte/time/rate/concurrency limits, retry policy and postcondition before it can be marked available.

## 5. Automation and safety model

- Discovery reads are bounded, cancellable and rate-limited per source. Cache results and preserve source freshness.
- A failed or blocked source is visible; it does not silently fall back to an unapproved source.
- No credentials, cookies or private app data are copied into logs, evidence exports or model prompts by default.
- Remote AI disclosure is explicit, provider-specific and scoped to selected context. Local/deterministic paths remain useful without paid AI.
- External writes require approval bound to exact recipient, destination, content hash and capability. A changed draft invalidates approval.
- Do not auto-retry uncertain external side effects. Move them to NEEDS_REVIEW.
- Browser automation remains visible and interruptible. The user can take over at any point.
- Every completed action has a defined observable postcondition; launch/click/attempt alone is not success.

## 6. First genuinely useful end-to-end release

The first release that should replace the current manual impression is a focused **proof asset → company discovery → dossier → outreach preparation** journey:

1. Select one real proof asset from the user's existing work.
2. Create a discovery strategy describing the problem it solves and the target company signals.
3. Run at least one real, permitted public source adapter and show its source health and provenance.
4. Produce deduplicated candidate companies with original source evidence.
5. Open a company dossier that explains fit, unknowns and next research steps.
6. Open the company's website in Trader's browser, capture a relevant page and add the evidence to the dossier.
7. Generate an individualized draft grounded only in verified facts and the selected proof asset.
8. Open the correct communication route with the draft ready for user review; do not claim it was sent unless a real provider confirmation is captured.

A static list, seeded demo data, search mock, browser link-out or draft-only screen does not satisfy this release.

## 7. Delivery sequence

1. **Experience and data foundation:** define route/state ownership; add a real Work/Discover/Live shell without losing existing Inbox/Pipeline/Activity records; add navigation and empty/error/loading tests.
2. **Source discovery vertical slice:** implement one documented, permitted public source end-to-end with real query execution, result provenance, cancellation, bounded limits and deduplication proposals.
3. **Trader browser:** implement the in-app browser baseline, persistent tabs/session, evidence capture and visible task controls.
4. **Company dossier and explainable fit:** introduce normalized organization/person records, evidence claims and a first proof-asset fit assessment with tests and migration/backup compatibility.
5. **Outreach preparation and channel handoff:** draft from verified facts, show channel capability truthfully, bind approval to exact payload and capture handoff/receipt/uncertainty.
6. **Broaden source/channel coverage only after the first end-to-end journey works on the target phone.**

Do not attempt all six as one giant rewrite. Each increment must preserve current data, include tests and docs, pass CI, produce a verified APK and record physical-device acceptance separately.


## Source Manager and Discover

Discover is not a fixed list of hardcoded targets. The operator can add/configure source instances (initially public RSS/Atom feed URLs), enable/disable/remove them, run a visible one-shot public-web search, manually refresh a feed, inspect source health and save an item into Work as source-linked UNREVIEWED research evidence. Search results and feed items retain original source URL, source label, publication time when available and retrieval time. Saving the same canonical URL twice resolves to the existing research record rather than creating duplicate leads.

The broader ecosystem includes public search, feeds/publications, company sites/careers/changelogs, registers/procurement/job marketplaces, public communities/federated networks, developer/product ecosystems, reviews, explicitly authorized messaging workspaces, user-provided material and signed webhooks. Provider access is adapter-specific; see docs/SOURCE_ECOSYSTEM.md.

A manual refresh is not a scheduled watch. Do not claim background monitoring until durable jobs, per-source rate/cursor/backoff, pause/stop, deletion/retention and recovery exist. Facebook group research is visible browser capture initiated by the user unless an approved API route is obtained. Reddit requires dedicated OAuth and deletion/retention enforcement; Bluesky and Mastodon need separate public API adapters; private Telegram/Discord/Slack groups require explicit authorized installation.


The first implemented example feed is Hacker News front page at https://news.ycombinator.com/rss. It is an optional one-tap sample, not a required source catalogue; operators can add any permitted public RSS/Atom URL. A feed check is explicitly manual and results are saved only after a user action. The branch CI validation (run 385) passed 97 tests and the full Android build/lint/signature/package pipeline; device acceptance and a real network feed check remain open.
