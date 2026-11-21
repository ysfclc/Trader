# Trader source ecosystem and monitoring model

_Last researched: 2026-11-21. Provider terms, APIs and quotas must be rechecked before enabling a production adapter._

## Product lock

Trader is not a fixed list of websites and not a universal crawler. It is a source platform: users configure places and signals they care about; Trader selects a built-in adapter that is allowed to read each source; all adapters return a common evidence envelope. Adding another subreddit, RSS feed, company site, public registry or supported community should normally mean adding a source configuration, not changing the discovery data model.

There are three separate layers:
1. **Connector / adapter** — code that implements one protocol or provider API (RSS/Atom, public search, Reddit API, Bluesky, a registry API, visible browser capture, webhook, etc.).
2. **Source instance** — a user-configured place within that adapter (a feed URL, a subreddit, a public Bluesky search, a company careers page, a registry query, a browser-captured group page).
3. **Watch rule / discovery strategy** — what to look for, which languages/geographies/entities matter, what counts as a relevant signal, how often a permitted source may be checked and what should be surfaced.

A provider adapter can serve many source instances. Source IDs, source instances, queries, and result records must not be conflated.

## Source families

| Family | Examples | Useful signals | Preferred access |
|---|---|---|---|
| Public web search | DuckDuckGo and other permitted search providers | Company mentions, public requests, news, niche directories | Search API or permitted public search; one-shot discovery |
| Feeds and publications | RSS 2.0, Atom, JSON Feed, company blogs, trade publications, release feeds | Announcements, hiring, product/process changes, tenders, customer stories | Public HTTPS feed; bounded polling with ETag/Last-Modified |
| Company-owned properties | Company pages, careers, partner pages, changelogs, docs, pricing, customer stories | New products, expansion, hiring, explicit pain, technology changes | Public page fetch when permitted; visible browser for JS/login |
| Public registers and procurement | KRS/CEIDG/REGON/LEI, TED, national e-procurement portals, public tenders/RFQs | Legal identity, contract notices, procurement intent, supplier requirements | Official API, open data download, documented search interface |
| Job and project marketplaces | Employer career pages, Greenhouse/Lever feeds, public job boards, freelance/project requests | Hiring for a capability, outsourcing demand, implementation backlog | Official API/feed or permitted public pages |
| Community discussions | Reddit, Hacker News, public forums, selected Q&A communities, public Discord/Telegram spaces | Explicit asks, workarounds, repeated operational pain, vendor dissatisfaction | Official API or public feed; group membership/bot installation only with owner/admin authorization |
| Federated/social networks | Bluesky, Mastodon, public Threads/YouTube surfaces | Public requests, product launches, technical discussion, creators/companies | Public documented API where available; respect instance/provider settings and quotas |
| Developer/product ecosystems | GitHub issues/discussions/releases, package registries, public roadmaps, app directories | Bugs, integration demand, adoption, migration, product changes | Official API, Atom feed or public page |
| Reviews and customer feedback | Public app-store reviews, public product reviews, forums | Repeated complaints, missing workflows, churn causes, support burden | Official API or permitted public access; avoid collecting unnecessary reviewer identity |
| Messaging/community workspaces | Telegram, Discord, Slack, WhatsApp communities | Private or semi-private needs and requests | Explicitly authorized bot/app installation or user-triggered visible browser/capture |
| User-provided material | Shared pages, files, exports, screenshots, notes, email forwards | Any signal supplied by the operator | Android Sharesheet, file import, browser capture, manual entry |
| Event-driven inputs | Signed webhooks and provider notifications | New records without polling delay | Authenticated, signature-verified webhook endpoint |

This is a source taxonomy, not a claim that every source is currently integrated. The Source Manager must expose actual adapter status per provider: available, experimental, credentials required, user authorization required, browser-assisted, blocked by policy, or not implemented.

## Community-provider research and implementation boundaries

### Reddit
- Reddit Data API guidance requires a registered OAuth token, a truthful and descriptive User-Agent, and compliance with its Developer Terms, Data API Terms and Responsible Builder Policy. Unidentified API traffic may be throttled or blocked.
- Reddit's published free-access limit is 100 queries per minute per OAuth client ID, monitored through rate-limit response headers. Trader must still use conservative per-source limits and honor actual response headers.
- Deleted posts/comments and deleted-account identifying information must be removed from stored data. Reddit recommends routinely deleting stored user content/data within 48 hours. Trader must not build a permanent archive of Reddit content.
- Reddit content may not be used to train an LLM without Reddit's explicit consent. Summarization/classification must be scoped to permitted uses, not treated as model-training permission.
- **Trader route:** dedicated OAuth adapter after app registration and policy review. Persist native item IDs and source URLs, support deletion reconciliation and retention expiry, and make data use/retention visible. Do not scrape logged-in pages as a substitute for API permission.

Official references:
- https://support.reddithelp.com/hc/en-us/articles/16160319875092-Reddit-Data-API-Wiki
- https://support.reddithelp.com/hc/en-us/articles/14945211791892-Developer-Platform-Accessing-Reddit-Data
- https://redditinc.com/policies/data-api-terms

### Facebook groups and Meta content
- Meta's current Content Library covers public Facebook groups and other public content, but is designed for approved research access in a Secure Research Environment or approved cleanroom. The documented API does not grant an ordinary commercial Android app general group-reading access; downloading content from the Content Library API is not permitted.
- It must not be represented as a turnkey Facebook Groups API integration for Trader.
- **Trader route:** user-triggered visible browsing of a group the user can legitimately access, followed by page/selection review and source-linked capture. This is research assistance, not unattended monitoring. A future formal integration requires a separately approved Meta access route and legal/product review. Never bypass membership, login, CAPTCHA, privacy or platform controls.

Official references:
- https://developers.facebook.com/docs/content-library-and-api/content-library-api/overview
- https://developers.facebook.com/docs/content-library-and-api/content-library-api/guides/fb-groups
- https://developers.facebook.com/docs/content-library-and-api/content-library-api/guides/fb-posts

### Bluesky
- The documented public AppView exposes most app.bsky GET endpoints without authentication at https://public.api.bsky.app.
- The AT Protocol ecosystem also exposes Jetstream for a filterable JSON firehose/replay slice. A streaming adapter is promising but needs bounded subscriptions, cursor/reconnect logic, storage limits and policy review.
- **Trader route:** public search/read adapter first; Jetstream only when a real use case requires a live stream.

Official references:
- https://docs.bsky.app
- https://endpoints.bsky.app/

### Mastodon
- Mastodon offers public timeline and hashtag timeline endpoints. Public access can be disabled by the instance.
- Status full-text search is not universally available; it depends on the instance's search backend and authentication. Accounts/hashtags have broader availability than status search.
- **Trader route:** a per-instance adapter with visible host, instance policy, authentication and health, not one assumed global search service.

Official references:
- https://docs.joinmastodon.org/methods/timelines/
- https://docs.joinmastodon.org/methods/search/

### Telegram and other private communities
- Telegram bots see only messages directed to them by default. In a group, a bot can see all messages only when it is added and its privacy mode is disabled; Telegram visibly indicates message access.
- **Trader route:** opt-in group bot only when the user is an authorized group admin/member and the group explicitly accepts the bot. Otherwise use user-triggered browser/share capture; do not read private chats/groups invisibly.
- Discord and Slack should follow the same rule: documented bot/app installation, minimum scopes and workspace-owner authorization.

Official references:
- https://telegram.org/faq
- https://telegram.org/privacy

### YouTube and developer platforms
- YouTube Data API search is quota-governed; current Google documentation describes a separate default 100-call/day bucket for search.list. Treat quota as a hard adapter budget and surface exhaustion.
- GitHub exposes public repository resources, issues, releases and Atom feeds, making it a useful technical-signal adapter candidate.
- **Trader route:** add these only as separate provider adapters with declared quotas/scopes. An API key or OAuth token is not permission to ignore provider terms.

Official references:
- https://developers.google.com/youtube/v3/docs/search/list
- https://developers.google.com/youtube/data/determine_quota_cost
- https://docs.github.com/en/rest

## Monitoring modes

Every source instance declares one of these read modes; the UI must not call them all "monitoring":

1. **One-shot search** — executes a query now; no continued watch is implied.
2. **Manual refresh** — user explicitly checks a configured source; suitable for the first feed implementation.
3. **Scheduled polling** — durable WorkManager/backend job with per-source cadence, ETag/Last-Modified, cursor, backoff, retention and missed-run recovery.
4. **Streaming** — long-lived/event stream (for example, a provider firehose); needs reconnect cursors, bounded queues, heartbeat, battery/data budgets and stop controls.
5. **Webhook** — provider sends events to an authenticated endpoint; requires signature verification and replay protection.
6. **Browser-assisted capture** — user is visibly logged in, sees the target page, starts capture and reviews/redacts material before saving. It is not unattended polling.
7. **Import/manual** — user supplies a file, link, message, screenshot or note.

The first mobile implementation is available in PR #18: one-shot DuckDuckGo search, configurable public RSS/Atom source instances, explicit manual refresh, source health, open-in-Trader-browser and save-to-Work as UNREVIEWED evidence. Fresh installs land on Discover; a one-tap Hacker News front-page RSS example can be added. CI run 385 passed 97 tests and the full build/lint/signature/package pipeline. No live-device or real-feed network smoke test has been recorded. It is intentionally one-shot public search plus user-triggered RSS/Atom refresh. Do not schedule background polls or run an unbounded crawler until the durable job runtime and source-specific policy controls are implemented.

## Common adapter and evidence contracts

An adapter declares:
- stable adapter ID/version and supported operations (search, read, poll, stream, webhook, capture);
- source documentation/terms URL, legal/permission assumptions and access mode;
- auth scopes/consent and credential storage requirements;
- host/origin scope, region restrictions, request/result/byte/time/concurrency ceilings;
- quota/rate-limit headers, backoff, cursor/ETag support and deletion/reconciliation behavior;
- data categories, personal-data minimization, retention/deletion rules and costs;
- explicit health, zero-results, partial-results, blocked, throttled and error states.

A configured source instance stores adapter ID, user label, endpoint/native scope, user query/watch rules, enabled state, last attempt, last successful check, cursor/checkpoint, source health and policy/consent version. Secret credentials never belong in ordinary source configuration or logs.

A raw item/source document should preserve source ID, adapter version, source-native ID, canonical URL, source-published timestamp, retrieval timestamp, title/body excerpt, language when known, content hash, raw/parsed status, and deletion/retention state. Each derived Signal/EvidenceClaim links back to one or more source documents. Search snippets and extracted text remain unverified source claims until inspected; an AI hypothesis never becomes a fact by generation alone.

Normalize and deduplicate by provider-native ID where available, canonical URL otherwise. Cross-source company identity is a separate reviewable step; do not merge a person or organization because names look similar.

## Signal model: what Trader should notice

Source adapters collect material; the signal layer interprets it. Signal categories should include:
- explicit request / looking for / RFP / RFQ / tender;
- operational pain, broken process, manual-work complaint, missed KPI or support backlog;
- hiring for a capability, role, migration or implementation;
- budget, investment, funding, expansion, new location or capacity increase;
- new product, integration, technology, leadership or process change;
- supplier/vendor search, replacement, dissatisfaction or contract renewal;
- compliance/certification/deadline pressure;
- product feedback, repeated customer complaint, unresolved issue or feature request;
- partnership, acquisition, distribution or market-entry signal;
- no-fit, stale, contradictory or insufficient-evidence signal.

A community author is not automatically a business buyer. A post may be personal, speculative, historical, sarcastic, promotional, already resolved or outside the user's authority. Record the original context and uncertainty; score the need and fit separately from the person's identity/contactability.

## Configuration and extensibility rules

- Source instances are user data, not compile-time constants. A source adapter is code; a subreddit, feed URL, company page or group URL is configuration.
- Adding a new instance must not require a database migration. Adding a genuinely new protocol/provider may require a new adapter and tests, but must reuse the common document/evidence/health contracts.
- Built-in adapter registration is explicit and fail-closed. Do not download or execute arbitrary third-party code, scripts or JARs as plugins.
- User-configured URLs must be HTTPS, credential-free and host-validated. Redirects, if enabled by a future adapter, must be bounded and revalidated at every hop.
- No stealth automation, credential/cookie extraction, CAPTCHA solving, access-control bypass or fake user-agent identity.
- The initial local source list contains public feed URLs only; it stores no API secrets. It is currently app-private preference data and is not yet included in Trader's encrypted workspace backup. This limitation must remain visible until backup export/restore integration is added.
- Monitoring and source-derived data must have a user-visible pause/disable/remove action. A source failure cannot silently fall back to a different provider.

## Build order

1. **Delivered and CI-verified:** built-in adapter registry/common result envelope; configurable public RSS/Atom source instances; manual refresh; one-shot DuckDuckGo query in Discover; source health; save-to-Work as unreviewed evidence; open result in Trader WebView.
2. Include source configuration in encrypted backup/restore and add explicit editable watch rules (keywords, language, geography, signal classes).
3. Add durable DiscoveryRun/SourceRun state, scheduled polling, cursor/ETag, per-source backoff, pause/stop and recovery only after WorkManager execution is implemented and verified.
4. Add public API adapters for Bluesky and per-instance Mastodon.
5. Add Reddit OAuth with deletion reconciliation, 48-hour retention controls and current terms review.
6. Add GitHub, public procurement/register and selected job-board adapters.
7. Add Telegram/Discord/Slack with explicit installation/permission; Facebook remains visible user-initiated browser capture unless formal permitted API access is approved.
8. Build broader signal extraction, explainable project-fit assessment and normalized company/person dossiers.

The success metric is qualified, verifiable signal-to-action progression, not feed volume or scraped profile counts.
